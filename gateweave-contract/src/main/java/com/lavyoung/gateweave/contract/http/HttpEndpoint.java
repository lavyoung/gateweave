package com.lavyoung.gateweave.contract.http;

import java.net.URI;
import java.util.Objects;

/**
 * 表示可以建立 HTTP 或 HTTPS 通信的安全端点。
 *
 * <p>该值对象统一维护跨模块共享的协议地址规则，不包含数据库字段长度、
 * 连接超时、认证信息或具体业务路径等调用方自身规则。</p>
 *
 * @param uri HTTP 或 HTTPS 端点地址
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/12
 */
public record HttpEndpoint(URI uri) {

    /**
     * 校验并保存 HTTP 端点。
     *
     * @throws NullPointerException     uri 为 null 时抛出
     * @throws IllegalArgumentException URI 不是安全、完整的 HTTP 或 HTTPS 地址时抛出
     */
    public HttpEndpoint {
        uri = Objects.requireNonNull(uri, "uri must not be null");
        String scheme = uri.getScheme();
        boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!uri.isAbsolute() || !supportedScheme || uri.getHost() == null) {
            throw new IllegalArgumentException("uri must be an absolute HTTP or HTTPS URI");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("uri must not contain user information");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("uri must not contain query or fragment");
        }
    }
}
