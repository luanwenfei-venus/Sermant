/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.event.collector;

import com.huaweicloud.sermant.core.event.EventLevel;
import com.huaweicloud.sermant.core.event.EventType;

/**
 * 日志事件定义
 *
 * @author luanwenfei
 * @since 2023-03-04
 */
public enum LogEventDefinitions {
    /**
     * 错误日志事件信息定义
     */
    ERROR_LOG("ERROR_LOG", EventType.LOG, EventLevel.EMERGENCY),

    /**
     * 警告日志事件信息定义
     */
    WARN_LOG("WARN_LOG", EventType.LOG, EventLevel.IMPORTANT);

    /**
     * 事件
     */
    private final String name;

    private final EventType eventType;

    private final EventLevel eventLevel;

    LogEventDefinitions(String name, EventType eventType, EventLevel eventLevel) {
        this.name = name;
        this.eventType = eventType;
        this.eventLevel = eventLevel;
    }

    public String getName() {
        return name;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventLevel getEventLevel() {
        return eventLevel;
    }
}
