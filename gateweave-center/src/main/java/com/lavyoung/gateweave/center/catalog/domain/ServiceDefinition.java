package com.lavyoung.gateweave.center.catalog.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 描述控制面服务目录中的一个服务定义。
 *
 * <p>服务定义是服务目录聚合根，负责维护服务身份、协议属性和操作集合。
 * 服务实例地址及心跳状态属于后续实例管理领域，不进入该聚合。</p>
 *
 * @param serviceId       稳定的服务标识
 * @param applicationName 服务所属应用名称
 * @param protocol        服务调用协议
 * @param namespace       服务命名空间，可以为空字符串
 * @param version         服务版本，可以为空字符串
 * @param status          服务目录状态
 * @param revision        乐观并发修订号
 * @param operations      服务提供的操作集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record ServiceDefinition(String serviceId, String applicationName, String protocol, String namespace,
                                String version, ServiceCatalogStatus status, long revision,
                                List<ServiceOperation> operations) {

    /**
     * 校验服务定义不变量并建立操作集合快照。
     *
     * @throws NullPointerException     任一引用属性为 null，或操作集合包含 null 时抛出
     * @throws IllegalArgumentException 必填文本为空白、修订号为负数、操作归属错误或操作定义重复时抛出
     */
    public ServiceDefinition {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(operations, "operations must not be null");

        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        if (protocol.isBlank()) {
            throw new IllegalArgumentException("protocol must not be blank");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }

        operations = List.copyOf(operations);
        boolean containsForeignOperation = operations.stream()
                .anyMatch(operation -> !serviceId.equals(operation.serviceId()));
        if (containsForeignOperation) {
            throw new IllegalArgumentException("operations must belong to the service");
        }

        long distinctOperationIds = operations.stream().map(ServiceOperation::operationId).distinct().count();
        if (distinctOperationIds != operations.size()) {
            throw new IllegalArgumentException("operationIds must not be duplicated");
        }

        long distinctDefinitions = operations.stream()
                .map(operation -> Map.entry(operation.interfaceName(), operation.operationName()))
                .distinct()
                .count();
        if (distinctDefinitions != operations.size()) {
            throw new IllegalArgumentException("operation definitions must not be duplicated");
        }
    }
}
