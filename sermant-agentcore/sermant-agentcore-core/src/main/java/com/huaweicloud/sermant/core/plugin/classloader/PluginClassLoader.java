/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.plugin.classloader;

import com.huaweicloud.sermant.core.common.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 加载插件主模块的类加载器
 *
 * @author luanwenfei
 * @since 2023-04-27
 */
public class PluginClassLoader extends URLClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    /**
     * 对PluginClassLoader已经加载的类进行管理
     */
    private final Map<String, Class<?>> pluginClassMap = new HashMap<>();

    private final ArrayList<ClassLoader> classLoaders = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param urls   Url of sermant-xxx-plugin
     * @param parent parent classloader
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void appendUrl(URL url) {
        this.addURL(url);
    }

    public void appendClassLoader(ClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    private Class<?> findPluginClass(String name) {
        if (!pluginClassMap.containsKey(name)) {
            try {
                pluginClassMap.put(name, findClass(name));
            } catch (ClassNotFoundException ignored) {
                pluginClassMap.put(name, null);
            }
        }
        return pluginClassMap.get(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            LOGGER.log(Level.WARNING, "Load class by classLoaderCache class name : {0}.", name);
            // 再从自身查找 先从父类查找 再从上下文类加载器查找 再从类加载器缓存中查找
            Class<?> clazz = findPluginClass(name);

            if (clazz == null) {
                try {
                    // 这里需要调用父类的同参数方法 否则: StackOverFlow
                    clazz = super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    // 破坏双亲委派
                }
            }

            if (clazz == null) {
                // 自身和线程上下文类加载器不同时从自身找,否则会堆栈溢出
                if (!this.equals(Thread.currentThread().getContextClassLoader())) {
                    try {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // 通过线程上下文类加载器找不到
                    }
                }
            }

//            if (clazz == null) {
//                LOGGER.log(Level.WARNING, "Load class by classLoaderCache class name : {0}.", name);
//                for (ClassLoader classLoader : classLoaders) {
//                    if (!this.equals(classLoader)) {
//                        try {
//                            clazz = classLoader.loadClass(name);
//                        } catch (ClassNotFoundException e) {
//                            // 缓存类加载器也找不到
//                        }
//                    }
//                }
//            }
            if (clazz == null) {
                throw new ClassNotFoundException("Sermant pluginClassLoader can not load class: " + name);
            }

            // 通过PluginClassLoader的super.loadClass方法把从自身加载的类放入缓存
            if (clazz.getClassLoader() == this) {
                pluginClassMap.put(name, clazz);
            }

            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }
}
