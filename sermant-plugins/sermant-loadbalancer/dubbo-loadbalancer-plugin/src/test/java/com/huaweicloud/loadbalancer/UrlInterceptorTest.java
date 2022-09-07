/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huaweicloud.loadbalancer;

import com.huaweicloud.loadbalancer.cache.DubboLoadbalancerCache;
import com.huaweicloud.loadbalancer.cache.RuleManagerHelper;
import com.huaweicloud.loadbalancer.cache.YamlRuleConverter;
import com.huaweicloud.loadbalancer.config.DubboLoadbalancerType;
import com.huaweicloud.loadbalancer.config.LoadbalancerConfig;
import com.huaweicloud.loadbalancer.interceptor.UrlInterceptor;
import com.huaweicloud.loadbalancer.service.RuleConverter;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.utils.ClassUtils;
import com.huaweicloud.sermant.core.utils.ReflectUtils;

import org.apache.dubbo.common.URL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 测试URL getMethodParameter方法的拦截点
 *
 * @author provenceee
 * @see com.alibaba.dubbo.common.URL
 * @see org.apache.dubbo.common.URL
 * @since 2022-03-01
 */
public class UrlInterceptorTest {
    private static final String SERVICE_NAME = "test";

    private MockedStatic<ServiceManager> serviceManagerMockedStatic;

    private MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    /**
     * 配置转换器
     */
    @Before
    public void setUp() {
        serviceManagerMockedStatic = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMockedStatic.when(() -> ServiceManager.getService(RuleConverter.class))
                .thenReturn(new YamlRuleConverter());
        pluginConfigManagerMockedStatic = Mockito.mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(LoadbalancerConfig.class))
                .thenReturn(new LoadbalancerConfig());
    }

    @After
    public void close() {
        serviceManagerMockedStatic.close();
        pluginConfigManagerMockedStatic.close();
    }

    /**
     * 测试不合法的参数
     */
    @Test
    public void testInvalidArguments() throws NoSuchMethodException {
        ExecuteContext context = buildContext(null);
        final UrlInterceptor interceptor = new UrlInterceptor();
        // 测试arguments为null
        interceptor.before(context);
        Assert.assertFalse(context.isSkip());

        // 测试参数数组大小小于2
        context = buildContext(new Object[1]);
        interceptor.before(context);
        Assert.assertFalse(context.isSkip());

        // 测试参数数组大小大于1
        Object[] arguments = new Object[2];
        context = buildContext(arguments);

        // 测试arguments[1]为null
        interceptor.before(context);
        Assert.assertFalse(context.isSkip());

        // 测试arguments[1]为bar
        arguments[1] = "bar";
        interceptor.before(context);
        Assert.assertFalse(context.isSkip());
    }

    /**
     * 测试加载核对规则
     */
    @Test
    public void testCheckRules() {
        // 测试Apache分支
        final UrlInterceptor interceptor = new UrlInterceptor();
        ReflectUtils.invokeMethod(interceptor, "checkRules", null, null);
        final Optional<Object> supportRules = ReflectUtils.getFieldValue(interceptor, "supportRules");
        Assert.assertTrue(supportRules.isPresent() && supportRules.get() instanceof Set);
        Assert.assertFalse(((Set<?>) supportRules.get()).isEmpty());

        // 测试alibaba分支
        try (final MockedStatic<ClassUtils> classUtilsMockedStatic = Mockito.mockStatic(ClassUtils.class)){
            classUtilsMockedStatic.when(() -> ClassUtils.loadClass("com.alibaba.dubbo.common.extension.ExtensionLoader",
                    Thread.currentThread().getContextClassLoader(), false))
                    .thenReturn(Optional.of(new Object()));
            final UrlInterceptor alibabaInterceptor = new UrlInterceptor();
            ReflectUtils.invokeMethod(alibabaInterceptor, "checkRules", null, null);
            final Optional<Object> alibabaSupportRules = ReflectUtils.getFieldValue(alibabaInterceptor, "supportRules");
            Assert.assertTrue(alibabaSupportRules.isPresent() && alibabaSupportRules.get() instanceof Set);
        }
    }

    /**
     * 测试规则支持
     */
    @Test
    public void testSupport() {
        final UrlInterceptor interceptor = new UrlInterceptor();
        ReflectUtils.setFieldValue(interceptor, "supportRules", new HashSet<>());
        final Optional<Object> isSupport = ReflectUtils.invokeMethod(interceptor, "isSupport", new Class[] {String.class},
                new Object[] {DubboLoadbalancerType.RANDOM.name().toLowerCase(Locale.ROOT)});
        Assert.assertTrue(isSupport.isPresent() && isSupport.get() instanceof Boolean);
        Assert.assertTrue((Boolean) isSupport.get());

        // 测试已加载相关规则
        final UrlInterceptor loadedInterceptor = new UrlInterceptor();
        ReflectUtils.invokeMethod(loadedInterceptor, "checkRules", null, null);
        final Optional<Object> isSupportForInit = ReflectUtils.invokeMethod(loadedInterceptor, "isSupport", new Class[] {String.class},
                new Object[] {DubboLoadbalancerType.RANDOM.name().toLowerCase(Locale.ROOT)});
        Assert.assertTrue(isSupportForInit.isPresent() && isSupportForInit.get() instanceof Boolean);
        Assert.assertTrue((Boolean) isSupportForInit.get());

        // 测试不支持
        final Optional<Object> notSupport = ReflectUtils.invokeMethod(loadedInterceptor, "isSupport", new Class[] {String.class},
                new Object[] {"test"});
        Assert.assertTrue(notSupport.isPresent() && notSupport.get() instanceof Boolean);
        Assert.assertFalse((Boolean) notSupport.get());
    }

    /**
     * 测试合法的参数
     */
    @Test
    public void test() throws NoSuchMethodException {
        // 测试参数数组大小大于1
        Object[] arguments = new Object[2];
        ExecuteContext context = buildContext(arguments);
        arguments[1] = "loadbalance";

        // 测试配置为null
        UrlInterceptor nullConfigInterceptor = new UrlInterceptor();
        nullConfigInterceptor.before(context);
        Assert.assertFalse(context.isSkip());
        Assert.assertNull(context.getResult());

        final UrlInterceptor interceptor = new UrlInterceptor();
        configSupports(interceptor);

        // 测试负载均衡策略为null
        interceptor.before(context);
        Assert.assertFalse(context.isSkip());
        Assert.assertNull(context.getResult());

        // 测试正常情况
        final DubboLoadbalancerCache instance = DubboLoadbalancerCache.INSTANCE;
        RuleManagerHelper.publishRule(SERVICE_NAME, DubboLoadbalancerType.SHORTESTRESPONSE.getMapperName());
        interceptor.before(context);
        Assert.assertNotNull(instance.getNewCache().get(SERVICE_NAME));
        Assert.assertTrue(context.isSkip());
    }

    private void configSupports(UrlInterceptor interceptor) {
        final HashSet<String> supportRules = new HashSet<>();
        for (DubboLoadbalancerType type : DubboLoadbalancerType.values()) {
            supportRules.add(type.name().toLowerCase(Locale.ROOT));
        }
        ReflectUtils.setFieldValue(interceptor, "supportRules", supportRules);
    }

    private ExecuteContext buildContext(Object[] arguments) throws NoSuchMethodException {
        final URL url = new URL("dubbo", "localhost", 8080, null, "remote.application", SERVICE_NAME);
        return ExecuteContext.forMemberMethod(url, String.class.getDeclaredMethod("trim"), arguments,
                Collections.emptyMap(),
                Collections.emptyMap());
    }
}
