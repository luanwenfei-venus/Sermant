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

import com.huaweicloud.sermant.core.plugin.Plugin;
import com.huaweicloud.sermant.core.plugin.agent.collector.PluginCollectorManager;

import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;

/**
 * 字节码增强管理器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-22
 */
public class ByteEnhanceManager {
    private static Instrumentation instrumentation;

    private static final BufferedAgentBuilder AGENT_BUILDER = BufferedAgentBuilder.build();

    private ByteEnhanceManager() {
    }

    public static void init(Instrumentation instrumentation) {
        ByteEnhanceManager.instrumentation = instrumentation;
    }

    /**
     * 增强字节码
     */
    public static void enhance() {
        AGENT_BUILDER.addClassLoaderEnhance()
            .install(instrumentation);
    }
    public static void enhanceBaseClass(){
        AGENT_BUILDER.addClassLoaderEnhance();
    }

    public static void enhanceStaticPlugin(Plugin plugin){
        AGENT_BUILDER.addPlugins(PluginCollectorManager.getPluginDescription(plugin));
    }

    public static void enhanceDynamicPlugin(Plugin plugin) {
        ResettableClassFileTransformer resettableClassFileTransformer = BufferedAgentBuilder.build()
            .addPlugins(PluginCollectorManager.getPluginDescription(plugin)).install(instrumentation);
        plugin.setResettableClassFileTransformer(resettableClassFileTransformer);
    }
}
