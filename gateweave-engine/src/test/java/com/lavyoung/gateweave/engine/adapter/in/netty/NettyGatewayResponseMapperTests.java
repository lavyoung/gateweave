package com.lavyoung.gateweave.engine.adapter.in.netty;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.result.GatewayResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyGatewayResponseMapperTests {

    @Test
    void shouldMapApplicationResponseAndCorrectTransportHeaders() {
        // Given
        var mapper = new NettyGatewayResponseMapper();
        var body = "创建成功".getBytes(StandardCharsets.UTF_8);
        var headers = HttpHeaders.of(
                Map.of(
                        "Set-Cookie", List.of("session=one", "theme=dark"),
                        "Content-Length", List.of("999"),
                        "Transfer-Encoding", List.of("chunked")),
                (name, value) -> true);
        var applicationResponse = new GatewayResponse(201, headers, body);

        // When
        FullHttpResponse nettyResponse = mapper.map(HttpVersion.HTTP_1_0, applicationResponse);

        // Then
        try {
            assertThat(nettyResponse.protocolVersion()).isEqualTo(HttpVersion.HTTP_1_0);
            assertThat(nettyResponse.status().code()).isEqualTo(201);
            assertThat(nettyResponse.headers().getAll("Set-Cookie"))
                    .containsExactly("session=one", "theme=dark");
            assertThat(HttpUtil.getContentLength(nettyResponse)).isEqualTo(body.length);
            assertThat(HttpUtil.isTransferEncodingChunked(nettyResponse)).isFalse();
            assertThat(nettyResponse.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
            assertThat(nettyResponse.content().toString(StandardCharsets.UTF_8)).isEqualTo("创建成功");
        } finally {
            ReferenceCountUtil.release(nettyResponse);
        }
    }

    @Test
    void shouldRejectMissingProtocolVersionOrResponse() {
        // Given
        var mapper = new NettyGatewayResponseMapper();
        var response = GatewayResponse.text(200, "ok");

        // When & Then
        assertThatThrownBy(() -> mapper.map(null, response))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("protocolVersion must not be null");

        assertThatThrownBy(() -> mapper.map(HttpVersion.HTTP_1_1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("response must not be null");
    }
}
