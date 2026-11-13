package com.lavyoung.gateweave.starter.registration.http;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationParameterRequest;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationRequest;
import com.lavyoung.gateweave.contract.catalog.ServiceDefinitionRegistrationResponse;
import com.lavyoung.gateweave.contract.web.Result;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceOperationMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceParameterMetadata;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

/**
 * 通过 Spring HTTP Interface 向 GateWeave 控制面注册服务定义。
 *
 * <p>该适配器负责将 Starter 内部元数据转换为跨进程注册契约，并校验 HTTP 状态、
 * 统一响应、服务标识和幂等注册结果。本层不执行自动重试，注册失败由启动流程快速失败。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
public final class HttpServiceRegistrationClient implements ServiceRegistrationClient {

    private final ServiceRegistrationHttpApi httpApi;

    /**
     * 创建服务注册 HTTP 适配器。
     *
     * @param httpApi Spring 生成的服务注册 HTTP 代理
     * @throws NullPointerException httpApi 为 null 时抛出
     */
    public HttpServiceRegistrationClient(ServiceRegistrationHttpApi httpApi) {
        this.httpApi = Objects.requireNonNull(httpApi, "httpApi must not be null");
    }

    @Override
    public CatalogRegistrationOutcome register(ServiceMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        RegisterServiceDefinitionRequest request = toRequest(metadata);

        try {
            ResponseEntity<Result<ServiceDefinitionRegistrationResponse>> response = this.httpApi.register(request);
            Result<ServiceDefinitionRegistrationResponse> result = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || result == null || !result.success()) {
                String responseCode = result == null ? "missing-result" : result.code();
                throw new IllegalStateException(
                        "Service registration failed with status %d and code %s".formatted(
                                response.getStatusCode().value(), responseCode));
            }

            ServiceDefinitionRegistrationResponse registration = result.data();
            if (!metadata.serviceId().equals(registration.serviceId())) {
                throw new IllegalStateException("Service registration response contains an unexpected serviceId");
            }

            HttpStatus expectedStatus = switch (registration.outcome()) {
                case CREATED -> HttpStatus.CREATED;
                case UNCHANGED -> HttpStatus.OK;
            };
            if (response.getStatusCode() != expectedStatus) {
                throw new IllegalStateException(
                        "Service registration response status does not match outcome: " + registration.outcome());
            }
            return registration.outcome();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Service registration request failed", exception);
        }
    }

    private static RegisterServiceDefinitionRequest toRequest(ServiceMetadata metadata) {
        List<ServiceOperationRequest> operations =
                metadata.operations().stream().map(HttpServiceRegistrationClient::toOperationRequest).toList();
        return new RegisterServiceDefinitionRequest(metadata.serviceId(), metadata.applicationName(),
                metadata.protocol(), metadata.namespace(), metadata.version(), operations);
    }

    private static ServiceOperationRequest toOperationRequest(ServiceOperationMetadata operation) {
        List<ServiceOperationParameterRequest> parameters =
                operation.parameters().stream().map(HttpServiceRegistrationClient::toParameterRequest).toList();
        return new ServiceOperationRequest(operation.operationId(), operation.interfaceName(),
                operation.operationName(), parameters);
    }

    private static ServiceOperationParameterRequest toParameterRequest(ServiceParameterMetadata parameter) {
        return new ServiceOperationParameterRequest(parameter.position(), parameter.name(), parameter.typeName());
    }
}
