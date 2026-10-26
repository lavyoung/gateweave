package com.lavyoung.gateweave.engine.adapter.in.netty;

import java.util.Objects;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

import com.lavyoung.gateweave.engine.application.result.GatewayResponse;

/**
 * 将网关应用层响应转换为 Netty HTTP 响应。
 *
 * <p>该类负责隔离应用层响应模型和 Netty 响应类型，同时保证完整响应的
 * Content-Length 与实际响应体长度一致。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class NettyGatewayResponseMapper {

    /**
     * 将应用层响应转换为指定 HTTP 版本的 Netty 完整响应。
     *
     * @param protocolVersion 请求使用的 HTTP 协议版本
     * @param response        网关应用层响应
     * @return Netty 完整 HTTP 响应
     * @throws NullPointerException protocolVersion 或 response 为 null 时抛出
     */
    public FullHttpResponse map(HttpVersion protocolVersion, GatewayResponse response) {
        Objects.requireNonNull(protocolVersion, "protocolVersion must not be null");
        Objects.requireNonNull(response, "response must not be null");

        byte[] body = response.body();
        DefaultFullHttpResponse nettyResponse = new DefaultFullHttpResponse(protocolVersion,
                HttpResponseStatus.valueOf(response.statusCode()), Unpooled.wrappedBuffer(body));
        response.headers().map().forEach((name, values) -> nettyResponse.headers().set(name, values));
        HttpUtil.setTransferEncodingChunked(nettyResponse, false);
        HttpUtil.setContentLength(nettyResponse, body.length);
        return nettyResponse;
    }
}
