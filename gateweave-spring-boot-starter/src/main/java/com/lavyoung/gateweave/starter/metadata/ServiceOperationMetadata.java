package com.lavyoung.gateweave.starter.metadata;

import java.util.List;
import java.util.Objects;

/**
 * 描述 Starter 从一个网关服务方法中采集的操作元数据。
 *
 * @param operationId   服务内稳定且唯一的操作标识
 * @param interfaceName 服务接口全限定名
 * @param operationName Java 方法名称
 * @param parameters    按方法签名顺序排列的参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
public record ServiceOperationMetadata(String operationId, String interfaceName, String operationName,
                                       List<ServiceParameterMetadata> parameters) {

    /**
     * 校验操作身份并建立参数元数据快照。
     *
     * @throws NullPointerException     任一属性为 null，或参数集合包含 null 时抛出
     * @throws IllegalArgumentException 文本为空白、参数位置不连续或参数名称重复时抛出
     */
    public ServiceOperationMetadata {
        operationId = Objects.requireNonNull(operationId, "operationId must not be null").strip();
        interfaceName = Objects.requireNonNull(interfaceName, "interfaceName must not be null").strip();
        operationName = Objects.requireNonNull(operationName, "operationName must not be null").strip();
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));

        if (operationId.isEmpty()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (interfaceName.isEmpty()) {
            throw new IllegalArgumentException("interfaceName must not be blank");
        }
        if (operationName.isEmpty()) {
            throw new IllegalArgumentException("operationName must not be blank");
        }

        for (int position = 0; position < parameters.size(); position++) {
            if (parameters.get(position).position() != position) {
                throw new IllegalArgumentException("parameter positions must be continuous from zero");
            }
        }

        long distinctParameterNames = parameters.stream().map(ServiceParameterMetadata::name).distinct().count();
        if (distinctParameterNames != parameters.size()) {
            throw new IllegalArgumentException("parameter names must not be duplicated");
        }
    }
}
