package com.lavyoung.gateweave.center.gateway.domain;

import java.net.URI;
import java.util.Objects;

/**
 * 表示一次网关节点注册提交的稳定身份和访问地址。
 *
 * <p>该值对象不包含节点状态、心跳时间和配置版本。这些字段由控制面决定，
 * 不能由外部注册请求直接指定。</p>
 *
 * @param nodeId            节点稳定标识
 * @param gatewayGroupId    所属网关分组标识
 * @param nodeName          节点展示名称
 * @param advertisedAddress 节点对外公布的 HTTP 地址
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record GatewayNodeRegistration(String nodeId, String gatewayGroupId, String nodeName, URI advertisedAddress) {
    private static final int MAX_IDENTIFIER_LENGTH = 64;
    private static final int MAX_NODE_NAME_LENGTH = 128;
    private static final int MAX_ADDRESS_LENGTH = 512;

    /**
     * 创建节点注册信息。
     *
     * @param nodeId            节点稳定标识
     * @param gatewayGroupId    所属网关分组标识
     * @param nodeName          节点展示名称
     * @param advertisedAddress 节点对外公布的 HTTP 地址
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException 文本为空白、超过数据库长度，或者地址格式不合法时抛出
     */
    public GatewayNodeRegistration {
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null").strip();
        gatewayGroupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        nodeName = Objects.requireNonNull(nodeName, "nodeName must not be null").strip();
        if (nodeId.isEmpty()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (nodeId.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("nodeId length must not exceed " + MAX_IDENTIFIER_LENGTH);
        }
        if (gatewayGroupId.isEmpty()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        if (gatewayGroupId.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("gatewayGroupId length must not exceed " + MAX_IDENTIFIER_LENGTH);
        }
        if (nodeName.isEmpty()) {
            throw new IllegalArgumentException("nodeName must not be blank");
        }
        if (nodeName.length() > MAX_NODE_NAME_LENGTH) {
            throw new IllegalArgumentException("nodeName length must not exceed " + MAX_NODE_NAME_LENGTH);
        }
        advertisedAddress = requireAdvertisedAddress(advertisedAddress);
    }

    private static URI requireAdvertisedAddress(URI value) {
        URI checkedValue = Objects.requireNonNull(value, "advertisedAddress must not be null");
        String scheme = checkedValue.getScheme();
        boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!checkedValue.isAbsolute() || !supportedScheme || checkedValue.getHost() == null) {
            throw new IllegalArgumentException("advertisedAddress must be an absolute HTTP or HTTPS URI");
        }
        if (checkedValue.getUserInfo() != null) {
            throw new IllegalArgumentException("advertisedAddress must not contain user information");
        }
        if (checkedValue.getQuery() != null || checkedValue.getFragment() != null) {
            throw new IllegalArgumentException("advertisedAddress must not contain query or fragment");
        }
        if (checkedValue.toASCIIString().length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("advertisedAddress length must not exceed " + MAX_ADDRESS_LENGTH);
        }
        return checkedValue;
    }
}
