package com.lavyoung.gateweave.center.route.web;

import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import com.lavyoung.gateweave.center.route.application.QueryRoutePageUseCase;
import com.lavyoung.gateweave.center.route.application.RoutePage;
import com.lavyoung.gateweave.center.route.application.RoutePageQuery;
import com.lavyoung.gateweave.center.route.web.response.RoutePageResponse;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import com.lavyoung.gateweave.contract.web.Result;

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
    private final GatewayRouteConfigurationQuery gatewayRouteConfigurationQuery;
    private final LocalizedResultFactory resultFactory;

    /**
     * 创建路由查询 Controller。
     *
     * @param queryRoutePageUseCase          路由分页查询用例
     * @param gatewayRouteConfigurationQuery 路由配置候选查询
     * @param resultFactory                  本地化结果工厂
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public RouteQueryController(QueryRoutePageUseCase queryRoutePageUseCase,
            GatewayRouteConfigurationQuery gatewayRouteConfigurationQuery, LocalizedResultFactory resultFactory) {
        this.queryRoutePageUseCase = Objects.requireNonNull(
                queryRoutePageUseCase, "queryRoutePageUseCase must not be null");
        this.gatewayRouteConfigurationQuery = Objects.requireNonNull(
                gatewayRouteConfigurationQuery, "gatewayRouteConfigurationQuery must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
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
    public Result<RoutePageResponse> query(
            @PathVariable("gatewayGroupId") @NotBlank String gatewayGroupId,
            @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        RoutePageQuery query = new RoutePageQuery(gatewayGroupId, pageNumber, pageSize);
        RoutePage page = this.queryRoutePageUseCase.query(query);
        return this.resultFactory.success(RoutePageResponse.from(page));
    }

    /**
     * 预览指定网关分组当前可参与校验和发布的路由配置。
     *
     * <p>该接口返回编辑数据生成的配置候选集，不包含发布版本，
     * 不得作为 Engine 的运行时配置拉取接口。</p>
     *
     * @param gatewayGroupId 网关分组标识
     * @return 路由配置候选集
     */
    @GetMapping("/configuration-candidates")
    public Result<List<RouteConfiguration>> queryConfigurationCandidates(
            @PathVariable("gatewayGroupId") @NotBlank String gatewayGroupId) {
        List<RouteConfiguration> candidates = this.gatewayRouteConfigurationQuery.findCandidates(gatewayGroupId);
        return this.resultFactory.success(candidates);
    }
}
