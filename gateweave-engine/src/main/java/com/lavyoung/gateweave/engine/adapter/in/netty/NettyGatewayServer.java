package com.lavyoung.gateweave.engine.adapter.in.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import org.springframework.context.SmartLifecycle;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;

/**
 * 管理网关 Netty 监听 Channel 的启动和关闭。
 *
 * <p>服务器作为 Spring 生命周期组件，在节点注册和初始配置同步完成后开始监听。
 * 该类不创建或销毁 EventLoopGroup，线程资源仍由装配层统一管理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class NettyGatewayServer implements SmartLifecycle, AutoCloseable {

    private final ServerBootstrap serverBootstrap;
    private final InetSocketAddress bindAddress;
    private Channel serverChannel;

    /**
     * 创建 Netty 网关服务器。
     *
     * @param serverBootstrap 已完成传输层配置的 Netty ServerBootstrap
     * @param bindAddress     服务器绑定地址
     * @throws NullPointerException 任一参数为 null 时抛出
     */
    public NettyGatewayServer(ServerBootstrap serverBootstrap, InetSocketAddress bindAddress) {
        this.serverBootstrap = Objects.requireNonNull(serverBootstrap, "serverBootstrap must not be null").clone();
        this.bindAddress = Objects.requireNonNull(bindAddress, "bindAddress must not be null");
    }


    /**
     * 绑定监听地址并启动服务器。
     *
     * <p>重复调用时，如果监听 Channel 仍然有效，则直接返回。</p>
     */
    @Override
    public synchronized void start() {
        if (this.serverChannel != null && this.serverChannel.isActive()) {
            return;
        }
        this.serverChannel = this.serverBootstrap.bind(this.bindAddress).syncUninterruptibly().channel();
    }

    /**
     * 最终启动顺序将由 Spring 阶段控制：
     * GatewayNodeLifecycle（Integer.MIN_VALUE）
     * → 节点注册
     * → 初始配置同步
     * → NettyGatewayServer（0）
     * → 开始监听端口
     */
    @Override
    public synchronized void stop() {
        this.close();
    }

    /**
     * 判断服务器监听 Channel 是否处于活动状态。
     *
     * @return 服务器运行时返回 true，否则返回 false
     */
    @Override
    public synchronized boolean isRunning() {
        return this.serverChannel != null && this.serverChannel.isActive();
    }

    /**
     * 让监听端口在节点注册和初始配置同步完成后启动。
     *
     * @return 普通运行时生命周期阶段
     */
    @Override
    public int getPhase() {
        return 0;
    }

    /**
     * 获取服务器实际绑定的地址。
     *
     * @return 已启动时返回实际地址，否则返回空
     */
    public synchronized Optional<SocketAddress> localAddress() {
        return this.serverChannel == null ? Optional.empty() : Optional.ofNullable(this.serverChannel.localAddress());
    }

    /**
     * 关闭服务器监听 Channel。
     *
     * <p>该操作是幂等的，不负责关闭外部注入的 EventLoopGroup。</p>
     */
    @Override
    public synchronized void close() {
        if (this.serverChannel == null) {
            return;
        }
        this.serverChannel.close().syncUninterruptibly();
        this.serverChannel = null;
    }
}
