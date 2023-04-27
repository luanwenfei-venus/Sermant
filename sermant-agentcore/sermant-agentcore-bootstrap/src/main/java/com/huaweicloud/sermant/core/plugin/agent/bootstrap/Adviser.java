package com.huaweicloud.sermant.core.plugin.agent.bootstrap;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.ServiceLoader;

/**
 * 转换器
 *
 * @author luanwenfei
 * @since 2023-04-11
 */
public class Adviser {
    private static AdviserInterface defaultAdviser;

    private static final HashMap<String, List<Interceptor>> InterceptorListMap = new HashMap<>();

    private Adviser() {}

    public static void registry(AdviserInterface adviserInterface) {
        defaultAdviser = adviserInterface;
    }

    public static ExecuteContext onMethodEnter(Object context, ListIterator<?> interceptorItr) throws Throwable {
        return defaultAdviser.onMethodEnter((ExecuteContext)context, (ListIterator<Interceptor>)interceptorItr);
    }

    public static ExecuteContext onMethodExit(Object context, ListIterator<?> interceptorItr) throws Throwable {
        return defaultAdviser.onMethodExit((ExecuteContext)context, (ListIterator<Interceptor>)interceptorItr);
    }

    public static HashMap<String, List<Interceptor>> getInterceptorListMap() {
        return InterceptorListMap;
    }
}
