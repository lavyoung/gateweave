package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.contract.web.Result;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

/**
 * 在网关进程生命周期中维护节点与控制面的注册关系。
 *
 * <p>当前阶段只完成启动注册。后续心跳与注销仍归属于该生命周期边界，
 * 不需要再次创建监听器或生命周期组件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class GatewayNodeLifecycle implements SmartLifecycle {

    private static final String REGISTRATION_PATH = "/api/v1/gateway-nodes";
    private static final TypeReference<Result<JsonNode>> RESULT_TYPE = new TypeReference<Result<JsonNode>>() {
    };

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GatewayControlPlaneProperties properties;
    private volatile boolean running;

    /**
     * 创建网关节点生命周期适配器。
     *
     * @param httpClient   共享 HTTP 客户端
     * @param objectMapper 共享 JSON 映射器
     * @param properties   控制面连接和节点身份配置
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeLifecycle(
            HttpClient httpClient, ObjectMapper objectMapper, GatewayControlPlaneProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 在 Spring 完成单例初始化后向控制面注册当前网关节点。
     *
     * @throws IllegalStateException 请求序列化失败、控制面响应无法解析或注册被拒绝时抛出
     */
    @Override
    public synchronized void start() {
        if (this.running) {
            return;
        }
        RegisterGatewayNodeRequest request = new RegisterGatewayNodeRequest(
                this.properties.nodeId(), this.properties.gatewayGroupId(), this.properties.nodeName(),
                this.properties.advertisedAddress());
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(registrationUri())
                .timeout(this.properties.requestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(serialize(request)))
                .build();
        HttpResponse<byte[]> response = this.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .join();
        verifyRegistration(response);
        this.running = true;
    }

    @Override
    public synchronized void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    private URI registrationUri() {
        return this.properties.baseUri().resolve(REGISTRATION_PATH);
    }

    private byte[] serialize(RegisterGatewayNodeRequest request) {
        try {
            return this.objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize gateway node registration request", exception);
        }
    }

    private void verifyRegistration(HttpResponse<byte[]> response) {
        Result<JsonNode> result = deserialize(response.body());
        boolean successfulStatus = response.statusCode() == HttpURLConnection.HTTP_OK
                || response.statusCode() == HttpURLConnection.HTTP_CREATED;
        if (!successfulStatus || !result.success()) {
            throw new IllegalStateException(
                    "Gateway node registration failed with status %d and code %s"
                            .formatted(response.statusCode(), result.code()));
        }
    }

    private Result<JsonNode> deserialize(byte[] responseBody) {
        try {
            return this.objectMapper.readValue(responseBody, RESULT_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to deserialize gateway node registration response", exception);
        }
    }
}
