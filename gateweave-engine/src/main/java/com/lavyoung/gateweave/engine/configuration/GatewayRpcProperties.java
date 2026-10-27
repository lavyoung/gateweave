package com.lavyoung.gateweave.engine.configuration;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.lavyoung.gateweave.plugin.invoker.rpc.DubboDiscoveryMode;

/**
 * 描述网关数据面连接 RPC 基础设施所需的运行参数。
 *
 * @param applicationName Dubbo 消费端应用名称
 * @param registryAddress Dubbo 注册中心地址
 * @param discoveryMode   Dubbo 服务发现模式
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@ConfigurationProperties(prefix = "gateweave.rpc")
public record GatewayRpcProperties(String applicationName, String registryAddress, DubboDiscoveryMode discoveryMode) {

    /**
     * 校验 RPC 运行参数。
     *
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException 任一字符串参数为空白时抛出
     */
    public GatewayRpcProperties {
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(registryAddress, "registryAddress must not be null");
        Objects.requireNonNull(discoveryMode, "discoveryMode must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        if (registryAddress.isBlank()) {
            throw new IllegalArgumentException("registryAddress must not be blank");
        }
    }
}
