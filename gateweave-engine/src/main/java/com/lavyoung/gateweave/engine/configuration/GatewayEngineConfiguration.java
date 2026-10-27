package com.lavyoung.gateweave.engine.configuration;

import java.net.InetSocketAddress;

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

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import com.lavyoung.gateweave.engine.adapter.in.configuration.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.adapter.in.netty.GatewayChannelInitializer;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayRequestHandler;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayRequestMapper;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayResponseMapper;
import com.lavyoung.gateweave.engine.adapter.in.netty.NettyGatewayServer;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.service.DefaultGatewayRequestService;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;
import com.lavyoung.gateweave.plugin.invoker.rpc.DubboReferenceOptions;
import com.lavyoung.gateweave.plugin.invoker.rpc.GenericReferenceRegistry;

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
@EnableConfigurationProperties(
        {GatewayServerProperties.class, GatewayRouteProperties.class, GatewayRpcProperties.class})
public class GatewayEngineConfiguration {

    private static final int CONNECTION_BACKLOG = 1024;

    /**
     * 创建 Dubbo 泛化调用注册表，并注册当前路由快照中的所有 Dubbo 调用目标。
     *
     * <p>注册失败时立即关闭已经创建的 Dubbo 引用，避免 Spring Bean 创建失败后遗留连接资源。</p>
     *
     * @param rpcProperties   RPC 运行配置
     * @param routeProperties 当前路由配置快照
     * @return 完成初始化的 Dubbo 泛化调用注册表
     * @throws RuntimeException Dubbo 服务引用创建失败时抛出
     */
    @Bean(destroyMethod = "close")
    public GenericReferenceRegistry genericReferenceRegistry(GatewayRpcProperties rpcProperties,
            GatewayRouteProperties routeProperties) {
        GenericReferenceRegistry referenceRegistry = new GenericReferenceRegistry(
                rpcProperties.applicationName(), rpcProperties.registryAddress(), rpcProperties.discoveryMode());
        try {
            for (RouteConfiguration route : routeProperties.routes()) {
                InvocationTargetConfiguration targetConfiguration = route.target();
                if (!"dubbo".equalsIgnoreCase(targetConfiguration.protocol())) {
                    continue;
                }
                DubboReferenceOptions options = DubboReferenceOptions.from(targetConfiguration.attributes());
                referenceRegistry.addGenericReference(
                        targetConfiguration.service(), targetConfiguration.operation(), options.version());
            }
            return referenceRegistry;
        } catch (RuntimeException exception) {
            referenceRegistry.close();
            throw exception;
        }
    }

    /**
     * 创建负责路由匹配和协议调用编排的网关请求用例。
     *
     * @param routeTable               当前生效的领域路由表
     * @param genericReferenceRegistry Dubbo 泛化调用代理注册表
     * @return 网关请求用例
     */
    @Bean
    public HandleGatewayRequestUseCase gatewayRequestUseCase(
            GatewayRouteTable routeTable, GenericReferenceRegistry genericReferenceRegistry) {
        return new DefaultGatewayRequestService(routeTable, target -> {
            DubboReferenceOptions options = DubboReferenceOptions.from(target.attributes());
            return genericReferenceRegistry.getGenericReference(
                    target.service(), target.operation(), options.version());
        });
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
     * @param properties 绑定完成的路由配置
     * @param mapper     路由契约防腐转换器
     * @return 当前生效的领域路由表
     * @throws NullPointerException     路由配置包含 null 时抛出
     * @throws IllegalArgumentException 路由重复或违反领域不变量时抛出
     */
    @Bean
    public GatewayRouteTable gatewayRouteTable(
            GatewayRouteProperties properties, GatewayRouteConfigurationMapper mapper) {
        return new GatewayRouteTable(properties.routes().stream().map(mapper::toDomain).toList());
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
     * @return 配置完成的 ServerBootstrap
     */
    @Bean
    public ServerBootstrap gatewayServerBootstrap(@Qualifier("gatewayBossGroup") EventLoopGroup bossGroup,
            @Qualifier("gatewayWorkerGroup") EventLoopGroup workerGroup, GatewayChannelInitializer initializer) {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, CONNECTION_BACKLOG)
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
    @Bean(initMethod = "start", destroyMethod = "close")
    public NettyGatewayServer nettyGatewayServer(ServerBootstrap bootstrap, GatewayServerProperties properties) {
        InetSocketAddress address = new InetSocketAddress(properties.host(), properties.port());
        return new NettyGatewayServer(bootstrap, address);
    }
}
