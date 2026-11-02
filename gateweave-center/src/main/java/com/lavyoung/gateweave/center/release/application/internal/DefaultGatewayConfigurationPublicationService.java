package com.lavyoung.gateweave.center.release.application.internal;

import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * 编排网关配置快照发布流程。
 *
 * <p>应用服务读取当前路由候选，并将候选配置和控制面时间交给发布存储端口。
 * 版本分配、内容序列化、摘要计算及事务处理均由存储适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public class DefaultGatewayConfigurationPublicationService implements PublishGatewayConfigurationUseCase {

    private final GatewayRouteConfigurationQuery routeConfigurationQuery;
    private final ConfigurationReleaseRepository configurationReleaseRepository;
    private final Clock clock;

    /**
     * 创建网关配置发布应用服务。
     *
     * @param routeConfigurationQuery        路由候选配置查询
     * @param configurationReleaseRepository 配置快照发布存储端口
     * @param clock                          控制面时钟
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public DefaultGatewayConfigurationPublicationService(
            GatewayRouteConfigurationQuery routeConfigurationQuery,
            ConfigurationReleaseRepository configurationReleaseRepository, Clock clock) {
        this.routeConfigurationQuery = Objects.requireNonNull(
                routeConfigurationQuery, "routeConfigurationQuery must not be null");
        this.configurationReleaseRepository = Objects.requireNonNull(
                configurationReleaseRepository, "configurationReleaseRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public GatewayConfigurationSnapshot publish(String gatewayGroupId) {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        if (gatewayGroupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        List<RouteConfiguration> routes = this.routeConfigurationQuery.findCandidates(gatewayGroupId);
        return this.configurationReleaseRepository.publish(gatewayGroupId, routes, this.clock.instant());
    }
}
