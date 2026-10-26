package com.lavyoung.gateweave.engine.configuration;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 定义网关数据面 Netty 服务器的运行参数。
 *
 * @param host             服务器绑定地址
 * @param port             服务器监听端口
 * @param maxContentLength 单个完整 HTTP 请求允许的最大字节数
 * @param bossThreads      接收连接的 EventLoop 线程数
 * @param workerThreads    处理连接读写的 EventLoop 线程数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@ConfigurationProperties(prefix = "gateweave.server")
public record GatewayServerProperties(String host, int port, int maxContentLength, int bossThreads,
        int workerThreads) {
    /**
     * 校验服务器启动所需的全部参数。
     *
     * @throws NullPointerException     host 为 null 时抛出
     * @throws IllegalArgumentException host 为空白，或数值参数超出合法范围时抛出
     */
    public GatewayServerProperties {
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (maxContentLength <= 0) {
            throw new IllegalArgumentException("maxContentLength must be greater than zero");
        }
        if (bossThreads <= 0) {
            throw new IllegalArgumentException("bossThreads must be greater than zero");
        }
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be greater than zero");
        }
    }
}
