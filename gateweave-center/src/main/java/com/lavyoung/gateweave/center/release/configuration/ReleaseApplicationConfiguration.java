package com.lavyoung.gateweave.center.release.configuration;

import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.QueryPublishedGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.internal.DefaultGatewayConfigurationPublicationService;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 装配网关配置发布能力的应用服务。
 *
 * <p>该配置将路由候选查询、配置发布存储端口和控制面统一时钟组装为发布用例，
 * 并将已发布快照查询能力暴露为稳定应用入口。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@Configuration(proxyBeanMethods = false)
public class ReleaseApplicationConfiguration {

    /**
     * 创建网关配置发布用例。
     *
     * @param routeConfigurationQuery        路由候选配置查询
     * @param configurationReleaseRepository 配置发布存储端口
     * @param controlPlaneClock              控制面统一时钟
     * @return 网关配置发布用例
     */
    @Bean
    public PublishGatewayConfigurationUseCase publishGatewayConfigurationUseCase(
            GatewayRouteConfigurationQuery routeConfigurationQuery,
            ConfigurationReleaseRepository configurationReleaseRepository, Clock controlPlaneClock) {
        return new DefaultGatewayConfigurationPublicationService(
                routeConfigurationQuery, configurationReleaseRepository, controlPlaneClock);
    }

    /**
     * 暴露已发布配置快照查询用例。
     *
     * @param configurationReleaseRepository 配置发布存储端口
     * @return 已发布配置快照查询用例
     */
    @Bean
    public QueryPublishedGatewayConfigurationUseCase queryPublishedGatewayConfigurationUseCase(
            ConfigurationReleaseRepository configurationReleaseRepository) {
        return configurationReleaseRepository::findLatestPublished;
    }
}
