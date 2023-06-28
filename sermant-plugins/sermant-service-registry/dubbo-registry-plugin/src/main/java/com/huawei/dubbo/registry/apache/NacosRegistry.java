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

package com.huawei.dubbo.registry.apache;

import com.huawei.dubbo.registry.service.nacos.NacosRegistryService;

import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.service.ServiceManager;

import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

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
    public NacosRegistry(org.apache.dubbo.common.URL url) {
        super(url);
        registryService = PluginServiceManager.getPluginService(NacosRegistryService.class);
    }

    @Override
    public void doRegister(org.apache.dubbo.common.URL url) {
        registryService.doRegister(url);
    }

    @Override
    public void doUnregister(org.apache.dubbo.common.URL url) {
        registryService.doUnregister(url);
    }

    @Override
    public void doSubscribe(org.apache.dubbo.common.URL url, NotifyListener listener) {
        registryService.doSubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(org.apache.dubbo.common.URL url, NotifyListener listener) {
        registryService.doUnsubscribe(url, listener);
    }

    @Override
    public boolean isAvailable() {
        return registryService.isAvailable();
    }
}
