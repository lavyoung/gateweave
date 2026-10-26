package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;

/**
 * 描述数据面中可参与请求匹配的网关路由。
 *
 * <p>当前阶段使用 HTTP 方法和精确路径共同匹配，查询参数不参与匹配。
 * 后续可在领域层扩展路径变量、通配符和路由优先级。</p>
 *
 * @param routeId    稳定的路由标识
 * @param path       精确匹配的 HTTP 路径
 * @param method     规范化后的 HTTP 方法
 * @param serviceId  控制面中的稳定服务标识
 * @param target     路由命中后的领域调用目标
 * @param attributes 路由策略扩展属性
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record GatewayRoute(String routeId, String path, String method, String serviceId, InvocationTarget target,
        Map<String, String> attributes) {
    /**
     * 校验路由不变量、规范化 HTTP 方法并建立不可变快照。
     *
     * @throws NullPointerException     任一属性为 null，或 attributes 包含 null 时抛出
     * @throws IllegalArgumentException 必填文本为空白或路径不是绝对路径时抛出
     */
    public GatewayRoute {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");

        if (routeId.isBlank()) {
            throw new IllegalArgumentException("routeId must not be blank");
        }
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        method = method.toUpperCase(Locale.ROOT);
        attributes = Map.copyOf(attributes);
    }

    /**
     * 判断 HTTP 请求是否命中当前路由。
     *
     * @param requestMethod 请求使用的 HTTP 方法
     * @param requestUri    请求 URI
     * @return 方法和路径同时匹配时返回 true
     * @throws NullPointerException requestMethod 或 requestUri 为 null 时抛出
     */
    public boolean matches(String requestMethod, URI requestUri) {
        Objects.requireNonNull(requestMethod, "requestMethod must not be null");
        Objects.requireNonNull(requestUri, "requestUri must not be null");
        return this.method.equals(requestMethod.toUpperCase(Locale.ROOT)) && this.path.equals(requestUri.getPath());
    }
}
