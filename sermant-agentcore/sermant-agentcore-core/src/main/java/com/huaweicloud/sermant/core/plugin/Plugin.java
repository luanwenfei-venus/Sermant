package com.huaweicloud.sermant.core.plugin;

import com.huaweicloud.sermant.core.plugin.classloader.PluginClassLoader;
import com.huaweicloud.sermant.core.plugin.classloader.ServiceClassLoader;

import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于管理插件信息
 * 插件名，插件包目录，插件服务，插件配置，插件主模块的类加载器，插件服务类加载器，插件的ResetTransformer
 *
 * @author luanwenfei
 * @since 2023-05-30
 */
public class Plugin {
    private String name;

    private String version;

    private String path;

    private List<String> serviceList = new ArrayList<>();

    private List<String> configList = new ArrayList<>();

    private PluginClassLoader pluginClassLoader;

    private ServiceClassLoader serviceClassLoader;

    private ResettableClassFileTransformer resettableClassFileTransformer;

    public Plugin(String name, String path, PluginClassLoader pluginClassLoader) {
        this.name = name;
        this.path = path;
        this.pluginClassLoader = pluginClassLoader;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<String> serviceList) {
        this.serviceList = serviceList;
    }

    public List<String> getConfigList() {
        return configList;
    }

    public void setConfigList(List<String> configList) {
        this.configList = configList;
    }

    public PluginClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }

    public void setPluginClassLoader(PluginClassLoader pluginClassLoader) {
        this.pluginClassLoader = pluginClassLoader;
    }

    public ServiceClassLoader getServiceClassLoader() {
        return serviceClassLoader;
    }

    public void setServiceClassLoader(ServiceClassLoader serviceClassLoader) {
        this.serviceClassLoader = serviceClassLoader;
    }

    public ResettableClassFileTransformer getResettableClassFileTransformer() {
        return resettableClassFileTransformer;
    }

    public void setResettableClassFileTransformer(ResettableClassFileTransformer resettableClassFileTransformer) {
        this.resettableClassFileTransformer = resettableClassFileTransformer;
    }

    public void createServiceClassLoader(URL[] urls) {
        if (urls.length > 0) {
            this.serviceClassLoader = new ServiceClassLoader(urls, this.pluginClassLoader);
        }
    }
}
