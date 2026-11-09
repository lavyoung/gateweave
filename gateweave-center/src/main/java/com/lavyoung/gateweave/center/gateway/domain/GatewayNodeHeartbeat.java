package com.lavyoung.gateweave.center.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示控制面接收到的一次网关节点心跳。
 *
 * <p>该领域命令将 HTTP 路径中的节点标识、请求契约中的租约和配置版本，
 * 与控制面生成的可信接收时间组合起来，供应用服务和持久化端口共同使用。</p>
 *
 * @param nodeId               节点稳定标识
 * @param leaseId              当前运行实例的租约标识
 * @param receivedAt           控制面接收到心跳的时间
 * @param currentConfigVersion Engine 已成功应用的配置版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
public record GatewayNodeHeartbeat(String nodeId, UUID leaseId, Instant receivedAt, long currentConfigVersion) {

    /**
     * 校验节点心跳领域命令的不变量。
     *
     * <p>节点标识的数据库长度由节点注册模型统一维护，此处不复制相同常量；
     * 不存在或不匹配的节点标识将在原子续约时统一拒绝。</p>
     *
     * @throws NullPointerException     nodeId、leaseId 或 receivedAt 为 null 时抛出
     * @throws IllegalArgumentException nodeId 为空白或 currentConfigVersion 为负数时抛出
     */
    public GatewayNodeHeartbeat {
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null").strip();
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        if (nodeId.isEmpty()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (currentConfigVersion < 0) {
            throw new IllegalArgumentException("currentConfigVersion must not be negative");
        }
    }
}
