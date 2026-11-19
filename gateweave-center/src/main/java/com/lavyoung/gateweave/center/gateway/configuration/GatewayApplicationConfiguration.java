package com.lavyoung.gateweave.center.gateway.configuration;

import com.lavyoung.gateweave.center.gateway.application.ExpireGatewayNodeLeasesUseCase;
import com.lavyoung.gateweave.center.gateway.application.GatewayNodeQueryRepository;
import com.lavyoung.gateweave.center.gateway.application.QueryGatewayNodePageUseCase;
import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeLeaseService;
import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeQueryService;
import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeRegistrationService;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;

/**
 * 装配网关集群限界上下文的应用服务。
 *
 * <p>领域层和应用层保持框架无关，Spring Bean、控制面时钟及持久化端口
 * 在最外层配置边界完成组装。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayNodeLeaseProperties.class)
public class GatewayApplicationConfiguration {

    /**
     * 创建网关节点注册用例。
     *
     * @param gatewayNodeRepository 网关节点持久化端口
     * @param controlPlaneClock     控制面统一时钟
     * @return 网关节点注册用例
     */
    @Bean
    public RegisterGatewayNodeUseCase registerGatewayNodeUseCase(GatewayNodeRepository gatewayNodeRepository,
            Clock controlPlaneClock) {
        return new DefaultGatewayNodeRegistrationService(gatewayNodeRepository, controlPlaneClock);
    }

    /**
     * 创建网关节点租约应用服务。
     *
     * @param gatewayNodeRepository 网关节点持久化端口
     * @param controlPlaneClock     控制面统一时钟
     * @param leaseProperties       节点租约时间策略
     * @return 同时支持节点续租和过期回收的应用服务
     */
    @Bean
    public DefaultGatewayNodeLeaseService gatewayNodeLeaseService(GatewayNodeRepository gatewayNodeRepository,
            Clock controlPlaneClock, GatewayNodeLeaseProperties leaseProperties) {
        return new DefaultGatewayNodeLeaseService(gatewayNodeRepository, controlPlaneClock,
                leaseProperties.leaseTimeout());
    }

    /**
     * 创建节点过期租约扫描适配器。
     *
     * @param expireGatewayNodeLeasesUseCase 过期租约回收用例
     * @param taskScheduler                  节点维护任务调度器
     * @param leaseProperties                节点租约时间策略
     * @return 节点过期租约扫描适配器
     */
    @Bean
    public GatewayNodeLeaseExpirationScheduler gatewayNodeLeaseExpirationScheduler(
            ExpireGatewayNodeLeasesUseCase expireGatewayNodeLeasesUseCase,
            @Qualifier("controlPlaneTaskScheduler") TaskScheduler taskScheduler,
            GatewayNodeLeaseProperties leaseProperties) {
        return new GatewayNodeLeaseExpirationScheduler(expireGatewayNodeLeasesUseCase, taskScheduler,
                leaseProperties.expirationScanInterval());
    }

    /**
     * 创建网关节点分页查询用例。
     *
     * @param queryRepository 网关节点查询持久化端口
     * @return 网关节点分页查询用例
     */
    @Bean
    public QueryGatewayNodePageUseCase queryGatewayNodePageUseCase(GatewayNodeQueryRepository queryRepository) {
        return new DefaultGatewayNodeQueryService(queryRepository);
    }
}
