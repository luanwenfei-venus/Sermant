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

package com.huaweicloud.sermant.core;

import com.huawei.sermant.premain.spi.AgentEntrance;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.BootArgsIndexer;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.event.EventManager;
import com.huaweicloud.sermant.core.event.collector.FrameworkEventCollector;
import com.huaweicloud.sermant.core.operation.OperationManager;
import com.huaweicloud.sermant.core.plugin.PluginSystemEntrance;
import com.huaweicloud.sermant.core.plugin.agent.ByteEnhanceManager;
import com.huaweicloud.sermant.core.plugin.agent.bootstrap.Adviser;
import com.huaweicloud.sermant.core.plugin.agent.template.CommonMethodAdviser;
import com.huaweicloud.sermant.core.plugin.common.PluginSchemaValidator;
import com.huaweicloud.sermant.core.service.ServiceManager;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * agent core入口
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public class AgentCoreEntrance implements AgentEntrance {
    public AgentCoreEntrance() {}

    /**
     * 入口方法
     *
     * @param argsMap 参数集
     * @param instrumentation Instrumentation对象
     * @throws Exception agent core执行异常
     */
    public void run(Map<String, Object> argsMap, Instrumentation instrumentation) throws Exception {
        // 初始化框架类加载器
        ClassLoaderManager.init(argsMap);

        // 初始化日志
        LoggerFactory.init();

        // 通过启动配置构建路径索引
        BootArgsIndexer.build(argsMap);

        // 初始化统一配置
        ConfigManager.initialize(argsMap);

        // 初始化操作类
        OperationManager.initOperations();

        // 启动核心服务
        ServiceManager.initServices();

        // 初始化事件系统
        EventManager.init();

        // 初始化转换器
        Adviser.registry(new CommonMethodAdviser());

        // 初始化插件
        PluginSystemEntrance.initialize(instrumentation);

        // 上报Sermant启动事件
        FrameworkEventCollector.getInstance().collectAgentStartEvent();
    }

    public void unInstall() {
        cleanPlugins();
        cleanFramework();
    }

    public void cleanPlugins() {
        // todo 插件清理：
        // todo 首先卸载transformer，让类转换器不再生效，从而无法再触发到拦截器逻辑
        if (ByteEnhanceManager.unEnhance()) {
            System.out.println("==============un enhance success==============");
            ByteEnhanceManager.clear();
            // todo 清理拦截器数组中的拦截器缓存，消除对拦截器类的引用，避免关闭插件类加载器时导致报错
            Adviser.getInterceptorListMap().clear();
            // todo 清理字节码增强声明器中的缓存，消除对字节码声明器的引用，避免关闭插件类加载器时导致报错
            // 每次都会创建新的 似乎没有需要清理的部分

            // todo 关闭并清理插件服务，关闭插件服务可以进行资源清理，并且消除插件服务运行对其他类的引用
            // todo 清理插件服务管理器中的插件服务缓存，消除对插件服务类的引用，避免关闭插件类加载器时导致报错
            // todo 卸载各个插件的事件收集器
            ServiceManager.shutdown();

            // todo 清理插件配置管理器中的插件配置缓存，消除对插件配置类的引用，避免关闭插件类加载器时导致报错
            ConfigManager.shutdown();

            // todo 清理插件元信息缓存
            PluginSchemaValidator.shutdown();
        }
    }

    public void cleanFramework() {
        // todo 框架清理：agent-core及agent-bootstrap暂时先不考虑变更代码逻辑的问题
        // todo 重制转换器————当前场景暂不做
        // todo operation类同上
        OperationManager.shutdown();

        // todo 清理类加载器
        ClassLoaderManager.shutdown();
    }
}
