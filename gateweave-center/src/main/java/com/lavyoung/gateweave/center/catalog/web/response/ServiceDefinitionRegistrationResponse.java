package com.lavyoung.gateweave.center.catalog.web.response;

import java.util.Objects;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;

/**
 * 表示服务定义注册成功后的 HTTP 响应数据。
 *
 * @param serviceId 稳定的服务标识
 * @param outcome   本次注册结果
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public record ServiceDefinitionRegistrationResponse(String serviceId, CatalogRegistrationOutcome outcome) {

    /**
     * 校验注册响应中的必要数据。
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
