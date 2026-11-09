package com.lavyoung.gateweave.center.gateway.web;

import com.lavyoung.gateweave.center.gateway.application.RenewGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.contract.gateway.GatewayNodeHeartbeatRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 提供网关算力节点租约续期的 HTTP 入站接口。
 *
 * <p>Controller 只提取路径中的节点标识及请求体中的租约信息，
 * 续租时间、租约有效性和配置版本规则由应用层及领域层处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@RestController
@RequestMapping("/api/v1/gateway-nodes")
public class GatewayNodeLeaseController {

    private final RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase;

    /**
     * 创建网关节点租约控制器。
     *
     * @param renewGatewayNodeLeaseUseCase 节点续租用例
     * @throws NullPointerException renewGatewayNodeLeaseUseCase 为 null 时抛出
     */
    public GatewayNodeLeaseController(RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase) {
        this.renewGatewayNodeLeaseUseCase = Objects.requireNonNull(
                renewGatewayNodeLeaseUseCase, "renewGatewayNodeLeaseUseCase must not be null");
    }

    /**
     * 使用当前租约刷新节点心跳。
     *
     * @param nodeId  节点唯一标识
     * @param request 节点心跳请求
     * @return 续租成功时返回 HTTP 204
     * @throws NullPointerException     request 或必要字段为 null 时抛出
     * @throws IllegalArgumentException 节点标识为空白或配置版本为负数时抛出
     */
    @PutMapping("/{nodeId}/lease")
    public ResponseEntity<Void> renewLease(
            @PathVariable("nodeId") String nodeId, @RequestBody GatewayNodeHeartbeatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.renewGatewayNodeLeaseUseCase.renew(nodeId, request.leaseId(), request.currentConfigVersion());
        return ResponseEntity.noContent().build();
    }
}
