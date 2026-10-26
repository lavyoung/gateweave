package com.lavyoung.gateweave.engine.application.service;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGatewayRequestServiceTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);

    @Test
    void shouldHandleGatewayRequest() {
        // Given
        var service = new DefaultGatewayRequestService();
        var command = new GatewayRequestCommand(
                "request-1", "GET", URI.create("/queryUserInfo?id=1"), EMPTY_HEADERS, new byte[0]);

        // When
        var response = service.handle(command).toCompletableFuture().join();

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type"))
                .contains("text/plain; charset=UTF-8");
        assertThat(new String(response.body(), StandardCharsets.UTF_8))
                .isEqualTo("GateWeave received request: GET /queryUserInfo?id=1");
    }

    @Test
    void shouldRejectMissingCommand() {
        // Given
        var service = new DefaultGatewayRequestService();

        // When & Then
        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
    }
}
