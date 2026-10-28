package com.lavyoung.gateweave.engine.configuration;

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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.netty.channel.EventLoopGroup;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class GatewayHttpEndToEndTests {

    @Test
    void shouldProxyRequestFromNettyGatewayToHttpService() throws Exception {
        // Given
        int gatewayPort = findAvailablePort();
        AtomicReference<String> downstreamMethod = new AtomicReference<>();
        AtomicReference<String> downstreamBody = new AtomicReference<>();
        AtomicReference<String> downstreamTraceId = new AtomicReference<>();
        HttpServer downstreamServer = createDownstreamServer(downstreamMethod, downstreamBody, downstreamTraceId);
        int downstreamPort = downstreamServer.getAddress().getPort();
        SimpleReferenceCache referenceCache = mock(SimpleReferenceCache.class);
        AtomicReference<NettyGatewayServer> gatewayServerReference = new AtomicReference<>();
        AtomicReference<EventLoopGroup> bossGroupReference = new AtomicReference<>();
        AtomicReference<EventLoopGroup> workerGroupReference = new AtomicReference<>();
        AtomicReference<ThreadPoolExecutor> httpExecutorReference = new AtomicReference<>();
        ApplicationContextRunner contextRunner = createContextRunner(gatewayPort, downstreamPort);

        // When & Then
        downstreamServer.start();
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
            });
        } finally {
            downstreamServer.stop(0);
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

    private static ApplicationContextRunner createContextRunner(int gatewayPort, int downstreamPort) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(GatewayEngineConfiguration.class)
                .withPropertyValues(
                        "gateweave.server.host=127.0.0.1",
                        "gateweave.server.port=" + gatewayPort,
                        "gateweave.server.max-content-length=1048576",
                        "gateweave.server.boss-threads=1",
                        "gateweave.server.worker-threads=1",
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
                        "gateweave.routing.routes[0].target.protocol=http",
                        "gateweave.routing.routes[0].target.service=http://127.0.0.1:" + downstreamPort,
                        "gateweave.routing.routes[0].target.operation=/downstream",
                        "gateweave.routing.routes[0].target.timeout=2s",
                        "gateweave.routing.routes[0].target.attributes.method=POST",
                        "gateweave.routing.routes[0].attributes.source=test");
    }

    private static HttpServer createDownstreamServer(AtomicReference<String> method, AtomicReference<String> body,
            AtomicReference<String> traceId) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/downstream", exchange -> handleDownstreamRequest(exchange, method, body, traceId));
        return server;
    }

    private static void handleDownstreamRequest(HttpExchange exchange, AtomicReference<String> method,
            AtomicReference<String> body, AtomicReference<String> traceId) throws IOException {
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

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
