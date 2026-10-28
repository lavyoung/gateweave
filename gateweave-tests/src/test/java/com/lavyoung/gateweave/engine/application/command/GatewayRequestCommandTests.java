package com.lavyoung.gateweave.engine.application.command;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRequestCommandTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);

    @Test
    void shouldCreateImmutableRequestSnapshot() {
        // Given
        var contentTypes = new ArrayList<>(List.of("application/json"));
        var sourceHeaders = new HashMap<String, List<String>>();
        sourceHeaders.put("Content-Type", contentTypes);
        var headers = HttpHeaders.of(sourceHeaders, (name, value) -> true);
        var queryParameters = new HttpParameterValues(Map.of("id", List.of("1")));
        var formParameters = new HttpParameterValues(Map.of("role", List.of("admin", "developer")));
        var body = "hello".getBytes(StandardCharsets.UTF_8);

        // When
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/queryUserInfo?id=1"), headers,
                queryParameters, formParameters, body);
        contentTypes.add("text/plain");
        sourceHeaders.put("X-Changed", List.of("true"));
        body[0] = 'X';

        // Then
        assertThat(command.headers().map()).containsExactlyEntriesOf(
                Map.of("Content-Type", List.of("application/json")));
        assertThat(command.headers().firstValue("content-type")).contains("application/json");
        assertThat(command.queryParameters()).isSameAs(queryParameters);
        assertThat(command.formParameters()).isSameAs(formParameters);
        assertThat(command.body()).containsExactly("hello".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> command.headers().map().put("X-Test", List.of("value")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnDefensiveBodyCopy() {
        // Given
        var command = validCommand();

        // When
        var returnedBody = command.body();
        returnedBody[0] = 'X';

        // Then
        assertThat(command.body()).containsExactly("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(command.formParameters().values()).isEmpty();
    }

    @Test
    void shouldRejectBlankRequestId() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRequestCommand(" ", "GET", URI.create("/queryUserInfo"),
                EMPTY_HEADERS, new HttpParameterValues(Map.of()), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestId must not be blank");
    }

    @Test
    void shouldRejectBlankMethod() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRequestCommand("request-1", " ", URI.create("/queryUserInfo"),
                EMPTY_HEADERS, new HttpParameterValues(Map.of()), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method must not be blank");
    }

    @Test
    void shouldExplainMissingHeaders() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRequestCommand("request-1", "GET", URI.create("/queryUserInfo"), null,
                new HttpParameterValues(Map.of()), new byte[0]))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("headers must not be null");
    }

    @Test
    void shouldExplainMissingQueryParameters() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRequestCommand("request-1", "GET", URI.create("/queryUserInfo"),
                EMPTY_HEADERS, null, new byte[0]))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("queryParameters must not be null");
    }

    @Test
    void shouldExplainMissingFormParameters() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRequestCommand("request-1", "POST", URI.create("/queryUserInfo"),
                EMPTY_HEADERS, new HttpParameterValues(Map.of()), null, new byte[0]))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("formParameters must not be null");
    }

    private static GatewayRequestCommand validCommand() {
        return new GatewayRequestCommand("request-1", "POST", URI.create("/queryUserInfo"), EMPTY_HEADERS,
                new HttpParameterValues(Map.of()), "hello".getBytes(StandardCharsets.UTF_8));
    }
}
