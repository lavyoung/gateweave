package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * 定义网关数据面连接控制面所需的节点身份和网络地址。
 *
 * <p>该配置同时服务于节点注册、心跳、注销和后续配置拉取，避免不同生命周期组件分别维护
 * Center 地址和节点身份。</p>
 *
 * @param baseUri           控制面 HTTP 根地址
 * @param nodeId            网关节点稳定标识
 * @param gatewayGroupId    节点所属网关分组标识
 * @param nodeName          节点展示名称
 * @param advertisedAddress 节点对外公布的 HTTP 地址
 * @param requestTimeout    单次控制面 HTTP 请求的最长等待时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@ConfigurationProperties(prefix = "gateweave.control-plane")
public record GatewayControlPlaneProperties(
        URI baseUri, String nodeId, String gatewayGroupId, String nodeName, URI advertisedAddress,
        Duration requestTimeout) {

    /**
     * 校验 Engine 建立控制面连接所需的本地配置。
     *
     * @throws NullPointerException     任一配置为 null 时抛出
     * @throws IllegalArgumentException 控制面根地址不合法或请求超时不为正数时抛出
     */
    public GatewayControlPlaneProperties {
        baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Objects.requireNonNull(nodeName, "nodeName must not be null");
        Objects.requireNonNull(advertisedAddress, "advertisedAddress must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");

        String scheme = baseUri.getScheme();
        boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!baseUri.isAbsolute() || !supportedScheme || baseUri.getHost() == null) {
            throw new IllegalArgumentException("baseUri must be an absolute HTTP or HTTPS URI");
        }
        if (baseUri.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUri must not contain user information");
        }
        if (baseUri.getQuery() != null || baseUri.getFragment() != null) {
            throw new IllegalArgumentException("baseUri must not contain query or fragment");
        }
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be greater than zero");
        }
    }
}
