package com.lavyoung.gateweave.center.route.application;

import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import java.util.List;

/**
 * 查询指定网关分组当前可供校验和发布的路由配置候选集。
 *
 * <p>该查询读取控制面的路由编辑数据和服务目录数据，并投影为稳定配置契约。
 * 返回结果尚未形成发布版本，Engine 不得直接消费。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@FunctionalInterface
public interface GatewayRouteConfigurationQuery {

    /**
     * 查询指定网关分组中启用的路由配置候选集。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 按路由稳定顺序排列的配置候选集；没有启用路由时返回空列表
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    List<RouteConfiguration> findCandidates(String gatewayGroupId);
}
