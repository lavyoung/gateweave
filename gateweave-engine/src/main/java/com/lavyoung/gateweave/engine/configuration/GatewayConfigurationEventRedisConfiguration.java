package com.lavyoung.gateweave.engine.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import com.lavyoung.gateweave.engine.adapter.in.redis.GatewayConfigurationPublishedEventListener;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayControlPlaneProperties;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 装配网关配置发布事件的 Redis 入站适配器。
 *
 * <p>Redis 容器只负责接收和反序列化事件，实际配置刷新由监听器提交到现有控制面调度器。
 * 消息接收使用独立有界线程池，避免 Redis 消息突发形成无界任务堆积。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
@Configuration(proxyBeanMethods = false)
public class GatewayConfigurationEventRedisConfiguration {

    private static final int LISTENER_THREADS = 2;
    private static final int LISTENER_QUEUE_CAPACITY = 64;

    /**
     * 创建 Redis 订阅与消息分发使用的有界线程池。
     *
     * <p>一个线程可用于维护订阅，另一个线程处理事件回调。队列满时拒绝新任务，
     * 未消费的事件最终由周期配置刷新补偿。</p>
     *
     * @return Redis 配置事件执行器
     */
    @Bean(name = "gatewayConfigurationEventExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor gatewayConfigurationEventExecutor() {
        return new ThreadPoolExecutor(LISTENER_THREADS, LISTENER_THREADS, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(LISTENER_QUEUE_CAPACITY),
                new CustomizableThreadFactory("gateweave-configuration-event-"), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 创建配置发布事件监听器。
     *
     * @param synchronizer           配置同步应用服务
     * @param controlPlaneExecutor   控制面任务执行器
     * @param controlPlaneProperties 当前网关节点控制面配置
     * @return 配置发布事件监听器
     */
    @Bean
    public GatewayConfigurationPublishedEventListener gatewayConfigurationPublishedEventListener(
            GatewayConfigurationSynchronizer synchronizer,
            @Qualifier("gatewayControlPlaneTaskScheduler") Executor controlPlaneExecutor,
            GatewayControlPlaneProperties controlPlaneProperties) {
        return new GatewayConfigurationPublishedEventListener(synchronizer, controlPlaneExecutor,
                controlPlaneProperties.gatewayGroupId());
    }

    /**
     * 创建使用项目统一 ObjectMapper 的 Redis 消息适配器。
     *
     * @param listener     配置发布事件监听器
     * @param objectMapper 项目统一 JSON 映射器
     * @return Redis 消息监听适配器
     */
    @Bean
    public MessageListenerAdapter gatewayConfigurationPublishedEventMessageListener(
            GatewayConfigurationPublishedEventListener listener, ObjectMapper objectMapper) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "onMessage");
        adapter.setSerializer(
                new Jackson2JsonRedisSerializer<>(objectMapper, GatewayConfigurationPublishedEvent.class));
        return adapter;
    }

    /**
     * 创建并管理 Redis 配置事件订阅容器。
     *
     * @param connectionFactory Redis 连接工厂
     * @param listenerExecutor  Redis 订阅与消息回调执行器
     * @param listenerAdapter   配置发布事件消息适配器
     * @param properties        当前网关节点控制面配置
     * @return Redis 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer gatewayConfigurationEventListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("gatewayConfigurationEventExecutor") Executor listenerExecutor,
            MessageListenerAdapter listenerAdapter, GatewayControlPlaneProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(listenerExecutor);
        container.setSubscriptionExecutor(listenerExecutor);
        String topic = properties.configurationEventTopicPrefix() + ":" + properties.gatewayGroupId();
        container.addMessageListener(listenerAdapter, new ChannelTopic(topic));
        return container;
    }
}
