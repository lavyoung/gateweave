package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 保存数据面当前生效的不可变路由快照，并负责查找唯一匹配路由。
 *
 * <p>路由表允许为空，以支持网关在尚未接收到控制面配置时启动。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class GatewayRouteTable {

    private final AtomicReference<RouteSnapshot> snapshot;


    /**
     * 使用本地配置创建初始路由表。
     *
     * @param routes 初始路由列表
     * @throws NullPointerException     routes 为 null 或包含 null 元素时抛出
     * @throws IllegalArgumentException 路由标识重复，或方法与路径组合重复时抛出
     */
    public GatewayRouteTable(List<GatewayRoute> routes) {
        Objects.requireNonNull(routes, "routes must not be null");
        this.snapshot = new AtomicReference<>(createSnapshot(0, routes));
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
        return this.snapshot.get().routes.stream()
                .filter(route -> route.matches(requestMethod, requestUri)).findFirst();
    }


    /**
     * 当发布版本高于当前版本时原子替换全部运行时路由。
     *
     * <p>新路由会在切换前完成不可变复制和唯一性校验。校验失败时不会修改当前快照，
     * 从而保留最后一个有效版本。</p>
     *
     * @param version 控制面发布版本
     * @param routes  新版本包含的完整路由列表
     * @return 完成替换返回 true，版本未更新返回 false
     * @throws NullPointerException     routes 为 null 或包含 null 元素时抛出
     * @throws IllegalArgumentException version 小于 1，路由标识重复，或匹配条件重复时抛出
     */
    public boolean replaceIfNew(long version, List<GatewayRoute> routes) {
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        RouteSnapshot routeSnapshot = createSnapshot(version, routes);
        while (true) {
            RouteSnapshot current = this.snapshot.get();
            if (version <= current.version()) {
                return false;
            }
            if (this.snapshot.compareAndSet(current, routeSnapshot)) {
                return true;
            }
        }
    }

    /**
     * 返回当前生效的发布版本。
     *
     * @return 本地初始配置返回 0，控制面配置返回对应发布版本
     */
    public long currentVersion() {
        return this.snapshot.get().version();
    }

    private RouteSnapshot createSnapshot(long version, List<GatewayRoute> routes) {
        Objects.requireNonNull(routes, "routes must not be null");
        validateUniqueRoutes(routes);
        return new RouteSnapshot(version, List.copyOf(routes));
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


    private record RouteSnapshot(long version, List<GatewayRoute> routes) {
    }

    private record RouteMatchKey(String method, String path) {
    }
}
