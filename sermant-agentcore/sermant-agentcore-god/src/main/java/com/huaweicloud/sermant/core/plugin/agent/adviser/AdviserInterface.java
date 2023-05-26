package com.huaweicloud.sermant.core.plugin.agent.adviser;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;

import java.util.ListIterator;

/**
 * 转换器接口
 *
 * @author luanwenfei
 * @since 2023-04-11
 */
public interface AdviserInterface {
    /**
     * 调用方法的前置触发点
     *
     * @param context 执行上下文
     * @param interceptorItr 拦截器双向迭代器
     * @return 执行上下文
     */
    ExecuteContext onMethodEnter(ExecuteContext context, ListIterator<Interceptor> interceptorItr) throws Throwable;

    /**
     * 调用方法的后置触发点
     *
     * @param context 执行上下文
     * @param interceptorItr 拦截器双向迭代器
     * @return 执行上下文
     */
    ExecuteContext onMethodExit(ExecuteContext context, ListIterator<Interceptor> interceptorItr) throws Throwable;
}
