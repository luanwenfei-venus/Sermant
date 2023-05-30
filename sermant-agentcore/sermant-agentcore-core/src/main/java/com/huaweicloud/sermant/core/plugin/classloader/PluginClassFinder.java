package com.huaweicloud.sermant.core.plugin.classloader;

import com.huaweicloud.sermant.core.plugin.Plugin;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于适配从多个PluginClassLoader中寻找对应类和资源
 *
 * @author luanwenfei
 * @since 2023-05-30
 */
public class PluginClassFinder {
    private final Map<String, PluginClassLoader> pluginClassLoaderMap = new HashMap<>();
    
    public void addPluginClassLoader(Plugin plugin) {
        pluginClassLoaderMap.put(plugin.getName(), plugin.getPluginClassLoader());
    }

    public void removePluginClassLoader(Plugin plugin) {
        pluginClassLoaderMap.remove(plugin.getName());
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (PluginClassLoader pluginClassLoader : pluginClassLoaderMap.values()) {
            try {
                Class<?> clazz = pluginClassLoader.loadClass(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignore) {
                // ignore
            }
        }
        throw new ClassNotFoundException("Can not load class in pluginClassLoaders: " + name);
    }

    public Class<?> loadClassOnlySermant(String name, boolean resolve) throws ClassNotFoundException {
        for (PluginClassLoader pluginClassLoader : pluginClassLoaderMap.values()) {
            try {
                Class<?> clazz = pluginClassLoader.loadClassOnlySermant(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignore) {
                // ignore
            }
        }
        throw new ClassNotFoundException("Can not load class in pluginClassLoaders: " + name);
    }

    public URL findResource(String path) {
        for (PluginClassLoader pluginClassLoader : pluginClassLoaderMap.values()) {
            URL url = pluginClassLoader.findResource(path);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
