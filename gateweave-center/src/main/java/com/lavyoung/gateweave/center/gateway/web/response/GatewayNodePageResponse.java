package com.lavyoung.gateweave.center.gateway.web.response;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 表示管理 API 返回的网关节点分页数据。
 *
 * <p>响应模型只暴露管理页面需要的节点信息，不返回租约标识等内部运行数据。</p>
 *
 * @param nodes         当前页网关节点
 * @param pageNumber    当前页码
 * @param pageSize      每页节点数量
 * @param totalElements 节点总数
 * @param totalPages    总页数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
public record GatewayNodePageResponse(List<GatewayNodeResponse> nodes, int pageNumber, int pageSize, long totalElements,
        long totalPages) {

    /**
     * 建立节点响应集合的不可变快照。
     *
     * @throws NullPointerException nodes 为 null 或包含 null 元素时抛出
     */
    public GatewayNodePageResponse {
        nodes = List.copyOf(nodes);
    }

    /**
     * 将应用层分页结果转换为 HTTP 响应。
     *
     * @param page 应用层网关节点分页结果
     * @return HTTP 网关节点分页响应
     * @throws NullPointerException page 为 null 时抛出
     */
    public static GatewayNodePageResponse from(PageResult<GatewayNode> page) {
        Objects.requireNonNull(page, "page must not be null");

        PageRequest pageRequest = page.pageRequest();
        List<GatewayNodeResponse> nodes = page.content().stream().map(GatewayNodeResponse::from).toList();
        return new GatewayNodePageResponse(nodes, pageRequest.pageNumber(), pageRequest.pageSize(),
                page.totalElements(), page.totalPages());
    }

    /**
     * 表示一条网关节点的 HTTP 响应。
     *
     * @param nodeId               节点标识
     * @param gatewayGroupId       网关分组标识
     * @param nodeName             节点名称
     * @param advertisedAddress    节点对外地址
     * @param status               节点运行状态
     * @param lastHeartbeatAt      最后心跳时间
     * @param currentConfigVersion 当前加载的配置版本
     */
    public record GatewayNodeResponse(String nodeId, String gatewayGroupId, String nodeName, String advertisedAddress,
            String status, Instant lastHeartbeatAt, long currentConfigVersion) {
        private static GatewayNodeResponse from(GatewayNode node) {
            GatewayNodeRegistration registration = node.registration();
            return new GatewayNodeResponse(registration.nodeId(), registration.gatewayGroupId(),
                    registration.nodeName(), registration.advertisedAddress().toASCIIString(), node.status().name(),
                    node.lastHeartbeatAt(), node.currentConfigVersion());
        }
    }
}
