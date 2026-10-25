package com.lavyoung.gateweave.engine.adapter.in.netty;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyGatewayRequestHandlerTest {

    private static final NettyGatewayRequestMapper REQUEST_MAPPER = new NettyGatewayRequestMapper();
    private static final NettyGatewayResponseMapper RESPONSE_MAPPER = new NettyGatewayResponseMapper();

    @Test
    void shouldHandleRequestAndKeepConnectionAlive() {
        // Given
        var capturedCommand = new AtomicReference<GatewayRequestCommand>();
        HandleGatewayRequestUseCase useCase = command -> {
            capturedCommand.set(command);
            return CompletableFuture.completedFuture(GatewayResponse.text(200, "gateway-ok"));
        };
        var channel = new EmbeddedChannel(new NettyGatewayRequestHandler(useCase, REQUEST_MAPPER, RESPONSE_MAPPER));
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/orders?id=1",
                Unpooled.copiedBuffer("payload", StandardCharsets.UTF_8));
        request.headers().set("X-Request-Id", "request-1");

        // When
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Then
        try {
            assertThat(capturedCommand.get().requestId()).isEqualTo("request-1");
            assertThat(capturedCommand.get().method()).isEqualTo("POST");
            assertThat(capturedCommand.get().uri().toString()).isEqualTo("/orders?id=1");
            assertThat(new String(capturedCommand.get().body(), StandardCharsets.UTF_8)).isEqualTo("payload");
            assertThat(request.refCnt()).isZero();
            assertThat(response.status().code()).isEqualTo(200);
            assertThat(response.content().toString(StandardCharsets.UTF_8)).isEqualTo("gateway-ok");
            assertThat(HttpUtil.isKeepAlive(response)).isTrue();
            assertThat(channel.isActive()).isTrue();
        } finally {
            ReferenceCountUtil.release(response);
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldReturnInternalServerErrorWhenApplicationStageFails() {
        // Given
        var responseStage = new CompletableFuture<GatewayResponse>();
        HandleGatewayRequestUseCase useCase = command -> responseStage;
        var channel = new EmbeddedChannel(new NettyGatewayRequestHandler(useCase, REQUEST_MAPPER, RESPONSE_MAPPER));
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/orders");

        // When
        channel.writeInbound(request);
        responseStage.completeExceptionally(new IllegalStateException("database address must stay private"));
        channel.runPendingTasks();
        FullHttpResponse response = channel.readOutbound();

        // Then
        try {
            assertThat(request.refCnt()).isZero();
            assertThat(response.status().code()).isEqualTo(500);
            assertThat(response.content().toString(StandardCharsets.UTF_8)).isEqualTo("Internal Server Error");
            assertThat(response.content().toString(StandardCharsets.UTF_8)).doesNotContain("database address");
            assertThat(channel.isActive()).isTrue();
        } finally {
            ReferenceCountUtil.release(response);
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldReturnBadRequestAndCloseConnectionWhenUriIsInvalid() {
        // Given
        HandleGatewayRequestUseCase useCase = command ->
                CompletableFuture.completedFuture(GatewayResponse.text(200, "must-not-be-called"));
        var channel = new EmbeddedChannel(new NettyGatewayRequestHandler(useCase, REQUEST_MAPPER, RESPONSE_MAPPER));
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/invalid path");

        // When
        channel.writeInbound(request);
        channel.runPendingTasks();
        FullHttpResponse response = channel.readOutbound();

        // Then
        try {
            assertThat(request.refCnt()).isZero();
            assertThat(response.status().code()).isEqualTo(400);
            assertThat(response.content().toString(StandardCharsets.UTF_8)).isEqualTo("Bad Request");
            assertThat(HttpUtil.isKeepAlive(response)).isFalse();
            assertThat(channel.isActive()).isFalse();
        } finally {
            ReferenceCountUtil.release(response);
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldCloseConnectionAfterWritingResponseWhenKeepAliveIsDisabled() {
        // Given
        var applicationResponse = GatewayResponse.text(200, "ok");
        HandleGatewayRequestUseCase useCase = command -> CompletableFuture.completedFuture(applicationResponse);
        var channel = new EmbeddedChannel(new NettyGatewayRequestHandler(useCase, REQUEST_MAPPER, RESPONSE_MAPPER));
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/health");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        // When
        channel.writeInbound(request);
        channel.runPendingTasks();
        FullHttpResponse response = channel.readOutbound();

        // Then
        try {
            assertThat(response.status().code()).isEqualTo(200);
            assertThat(HttpUtil.isKeepAlive(response)).isFalse();
            assertThat(channel.isActive()).isFalse();
        } finally {
            ReferenceCountUtil.release(response);
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        var applicationResponse = GatewayResponse.text(200, "ok");
        HandleGatewayRequestUseCase useCase = command -> CompletableFuture.completedFuture(applicationResponse);

        // When & Then
        assertThatThrownBy(() -> new NettyGatewayRequestHandler(null, REQUEST_MAPPER, RESPONSE_MAPPER))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("useCase must not be null");
        assertThatThrownBy(() -> new NettyGatewayRequestHandler(useCase, null, RESPONSE_MAPPER))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestMapper must not be null");
        assertThatThrownBy(() -> new NettyGatewayRequestHandler(useCase, REQUEST_MAPPER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("responseMapper must not be null");
    }
}
