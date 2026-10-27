package com.lavyoung.gateweave.engine.adapter.in.configuration;

import java.util.Objects;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

/**
 * 将控制面发布的路由契约转换为数据面的领域路由。
 *
 * <p>该转换器是配置入站适配器的一部分，负责隔离跨进程契约模型与数据面领域模型，
 * 避免领域层直接依赖控制面的配置表达。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */

public final class GatewayRouteConfigurationMapper {

    /**
     * 将单条路由配置转换为领域路由。
     *
     * @param configuration 控制面发布的路由配置
     * @return 完成领域不变量校验的网关路由
     * @throws NullPointerException     configuration 或必要属性为 null 时抛出
     * @throws IllegalArgumentException 配置违反领域不变量时抛出
     */
    public GatewayRoute toDomain(RouteConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        InvocationTargetConfiguration target = configuration.target();
        InvocationTarget invocationTarget = new InvocationTarget(
                target.protocol(), target.service(), target.operation(), target.parameterTypes(), target.timeout(),
                target.attributes());

        return new GatewayRoute(
                configuration.routeId(), configuration.path(), configuration.method(), configuration.serviceId(),
                invocationTarget, configuration.attributes());
    }
}
