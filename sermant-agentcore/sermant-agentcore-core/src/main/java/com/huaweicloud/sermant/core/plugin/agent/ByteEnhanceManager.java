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

import com.huaweicloud.sermant.core.plugin.agent.collector.PluginCollectorManager;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDescription;

import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;

/**
 * 字节码增强管理器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-22
 */
public class ByteEnhanceManager {
    private static ResettableClassFileTransformer resettableClassFileTransformer;

    private static Instrumentation instrumentation;

    private static List<PluginDescription> pluginDescriptions;

    private ByteEnhanceManager() {
    }

    /**
     * 增强字节码
     *
     * @param instrumentation Instrumentation对象
     */
    public static void enhance(Instrumentation instrumentation) {
        ByteEnhanceManager.instrumentation = instrumentation;
        pluginDescriptions = PluginCollectorManager.getPlugins();
        resettableClassFileTransformer =
                BufferedAgentBuilder.build().addPlugins(pluginDescriptions).install(instrumentation);
    }

    public static void unEnhance(String className) {
        System.out.println(
                resettableClassFileTransformer.reset(instrumentation, RedefinitionStrategy.RETRANSFORMATION));
        try {
            for (Class clazz : instrumentation.getAllLoadedClasses()) {
                if (className.equals(clazz.getCanonicalName())) {
                    System.out.println(clazz.getCanonicalName());
                    ByteEnhanceManager.instrumentation.retransformClasses(clazz);
                }
            }
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reEnhance() {
        BufferedAgentBuilder agentBuilder =
                BufferedAgentBuilder.build().addPlugins(pluginDescriptions);
        resettableClassFileTransformer =
                agentBuilder.install(instrumentation);
    }
}
