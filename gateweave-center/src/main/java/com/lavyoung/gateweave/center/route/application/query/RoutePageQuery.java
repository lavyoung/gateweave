package com.lavyoung.gateweave.center.route.application.query;

import java.util.Objects;

/**
 * 描述分页查询指定网关分组路由的应用请求。
 *
 * <p>页码从 1 开始。应用层在进入 Repository 前统一校验分页边界，
 * 避免 Controller、定时任务或其他入口分别实现分页规则。</p>
 *
 * @param gatewayGroupId 网关分组标识
 * @param pageNumber     页码，从 1 开始
 * @param pageSize       每页最大路由数量
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RoutePageQuery(String gatewayGroupId, int pageNumber, int pageSize) {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 校验网关分组和分页边界。
     *
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException 网关分组为空白、页码小于 1，
     *                                  或每页数量不在 1 至 100 之间时抛出
     */
    public RoutePageQuery {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");

        if (gatewayGroupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than zero");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }

    /**
     * 将从 1 开始的页码转换为 Repository 使用的行偏移量。
     *
     * @return 从零开始的数据库查询偏移量
     */
    public long offset() {
        return (long) (this.pageNumber - 1) * this.pageSize;
    }
}
