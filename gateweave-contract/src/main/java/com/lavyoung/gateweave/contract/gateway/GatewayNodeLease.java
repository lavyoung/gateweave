package com.lavyoung.gateweave.contract.gateway;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示控制面授予网关节点的一次运行租约。
 *
 * <p>每次节点注册都会获得新的租约标识。后续心跳和主动注销必须携带该标识，
 * 避免已经退出的旧进程覆盖新实例的在线状态。</p>
 *
 * @param nodeId            节点稳定标识
 * @param leaseId           本次运行实例的租约标识
 * @param heartbeatInterval Engine 建议发送心跳的时间间隔
 * @param leaseTimeout      Center 判定租约失效的超时时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/07
 */
public record GatewayNodeLease(String nodeId, UUID leaseId, Duration heartbeatInterval, Duration leaseTimeout) {

    /**
     * 校验节点租约协议的不变量。
     *
     * @throws NullPointerException     任一字段为 null 时抛出
     * @throws IllegalArgumentException nodeId 为空白、心跳间隔不是正数，
     *                                  或租约超时时间不大于心跳间隔时抛出
     */
    public GatewayNodeLease {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
        Objects.requireNonNull(leaseTimeout, "leaseTimeout must not be null");
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            throw new IllegalArgumentException("heartbeatInterval must be positive");
        }
        if (leaseTimeout.compareTo(heartbeatInterval) <= 0) {
            throw new IllegalArgumentException("leaseTimeout must be greater than heartbeatInterval");
        }
    }
}
