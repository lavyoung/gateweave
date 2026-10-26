package com.lavyoung.gateweave.contract.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 描述网关路由最终需要调用的协议目标。
 *
 * <p>该契约只表达调用目标，不包含 Dubbo、HTTP 客户端或其他厂商 SDK 类型。
 * 具体协议插件根据 protocol 和 attributes 解释目标并完成实际调用。</p>
 *
 * @param protocol       目标协议，也是调用插件的选择依据，例如 dubbo 或 http
 * @param service        目标服务标识，例如 RPC 接口全限定名
 * @param operation      目标操作名称，例如 RPC 方法名
 * @param parameterTypes 目标方法的参数类型名称列表
 * @param timeout        单次调用超时时间
 * @param attributes     协议扩展属性，例如 group、version
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record InvocationTargetConfiguration(String protocol, String service, String operation,
        List<String> parameterTypes, Duration timeout, Map<String, String> attributes) {
    /**
     * 校验调用目标并建立不可变快照。
     *
     * @throws NullPointerException     任一属性为 null，或集合包含 null 元素时抛出
     * @throws IllegalArgumentException 必填文本为空白、参数类型为空白或超时时间不为正数时抛出
     */
    public InvocationTargetConfiguration {
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
