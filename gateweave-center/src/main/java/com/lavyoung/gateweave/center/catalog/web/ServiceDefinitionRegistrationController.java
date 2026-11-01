package com.lavyoung.gateweave.center.catalog.web;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.catalog.application.RegisterServiceDefinitionUseCase;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogStatus;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperation;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperationParameter;
import com.lavyoung.gateweave.center.catalog.web.request.RegisterServiceDefinitionRequest;
import com.lavyoung.gateweave.center.catalog.web.response.ServiceDefinitionRegistrationResponse;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.web.Result;

/**
 * 提供服务目录定义注册的 HTTP 入站接口。
 *
 * <p>Controller 只负责请求校验、模型转换、应用用例调用和 HTTP 状态选择。
 * 注册幂等性、定义冲突及事务处理均由应用层和持久化适配器负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
@RestController
@RequestMapping("/api/v1/catalog/services")
public class ServiceDefinitionRegistrationController {

    private final RegisterServiceDefinitionUseCase registerServiceDefinitionUseCase;
    private final LocalizedResultFactory resultFactory;

    /**
     * 创建服务定义注册控制器。
     *
     * @param registerServiceDefinitionUseCase 服务定义注册用例
     * @param resultFactory                    本地化统一响应工厂
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public ServiceDefinitionRegistrationController(
            RegisterServiceDefinitionUseCase registerServiceDefinitionUseCase,
            LocalizedResultFactory resultFactory) {
        this.registerServiceDefinitionUseCase = Objects.requireNonNull(
                registerServiceDefinitionUseCase, "registerServiceDefinitionUseCase must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
    }

    /**
     * 原子、幂等地注册完整服务定义。
     *
     * @param request 服务、操作及参数注册请求
     * @return 首次创建返回 HTTP 201，已有相同定义返回 HTTP 200
     * @throws IllegalArgumentException 请求违反服务目录领域约束时抛出
     */
    @PostMapping
    public ResponseEntity<Result<ServiceDefinitionRegistrationResponse>> register(
            @Valid @RequestBody RegisterServiceDefinitionRequest request) {
        ServiceDefinition definition = toServiceDefinition(request);
        CatalogRegistrationOutcome outcome = this.registerServiceDefinitionUseCase.register(definition);
        ServiceDefinitionRegistrationResponse response =
                new ServiceDefinitionRegistrationResponse(definition.serviceId(), outcome);
        HttpStatus status = switch (outcome) {
            case CREATED -> HttpStatus.CREATED;
            case UNCHANGED -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(this.resultFactory.success(response));
    }

    /**
     * 将 HTTP 服务请求转换为服务目录聚合根。
     *
     * @param request HTTP 注册请求
     * @return 服务目录聚合根
     * @throws IllegalArgumentException 操作归属或定义违反领域约束时抛出
     */
    private static ServiceDefinition toServiceDefinition(RegisterServiceDefinitionRequest request) {
        List<ServiceOperation> operations = request.operations().stream()
                .map(operation -> toServiceOperation(request.serviceId(), operation))
                .toList();
        return new ServiceDefinition(
                request.serviceId(),
                request.applicationName(),
                request.protocol(),
                request.namespace(),
                request.version(),
                ServiceCatalogStatus.ACTIVE,
                0L,
                operations);
    }

    /**
     * 将 HTTP 操作请求转换为服务操作领域对象。
     *
     * @param serviceId 所属服务标识
     * @param request   HTTP 操作请求
     * @return 服务操作领域对象
     * @throws IllegalArgumentException 参数位置或名称违反领域约束时抛出
     */
    private static ServiceOperation toServiceOperation(
            String serviceId, RegisterServiceDefinitionRequest.ServiceOperationRequest request) {
        List<ServiceOperationParameter> parameters = request.parameters().stream()
                .map(ServiceDefinitionRegistrationController::toServiceOperationParameter)
                .toList();
        return new ServiceOperation(
                request.operationId(),
                serviceId,
                request.interfaceName(),
                request.operationName(),
                ServiceCatalogStatus.ACTIVE,
                0L,
                parameters);
    }

    /**
     * 将 HTTP 参数请求转换为服务操作参数值对象。
     *
     * @param request HTTP 参数请求
     * @return 服务操作参数值对象
     * @throws IllegalArgumentException 参数位置或文本违反领域约束时抛出
     */
    private static ServiceOperationParameter toServiceOperationParameter(
            RegisterServiceDefinitionRequest.ServiceOperationParameterRequest request) {
        return new ServiceOperationParameter(request.position(), request.name(), request.typeName());
    }
}
