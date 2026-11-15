package com.lavyoung.gateweave.center.release.application.internal;

import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventOutbox;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * 编排配置发布事件的可靠投递流程。
 *
 * <p>每轮先从 Outbox 领取一批事件，再逐条交给消息发布端口。消息发送失败会记录失败结果；
 * 消息发送成功但数据库确认失败时保留异常，由锁超时机制重新投递，因此整体语义是至少一次。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
public final class ConfigurationPublishedEventDispatcher {

    private final ConfigurationPublishedEventOutbox outbox;
    private final ConfigurationPublishedEventPublisher publisher;
    private final Clock clock;

    /**
     * 创建配置发布事件投递服务。
     *
     * @param outbox    配置发布事件 Outbox
     * @param publisher 消息发布端口
     * @param clock     控制面统一时钟
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public ConfigurationPublishedEventDispatcher(ConfigurationPublishedEventOutbox outbox,
            ConfigurationPublishedEventPublisher publisher, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 领取并投递一批已经到期的配置发布事件。
     *
     * @return 本轮成功发布并确认的事件数量
     */
    public int dispatch() {
        List<GatewayConfigurationPublishedEvent> events = this.outbox.claimDue(this.clock.instant());
        int publishedCount = 0;
        for (GatewayConfigurationPublishedEvent event : events) {
            try {
                this.publisher.publish(event);
            } catch (RuntimeException exception) {
                this.outbox.markFailed(event.eventId(), this.clock.instant(), failureMessage(exception));
                continue;
            }
            this.outbox.markPublished(event.eventId(), this.clock.instant());
            publishedCount++;
        }
        return publishedCount;
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + message;
    }
}
