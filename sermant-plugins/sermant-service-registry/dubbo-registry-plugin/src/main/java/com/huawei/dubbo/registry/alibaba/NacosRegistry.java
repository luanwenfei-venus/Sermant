/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.dubbo.registry.alibaba;

import com.huawei.dubbo.registry.service.nacos.NacosRegistryService;

import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.service.ServiceManager;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;

/**
 * nacos注册
 *
 * @author chengyouling
 * @since 2022-10-25
 */
public class NacosRegistry extends FailbackRegistry {
    private final NacosRegistryService registryService;

    /**
     * 构造方法
     *
     * @param url 注册url
     */
    public NacosRegistry(URL url) {
        super(url);
        registryService = PluginServiceManager.getPluginService(NacosRegistryService.class);
    }

    @Override
    public void doRegister(URL url) {
        registryService.doRegister(url);
    }

    @Override
    public void doUnregister(URL url) {
        registryService.doUnregister(url);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener notifyListener) {
        registryService.doSubscribe(url, notifyListener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener notifyListener) {
        registryService.doUnsubscribe(url, notifyListener);
    }

    @Override
    public boolean isAvailable() {
        return registryService.isAvailable();
    }
}
