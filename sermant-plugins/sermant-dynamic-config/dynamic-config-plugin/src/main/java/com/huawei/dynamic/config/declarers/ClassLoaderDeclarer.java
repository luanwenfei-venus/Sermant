package com.huawei.dynamic.config.declarers;

import com.huaweicloud.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.agent.matcher.ClassMatcher;
import com.huaweicloud.sermant.core.plugin.agent.matcher.MethodMatcher;

/**
 * ClassLoader增强
 *
 * @author luanwenfei
 * @since 2023-04-28
 */
public class ClassLoaderDeclarer extends AbstractPluginDeclarer {
    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals("org.springframework.boot.loader.LaunchedURLClassLoader");
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[] {
            InterceptDeclarer.build(MethodMatcher.nameEquals("loadClass"),
                "com.huawei.dynamic.config.interceptors.ClassLoaderLoadClassInterceptor"),
            InterceptDeclarer.build(MethodMatcher.nameEquals("findResource"),
                "com.huawei.dynamic.config.interceptors.ClassLoaderFindResourceInterceptor")};
    }
}
