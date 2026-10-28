package com.lavyoung.gateweave.engine.adapter.in.netty;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
        assertThat(command.queryParameters().values()).containsExactlyEntriesOf(Map.of("id", List.of("1")));
        assertThat(new String(command.body(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void shouldDecodeUtf8RepeatedAndEmptyQueryParameters() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var uri = "/search?keyword=%E7%BD%91%E5%85%B3&tag=java&tag=netty&empty=";
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri, Unpooled.buffer(0));

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.uri().toString()).isEqualTo(uri);
        assertThat(command.queryParameters().values())
                .containsEntry("keyword", List.of("网关"))
                .containsEntry("tag", List.of("java", "netty"))
                .containsEntry("empty", List.of(""));
    }

    @Test
    void shouldDecodeUrlEncodedFormWithoutMergingQueryParameters() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/roles?role=query",
                Unpooled.copiedBuffer("role=admin&role=developer&empty=", StandardCharsets.UTF_8));
        request.headers().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.queryParameters().values()).containsEntry("role", List.of("query"));
        assertThat(command.formParameters().values())
                .containsEntry("role", List.of("admin", "developer"))
                .containsEntry("empty", List.of(""));
    }

    @Test
    void shouldDecodeMultipartFormFields() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var boundary = "GateWeaveBoundary";
        var body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n\r\n"
                + "lavyoung\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n\r\n"
                + "netty\r\n"
                + "--" + boundary + "--\r\n";
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/users",
                Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
        request.headers().set("Content-Type", "multipart/form-data; boundary=" + boundary);

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.formParameters().values())
                .containsEntry("name", List.of("lavyoung"))
                .containsEntry("tag", List.of("netty"));
    }

    @Test
    void shouldRejectMultipartFileUploadUntilFileParametersAreSupported() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var boundary = "GateWeaveFileBoundary";
        var body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"gateway.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "GateWeave\r\n"
                + "--" + boundary + "--\r\n";
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/upload",
                Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
        request.headers().set("Content-Type", "multipart/form-data; boundary=" + boundary);

        // When & Then
        try {
            assertThatThrownBy(() -> mapper.map(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("file upload form fields are not supported");
        } finally {
            ReferenceCountUtil.release(request);
        }
    }

    @Test
    void shouldKeepFormParametersEmptyForJsonRequest() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/users",
                Unpooled.copiedBuffer("{\"name\":\"lavyoung\"}", StandardCharsets.UTF_8));
        request.headers().set("Content-Type", "application/json");

        // When
        GatewayRequestCommand command;
        try {
            command = mapper.map(request);
        } finally {
            ReferenceCountUtil.release(request);
        }

        // Then
        assertThat(command.formParameters().values()).isEmpty();
        assertThat(new String(command.body(), StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"lavyoung\"}");
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
        assertThat(command.queryParameters().values()).isEmpty();
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

    @Test
    void shouldRejectInvalidQueryEncoding() {
        // Given
        var mapper = new NettyGatewayRequestMapper();
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/search?name=%ZZ",
                Unpooled.buffer(0));

        // When & Then
        try {
            assertThatThrownBy(() -> mapper.map(request)).isInstanceOf(IllegalArgumentException.class);
        } finally {
            ReferenceCountUtil.release(request);
        }
    }
}
