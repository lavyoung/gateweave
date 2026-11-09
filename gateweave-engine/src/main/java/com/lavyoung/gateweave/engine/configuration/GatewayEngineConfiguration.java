package com.lavyoung.gateweave.engine.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;
import com.lavyoung.gateweave.engine.adapter.in.netty.GatewayChannelInitializer;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayRequestHandler;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayRequestMapper;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayResponseMapper;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.ControlPlaneHttpApi;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayControlPlaneProperties;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayNodeLeaseManager;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.GatewayNodeLifecycle;
import com.lavyoung.gateweave.engine.adapter.out.controlplane.HttpGatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.GatewayInvocationArgumentsResolver;
import com.lavyoung.gateweave.engine.application.mapper.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.engine.application.service.DefaultGatewayRequestService;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import com.lavyoung.gateweave.engine.application.session.DefaultGatewaySessionFactory;
import com.lavyoung.gateweave.engine.application.session.GatewaySessionFactory;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;
import com.lavyoung.gateweave.plugin.invoker.http.HttpProtocolInvoker;
import com.lavyoung.gateweave.plugin.invoker.rpc.DubboProtocolInvoker;
import com.lavyoung.gateweave.plugin.invoker.rpc.GenericReferenceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 装配网关数据面的应用服务、Netty 适配器及运行时资源。
 *
 * <p>该配置类是最终装配边界。应用层和 Netty 适配器保持无 Spring 依赖，
 * EventLoopGroup 的创建与销毁统一交由 Spring 容器管理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({GatewayServerProperties.class, GatewayRouteProperties.class, GatewayRpcProperties.class,
        GatewayHttpProperties.class, GatewayControlPlaneProperties.class})
public class GatewayEngineConfiguration {

    /**
     * 创建并统一管理 Dubbo 泛化调用引用。
     *
     * <p>具体服务引用由 Dubbo 协议插件按路由目标准备，装配层不解析协议专属属性。</p>
     *
     * @param rpcProperties RPC 运行配置
     * @return Dubbo 泛化调用注册表
     */
    @Bean(destroyMethod = "close")
    public GenericReferenceRegistry genericReferenceRegistry(GatewayRpcProperties rpcProperties) {
        return new GenericReferenceRegistry(
                rpcProperties.applicationName(), rpcProperties.registryAddress(), rpcProperties.discoveryMode());
    }

    /**
     * 创建 Dubbo 协议调用插件。
     *
     * @param referenceRegistry Dubbo 泛化引用注册表
     * @return Dubbo 协议调用插件
     */
    @Bean
    public DubboProtocolInvoker dubboProtocolInvoker(GenericReferenceRegistry referenceRegistry) {
        return new DubboProtocolInvoker(referenceRegistry);
    }

    /**
     * 创建供 HTTP 客户端异步任务使用的有界线程池。
     *
     * <p>队列达到容量且线程数达到上限后直接拒绝新任务，
     * 防止下游服务阻塞时请求任务在进程内无限堆积。</p>
     *
     * @param properties HTTP 客户端运行配置
     * @return HTTP 客户端专用线程池
     */
    @Bean(name = "gatewayHttpExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor gatewayHttpExecutor(GatewayHttpProperties properties) {
        return new ThreadPoolExecutor(properties.coreThreads(), properties.maximumThreads(),
                properties.keepAlive().toMillis(), TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.queueCapacity()),
                new CustomizableThreadFactory("gateweave-http-"), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 创建由网关进程共享的 HTTP 客户端。
     *
     * <p>客户端负责连接复用。单次请求超时仍由 InvocationRequest 提供，
     * 此处只设置建立连接的超时时间。</p>
     *
     * @param executor   HTTP 客户端专用线程池
     * @param properties HTTP 客户端运行配置
     * @return 共享 HTTP 客户端
     */
    @Bean
    public HttpClient gatewayHttpClient(@Qualifier("gatewayHttpExecutor") ThreadPoolExecutor executor,
                                        GatewayHttpProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .executor(executor)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 创建 HTTP 协议调用插件。
     *
     * @param gatewayHttpClient 共享 HTTP 客户端
     * @return HTTP 协议调用插件
     */
    @Bean
    public HttpProtocolInvoker httpProtocolInvoker(HttpClient gatewayHttpClient) {
        return new HttpProtocolInvoker(gatewayHttpClient);
    }

    /**
     * 创建统一协议调用分派器。
     *
     * @param protocolInvokers 当前已经注册的协议调用插件
     * @return 协议调用分派器
     * @throws NullPointerException     protocolInvokers 为 null 时抛出
     * @throws IllegalArgumentException 没有插件或存在重复协议时抛出
     */
    @Bean
    public ProtocolInvocationDispatcher protocolInvocationDispatcher(List<ProtocolInvoker> protocolInvokers) {
        return new ProtocolInvocationDispatcher(protocolInvokers);
    }

    /**
     * 创建根据领域路由表打开请求会话的工厂。
     *
     * @param routeTable 当前生效的领域路由表
     * @return 网关会话工厂
     */
    @Bean
    public GatewaySessionFactory gatewaySessionFactory(GatewayRouteTable routeTable) {
        return new DefaultGatewaySessionFactory(routeTable);
    }

    /**
     * 创建 HTTP 请求参数到泛化调用参数的解析器。
     *
     * @param objectMapper Spring Boot 统一配置的 JSON 解析器
     * @return 调用参数解析器
     */
    @Bean
    public GatewayInvocationArgumentsResolver gatewayInvocationArgumentsResolver(ObjectMapper objectMapper) {
        return new GatewayInvocationArgumentsResolver(objectMapper);
    }

    /**
     * 创建网关会话到协议调用请求的转换器。
     *
     * @param argumentsResolver 调用参数解析器
     * @return 协议调用请求转换器
     */
    @Bean
    public InvocationRequestMapper invocationRequestMapper(GatewayInvocationArgumentsResolver argumentsResolver) {
        return new InvocationRequestMapper(argumentsResolver);
    }

    /**
     * 创建负责完整请求会话编排的网关用例。
     *
     * @param sessionFactory        网关会话工厂
     * @param authenticationService 会话鉴权应用服务
     * @param requestMapper         协议调用请求转换器
     * @param invocationDispatcher  协议调用分派器
     * @return 网关请求用例
     */
    @Bean
    public HandleGatewayRequestUseCase gatewayRequestUseCase(GatewaySessionFactory sessionFactory,
                                                             GatewaySessionAuthenticationService authenticationService,
                                                             InvocationRequestMapper requestMapper,
                                                             ProtocolInvocationDispatcher invocationDispatcher) {
        return new DefaultGatewayRequestService(
                sessionFactory, requestMapper, invocationDispatcher, authenticationService);
    }

    /**
     * 创建控制面路由契约到数据面领域模型的转换器。
     *
     * @return 路由配置转换器
     */
    @Bean
    public GatewayRouteConfigurationMapper gatewayRouteConfigurationMapper() {
        return new GatewayRouteConfigurationMapper();
    }

    /**
     * 根据启动配置建立当前生效的不可变领域路由表。
     *
     * <p>全部路由完成领域转换后，再通过统一协议分派器准备运行时资源。
     * 任一路由转换或资源准备失败时，网关启动失败，不会开放不完整的请求链路。</p>
     *
     * @param properties           绑定完成的路由配置
     * @param mapper               路由契约防腐转换器
     * @param invocationDispatcher 协议调用与资源准备分派器
     * @return 当前生效的领域路由表
     * @throws NullPointerException     参数或路由必要属性为 null 时抛出
     * @throws IllegalArgumentException 路由重复或违反领域不变量时抛出
     * @throws RuntimeException         协议运行时资源准备失败时抛出
     */
    @Bean
    public GatewayRouteTable gatewayRouteTable(
            GatewayRouteProperties properties, GatewayRouteConfigurationMapper mapper,
            ProtocolInvocationDispatcher invocationDispatcher) {
        List<GatewayRoute> routes = properties.routes().stream().map(mapper::toDomain).toList();
        properties.routes().forEach(route -> invocationDispatcher.prepare(route.target()));
        return new GatewayRouteTable(routes);
    }

    /**
     * 创建 Netty 请求转换器。
     *
     * @return Netty 请求转换器
     */
    @Bean
    public NettyGatewayRequestMapper nettyGatewayRequestMapper() {
        return new NettyGatewayRequestMapper();
    }

    /**
     * 创建 Netty 响应转换器。
     *
     * @return Netty 响应转换器
     */
    @Bean
    public NettyGatewayResponseMapper nettyGatewayResponseMapper() {
        return new NettyGatewayResponseMapper();
    }

    /**
     * 创建接收连接的 EventLoopGroup。
     *
     * @param properties 网关服务器配置
     * @return boss EventLoopGroup
     */
    @Bean(name = "gatewayBossGroup", destroyMethod = "shutdownGracefully")
    public EventLoopGroup gatewayBossGroup(GatewayServerProperties properties) {
        DefaultThreadFactory factory = new DefaultThreadFactory("gateweave-netty-boss");
        return new NioEventLoopGroup(properties.bossThreads(), factory);
    }

    /**
     * 创建处理连接读写的 EventLoopGroup。
     *
     * @param properties 网关服务器配置
     * @return worker EventLoopGroup
     */
    @Bean(name = "gatewayWorkerGroup", destroyMethod = "shutdownGracefully")
    public EventLoopGroup gatewayWorkerGroup(GatewayServerProperties properties) {
        DefaultThreadFactory threadFactory = new DefaultThreadFactory("gateweave-netty-worker");
        return new NioEventLoopGroup(properties.workerThreads(), threadFactory);
    }

    /**
     * 创建 HTTP Channel Pipeline 初始化器。
     *
     * @param properties     网关服务器配置
     * @param useCase        网关请求用例
     * @param requestMapper  请求转换器
     * @param responseMapper 响应转换器
     * @return Channel Pipeline 初始化器
     */
    @Bean
    public GatewayChannelInitializer gatewayChannelInitializer(
            GatewayServerProperties properties, HandleGatewayRequestUseCase useCase,
            NettyGatewayRequestMapper requestMapper, NettyGatewayResponseMapper responseMapper) {
        return new GatewayChannelInitializer(properties.maxContentLength(),
                () -> new NettyGatewayRequestHandler(useCase, requestMapper, responseMapper));
    }

    /**
     * 创建 Netty ServerBootstrap。
     *
     * @param bossGroup   接收连接的 EventLoopGroup
     * @param workerGroup 处理连接读写的 EventLoopGroup
     * @param initializer Channel Pipeline 初始化器
     * @param properties  网关服务器配置
     * @return 配置完成的 ServerBootstrap
     */
    @Bean
    public ServerBootstrap gatewayServerBootstrap(
            @Qualifier("gatewayBossGroup") EventLoopGroup bossGroup,
            @Qualifier("gatewayWorkerGroup") EventLoopGroup workerGroup, GatewayChannelInitializer initializer,
            GatewayServerProperties properties) {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, properties.connectionBacklog())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(initializer);
    }

    /**
     * 创建并自动启动 Netty 网关服务器。
     *
     * @param bootstrap  Netty ServerBootstrap
     * @param properties 网关服务器配置
     * @return Netty 网关服务器
     */
    @Bean
    public NettyGatewayServer nettyGatewayServer(ServerBootstrap bootstrap, GatewayServerProperties properties) {
        InetSocketAddress address = new InetSocketAddress(properties.host(), properties.port());
        return new NettyGatewayServer(bootstrap, address);
    }

    /**
     * 创建由 Spring HTTP Interface 实现的控制面 HTTP 代理。
     *
     * <p>复用 Boot 管理的 RestClient Builder，从而复用统一 ObjectMapper 和消息转换器；
     * 底层连接继续由网关共享的 JDK HttpClient 管理。</p>
     *
     * @param restClientBuilder      Boot 管理的 RestClient Builder
     * @param gatewayHttpClient      共享 JDK HTTP 客户端
     * @param controlPlaneProperties 控制面连接和超时配置
     * @return 控制面 HTTP 代理
     */
    @Bean
    public ControlPlaneHttpApi controlPlaneHttpApi(
            RestClient.Builder restClientBuilder, HttpClient gatewayHttpClient,
            GatewayControlPlaneProperties controlPlaneProperties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(gatewayHttpClient);
        requestFactory.setReadTimeout(controlPlaneProperties.requestTimeout());
        RestClient restClient = restClientBuilder.baseUrl(controlPlaneProperties.baseUri().toString())
                .requestFactory(requestFactory)
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    // 由控制面适配器同时判断 HTTP 状态和统一响应码。
                })
                .build();
        HttpServiceProxyFactory proxyFactory =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
        return proxyFactory.createClient(ControlPlaneHttpApi.class);
    }

    /**
     * 创建控制面应用端口的 HTTP 实现。
     *
     * @param controlPlaneHttpApi Spring 生成的控制面 HTTP 代理
     * @return 控制面应用端口
     */
    @Bean
    public GatewayControlPlaneClient gatewayControlPlaneClient(ControlPlaneHttpApi controlPlaneHttpApi) {
        return new HttpGatewayControlPlaneClient(controlPlaneHttpApi);
    }

    /**
     * 创建控制面配置同步应用服务。
     *
     * @param controlPlaneClient   控制面出站端口
     * @param routeMapper          配置契约到领域路由的转换器
     * @param invocationDispatcher 协议调用与资源准备分派器
     * @param routeTable           当前运行时路由表
     * @return 配置同步应用服务
     */
    @Bean
    public GatewayConfigurationSynchronizer gatewayConfigurationSynchronizer(
            GatewayControlPlaneClient controlPlaneClient, GatewayRouteConfigurationMapper routeMapper,
            ProtocolInvocationDispatcher invocationDispatcher, GatewayRouteTable routeTable) {
        return new GatewayConfigurationSynchronizer(controlPlaneClient, routeMapper, invocationDispatcher, routeTable);
    }

    /**
     * 创建网关节点租约管理器。
     *
     * @param controlPlaneClient 控制面出站端口
     * @param routeTable         当前运行时路由表
     * @param taskScheduler      控制面统一任务调度器
     * @return 网关节点租约管理器
     */
    @Bean
    public GatewayNodeLeaseManager gatewayNodeLeaseManager(
            GatewayControlPlaneClient controlPlaneClient, GatewayRouteTable routeTable,
            @Qualifier("gatewayControlPlaneTaskScheduler") TaskScheduler taskScheduler) {
        return new GatewayNodeLeaseManager(controlPlaneClient, routeTable, taskScheduler);
    }

    /**
     * 创建随 Spring 容器启动和停止的网关节点生命周期组件。
     *
     * @param leaseManager              节点租赁管理器
     * @param configurationSynchronizer 配置同步应用服务
     * @param controlPlaneProperties    控制面节点身份配置
     * @return 网关节点生命周期组件
     */
    @Bean
    public GatewayNodeLifecycle gatewayNodeLifecycle(
            GatewayNodeLeaseManager leaseManager,
            GatewayConfigurationSynchronizer configurationSynchronizer,
            GatewayControlPlaneProperties controlPlaneProperties) {
        return new GatewayNodeLifecycle(leaseManager, configurationSynchronizer, controlPlaneProperties);
    }

    /**
     * 创建控制面周期任务使用的统一调度器。
     *
     * <p>节点续租与配置刷新分别占用调度线程，避免配置拉取阻塞心跳续租。
     * 周期任务异常由 Spring 官方错误处理器记录并抑制，不会终止后续调度。</p>
     *
     * @return 控制面任务调度器
     */
    @Bean(name = "gatewayControlPlaneTaskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler gatewayControlPlaneTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("gateweave-control-plane-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        // scheduler.setDaemon(true);
        scheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        return scheduler;
    }
}
