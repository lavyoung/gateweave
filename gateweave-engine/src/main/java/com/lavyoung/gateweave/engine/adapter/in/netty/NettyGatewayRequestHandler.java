package com.lavyoung.gateweave.engine.adapter.in.netty;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 接收完整 HTTP 请求并调用网关应用用例。
 *
 * <p>该处理器负责 Netty 请求与应用层命令之间的转换、异步用例调用、
 * 应用响应回写及连接生命周期处理，不包含路由或服务调用规则。</p>
 *
 * <p>{@link SimpleChannelInboundHandler} 会在本方法返回后释放请求对象，
 * 因此请求 Mapper 必须在异步处理开始前复制所有需要的数据。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public final class NettyGatewayRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyGatewayRequestHandler.class);
    private static final GatewayResponse BAD_REQUEST_RESPONSE = GatewayResponse.text(400, "Bad Request");
    private static final GatewayResponse INTERNAL_ERROR_RESPONSE = GatewayResponse.text(500, "Internal Server Error");

    private final HandleGatewayRequestUseCase useCase;
    private final NettyGatewayRequestMapper requestMapper;
    private final NettyGatewayResponseMapper responseMapper;

    /**
     * 创建 Netty 网关请求处理器。
     *
     * @param useCase        网关请求处理用例
     * @param requestMapper  Netty 请求转换器
     * @param responseMapper Netty 响应转换器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public NettyGatewayRequestHandler(HandleGatewayRequestUseCase useCase, NettyGatewayRequestMapper requestMapper,
            NettyGatewayResponseMapper responseMapper) {

        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.requestMapper = Objects.requireNonNull(requestMapper, "requestMapper must not be null");
        this.responseMapper = Objects.requireNonNull(responseMapper, "responseMapper must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        HttpVersion protocolVersion = request.protocolVersion();
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        GatewayRequestCommand command;
        try {
            command = requestMapper.map(request);
        } catch (RuntimeException exception) {
            LOGGER.warn("Rejected HTTP request from {}", ctx.channel().remoteAddress(), exception);
            writeResponse(ctx, protocolVersion, false, BAD_REQUEST_RESPONSE);
            return;
        }
        CompletionStage<GatewayResponse> responseStage;
        try {
            responseStage = Objects.requireNonNull(useCase.handle(command), "useCase must not return null");
        } catch (RuntimeException exception) {
            handleApplicationFailure(ctx, protocolVersion, keepAlive, command.requestId(), exception);
            return;
        }
        responseStage.whenComplete((response, failure) -> {
            if (failure != null) {
                handleApplicationFailure(ctx, protocolVersion, keepAlive, command.requestId(), failure);
                return;
            }
            if (response == null) {
                var missingResponseFailure = new NullPointerException("useCase response must not be null");
                handleApplicationFailure(ctx, protocolVersion, keepAlive, command.requestId(), missingResponseFailure);
                return;
            }
            writeResponse(ctx, protocolVersion, keepAlive, response);
        });

    }

    /**
     * 记录应用处理异常并返回不泄漏内部细节的 500 响应。
     *
     * @param context         Netty Channel 上下文
     * @param protocolVersion HTTP 协议版本
     * @param keepAlive       是否保持连接
     * @param requestId       请求追踪标识
     * @param failure         应用处理异常
     */
    private void handleApplicationFailure(ChannelHandlerContext context, HttpVersion protocolVersion,
            boolean keepAlive, String requestId, Throwable failure) {
        LOGGER.error("Gateway request processing failed, requestId={}", requestId, failure);
        writeResponse(context, protocolVersion, keepAlive, INTERNAL_ERROR_RESPONSE);
    }

    /**
     * 将应用响应转换为 Netty 响应并写入连接。
     *
     * @param context         Netty Channel 上下文
     * @param protocolVersion HTTP 协议版本
     * @param keepAlive       是否保持连接
     * @param response        应用层响应
     */
    private void writeResponse(ChannelHandlerContext context, HttpVersion protocolVersion, boolean keepAlive,
            GatewayResponse response) {
        FullHttpResponse httpResponse = responseMapper.map(protocolVersion, response);
        HttpUtil.setKeepAlive(httpResponse, keepAlive);
        ChannelFuture writtenAndFlush = context.writeAndFlush(httpResponse);
        if (!keepAlive) {
            writtenAndFlush.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Unhandled Netty pipeline exception from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
