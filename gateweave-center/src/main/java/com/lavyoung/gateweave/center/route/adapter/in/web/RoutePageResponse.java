package com.lavyoung.gateweave.center.route.adapter.in.web;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.lavyoung.gateweave.center.route.application.query.RoutePage;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteParameterMapping;

/**
 * 表示管理 API 返回的路由分页数据。
 *
 * <p>该类型属于 HTTP 入站适配器，不向调用方暴露应用层或领域层对象。</p>
 *
 * @param routes        当前页路由
 * @param pageNumber    当前页码
 * @param pageSize      每页数量
 * @param totalElements 路由总数
 * @param totalPages    总页数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RoutePageResponse(
        List<RouteResponse> routes, int pageNumber, int pageSize, long totalElements, long totalPages) {

    /**
     * 建立响应集合的不可变快照。
     *
     * @throws NullPointerException routes 为 null 或包含 null 时抛出
     */
    public RoutePageResponse {
        routes = List.copyOf(routes);
    }

    /**
     * 将应用层分页结果转换为 HTTP 响应。
     *
     * @param page 应用层分页结果
     * @return HTTP 路由分页响应
     * @throws NullPointerException page 为 null 时抛出
     */
    public static RoutePageResponse from(RoutePage page) {
        Objects.requireNonNull(page, "page must not be null");

        List<RouteResponse> routes = page.routes().stream().map(RouteResponse::from).toList();
        return new RoutePageResponse(
                routes, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }

    /**
     * 表示一条路由的 HTTP 响应。
     *
     * @param routeId                   路由标识
     * @param gatewayGroupId            网关分组标识
     * @param path                      HTTP 路径
     * @param method                    HTTP 方法
     * @param serviceOperationId        目标服务操作标识
     * @param authenticationRequirement 身份认证要求
     * @param invocationTimeoutMillis   调用超时毫秒数
     * @param targetAttributes          协议调用扩展属性
     * @param attributes                路由扩展属性
     * @param status                    路由状态
     * @param revision                  修订号
     * @param parameterMappings         请求参数映射
     */
    public record RouteResponse(
            String routeId, String gatewayGroupId, String path, String method, String serviceOperationId,
            String authenticationRequirement, long invocationTimeoutMillis,
            Map<String, String> targetAttributes, Map<String, String> attributes,
            String status, long revision, List<ParameterMappingResponse> parameterMappings) {

        private static RouteResponse from(RouteDefinition route) {
            List<ParameterMappingResponse> mappings = route.parameterMappings()
                    .stream()
                    .map(ParameterMappingResponse::from)
                    .toList();

            return new RouteResponse(
                    route.routeId(), route.gatewayGroupId(), route.path(), route.method(),
                    route.serviceOperationId(), route.authenticationRequirement().name(),
                    route.invocationTimeout().toMillis(), route.targetAttributes(), route.attributes(),
                    route.status().name(), route.revision(), mappings);
        }
    }

    /**
     * 表示 HTTP 请求参数到服务参数位置的映射。
     *
     * @param position    目标参数位置
     * @param requestName HTTP 请求参数名称
     */
    public record ParameterMappingResponse(int position, String requestName) {

        private static ParameterMappingResponse from(RouteParameterMapping mapping) {
            return new ParameterMappingResponse(mapping.position(), mapping.requestName());
        }
    }
}
