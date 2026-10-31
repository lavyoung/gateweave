package com.lavyoung.gateweave.contract.web;

import java.util.Objects;

/**
 * 表示 HTTP 失败响应携带的非敏感错误上下文。
 *
 * @param path      发生错误的请求路径
 * @param requestId 请求追踪标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record ErrorDetails(String path, String requestId) {
    /**
     * 创建错误详情。
     *
     * @param path      发生错误的请求路径
     * @param requestId 请求追踪标识
     * @throws NullPointerException     path 或 requestId 为 null 时抛出
     * @throws IllegalArgumentException path 或 requestId 为空白时抛出
     */
    public ErrorDetails {
        path = Objects.requireNonNull(path, "path must not be null");
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
    }
}
