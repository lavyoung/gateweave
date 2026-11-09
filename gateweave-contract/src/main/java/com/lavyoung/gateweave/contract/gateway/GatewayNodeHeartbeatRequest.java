package com.lavyoung.gateweave.contract.gateway;

import java.util.Objects;
import java.util.UUID;

/**
 * 表示网关节点续约时提交的心跳信息。
 *
 * <p>节点标识通过 HTTP 路径传递，请求体只携带本次运行租约和 Engine
 * 已经成功应用的配置版本。Center 使用自己的时钟记录心跳时间。</p>
 *
 * @param leaseId              当前运行实例的租约标识
 * @param currentConfigVersion Engine 已成功应用的配置版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
public record GatewayNodeHeartbeatRequest(UUID leaseId, long currentConfigVersion) {

    /**
     * 校验节点心跳请求的不变量。
     *
     * @throws NullPointerException     leaseId 为 null 时抛出
     * @throws IllegalArgumentException currentConfigVersion 小于零时抛出
     */
    public GatewayNodeHeartbeatRequest {
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        if (currentConfigVersion < 0) {
            throw new IllegalArgumentException("currentConfigVersion must not be negative");
        }
    }
}
