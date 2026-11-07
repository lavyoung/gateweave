package com.lavyoung.gateweave.center.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示控制面维护的一个网关算力节点。
 *
 * <p>节点注册信息描述稳定身份，租约标识区分同一节点的不同运行实例。
 * 状态、心跳时间和配置版本由控制面维护。</p>
 *
 * @param registration         节点注册信息
 * @param status               节点运行状态
 * @param leaseId              当前运行实例的租约标识
 * @param lastHeartbeatAt      最后一次注册或心跳时间
 * @param currentConfigVersion 节点当前加载的配置版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record GatewayNode(GatewayNodeRegistration registration, GatewayNodeStatus status, UUID leaseId,
                          Instant lastHeartbeatAt, long currentConfigVersion) {


    /**
     * 创建网关节点。
     *
     * @param registration         节点注册信息
     * @param status               节点运行状态
     * @param leaseId              当前运行实例的租约标识
     * @param lastHeartbeatAt      最后一次注册或心跳时间
     * @param currentConfigVersion 节点当前加载的配置版本
     * @throws NullPointerException     registration、status、leaseId 或 lastHeartbeatAt 为 null 时抛出
     * @throws IllegalArgumentException currentConfigVersion 为负数时抛出
     */
    public GatewayNode {
        Objects.requireNonNull(registration, "registration must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        Objects.requireNonNull(lastHeartbeatAt, "lastHeartbeatAt must not be null");
        if (currentConfigVersion < 0) {
            throw new IllegalArgumentException("currentConfigVersion must not be negative");
        }
    }

    /**
     * 根据一次注册请求创建具有新租约的在线节点。
     *
     * @param registration 节点注册信息
     * @param registeredAt 控制面接收到注册请求的时间
     * @return 状态为 ONLINE、具有新租约且配置版本为零的节点
     * @throws NullPointerException registration 或 registeredAt 为 null 时抛出
     */
    public static GatewayNode registered(GatewayNodeRegistration registration, Instant registeredAt) {
        return new GatewayNode(
                registration, GatewayNodeStatus.ONLINE, UUID.randomUUID(), registeredAt, 0);
    }
}
