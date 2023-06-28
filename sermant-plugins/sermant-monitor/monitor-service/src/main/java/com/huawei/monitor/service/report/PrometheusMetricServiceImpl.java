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

package com.huawei.monitor.service.report;

import com.huawei.monitor.config.MonitorServiceConfig;
import com.huawei.monitor.service.MetricReportService;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.ConfigManager;
import com.huaweicloud.sermant.core.plugin.config.PluginConfigManager;
import com.huaweicloud.sermant.core.utils.AesUtil;
import com.huaweicloud.sermant.core.utils.StringUtils;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpServer;

import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 监控插件配置服务接口实现
 *
 * @author zhp
 * @since 2022-09-15
 */
public class PrometheusMetricServiceImpl implements MetricReportService {
    private static HTTPServer httpServer = null;

    private static final int TCP_NUM = 3;

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public void startMonitorServer() {
        MonitorServiceConfig monitorServiceConfig = PluginConfigManager.getPluginConfig(MonitorServiceConfig.class);
        if (StringUtils.isBlank(monitorServiceConfig.getAddress()) || monitorServiceConfig.getPort() == 0) {
            LOGGER.info("monitor config missing address or port");
            return;
        }
        LOGGER.info("the monitor inspection is passed and the service starts");
        InetSocketAddress socketAddress =
                new InetSocketAddress(monitorServiceConfig.getAddress(), monitorServiceConfig.getPort());
        try {
            HTTPServer.Builder builder = new HTTPServer.Builder()
                    .withHttpServer(HttpServer.create(socketAddress, TCP_NUM)).withDaemonThreads(true);
            String key = monitorServiceConfig.getKey();
            if (StringUtils.isNoneBlank(key) && StringUtils.isNoneBlank(monitorServiceConfig.getUserName())
                    && StringUtils.isNoneBlank(monitorServiceConfig.getPassword())) {
                Optional<String> userNameOptional = AesUtil.decrypt(key, monitorServiceConfig.getUserName());
                Optional<String> passwordOptional = AesUtil.decrypt(key, monitorServiceConfig.getPassword());
                if (userNameOptional.isPresent() && passwordOptional.isPresent()) {
                    builder.withAuthenticator(new MonitorAuthenticator("", userNameOptional.get(),
                            passwordOptional.get()));
                }
            }
            httpServer = builder.build();
        } catch (IOException exception) {
            LOGGER.warning("could not create httpServer for prometheus");
        }
    }

    /**
     * 服务关闭方法
     */
    public void stopMonitorServer() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    /**
     * 授权认证
     *
     * @author zhp
     * @since 2022-11-11
     */
    static class MonitorAuthenticator extends BasicAuthenticator {
        private final String userName;

        private final String password;

        MonitorAuthenticator(String realm, String userName, String password) {
            super(realm);
            this.userName = userName;
            this.password = password;
        }

        @Override
        public boolean checkCredentials(String username, String key) {
            return StringUtils.equals(this.userName, username) && StringUtils.equals(this.password, key);
        }
    }
}