package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

/**
 * 在网关进程生命周期中完成节点注册和初始配置同步。
 *
 * <p>启动时先注册节点，再拉取所属网关分组最后发布的配置。任一步失败都不会将生命周期
 * 标记为运行。后续心跳、注销和持续配置刷新仍复用该生命周期边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class GatewayNodeLifecycle implements SmartLifecycle {

    private final GatewayControlPlaneClient controlPlaneClient;
    private final GatewayConfigurationSynchronizer configurationSynchronizer;
    private final GatewayControlPlaneProperties properties;
    private volatile boolean running;

    /**
     * 创建网关节点生命周期适配器。
     *
     * @param controlPlaneClient        控制面出站端口
     * @param configurationSynchronizer 配置同步应用服务
     * @param properties                控制面连接和节点身份配置
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeLifecycle(
            GatewayControlPlaneClient controlPlaneClient,
            GatewayConfigurationSynchronizer configurationSynchronizer,
            GatewayControlPlaneProperties properties) {
        this.controlPlaneClient = Objects.requireNonNull(
                controlPlaneClient, "controlPlaneClient must not be null");
        this.configurationSynchronizer = Objects.requireNonNull(
                configurationSynchronizer, "configurationSynchronizer must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }


    /**
     * 注册当前节点并同步最后发布的配置快照。
     *
     * @throws IllegalStateException 节点注册、控制面查询或配置同步失败时抛出
     */
    @Override
    public synchronized void start() {
        if (this.running) {
            return;
        }
        RegisterGatewayNodeRequest request = new RegisterGatewayNodeRequest(
                this.properties.nodeId(), this.properties.gatewayGroupId(), this.properties.nodeName(),
                this.properties.advertisedAddress());
        this.controlPlaneClient.registerNode(request);
        this.configurationSynchronizer.synchronize(this.properties.gatewayGroupId());
        this.running = true;
    }

    @Override
    public synchronized void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    /**
     * 按固定延迟查询并应用控制面最后发布的配置。
     *
     * <p>初始同步由 start 完成。周期任务只有在生命周期运行时才执行；
     * 单次失败由统一调度器记录，并保留最后有效路由等待下次重试。</p>
     */
    @Scheduled(fixedDelayString = "${gateweave.control-plane.refresh-interval}",
            initialDelayString = "${gateweave.control-plane.refresh-interval}")
    public void refreshConfiguration() {
        if (!this.running) {
            return;
        }
        this.configurationSynchronizer.synchronize(this.properties.gatewayGroupId());
    }

    /**
     * 让节点注册和初始配置同步早于普通运行时生命周期组件启动。
     *
     * @return 生命周期最早阶段
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
