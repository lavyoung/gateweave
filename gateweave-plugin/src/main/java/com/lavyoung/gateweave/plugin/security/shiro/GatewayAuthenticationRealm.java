package com.lavyoung.gateweave.plugin.security.shiro;

import java.util.Objects;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;


/**
 * 将 Shiro Bearer Token 适配到网关认证契约。
 *
 * <p>Realm 不负责解析 JWT，也不依赖具体的认证技术实现。只有
 * {@link GatewayAuthenticator} 返回可信主体后，Realm 才会创建 Shiro 认证信息。</p>
 *
 * <p>凭据验证已经由 GatewayAuthenticator 完成，因此不再使用 Shiro 的字符串凭据比较。
 * 认证信息中也不会保存原始 Token，避免凭据进入认证缓存或调试输出。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public final class GatewayAuthenticationRealm extends AuthenticatingRealm {

    private final GatewayAuthenticator authenticator;

    /**
     * 创建网关认证 Realm。
     *
     * @param authenticator 网关认证扩展
     * @throws NullPointerException authenticator 为 null 时抛出
     */
    public GatewayAuthenticationRealm(GatewayAuthenticator authenticator) {
        // 认证已经由 authenticate 完成，失败时根本不会产生 AuthenticationInfo
        super(new AllowAllCredentialsMatcher());
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator must not be null");
        setAuthenticationTokenClass(BearerToken.class);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        BearerToken bearerToken = (BearerToken) token;
        AuthenticatedPrincipal principal = this.authenticator.authenticate(bearerToken.getToken())
                .orElseThrow(() -> new IncorrectCredentialsException("bearer token is invalid"));
        // null credentials：避免将原始 JWT 保存在认证信息中
        return new SimpleAuthenticationInfo(principal, null, this.getName());
    }
}
