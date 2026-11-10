package com.lavyoung.gateweave.center.gateway.configuration;

import com.lavyoung.gateweave.center.gateway.application.ExpireGatewayNodeLeasesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 定期触发网关节点过期租约回收。
 *
 * <p>该类型是控制面时间触发入站适配器，只负责调度和生命周期管理。
 * 截止时间计算及节点状态修改分别由应用服务和持久化适配器完成。</p>
 *
 * <p>多个 Center 实例可以同时执行扫描，因为底层使用带在线状态和心跳截止时间的
 * 原子条件更新；已经被其他实例下线的节点不会再次计入更新数量。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/10
 */
public final class GatewayNodeLeaseExpirationScheduler implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayNodeLeaseExpirationScheduler.class);

    private final ExpireGatewayNodeLeasesUseCase expireGatewayNodeLeasesUseCase;
    private final TaskScheduler taskScheduler;
    private final Duration scanInterval;
    private ScheduledFuture<?> expirationTask;

    /**
     * 创建节点过期租约调度器。
     *
     * @param expireGatewayNodeLeasesUseCase 过期租约回收用例
     * @param taskScheduler                  Spring 管理的任务调度器
     * @param scanInterval                   过期租约扫描周期
     * @throws NullPointerException 任一参数为 null 时抛出
     */
    public GatewayNodeLeaseExpirationScheduler(
            ExpireGatewayNodeLeasesUseCase expireGatewayNodeLeasesUseCase,
            TaskScheduler taskScheduler, Duration scanInterval) {
        this.expireGatewayNodeLeasesUseCase = Objects.requireNonNull(
                expireGatewayNodeLeasesUseCase, "expireGatewayNodeLeasesUseCase must not be null");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler must not be null");
        this.scanInterval = Objects.requireNonNull(scanInterval, "scanInterval must not be null");
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        Instant firstExecutionAt = this.taskScheduler.getClock().instant().plus(this.scanInterval);
        this.expirationTask = this.taskScheduler.scheduleWithFixedDelay(this::expireLeases,
                firstExecutionAt, this.scanInterval);
    }

    @Override
    public void stop() {
        ScheduledFuture<?> scheduledFuture = this.expirationTask;
        this.expirationTask = null;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public boolean isRunning() {
        return this.expirationTask != null
                && !this.expirationTask.isCancelled()
                && !this.expirationTask.isDone();
    }

    /**
     * 让租约扫描在普通生命周期组件之后启动，并在关闭时优先停止。
     *
     * @return 调度器生命周期阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void expireLeases() {
        int expiredCount = this.expireGatewayNodeLeasesUseCase.expire();
        if (expiredCount > 0) {
            LOGGER.info("Marked {} expired gateway nodes offline", expiredCount);
        }
    }
}
