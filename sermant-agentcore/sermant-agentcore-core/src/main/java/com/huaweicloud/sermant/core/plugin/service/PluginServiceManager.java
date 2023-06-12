/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huaweicloud.sermant.core.plugin.service;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.event.EventManager;
import com.huaweicloud.sermant.core.event.collector.FrameworkEventCollector;
import com.huaweicloud.sermant.core.exception.DupServiceException;
import com.huaweicloud.sermant.core.plugin.Plugin;
import com.huaweicloud.sermant.core.service.BaseService;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.utils.SpiLoadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 插件服务管理器，核心服务管理器{@link ServiceManager}的特化，专门用来初始化{@link PluginService}
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public class PluginServiceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 服务集合
     */
    private static final Map<String, BaseService> SERVICES = new HashMap<String, BaseService>();

    private PluginServiceManager() {
    }

    /**
     * 初始化插件服务
     *
     * @param plugin 插件
     */
    public static void initPluginService(Plugin plugin) {
        ClassLoader classLoader =
            plugin.getServiceClassLoader() != null ? plugin.getServiceClassLoader() : plugin.getPluginClassLoader();
        ArrayList<String> startServiceArray = new ArrayList<>();
        for (PluginService service : ServiceLoader.load(PluginService.class, classLoader)) {
            if (loadService(service, service.getClass(), PluginService.class)) {
                plugin.getServiceList().add(service.getClass().getName());
                try {
                    service.start();
                    startServiceArray.add(service.getClass().getName());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH,
                        "Error occurs while starting plugin service: %s", service.getClass()), ex);
                }
            }
        }
        FrameworkEventCollector.getInstance().collectServiceStartEvent(startServiceArray.toString());
//        addStopHook(); // 加载完所有服务再启动服务
    }

    /**
     * 加载服务对象至服务集中
     *
     * @param service 服务对象
     * @param serviceCls 服务class
     * @param baseCls 服务基class，用于spi
     * @return 是否加载成功
     */
    protected static boolean loadService(BaseService service, Class<?> serviceCls,
        Class<? extends BaseService> baseCls) {
        if (serviceCls == null || serviceCls == baseCls || !baseCls.isAssignableFrom(serviceCls)) {
            return false;
        }
        final String serviceName = serviceCls.getName();
        final BaseService oldService = SERVICES.get(serviceName);
        if (oldService != null && oldService.getClass() == service.getClass()) {
            return false;
        }
        boolean isLoadSucceed = false;
        final BaseService betterService =
            SpiLoadUtils.getBetter(oldService, service, new SpiLoadUtils.WeightEqualHandler<BaseService>() {
                @Override
                public BaseService handle(BaseService source, BaseService target) {
                    throw new DupServiceException(serviceName);
                }
            });
        if (betterService != oldService) {
            SERVICES.put(serviceName, service);
            isLoadSucceed = true;
        }
        isLoadSucceed |= loadService(service, serviceCls.getSuperclass(), baseCls);
        for (Class<?> interfaceCls : serviceCls.getInterfaces()) {
            isLoadSucceed |= loadService(service, interfaceCls, baseCls);
        }
        return isLoadSucceed;
    }

    /**
     * 获取插件服务
     *
     * @param serviceClass 插件服务类
     * @param <T> 插件服务类型
     * @return 插件服务实例
     */
    public static <T extends PluginService> T getPluginService(Class<T> serviceClass) {
        final BaseService baseService = SERVICES.get(serviceClass.getName());
        if (baseService != null && serviceClass.isAssignableFrom(baseService.getClass())) {
            return (T)baseService;
        }
        throw new IllegalArgumentException("Service instance of [" + serviceClass + "] is not found. ");
    }

    /**
     * 添加关闭服务的钩子
     */
    private static void addStopHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                offerEvent();
                for (BaseService baseService : new HashSet<>(SERVICES.values())) {
                    try {
                        baseService.stop();
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH,
                            "Error occurs while stopping service: %s", baseService.getClass().toString()), ex);
                    }
                }
            }
        }));
    }

    private static void offerEvent() {
        // 上报服务关闭事件
        for (BaseService baseService : new HashSet<>(SERVICES.values())) {
            FrameworkEventCollector.getInstance().collectServiceStopEvent(baseService.getClass().getName());
        }

        // 上报Sermant关闭的事件
        FrameworkEventCollector.getInstance().collectAgentStopEvent();
        EventManager.shutdown();
    }

    /**
     * 关闭插件服务
     *
     * @param serviceName 插件服务名
     * @return
     */
    public static boolean stopService(String serviceName) {
        if (SERVICES.get(serviceName) == null) {
            return false;
        }
        SERVICES.get(serviceName).stop();
        SERVICES.remove(serviceName);
        return true;
    }
}
