package com.lavyoung.gateweave.contract.extension.security;

import java.util.Objects;

/**
 * 描述网关已经完成认证的调用主体。
 *
 * <p>该契约只保存已经通过认证机制确认的稳定主体标识，不暴露 JWT Claims、
 * Shiro PrincipalCollection 或其他安全框架类型。</p>
 *
 * @param subject 已经验证的主体标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public record AuthenticatedPrincipal(String subject) {
    /**
     * 校验已经认证的主体标识。
     *
     * @throws NullPointerException     subject 为 null 时抛出
     * @throws IllegalArgumentException subject 为空白时抛出
     */
    public AuthenticatedPrincipal {
        Objects.requireNonNull(subject, "subject must not be null");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }
}
