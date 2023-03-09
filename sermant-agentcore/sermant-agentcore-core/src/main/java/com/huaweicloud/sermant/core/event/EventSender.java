package com.huaweicloud.sermant.core.event;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.service.send.api.GatewayClient;

import java.util.logging.Logger;

/**
 * 事件发送工具类
 *
 * @author luanwenfei
 * @since 2023-03-07
 */
public class EventSender {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final GatewayClient GATEWAY_CLIENT = ServiceManager.getService(GatewayClient.class);

    private static final int EVENT_DATA_VALUE = 1;

    private EventSender() {
    }

    /**
     * 发送事件消息
     *
     * @param eventMessage 事件消息
     */
    public static void sendEvent(EventMessage eventMessage) {
        if (GATEWAY_CLIENT.sendImmediately(eventMessage, EVENT_DATA_VALUE)) {
            LOGGER.info("Send events successful. MetaHash:" + eventMessage.getMetaHash());
        } else {
            // todo 定义两种策略 丢弃｜日志输出 默认日志输出
            LOGGER.warning(eventMessage.toString());
        }
    }
}