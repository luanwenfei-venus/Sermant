/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.plugin.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件设定配置
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public class PluginSetting {
    /**
     * 插件名称集
     */
    private Set<String> plugins;

    /**
     * 场景与插件映射关系
     */
    private Map<String, Set<String>> profiles;

    /**
     * 启动场景名
     */
    private String profile;

    public Set<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(Set<String> plugins) {
        this.plugins = plugins;
    }

    public void setProfiles(Map<String, Set<String>> profiles) {
        this.profiles = profiles;
    }

    public Map<String, Set<String>> getProfiles() {
        return profiles;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
