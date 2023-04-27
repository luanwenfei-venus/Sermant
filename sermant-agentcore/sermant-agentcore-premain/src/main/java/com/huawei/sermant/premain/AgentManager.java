package com.huawei.sermant.premain;

import com.huawei.sermant.premain.classloader.CommonClassLoader;

import java.net.URL;

/**
 * Agent管理器
 *
 * @author luanwenfei
 * @since 2023-04-19
 */
public class AgentManager {
    private static CommonClassLoader commonClassLoader;

    public static void initCommonClassLoader() {
        commonClassLoader = new CommonClassLoader(new URL[0]);
    }

    public static CommonClassLoader getCommonClassLoader() {
        if (commonClassLoader == null) {
            initCommonClassLoader();
        }
        return commonClassLoader;
    }
}
