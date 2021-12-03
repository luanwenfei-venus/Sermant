/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.gray.feign.definition.register;

import com.huawei.javamesh.core.agent.definition.EnhanceDefinition;
import com.huawei.javamesh.core.agent.definition.MethodInterceptPoint;
import com.huawei.javamesh.core.agent.matcher.ClassMatcher;
import com.huawei.javamesh.core.agent.matcher.ClassMatchers;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 拦截MicroserviceFactory 创建微服务的方法，获取当前服务名
 *
 * @author lilai
 * @since 2021-11-03
 */
public class ServiceCenterRegisterDefinition implements EnhanceDefinition {

    /**
     * Intercept class.
     */
    private static final String INTERCEPT_CLASS = "com.huawei.gray.feign.interceptor.ServiceCenterRegisterInterceptor";

    @Override
    public ClassMatcher enhanceClass() {
        return ClassMatchers.named("org.apache.servicecomb.registry.api.registry.MicroserviceFactory");
    }

    @Override
    public MethodInterceptPoint[] getMethodInterceptPoints() {
        return new MethodInterceptPoint[]{
                MethodInterceptPoint.newInstMethodInterceptPoint(
                        INTERCEPT_CLASS, ElementMatchers.<MethodDescription>named("createMicroserviceFromConfiguration")
                )
        };
    }
}
