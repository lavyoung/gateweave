package com.lavyoung.gateweave.center.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 定义控制面对外 HTTP API 的全局路径配置。
 *
 * <p>业务 Controller 只声明资源路径，API 前缀由 Spring MVC 在注册处理器时统一添加，
 * 避免版本前缀分散在各个 Controller 中。</p>
 *
 * @param apiPrefix     控制面对外 HTTP API 的统一前缀
 * @param allowedOrigins 允许访问控制面 API 的浏览器来源
 * @param corsMaxAge    浏览器缓存 CORS 预检结果的最长时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
@ConfigurationProperties("gateweave.web")
public record ControlPlaneWebProperties(@DefaultValue("/api/v1") String apiPrefix,
        @DefaultValue({"http://localhost:5173", "http://127.0.0.1:5173"}) List<String> allowedOrigins,
        @DefaultValue("30m") Duration corsMaxAge) {

    /**
     * 校验并规范化控制面 API 前缀。
     *
     * @throws NullPointerException     任一配置为 null 时抛出
     * @throws IllegalArgumentException API 前缀、浏览器来源或预检缓存时间不合法时抛出
     */
    public ControlPlaneWebProperties {
        apiPrefix = Objects.requireNonNull(apiPrefix, "apiPrefix must not be null").strip();
        if (apiPrefix.length() < 2 || !apiPrefix.startsWith("/") || apiPrefix.endsWith("/")) {
            throw new IllegalArgumentException("apiPrefix must start with '/' and must not end with '/'");
        }
        if (apiPrefix.contains("//") || apiPrefix.contains("?") || apiPrefix.contains("#")) {
            throw new IllegalArgumentException("apiPrefix must be a normalized path without query or fragment");
        }

        List<String> normalizedOrigins = Objects.requireNonNull(allowedOrigins, "allowedOrigins must not be null")
                .stream().map(origin -> Objects.requireNonNull(origin, "allowedOrigins must not contain null").strip())
                .toList();
        if (normalizedOrigins.isEmpty() || normalizedOrigins.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("allowedOrigins must not be empty or contain blank origins");
        }
        for (String origin : normalizedOrigins) {
            URI originUri = URI.create(origin);
            String scheme = originUri.getScheme();
            boolean httpOrigin = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpOrigin || originUri.getHost() == null || !originUri.getPath().isEmpty()
                    || originUri.getQuery() != null || originUri.getFragment() != null
                    || originUri.getUserInfo() != null) {
                throw new IllegalArgumentException("allowedOrigins must contain only HTTP origins without paths");
            }
        }
        allowedOrigins = List.copyOf(normalizedOrigins);

        corsMaxAge = Objects.requireNonNull(corsMaxAge, "corsMaxAge must not be null");
        if (corsMaxAge.isZero() || corsMaxAge.isNegative()) {
            throw new IllegalArgumentException("corsMaxAge must be greater than zero");
        }
    }
}
