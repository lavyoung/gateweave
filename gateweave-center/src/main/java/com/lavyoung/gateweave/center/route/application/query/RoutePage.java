package com.lavyoung.gateweave.center.route.application.query;

import com.lavyoung.gateweave.center.route.domain.RouteDefinition;

import java.util.List;
import java.util.Objects;

/**
 * 保存一次路由分页查询的应用层结果。
 *
 * <p>结果中保留领域路由对象，由具体入站适配器负责转换为 HTTP 响应或其他协议模型，
 * 应用层不依赖 Controller DTO。</p>
 *
 * @param routes        当前页路由
 * @param pageNumber    当前页码，从 1 开始
 * @param pageSize      每页最大数量
 * @param totalElements 符合查询条件的路由总数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RoutePage(List<RouteDefinition> routes, int pageNumber, int pageSize, long totalElements) {

    /**
     * 校验分页结果并建立路由集合快照。
     *
     * @throws NullPointerException     routes 为 null或包含 null 时抛出
     * @throws IllegalArgumentException 页码、每页数量、总数或当前页数量不合法时抛出
     */
    public RoutePage {
        Objects.requireNonNull(routes, "routes must not be null");

        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than zero");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }

        routes = List.copyOf(routes);
        if (routes.size() > pageSize) {
            throw new IllegalArgumentException("routes size must not exceed pageSize");
        }
        if (routes.size() > totalElements) {
            throw new IllegalArgumentException("routes size must not exceed totalElements");
        }
    }

    /**
     * 计算查询结果的总页数。
     *
     * @return 没有数据时返回 0，否则返回向上取整后的总页数
     */
    public long totalPages() {
        if (this.totalElements == 0) {
            return 0;
        }
        return (this.totalElements - 1) / this.pageSize + 1;
    }
}
