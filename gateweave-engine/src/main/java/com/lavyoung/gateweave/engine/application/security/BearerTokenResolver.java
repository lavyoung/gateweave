package com.lavyoung.gateweave.engine.application.security;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 HTTP Authorization 请求头中解析 Bearer Token。
 *
 * <p>解析器只识别标准 Bearer Scheme，不验证 Token 格式、签名或声明。
 * Token 的可信性仍由 GatewayAuthenticator 判断。</p>
 *
 * <p>同一请求携带多个 Authorization 值时拒绝解析，避免代理、客户端与网关
 * 对凭据选择规则理解不一致。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public final class BearerTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Pattern BEARER_PATTERN = Pattern.compile("^Bearer +([^\\s]+)$", Pattern.CASE_INSENSITIVE);

    /**
     * 从请求头中解析唯一的 Bearer Token。
     *
     * @param headers HTTP 请求头快照
     * @return 请求头合法时返回 Token；请求头缺失、重复或格式错误时返回空结果
     * @throws NullPointerException headers 为 null 时抛出
     */
    public Optional<String> resolve(HttpHeaders headers) {
        Objects.requireNonNull(headers, "headers must not be null");
        List<String> authorizationValues = headers.allValues(AUTHORIZATION_HEADER);
        if (authorizationValues.size() != 1) {
            return Optional.empty();
        }
        Matcher matcher = BEARER_PATTERN.matcher(authorizationValues.get(0).strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
