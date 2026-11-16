package com.lavyoung.gateweave.center.release.messaging;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisConfigurationPublishedEventPublisherTests {

    private static final String TOPIC_PREFIX = "gateweave:configuration:published";
    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-16T01:00:00Z");

    @Test
    void shouldPublishEventToGatewayGroupTopic() {
        // Given
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = redisTemplate();
        RedisConfigurationPublishedEventPublisher publisher =
                new RedisConfigurationPublishedEventPublisher(redisTemplate, " " + TOPIC_PREFIX + " ");
        GatewayConfigurationPublishedEvent event = event();
        when(redisTemplate.convertAndSend(TOPIC_PREFIX + ":default", event)).thenReturn(2L);

        // When
        publisher.publish(event);

        // Then
        verify(redisTemplate).convertAndSend(TOPIC_PREFIX + ":default", event);
    }

    @Test
    void shouldFailWhenNoGatewayEngineSubscribesToTopic() {
        // Given
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = redisTemplate();
        RedisConfigurationPublishedEventPublisher publisher =
                new RedisConfigurationPublishedEventPublisher(redisTemplate, TOPIC_PREFIX);
        GatewayConfigurationPublishedEvent event = event();
        when(redisTemplate.convertAndSend(TOPIC_PREFIX + ":default", event)).thenReturn(0L);

        // When & Then
        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(IllegalStateException.class)
                .hasMessage("No gateway engine subscribed to configuration topic: " + TOPIC_PREFIX + ":default");
    }

    @Test
    void shouldRejectMissingEventBeforeCallingRedis() {
        // Given
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = redisTemplate();
        RedisConfigurationPublishedEventPublisher publisher =
                new RedisConfigurationPublishedEventPublisher(redisTemplate, TOPIC_PREFIX);

        // When & Then
        assertThatThrownBy(() -> publisher.publish(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("event must not be null");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldRejectMissingDependenciesAndBlankTopicPrefix() {
        // Given
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = redisTemplate();

        // When & Then
        assertThatThrownBy(() -> new RedisConfigurationPublishedEventPublisher(null, TOPIC_PREFIX))
                .isInstanceOf(NullPointerException.class).hasMessage("redisTemplate must not be null");
        assertThatThrownBy(() -> new RedisConfigurationPublishedEventPublisher(redisTemplate, null))
                .isInstanceOf(NullPointerException.class).hasMessage("topicPrefix must not be null");
        assertThatThrownBy(() -> new RedisConfigurationPublishedEventPublisher(redisTemplate, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("topicPrefix must not be blank");
    }

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    private static GatewayConfigurationPublishedEvent event() {
        return new GatewayConfigurationPublishedEvent(UUID.randomUUID(), "default", 3, PUBLISHED_AT);
    }
}
