package com.lavyoung.gateweave.contract.catalog;

import java.util.List;
import java.util.Objects;

/**
 * 表示服务提供方向控制面提交的完整服务定义。
 *
 * <p>该契约一次携带服务、操作和参数信息，使控制面能够原子、幂等地完成服务目录注册。
 * 服务状态、修订号、路由和认证策略由控制面维护，不允许注册客户端提交。</p>
 *
 * @param serviceId       稳定服务标识
 * @param applicationName 服务所属应用名称
 * @param protocol        服务调用协议
 * @param namespace       服务命名空间，可以为空字符串
 * @param version         服务版本，可以为空字符串
 * @param operations      服务公开的操作集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/12
 */
public record RegisterServiceDefinitionRequest(String serviceId, String applicationName, String protocol,
        String namespace, String version, List<ServiceOperationRequest> operations) {

    /**
     * 建立服务注册请求的不可变快照。
     *
     * <p>这里只校验跨进程传输必需的非空结构，不复制控制面的标识长度和领域规则。</p>
     *
     * @throws NullPointerException 任一属性为 null，或操作集合包含 null 时抛出
     */
    public RegisterServiceDefinitionRequest {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(version, "version must not be null");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations must not be null"));
    }

    /**
     * 表示服务定义中的一个可调用操作。
     *
     * @param operationId   服务内稳定且唯一的操作标识
     * @param interfaceName 服务接口全限定名
     * @param operationName Java 方法名称
     * @param parameters    按方法签名顺序排列的参数
     */
    public record ServiceOperationRequest(String operationId, String interfaceName, String operationName,
            List<ServiceOperationParameterRequest> parameters) {
        /**
         * 建立服务操作请求的不可变快照。
         *
         * @throws NullPointerException 任一属性为 null，或参数集合包含 null 时抛出
         */
        public ServiceOperationRequest {
            Objects.requireNonNull(operationId, "operationId must not be null");
            Objects.requireNonNull(interfaceName, "interfaceName must not be null");
            Objects.requireNonNull(operationName, "operationName must not be null");
            parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
        }
    }

    /**
     * 表示服务操作签名中的一个参数。
     *
     * @param position 参数位置，从零开始
     * @param name     参数名称
     * @param typeName 参数类型全限定名
     */
    public record ServiceOperationParameterRequest(Integer position, String name, String typeName) {

        /**
         * 校验参数请求的必要传输字段。
         *
         * @throws NullPointerException 任一属性为 null 时抛出
         */
        public ServiceOperationParameterRequest {
            Objects.requireNonNull(position, "position must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(typeName, "typeName must not be null");
        }
    }
}
