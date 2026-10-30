package com.lavyoung.gateweave.center.route.domain;


import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 描述控制面中一条可编辑的网关路由定义。
 *
 * <p>路由定义维护外部 HTTP 入口、目标服务操作、认证要求和参数映射。
 * 服务接口、操作名称及参数类型仍由服务目录维护，不在路由聚合中重复保存。</p>
 *
 * @param routeId                   稳定的路由标识
 * @param gatewayGroupId            路由所属网关分组
 * @param path                      对外暴露的 HTTP 路径
 * @param method                    规范化后的 HTTP 方法
 * @param serviceOperationId        目标服务操作标识
 * @param authenticationRequirement 身份认证要求
 * @param invocationTimeout         单次下游调用超时时间
 * @param targetAttributes          协议调用扩展属性
 * @param attributes                路由扩展属性
 * @param status                    路由编辑状态
 * @param revision                  乐观并发修订号
 * @param parameterMappings         有序请求参数映射
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RouteDefinition(
        String routeId, String gatewayGroupId, String path, String method, String serviceOperationId,
        RouteAuthenticationRequirement authenticationRequirement, Duration invocationTimeout,
        Map<String, String> targetAttributes, Map<String, String> attributes, RouteStatus status,
        long revision, List<RouteParameterMapping> parameterMappings) {

    /**
     * 校验路由定义不变量并建立集合快照。
     *
     * @throws NullPointerException     任一引用属性为 null，或集合包含 null 时抛出
     * @throws IllegalArgumentException 必填文本为空白、路径格式错误、超时无效、修订号为负数、
     *                                  参数位置不连续或请求参数名称重复时抛出
     */
    public RouteDefinition {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(serviceOperationId, "serviceOperationId must not be null");
        Objects.requireNonNull(authenticationRequirement, "authenticationRequirement must not be null");
        Objects.requireNonNull(invocationTimeout, "invocationTimeout must not be null");
        Objects.requireNonNull(targetAttributes, "targetAttributes must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(parameterMappings, "parameterMappings must not be null");

        if (routeId.isBlank()) {
            throw new IllegalArgumentException("routeId must not be blank");
        }
        if (gatewayGroupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
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
        if (serviceOperationId.isBlank()) {
            throw new IllegalArgumentException("serviceOperationId must not be blank");
        }
        if (invocationTimeout.isZero() || invocationTimeout.isNegative()) {
            throw new IllegalArgumentException("invocationTimeout must be greater than zero");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }

        method = method.toUpperCase(Locale.ROOT);
        targetAttributes = Map.copyOf(targetAttributes);
        attributes = Map.copyOf(attributes);
        parameterMappings = List.copyOf(parameterMappings);

        for (int position = 0; position < parameterMappings.size(); position++) {
            if (parameterMappings.get(position).position() != position) {
                throw new IllegalArgumentException("parameter positions must be continuous from zero");
            }
        }

        long distinctNames = parameterMappings.stream()
                .map(RouteParameterMapping::requestName)
                .distinct()
                .count();
        if (distinctNames != parameterMappings.size()) {
            throw new IllegalArgumentException("request parameter names must not be duplicated");
        }
    }
}
