package com.lavyoung.gateweave.contract.gateway;

import java.net.URI;
import java.util.Objects;

/**
 * 表示网关节点向控制面提交的注册请求。
 *
 * <p>该类型是 Engine 与 Center 共享的跨进程契约，只声明必需字段，不复制 Center 领域模型中的
 * 标识长度、节点名称长度和公布地址协议规则。</p>
 *
 * @param nodeId            节点稳定标识
 * @param gatewayGroupId    所属网关分组标识
 * @param nodeName          节点展示名称
 * @param advertisedAddress 节点对外公布地址
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public record RegisterGatewayNodeRequest(
        String nodeId, String gatewayGroupId, String nodeName, URI advertisedAddress) {

    /**
     * 校验跨进程请求的必需字段是否存在。
     *
     * @throws NullPointerException 任一必需字段为 null 时抛出
     */
    public RegisterGatewayNodeRequest {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Objects.requireNonNull(nodeName, "nodeName must not be null");
        Objects.requireNonNull(advertisedAddress, "advertisedAddress must not be null");
    }
}
