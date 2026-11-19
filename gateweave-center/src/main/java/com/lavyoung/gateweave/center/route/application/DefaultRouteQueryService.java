package com.lavyoung.gateweave.center.route.application;

import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteDefinitionRepository;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;

import java.util.List;
import java.util.Objects;

/**
 * 编排控制面路由分页查询。
 *
 * <p>应用服务负责协调路由总数和当前页数据查询。分页 SQL、数据库行映射及参数批量加载
 * 仍由 Repository 出站适配器负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public class DefaultRouteQueryService implements QueryRoutePageUseCase {

    private final RouteDefinitionRepository routeRepository;

    /**
     * 创建路由查询应用服务。
     *
     * @param routeRepository 路由聚合持久化端口
     * @throws NullPointerException routeRepository 为 null 时抛出
     */
    public DefaultRouteQueryService(RouteDefinitionRepository routeRepository) {
        this.routeRepository = Objects.requireNonNull(routeRepository, "routeRepository must not be null");
    }


    @Override
    public PageResult<RouteDefinition> query(RoutePageQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        PageRequest pageRequest = query.pageRequest();
        long count = this.routeRepository.countByGatewayGroupId(query.gatewayGroupId());
        if (count == 0) {
            return new PageResult<>(List.of(), pageRequest, 0);
        }
        List<RouteDefinition> routes = this.routeRepository.findPageByGatewayGroupId(query.gatewayGroupId(),
                pageRequest.offset(), pageRequest.pageSize());
        return new PageResult<>(routes, pageRequest, count);
    }
}
