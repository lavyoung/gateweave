package com.lavyoung.gateweave.contract.configuration;

import java.util.Map;
import java.util.Objects;

/**
 * 描述控制面发布给网关数据面的路由配置。
 *
 * @param routeId    稳定的路由标识
 * @param path       对外暴露的 HTTP 请求路径
 * @param method     对外接受的 HTTP 方法
 * @param serviceId  控制面中的稳定服务标识
 * @param target     路由命中后的协议调用目标
 * @param attributes 路由扩展属性，例如过滤器和策略配置
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record RouteConfiguration(String routeId, String path, String method, String serviceId,
        InvocationTargetConfiguration target, Map<String, String> attributes) {

    /**
     * 校验路由属性并建立不可变快照。
     *
     * @throws NullPointerException     任一属性为 null，或 attributes 包含 null 时抛出
     * @throws IllegalArgumentException 必填文本为空白时抛出
     */
    public RouteConfiguration {
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
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        attributes = Map.copyOf(attributes);
    }
}
