package com.lavyoung.gateweave.contract.extension;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 描述网关应用层提交给协议插件的一次服务调用。
 *
 * <p>该契约只包含网关自有类型，不暴露 Dubbo、HTTP 客户端等厂商 SDK 类型。
 * 协议插件根据 protocol 判断是否支持请求，并根据其余字段完成具体调用。</p>
 *
 * @param requestId     请求标识，用于日志和链路追踪
 * @param routeId       已命中的路由标识
 * @param protocol      目标协议，例如 dubbo 或 http
 * @param service       目标服务标识，例如 RPC 接口全限定名
 * @param operation     目标操作名称，例如 RPC 方法名
 * @param parameterTypes 参数类型名称列表
 * @param timeout       本次调用超时时间
 * @param headers       需要传递给下游的请求头
 * @param attributes    协议扩展属性，例如 Dubbo 的 group 和 version
 * @param payload       原始请求负载
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record InvocationRequest(String requestId, String routeId, String protocol, String service, String operation,
        List<String> parameterTypes, Duration timeout, Map<String, String> headers, Map<String, String> attributes,
        byte[] payload) {

    /**
     * 校验调用信息、规范化协议名称并建立不可变快照。
     *
     * @throws NullPointerException     任一属性为 null，或集合包含 null 元素时抛出
     * @throws IllegalArgumentException 必填文本为空白、参数类型为空白或超时时间不为正数时抛出
     */
    public InvocationRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (routeId.isBlank()) {
            throw new IllegalArgumentException("routeId must not be blank");
        }
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

        headers = Map.copyOf(headers);
        attributes = Map.copyOf(attributes);
        payload = Arrays.copyOf(payload, payload.length);
    }

    /**
     * 返回请求负载副本，避免协议插件修改调用契约内部状态。
     *
     * @return 请求负载副本
     */
    @Override
    public byte[] payload() {
        return Arrays.copyOf(this.payload, this.payload.length);
    }
}
