package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeLifecycleTests {

    private static final String SUCCESS_RESPONSE = """
            {"success":true,"code":"SUCCESS","message":"Success","data":{}}
            """;

    private final AtomicInteger invocations = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(201);
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestContentType = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> responseBody = new AtomicReference<>(SUCCESS_RESPONSE);
    private final AtomicReference<CountDownLatch> responseGate = new AtomicReference<>();
    private final CountDownLatch requestReceived = new CountDownLatch(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/api/v1/gateway-nodes", this::handleRegistration);
        this.server.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    @Test
    void shouldRegisterNodeAndStartOnlyOnce() throws Exception {
        // Given
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofSeconds(2));

        // When
        lifecycle.start();
        lifecycle.start();

        // Then
        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(this.invocations).hasValue(1);
        assertThat(this.requestMethod).hasValue("POST");
        assertThat(this.requestContentType).hasValue("application/json");
        JsonNode body = this.objectMapper.readTree(this.requestBody.get());
        assertThat(body.path("nodeId").asText()).isEqualTo("node-1");
        assertThat(body.path("gatewayGroupId").asText()).isEqualTo("default");
        assertThat(body.path("nodeName").asText()).isEqualTo("Gateway Node 1");
        assertThat(body.path("advertisedAddress").asText()).isEqualTo("http://127.0.0.1:7397");

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldAcceptRefreshedRegistrationResponse() {
        // Given
        this.responseStatus.set(200);
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofSeconds(2));

        // When
        lifecycle.start();

        // Then
        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(this.invocations).hasValue(1);
    }

    @Test
    void shouldRegisterOnlyOnceWhenStartedConcurrently() throws Exception {
        // Given
        CountDownLatch releaseResponse = new CountDownLatch(1);
        this.responseGate.set(releaseResponse);
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofSeconds(2));
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When & Then
        try {
            Future<?> firstStart = executor.submit(lifecycle::start);
            assertThat(this.requestReceived.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> secondStart = executor.submit(lifecycle::start);
            releaseResponse.countDown();
            firstStart.get(3, TimeUnit.SECONDS);
            secondStart.get(3, TimeUnit.SECONDS);

            assertThat(this.invocations).hasValue(1);
            assertThat(lifecycle.isRunning()).isTrue();
        } finally {
            releaseResponse.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void shouldRejectControlPlaneFailureResponse() {
        // Given
        this.responseStatus.set(422);
        this.responseBody.set("""
                {"success":false,"code":"GATEWAY-0001","message":"Gateway group missing","data":{}}
                """);
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofSeconds(2));

        // When & Then
        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gateway node registration failed with status 422 and code GATEWAY-0001");
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldRejectUnreadableControlPlaneResponse() {
        // Given
        this.responseBody.set("not-json");
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofSeconds(2));

        // When & Then
        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to deserialize gateway node registration response")
                .hasCauseInstanceOf(IOException.class);
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldApplyRegistrationRequestTimeout() {
        // Given
        CountDownLatch releaseResponse = new CountDownLatch(1);
        this.responseGate.set(releaseResponse);
        GatewayNodeLifecycle lifecycle = lifecycle(Duration.ofMillis(100));

        // When & Then
        try {
            assertThatThrownBy(lifecycle::start)
                    .isInstanceOf(CompletionException.class)
                    .hasRootCauseInstanceOf(HttpTimeoutException.class);
            assertThat(lifecycle.isRunning()).isFalse();
        } finally {
            releaseResponse.countDown();
        }
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        GatewayControlPlaneProperties properties = properties(Duration.ofSeconds(2));

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLifecycle(null, this.objectMapper, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("httpClient must not be null");
        assertThatThrownBy(() -> new GatewayNodeLifecycle(HttpClient.newHttpClient(), null, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("objectMapper must not be null");
        assertThatThrownBy(() -> new GatewayNodeLifecycle(HttpClient.newHttpClient(), this.objectMapper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties must not be null");
    }

    private GatewayNodeLifecycle lifecycle(Duration requestTimeout) {
        return new GatewayNodeLifecycle(HttpClient.newHttpClient(), this.objectMapper, properties(requestTimeout));
    }

    private GatewayControlPlaneProperties properties(Duration requestTimeout) {
        return new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:" + this.server.getAddress().getPort()),
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"), requestTimeout);
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        this.invocations.incrementAndGet();
        this.requestReceived.countDown();
        this.requestMethod.set(exchange.getRequestMethod());
        this.requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        try (InputStream inputStream = exchange.getRequestBody()) {
            this.requestBody.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        CountDownLatch gate = this.responseGate.get();
        if (gate != null) {
            awaitResponseRelease(gate);
        }
        byte[] response = this.responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(this.responseStatus.get(), response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static void awaitResponseRelease(CountDownLatch gate) throws IOException {
        try {
            if (!gate.await(3, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting to release control-plane response");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Control-plane test server was interrupted", exception);
        }
    }
}
