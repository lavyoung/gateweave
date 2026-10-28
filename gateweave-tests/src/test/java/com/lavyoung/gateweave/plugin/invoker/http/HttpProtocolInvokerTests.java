package com.lavyoung.gateweave.plugin.invoker.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.GenericInvocationArguments;
import com.lavyoung.gateweave.contract.extension.InvocationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpProtocolInvokerTests {

    private static final GenericInvocationArguments EMPTY_ARGUMENTS =
            new GenericInvocationArguments(List.of(), List.of());

    private final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedTraceId = new AtomicReference<>();
    private final AtomicReference<String> receivedHost = new AtomicReference<>();
    private final AtomicReference<String> receivedConnection = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/echo", this::handleEchoRequest);
        this.server.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    @Test
    void shouldExposeHttpProtocol() {
        // Given
        HttpProtocolInvoker invoker = new HttpProtocolInvoker(HttpClient.newHttpClient());

        // When & Then
        assertThat(invoker.protocol()).isEqualTo("http");
    }

    @Test
    void shouldInvokeHttpServiceWithMethodHeadersAndPayload() throws Exception {
        // Given
        HttpProtocolInvoker invoker = new HttpProtocolInvoker(HttpClient.newHttpClient());
        InvocationRequest request = new InvocationRequest(
                "request-1", "route-http", "http", baseUrl(), "/echo", EMPTY_ARGUMENTS, Duration.ofSeconds(2),
                Map.of("X-Trace-Id", "trace-1", "Host", "forged-host", "Connection", "close"),
                Map.of("method", "post"), "GateWeave".getBytes(StandardCharsets.UTF_8));

        // When
        String response = invoker.invoke(request).toCompletableFuture().get(3, TimeUnit.SECONDS);

        // Then
        assertThat(response).isEqualTo("received:GateWeave");
        assertThat(this.receivedMethod.get()).isEqualTo("POST");
        assertThat(this.receivedBody.get()).isEqualTo("GateWeave");
        assertThat(this.receivedTraceId.get()).isEqualTo("trace-1");
        assertThat(this.receivedHost.get()).isNotEqualTo("forged-host");
        assertThat(this.receivedConnection.get()).isNotEqualTo("close");
    }

    @Test
    void shouldSendNoBodyWhenPayloadIsEmpty() throws Exception {
        // Given
        HttpProtocolInvoker invoker = new HttpProtocolInvoker(HttpClient.newHttpClient());
        InvocationRequest request = new InvocationRequest(
                "request-2", "route-http", "http", baseUrl(), "/echo", EMPTY_ARGUMENTS, Duration.ofSeconds(2),
                Map.of(), Map.of("method", "get"), new byte[0]);

        // When
        String response = invoker.invoke(request).toCompletableFuture().get(3, TimeUnit.SECONDS);

        // Then
        assertThat(response).isEqualTo("received:");
        assertThat(this.receivedMethod.get()).isEqualTo("GET");
        assertThat(this.receivedBody.get()).isEmpty();
    }

    @Test
    void shouldApplyInvocationTimeout() {
        // Given
        CountDownLatch releaseResponse = new CountDownLatch(1);
        this.server.createContext("/slow", exchange -> handleDelayedResponse(exchange, releaseResponse));
        HttpProtocolInvoker invoker = new HttpProtocolInvoker(HttpClient.newHttpClient());
        InvocationRequest request = new InvocationRequest(
                "request-3", "route-http", "http", baseUrl(), "/slow", EMPTY_ARGUMENTS, Duration.ofMillis(200),
                Map.of(), Map.of("method", "get"), new byte[0]);

        // When & Then
        try {
            assertThatThrownBy(() -> invoker.invoke(request).toCompletableFuture().get(3, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(HttpTimeoutException.class);
        } finally {
            releaseResponse.countDown();
        }
    }

    @Test
    void shouldRejectMissingClientOrRequest() {
        // Given
        HttpProtocolInvoker invoker = new HttpProtocolInvoker(HttpClient.newHttpClient());

        // When & Then
        assertThatThrownBy(() -> new HttpProtocolInvoker(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("httpClient must not be null");
        assertThatThrownBy(() -> invoker.invoke(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + this.server.getAddress().getPort();
    }

    private void handleEchoRequest(HttpExchange exchange) throws IOException {
        this.receivedMethod.set(exchange.getRequestMethod());
        this.receivedTraceId.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
        this.receivedHost.set(exchange.getRequestHeaders().getFirst("Host"));
        this.receivedConnection.set(exchange.getRequestHeaders().getFirst("Connection"));
        try (InputStream inputStream = exchange.getRequestBody()) {
            this.receivedBody.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        writeResponse(exchange, "received:" + this.receivedBody.get());
    }

    private void handleDelayedResponse(HttpExchange exchange, CountDownLatch releaseResponse) throws IOException {
        try {
            releaseResponse.await(3, TimeUnit.SECONDS);
            writeResponse(exchange, "too late");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP test server was interrupted", exception);
        }
    }

    private void writeResponse(HttpExchange exchange, String content) throws IOException {
        byte[] response = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
