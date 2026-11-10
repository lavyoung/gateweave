package com.lavyoung.gateweave.center.gateway.application.service;

import com.lavyoung.gateweave.center.gateway.application.ExpireGatewayNodeLeasesUseCase;
import com.lavyoung.gateweave.center.gateway.application.RenewGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeHeartbeat;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 编排网关算力节点的租约生命周期。
 *
 * <p>应用服务使用控制面统一时钟创建心跳领域对象，并将续租交给 Repository 原子执行。
 * 同时根据控制面授予的租约超时时间计算截止时间，批量回收已经过期的在线租约。</p>
 *
 * <p>不存在、已离线、租约不匹配及配置版本回退统一表现为租约失效，
 * 避免向外部调用方泄漏节点内部状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
public class DefaultGatewayNodeLeaseService implements RenewGatewayNodeLeaseUseCase, ExpireGatewayNodeLeasesUseCase {

    private final GatewayNodeRepository gatewayNodeRepository;
    private final Clock clock;
    private final Duration leaseTimeout;

    /**
     * 创建节点租约应用服务。
     *
     * @param gatewayNodeRepository 网关节点持久化端口
     * @param clock                 控制面统一时钟
     * @param leaseTimeout          控制面授予节点的租约超时时间
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public DefaultGatewayNodeLeaseService(
            GatewayNodeRepository gatewayNodeRepository, Clock clock, Duration leaseTimeout) {
        this.gatewayNodeRepository = Objects.requireNonNull(
                gatewayNodeRepository, "gatewayNodeRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.leaseTimeout = Objects.requireNonNull(leaseTimeout, "leaseTimeout must not be null");
    }

    @Override
    public void renew(String nodeId, UUID leaseId, long currentConfigVersion) {
        GatewayNodeHeartbeat heartbeat = new GatewayNodeHeartbeat(nodeId, leaseId,
                this.clock.instant(), currentConfigVersion);
        if (!this.gatewayNodeRepository.renewLease(heartbeat)) {
            throw new BusinessException(GatewayResponseCode.GATEWAY_NODE_LEASE_REJECTED, heartbeat.nodeId());
        }
    }

    @Override
    public int expire() {
        Instant heartbeatDeadline = this.clock.instant().minus(this.leaseTimeout);
        return this.gatewayNodeRepository.markExpiredLeasesOffline(heartbeatDeadline);
    }
}
