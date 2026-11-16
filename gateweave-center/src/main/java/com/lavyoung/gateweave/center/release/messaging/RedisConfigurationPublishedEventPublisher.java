package com.lavyoung.gateweave.center.release.messaging;

import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

/**
 * 使用 Redis Pub/Sub 发布配置变更提示。
 *
 * <p>每个网关分组使用独立频道，避免无关数据面节点收到其他分组的配置事件。
 * 事件序列化由 Spring Data Redis 配置的官方序列化器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
public final class RedisConfigurationPublishedEventPublisher implements ConfigurationPublishedEventPublisher {

    private final RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate;
    private final String topicPrefix;

    /**
     * 创建 Redis 配置发布事件适配器。
     *
     * @param redisTemplate 类型安全的 Redis 操作模板
     * @param topicPrefix   配置发布频道前缀
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException topicPrefix 为空白时抛出
     */
    public RedisConfigurationPublishedEventPublisher(
            RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate, String topicPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.topicPrefix = Objects.requireNonNull(topicPrefix, "topicPrefix must not be null").strip();
        if (this.topicPrefix.isEmpty()) {
            throw new IllegalArgumentException("topicPrefix must not be blank");
        }
    }


    @Override
    public void publish(GatewayConfigurationPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String topic = this.topicPrefix + ":" + event.gatewayGroupId();
        Long subscriberCount = this.redisTemplate.convertAndSend(topic, event);
        if (subscriberCount == 0) {
            throw new IllegalStateException("No gateway engine subscribed to configuration topic: " + topic);
        }
    }
}
