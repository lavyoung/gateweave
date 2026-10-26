package com.lavyoung.gateweave.engine.adapter.in.netty;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyGatewayRequestMapperTests {

    @Test
    void shouldMapFullHttpRequestToApplicationCommand() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/orders?id=1",
                Unpooled.copiedBuffer("hello", StandardCharsets.UTF_8));
        request.headers().set("X-Request-Id", "request-1");
        request.headers().add("X-Tag", "first");
        request.headers().add("X-Tag", "second");

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.requestId()).isEqualTo("request-1");
        assertThat(command.method()).isEqualTo("POST");
        assertThat(command.uri().toString()).isEqualTo("/orders?id=1");
        assertThat(command.headers().allValues("x-tag")).containsExactly("first", "second");
        assertThat(new String(command.body(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/health", Unpooled.buffer(0));

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.requestId()).isNotBlank();
        assertThatCode(() -> UUID.fromString(command.requestId()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingRequest() {
        // Given
        var mapper = new NettyGatewayRequestMapper();

        // When & Then
        assertThatThrownBy(() -> mapper.map(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    @Test
    void shouldRejectInvalidRequestUri() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/invalid path",
                Unpooled.buffer(0));

        // When & Then
        try {
            assertThatThrownBy(() -> mapper.map(request))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            ReferenceCountUtil.release(request);
        }
    }
}
