package com.huaweicloud.sermant.core.plugin.agent.adviser;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;

import java.util.ArrayList;

/**
 * 转换器
 *
 * @author luanwenfei
 * @since 2023-04-11
 */
public class Adviser {
    private static final ArrayList<AdviserInterface> advisers = new ArrayList<>();

    private Adviser() {}

    public static void registry(AdviserInterface adviser) {
        advisers.add(adviser);
    }

    public static void unRegistry(AdviserInterface adviser) {
        advisers.remove(adviser);
    }

    public static ExecuteContext onMethodEnter(Object context, String adviceClassName) throws Throwable {
        // 多artifacts情况下顺序执行
        ExecuteContext executeContext = (ExecuteContext)context;
        for (AdviserInterface adviser : advisers) {
            executeContext = adviser.onMethodEnter(executeContext, adviceClassName);
        }
        return executeContext;
    }

    public static ExecuteContext onMethodExit(Object context, String adviceClassName) throws Throwable {
        // 多artifacts情况下顺序执行
        ExecuteContext executeContext = (ExecuteContext)context;
        for (AdviserInterface adviser : advisers) {
            executeContext = adviser.onMethodExit(executeContext, adviceClassName);
        }
        return executeContext;
    }
}
