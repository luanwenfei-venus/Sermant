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

package com.huaweicloud.sermant.core.plugin.agent.transformer;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.Plugin;
import com.huaweicloud.sermant.core.plugin.agent.BufferedAgentBuilder;
import com.huaweicloud.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.agent.template.CommonBaseAdviser;
import com.huaweicloud.sermant.core.plugin.agent.template.DefaultConstTemplate;
import com.huaweicloud.sermant.core.plugin.agent.template.DefaultMemberTemplate;
import com.huaweicloud.sermant.core.plugin.agent.template.DefaultStaticTemplate;
import com.huaweicloud.sermant.core.plugin.agent.template.MethodKeyCreator;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * 启动类加载器加载类的Transformer，advice风格
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-10-27
 */
public class DefaultTransformer implements AgentBuilder.Transformer {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 创建该DefaultTransformer 的BufferAgentBuilder
     */
    private BufferedAgentBuilder agentBuilder;

    /**
     * 创建该DefaultTransformer 的PluginDeclarer
     */
    private PluginDeclarer pluginDeclarer;

    /**
     * 拦截定义数组
     */
    private final InterceptDeclarer[] interceptDeclarers;

    /**
     * 构造方法
     *
     * @param interceptDeclarers interceptDeclarers
     */
    public DefaultTransformer(InterceptDeclarer[] interceptDeclarers) {
        this.interceptDeclarers = interceptDeclarers;
    }

    public DefaultTransformer(PluginDeclarer pluginDeclarer, BufferedAgentBuilder builder) {
        this.agentBuilder = builder;
        this.pluginDeclarer =pluginDeclarer;
        this.interceptDeclarers = pluginDeclarer.getInterceptDeclarers(ClassLoader.getSystemClassLoader());
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDesc,
            ClassLoader classLoader, JavaModule module) {
        if (interceptDeclarers == null || interceptDeclarers.length == 0) {
            return builder;
        }

        // todo: 新的类隔离架构将不再依赖于被增强类的类加载器，这里可以选择合并
//        // 引导类加载器的时候为null
//        if (classLoader == null) {
//            return enhanceMethods(builder, typeDesc, ClassLoaderManager.getPluginClassLoader());
//        }
        return enhanceMethods(builder, typeDesc, classLoader);
    }

    /**
     * 检查类定义的所有方法，并尝试增强，见{@link #enhanceMethod}
     * <p>注意，native方法，抽象方法，及父类定义的方法不会被检查
     *
     * @param builder 构建器
     * @param typeDesc 类定义
     * @param classLoader 加载被增强类的类加载器
     * @return 构建器
     */
    private DynamicType.Builder<?> enhanceMethods(DynamicType.Builder<?> builder, TypeDescription typeDesc,
            ClassLoader classLoader) {
        final MethodList<MethodDescription.InDefinedShape> declaredMethods = typeDesc.getDeclaredMethods();
        DynamicType.Builder<?> newBuilder = builder;
        for (MethodDescription.InDefinedShape methodDesc : declaredMethods) {
            if (methodDesc.isNative() || methodDesc.isAbstract()) {
                continue;
            }
            newBuilder = enhanceMethod(newBuilder, methodDesc, classLoader);
        }
        return newBuilder;
    }

    /**
     * 对单个方法进行增强
     *
     * @param builder 构建器
     * @param methodDesc 方法定义
     * @param classLoader 加载被增强类的类加载器
     * @return 构建器
     */
    private DynamicType.Builder<?> enhanceMethod(DynamicType.Builder<?> builder,
            MethodDescription.InDefinedShape methodDesc, ClassLoader classLoader) {
        final List<Interceptor> interceptors = getInterceptors(methodDesc, classLoader);
        if (interceptors.isEmpty()) {
            return builder;
        }
        try {
            if (methodDesc.isStatic()) {
                return resolve(builder, methodDesc, interceptors, DefaultStaticTemplate.class);
            } else if (methodDesc.isConstructor()) {
                return resolve(builder, methodDesc, interceptors, DefaultConstTemplate.class);
            } else {
                return resolve(builder, methodDesc, interceptors, DefaultMemberTemplate.class);
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warning(String.format(Locale.ROOT, "Enhance [%s] failed for [%s], caused by [%s]. ",
                    MethodKeyCreator.getMethodDescKey(methodDesc), e.getClass().getName(), e.getMessage()));
        }
        return builder;
    }

    /**
     * 获取单个方法有关的拦截器列表
     *
     * @param methodDesc 方法定义
     * @param classLoader 类加载器
     * @return 拦截器列表
     */
    private List<Interceptor> getInterceptors(MethodDescription.InDefinedShape methodDesc, ClassLoader classLoader) {
        final List<Interceptor> interceptors = new ArrayList<>();
        for (InterceptDeclarer declarer : interceptDeclarers) {
            if (!declarer.getMethodMatcher().matches(methodDesc)) {
                continue;
            }
            interceptors.addAll(Arrays.asList(declarer.getInterceptors(classLoader)));
        }
        return interceptors;
    }

    /**
     * 处理方法增强
     * <pre>
     *     1.依模板类创建增强Adviser
     *     2.使用被增强类的类加载器定义该Adviser
     *     3.为该Adviser添加增强的拦截器
     *     4.在构建器中定义增强逻辑
     * </pre>
     *
     * @param builder 构建器
     * @param methodDesc 方法定义
     * @param interceptors 拦截器列表
     * @param templateCls 增强模板类
     * @return 构建器
     * @throws InvocationTargetException 调用方法错误
     * @throws IllegalAccessException 无法访问属性或方法，正常不会报出
     * @throws NoSuchMethodException 无法找到方法，正常不会报出
     * @throws NoSuchFieldException 找不到属性
     */
    private DynamicType.Builder<?> resolve(DynamicType.Builder<?> builder, MethodDescription.InDefinedShape methodDesc,
        List<Interceptor> interceptors, Class<?> templateCls)
        throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        final String adviceClassName = getAdviceClassName(templateCls, methodDesc);
        List<Interceptor> globalInterceptors = CommonBaseAdviser.getInterceptorListMap().get(adviceClassName);
        Boolean adviceFlag = BufferedAgentBuilder.getAdviceFlagMap().get(adviceClassName);
        Plugin plugin = agentBuilder.getPlugin();
        if (Boolean.TRUE.equals(adviceFlag)) {
            if (globalInterceptors == null) {
                CommonBaseAdviser.getInterceptorListMap().put(adviceClassName, new ArrayList<>());
                globalInterceptors = CommonBaseAdviser.getInterceptorListMap().get(adviceClassName);
            }
            for (Interceptor interceptor : interceptors) {
                if (plugin.checkInterceptor(adviceClassName, interceptor.getClass().getCanonicalName())) {
                    globalInterceptors.add(interceptor);
                }
            }
            // 检查锁是否归自己持有
            if(agentBuilder.getPlugin().getAdviceClassLockSet(pluginDeclarer).contains(adviceClassName)){
                return builder.visit(Advice.to(templateCls).on(ElementMatchers.is(methodDesc)));
            }else {
                return builder;
            }
        } else {
            // 获得锁
            agentBuilder.getPlugin().getAdviceClassLockSet(pluginDeclarer).add(adviceClassName);
            if (globalInterceptors == null) {
                CommonBaseAdviser.getInterceptorListMap().put(adviceClassName, new ArrayList<>());
                globalInterceptors = CommonBaseAdviser.getInterceptorListMap().get(adviceClassName);
            }
            for (Interceptor interceptor : interceptors) {
                if (plugin.checkInterceptor(adviceClassName, interceptor.getClass().getCanonicalName())) {
                    globalInterceptors.add(interceptor);
                }
            }
            BufferedAgentBuilder.getAdviceFlagMap().put(adviceClassName, true);
            return builder.visit(Advice.to(templateCls).on(ElementMatchers.is(methodDesc)));
        }
    }

    /**
     * 获取增强Adviser的全限定名
     * <p>由模板类全限定名拼接被增强方法元信息的hash值所得，见于{@link MethodKeyCreator#getMethodDescKey}
     *
     * @param templateCls 增强模板类
     * @return 增强Adviser全限定名
     */
    private String getAdviceClassName(Class<?> templateCls, MethodDescription.InDefinedShape methodDesc) {
        return templateCls.getName() + "_"
                + Integer.toHexString(MethodKeyCreator.getMethodDescKey(methodDesc).hashCode());
    }
}