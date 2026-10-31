package com.lavyoung.gateweave.center.gateway.adapter.in.web;

import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lavyoung.gateweave.center.adapter.in.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.gateway.application.port.in.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.contract.web.Result;

/**
 * 提供网关算力节点注册的 HTTP 入站接口。
 *
 * <p>Controller 只负责 HTTP 参数校验、模型转换、用例调用和响应状态选择，
 * 不直接访问 Repository，也不处理节点注册业务规则。</p>
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

    /**
     * 创建网关节点注册控制器。
     *
     * @param registerGatewayNodeUseCase 节点注册用例
     * @param resultFactory              本地化统一响应工厂
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeRegistrationController(
            RegisterGatewayNodeUseCase registerGatewayNodeUseCase, LocalizedResultFactory resultFactory) {
        this.registerGatewayNodeUseCase = Objects.requireNonNull(
                registerGatewayNodeUseCase, "registerGatewayNodeUseCase must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
    }

    /**
     * 注册或刷新一个网关算力节点。
     *
     * @param request 节点注册请求
     * @return 首次注册返回 HTTP 201，已有节点刷新返回 HTTP 200
     * @throws IllegalArgumentException 请求内容违反节点领域约束时抛出
     */
    @PostMapping
    public ResponseEntity<Result<GatewayNodeRegistrationResponse>> register(
            @Valid @RequestBody RegisterGatewayNodeRequest request) {
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                request.nodeId(), request.gatewayGroupId(), request.nodeName(), request.advertisedAddress());
        GatewayNodeRegistrationResult registrationResult = this.registerGatewayNodeUseCase.register(registration);
        GatewayNodeRegistrationResponse response = GatewayNodeRegistrationResponse.from(registrationResult);
        HttpStatus status = registrationResult.outcome() == GatewayNodeRegistrationOutcome.REGISTERED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(this.resultFactory.success(response));
    }
}
