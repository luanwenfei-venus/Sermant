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

package com.huaweicloud.sermant.core.plugin.classloader;

import com.huaweicloud.sermant.core.common.BootArgsIndexer;
import com.huaweicloud.sermant.core.common.CommonConstant;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.plugin.agent.config.AgentConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 插件类加载器，用于加载插件服务包
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public class ServiceClassLoader extends URLClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    /**
     * 不优先使用PluginClassLoader加载的全限定名前缀
     */
    private final String[] ignoredPrefixes;

    /**
     * 对ClassLoader内部已加载的Class的管理
     */
    private final Map<String, Class<?>> serviceClassMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param urls   Url of plugin package
     * @param parent parent classloader
     */
    public ServiceClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        if (ConfigManager.getConfig(AgentConfig.class) != null) {
            ignoredPrefixes = ConfigManager.getConfig(AgentConfig.class).getIgnoredPrefixes().toArray(new String[0]);
        } else {
            LOGGER.info("AgentConfig is null, set default ignored prefixes: [com.huaweicloud.sermant,com.huawei.sermant]");
            ignoredPrefixes = new String[] {"com.huaweicloud.sermant", "com.huawei.sermant"};
        }
    }

    /**
     * 加载插件服务包中的类并维护
     *
     * @param name 全限定名
     * @return Class对象
     */
    private Class<?> loadPluginClass(String name) {
        if (!serviceClassMap.containsKey(name)) {
            try {
                serviceClassMap.put(name, findClass(name));
            } catch (ClassNotFoundException ignored) {
                serviceClassMap.put(name, null);
            }
        }
        return serviceClassMap.get(name);
    }

    private boolean ifExclude(String name) {
        for (String excludePrefix : ignoredPrefixes) {
            if (name.startsWith(excludePrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = null;
            if (!ifExclude(name)) {
                clazz = loadPluginClass(name);
            }
            if (clazz == null) {
                clazz = super.loadClass(name, resolve);

                // 通过PluginClassLoader的super.loadClass方法把从自身加载的类放入缓存
                if (clazz != null && clazz.getClassLoader() == this) {
                    serviceClassMap.put(name, clazz);
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = null;

        // 针对日志配置文件，定制化getResource方法，首先获取agent/config/logback.xml,其次PluginClassloader下资源文件中的logback.xml
        if (CommonConstant.LOG_SETTING_FILE_NAME.equals(name)) {
            File logSettingFile = BootArgsIndexer.getLogSettingFile();
            if (logSettingFile.exists() && logSettingFile.isFile()) {
                try {
                    url = logSettingFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    url = findResource(name);
                }
            } else {
                url = findResource(name);
            }
        }
        if (url == null) {
            url = super.getResource(name);
        }
        return url;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
