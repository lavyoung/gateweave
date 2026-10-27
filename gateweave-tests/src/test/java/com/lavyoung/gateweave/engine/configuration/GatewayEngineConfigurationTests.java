package com.lavyoung.gateweave.engine.configuration;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.EventLoopGroup;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.session.GatewaySessionFactory;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;
import com.lavyoung.gateweave.plugin.invoker.http.HttpProtocolInvoker;
import com.lavyoung.gateweave.plugin.invoker.rpc.DubboProtocolInvoker;
import com.lavyoung.gateweave.plugin.invoker.rpc.GenericReferenceRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayEngineConfigurationTests {

    @Test
    void shouldRejectTasksWhenHttpExecutorIsSaturated() throws InterruptedException {
        // Given
        var configuration = new GatewayEngineConfiguration();
        var properties = new GatewayHttpProperties(Duration.ofSeconds(2), 1, 1, 1, Duration.ofSeconds(30));
        var executor = configuration.gatewayHttpExecutor(properties);
        var taskStarted = new CountDownLatch(1);
        var releaseTask = new CountDownLatch(1);

        try {
            executor.execute(() -> {
                taskStarted.countDown();
                try {
                    releaseTask.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(taskStarted.await(1, TimeUnit.SECONDS)).isTrue();
            executor.execute(() -> {
                // Occupy the only queue slot while the first task is running.
            });

            // When & Then
            assertThatThrownBy(() -> executor.execute(() -> {
                // The saturated executor must reject this task.
            })).isInstanceOf(RejectedExecutionException.class);
        } finally {
            releaseTask.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void shouldStartHttpToRpcPipelineAndReleaseRuntimeResources() throws Exception {
        // Given
        var port = findAvailablePort();
        var referenceCache = mock(SimpleReferenceCache.class);
        var genericService = mock(GenericService.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class))).thenReturn(genericService);
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{""})))
                .thenReturn(CompletableFuture.completedFuture("hi gateweave"));

        var serverReference = new AtomicReference<NettyGatewayServer>();
        var bossGroupReference = new AtomicReference<EventLoopGroup>();
        var workerGroupReference = new AtomicReference<EventLoopGroup>();
        var httpExecutorReference = new AtomicReference<ThreadPoolExecutor>();
        var contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(GatewayEngineConfiguration.class)
                .withPropertyValues(
                        "gateweave.server.host=127.0.0.1",
                        "gateweave.server.port=" + port,
                        "gateweave.server.max-content-length=1048576",
                        "gateweave.server.boss-threads=1",
                        "gateweave.server.worker-threads=1",
                        "gateweave.rpc.application-name=gateweave",
                        "gateweave.rpc.registry-address=zookeeper://127.0.0.1:2181",
                        "gateweave.rpc.discovery-mode=INTERFACE",
                        "gateweave.http.connect-timeout=2s",
                        "gateweave.http.core-threads=2",
                        "gateweave.http.maximum-threads=8",
                        "gateweave.http.queue-capacity=256",
                        "gateweave.http.keep-alive=30s",
                        "gateweave.routing.routes[0].route-id=route-say-hi",
                        "gateweave.routing.routes[0].path=/sayHi",
                        "gateweave.routing.routes[0].method=GET",
                        "gateweave.routing.routes[0].service-id=activity-service",
                        "gateweave.routing.routes[0].target.protocol=dubbo",
                        "gateweave.routing.routes[0].target.service=cn.example.ActivityBooth",
                        "gateweave.routing.routes[0].target.operation=sayHi",
                        "gateweave.routing.routes[0].target.parameter-types[0]=java.lang.String",
                        "gateweave.routing.routes[0].target.timeout=3s",
                        "gateweave.routing.routes[0].target.attributes.version=1.0.0",
                        "gateweave.routing.routes[0].attributes.source=test");

        // When & Then
        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(GatewayServerProperties.class);
                assertThat(context).hasSingleBean(GatewayRouteProperties.class);
                assertThat(context).hasSingleBean(GatewayRpcProperties.class);
                assertThat(context).hasSingleBean(GatewayHttpProperties.class);
                assertThat(context).hasSingleBean(GatewayRouteTable.class);
                assertThat(context).hasSingleBean(GenericReferenceRegistry.class);
                assertThat(context).hasSingleBean(DubboProtocolInvoker.class);
                assertThat(context).hasSingleBean(HttpProtocolInvoker.class);
                assertThat(context).hasSingleBean(HttpClient.class);
                assertThat(context.getBeansOfType(ProtocolInvoker.class)).hasSize(2);
                assertThat(context).hasSingleBean(ProtocolInvocationDispatcher.class);
                assertThat(context).hasSingleBean(GatewaySessionFactory.class);
                assertThat(context).hasSingleBean(InvocationRequestMapper.class);
                assertThat(context).hasSingleBean(HandleGatewayRequestUseCase.class);
                assertThat(context).hasSingleBean(NettyGatewayServer.class);

                var server = context.getBean(NettyGatewayServer.class);
                var bossGroup = context.getBean("gatewayBossGroup", EventLoopGroup.class);
                var workerGroup = context.getBean("gatewayWorkerGroup", EventLoopGroup.class);
                var httpExecutor = context.getBean("gatewayHttpExecutor", ThreadPoolExecutor.class);
                var gatewayHttpClient = context.getBean(HttpClient.class);
                serverReference.set(server);
                bossGroupReference.set(bossGroup);
                workerGroupReference.set(workerGroup);
                httpExecutorReference.set(httpExecutor);

                assertThat(server.isRunning()).isTrue();
                assertThat(bossGroup.isShuttingDown()).isFalse();
                assertThat(workerGroup.isShuttingDown()).isFalse();
                assertThat(httpExecutor.getCorePoolSize()).isEqualTo(2);
                assertThat(httpExecutor.getMaximumPoolSize()).isEqualTo(8);
                assertThat(httpExecutor.getQueue().remainingCapacity()).isEqualTo(256);
                assertThat(httpExecutor.getRejectedExecutionHandler())
                        .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
                assertThat(gatewayHttpClient.connectTimeout()).contains(Duration.ofSeconds(2));
                assertThat(gatewayHttpClient.executor()).containsSame(httpExecutor);
                assertThat(gatewayHttpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
                assertThat(gatewayHttpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);

                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/sayHi"))
                        .GET()
                        .build();
                var response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo("hi gateweave");
                assertThat(response.headers().firstValue("content-type")).contains("text/plain; charset=UTF-8");
            });
        }

        var bossGroup = bossGroupReference.get();
        var workerGroup = workerGroupReference.get();
        bossGroup.terminationFuture().syncUninterruptibly();
        workerGroup.terminationFuture().syncUninterruptibly();

        assertThat(serverReference.get().isRunning()).isFalse();
        assertThat(bossGroup.isTerminated()).isTrue();
        assertThat(workerGroup.isTerminated()).isTrue();
        assertThat(httpExecutorReference.get().isShutdown()).isTrue();
        verify(referenceCache).destroyAll();
    }

    private static int findAvailablePort() throws Exception {
        try (var socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
