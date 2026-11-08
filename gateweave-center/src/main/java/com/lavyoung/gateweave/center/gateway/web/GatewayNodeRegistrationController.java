package com.lavyoung.gateweave.center.gateway.web;

import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.configuration.GatewayNodeLeaseProperties;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.contract.gateway.GatewayNodeLease;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.contract.web.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 提供网关算力节点注册的 HTTP 入站接口。
 *
 * <p>Controller 负责将注册请求转换为领域模型，并把注册结果转换为 Engine
 * 后续执行心跳和注销操作所需的节点租约。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@RestController
@RequestMapping("/api/v1/gateway-nodes")
public class GatewayNodeRegistrationController {

    private final RegisterGatewayNodeUseCase registerGatewayNodeUseCase;
    private final LocalizedResultFactory resultFactory;
    private final GatewayNodeLeaseProperties leaseProperties;

    /**
     * 创建网关节点注册控制器。
     *
     * @param registerGatewayNodeUseCase 节点注册用例
     * @param resultFactory              本地化统一响应工厂
     * @param leaseProperties            节点租约时间配置
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeRegistrationController(
            RegisterGatewayNodeUseCase registerGatewayNodeUseCase, LocalizedResultFactory resultFactory,
            GatewayNodeLeaseProperties leaseProperties) {
        this.registerGatewayNodeUseCase = Objects.requireNonNull(
                registerGatewayNodeUseCase, "registerGatewayNodeUseCase must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
        this.leaseProperties = Objects.requireNonNull(leaseProperties, "leaseProperties must not be null");
    }

    /**
     * 注册或刷新一个网关算力节点并返回本次运行租约。
     *
     * @param request 节点注册请求
     * @return 首次注册返回 HTTP 201，已有节点刷新返回 HTTP 200，响应体携带新的节点租约
     * @throws NullPointerException     request 或必要字段为 null 时抛出
     * @throws IllegalArgumentException 请求内容违反节点领域约束或租约约束时抛出
     */
    @PostMapping
    public ResponseEntity<Result<GatewayNodeLease>> register(@RequestBody RegisterGatewayNodeRequest request) {
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                request.nodeId(), request.gatewayGroupId(), request.nodeName(), request.advertisedAddress());
        GatewayNodeRegistrationResult registrationResult = this.registerGatewayNodeUseCase.register(registration);
        GatewayNode node = registrationResult.node();
        GatewayNodeLease lease = new GatewayNodeLease(node.registration().nodeId(), node.leaseId(),
                this.leaseProperties.heartbeatInterval(), this.leaseProperties.leaseTimeout());
        HttpStatus status = registrationResult.outcome() == GatewayNodeRegistrationOutcome.REGISTERED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(this.resultFactory.success(lease));
    }
}
