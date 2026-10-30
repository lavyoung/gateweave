package com.lavyoung.gateweave.center.route.application.port.in;

import com.lavyoung.gateweave.center.route.application.query.RoutePage;
import com.lavyoung.gateweave.center.route.application.query.RoutePageQuery;

/**
 * 定义分页查询控制面路由的入站用例。
 *
 * <p>Web Controller、管理控制台及其他入站适配器只能通过该端口查询路由，
 * 不得直接调用领域 Repository。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@FunctionalInterface
public interface QueryRoutePageUseCase {

    /**
     * 查询指定网关分组中的路由分页数据。
     *
     * @param query 已完成边界校验的分页查询
     * @return 路由分页结果
     * @throws NullPointerException query 为 null 时抛出
     */
    RoutePage query(RoutePageQuery query);
}
