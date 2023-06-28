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

package com.huaweicloud.sermant.core.plugin.agent.declarer;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.agent.matcher.MethodMatcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 拦截声明器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-27
 */
public abstract class InterceptDeclarer {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 构建拦截声明器
     *
     * @param methodMatcher 方法匹配器
     * @param interceptors 拦截器集
     * @return 拦截声明器
     * @throws IllegalArgumentException IllegalArgumentException
     */
    public static InterceptDeclarer build(MethodMatcher methodMatcher, Interceptor... interceptors) {
        if (methodMatcher == null || interceptors == null || interceptors.length <= 0) {
            throw new IllegalArgumentException("Matcher cannot be null and interceptor array cannot be empty. ");
        }
        return new InterceptDeclarer() {
            @Override
            public MethodMatcher getMethodMatcher() {
                return methodMatcher;
            }

            @Override
            public Interceptor[] getInterceptors(ClassLoader classLoader) {
                return interceptors;
            }
        };
    }

    /**
     * 构建拦截声明器，该api将使用
     *
     * @param methodMatcher 方法匹配器
     * @param interceptors  拦截器集
     * @return 拦截声明器
     * @throws IllegalArgumentException IllegalArgumentException
     */
    public static InterceptDeclarer build(MethodMatcher methodMatcher, String... interceptors) {
        if (methodMatcher == null || interceptors == null || interceptors.length <= 0) {
            throw new IllegalArgumentException("Matcher cannot be null and interceptor array cannot be empty. ");
        }
        return new InterceptDeclarer() {
            @Override
            public MethodMatcher getMethodMatcher() {
                return methodMatcher;
            }

            @Override
            public Interceptor[] getInterceptors(ClassLoader classLoader) {
                try {
                    return createInterceptors(interceptors, classLoader);
                } catch (IOException | IllegalAccessException | NoSuchMethodException | InvocationTargetException
                    | ClassNotFoundException | InstantiationException e) {
                    LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                        "Unable to create instance of interceptors:  [%s]. ", Arrays.toString(interceptors)), e);
                }
                return new Interceptor[0];
            }
        };
    }

    /**
     * 使用被增强类的类加载器创建所有拦截器对象
     *
     * @param interceptors 拦截器全限定名集
     * @param classLoader  被增强类的类加载器
     * @return 拦截器集
     * @throws ClassNotFoundException    找不到类
     * @throws IOException               定义类失败
     * @throws InvocationTargetException 调用addURL方法失败或调用defineClass方法失败
     * @throws NoSuchMethodException     找不到addURL方法或defineClass方法
     * @throws IllegalAccessException    无法访问addURL方法或defineClass方法
     * @throws InstantiationException    实例化失败
     */
    private static Interceptor[] createInterceptors(String[] interceptors, ClassLoader classLoader)
        throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
        IllegalAccessException, InstantiationException {
        final ArrayList<Interceptor> interceptorList = new ArrayList<>();
        if (!ClassLoaderManager.getPluginClassLoader().equals(classLoader)) {
            // 如果不是PlugClassLoader 则放入类加载器缓存
            ClassLoaderManager.getPluginClassLoader().appendClassLoader(classLoader);
        }

        for (String interceptor : interceptors) {
            // final Object instance = getInterceptorClass(interceptor, classLoader).newInstance();
            final Object instance = ClassLoaderManager.getPluginClassLoader().loadClass(interceptor).newInstance();
            if (instance instanceof Interceptor) {
                interceptorList.add((Interceptor)instance);
            }
        }
        return interceptorList.toArray(new Interceptor[0]);
    }

    /**
     * 获取方法匹配器
     *
     * @return 方法匹配器
     */
    public abstract MethodMatcher getMethodMatcher();

    /**
     * 获取拦截器集
     *
     * @param classLoader 被增强类的类加载器
     * @return 拦截器集
     */
    public abstract Interceptor[] getInterceptors(ClassLoader classLoader);
}
