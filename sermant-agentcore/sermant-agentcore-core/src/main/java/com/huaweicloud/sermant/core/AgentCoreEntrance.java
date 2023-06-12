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

package com.huaweicloud.sermant.core;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.BootArgsIndexer;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.event.EventManager;
import com.huaweicloud.sermant.core.event.collector.FrameworkEventCollector;
import com.huaweicloud.sermant.core.operation.OperationManager;
import com.huaweicloud.sermant.core.plugin.PluginManager;
import com.huaweicloud.sermant.core.plugin.PluginSystemEntrance;
import com.huaweicloud.sermant.core.plugin.agent.ByteEnhanceManager;
import com.huaweicloud.sermant.core.plugin.agent.adviser.Adviser;
import com.huaweicloud.sermant.core.plugin.agent.template.CommonMethodAdviser;
import com.huaweicloud.sermant.core.service.ServiceConfig;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.service.dynamicconfig.DynamicConfigService;
import com.huaweicloud.sermant.core.service.dynamicconfig.common.DynamicConfigEvent;
import com.huaweicloud.sermant.core.service.dynamicconfig.common.DynamicConfigEventType;
import com.huaweicloud.sermant.core.service.dynamicconfig.common.DynamicConfigListener;
import com.huaweicloud.sermant.god.common.SermantManager;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * agent core入口
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public class AgentCoreEntrance {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    
    private static String artifactCache;

    private static CommonMethodAdviser commonMethodAdviser;
    
    private AgentCoreEntrance() {
    }

    /**
     * 入口方法
     *
     * @param argsMap 参数集
     * @param instrumentation Instrumentation对象
     * @throws Exception agent core执行异常
     */
    public static void install(String artifact, Map<String, Object> argsMap, Instrumentation instrumentation,
        boolean dynamicInstall) throws Exception {
        artifactCache = artifact;

        // 初始化框架类加载器
        ClassLoaderManager.init(argsMap);

        // 初始化日志
        LoggerFactory.init();

        // 通过启动配置构建路径索引
        BootArgsIndexer.build(argsMap);

        // 初始化统一配置
        ConfigManager.initialize(argsMap);

        // 初始化操作类
        OperationManager.initOperations();

        // 启动核心服务
        ServiceManager.initServices();

        // 初始化事件系统
        EventManager.init();

        // 初始化ByteEnhanceManager
        ByteEnhanceManager.init(instrumentation);

        // 初始化插件
        PluginSystemEntrance.initialize(dynamicInstall);

        // 注册Adviser
        commonMethodAdviser = new CommonMethodAdviser();
        Adviser.registry(commonMethodAdviser);

        // 增强静态插件
        if (!dynamicInstall) {
            ByteEnhanceManager.enhance();
        }

        // 创建指令监听器
        createOrderListener(artifact);

        // 上报Sermant启动事件
        FrameworkEventCollector.getInstance().collectAgentStartEvent();
    }

    public static void unInstall() {
        // 关闭监听器
        deleteOrderListener(artifactCache);

        // 卸载Adviser中的Adviser
        Adviser.unRegistry(commonMethodAdviser);

        // 先卸载全部的插件 done
        PluginManager.unInstallAll();

        // 关闭事件系统
        EventManager.shutdown();

        // 关闭所有服务
        ServiceManager.shutdown();

        // 清理操作类
        OperationManager.shutdown();

        // 清理配置类
        ConfigManager.shutdown();

        // 设置该artifact的Sermant状态为false，非运行状态
        SermantManager.updateSermantStatus(artifactCache,false);
    }

    private static void deleteOrderListener(String artifact) {
        if (!ConfigManager.getConfig(ServiceConfig.class).isDynamicConfigEnable()) {
            LOGGER.log(Level.WARNING, "Dynamic config service is disable, create order listener may not be created.");
            return;
        }
        DynamicConfigService dynamicConfigService = ServiceManager.getService(DynamicConfigService.class);
        String key = "agent/" + artifact;
        dynamicConfigService.removeConfigListener(key);
        dynamicConfigService.removeConfig(key);
    }

    // todo创建一个动态配置监听来管理
    private static void createOrderListener(String artifact) {
        if (!ConfigManager.getConfig(ServiceConfig.class).isDynamicConfigEnable()) {
            LOGGER.log(Level.WARNING,
                "Dynamic config service is disable, can not create order listener.Only operation" + " by java api.");
            return;
        }
        DynamicConfigService dynamicConfigService = ServiceManager.getService(DynamicConfigService.class);
        String key = "agent/" + artifact;
        dynamicConfigService.publishConfig(key, "ORDER INPUT");
        dynamicConfigService.addConfigListener(key, new DynamicConfigListener() {
            @Override
            public void process(DynamicConfigEvent event) {
                if (DynamicConfigEventType.MODIFY.equals(event.getEventType())) {
                    String content = event.getContent();
                    switch (content.split(":")[0]) {
                        case "UNINSTALL-AGENT": {
                            LOGGER.info(content + "...");
                            AgentCoreEntrance.unInstall();
                            break;
                        }
                        case "INSTALL-PLUGINS": {
                            LOGGER.info(content + "...");
                            HashSet<String> plugins = new HashSet<>(Arrays.asList(content.split(":")[1].split(",")));
                            PluginManager.install(plugins);
                            break;
                        }
                        case "UNINSTALL-PLUGINS": {
                            LOGGER.info(content + "...");
                            HashSet<String> plugins = new HashSet<>(Arrays.asList(content.split(":")[1].split(",")));
                            PluginManager.unInstall(plugins);
                            break;
                        }
                        default: {
                            LOGGER.info("CAN NOT RESOLVE COMMAND");
                        }
                    }
                }
            }
        });
    }

    public static String getArtifactCache() {
        return artifactCache;
    }
}
