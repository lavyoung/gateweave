package com.lavyoung.gateweave.engine.configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import com.lavyoung.gateweave.engine.adapter.in.redis.GatewayConfigurationPublishedEventListener;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayControlPlaneProperties;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatewayConfigurationEventRedisConfigurationTests {

    private static final String TOPIC = "gateweave:configuration:published:default";
    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-16T15:00:00Z");

    @Test
    void shouldCreateBoundedListenerExecutorThatRejectsOverflow() throws Exception {
        // Given
        GatewayConfigurationEventRedisConfiguration configuration =
                new GatewayConfigurationEventRedisConfiguration();
        ThreadPoolExecutor executor = configuration.gatewayConfigurationEventExecutor();
        CountDownLatch workersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        Runnable blockingTask = () -> {
            workersStarted.countDown();
            await(releaseWorkers);
        };

        try {
            // When
            executor.execute(blockingTask);
            executor.execute(blockingTask);
            assertThat(workersStarted.await(2, TimeUnit.SECONDS)).isTrue();
            for (int index = 0; index < 64; index++) {
                executor.execute(() -> {
                });
            }

            // Then
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
            assertThat(executor.getQueue()).hasSize(64);
            assertThatThrownBy(() -> executor.execute(() -> {
            })).isInstanceOf(RejectedExecutionException.class);
        } finally {
            releaseWorkers.countDown();
            executor.shutdownNow();
        }
        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void shouldDeserializeEventAndDelegateToConfigurationListener() {
        // Given
        GatewayConfigurationEventRedisConfiguration configuration =
                new GatewayConfigurationEventRedisConfiguration();
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        GatewayConfigurationPublishedEventListener listener =
                configuration.gatewayConfigurationPublishedEventListener(synchronizer, Runnable::run, properties());
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MessageListenerAdapter adapter =
                configuration.gatewayConfigurationPublishedEventMessageListener(listener, objectMapper);
        adapter.afterPropertiesSet();
        GatewayConfigurationPublishedEvent event =
                new GatewayConfigurationPublishedEvent(UUID.randomUUID(), "default", 3, PUBLISHED_AT);
        Jackson2JsonRedisSerializer<GatewayConfigurationPublishedEvent> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, GatewayConfigurationPublishedEvent.class);
        byte[] body = serializer.serialize(event);

        // When
        adapter.onMessage(new DefaultMessage(TOPIC.getBytes(StandardCharsets.UTF_8), body), null);

        // Then
        verify(synchronizer).synchronize("default");
    }

    @Test
    void shouldConfigureContainerWithScopedTopicAndBoundedExecutor() {
        // Given
        GatewayConfigurationEventRedisConfiguration configuration =
                new GatewayConfigurationEventRedisConfiguration();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        Executor executor = mock(Executor.class);
        MessageListenerAdapter listenerAdapter = mock(MessageListenerAdapter.class);

        // When
        RedisMessageListenerContainer container = configuration.gatewayConfigurationEventListenerContainer(
                connectionFactory, executor, listenerAdapter, properties());

        // Then
        assertThat(container.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(ReflectionTestUtils.getField(container, "taskExecutor")).isSameAs(executor);
        assertThat(ReflectionTestUtils.getField(container, "subscriptionExecutor")).isSameAs(executor);
        Map<MessageListener, Set<Topic>> listenerTopics = listenerTopics(container);
        assertThat(listenerTopics).containsEntry(listenerAdapter, Set.of(new ChannelTopic(TOPIC)));
    }

    @SuppressWarnings("unchecked")
    private static Map<MessageListener, Set<Topic>> listenerTopics(RedisMessageListenerContainer container) {
        return (Map<MessageListener, Set<Topic>>) ReflectionTestUtils.getField(container, "listenerTopics");
    }

    private static GatewayControlPlaneProperties properties() {
        return new GatewayControlPlaneProperties(URI.create("http://127.0.0.1:8080"), "node-1", "default",
                "Gateway Node 1", URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(3),
                Duration.ofSeconds(30), "gateweave:configuration:published");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
