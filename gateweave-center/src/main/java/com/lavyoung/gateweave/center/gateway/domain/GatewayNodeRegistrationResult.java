package com.lavyoung.gateweave.center.gateway.domain;

import java.util.Objects;

/**
 * 表示节点幂等注册完成后的领域结果。
 *
 * @param node    注册或刷新的网关节点
 * @param outcome 本次注册产生的实际结果
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record GatewayNodeRegistrationResult(
        GatewayNode node,
        GatewayNodeRegistrationOutcome outcome) {

    /**
     * 创建节点注册结果。
     *
     * @param node    注册或刷新的网关节点
     * @param outcome 本次注册产生的实际结果
     * @throws NullPointerException node 或 outcome 为 null 时抛出
     */
    public GatewayNodeRegistrationResult {
        node = Objects.requireNonNull(node, "node must not be null");
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
    }
}
