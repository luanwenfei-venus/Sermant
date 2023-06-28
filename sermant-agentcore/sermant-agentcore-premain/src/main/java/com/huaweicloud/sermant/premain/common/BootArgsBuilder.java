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

package com.huaweicloud.sermant.premain.common;

import static com.huaweicloud.sermant.premain.AgentPremain.getLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 启动参数构建器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-11-12
 */
public abstract class BootArgsBuilder {
    private static final Logger LOGGER = getLogger();

    /**
     * 构建启动参数
     *
     * @param agentArgs 程序入参
     * @return 启动参数
     */
    public static Map<String, Object> build(String agentArgs) {
        final Properties configMap = loadConfig();
        final Map<String, Object> argsMap = toArgsMap(agentArgs);
        addNotNullEntries(argsMap, configMap);
        addNormalEntries(argsMap, configMap);
        addPathEntries(argsMap);
        return argsMap;
    }

    /**
     * 构建入参集
     *
     * @param args 程序入参
     * @return 入参集
     */
    private static Map<String, Object> toArgsMap(String args) {
        final Map<String, Object> argsMap = new HashMap<String, Object>();
        if (args == null) {
            return argsMap;
        }
        for (String arg : args.trim().split(",")) {
            final int index = arg.indexOf('=');
            if (index >= 0) {
                argsMap.put(arg.substring(0, index).trim(), arg.substring(index + 1).trim());
            }
        }
        return argsMap;
    }

    /**
     * 读取启动配置
     *
     * @return 启动配置
     */
    private static Properties loadConfig() {
        final Properties properties = new Properties();
        try (InputStream configStream = new FileInputStream(PathDeclarer.getBootConfigPath())) {
            properties.load(configStream);
        } catch (IOException ioException) {
            LOGGER.severe(String.format(Locale.ROOT, "Exception occurs when close config InputStream , %s .",
                ioException.getMessage()));
        }
        return properties;
    }

    /**
     * 通用获取参数值方式，优先从配置获取，在从环境变量获取
     *
     * @param key 参数键
     * @param configMap 配置集
     * @return 参数值
     */
    private static String getCommonValue(String key, Properties configMap) {
        String value = configMap.getProperty(key);
        return getActualValue(value);
    }

    /**
     * 添加非空的键值对，涉及的参数均不可为空 appName、serviceName、instanceName、appType
     *
     * @param argsMap 参数集
     * @param configMap 配置集
     */
    private static void addNotNullEntries(Map<String, Object> argsMap, Properties configMap) {
        String key = SermantConstant.APP_NAME_KEY;
        if (!argsMap.containsKey(key)) {
            final String value = getCommonValue(key, configMap);
            argsMap.put(key, value == null ? "default" : value);
        }
        key = SermantConstant.SERVICE_NAME_KEY;
        if (!argsMap.containsKey(key)) {
            final String value = getCommonValue(key, configMap);
            argsMap.put(key, value == null ? "default" : value);
        }
        key = SermantConstant.APP_TYPE_KEY;
        if (!argsMap.containsKey(key)) {
            final String value = getCommonValue(key, configMap);
            argsMap.put(key, value == null ? "default" : value);
        }
    }

    /**
     * 添加普通键值对，为空时不添加
     *
     * @param argsMap 参数集
     * @param configMap 配置集
     */
    private static void addNormalEntries(Map<String, Object> argsMap, Properties configMap) {
        for (Object key : configMap.keySet()) {
            if (!argsMap.containsKey((String)key)) {
                final String value = configMap.getProperty((String)key);
                if (value != null) {
                    argsMap.put((String)key, getActualValue(value));
                }
            }
        }
    }

    /**
     * 获取形如"${}"的配置，环境变量或系统变量
     *
     * @param configVal 配置值
     * @return 真实配置
     */
    private static String getActualValue(String configVal) {
        if (configVal != null && configVal.matches("^.*\\$\\{[\\w.]+(:.*)?}.*$")) {
            final int startIndex = configVal.indexOf("${") + 2;
            final int endIndex = configVal.indexOf('}', startIndex);
            final String envKey = configVal.substring(startIndex, endIndex);
            final int separatorIndex = envKey.indexOf(':');
            final String key = separatorIndex >= 0 ? envKey.substring(0, separatorIndex) : envKey;
            final String defaultValue = separatorIndex >= 0 ? envKey.substring(separatorIndex + 1) : "";

            // 优先级为环境变量 > 系统变量
            if (isNotBlank(System.getenv(key))) {
                return System.getenv(key);
            }
            if (isNotBlank(System.getProperty(key))) {
                return System.getProperty(key);
            }
            return defaultValue;
        }
        return configVal;
    }

    /**
     * 添加路径键值对
     *
     * @param argsMap 参数集
     */
    private static void addPathEntries(Map<String, Object> argsMap) {
        argsMap.put(SermantConstant.AGENT_ROOT_DIR_KEY, PathDeclarer.getAgentPath());
        argsMap.put(SermantConstant.CORE_IMPLEMENT_DIR_KEY, PathDeclarer.getImplementPath());
        argsMap.put(SermantConstant.CORE_CONFIG_FILE_KEY, PathDeclarer.getConfigPath());
        argsMap.put(SermantConstant.PLUGIN_SETTING_FILE_KEY, PathDeclarer.getPluginSettingPath());
        argsMap.put(SermantConstant.PLUGIN_PACKAGE_DIR_KEY, PathDeclarer.getPluginPackagePath());
        argsMap.put(SermantConstant.LOG_SETTING_FILE_KEY, PathDeclarer.getLogbackSettingPath());
        argsMap.put(SermantConstant.COMMON_DEPENDENCY_DIR_KEY, PathDeclarer.getCommonLibPath());
    }

    /**
     * isNotBlank
     *
     * @param charSequence charSequence
     * @return boolean
     */
    public static boolean isNotBlank(final CharSequence charSequence) {
        int charSequenceLen = charSequence == null ? 0 : charSequence.length();
        if (charSequenceLen == 0) {
            return false;
        }
        for (int i = 0; i < charSequenceLen; i++) {
            if (!Character.isWhitespace(charSequence.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
