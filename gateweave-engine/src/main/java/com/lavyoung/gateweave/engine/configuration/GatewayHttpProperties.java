package com.lavyoung.gateweave.engine.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * 描述网关访问下游 HTTP 服务所需的运行参数。
 *
 * @param connectTimeout 建立下游 HTTP 连接的最长等待时间
 * @param coreThreads    HTTP 客户端执行器核心线程数
 * @param maximumThreads HTTP 客户端执行器最大线程数
 * @param queueCapacity  HTTP 客户端执行器队列容量
 * @param keepAlive      非核心线程空闲存活时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@ConfigurationProperties(prefix = "gateweave.http")
public record GatewayHttpProperties(
        Duration connectTimeout,
        int coreThreads,
        int maximumThreads,
        int queueCapacity,
        Duration keepAlive
) {
    /**
     * 校验 HTTP 客户端及其执行器配置。
     *
     * @throws NullPointerException     时间配置为 null 时抛出
     * @throws IllegalArgumentException 时间不为正数、线程数不合法或队列容量不为正数时抛出
     */
    public GatewayHttpProperties {
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(keepAlive, "keepAlive must not be null");

        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must be greater than zero");
        }
        if (coreThreads <= 0) {
            throw new IllegalArgumentException("coreThreads must be greater than zero");
        }
        if (maximumThreads < coreThreads) {
            throw new IllegalArgumentException("maximumThreads must be greater than or equal to coreThreads");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero");
        }
        if (keepAlive.isZero() || keepAlive.isNegative()) {
            throw new IllegalArgumentException("keepAlive must be greater than zero");
        }
    }
}
