package com.lavyoung.gateweave.engine.adapter.in.netty;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 将 Netty HTTP 请求转换为网关应用层命令。
 *
 * <p>该类是 Netty 入站适配器的防腐转换边界，负责隔离 Netty 请求、
 * 请求头和 ByteBuf，避免网络框架类型进入应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public final class NettyGatewayRequestMapper {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 将完整的 Netty HTTP 请求转换为应用层命令。
     *
     * @param request 已聚合的 Netty HTTP 请求
     * @return 与 Netty 无关的网关请求命令
     * @throws NullPointerException     request 为 null 时抛出
     * @throws IllegalArgumentException 请求 URI 不合法时抛出
     */
    public GatewayRequestCommand map(FullHttpRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ByteBuf content = request.content();
        byte[] body = ByteBufUtil.getBytes(content, content.readerIndex(), content.readableBytes(), true);
        return new GatewayRequestCommand(
                resolveRequestId(request),
                request.method().name(),
                URI.create(request.uri()),
                mapHeaders(request.headers()),
                body
        );
    }

    /**
     * 获取上游请求标识；上游未提供时生成新标识。
     *
     * @param request Netty HTTP 请求
     * @return 非空白的请求标识
     */
    private static String resolveRequestId(FullHttpRequest request) {
        String requestId = request.headers().get(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    /**
     * 将 Netty 请求头转换为 JDK 不可变请求头。
     *
     * @param nettyHeaders Netty 请求头
     * @return JDK 不可变请求头
     */
    private static HttpHeaders mapHeaders(io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        Map<String, List<String>> valuesByName = nettyHeaders.names().stream()
                .collect(Collectors.toUnmodifiableMap(name -> name,
                        name -> List.copyOf(nettyHeaders.getAll(name))));
        return HttpHeaders.of(valuesByName, (n, v) -> true);
    }
}
