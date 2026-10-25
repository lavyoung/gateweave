package com.lavyoung.gateweave.engine.adapter.in.netty;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayChannelInitializerTest {

    @Test
    void shouldConfigureHttpPipelineInOrder() {
        // Given
        var requestHandler = new ChannelInboundHandlerAdapter();
        var initializer = new GatewayChannelInitializer(1024, () -> requestHandler);

        // When
        var channel = new EmbeddedChannel();
        try {
            initializer.initChannel(channel);

            // Then
            assertThat(channel.pipeline().names())
                    .containsSubsequence("httpCodec", "httpAggregator", "gatewayRequestHandler");
            assertThat(channel.pipeline().get("httpCodec")).isInstanceOf(HttpServerCodec.class);
            assertThat(channel.pipeline().get("httpAggregator")).isInstanceOf(HttpObjectAggregator.class);
            assertThat(channel.pipeline().get("gatewayRequestHandler")).isSameAs(requestHandler);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldCreateIndependentRequestHandlerForEachChannel() {
        // Given
        var createdHandlers = new ArrayList<ChannelInboundHandler>();
        var initializer = new GatewayChannelInitializer(1024, () -> {
            var handler = new ChannelInboundHandlerAdapter();
            createdHandlers.add(handler);
            return handler;
        });

        // When
        var firstChannel = new EmbeddedChannel();
        var secondChannel = new EmbeddedChannel();
        try {
            initializer.initChannel(firstChannel);
            initializer.initChannel(secondChannel);

            // Then
            assertThat(createdHandlers).hasSize(2);
            assertThat(createdHandlers.get(0)).isNotSameAs(createdHandlers.get(1));
            assertThat(firstChannel.pipeline().get("gatewayRequestHandler"))
                    .isSameAs(createdHandlers.get(0));
            assertThat(secondChannel.pipeline().get("gatewayRequestHandler"))
                    .isSameAs(createdHandlers.get(1));
        } finally {
            firstChannel.finishAndReleaseAll();
            secondChannel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        // When & Then
        assertThatThrownBy(() -> new GatewayChannelInitializer(
                0, ChannelInboundHandlerAdapter::new))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxContentLength must be greater than zero");

        assertThatThrownBy(() -> new GatewayChannelInitializer(1024, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestHandlerFactory must not be null");
    }

    @Test
    void shouldRejectFactoryReturningNull() {
        // Given
        var initializer = new GatewayChannelInitializer(1024, () -> null);

        // When & Then
        var channel = new EmbeddedChannel();
        try {
            assertThatThrownBy(() -> initializer.initChannel(channel))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("requestHandlerFactory must not return null");
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
