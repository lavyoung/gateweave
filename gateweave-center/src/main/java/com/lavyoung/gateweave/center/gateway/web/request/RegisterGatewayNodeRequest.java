package com.lavyoung.gateweave.center.gateway.web.request;

import java.net.URI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 表示网关算力节点注册的 HTTP 请求体。
 *
 * <p>该类型只负责 HTTP 参数接收和基础格式校验。节点状态、心跳时间和配置版本
 * 由控制面决定，不允许客户端提交。</p>
 *
 * @param nodeId            节点稳定标识
 * @param gatewayGroupId    所属网关分组标识
 * @param nodeName          节点展示名称
 * @param advertisedAddress 节点对外公布的 HTTP 地址
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record RegisterGatewayNodeRequest(
        @NotBlank @Size(max = 64) String nodeId,
        @NotBlank @Size(max = 64) String gatewayGroupId,
        @NotBlank @Size(max = 128) String nodeName,
        @NotNull URI advertisedAddress) {
}
