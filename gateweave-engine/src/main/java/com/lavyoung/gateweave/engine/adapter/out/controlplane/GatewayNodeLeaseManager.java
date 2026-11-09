package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.GatewayNodeLease;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 管理网关节点注册及运行期租约续期。
 *
 * <p>管理器使用控制面返回的心跳间隔调度续租，并直接读取运行时路由表的当前配置版本。
 * 心跳复用控制面统一调度器，不创建线程或维护重复的配置版本状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
public final class GatewayNodeLeaseManager {

    private final GatewayControlPlaneClient controlPlaneClient;
    private final GatewayRouteTable routeTable;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> heartbeatTask;

    /**
     * 创建网关节点租约管理器。
     *
     * @param controlPlaneClient 控制面出站端口
     * @param routeTable         当前运行时路由表
     * @param taskScheduler      控制面统一任务调度器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeLeaseManager(
            GatewayControlPlaneClient controlPlaneClient, GatewayRouteTable routeTable,
            TaskScheduler taskScheduler) {
        this.controlPlaneClient = Objects.requireNonNull(
                controlPlaneClient, "controlPlaneClient must not be null");
        this.routeTable = Objects.requireNonNull(routeTable, "routeTable must not be null");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler must not be null");
    }

    /**
     * 注册当前节点，并按照控制面授予的间隔启动心跳任务。
     *
     * @param request 节点注册请求
     * @throws NullPointerException  request 为 null时抛出
     * @throws IllegalStateException 节点注册失败或调度器拒绝心跳任务时抛出
     */
    public synchronized void start(RegisterGatewayNodeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (this.heartbeatTask != null && !this.heartbeatTask.isDone()) {
            return;
        }
        GatewayNodeLease lease = this.controlPlaneClient.registerNode(request);
        Instant firstHeartbeatAt = this.taskScheduler.getClock().instant().plus(lease.heartbeatInterval());
        this.heartbeatTask = this.taskScheduler.scheduleWithFixedDelay(
                () -> this.renewLease(lease), firstHeartbeatAt, lease.heartbeatInterval());
    }

    /**
     * 停止后续心跳任务。
     */
    public synchronized void stop() {
        ScheduledFuture<?> scheduledFuture = this.heartbeatTask;
        this.heartbeatTask = null;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    private void renewLease(GatewayNodeLease lease) {
        this.controlPlaneClient.renewNodeLease(lease.nodeId(), lease.leaseId(), this.routeTable.currentVersion());
    }
}
