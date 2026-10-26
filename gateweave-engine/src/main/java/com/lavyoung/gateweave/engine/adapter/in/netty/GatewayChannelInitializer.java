package com.lavyoung.gateweave.engine.adapter.in.netty;

import java.util.Objects;
import java.util.function.Supplier;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 为网关连接装配 HTTP 入站处理链。
 *
 * <p>该初始化器只负责 Netty 协议组件的组装，不解析业务命令，
 * 也不直接调用路由、过滤器或下游服务。</p>
 *
 * <p>请求处理器通过工厂创建，确保每个 Channel 获得独立实例，
 * 从而不要求处理器必须支持跨连接共享。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GatewayChannelInitializer extends ChannelInitializer<Channel> {

    private final int maxContentLength;
    private final Supplier<? extends ChannelInboundHandler> requestHandlerFactory;


    /**
     * 创建网关 Channel 初始化器。
     *
     * @param maxContentLength      单个完整 HTTP 请求允许的最大字节数
     * @param requestHandlerFactory 网关请求处理器工厂
     * @throws IllegalArgumentException maxContentLength 小于或等于 0 时抛出
     * @throws NullPointerException     requestHandlerFactory 为 null 时抛出
     */
    public GatewayChannelInitializer(int maxContentLength,
            Supplier<? extends ChannelInboundHandler> requestHandlerFactory) {
        if (maxContentLength <= 0) {
            throw new IllegalArgumentException("maxContentLength must be greater than zero");
        }
        this.maxContentLength = maxContentLength;
        this.requestHandlerFactory =
                Objects.requireNonNull(requestHandlerFactory, "requestHandlerFactory must not be null");
    }

    /**
     * 为新连接装配 HTTP 编解码、请求聚合和网关请求处理器。
     *
     * @param channel 新建立的网络连接
     * @throws NullPointerException 请求处理器工厂返回 null 时抛出
     */
    @Override
    protected void initChannel(Channel channel) {
        ChannelInboundHandler requestHandler = Objects.requireNonNull(this.requestHandlerFactory.get(),
                "requestHandlerFactory must not return null");
        channel.pipeline()
                .addLast("httpCodec", new HttpServerCodec())
                .addLast("httpAggregator", new HttpObjectAggregator(this.maxContentLength))
                .addLast("gatewayRequestHandler", requestHandler);
    }
}
