package com.lavyoung.gateweave.plugin.security.jwt;

import java.time.Duration;
import java.util.Objects;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

/**
 * 描述网关验证 HS256 JWT 时使用的安全策略。
 *
 * <p>该策略由运行进程的装配层根据外部配置创建，JWT 验证器只消费已经完成
 * 类型转换和基础校验的配置，不自行读取环境变量或配置文件。</p>
 *
 * @param verificationKey   JWT 签名验证密钥
 * @param issuer            允许的 Token 发行方
 * @param audience          当前网关对应的 Token 受众
 * @param clockSkew         验证时间声明时允许的时钟偏差
 * @param maximumTokenLength 允许接收的紧凑 JWT 最大字符数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public record JwtVerificationPolicy(SecretKey verificationKey, String issuer, String audience, Duration clockSkew,
                                    int maximumTokenLength) {

    /**
     * 使用原始 HS256 密钥字节创建验证策略。
     *
     * <p>密钥强度由 JJWT 官方能力校验，不足 256 位时立即拒绝。</p>
     *
     * @param verificationKeyBytes HS256 验签密钥字节
     * @param issuer               允许的 Token 发行方
     * @param audience             当前网关对应的 Token 受众
     * @param clockSkew            验证时间声明时允许的时钟偏差
     * @param maximumTokenLength   允许接收的紧凑 JWT 最大字符数
     * @throws NullPointerException verificationKeyBytes 为 null 时抛出
     * @throws io.jsonwebtoken.security.WeakKeyException 密钥不足 256 位时抛出
     */
    public JwtVerificationPolicy(byte[] verificationKeyBytes, String issuer, String audience, Duration clockSkew,
                                 int maximumTokenLength) {
        this(Keys.hmacShaKeyFor(Objects.requireNonNull(
                        verificationKeyBytes, "verificationKeyBytes must not be null")),
                issuer, audience, clockSkew, maximumTokenLength);
    }

    /**
     * 校验 JWT 验证策略。
     *
     * @throws NullPointerException     任一引用属性为 null 时抛出
     * @throws IllegalArgumentException 文本属性为空白、时钟偏差为负数或长度限制不为正数时抛出
     */
    public JwtVerificationPolicy {
        Objects.requireNonNull(verificationKey, "verificationKey must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        Objects.requireNonNull(clockSkew, "clockSkew must not be null");

        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (audience.isBlank()) {
            throw new IllegalArgumentException("audience must not be blank");
        }
        if (clockSkew.isNegative()) {
            throw new IllegalArgumentException("clockSkew must not be negative");
        }
        if (maximumTokenLength <= 0) {
            throw new IllegalArgumentException("maximumTokenLength must be greater than zero");
        }
    }
}
