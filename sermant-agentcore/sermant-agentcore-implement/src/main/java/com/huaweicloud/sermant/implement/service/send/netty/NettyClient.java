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

package com.huaweicloud.sermant.implement.service.send.netty;

import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.service.ServiceManager;
import com.huaweicloud.sermant.core.service.visibility.api.VisibilityService;
import com.huaweicloud.sermant.core.utils.ThreadFactoryUtils;
import com.huaweicloud.sermant.implement.service.send.netty.pojo.Message;
import com.huaweicloud.sermant.implement.utils.GzipUtils;

import com.google.protobuf.ByteString;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 网关客户端
 *
 * @author lilai
 * @version 0.0.1
 * @since 2022-03-26
 */
public class NettyClient {
    // 运行日志
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final int CONNECT_TIMEOUT = 9000;

    private static final int WAIT_TIME = 30;

    private static final int SEND_INTERNAL_MILLISECOND = 10000;

    private static final int RECONNECT_INTERVAL_SECOND = 10;

    private final BlockingQueue<Message.ServiceData> queue = new ArrayBlockingQueue<>(100);

    private final int writeOrReadWaitTime;

    private final String ip;

    private final int port;

    private final int sendInterval;

    private final int reconnectInterval;

    private Bootstrap bootstrap;

    private EventLoopGroup eventLoopGroup;

    private Channel channel;

    private ScheduledExecutorService executorService;

    private final VisibilityService service = ServiceManager.getService(VisibilityService.class);

    private boolean connectionAvailable = false;

    /**
     * 构造函数
     *
     * @param serverIp   serverIp
     * @param serverPort serverPort
     */
    public NettyClient(String serverIp, int serverPort) {
        ip = serverIp;
        port = serverPort;
        writeOrReadWaitTime = WAIT_TIME;
        sendInterval = SEND_INTERNAL_MILLISECOND;
        reconnectInterval = RECONNECT_INTERVAL_SECOND;
        bind();
    }

    /**
     * 优雅关闭Netty
     */
    public void stop(){
        eventLoopGroup.shutdownGracefully();
    }

    private void bind() {
        eventLoopGroup = new NioEventLoopGroup(new ThreadFactoryUtils("netty-nio-event-loop-group"));
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT).handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel newChannel) {
                    ChannelPipeline pipeline = newChannel.pipeline();
                    pipeline.addLast(new IdleStateHandler(0, 0, writeOrReadWaitTime));
                    pipeline.addLast(new ProtobufVarint32FrameDecoder());
                    pipeline.addLast(new ProtobufDecoder(Message.NettyMessage.getDefaultInstance()));
                    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                    pipeline.addLast(new ProtobufEncoder());
                    pipeline.addLast(new ClientHandler(NettyClient.this));
                }
            });
        doConnect();
    }

    /**
     * 连接服务器
     */
    public synchronized void doConnect() {
        LOGGER.info("Netty do connect.");
        if (channel != null && channel.isActive()) {
            return;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        ChannelFuture connect = bootstrap.connect(ip, port);

        // 添加连接监听
        connect.addListener((ChannelFutureListener)channelFuture -> {
            this.connectionAvailable = channelFuture.isSuccess();

            // 如果连接成功，启动发送线程，循环发送消息队列中的内容
            if (this.connectionAvailable) {
                channel = channelFuture.channel();
                if (channel.isActive()) {
                    Sender sender = new Sender(channel, queue);
                    LOGGER.info("Successfully Connected to server");
                    executorService = Executors.newScheduledThreadPool(1,new ThreadFactoryUtils("netty-send-thread"));
                    executorService.scheduleAtFixedRate(sender, 0, sendInterval, TimeUnit.MILLISECONDS);
                }
                service.reconnectHandler();
            } else {
                // 失败则在X秒后重试连接
                LOGGER.info(String.format(Locale.ROOT, "Failed to connect,try reconnecting after %s seconds ",
                    reconnectInterval));
                channelFuture.channel().eventLoop().schedule(this::doConnect, reconnectInterval, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * 发送数据至服务端
     *
     * @param msg 传输数据
     * @param dataType 数据类型
     */
    public void sendData(byte[] msg, Message.ServiceData.DataType dataType) {
        if (msg == null) {
            LOGGER.warning("Message is null.");
            return;
        }
        byte[] compressMsg = GzipUtils.compress(msg);
        Message.ServiceData serviceData =
            Message.ServiceData.newBuilder().setDataType(dataType).setData(ByteString.copyFrom(compressMsg)).build();
        if (!queue.offer(serviceData)) {
            LOGGER.info(String.format(Locale.ROOT, "Message queue is full, add %s failed.", serviceData.getDataType()));
        }
    }

    /**
     * 发送即时数据到服务端
     *
     * @param msg 传输数据
     * @param dataType 数据类型
     */
    public boolean sendInstantData(byte[] msg, Message.ServiceData.DataType dataType) {
        if (!this.connectionAvailable) {
            LOGGER.warning("Netty connection is not available.");
            return false;
        }
        byte[] compressMsg = GzipUtils.compress(msg);
        Message.ServiceData serviceData =
            Message.ServiceData.newBuilder().setDataType(dataType).setData(ByteString.copyFrom(compressMsg)).build();
        Message.NettyMessage message = Message.NettyMessage.newBuilder()
            .setMessageType(Message.NettyMessage.MessageType.SERVICE_DATA).addServiceData(serviceData).build();
        if (channel == null) {
            LOGGER.warning("Netty channel is null, send instant data failure.");
            return false;
        } else {
            channel.writeAndFlush(message);
            LOGGER.info("Sent instant data successfully by netty.");
            return true;
        }
    }
}
