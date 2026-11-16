package com.lavyoung.gateweave.engine.adapter.in.redis;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 接收配置发布事件并触发当前网关分组的配置同步。
 *
 * <p>Redis 事件只作为配置变化提示，监听器仍通过控制面查询最新完整快照。
 * 多个连续事件会被合并，避免在消息突发时无界提交重复刷新任务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
public final class GatewayConfigurationPublishedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayConfigurationPublishedEventListener.class);

    private final GatewayConfigurationSynchronizer configurationSynchronizer;
    private final Executor executor;
    private final String gatewayGroupId;
    private final AtomicBoolean refreshRequested = new AtomicBoolean();
    private final AtomicBoolean refreshRunning = new AtomicBoolean();

    /**
     * 创建配置发布事件监听器。
     *
     * @param configurationSynchronizer 配置同步应用服务
     * @param executor                  控制面任务执行器
     * @param gatewayGroupId            当前节点所属网关分组
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    public GatewayConfigurationPublishedEventListener(GatewayConfigurationSynchronizer configurationSynchronizer,
            Executor executor, String gatewayGroupId) {
        this.configurationSynchronizer =
                Objects.requireNonNull(configurationSynchronizer, "configurationSynchronizer must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.gatewayGroupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        if (this.gatewayGroupId.isEmpty()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
    }

    /**
     * 处理配置发布事件。
     *
     * <p>同一时刻最多运行一个刷新任务。刷新期间收到的新事件只记录一次待刷新状态，
     * 当前刷新结束后再拉取最新快照，不会为每条消息创建独立排队任务。</p>
     *
     * @param event 配置发布事件
     * @throws NullPointerException event 为 null 时抛出
     */
    public void onMessage(GatewayConfigurationPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!this.gatewayGroupId.equals(event.gatewayGroupId())) {
            LOGGER.warn("Ignored configuration event {} for unrelated gateway group {}", event.eventId(),
                    event.gatewayGroupId());
            return;
        }
        this.refreshRequested.set(true);
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        if (!this.refreshRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            this.executor.execute(this::drainRefreshRequests);
        } catch (RuntimeException exception) {
            this.refreshRunning.set(false);
            this.refreshRequested.set(false);
            throw exception;
        }
    }

    private void drainRefreshRequests() {
        try {
            while (this.refreshRequested.getAndSet(false)) {
                this.configurationSynchronizer.synchronize(this.gatewayGroupId);
            }
        } finally {
            this.refreshRunning.set(false);
            if (this.refreshRequested.get()) {
                scheduleRefresh();
            }
        }
    }
}
