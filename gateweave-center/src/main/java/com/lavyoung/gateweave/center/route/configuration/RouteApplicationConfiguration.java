package com.lavyoung.gateweave.center.route.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lavyoung.gateweave.center.route.application.QueryRoutePageUseCase;
import com.lavyoung.gateweave.center.route.application.DefaultRouteQueryService;
import com.lavyoung.gateweave.center.route.domain.RouteDefinitionRepository;

/**
 * 装配路由业务能力的应用服务和出站端口。
 *
 * <p>领域层和应用层保持框架无关，Spring Bean 的创建集中在最外层装配边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@Configuration(proxyBeanMethods = false)
public class RouteApplicationConfiguration {

    /**
     * 创建路由分页查询用例。
     *
     * @param routeRepository 路由持久化端口
     * @return 路由分页查询用例
     */
    @Bean
    public QueryRoutePageUseCase queryRoutePageUseCase(RouteDefinitionRepository routeRepository) {
        return new DefaultRouteQueryService(routeRepository);
    }
}
