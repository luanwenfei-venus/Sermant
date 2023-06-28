/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huaweicloud.loadbalancer.rule;

import com.huaweicloud.loadbalancer.service.RuleConverter;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.service.PluginService;
import com.huaweicloud.sermant.core.plugin.service.PluginServiceManager;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.service.dynamicconfig.common.DynamicConfigEvent;
import com.huaweicloud.sermant.core.utils.StringUtils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 规则管理器测试
 *
 * @author zhouss
 * @since 2022-08-16
 */
public class RuleManagerTest {
    private static final String MATCH_GROUP_KEY = "servicecomb.matchGroup.test";
    private static final String BALANCER_KEY = "servicecomb.loadbalance.test";
    private static final String SERVICE_NAME = "service-test";
    private static final String RULE = "Random";

    /**
     * 测试解析配置
     */
    @Test
    public void testResolve() {
        Mockito.mockStatic(ServiceManager.class)
                .when(() -> PluginServiceManager.getPluginService(RuleConverter.class))
                .thenReturn(new YamlRuleConverter());
        final AtomicBoolean isNotify = new AtomicBoolean();
        RuleManager.INSTANCE.addRuleListener((rule, event) -> {
            if (event.getKey().equals(BALANCER_KEY)) {
                Assert.assertEquals(rule.getServiceName(), SERVICE_NAME);
                Assert.assertEquals(rule.getRule(), RULE);
            }
            isNotify.set(true);
        });
        final DynamicConfigEvent matchGroupEvent = buildEvent(MATCH_GROUP_KEY, getMatchGroup(SERVICE_NAME));
        RuleManager.INSTANCE.resolve(matchGroupEvent);
        final DynamicConfigEvent loadbalancerEvent = buildEvent(BALANCER_KEY, getLoadbalancer());
        RuleManager.INSTANCE.resolve(loadbalancerEvent);
        Assert.assertTrue(RuleManager.INSTANCE.isConfigured());
        Assert.assertTrue(isNotify.get());
        // 测试匹配规则
        final Optional<LoadbalancerRule> targetServiceRule = RuleManager.INSTANCE.getTargetServiceRule(SERVICE_NAME);
        Assert.assertTrue(targetServiceRule.isPresent());
        final Optional<LoadbalancerRule> other = RuleManager.INSTANCE
                .getTargetServiceRule(SERVICE_NAME + "sss");
        Assert.assertFalse(other.isPresent());
        // 测试无服务名场景下所有服务名均可匹配规则
        final DynamicConfigEvent dynamicConfigEvent = buildEvent(MATCH_GROUP_KEY, getMatchGroup(null));
        RuleManager.INSTANCE.resolve(dynamicConfigEvent);
        final Optional<LoadbalancerRule> allMatch = RuleManager.INSTANCE.getTargetServiceRule("SERVICE_NAME");
        Assert.assertTrue(allMatch.isPresent());
    }

    private String getLoadbalancer() {
        return "rule: " + RULE;
    }

    private String getMatchGroup(String serviceName) {
        return "alias: flowcontrol111\n"
                + "matches:\n"
                + "- apiPath:\n"
                + "    exact: /sc/provider/\n"
                + "  headers: {}\n"
                + "  method:\n"
                + "  - GET\n"
                + "  name: degrade\n"
                + "  showAlert: false\n"
                + "  uniqIndex: c3w7x\n"
                + ((serviceName != null) ? ("  serviceName: " + serviceName + "\n") : "");
    }

    private DynamicConfigEvent buildEvent(String key, String content) {
        return DynamicConfigEvent.createEvent(key, "default", content);
    }

    static class YamlRuleConverter implements RuleConverter, PluginService {
        private static final Logger LOGGER = LoggerFactory.getLogger();

        private final Yaml yaml;

        /**
         * 构造器
         */
        public YamlRuleConverter() {
            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);
            yaml = new Yaml(representer);
        }

        @Override
        public <T> Optional<T> convert(String rawContent, Class<T> clazz) {
            if (StringUtils.isBlank(rawContent)) {
                return Optional.empty();
            }
            try {
                return Optional.of(yaml.loadAs(rawContent, clazz));
            } catch (YAMLException ex) {
                LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH, "Can not convert content [%s] to LoadbalancerRule",
                        rawContent), ex);
            }
            return Optional.empty();
        }
    }
}
