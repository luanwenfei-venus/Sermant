package com.huaweicloud.sermant.god.common;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理现在已经存在的Sermant
 *
 * @author luanwenfei
 * @since 2023-05-24
 */
public class SermantManager {
    private static final ConcurrentHashMap<String, SermantClassLoader> SERMANT_MANAGE_MAP = new ConcurrentHashMap<>();
    
    private static final ConcurrentHashMap<String,Boolean> SERMANT_STATUS = new ConcurrentHashMap<>();

    /**
     * 当前产品是否挂载过Sermant底座
     *
     * @param artifact 标识Sermant
     * @return boolean
     */
    public static boolean hasSermant(String artifact) {
        return SERMANT_MANAGE_MAP.containsKey(artifact);
    }

    /**
     * 创建一个Sermant
     *
     * @param artifact 标识Sermant
     * @param urls Sermant资源路径
     * @return SermantClassLoader
     */
    public static SermantClassLoader createSermant(String artifact, URL[] urls) {
        if (hasSermant(artifact)) {
            return SERMANT_MANAGE_MAP.get(artifact);
        }
        SermantClassLoader sermantClassLoader = new SermantClassLoader(urls);
        SERMANT_MANAGE_MAP.put(artifact, sermantClassLoader);
        return sermantClassLoader;
    }

    /**
     * 移除一个Sermant
     *
     * @param artifact 需要移除的Sermant的命名空间
     */
    public static void removeSermant(String artifact) {
        SermantClassLoader sermantClassLoader = SERMANT_MANAGE_MAP.get(artifact);
        try {
            sermantClassLoader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SERMANT_MANAGE_MAP.remove(artifact);
    }

    public static boolean checkSermantStatus(String artifact){
        Boolean status = SERMANT_STATUS.get(artifact);
        if(status == null){
            return false;
        }
        return status;
    }
    
    public static void updateSermantStatus(String artifact, boolean status) {
        SERMANT_STATUS.put(artifact,status);
    }
}
