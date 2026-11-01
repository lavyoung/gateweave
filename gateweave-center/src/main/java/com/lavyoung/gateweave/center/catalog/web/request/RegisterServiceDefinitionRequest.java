package com.lavyoung.gateweave.center.catalog.web.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 表示服务及其操作定义的 HTTP 注册请求。
 *
 * <p>该请求只接收服务提供方可以声明的元数据。服务状态和修订号由控制面维护，
 * 不允许注册客户端直接提交。</p>
 *
 * @param serviceId       稳定的服务标识
 * @param applicationName 服务所属应用名称
 * @param protocol        服务调用协议
 * @param namespace       服务命名空间
 * @param version         服务版本
 * @param operations      服务提供的操作定义
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public record RegisterServiceDefinitionRequest(
        @NotBlank @Size(max = 64) String serviceId,
        @NotBlank @Size(max = 128) String applicationName,
        @NotBlank @Size(max = 32) String protocol,
        @NotNull @Size(max = 128) String namespace,
        @NotNull @Size(max = 64) String version,
        @NotNull List<@Valid ServiceOperationRequest> operations) {

    /**
     * 表示一个可被网关路由引用的服务操作。
     *
     * @param operationId   稳定的操作标识
     * @param interfaceName 服务接口名称
     * @param operationName 服务方法名称
     * @param parameters    按调用位置排列的参数
     */
    public record ServiceOperationRequest(
            @NotBlank @Size(max = 64) String operationId,
            @NotBlank @Size(max = 256) String interfaceName,
            @NotBlank @Size(max = 128) String operationName,
            @NotNull List<@Valid ServiceOperationParameterRequest> parameters) {
    }

    /**
     * 表示服务操作签名中的一个参数。
     *
     * @param position 参数位置，从零开始
     * @param name     参数名称
     * @param typeName 参数类型全限定名
     */
    public record ServiceOperationParameterRequest(
            @NotNull @PositiveOrZero Integer position,
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 256) String typeName) {
    }
}
