package com.lavyoung.gateweave.center.route.domain;

import java.util.Objects;

/**
 * 描述 HTTP 请求参数到目标服务参数位置的映射。
 *
 * <p>参数类型由目标服务操作签名提供，该值对象只维护请求参数名称和目标位置，
 * 避免在路由领域重复保存服务参数类型。</p>
 *
 * @param position    目标服务参数位置，从 0 开始
 * @param requestName HTTP 请求中的参数名称
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RouteParameterMapping(int position, String requestName) {

    /**
     * 校验路由参数映射。
     *
     * @throws NullPointerException     requestName 为 null 时抛出
     * @throws IllegalArgumentException position 为负数或 requestName 为空白时抛出
     */
    public RouteParameterMapping {
        Objects.requireNonNull(requestName, "requestName must not be null");

        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        if (requestName.isBlank()) {
            throw new IllegalArgumentException("requestName must not be blank");
        }
    }
}
