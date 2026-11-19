package com.lavyoung.gateweave.center.gateway.web;

import com.lavyoung.gateweave.center.gateway.application.ReleaseGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.gateway.application.RenewGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.contract.gateway.GatewayNodeHeartbeatRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * 提供网关算力节点租约续期和主动释放的 HTTP 入站接口。
 *
 * <p>Controller 只提取节点标识和租约信息，租约有效性、配置版本及节点状态变更规则
 * 由应用层和领域层处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@RestController
@RequestMapping("/gateway-nodes")
public class GatewayNodeLeaseController {

    private final RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase;
    private final ReleaseGatewayNodeLeaseUseCase releaseGatewayNodeLeaseUseCase;

    /**
     * 创建网关节点租约控制器。
     *
     * @param renewGatewayNodeLeaseUseCase   节点续租用例
     * @param releaseGatewayNodeLeaseUseCase 节点租约释放用例
     * @throws NullPointerException 任一用例为 null 时抛出
     */
    public GatewayNodeLeaseController(RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase,
            ReleaseGatewayNodeLeaseUseCase releaseGatewayNodeLeaseUseCase) {
        this.renewGatewayNodeLeaseUseCase =
                Objects.requireNonNull(renewGatewayNodeLeaseUseCase, "renewGatewayNodeLeaseUseCase must not be null");
        this.releaseGatewayNodeLeaseUseCase = Objects.requireNonNull(releaseGatewayNodeLeaseUseCase,
                "releaseGatewayNodeLeaseUseCase must not be null");
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
    public ResponseEntity<Void> renewLease(@PathVariable("nodeId") String nodeId,
            @RequestBody GatewayNodeHeartbeatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.renewGatewayNodeLeaseUseCase.renew(nodeId, request.leaseId(), request.currentConfigVersion());
        return ResponseEntity.noContent().build();
    }

    /**
     * 使用当前租约主动下线网关节点。
     *
     * @param nodeId  节点唯一标识
     * @param leaseId 当前运行实例持有的租约标识
     * @return 释放完成时返回 HTTP 204
     * @throws NullPointerException     nodeId 或 leaseId 为 null 时抛出
     * @throws IllegalArgumentException nodeId 为空白时抛出
     */
    @DeleteMapping("/{nodeId}/lease")
    public ResponseEntity<Void> releaseLease(@PathVariable("nodeId") String nodeId,
            @RequestParam("leaseId") UUID leaseId) {
        this.releaseGatewayNodeLeaseUseCase.release(nodeId, leaseId);
        return ResponseEntity.noContent().build();
    }
}
