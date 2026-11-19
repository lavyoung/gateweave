package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;

import java.util.Objects;
import java.util.Set;

/**
 * 描述网关节点分页查询条件。
 *
 * <p>节点标识、网关分组和状态集合为空时表示不使用对应条件。
 * 页码从 1 开始，数据库偏移量由应用层统一计算。</p>
 *
 * @param nodeId         节点标识，空字符串表示不限制
 * @param gatewayGroupId 网关分组标识，空字符串表示不限制
 * @param statuses       节点状态集合，空集合表示查询全部状态
 * @param pageRequest    通用分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/17
 */
public record GatewayNodePageQuery(String nodeId, String gatewayGroupId, Set<GatewayNodeStatus> statuses,
        PageRequest pageRequest) {

    /**
     * 规范化筛选条件并校验分页边界。
     *
     * @throws NullPointerException statuses 包含 null 元素或 pageRequest 为 null 时抛出
     */
    public GatewayNodePageQuery {
        nodeId = nodeId == null ? "" : nodeId.strip();
        gatewayGroupId = gatewayGroupId == null ? "" : gatewayGroupId.strip();
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
    }
}
