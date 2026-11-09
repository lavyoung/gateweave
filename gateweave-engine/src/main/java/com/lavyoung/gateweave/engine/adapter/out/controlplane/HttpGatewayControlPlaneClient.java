package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.gateway.GatewayNodeHeartbeatRequest;
import com.lavyoung.gateweave.contract.gateway.GatewayNodeLease;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.contract.web.Result;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 通过 Spring HTTP Interface 访问 GateWeave 控制面。
 *
 * <p>HTTP 请求构建、路径变量编码和 JSON 编解码由 Spring 负责；该适配器只把 HTTP 响应
 * 转换为应用层端口定义的节点租约和配置查询语义。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class HttpGatewayControlPlaneClient implements GatewayControlPlaneClient {

    private final ControlPlaneHttpApi controlPlaneHttpApi;

    /**
     * 创建控制面 HTTP 出站适配器。
     *
     * @param controlPlaneHttpApi Spring 生成的控制面 HTTP 代理
     * @throws NullPointerException controlPlaneHttpApi 为 null 时抛出
     */
    public HttpGatewayControlPlaneClient(ControlPlaneHttpApi controlPlaneHttpApi) {
        this.controlPlaneHttpApi = Objects.requireNonNull(controlPlaneHttpApi, "controlPlaneHttpApi must not be null");
    }

    @Override
    public GatewayNodeLease registerNode(RegisterGatewayNodeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            ResponseEntity<Result<GatewayNodeLease>> response = this.controlPlaneHttpApi.registerNode(request);
            Result<GatewayNodeLease> result = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || result == null || !result.success()) {
                String responseCode = result == null ? "missing-result" : result.code();
                throw new IllegalStateException("Gateway node registration failed with status %d and code %s"
                        .formatted(response.getStatusCode().value(), responseCode));
            }
            return result.data();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Gateway node registration request failed", exception);
        }
    }

    @Override
    public void renewNodeLease(String nodeId, UUID leaseId, long currentConfigVersion) {
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null").strip();
        if (nodeId.isEmpty()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        GatewayNodeHeartbeatRequest heartbeatRequest = new GatewayNodeHeartbeatRequest(leaseId, currentConfigVersion);
        try {
            ResponseEntity<Void> response = this.controlPlaneHttpApi.renewNodeLease(nodeId, heartbeatRequest);
            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new IllegalStateException("Gateway node lease renewal failed with status %d"
                        .formatted(response.getStatusCode().value()));
            }
        } catch (RestClientException exception) {
            throw new IllegalStateException("Gateway node lease renewal request failed", exception);
        }
    }

    @Override
    public Optional<GatewayConfigurationSnapshot> findLatestPublishedConfiguration(String gatewayGroupId) {
        String groupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        if (groupId.isEmpty()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        try {
            ResponseEntity<Result<GatewayConfigurationSnapshot>> response =
                    this.controlPlaneHttpApi.findLatestPublishedConfiguration(groupId);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return Optional.empty();
            }
            Result<GatewayConfigurationSnapshot> result = response.getBody();
            if (response.getStatusCode() != HttpStatus.OK || result == null || !result.success()) {
                String responseCode = result == null ? "missing-result" : result.code();
                throw new IllegalStateException("Gateway configuration query failed with status %d and code %s"
                        .formatted(response.getStatusCode().value(), responseCode));
            }
            return Optional.of(result.data());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Gateway configuration query request failed", exception);
        }
    }
}
