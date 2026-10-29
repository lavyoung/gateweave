package com.lavyoung.gateweave.plugin.security.jwt;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import io.jsonwebtoken.*;

import java.util.Objects;
import java.util.Optional;

/**
 * 使用 JJWT 验证 HS256 Token 并恢复可信调用主体。
 *
 * <p>验证器在创建时构建可复用的不可变解析器，每次调用只处理当前 Token，
 * 不保存 Claims、认证主体或其他请求状态，可以安全服务并发认证请求。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public final class JwtTokenVerifier implements GatewayAuthenticator {

    private final JwtVerificationPolicy policy;
    private final JwtParser parser;

    /**
     * 创建 JWT 验证器。
     *
     * @param policy JWT 验证策略
     * @throws NullPointerException policy 为 null 时抛出
     * @throws RuntimeException     密钥强度或解析器配置不符合 HS256 要求时抛出
     */
    public JwtTokenVerifier(JwtVerificationPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.parser = Jwts.parser()
                .verifyWith(policy.verificationKey())
                .requireIssuer(policy.issuer())
                .requireAudience(policy.audience())
                .clockSkewSeconds(policy.clockSkew().toSeconds())
                .sig()
                .clear()
                .add(Jwts.SIG.HS256)
                .and()
                .build();
    }

    /**
     * 验证紧凑 JWT 并返回其中已经确认的主体。
     *
     * <p>格式错误、签名无效、算法不受支持、时间声明无效、发行方或受众不匹配、
     * 缺少过期时间或主体时均返回空结果。方法不会记录或返回完整 Token。</p>
     *
     * @param token 待验证的紧凑 JWT
     * @return 验证成功时返回可信主体，否则返回空结果
     * @throws NullPointerException token 为 null 时抛出
     */
    @Override
    public Optional<AuthenticatedPrincipal> authenticate(String token) {
        Objects.requireNonNull(token, "token must not be null");
        if (token.isBlank() || token.length() > this.policy.maximumTokenLength()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> signedClaims = this.parser.parseSignedClaims(token);
            Claims claims = signedClaims.getPayload();
            if (claims.getExpiration() == null) {
                return Optional.empty();
            }
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedPrincipal(subject));
        } catch (JwtException | IllegalArgumentException exception) {
            // 要打印日志
            return Optional.empty();
        }
    }
}
