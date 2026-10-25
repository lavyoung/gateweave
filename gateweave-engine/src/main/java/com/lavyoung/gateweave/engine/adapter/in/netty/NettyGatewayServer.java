package com.lavyoung.gateweave.engine.adapter.in.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;

/**
 * 管理网关 Netty 监听 Channel 的启动和关闭。
 *
 * <p>该类不创建或销毁 EventLoopGroup。线程资源由外部装配层统一管理，
 * 服务器只负责绑定地址、保存监听 Channel 以及执行幂等关闭。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public final class NettyGatewayServer implements AutoCloseable {

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
        this.serverBootstrap = Objects.requireNonNull(serverBootstrap, "serverBootstrap must not be null")
                .clone();
        this.bindAddress = Objects.requireNonNull(bindAddress, "bindAddress must not be null");
    }

    /**
     * 绑定监听地址并启动服务器。
     *
     * <p>重复调用时，如果监听 Channel 仍然有效，则直接返回。</p>
     */
    public synchronized void start() {
        if (serverChannel != null && serverChannel.isActive()) {
            return;
        }
        serverChannel = serverBootstrap.bind(bindAddress).syncUninterruptibly().channel();
    }

    /**
     * 判断服务器监听 Channel 是否处于活动状态。
     *
     * @return 服务器运行时返回 true，否则返回 false
     */
    public synchronized boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }

    /**
     * 获取服务器实际绑定的地址。
     *
     * @return 已启动时返回实际地址，否则返回空
     */
    public synchronized Optional<SocketAddress> localAddress() {
        return serverChannel == null ? Optional.empty() : Optional.ofNullable(serverChannel.localAddress());
    }

    /**
     * 关闭服务器监听 Channel。
     *
     * <p>该操作是幂等的，不负责关闭外部注入的 EventLoopGroup。</p>
     */
    @Override
    public synchronized void close() throws Exception {
        if (serverChannel == null) {
            return;
        }
        serverChannel.close().syncUninterruptibly();
        serverChannel = null;
    }
}
