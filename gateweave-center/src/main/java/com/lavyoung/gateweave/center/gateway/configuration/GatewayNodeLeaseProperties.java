package com.lavyoung.gateweave.center.gateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

/**
 * 定义控制面授予网关节点的租约时间策略。
 *
 * <p>控制面负责决定心跳发送周期和租约失效时间，并在节点注册成功后将这两个值
 * 返回给 Engine。Engine 必须以注册响应为准，不能维护另一套本地租约策略。</p>
 *
 * @param heartbeatInterval 建议 Engine 发送心跳的时间间隔
 * @param leaseTimeout      Center 判定节点租约失效的超时时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/07
 */
@ConfigurationProperties(prefix = "gateweave.gateway-node.lease")
public record GatewayNodeLeaseProperties(
        @DefaultValue("10s") Duration heartbeatInterval, @DefaultValue("30s") Duration leaseTimeout) {

    /**
     * 校验租约配置的必要字段。
     *
     * <p>时间范围和先后关系由跨进程契约 {@code GatewayNodeLease} 统一校验，
     * 此处不复制相同业务规则。</p>
     *
     * @throws NullPointerException 任一配置为 null 时抛出
     */
    public GatewayNodeLeaseProperties {
        Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
        Objects.requireNonNull(leaseTimeout, "leaseTimeout must not be null");
    }
}
