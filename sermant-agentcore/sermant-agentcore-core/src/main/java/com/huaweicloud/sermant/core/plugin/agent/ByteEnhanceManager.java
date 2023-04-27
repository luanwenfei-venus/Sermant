/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.huaweicloud.sermant.core.plugin.agent;

import com.huaweicloud.sermant.core.plugin.agent.collector.PluginCollectorManager;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating;
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
    private static ResettableClassFileTransformer transformerCache;

    private static Instrumentation instrumentationCache;

    private ByteEnhanceManager() {}

    /**
     * 增强字节码
     *
     * @param instrumentation Instrumentation对象
     */
    public static void enhance(Instrumentation instrumentation) {
        instrumentationCache = instrumentation;
        transformerCache =
            BufferedAgentBuilder.build().addPlugins(PluginCollectorManager.getPlugins()).install(instrumentation);
    }

    public static boolean unEnhance() {
        return transformerCache.reset(instrumentationCache, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
            Reiterating.INSTANCE, ForTotal.INSTANCE,
            AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemOut());
    }

    public static void clear() {
        transformerCache = null;
        instrumentationCache = null;
    }
}
