/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.huaweicloud.sermant.core.classloader;

import com.huaweicloud.sermant.core.common.CommonConstant;

import com.huawei.sermant.premain.AgentManager;
import com.huawei.sermant.premain.classloader.CommonClassLoader;

import com.huaweicloud.sermant.core.plugin.classloader.PluginClassLoader;
import com.huaweicloud.sermant.core.utils.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manage custom classloaders.
 *
 * @author luanwenfei
 * @since 2022-06-20
 */
public class ClassLoaderManager {
    private static FrameworkClassLoader frameworkClassLoader;

    private static CommonClassLoader commonClassLoader;

    private static EnhanceClassLoader enhanceClassLoader;

    private static List<PluginClassLoader> pluginClassLoaders;

    private ClassLoaderManager() {}

    /**
     * Init custom classloaders.
     *
     * @param argsMap arguments map
     * @throws MalformedURLException MalformedURLException
     */
    public static void init(Map<String, Object> argsMap) throws MalformedURLException {
        pluginClassLoaders = new ArrayList<>();
        initCommonClassLoader(argsMap.get(CommonConstant.COMMON_DEPENDENCY_DIR_KEY).toString());
        initFrameworkClassLoader(argsMap.get(CommonConstant.CORE_IMPLEMENT_DIR_KEY).toString());
        enhanceClassLoader = new EnhanceClassLoader(new URL[0], commonClassLoader);
    }

    public static void shutdown() {
        for (PluginClassLoader pluginClassLoader : pluginClassLoaders) {
            pluginClassLoader.shutdown();
        }
        enhanceClassLoader.shutdown();
        frameworkClassLoader.shutdown();
        // commonClassLoader.shutdown();
    }

    /**
     * For getting FrameworkClassLoader
     *
     * @return A frameworkClassLoader that has been initialized.
     */
    public static FrameworkClassLoader getFrameworkClassLoader() {
        return frameworkClassLoader;
    }

    // /**
    // * For getting CommonClassLoader
    // *
    // * @return A commonClassLoader that has been initialized.
    // */
    // public static CommonClassLoader getCommonClassLoader() {
    // return commonClassLoader;
    // }

    /**
     * For getting EnhanceClassLoader
     *
     * @return A enhanceClassLoader that has been initialized.
     */
    public static EnhanceClassLoader getEnhanceClassLoader() {
        return enhanceClassLoader;
    }

    private static void initFrameworkClassLoader(String path) throws MalformedURLException {
        URL[] coreImplementUrls = listCoreImplementUrls(path);
        frameworkClassLoader = new FrameworkClassLoader(coreImplementUrls, commonClassLoader);
    }

    private static URL[] listCoreImplementUrls(String coreImplementPath) throws MalformedURLException {
        File coreImplementDir = new File(FileUtils.validatePath(coreImplementPath));
        if (!coreImplementDir.exists() || !coreImplementDir.isDirectory()) {
            throw new RuntimeException("core implement directory is not exist or is not directory.");
        }
        File[] jars = coreImplementDir.listFiles((file, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new RuntimeException("core implement directory is empty");
        }
        List<URL> urlList = new ArrayList<>();
        for (File jar : jars) {
            urlList.add(jar.toURI().toURL());
        }
        return urlList.toArray(new URL[0]);
    }

    private static void initCommonClassLoader(String path) throws MalformedURLException {
        commonClassLoader = AgentManager.getCommonClassLoader();
        URL[] commonLibUrls = listCommonLibUrls(path);
        for (URL url : commonLibUrls) {
            commonClassLoader.appendToCommonClassloaderSearch(url);
        }
    }

    private static URL[] listCommonLibUrls(String commonLibPath) throws MalformedURLException {
        File commonLibDir = new File(FileUtils.validatePath(commonLibPath));
        if (!commonLibDir.exists() || !commonLibDir.isDirectory()) {
            throw new RuntimeException("common lib is not exist or is not directory.");
        }
        File[] jars = commonLibDir.listFiles((file, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new RuntimeException("common lib directory is empty");
        }
        List<URL> urlList = new ArrayList<>();
        for (File jar : jars) {
            urlList.add(jar.toURI().toURL());
        }
        return urlList.toArray(new URL[0]);
    }

    public static List<PluginClassLoader> getPluginClassLoaders() {
        return pluginClassLoaders;
    }
}
