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

package com.huaweicloud.sermant.implement.service.heartbeat;

import com.huaweicloud.sermant.core.common.CommonConstant;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.plugin.common.PluginConstant;
import com.huaweicloud.sermant.core.plugin.common.PluginSchemaValidator;
import com.huaweicloud.sermant.core.service.heartbeat.api.ExtInfoProvider;
import com.huaweicloud.sermant.core.service.heartbeat.api.HeartbeatService;
import com.huaweicloud.sermant.core.service.heartbeat.common.HeartbeatConstant;
import com.huaweicloud.sermant.core.service.heartbeat.common.HeartbeatMessage;
import com.huaweicloud.sermant.core.service.heartbeat.common.PluginInfo;
import com.huaweicloud.sermant.core.service.heartbeat.config.HeartbeatConfig;
import com.huaweicloud.sermant.core.utils.JarFileUtils;
import com.huaweicloud.sermant.core.utils.ThreadFactoryUtils;
import com.huaweicloud.sermant.implement.service.send.netty.NettyClient;
import com.huaweicloud.sermant.implement.service.send.netty.NettyClientFactory;
import com.huaweicloud.sermant.implement.service.send.netty.pojo.Message;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * {@link HeartbeatService}的实现
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-10-25
 */
public class HeartbeatServiceImpl implements HeartbeatService {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 心跳额外参数
     */
    private static final Map<String, ExtInfoProvider> EXT_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * 心跳定时任务
     */
    private static ScheduledExecutorService EXECUTOR_SERVICE;

    /**
     * NettyClient
     */
    private NettyClient nettyClient;

    // 初始化心跳数据常量
    private HeartbeatMessage heartbeatMessage;

    @Override
    public void start() {
        heartbeatMessage = new HeartbeatMessage();
        nettyClient = NettyClientFactory.getInstance().getDefaultNettyClient();
        EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1, new ThreadFactoryUtils("heartbeat-task"));
        EXECUTOR_SERVICE.scheduleAtFixedRate(this::execute, 0,
                Math.max(ConfigManager.getConfig(HeartbeatConfig.class).getInterval(),
                        HeartbeatConstant.HEARTBEAT_MINIMAL_INTERVAL),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if(EXECUTOR_SERVICE != null){
            EXECUTOR_SERVICE.shutdown();
        }
        if (nettyClient != null && !nettyClient.isStop()) {
            nettyClient.stop();
        }
    }

    /**
     * 执行循环
     */
    private void execute() {
        // 获取插件名和版本集合
        final Map<String, String> pluginVersionMap = PluginSchemaValidator.getPluginVersionMap();
        for (Map.Entry<String, String> entry : pluginVersionMap.entrySet()) {
            heartbeatMessage.getPluginInfoMap().putIfAbsent(entry.getKey(),
                    new PluginInfo(entry.getKey(), entry.getValue()));
            addExtInfo(entry.getKey(), heartbeatMessage.getPluginInfoMap().get(entry.getKey()));
        }
        heartbeatMessage.updateHeartbeatVersion();
        if (nettyClient == null) {
            LOGGER.warning("Netty client is null when send heartbeat message.");
            return;
        }
        nettyClient.sendInstantData(JSONObject.toJSONString(heartbeatMessage).getBytes(CommonConstant.DEFAULT_CHARSET),
                Message.ServiceData.DataType.HEARTBEAT_DATA);
    }

    /**
     * 添加心跳额外信息
     *
     * @param pluginName 插件名称
     * @param pluginInfo 插件信息
     */
    private void addExtInfo(String pluginName, PluginInfo pluginInfo) {
        final ExtInfoProvider provider = EXT_INFO_MAP.get(pluginName);
        if (provider == null) {
            return;
        }
        final Map<String, String> extInfo = provider.getExtInfo();
        if (extInfo != null) {
            pluginInfo.setExtInfo(extInfo);
        }
    }

    @Override
    public void setExtInfo(ExtInfoProvider extInfo) {
        final String pluginJar = JarFileUtils.getJarUrl(extInfo.getClass()).getPath();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(pluginJar);
            final Object nameAttr = JarFileUtils.getManifestAttr(jarFile, PluginConstant.PLUGIN_NAME_KEY);
            if (nameAttr != null) {
                EXT_INFO_MAP.put(nameAttr.toString(), extInfo);
            } else {
                LOGGER.warning(String.format(Locale.ROOT, "Get plugin name of %s failed. ", pluginJar));
            }
        } catch (IOException ignored) {
            LOGGER.warning(String.format(Locale.ROOT, "Cannot find manifest file of %s. ", pluginJar));
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ignored) {
                    LOGGER.warning("Unexpected exception occurs. ");
                }
            }
        }
    }
}