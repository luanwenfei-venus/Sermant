/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

import com.huaweicloud.sermant.core.common.CommonConstant;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 加载插件主模块的类加载器
 *
 * @author luanwenfei
 * @since 2023-04-27
 */
public class PluginClassLoader extends URLClassLoader {
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

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 先从父类查找 再从自身查找 再从上下文类加载器查找 再从类加载器缓存中查找
            Class<?> clazz = null;

            try {
                // 这里需要调用父类的同参数方法 否则: StackOverFlow
                clazz = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // 破坏双亲委派
            }

            if (clazz == null && !ifExclude(name)) {
                // 自身和线程上下文类加载器不同时从自身找,否则会堆栈溢出
                if (!this.equals(Thread.currentThread().getContextClassLoader())) {
                    try {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // 通过线程上下文类加载器找不到
                    }
                }
            }

            if (clazz == null) {
                throw new ClassNotFoundException("Sermant pluginClassLoader can not load class: " + name);
            }

            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    public Class<?> loadClassOnlySermant(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz;

        // 这里需要调用父类的同参数方法 否则: StackOverFlow
        clazz = super.loadClass(name, resolve);

        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    private boolean ifExclude(String name) {
        for (String excludePrefix : CommonConstant.IGNORE_PREFIXES) {
            if (name.startsWith(excludePrefix)) {
                return true;
            }
        }
        return false;
    }
}
