package com.lavyoung.gateweave.center.route.domain.repository;

import java.util.List;

import com.lavyoung.gateweave.center.route.domain.RouteDefinition;

/**
 * 定义路由聚合的持久化访问端口。
 *
 * <p>接口属于领域层，具体的 SQL、分页语法和数据库行映射由出站适配器实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public interface RouteDefinitionRepository {


    /**
     * 统计指定网关分组中的路由数量。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 路由总数
     */
    long countByGatewayGroupId(String gatewayGroupId);

    /**
     * 分页查询指定网关分组中的路由。
     *
     * <p>实现必须使用稳定顺序查询路由，并批量加载当前页的参数映射，
     * 禁止为每条路由单独查询参数。</p>
     *
     * @param gatewayGroupId 网关分组标识
     * @param offset         从零开始的查询偏移量
     * @param limit          最大返回数量
     * @return 当前页路由，不存在数据时返回空集合
     */
    List<RouteDefinition> findPageByGatewayGroupId(String gatewayGroupId, long offset, int limit);

}
