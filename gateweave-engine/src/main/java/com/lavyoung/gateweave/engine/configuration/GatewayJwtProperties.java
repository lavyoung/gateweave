package com.lavyoung.gateweave.engine.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * 描述网关验证 JWT 所需的安全配置。
 *
 * <p>验证密钥使用 Base64 编码形式从外部配置注入。该类型重写了 toString，
 * 防止配置对象进入日志时泄漏完整密钥。</p>
 *
 * @param issuer                受信任的 Token 发行方
 * @param audience              当前网关接受的 Token 受众
 * @param verificationKeyBase64 Base64 编码的 JWT 验签密钥
 * @param clockSkew             允许的时钟偏差
 * @param maximumTokenLength    接受的 Token 最大字符数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
@ConfigurationProperties(prefix = "gateweave.security.jwt")
public record GatewayJwtProperties(
        String issuer, String audience, String verificationKeyBase64, Duration clockSkew, int maximumTokenLength) {

    /**
     * 校验 JWT 安全配置。
     *
     * @throws NullPointerException     任一对象配置为 null 时抛出
     * @throws IllegalArgumentException 字符串为空白、时钟偏差为负数或长度限制不为正数时抛出
     */
    public GatewayJwtProperties {
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        Objects.requireNonNull(verificationKeyBase64, "verificationKeyBase64 must not be null");
        Objects.requireNonNull(clockSkew, "clockSkew must not be null");

        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (audience.isBlank()) {
            throw new IllegalArgumentException("audience must not be blank");
        }
        if (verificationKeyBase64.isBlank()) {
            throw new IllegalArgumentException("verificationKeyBase64 must not be blank");
        }
        if (clockSkew.isNegative()) {
            throw new IllegalArgumentException("clockSkew must not be negative");
        }
        if (maximumTokenLength <= 0) {
            throw new IllegalArgumentException("maximumTokenLength must be greater than zero");
        }
    }

    /**
     * 返回不包含验签密钥的配置摘要。
     *
     * @return 已隐藏验签密钥的配置摘要
     */
    @Override
    public String toString() {
        return "GatewayJwtProperties[issuer=%s, audience=%s, verificationKeyBase64=PROTECTED]"
                .formatted(this.issuer, this.audience);
    }
}
