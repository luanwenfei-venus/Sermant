package com.huaweicloud.sermant.god.common;

import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * 管理现在已经存在的Sermant
 *
 * @author luanwenfei
 * @since 2023-05-24
 */
public class SermantManager {
    private static final HashMap<String, SermantClassLoader> SERMANT_MANAGE_MAP = new HashMap<>();

    /**
     * 当前命名空间下是否有Sermant
     *
     * @param namespace 标识Sermant
     * @return boolean
     */
    public static boolean hasSermant(String namespace) {
        return SERMANT_MANAGE_MAP.containsKey(namespace);
    }

    /**
     * 创建一个Sermant
     *
     * @param namespace 标识Sermant
     * @param urls Sermant资源路径
     * @return SermantClassLoader
     */
    public static SermantClassLoader createSermant(String namespace,URL[] urls) {
        if (hasSermant(namespace)) {
            return SERMANT_MANAGE_MAP.get(namespace);
        }
        return new SermantClassLoader(urls);
    }

    /**
     * 移除一个Sermant
     *
     * @param sermantClassLoader 需要移除的Sermant的类加载器
     */
    public static void removeSermant(SermantClassLoader sermantClassLoader) {
        if (SERMANT_MANAGE_MAP.containsValue(sermantClassLoader)) {
            for (Entry<String, SermantClassLoader> entry : SERMANT_MANAGE_MAP.entrySet()) {
                if (entry.getValue().equals(sermantClassLoader)) {
                    SERMANT_MANAGE_MAP.remove(entry.getKey());
                    break;
                }
            }
        }
    }
}
