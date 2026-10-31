package com.lavyoung.gateweave.center.gateway.adapter.in.web;

import java.time.Instant;
import java.util.Objects;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;

/**
 * 表示网关算力节点注册成功后的 HTTP 响应数据。
 *
 * <p>响应只暴露稳定的字符串状态，不直接把领域枚举作为外部 HTTP 契约。</p>
 *
 * @param nodeId               节点稳定标识
 * @param gatewayGroupId       所属网关分组标识
 * @param nodeName             节点展示名称
 * @param advertisedAddress    节点公布地址
 * @param status               节点当前状态
 * @param outcome              本次注册结果
 * @param lastHeartbeatAt      最后注册或心跳时间
 * @param currentConfigVersion 节点当前配置版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record GatewayNodeRegistrationResponse(
        String nodeId, String gatewayGroupId, String nodeName, String advertisedAddress,
        String status, String outcome, Instant lastHeartbeatAt, long currentConfigVersion) {

    /**
     * 将节点注册领域结果转换为 HTTP 响应。
     *
     * @param result 节点注册领域结果
     * @return HTTP 注册响应
     * @throws NullPointerException result 为 null 时抛出
     */
    public static GatewayNodeRegistrationResponse from(GatewayNodeRegistrationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        GatewayNode node = result.node();
        return new GatewayNodeRegistrationResponse(
                node.registration().nodeId(),
                node.registration().gatewayGroupId(),
                node.registration().nodeName(),
                node.registration().advertisedAddress().toASCIIString(),
                node.status().name(),
                result.outcome().name(),
                node.lastHeartbeatAt(),
                node.currentConfigVersion());
    }
}
