package com.lavyoung.gateweave.center.release.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

/**
 * 定义配置发布事件的可靠投递策略。
 *
 * @param dispatchInterval 两轮事件投递之间的固定间隔
 * @param batchSize        单次最多领取的事件数量
 * @param lockDuration     事件领取后的锁定时间
 * @param retryDelay       投递失败后的重试间隔
 * @param maxAttempts      单个事件允许的最大投递次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
@ConfigurationProperties("gateweave.configuration-events.delivery")
public record ConfigurationEventDeliveryProperties(@DefaultValue("1s") Duration dispatchInterval,
        @DefaultValue("100") int batchSize, @DefaultValue("30s") Duration lockDuration,
        @DefaultValue("5s") Duration retryDelay, @DefaultValue("10") int maxAttempts) {

    /**
     * 校验配置发布事件投递策略。
     *
     * @throws NullPointerException     时间配置为 null 时抛出
     * @throws IllegalArgumentException 数量或时间配置不为正数时抛出
     */
    public ConfigurationEventDeliveryProperties {
        Objects.requireNonNull(dispatchInterval, "dispatchInterval must not be null");
        Objects.requireNonNull(lockDuration, "lockDuration must not be null");
        Objects.requireNonNull(retryDelay, "retryDelay must not be null");
        if (dispatchInterval.isZero() || dispatchInterval.isNegative()) {
            throw new IllegalArgumentException("dispatchInterval must be greater than zero");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("lockDuration must be greater than zero");
        }
        if (retryDelay.isZero() || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must be greater than zero");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
    }
}
