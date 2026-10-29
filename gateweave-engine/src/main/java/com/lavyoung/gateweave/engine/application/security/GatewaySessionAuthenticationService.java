package com.lavyoung.gateweave.engine.application.security;

import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.domain.route.RouteAuthenticationPolicy;

import java.util.Objects;

/**
 * 根据路由策略认证一次网关请求会话。
 *
 * <p>匿名路由直接允许继续处理；需要认证的路由从请求头解析 Bearer Token，
 * 再通过网关认证扩展验证凭据。该服务不感知 Shiro 和 JWT 技术类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public final class GatewaySessionAuthenticationService {

    private final BearerTokenResolver tokenResolver;
    private final GatewayAuthenticator authenticator;

    /**
     * 创建网关会话认证服务。
     *
     * @param tokenResolver Bearer Token 解析器
     * @param authenticator 网关认证扩展
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewaySessionAuthenticationService(BearerTokenResolver tokenResolver, GatewayAuthenticator authenticator) {
        this.tokenResolver = Objects.requireNonNull(tokenResolver, "tokenResolver must not be null");
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator must not be null");
    }


    /**
     * 判断当前会话是否满足路由的身份认证要求。
     *
     * @param session 已完成路由匹配的网关会话
     * @return 匿名路由或凭据认证成功时返回 true，否则返回 false
     * @throws NullPointerException session 为 null 时抛出
     */
    public boolean authenticate(GatewaySession session) {
        Objects.requireNonNull(session, "session must not be null");
        if (RouteAuthenticationPolicy.ANONYMOUS.equals(session.route().authenticationPolicy())) {
            return true;
        }
        return this.tokenResolver.resolve(session.command().headers())
                .flatMap(this.authenticator::authenticate)
                .isPresent();
    }
}
