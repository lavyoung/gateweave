package com.lavyoung.gateweave.center.release.lifecycle;

import com.lavyoung.gateweave.center.release.application.internal.ConfigurationPublishedEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 定期触发配置发布事件的可靠投递。
 *
 * <p>该类型只负责时间触发和生命周期管理。事件领取、消息发布及状态迁移
 * 由应用服务和出站适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
public class ConfigurationPublishedEventDispatchScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationPublishedEventDispatchScheduler.class);

    private final ConfigurationPublishedEventDispatcher dispatcher;
    private final TaskScheduler taskScheduler;
    private final Duration dispatchInterval;
    private ScheduledFuture<?> dispatchTask;

    /**
     * 创建配置发布事件投递调度器。
     *
     * @param dispatcher       配置发布事件投递应用服务
     * @param taskScheduler    控制面共享任务调度器
     * @param dispatchInterval 两轮投递之间的固定间隔
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException dispatchInterval 不为正数时抛出
     */
    public ConfigurationPublishedEventDispatchScheduler(ConfigurationPublishedEventDispatcher dispatcher,
            TaskScheduler taskScheduler, Duration dispatchInterval) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler must not be null");
        this.dispatchInterval = Objects.requireNonNull(dispatchInterval, "dispatchInterval must not be null");
        if (this.dispatchInterval.isZero() || this.dispatchInterval.isNegative()) {
            throw new IllegalArgumentException("dispatchInterval must be greater than zero");
        }
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        Instant firstExecutionAt = this.taskScheduler.getClock().instant().plus(this.dispatchInterval);
        this.dispatchTask = this.taskScheduler.scheduleWithFixedDelay(() -> {
            int publishedCount = this.dispatcher.dispatch();
            if (publishedCount > 0) {
                LOGGER.info("Published {} gateway configuration events", publishedCount);
            }
        }, firstExecutionAt, this.dispatchInterval);
    }

    @Override
    public void stop() {
        ScheduledFuture<?> scheduledFuture = this.dispatchTask;
        this.dispatchTask = null;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public boolean isRunning() {
        return this.dispatchTask != null && !this.dispatchTask.isCancelled() && !this.dispatchTask.isDone();
    }

    /**
     * 让投递调度器在普通生命周期组件之后启动，并在关闭时优先停止。
     *
     * @return 调度器生命周期阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
