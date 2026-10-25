package com.lavyoung.gateweave.engine.application.command;

import java.net.http.HttpHeaders;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

/**
 * 描述进入网关的、与网络框架无关的 HTTP 请求。
 *
 * @param requestId 本次请求的唯一标识，用于日志与链路追踪
 * @param method    HTTP 请求方法
 * @param uri       请求路径及查询参数
 * @param headers   HTTP 请求头快照
 * @param body      原始请求体
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record GatewayRequestCommand(
        String requestId,
        String method,
        URI uri,
        HttpHeaders headers,
        byte[] body) {

    /**
     * 校验请求属性并建立不可变快照。
     *
     * @throws NullPointerException     请求属性为 null 时抛出
     * @throws IllegalArgumentException requestId 或 method 为空白时抛出
     */
    public GatewayRequestCommand {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(uri, "uri must not be null");

        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }

        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(body, "body must not be null");
        body = Arrays.copyOf(body, body.length);
    }

    /**
     * 返回请求体副本，防止调用方修改内部数组。
     *
     * @return 请求体副本
     */
    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }

}
