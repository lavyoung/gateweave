package com.lavyoung.gateweave.engine.application.result;

import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayResponseTest {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);

    @Test
    void shouldCreateUtf8TextResponse() {
        // When
        var response = GatewayResponse.text(200, "网关响应成功");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type"))
                .contains("text/plain; charset=UTF-8");
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("网关响应成功");
    }

    @Test
    void shouldReuseImmutableMultiValueHeaders() {
        // Given
        var headers = HttpHeaders.of(
                Map.of("Set-Cookie", List.of("a=1", "b=2")),
                (name, value) -> true);

        // When
        var response = new GatewayResponse(200, headers, new byte[0]);

        // Then
        assertThat(response.headers().allValues("set-cookie")).containsExactly("a=1", "b=2");
        assertThat(response.headers()).isSameAs(headers);
    }

    @Test
    void shouldReturnDefensiveBodyCopy() {
        // Given
        var sourceBody = "hello".getBytes(StandardCharsets.UTF_8);
        var response = new GatewayResponse(200, EMPTY_HEADERS, sourceBody);
        sourceBody[0] = 'X';

        // When
        var returnedBody = response.body();
        returnedBody[0] = 'Y';

        // Then
        assertThat(response.body()).containsExactly("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldRejectInvalidStatusCode() {
        // When & Then
        assertThatThrownBy(() -> new GatewayResponse(99, EMPTY_HEADERS, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("statusCode must be between 100 and 599");

        assertThatThrownBy(() -> new GatewayResponse(600, EMPTY_HEADERS, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("statusCode must be between 100 and 599");
    }

    @Test
    void shouldRejectMissingHeadersAndBody() {
        // When & Then
        assertThatThrownBy(() -> new GatewayResponse(200, null, new byte[0]))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("headers must not be null");

        assertThatThrownBy(() -> new GatewayResponse(200, EMPTY_HEADERS, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("body must not be null");
    }
}
