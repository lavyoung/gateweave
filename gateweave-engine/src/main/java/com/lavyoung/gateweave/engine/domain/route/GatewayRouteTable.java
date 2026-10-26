package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 保存数据面当前生效的不可变路由快照，并负责查找唯一匹配路由。
 *
 * <p>路由表允许为空，以支持网关在尚未接收到控制面配置时启动。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GatewayRouteTable {

    private final List<GatewayRoute> routes;

    /**
     * 创建不可变路由表。
     *
     * @param routes 当前生效的路由列表
     * @throws NullPointerException     routes 为 null 或包含 null 元素时抛出
     * @throws IllegalArgumentException 路由标识重复，或方法与路径组合重复时抛出
     */
    public GatewayRouteTable(List<GatewayRoute> routes) {
        Objects.requireNonNull(routes, "routes must not be null");
        this.routes = List.copyOf(routes);
        validateUniqueRoutes(this.routes);
    }

    /**
     * 根据 HTTP 方法和 URI 查找匹配路由。
     *
     * @param requestMethod 请求使用的 HTTP 方法
     * @param requestUri    请求 URI
     * @return 命中时返回唯一路由，否则返回空
     * @throws NullPointerException requestMethod 或 requestUri 为 null 时抛出
     */
    public Optional<GatewayRoute> match(String requestMethod, URI requestUri) {
        Objects.requireNonNull(requestMethod, "requestMethod must not be null");
        Objects.requireNonNull(requestUri, "requestUri must not be null");
        return this.routes.stream().filter(route -> route.matches(requestMethod, requestUri)).findFirst();
    }

    /**
     * 校验路由标识和匹配条件不存在冲突。
     *
     * @param routes 不可变路由列表
     * @throws IllegalArgumentException 路由标识重复，或方法与路径组合重复时抛出
     */
    private void validateUniqueRoutes(List<GatewayRoute> routes) {
        Set<String> routeIds = new HashSet<>();
        Set<RouteMatchKey> matchKeys = new HashSet<>();

        for (GatewayRoute route : routes) {
            if (!routeIds.add(route.routeId())) {
                throw new IllegalArgumentException("duplicate routeId: %s".formatted(route.routeId()));
            }
            RouteMatchKey matchKey = new RouteMatchKey(route.method(), route.path());
            if (!matchKeys.add(matchKey)) {
                throw new IllegalArgumentException(
                        "duplicate route match: %s %s".formatted(route.method(), route.path()));
            }
        }
    }

    /**
     * 表示参与唯一性校验的 HTTP 方法与路径组合。
     *
     * @param method 规范化后的 HTTP 方法
     * @param path   精确匹配路径
     */
    private record RouteMatchKey(String method, String path) {
    }
}
