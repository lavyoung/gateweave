package com.lavyoung.gateweave.engine.configuration;

import com.lavyoung.gateweave.engine.adapter.in.netty.*;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.service.DefaultGatewayRequestService;
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

import java.net.InetSocketAddress;

/**
 * 装配网关数据面的应用服务、Netty 适配器及运行时资源。
 *
 * <p>该配置类是最终装配边界。应用层和 Netty 适配器保持无 Spring 依赖，
 * EventLoopGroup 的创建与销毁统一交由 Spring 容器管理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayServerProperties.class)
public class GatewayEngineConfiguration {

    private static final int CONNECTION_BACKLOG = 1024;

    /**
     * 创建默认网关请求用例。
     *
     * @return 网关请求用例
     */
    @Bean
    public HandleGatewayRequestUseCase gatewayRequestUseCase() {
        return new DefaultGatewayRequestService();
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
        var threadFactory = new DefaultThreadFactory("gateweave-netty-worker");
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
    public GatewayChannelInitializer gatewayChannelInitializer(GatewayServerProperties properties,
                                                               HandleGatewayRequestUseCase useCase,
                                                               NettyGatewayRequestMapper requestMapper,
                                                               NettyGatewayResponseMapper responseMapper) {
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
                                                  @Qualifier("gatewayWorkerGroup") EventLoopGroup workerGroup,
                                                  GatewayChannelInitializer initializer) {
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
