package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import org.springframework.context.SmartLifecycle;

import java.util.Objects;

/**
 * 在网关进程生命周期中维护节点与控制面的注册关系。
 *
 * <p>当前阶段只完成启动注册。后续心跳与注销仍归属于该生命周期边界，
 * 不需要再次创建监听器或生命周期组件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class GatewayNodeLifecycle implements SmartLifecycle {

    private final GatewayControlPlaneClient controlPlaneClient;
    private final GatewayControlPlaneProperties properties;
    private volatile boolean running;

    /**
     * 创建网关节点生命周期适配器。
     *
     * @param controlPlaneClient 控制面出站端口
     * @param properties         控制面连接和节点身份配置
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeLifecycle(
            GatewayControlPlaneClient controlPlaneClient, GatewayControlPlaneProperties properties) {
        this.controlPlaneClient = Objects.requireNonNull(controlPlaneClient, "controlPlaneClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 在 Spring 完成单例初始化后向控制面注册当前网关节点。
     *
     * @throws IllegalStateException 请求序列化失败、控制面响应无法解析或注册被拒绝时抛出
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
}
