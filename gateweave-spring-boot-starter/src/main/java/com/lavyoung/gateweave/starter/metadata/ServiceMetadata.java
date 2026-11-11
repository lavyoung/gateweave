package com.lavyoung.gateweave.starter.metadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 描述 Starter 从一个服务 Bean 中采集的完整服务定义。
 *
 * <p>该模型只记录服务提供方声明和 Java 类型系统能够确定的事实，
 * 不包含路由地址、HTTP 方法、认证策略或控制面维护的状态。</p>
 *
 * @param serviceId       稳定服务标识
 * @param applicationName 服务所属应用名称
 * @param protocol        服务调用协议
 * @param namespace       服务命名空间
 * @param version         服务版本
 * @param operations      服务公开的操作集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
public record ServiceMetadata(String serviceId, String applicationName, String protocol, String namespace,
                              String version, List<ServiceOperationMetadata> operations) {
    /**
     * 校验服务身份并建立操作元数据快照。
     *
     * @throws NullPointerException     任一属性为 null，或操作集合包含 null 时抛出
     * @throws IllegalArgumentException 必填文本为空白、操作标识重复或操作定义重复时抛出
     */
    public ServiceMetadata {
        serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null").strip();
        applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null").strip();
        protocol = Objects.requireNonNull(protocol, "protocol must not be null").strip();
        namespace = Objects.requireNonNull(namespace, "namespace must not be null").strip();
        version = Objects.requireNonNull(version, "version must not be null").strip();
        operations = List.copyOf(Objects.requireNonNull(operations, "operations must not be null"));

        if (serviceId.isEmpty()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
        if (applicationName.isEmpty()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        if (protocol.isEmpty()) {
            throw new IllegalArgumentException("protocol must not be blank");
        }

        long distinctOperationIds = operations.stream().map(ServiceOperationMetadata::operationId).distinct().count();
        if (distinctOperationIds != operations.size()) {
            throw new IllegalArgumentException("operationIds must not be duplicated");
        }

        long distinctDefinitions =
                operations.stream().map(operation -> Map.entry(operation.interfaceName(), operation.operationName()))
                        .distinct().count();
        if (distinctDefinitions != operations.size()) {
            throw new IllegalArgumentException("operation definitions must not be duplicated");
        }
    }
}
