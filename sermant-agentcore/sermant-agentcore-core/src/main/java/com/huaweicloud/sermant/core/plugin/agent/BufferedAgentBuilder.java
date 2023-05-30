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

import com.huaweicloud.sermant.core.classloader.FrameworkClassLoader;
import com.huaweicloud.sermant.core.common.CommonConstant;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.event.collector.FrameworkEventCollector;
import com.huaweicloud.sermant.core.plugin.agent.config.AgentConfig;
import com.huaweicloud.sermant.core.plugin.agent.declarer.AbstractPluginDescription;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDescription;
import com.huaweicloud.sermant.core.plugin.agent.enhance.ClassLoaderDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.transformer.DefaultTransformer;
import com.huaweicloud.sermant.core.plugin.classloader.ServiceClassLoader;
import com.huaweicloud.sermant.core.utils.FileUtils;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Default;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassFileLocator.Resolution.Illegal;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.utility.JavaModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link AgentBuilder}的包装类提供一系列按配置进行的默认操作，并提供插件的增强操作
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-22
 */
public class BufferedAgentBuilder {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 增强后字节码输出路径的系统变量，优先级高于配置
     */
    private static final String OUTPUT_PATH_SYSTEM_KEY = "apm.agent.class.export.path";

    /**
     * 增强配置
     */
    private final AgentConfig config = ConfigManager.getConfig(AgentConfig.class);

    /**
     * 构建行为集
     */
    private final List<BuilderAction> actions = new ArrayList<>();

    private BufferedAgentBuilder() {}

    /**
     * 创建{@link BufferedAgentBuilder}并依据配置设置基础操作：
     * <pre>
     *     1.设置启动类加载器相关的增强策略，见{@link #setBootStrapStrategy}
     *     2.设置增强扫描过滤规则，见{@link #setIgnoredRule}
     *     3.设置增强时的扫描日志监听器，见{@link #setLogListener}
     *     4.设置输出增强后字节码的监听器，见{@link #setOutputListener}
     * </pre>
     *
     * @return BufferedAgentBuilder实例
     */
    public static BufferedAgentBuilder build() {
        return new BufferedAgentBuilder().setBootStrapStrategy()
                .setIgnoredRule()
                .setLogListener()
                .setOutputListener();
    }

    /**
     * 设置字节码增强的重定义策略，由{@link AgentConfig#isEnhanceBootStrapEnable()}而定
     * <pre>
     *     1.若不增强启动类加载器加载的类，则直接使用默认规则{@link AgentBuilder.RedefinitionStrategy#DISABLED}
     *     1.若增强启动类加载器加载的类，则使用规则{@link AgentBuilder.RedefinitionStrategy#RETRANSFORMATION}
     * </pre>
     *
     * @return BufferedAgentBuilder本身
     */
    private BufferedAgentBuilder setBootStrapStrategy() {
        if (!config.isEnhanceBootStrapEnable()) {
            return this;
        }
        return addAction(new BuilderAction() {
            @Override
            public AgentBuilder process(AgentBuilder builder) {
                return builder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
            }
        });
    }

    /**
     * 设置扫描的过滤规则
     * <p>注意，数组类型，8中基础类型，以及{@link ServiceClassLoader},{@link FrameworkClassLoader}加载的类默认不增强，直接被过滤
     * <p>其他类若符合配置中{@link AgentConfig#getIgnoredPrefixes}指定的前缀之一，则被过滤
     *
     * @return BufferedAgentBuilder本身
     */
    private BufferedAgentBuilder setIgnoredRule() {
        return addAction(new BuilderAction() {
            @Override
            public AgentBuilder process(AgentBuilder builder) {
                return builder.ignore(new IgnoredMatcher(config));
            }
        });
    }

    /**
     * 设置输出日志的监听器，由{@link AgentConfig#isShowEnhanceLog()}而定
     * <p>使用{@link AgentBuilder.Listener.StreamWriting}转化为字符串信息后输出为日志
     * <p>注意，输出时使用的缓冲区将不会被释放，需要关注{@link AgentBuilder.Listener.StreamWriting}中单行信息的长度
     *
     * @return BufferedAgentBuilder本身
     */
    private BufferedAgentBuilder setLogListener() {
        if (!config.isShowEnhanceLog()) {
            return this;
        }
        return addAction(new BuilderAction() {
            @Override
            public AgentBuilder process(AgentBuilder builder) {
                return builder
                    .with(new AgentBuilder.Listener.StreamWriting(new PrintStream(new ByteArrayOutputStream() {
                        private final byte[] separatorBytes =
                            System.lineSeparator().getBytes(CommonConstant.DEFAULT_CHARSET);

                        private final int separatorLength = separatorBytes.length;

                        @Override
                        public void flush() {
                            if (count < separatorLength) {
                                return;
                            }
                            for (int i = separatorLength - 1; i >= 0; i--) {
                                if (buf[count + i - separatorLength] != separatorBytes[i]) {
                                    return;
                                }
                            }
                            String enhanceLog = new String(Arrays.copyOf(buf, count - separatorLength));
                            logAndCollectEvent(enhanceLog);
                            reset();
                        }

                        private void logAndCollectEvent(String enhanceLog) {
                            if (enhanceLog.contains(CommonConstant.ERROR)) {
                                FrameworkEventCollector.getInstance().collectTransformFailureEvent(enhanceLog);
                                return;
                            }
                            if (enhanceLog.contains(CommonConstant.TRANSFORM)) {
                                FrameworkEventCollector.getInstance().collectTransformSuccessEvent(enhanceLog);
                            }
                            LOGGER.info(enhanceLog);
                        }
                    }, true)));
            }
        });
    }

    /**
     * 设置输出增强后字节码的监听器，输出路径优先选择：
     * <pre>
     *     1.系统变量{@link #OUTPUT_PATH_SYSTEM_KEY}
     *     2.配置{@link AgentConfig#getEnhancedClassesOutputPath}
     * </pre>
     * 若两者都无法正确获取路径，则视为无需该监听器
     *
     * @return BufferedAgentBuilder本身
     */
    private BufferedAgentBuilder setOutputListener() {
        if (!config.isOutputEnhancedClasses()) {
            return this;
        }

        String outputPath = config.getEnhancedClassesOutputPath();
        final Path outputDirectory;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String currentTime = LocalDateTime.now().format(formatter);
        if (outputPath == null || outputPath.isEmpty()) {
            outputDirectory = Paths.get(FileUtils.getAgentPath())
                .resolve(CommonConstant.ENHANCED_CLASS_OUTPUT_PARENT_DIR).resolve(currentTime);
        } else {
            outputDirectory =
                Paths.get(outputPath).resolve(CommonConstant.ENHANCED_CLASS_OUTPUT_PARENT_DIR).resolve(currentTime);
        }
        final File file;
        try {
            file = Files.createDirectories(outputDirectory).toFile();
        } catch (IOException e) {
            LOGGER.warning("Create enhanced class output directory fail!");
            return this;
        }
        return addAction(builder -> builder.with(new AgentBuilder.Listener.Adapter() {
            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                boolean loaded, DynamicType dynamicType) {
                try {
                    dynamicType.saveIn(file);
                } catch (IOException e) {
                    LOGGER.warning(String.format("Save class [%s] byte code failed. ", typeDescription.getTypeName()));
                }
            }
        }));
    }

    /**
     * 添加插件
     *
     * @param plugins 插件描述列表
     * @return BufferedAgentBuilder本身
     */
    public BufferedAgentBuilder addPlugins(Iterable<PluginDescription> plugins) {
        return addAction(new BuilderAction() {
            @Override
            public AgentBuilder process(AgentBuilder builder) {
                AgentBuilder newBuilder = builder;
                for (PluginDescription plugin : plugins) {
                    newBuilder = newBuilder.type(plugin).transform(plugin);
                }
                return newBuilder;
            }
        });
    }

    /**
     * 可针对指定类加载器进行增强，指定类优先通过Sermant的PluginClassLoader进行加载
     *
     * @return BufferedAgentBuilder本身
     */
    public BufferedAgentBuilder addClassLoaderEnhance() {
        return addAction(new BuilderAction() {
            @Override
            public AgentBuilder process(AgentBuilder builder) {
                PluginDescription pluginDescription = new AbstractPluginDescription() {
                    final ClassLoaderDeclarer classLoaderDeclarer = new ClassLoaderDeclarer();

                    @Override
                    public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription,
                        ClassLoader classLoader, JavaModule module) {
                        return new DefaultTransformer(classLoaderDeclarer.getInterceptDeclarers(classLoader))
                            .transform(builder, typeDescription, classLoader, module);
                    }

                    @Override
                    public boolean matches(TypeDescription target) {
                        try {
                            return classLoaderDeclarer.getClassMatcher().matches(target);
                        } catch (Exception exception) {
                            LOGGER.log(Level.WARNING,
                                    "Exception occur when math target: " + target.getActualName() + ",{0}",
                                exception.getMessage());
                            return false;
                        }
                    }
                };
                AgentBuilder newBuilder = builder;
                newBuilder = newBuilder.type(pluginDescription).transform(pluginDescription);
                return newBuilder;
            }
        });
    }

    /**
     * 添加行动
     *
     * @param action 行动
     * @return BufferedAgentBuilder本身
     */
    public BufferedAgentBuilder addAction(BuilderAction action) {
        actions.add(action);
        return this;
    }

    /**
     * 构建{@link AgentBuilder}，执行所有{@link BuilderAction}并执行{@link AgentBuilder#installOn(Instrumentation)}
     *
     * @param instrumentation Instrumentation对象
     * @return 安装结果，可重置的转换器，若无类元信息改动，调用其reset方法即可重置
     */
    public ResettableClassFileTransformer install(Instrumentation instrumentation) {
        AgentBuilder builder = new Default().disableClassFormatChanges();
        /**
         * todo：新建自定义的ClassFileLocator用于避免Can not resolve class的情况
         * builder = builder.with(
         * new Compound((classLoader, module) -> new UserDefineClassFileLocator(classLoader), ForClassLoader.STRONG));
         */
        for (BuilderAction action : actions) {
            builder = action.process(builder);
        }
        return builder.installOn(instrumentation);
    }

    /**
     * 忽略匹配器
     *
     * @author provenceee
     * @since 2022-11-17
     */
    private static class IgnoredMatcher implements AgentBuilder.RawMatcher {
        private final Set<String> ignoredPrefixes;

        private final Set<String> serviceInjectList;

        private final Set<String> ignoredInterfaces;

        IgnoredMatcher(AgentConfig config) {
            ignoredPrefixes = config.getIgnoredPrefixes();
            serviceInjectList = config.getServiceInjectList();
            ignoredInterfaces = config.getIgnoredInterfaces();
        }

        @Override
        public boolean matches(TypeDescription typeDesc, ClassLoader classLoader, JavaModule javaModule,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
            return isArrayOrPrimitive(typeDesc) || checkClassLoader(typeDesc, classLoader)
                || isIgnoredPrefixes(typeDesc) || isIgnoredInterfaces(typeDesc);
        }

        private boolean isArrayOrPrimitive(TypeDescription typeDesc) {
            return typeDesc.isArray() || typeDesc.isPrimitive();
        }

        private boolean checkClassLoader(TypeDescription typeDesc, ClassLoader classLoader) {
            // todo 此处可能还需要加一个，判断是否为PluginClassLoader
            if (classLoader instanceof FrameworkClassLoader) {
                return true;
            }
            if (classLoader instanceof ServiceClassLoader) {
                return !serviceInjectList.contains(typeDesc.getTypeName());
            }
            return false;
        }

        private boolean isIgnoredPrefixes(TypeDescription typeDesc) {
            if (ignoredPrefixes.isEmpty()) {
                return false;
            }
            for (String ignoredPrefix : ignoredPrefixes) {
                if (typeDesc.getTypeName().startsWith(ignoredPrefix)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isIgnoredInterfaces(TypeDescription typeDesc) {
            if (ignoredInterfaces == null || ignoredInterfaces.isEmpty()) {
                return false;
            }
            Generic interfaces = typeDesc.getInterfaces();
            if (interfaces == null || interfaces.isEmpty()) {
                return false;
            }
            for (TypeDescription.Generic interfaceClass : interfaces) {
                if (ignoredInterfaces.contains(interfaceClass.getTypeName())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 构建行为
     *
     * @since 2022-01-22
     */
    public interface BuilderAction {
        /**
         * 执行构建行为
         *
         * @param builder 构建器
         * @return 构建器
         */
        AgentBuilder process(AgentBuilder builder);
    }

    static class UserDefineClassFileLocator implements ClassFileLocator {
        private final ClassLoader classLoader;

        protected UserDefineClassFileLocator(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Resolution locate(String name) throws IOException {
            try {
                if (classLoader == null) {
                    return ForClassLoader.of(null).locate(name);
                }
                ClassLoader realLoader = classLoader.loadClass(name).getClassLoader();
                return ForClassLoader.of(realLoader).locate(name);
            } catch (ClassNotFoundException exception) {
                LOGGER.log(Level.WARNING, "Cannot load class {0}, by classloader {1}",
                    new Object[] {name, classLoader});
            }
            return new Illegal(name);
        }

        @Override
        public void close() throws IOException {

        }
    }
}