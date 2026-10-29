package com.lavyoung.gateweave.contract.extension.security;


import java.util.Optional;

/**
 * 定义网关验证调用凭证并恢复可信主体的扩展契约。
 *
 * <p>具体实现可以使用 JWT、API Key 或其他认证机制，但不得向调用方暴露
 * Shiro、JJWT 或外部身份服务的技术类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public interface GatewayAuthenticator {

    /**
     * 验证 Token 并返回已经确认的调用主体。
     *
     * <p>Token 无效、过期、尚未生效或不属于受信任发行方时返回空结果。
     * 实现不得返回 null，也不得在异常信息和日志中输出完整 Token。</p>
     *
     * @param token 待验证的紧凑 Token
     * @return 认证成功时返回可信主体，认证失败时返回空结果
     * @throws NullPointerException token 为 null 时抛出
     */
    Optional<AuthenticatedPrincipal> authenticate(String token);
}
