/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.plugin.agent;

import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.plugin.Plugin;
import com.huaweicloud.sermant.core.plugin.agent.collector.PluginCollectorManager;
import com.huaweicloud.sermant.core.plugin.agent.config.AgentConfig;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDescription;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * 字节码增强管理器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-22
 */
public class ByteEnhanceManager {
    private static Instrumentation instrumentation;

    private static BufferedAgentBuilder AGENT_BUILDER;

    private ByteEnhanceManager() {
    }

    public static void init(Instrumentation instrumentation) {
        ByteEnhanceManager.instrumentation = instrumentation;
        AGENT_BUILDER = BufferedAgentBuilder.build();
        AGENT_BUILDER.setPlugin(new Plugin(null, null, false, null));

    }

    /**
     * 安装增强字节码，仅用于premain方式启动
     */
    public static void enhance() {
        enhanceBaseClass();
        AGENT_BUILDER.install(instrumentation);
    }

    /**
     * 引入一些对基础类的字节码增强增强
     */
    public static void enhanceBaseClass() {
        if (ConfigManager.getConfig(AgentConfig.class).isEnhanceClassLoader()) {
            AGENT_BUILDER.addClassLoaderEnhance();
        }
    }

    /**
     * 基于支持静态安装的插件进行字节码增强
     *
     * @param plugin 支持静态安装的插件
     */
    public static void enhanceStaticPlugin(Plugin plugin) {
        AGENT_BUILDER.addPlugins(PluginCollectorManager.getPluginDescription(plugin, AGENT_BUILDER));
    }

    /**
     * 基于支持动态安装的插件进行字节码增强
     *
     * @param plugin 支持动态安装的插件
     */
    public static void enhanceDynamicPlugin(Plugin plugin) {
        if (!plugin.isDynamicSupport()) {
            return;
        }
        BufferedAgentBuilder builder = BufferedAgentBuilder.build();
        builder.setPlugin(plugin);
        List<PluginDescription> plugins = PluginCollectorManager.getPluginDescription(plugin,builder);
        ResettableClassFileTransformer resettableClassFileTransformer = builder
                .addPlugins(plugins).install(instrumentation);
        plugin.setResettableClassFileTransformer(resettableClassFileTransformer);
    }

    /**
     * 取消支持动态安装的插件的字节码增强
     * @param plugin 支持动态安装的插件
     */
    public static void unEnhanceDynamicPlugin(Plugin plugin) {
        if (!plugin.isDynamicSupport()) {
            return;
        }
        plugin.getResettableClassFileTransformer().reset(instrumentation, RedefinitionStrategy.RETRANSFORMATION,
                Reiterating.INSTANCE, ForTotal.INSTANCE,
                AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemOut());
    }
}
