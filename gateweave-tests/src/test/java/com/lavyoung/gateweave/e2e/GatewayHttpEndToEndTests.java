package com.lavyoung.gateweave.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.netty.channel.EventLoopGroup;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import com.lavyoung.gateweave.engine.configuration.GatewayEngineConfiguration;
import com.lavyoung.gateweave.engine.configuration.GatewaySecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class GatewayHttpEndToEndTests {

    private static final String JWT_ISSUER = "gateweave-issuer";
    private static final String JWT_AUDIENCE = "gateweave-engine";
    private static final byte[] JWT_KEY_BYTES =
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
    private static final String JWT_KEY_BASE64 = Base64.getEncoder().encodeToString(JWT_KEY_BYTES);

    @Test
    void shouldProxyRequestFromNettyGatewayToHttpService() throws Exception {
        // Given
        int gatewayPort = findAvailablePort();
        AtomicReference<String> downstreamMethod = new AtomicReference<>();
        AtomicReference<String> downstreamBody = new AtomicReference<>();
        AtomicReference<String> downstreamTraceId = new AtomicReference<>();
        AtomicInteger downstreamInvocations = new AtomicInteger();
        HttpServer downstreamServer = createDownstreamServer(
                downstreamMethod, downstreamBody, downstreamTraceId, downstreamInvocations);
        int downstreamPort = downstreamServer.getAddress().getPort();
        HttpServer controlPlaneServer = createControlPlaneServer();
        int controlPlanePort = controlPlaneServer.getAddress().getPort();
        SimpleReferenceCache referenceCache = mock(SimpleReferenceCache.class);
        AtomicReference<NettyGatewayServer> gatewayServerReference = new AtomicReference<>();
        AtomicReference<EventLoopGroup> bossGroupReference = new AtomicReference<>();
        AtomicReference<EventLoopGroup> workerGroupReference = new AtomicReference<>();
        AtomicReference<ThreadPoolExecutor> httpExecutorReference = new AtomicReference<>();
        ApplicationContextRunner contextRunner = createContextRunner(
                gatewayPort, downstreamPort, controlPlanePort, "ANONYMOUS");

        // When & Then
        downstreamServer.start();
        controlPlaneServer.start();
        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                gatewayServerReference.set(context.getBean(NettyGatewayServer.class));
                bossGroupReference.set(context.getBean("gatewayBossGroup", EventLoopGroup.class));
                workerGroupReference.set(context.getBean("gatewayWorkerGroup", EventLoopGroup.class));
                httpExecutorReference.set(context.getBean("gatewayHttpExecutor", ThreadPoolExecutor.class));

                HttpRequest request = HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + gatewayPort + "/proxy/http"))
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .header("X-Trace-Id", "trace-http-1")
                        .POST(HttpRequest.BodyPublishers.ofString("GateWeave", StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo("downstream:GateWeave");
                assertThat(downstreamMethod.get()).isEqualTo("POST");
                assertThat(downstreamBody.get()).isEqualTo("GateWeave");
                assertThat(downstreamTraceId.get()).isEqualTo("trace-http-1");
                assertThat(downstreamInvocations.get()).isEqualTo(1);
            });
        } finally {
            downstreamServer.stop(0);
            controlPlaneServer.stop(0);
        }

        EventLoopGroup bossGroup = bossGroupReference.get();
        EventLoopGroup workerGroup = workerGroupReference.get();
        bossGroup.terminationFuture().syncUninterruptibly();
        workerGroup.terminationFuture().syncUninterruptibly();

        assertThat(gatewayServerReference.get().isRunning()).isFalse();
        assertThat(bossGroup.isTerminated()).isTrue();
        assertThat(workerGroup.isTerminated()).isTrue();
        assertThat(httpExecutorReference.get().isShutdown()).isTrue();
    }

    @Test
    void shouldRejectMissingJwtAndProxyRequestWithValidJwt() throws Exception {
        // Given
        int gatewayPort = findAvailablePort();
        AtomicReference<String> downstreamMethod = new AtomicReference<>();
        AtomicReference<String> downstreamBody = new AtomicReference<>();
        AtomicReference<String> downstreamTraceId = new AtomicReference<>();
        AtomicInteger downstreamInvocations = new AtomicInteger();
        HttpServer downstreamServer = createDownstreamServer(
                downstreamMethod, downstreamBody, downstreamTraceId, downstreamInvocations);
        int downstreamPort = downstreamServer.getAddress().getPort();
        HttpServer controlPlaneServer = createControlPlaneServer();
        int controlPlanePort = controlPlaneServer.getAddress().getPort();
        SimpleReferenceCache referenceCache = mock(SimpleReferenceCache.class);
        ApplicationContextRunner contextRunner = createContextRunner(
                gatewayPort, downstreamPort, controlPlanePort, "REQUIRED");

        // When & Then
        downstreamServer.start();
        controlPlaneServer.start();
        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();

                HttpRequest missingTokenRequest = HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + gatewayPort + "/proxy/http"))
                        .POST(HttpRequest.BodyPublishers.ofString("GateWeave", StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> unauthorizedResponse = HttpClient.newHttpClient().send(
                        missingTokenRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                assertThat(unauthorizedResponse.statusCode()).isEqualTo(401);
                assertThat(unauthorizedResponse.headers().firstValue("WWW-Authenticate")).contains("Bearer");
                assertThat(downstreamInvocations.get()).isZero();

                HttpRequest authenticatedRequest = HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + gatewayPort + "/proxy/http"))
                        .header("Authorization", "Bearer " + createJwt())
                        .POST(HttpRequest.BodyPublishers.ofString("GateWeave", StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> authenticatedResponse = HttpClient.newHttpClient().send(
                        authenticatedRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                assertThat(authenticatedResponse.statusCode()).isEqualTo(200);
                assertThat(authenticatedResponse.body()).isEqualTo("downstream:GateWeave");
                assertThat(downstreamInvocations.get()).isEqualTo(1);
            });
        } finally {
            downstreamServer.stop(0);
            controlPlaneServer.stop(0);
        }
    }

    private static ApplicationContextRunner createContextRunner(
            int gatewayPort, int downstreamPort, int controlPlanePort, String authenticationMode) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(GatewayEngineConfiguration.class, GatewaySecurityConfiguration.class)
                .withPropertyValues(
                        "gateweave.server.host=127.0.0.1",
                        "gateweave.server.port=" + gatewayPort,
                        "gateweave.server.max-content-length=1048576",
                        "gateweave.server.boss-threads=1",
                        "gateweave.server.worker-threads=1",
                        "gateweave.control-plane.base-uri=http://127.0.0.1:" + controlPlanePort,
                        "gateweave.control-plane.node-id=e2e-node",
                        "gateweave.control-plane.gateway-group-id=default",
                        "gateweave.control-plane.node-name=E2E Gateway Node",
                        "gateweave.control-plane.advertised-address=http://127.0.0.1:" + gatewayPort,
                        "gateweave.control-plane.request-timeout=2s",
                        "gateweave.rpc.application-name=gateweave-http-test",
                        "gateweave.rpc.registry-address=zookeeper://127.0.0.1:2181",
                        "gateweave.rpc.discovery-mode=INTERFACE",
                        "gateweave.http.connect-timeout=2s",
                        "gateweave.http.core-threads=1",
                        "gateweave.http.maximum-threads=2",
                        "gateweave.http.queue-capacity=16",
                        "gateweave.http.keep-alive=30s",
                        "gateweave.routing.routes[0].route-id=route-http-proxy",
                        "gateweave.routing.routes[0].path=/proxy/http",
                        "gateweave.routing.routes[0].method=POST",
                        "gateweave.routing.routes[0].service-id=local-http-service",
                        "gateweave.routing.routes[0].authentication-mode=" + authenticationMode,
                        "gateweave.routing.routes[0].target.protocol=http",
                        "gateweave.routing.routes[0].target.service=http://127.0.0.1:" + downstreamPort,
                        "gateweave.routing.routes[0].target.operation=/downstream",
                        "gateweave.routing.routes[0].target.timeout=2s",
                        "gateweave.routing.routes[0].target.attributes.method=POST",
                        "gateweave.routing.routes[0].attributes.source=test",
                        "gateweave.security.jwt.issuer=" + JWT_ISSUER,
                        "gateweave.security.jwt.audience=" + JWT_AUDIENCE,
                        "gateweave.security.jwt.verification-key-base64=" + JWT_KEY_BASE64,
                        "gateweave.security.jwt.clock-skew=0s",
                        "gateweave.security.jwt.maximum-token-length=4096");
    }

    private static HttpServer createDownstreamServer(AtomicReference<String> method, AtomicReference<String> body,
            AtomicReference<String> traceId, AtomicInteger invocations) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/downstream", exchange -> handleDownstreamRequest(exchange, method, body, traceId, invocations));
        return server;
    }

    private static HttpServer createControlPlaneServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/gateway-nodes", exchange -> {
            byte[] response = """
                    {"success":true,"code":"SUCCESS","message":"Success","data":{}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        return server;
    }

    private static void handleDownstreamRequest(HttpExchange exchange, AtomicReference<String> method,
            AtomicReference<String> body, AtomicReference<String> traceId, AtomicInteger invocations)
            throws IOException {
        invocations.incrementAndGet();
        method.set(exchange.getRequestMethod());
        traceId.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
        try (InputStream inputStream = exchange.getRequestBody()) {
            body.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] response = ("downstream:" + body.get()).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static String createJwt() {
        SecretKey key = new SecretKeySpec(JWT_KEY_BYTES, "HmacSHA256");
        return Jwts.builder()
                .issuer(JWT_ISSUER)
                .audience()
                .add(JWT_AUDIENCE)
                .and()
                .subject("user-10001")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
