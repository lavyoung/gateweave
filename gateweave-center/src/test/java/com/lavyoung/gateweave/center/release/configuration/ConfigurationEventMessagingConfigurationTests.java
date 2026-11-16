package com.lavyoung.gateweave.center.release.configuration;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.center.release.messaging.RedisConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigurationEventMessagingConfigurationTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-16T01:00:00Z");

    @Test
    void shouldConfigureTypedRedisTemplateWithOfficialSerializers() {
        // Given
        ConfigurationEventMessagingConfiguration configuration = new ConfigurationEventMessagingConfiguration();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // When
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate =
                configuration.configurationPublishedEventRedisTemplate(connectionFactory, objectMapper);

        // Then
        assertThat(redisTemplate.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(redisTemplate.getKeySerializer()).isSameAs(StringRedisSerializer.UTF_8);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    @Test
    void shouldSerializeAndDeserializeConfigurationPublishedEvent() {
        // Given
        ConfigurationEventMessagingConfiguration configuration = new ConfigurationEventMessagingConfiguration();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate =
                configuration.configurationPublishedEventRedisTemplate(mock(RedisConnectionFactory.class),
                        objectMapper);
        GatewayConfigurationPublishedEvent expected =
                new GatewayConfigurationPublishedEvent(UUID.randomUUID(), "default", 3, PUBLISHED_AT);
        RedisSerializer<GatewayConfigurationPublishedEvent> serializer = valueSerializer(redisTemplate);

        // When
        byte[] payload = serializer.serialize(expected);
        GatewayConfigurationPublishedEvent actual = serializer.deserialize(payload);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldCreateRedisPublisherAdapter() {
        // Given
        ConfigurationEventMessagingConfiguration configuration = new ConfigurationEventMessagingConfiguration();
        RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate = new RedisTemplate<>();

        // When
        ConfigurationPublishedEventPublisher publisher = configuration.configurationPublishedEventPublisher(
                redisTemplate, "gateweave:configuration:published");

        // Then
        assertThat(publisher).isInstanceOf(RedisConfigurationPublishedEventPublisher.class);
    }

    @SuppressWarnings("unchecked")
    private static RedisSerializer<GatewayConfigurationPublishedEvent> valueSerializer(
            RedisTemplate<String, GatewayConfigurationPublishedEvent> redisTemplate) {
        return (RedisSerializer<GatewayConfigurationPublishedEvent>) redisTemplate.getValueSerializer();
    }
}
