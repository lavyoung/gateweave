package com.lavyoung.gateweave.center.release.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.center.release.messaging.RedisConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 装配配置发布事件的 Redis 消息适配器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationEventMessagingConfiguration {

    /**
     * 创建配置发布事件专用 Redis 模板。
     *
     * @param connectionFactory Boot 管理的 Redis 连接工厂
     * @param objectMapper      Boot 管理的 JSON 映射器
     * @return 配置发布事件 Redis 模板
     */
    @Bean
    public RedisTemplate<String, GatewayConfigurationPublishedEvent> configurationPublishedEventRedisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(
                new Jackson2JsonRedisSerializer<>(objectMapper, GatewayConfigurationPublishedEvent.class));
        return redisTemplate;
    }

    /**
     * 创建 Redis 配置发布事件端口实现。
     *
     * @param redisTemplate 配置发布事件专用 Redis 模板
     * @param topicPrefix   配置发布频道前缀
     * @return 配置发布事件端口
     */
    @Bean
    public ConfigurationPublishedEventPublisher configurationPublishedEventPublisher(
            RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate,
            @Value("${gateweave.configuration-events.topic-prefix:"
                    + "gateweave:configuration:published}") String topicPrefix) {
        return new RedisConfigurationPublishedEventPublisher(redisTemplate, topicPrefix);
    }
}
