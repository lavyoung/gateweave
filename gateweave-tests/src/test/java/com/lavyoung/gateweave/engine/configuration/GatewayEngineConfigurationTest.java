package com.lavyoung.gateweave.engine.configuration;

import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayEngineConfigurationTest {

    @Test
    void shouldStartCompleteHttpPipelineAndReleaseRuntimeResources() throws Exception {
        // Given
        var port = findAvailablePort();
        var serverReference = new AtomicReference<NettyGatewayServer>();
        var bossGroupReference = new AtomicReference<EventLoopGroup>();
        var workerGroupReference = new AtomicReference<EventLoopGroup>();
        var contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(GatewayEngineConfiguration.class)
                .withPropertyValues(
                        "gateweave.server.host=127.0.0.1",
                        "gateweave.server.port=" + port,
                        "gateweave.server.max-content-length=1048576",
                        "gateweave.server.boss-threads=1",
                        "gateweave.server.worker-threads=1");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GatewayServerProperties.class);
            assertThat(context).hasSingleBean(NettyGatewayServer.class);

            var server = context.getBean(NettyGatewayServer.class);
            var bossGroup = context.getBean("gatewayBossGroup", EventLoopGroup.class);
            var workerGroup = context.getBean("gatewayWorkerGroup", EventLoopGroup.class);
            serverReference.set(server);
            bossGroupReference.set(bossGroup);
            workerGroupReference.set(workerGroup);

            assertThat(server.isRunning()).isTrue();
            assertThat(bossGroup.isShuttingDown()).isFalse();
            assertThat(workerGroup.isShuttingDown()).isFalse();

            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/queryUserInfo?id=1"))
                    .GET()
                    .build();
            var response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("GateWeave received request: GET /queryUserInfo?id=1");
            assertThat(response.headers().firstValue("content-type")).contains("text/plain; charset=UTF-8");
        });

        var bossGroup = bossGroupReference.get();
        var workerGroup = workerGroupReference.get();
        bossGroup.terminationFuture().syncUninterruptibly();
        workerGroup.terminationFuture().syncUninterruptibly();

        assertThat(serverReference.get().isRunning()).isFalse();
        assertThat(bossGroup.isTerminated()).isTrue();
        assertThat(workerGroup.isTerminated()).isTrue();
    }

    private static int findAvailablePort() throws Exception {
        try (var socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
