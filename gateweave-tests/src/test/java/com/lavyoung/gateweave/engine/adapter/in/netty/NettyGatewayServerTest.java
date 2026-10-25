package com.lavyoung.gateweave.engine.adapter.in.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyGatewayServerTest {

    @Test
    void shouldBindRandomPortStartOnlyOnceAndCloseIdempotently() throws Exception {
        // Given
        var bossGroup = new NioEventLoopGroup(1);
        var workerGroup = new NioEventLoopGroup(1);
        var initializer = new GatewayChannelInitializer(1024, ChannelInboundHandlerAdapter::new);
        var bootstrap = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(initializer);
        var bindAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        var server = new NettyGatewayServer(bootstrap, bindAddress);

        try {
            assertThat(server.isRunning()).isFalse();
            assertThat(server.localAddress()).isEmpty();

            // When
            server.start();
            var firstAddress = server.localAddress();
            server.start();

            // Then
            assertThat(server.isRunning()).isTrue();
            assertThat(server.localAddress()).isEqualTo(firstAddress);
            assertThat(firstAddress).isPresent().get().isInstanceOf(InetSocketAddress.class);
            assertThat(((InetSocketAddress) firstAddress.orElseThrow()).getPort()).isPositive();

            // When
            server.close();

            // Then
            assertThat(server.isRunning()).isFalse();
            assertThat(server.localAddress()).isEmpty();
            assertThatCode(server::close).doesNotThrowAnyException();
        } finally {
            server.close();
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Test
    void shouldRejectMissingBootstrapOrBindAddress() {
        // Given
        var bootstrap = new ServerBootstrap();
        var bindAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 7397);

        // When & Then
        assertThatThrownBy(() -> new NettyGatewayServer(null, bindAddress))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serverBootstrap must not be null");
        assertThatThrownBy(() -> new NettyGatewayServer(bootstrap, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("bindAddress must not be null");
    }
}
