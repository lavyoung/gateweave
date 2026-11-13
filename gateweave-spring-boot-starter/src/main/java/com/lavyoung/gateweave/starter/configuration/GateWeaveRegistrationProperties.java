package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.contract.http.HttpEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * 定义 Starter 向 GateWeave 控制面注册服务所需的连接参数。
 *
 * <p>应用名称复用 {@code spring.application.name}，服务身份来自 {@code GateWeaveService}，
 * 因此这里只维护控制面地址和 HTTP 超时，不重复配置服务元数据。</p>
 *
 * @param baseUri        GateWeave 控制面 HTTP 根地址
 * @param connectTimeout 建立控制面连接的最长等待时间
 * @param requestTimeout 单次服务注册请求的最长等待时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
@ConfigurationProperties(prefix = "gateweave.registration")
public record GateWeaveRegistrationProperties(URI baseUri, @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration requestTimeout) {
    /**
     * 校验服务注册客户端建立连接所需的本地配置。
     *
     * @throws NullPointerException     任一配置为 null 时抛出
     * @throws IllegalArgumentException 控制面地址不合法，或者超时时间不为正数时抛出
     */
    public GateWeaveRegistrationProperties {
        baseUri = new HttpEndpoint(Objects.requireNonNull(baseUri, "baseUri must not be null")).uri();
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");

        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must be greater than zero");
        }
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be greater than zero");
        }
    }
}
