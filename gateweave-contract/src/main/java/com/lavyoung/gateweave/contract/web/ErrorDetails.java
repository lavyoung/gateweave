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
        path = requireText(path, "path");
        requestId = requireText(requestId, "requestId");
    }

    private static String requireText(String value, String name) {
        String checkedValue = Objects.requireNonNull(value, name + " must not be null");
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checkedValue;
    }
}
