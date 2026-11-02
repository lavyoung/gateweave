package com.lavyoung.gateweave.center.gateway.configuration;

import com.lavyoung.gateweave.center.gateway.application.DefaultGatewayNodeRegistrationService;
import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class GatewayApplicationConfiguration {

    /**
     * 创建网关节点注册用例。
     *
     * @param gatewayNodeRepository 网关节点持久化端口
     * @param controlPlaneClock     控制面统一时钟
     * @return 网关节点注册用例
     */
    @Bean
    public RegisterGatewayNodeUseCase registerGatewayNodeUseCase(
            GatewayNodeRepository gatewayNodeRepository, Clock controlPlaneClock) {
        return new DefaultGatewayNodeRegistrationService(gatewayNodeRepository, controlPlaneClock);
    }
}
