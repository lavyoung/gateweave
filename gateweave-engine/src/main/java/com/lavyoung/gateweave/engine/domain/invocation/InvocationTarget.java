package com.lavyoung.gateweave.engine.domain.invocation;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 描述数据面实际执行调用所需的协议目标。
 *
 * <p>该值对象属于数据面领域模型，不依赖控制面配置 DTO 或厂商 SDK。
 * 协议名称会被规范化，以便后续选择对应的协议调用实现。</p>
 *
 * @param protocol       规范化后的目标协议
 * @param service        目标服务标识
 * @param operation      目标操作名称
 * @param parameterTypes 参数类型名称列表
 * @param timeout        单次调用超时时间
 * @param attributes     协议扩展属性
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record InvocationTarget(String protocol, String service, String operation, List<String> parameterTypes,
        Duration timeout, Map<String, String> attributes) {
    /**
     * 校验领域属性、规范化协议并建立不可变快照。
     *
     * @throws NullPointerException     任一属性为 null，或集合包含 null 元素时抛出
     * @throws IllegalArgumentException 必填文本为空白、参数类型为空白或超时时间不为正数时抛出
     */
    public InvocationTarget {
        Objects.requireNonNull(protocol, "protocol must not be null");
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");

        if (protocol.isBlank()) {
            throw new IllegalArgumentException("protocol must not be blank");
        }
        if (service.isBlank()) {
            throw new IllegalArgumentException("service must not be blank");
        }
        if (operation.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }

        protocol = protocol.toLowerCase(Locale.ROOT);
        parameterTypes = List.copyOf(parameterTypes);
        if (parameterTypes.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("parameterTypes must not contain blank values");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be greater than zero");
        }

        attributes = Map.copyOf(attributes);
    }

}
