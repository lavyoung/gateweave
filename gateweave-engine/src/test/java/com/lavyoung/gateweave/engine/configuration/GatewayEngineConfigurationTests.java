package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.ControlPlaneHttpApi;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayControlPlaneProperties;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayNodeLifecycle;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.GatewayInvocationArgumentsResolver;
import com.lavyoung.gateweave.engine.application.mapper.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayEngineConfigurationTests {

    @Test
    void shouldPrepareLocalRouteResourcesBeforeCreatingRouteTable() {
        // Given
        var configuration = new GatewayEngineConfiguration();
        var target = target();
        var route = route("route-local", "/local", target);
        var properties = new GatewayRouteProperties(List.of(route));
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);

        // When
        var routeTable = configuration.gatewayRouteTable(
                properties, new GatewayRouteConfigurationMapper(), invocationDispatcher);

        // Then
        assertThat(routeTable.match("GET", URI.create("/local"))).isPresent();
        verify(invocationDispatcher).prepare(target);
    }

    @Test
    void shouldNotPrepareLocalResourcesWhenAnyRouteCannotBeMapped() {
        // Given
        var configuration = new GatewayEngineConfiguration();
        var target = target();
        var properties = new GatewayRouteProperties(List.of(
                route("route-valid", "/valid", target), route("route-invalid", "invalid", target)));
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);

        // When & Then
        assertThatThrownBy(() -> configuration.gatewayRouteTable(
                properties, new GatewayRouteConfigurationMapper(), invocationDispatcher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must start with '/'");
        verifyNoInteractions(invocationDispatcher);
    }

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
    void shouldContinueScheduledTasksAfterOneExecutionFails() throws InterruptedException {
        // Given
        var configuration = new GatewayEngineConfiguration();
        var scheduler = configuration.gatewayControlPlaneTaskScheduler();
        var invocations = new AtomicInteger();
        var secondInvocation = new CountDownLatch(1);

        try {
            scheduler.initialize();

            // When
            var task = scheduler.scheduleWithFixedDelay(() -> {
                if (invocations.incrementAndGet() == 1) {
                    throw new IllegalStateException("expected refresh failure");
                }
                secondInvocation.countDown();
            }, Duration.ofMillis(10));

            // Then
            assertThat(secondInvocation.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(invocations).hasValueGreaterThanOrEqualTo(2);
            task.cancel(true);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void shouldStartHttpToRpcPipelineAndReleaseRuntimeResources() throws Exception {
        // Given
        var port = findAvailablePort();
        var controlPlaneServer = createControlPlaneServer();
        controlPlaneServer.start();
        var referenceCache = mock(SimpleReferenceCache.class);
        var genericService = mock(GenericService.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class))).thenReturn(genericService);
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"})))
                .thenReturn(CompletableFuture.completedFuture("hi gateweave"));

        var serverReference = new AtomicReference<NettyGatewayServer>();
        var bossGroupReference = new AtomicReference<EventLoopGroup>();
        var workerGroupReference = new AtomicReference<EventLoopGroup>();
        var httpExecutorReference = new AtomicReference<ThreadPoolExecutor>();
        var controlPlaneSchedulerReference = new AtomicReference<ThreadPoolTaskScheduler>();
        var authenticationService = mock(GatewaySessionAuthenticationService.class);
        when(authenticationService.authenticate(any(GatewaySession.class))).thenReturn(true);
        var contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class, RestClientAutoConfiguration.class))
                .withUserConfiguration(GatewayEngineConfiguration.class)
                .withBean(GatewaySessionAuthenticationService.class, () -> authenticationService)
                .withPropertyValues(
                        "gateweave.control-plane.base-uri=http://127.0.0.1:"
                                + controlPlaneServer.getAddress().getPort(),
                        "gateweave.control-plane.node-id=node-1",
                        "gateweave.control-plane.gateway-group-id=default",
                        "gateweave.control-plane.node-name=Gateway Node 1",
                        "gateweave.control-plane.advertised-address=http://127.0.0.1:" + port,
                        "gateweave.control-plane.request-timeout=3s",
                        "gateweave.control-plane.refresh-interval=PT1H",
                        "gateweave.server.host=127.0.0.1",
                        "gateweave.server.port=" + port,
                        "gateweave.server.max-content-length=1048576",
                        "gateweave.server.boss-threads=2",
                        "gateweave.server.worker-threads=3",
                        "gateweave.server.connection-backlog=512",
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
                        "gateweave.routing.routes[0].authentication-mode=ANONYMOUS",
                        "gateweave.routing.routes[0].target.protocol=dubbo",
                        "gateweave.routing.routes[0].target.service=cn.example.ActivityBooth",
                        "gateweave.routing.routes[0].target.operation=sayHi",
                        "gateweave.routing.routes[0].target.parameters[0].name=name",
                        "gateweave.routing.routes[0].target.parameters[0].type-name=java.lang.String",
                        "gateweave.routing.routes[0].target.timeout=3s",
                        "gateweave.routing.routes[0].target.attributes.version=1.0.0",
                        "gateweave.routing.routes[0].attributes.source=test");

        // When & Then
        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache).thenCallRealMethod();
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(GatewayServerProperties.class);
                assertThat(context).hasSingleBean(GatewayRouteProperties.class);
                assertThat(context).hasSingleBean(GatewayRpcProperties.class);
                assertThat(context).hasSingleBean(GatewayHttpProperties.class);
                assertThat(context).hasSingleBean(GatewayControlPlaneProperties.class);
                assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class);
                assertThat(context).hasSingleBean(ControlPlaneHttpApi.class);
                assertThat(context).hasSingleBean(GatewayControlPlaneClient.class);
                assertThat(context).hasSingleBean(GatewayConfigurationSynchronizer.class);
                assertThat(context).hasSingleBean(GatewayNodeLifecycle.class);
                assertThat(context).hasSingleBean(GatewayRouteTable.class);
                assertThat(context).hasSingleBean(GenericReferenceRegistry.class);
                assertThat(context).hasSingleBean(DubboProtocolInvoker.class);
                assertThat(context).hasSingleBean(HttpProtocolInvoker.class);
                assertThat(context).hasSingleBean(HttpClient.class);
                assertThat(context.getBeansOfType(ProtocolInvoker.class)).hasSize(2);
                assertThat(context).hasSingleBean(ProtocolInvocationDispatcher.class);
                assertThat(context).hasSingleBean(GatewaySessionFactory.class);
                assertThat(context).hasSingleBean(ObjectMapper.class);
                assertThat(context).hasSingleBean(GatewayInvocationArgumentsResolver.class);
                assertThat(context).hasSingleBean(InvocationRequestMapper.class);
                assertThat(context).hasSingleBean(HandleGatewayRequestUseCase.class);
                assertThat(context).hasSingleBean(NettyGatewayServer.class);

                var server = context.getBean(NettyGatewayServer.class);
                var nodeLifecycle = context.getBean(GatewayNodeLifecycle.class);
                var bossGroup = context.getBean("gatewayBossGroup", NioEventLoopGroup.class);
                var workerGroup = context.getBean("gatewayWorkerGroup", NioEventLoopGroup.class);
                var serverBootstrap = context.getBean(ServerBootstrap.class);
                var httpExecutor = context.getBean("gatewayHttpExecutor", ThreadPoolExecutor.class);
                var gatewayHttpClient = context.getBean(HttpClient.class);
                var controlPlaneProperties = context.getBean(GatewayControlPlaneProperties.class);
                var controlPlaneScheduler = context.getBean(ThreadPoolTaskScheduler.class);
                serverReference.set(server);
                bossGroupReference.set(bossGroup);
                workerGroupReference.set(workerGroup);
                httpExecutorReference.set(httpExecutor);
                controlPlaneSchedulerReference.set(controlPlaneScheduler);

                assertThat(server.isRunning()).isTrue();
                assertThat(nodeLifecycle.isRunning()).isTrue();
                assertThat(nodeLifecycle.getPhase()).isLessThan(server.getPhase());
                assertThat(bossGroup.isShuttingDown()).isFalse();
                assertThat(workerGroup.isShuttingDown()).isFalse();
                assertThat(bossGroup.executorCount()).isEqualTo(2);
                assertThat(workerGroup.executorCount()).isEqualTo(3);
                assertThat(serverBootstrap.config().options()).containsEntry(ChannelOption.SO_BACKLOG, 512);
                assertThat(httpExecutor.getCorePoolSize()).isEqualTo(2);
                assertThat(httpExecutor.getMaximumPoolSize()).isEqualTo(8);
                assertThat(httpExecutor.getQueue().remainingCapacity()).isEqualTo(256);
                assertThat(httpExecutor.getRejectedExecutionHandler())
                        .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
                assertThat(controlPlaneScheduler.getPoolSize()).isEqualTo(1);
                assertThat(controlPlaneScheduler.getThreadNamePrefix()).isEqualTo("gateweave-control-plane-");
                assertThat(gatewayHttpClient.connectTimeout()).contains(Duration.ofSeconds(2));
                assertThat(gatewayHttpClient.executor()).containsSame(httpExecutor);
                assertThat(gatewayHttpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
                assertThat(gatewayHttpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
                assertThat(controlPlaneProperties.baseUri()).isEqualTo(
                        URI.create("http://127.0.0.1:" + controlPlaneServer.getAddress().getPort()));
                assertThat(controlPlaneProperties.nodeId()).isEqualTo("node-1");
                assertThat(controlPlaneProperties.gatewayGroupId()).isEqualTo("default");
                assertThat(controlPlaneProperties.nodeName()).isEqualTo("Gateway Node 1");
                assertThat(controlPlaneProperties.advertisedAddress())
                        .isEqualTo(URI.create("http://127.0.0.1:" + port));
                assertThat(controlPlaneProperties.requestTimeout()).isEqualTo(Duration.ofSeconds(3));
                assertThat(controlPlaneProperties.refreshInterval()).isEqualTo(Duration.ofHours(1));

                var request = HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + port + "/sayHi?name=gateweave"))
                        .GET()
                        .build();
                var response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo("hi gateweave");
                assertThat(response.headers().firstValue("content-type")).contains("text/plain; charset=UTF-8");
                verify(authenticationService).authenticate(any(GatewaySession.class));
            });
        } finally {
            controlPlaneServer.stop(0);
        }

        var bossGroup = bossGroupReference.get();
        var workerGroup = workerGroupReference.get();
        bossGroup.terminationFuture().syncUninterruptibly();
        workerGroup.terminationFuture().syncUninterruptibly();

        assertThat(serverReference.get().isRunning()).isFalse();
        assertThat(bossGroup.isTerminated()).isTrue();
        assertThat(workerGroup.isTerminated()).isTrue();
        assertThat(httpExecutorReference.get().isShutdown()).isTrue();
        assertThat(controlPlaneSchedulerReference.get().getScheduledThreadPoolExecutor().isShutdown()).isTrue();
        verify(referenceCache).destroyAll();
    }

    private static int findAvailablePort() throws Exception {
        try (var socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static InvocationTargetConfiguration target() {
        return new InvocationTargetConfiguration("dubbo", "cn.example.ActivityBooth", "sayHi", List.of(),
                Duration.ofSeconds(3), Map.of("version", "1.0.0"));
    }

    private static RouteConfiguration route(
            String routeId, String path, InvocationTargetConfiguration target) {
        return new RouteConfiguration(routeId, path, "GET", "activity-service", RouteAuthenticationMode.ANONYMOUS,
                target, Map.of());
    }

    private static HttpServer createControlPlaneServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/gateway-nodes", exchange -> {
            byte[] response = """
                    {"success":true,"code":"SUCCESS","message":"Success","data":{}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        server.createContext("/api/v1/gateway-groups/default/configuration-releases/latest", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        return server;
    }
}
