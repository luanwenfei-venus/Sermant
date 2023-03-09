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

package com.huaweicloud.sermant.core.event;

import com.huaweicloud.sermant.core.common.BootArgsIndexer;
import com.huaweicloud.sermant.core.common.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * 事件采集器
 *
 * @author luanwenfei
 * @since 2023-03-02
 */
public class EventCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    // 有界阻塞队列 缓存事件，未满则定时上报，已满则主动上报
    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<>(100);

    protected EventCollector() {}

    /**
     * 用于事件采集管理器获取当前缓存事件
     */
    public final BlockingQueue<Event> collect() {
        return eventQueue;
    }

    /**
     * 用于向事件采集器添加事件, 如果满了就会主动发送事件信息
     *
     * @param event 事件
     * @return 事件添加状态
     */
    public boolean offerEvent(Event event) {
        if (!eventQueue.offer(event)) {
            LOGGER.info("Event queue is full when offer new event, send events initiative.");
            sendEventInitiative(eventQueue);
        }
        return eventQueue.offer(event);
    }

    private void sendEventInitiative(BlockingQueue<Event> eventQueue) {
        if (eventQueue.isEmpty()) {
            LOGGER.severe("Event queue is empty when send events initiative.");
            return;
        }
        List<Event> events = new ArrayList<>();
        eventQueue.drainTo(events);
        EventSender.sendEvent(new EventMessage(BootArgsIndexer.getInstanceId(), events));
    }
}
