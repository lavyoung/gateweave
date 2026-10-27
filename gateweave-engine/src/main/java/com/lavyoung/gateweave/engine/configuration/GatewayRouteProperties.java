package com.lavyoung.gateweave.engine.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

/**
 * 承接网关启动时加载的路由配置快照。
 *
 * <p>当前章节从本地配置文件加载路由；后续接入控制面后，可以将数据来源替换为配置订阅，
 * 而不改变数据面的领域路由模型。</p>
 *
 * @param routes 控制面契约形式的路由配置列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@ConfigurationProperties(prefix = "gateweave.routing")
public record GatewayRouteProperties(List<RouteConfiguration> routes) {

    /**
     * 建立不可变的路由配置快照；未配置路由时使用空列表。
     *
     * @param routes 配置文件绑定得到的路由列表
     * @throws NullPointerException routes 包含 null 元素时抛出
     */
    public GatewayRouteProperties {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }
}
