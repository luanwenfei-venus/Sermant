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

package com.huaweicloud.sermant.core.plugin.agent.collector;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.Plugin;
import com.huaweicloud.sermant.core.plugin.agent.BufferedAgentBuilder;
import com.huaweicloud.sermant.core.plugin.agent.declarer.AbstractPluginDescription;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDescription;
import com.huaweicloud.sermant.core.plugin.agent.transformer.DefaultTransformer;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 插件收集器管理器，用于从所有插件收集器中获取插件描述器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-26
 */
public class PluginCollectorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 通过spi检索所有配置的插件收集器
     */
    private static final Iterable<PluginCollector> COLLECTORS = ServiceLoader.load(PluginCollector.class,
            PluginCollectorManager.class.getClassLoader());

    private PluginCollectorManager() {
    }

    public static List<PluginDescription> getPluginDescription(Plugin plugin){
        return new ArrayList<>(describeDeclarers(getDeclarers(plugin.getPluginClassLoader())));
    }

    public static List<PluginDescription> getPluginDescription(Plugin plugin, BufferedAgentBuilder builder){
        return new ArrayList<>(describeDeclarers(getDeclarers(plugin.getPluginClassLoader()),builder));
    }

    /**
     * 从插件收集器中获取所有插件声明器
     *
     * @return 插件声明器集
     */
    private static List<? extends PluginDeclarer> getDeclarers(ClassLoader classLoader) {
        final List<PluginDeclarer> declares = new ArrayList<>();
        for (PluginCollector collector : COLLECTORS) {
            for (PluginDeclarer declarer : collector.getDeclarers(classLoader)) {
                if (declarer.isEnabled()) {
                    declares.add(declarer);
                }
            }
        }
        return declares;
    }

    /**
     * 直接将所有插件声明器描述为插件描述器
     *
     * @param declarers 插件声明器集
     * @return 插件描述器
     */
    private static List<PluginDescription> describeDeclarers(Iterable<? extends PluginDeclarer> declarers) {
        final List<PluginDescription> plugins = new ArrayList<>();
        for (PluginDeclarer pluginDeclarer : declarers) {
            plugins.add(describeDeclarer(pluginDeclarer));
        }
        return plugins;
    }

    private static List<PluginDescription> describeDeclarers(Iterable<? extends PluginDeclarer> declarers,
            BufferedAgentBuilder builder) {
        final List<PluginDescription> plugins = new ArrayList<>();
        for (PluginDeclarer pluginDeclarer : declarers) {
            plugins.add(describeDeclarer(pluginDeclarer,builder));
        }
        return plugins;
    }

    /**
     * 将一个插件声明器描述为插件描述器
     *
     * @param declarer 插件声明器
     * @return 插件描述器
     */
    private static PluginDescription describeDeclarer(PluginDeclarer declarer) {
        return new AbstractPluginDescription() {
            @Override
            public boolean matches(TypeDescription target) {
                return matchTarget(declarer.getClassMatcher(), target);
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                ClassLoader classLoader, JavaModule module) {
                // todo ClassLoader.getSystemClassLoader() 已经没有作用
                return new DefaultTransformer(declarer.getInterceptDeclarers(ClassLoader.getSystemClassLoader()))
                    .transform(builder, typeDescription, classLoader, module);
            }
        };
    }

    private static PluginDescription describeDeclarer(PluginDeclarer declarer,BufferedAgentBuilder agentBuilder) {
        return new AbstractPluginDescription() {
            @Override
            public boolean matches(TypeDescription target) {
                return matchTarget(declarer.getClassMatcher(), target);
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                ClassLoader classLoader, JavaModule module) {
                // todo ClassLoader.getSystemClassLoader() 已经没有作用
                return new DefaultTransformer(declarer   ,
                    agentBuilder).transform(builder, typeDescription, classLoader, module);
            }
        };
    }

    private static boolean matchTarget(ElementMatcher<TypeDescription> matcher, TypeDescription target) {
        try {
            return matcher.matches(target);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Exception occur when math target: " + target.getActualName() + ",{0}",
                exception.getMessage());
            return false;
        }
    }
}
