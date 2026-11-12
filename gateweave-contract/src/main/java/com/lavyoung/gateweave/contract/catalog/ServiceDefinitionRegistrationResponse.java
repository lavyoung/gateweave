package com.lavyoung.gateweave.contract.catalog;

import java.util.Objects;

/**
 * 表示控制面完成服务定义注册后的响应数据。
 *
 * <p>首次创建返回 {@link CatalogRegistrationOutcome#CREATED}，已有完全相同的服务定义返回
 * {@link CatalogRegistrationOutcome#UNCHANGED}。定义冲突通过错误响应表达，不增加覆盖状态。</p>
 *
 * @param serviceId 稳定服务标识
 * @param outcome   本次注册结果
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/12
 */
public record ServiceDefinitionRegistrationResponse(String serviceId, CatalogRegistrationOutcome outcome) {

    /**
     * 校验注册响应的必要字段。
     *
     * @throws NullPointerException     serviceId 或 outcome 为 null 时抛出
     * @throws IllegalArgumentException serviceId 为空白时抛出
     */
    public ServiceDefinitionRegistrationResponse {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
    }
}
