package com.lavyoung.gateweave.center.route.adapter.in.web;

import java.util.Objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lavyoung.gateweave.center.route.application.port.in.QueryRoutePageUseCase;
import com.lavyoung.gateweave.center.route.application.query.RoutePage;
import com.lavyoung.gateweave.center.route.application.query.RoutePageQuery;

/**
 * 提供控制面路由分页查询管理 API。
 *
 * <p>Controller 只负责解析和校验 HTTP 参数、调用应用用例并转换响应，
 * 不直接访问 Repository 或编排数据库查询。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@RestController
@RequestMapping("/api/v1/gateway-groups/{gatewayGroupId}/routes")
public class RouteQueryController {

    private final QueryRoutePageUseCase queryRoutePageUseCase;

    /**
     * 创建路由查询 Controller。
     *
     * @param queryRoutePageUseCase 路由分页查询用例
     * @throws NullPointerException queryRoutePageUseCase 为 null 时抛出
     */
    public RouteQueryController(QueryRoutePageUseCase queryRoutePageUseCase) {
        this.queryRoutePageUseCase = Objects.requireNonNull(
                queryRoutePageUseCase, "queryRoutePageUseCase must not be null");
    }

    /**
     * 分页查询指定网关分组的路由。
     *
     * @param gatewayGroupId 网关分组标识
     * @param pageNumber     页码，从 1 开始
     * @param pageSize       每页数量，最大为 100
     * @return 路由分页响应
     */
    @GetMapping
    public RoutePageResponse query(
            @PathVariable("gatewayGroupId") @NotBlank String gatewayGroupId,
            @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        RoutePageQuery query = new RoutePageQuery(gatewayGroupId, pageNumber, pageSize);
        RoutePage page = this.queryRoutePageUseCase.query(query);
        return RoutePageResponse.from(page);
    }
}
