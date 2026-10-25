package com.lavyoung.gateweave.engine.application.result;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 描述网关应用层产生的、与网络框架无关的 HTTP 响应。
 *
 * @param statusCode HTTP 状态码
 * @param headers    响应头快照
 * @param body       原始响应体
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record GatewayResponse(
        int statusCode,
        HttpHeaders headers,
        byte[] body) {
    /**
     * 校验响应属性并建立不可变快照。
     *
     * @throws NullPointerException     headers 或 body 为 null 时抛出
     * @throws IllegalArgumentException 状态码不在合法 HTTP 范围时抛出
     */
    public GatewayResponse {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException(
                    "statusCode must be between 100 and 599");
        }

        headers = Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(body, "body must not be null");
        body = Arrays.copyOf(body, body.length);
    }

    /**
     * 创建 UTF-8 纯文本响应。
     *
     * @param statusCode HTTP 状态码
     * @param content    文本响应内容
     * @return UTF-8 纯文本响应
     * @throws NullPointerException     content 为 null 时抛出
     * @throws IllegalArgumentException 状态码不合法时抛出
     */
    public static GatewayResponse text(int statusCode, String content) {
        Objects.requireNonNull(content, "content must not be null");
        var headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("text/plain; charset=UTF-8")),
                (name, value) -> true);

        return new GatewayResponse(statusCode, headers, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 返回响应体副本，防止调用方修改内部数组。
     *
     * @return 响应体副本
     */
    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }

}
