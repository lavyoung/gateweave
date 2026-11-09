package com.lavyoung.gateweave.center.gateway.application.service;

import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * 编排网关算力节点注册用例。
 *
 * <p>应用服务负责取得控制面时间、创建在线节点并调用持久化端口。
 * 并发幂等、唯一键处理和数据库行映射由 Repository 实现负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public class DefaultGatewayNodeRegistrationService implements RegisterGatewayNodeUseCase {

    private final GatewayNodeRepository gatewayNodeRepository;
    private final Clock clock;

    /**
     * 创建节点注册应用服务。
     *
     * @param gatewayNodeRepository 网关节点持久化端口
     * @param clock                 控制面时钟
     * @throws NullPointerException gatewayNodeRepository 或 clock 为 null 时抛出
     */
    public DefaultGatewayNodeRegistrationService(GatewayNodeRepository gatewayNodeRepository, Clock clock) {
        this.gatewayNodeRepository = Objects.requireNonNull(
                gatewayNodeRepository, "gatewayNodeRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public GatewayNodeRegistrationResult register(GatewayNodeRegistration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        GatewayNode node = GatewayNode.registered(registration, this.clock.instant());
        return this.gatewayNodeRepository.registerOrRefresh(node);
    }
}
