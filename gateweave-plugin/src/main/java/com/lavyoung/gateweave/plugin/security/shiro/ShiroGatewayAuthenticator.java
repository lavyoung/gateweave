package com.lavyoung.gateweave.plugin.security.shiro;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

import java.util.Objects;
import java.util.Optional;

/**
 * 使用 Shiro 完成网关请求的无状态身份认证。
 *
 * <p>每次认证都会创建独立 Subject，禁止在实例字段中保存 Subject，
 * 从而避免 Netty 并发请求之间共享认证状态。</p>
 *
 * <p>Subject 禁止创建 Session。认证完成后立即注销并释放当前认证状态，
 * 不依赖 SecurityUtils 的全局静态 SecurityManager。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public class ShiroGatewayAuthenticator implements GatewayAuthenticator {

    private final SecurityManager securityManager;

    /**
     * 创建 Shiro 网关认证器。
     *
     * @param securityManager Shiro 安全管理器
     * @throws NullPointerException securityManager 为 null 时抛出
     */
    public ShiroGatewayAuthenticator(SecurityManager securityManager) {
        this.securityManager = Objects.requireNonNull(securityManager, "securityManager must not be null");
    }

    @Override
    public Optional<AuthenticatedPrincipal> authenticate(String token) {
        Objects.requireNonNull(token, "token must not be null");
        Subject subject = new Subject.Builder(this.securityManager)
                .sessionCreationEnabled(false)
                .buildSubject();
        try {
            subject.login(new BearerToken(token));
            Object principal = subject.getPrincipal();
            if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
                return Optional.of(authenticatedPrincipal);
            }
            throw new IllegalStateException("Shiro returned an unsupported principal type");
        } catch (AuthenticationException exception) {
            // 打印日Debug日志
            return Optional.empty();
        } finally {
            subject.logout();
        }
    }
}
