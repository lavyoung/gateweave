package com.lavyoung.gateweave.center.catalog.domain;

import java.util.List;
import java.util.Objects;

/**
 * 描述服务目录中一个可被网关路由引用的服务操作。
 *
 * <p>服务操作维护目标接口、方法和有序参数签名。参数必须按位置连续排列，
 * 从而保证发布到数据面后能够按照目标方法签名完成调用。</p>
 *
 * @param operationId   稳定的服务操作标识
 * @param serviceId     所属服务标识
 * @param interfaceName 服务接口名称
 * @param operationName 服务操作名称
 * @param status        服务目录状态
 * @param revision      乐观并发修订号
 * @param parameters    有序服务参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record ServiceOperation(String operationId, String serviceId, String interfaceName, String operationName,
                               ServiceCatalogStatus status, long revision, List<ServiceOperationParameter> parameters) {
    /**
     * 校验服务操作不变量并建立参数集合快照。
     *
     * @throws NullPointerException     任一引用属性为 null，或参数集合包含 null 时抛出
     * @throws IllegalArgumentException 文本为空白、修订号为负数、参数位置不连续或参数名称重复时抛出
     */
    public ServiceOperation {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(interfaceName, "interfaceName must not be null");
        Objects.requireNonNull(operationName, "operationName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");

        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
        if (interfaceName.isBlank()) {
            throw new IllegalArgumentException("interfaceName must not be blank");
        }
        if (operationName.isBlank()) {
            throw new IllegalArgumentException("operationName must not be blank");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }

        parameters = List.copyOf(parameters);
        for (int position = 0; position < parameters.size(); position++) {
            if (parameters.get(position).position() != position) {
                throw new IllegalArgumentException("parameter positions must be continuous from zero");
            }
        }

        long distinctNames = parameters.stream().map(ServiceOperationParameter::name).distinct().count();
        if (distinctNames != parameters.size()) {
            throw new IllegalArgumentException("parameter names must not be duplicated");
        }
    }
}
