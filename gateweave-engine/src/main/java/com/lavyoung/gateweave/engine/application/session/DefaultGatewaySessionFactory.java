package com.lavyoung.gateweave.engine.application.session;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import java.util.Objects;
import java.util.Optional;

/**
 * 使用当前生效的领域路由表创建网关会话。
 *
 * <p>该实现只负责路由匹配和会话创建，不负责生成 HTTP 404 响应，
 * 也不负责调用下游服务。响应决策仍由网关请求用例完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class DefaultGatewaySessionFactory implements GatewaySessionFactory {

    private final GatewayRouteTable routeTable;

    /**
     * 创建默认网关会话工厂。
     *
     * @param routeTable 当前生效的不可变路由表
     * @throws NullPointerException routeTable 为 null 时抛出
     */
    public DefaultGatewaySessionFactory(GatewayRouteTable routeTable) {
        this.routeTable = Objects.requireNonNull(routeTable, "routeTable must not be null");
    }

    @Override
    public Optional<GatewaySession> openSession(GatewayRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return this.routeTable.match(command.method(), command.uri())
                .map(route -> new GatewaySession(command, route));
    }
}
