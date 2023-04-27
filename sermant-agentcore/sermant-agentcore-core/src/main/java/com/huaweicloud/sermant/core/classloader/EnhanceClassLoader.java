package com.huaweicloud.sermant.core.classloader;

import com.huaweicloud.sermant.core.common.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强定义的类加载器
 *
 * @author luanwenfei
 * @since 2023-04-18
 */
public class EnhanceClassLoader extends URLClassLoader {
    /**
     * 对CommonClassLoader已经加载的类进行管理
     */
    private final Map<String, Class<?>> enhanceClassMap = new HashMap<>();

    /**
     * 构造方法，CommonClassLoader默认以AppClassloader为父类加载器
     *
     * @param urls Url of sermant-common
     */
    public EnhanceClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addUrlToEnhanceClassLoaderSearch(URL url) {
        this.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 破坏双亲委派，先从自身加载，再从父类加载，保持Sermant插件服务的第三方依赖和宿主依赖隔离
            Class<?> clazz = loadEnhanceClass(name);

            if (clazz == null) {
                clazz = getParent().loadClass(name);
            }

            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    /**
     * 加载增强定义
     *
     * @param name 类名
     * @return Class<?>
     */
    private Class<?> loadEnhanceClass(String name) {
        if (!enhanceClassMap.containsKey(name)) {
            try {
                enhanceClassMap.put(name, findClass(name));
            } catch (ClassNotFoundException ignored) {
                // 若自身无法加载则把类名放入缓存，后续不再尝试加载
                enhanceClassMap.put(name, null);
            }
        }
        return enhanceClassMap.get(name);
    }

    public void shutdown() {
        enhanceClassMap.clear();
        this.clearAssertionStatus();
        try {
            this.close();
        } catch (IOException e) {
            LoggerFactory.getLogger().severe("Close EnhanceClassLoader error:" + e.getMessage());
        }
    }
}
