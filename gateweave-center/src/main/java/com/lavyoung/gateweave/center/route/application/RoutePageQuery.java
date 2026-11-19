package com.lavyoung.gateweave.center.route.application;

import com.lavyoung.gateweave.center.shared.pagination.PageRequest;

import java.util.Objects;

/**
 * 描述分页查询指定网关分组路由的应用请求。
 *
 * <p>页码从 1 开始。应用层在进入 Repository 前统一校验分页边界，
 * 避免 Controller、定时任务或其他入口分别实现分页规则。</p>
 *
 * @param gatewayGroupId 网关分组标识
 * @param pageRequest    通用分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record RoutePageQuery(String gatewayGroupId, PageRequest pageRequest) {

    /**
     * 校验网关分组和分页边界。
     *
     * @throws NullPointerException     gatewayGroupId 或 pageRequest 为 null 时抛出
     * @throws IllegalArgumentException 网关分组为空白时抛出
     */
    public RoutePageQuery {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");

        if (gatewayGroupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
    }
}
