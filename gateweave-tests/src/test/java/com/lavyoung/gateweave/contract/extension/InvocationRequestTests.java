package com.lavyoung.gateweave.contract.extension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationRequestTests {

    @Test
    void shouldNormalizeProtocolAndCreateImmutableSnapshot() {
        // Given
        var parameterTypes = new ArrayList<>(List.of("java.lang.String"));
        var headers = new HashMap<>(Map.of("trace-id", "trace-1"));
        var attributes = new HashMap<>(Map.of("version", "1.0.0"));
        byte[] payload = {1, 2, 3};

        // When
        var request = new InvocationRequest("request-1", "route-say-hi", "DUBBO", "cn.example.ActivityBooth",
                "sayHi", parameterTypes, Duration.ofSeconds(3), headers, attributes, payload);
        parameterTypes.add("java.lang.Long");
        headers.put("changed", "true");
        attributes.put("group", "test");
        payload[0] = 9;

        // Then
        assertThat(request.protocol()).isEqualTo("dubbo");
        assertThat(request.parameterTypes()).containsExactly("java.lang.String");
        assertThat(request.headers()).containsExactlyEntriesOf(Map.of("trace-id", "trace-1"));
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThat(request.payload()).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> request.parameterTypes().add("java.lang.Long"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.headers().put("changed", "true"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.attributes().put("group", "test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnPayloadCopy() {
        // Given
        var request = validRequest();

        // When
        byte[] returnedPayload = request.payload();
        returnedPayload[0] = 9;

        // Then
        assertThat(request.payload()).containsExactly(1, 2, 3);
    }

    @Test
    void shouldRejectBlankRequiredText() {
        // When & Then
        assertThatThrownBy(() -> requestWithText("", "route-1", "dubbo", "cn.example.Service", "invoke"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestId must not be blank");
        assertThatThrownBy(() -> requestWithText("request-1", " ", "dubbo", "cn.example.Service", "invoke"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("routeId must not be blank");
        assertThatThrownBy(() -> requestWithText("request-1", "route-1", " ", "cn.example.Service", "invoke"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
        assertThatThrownBy(() -> requestWithText("request-1", "route-1", "dubbo", " ", "invoke"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("service must not be blank");
        assertThatThrownBy(() -> requestWithText("request-1", "route-1", "dubbo", "cn.example.Service", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operation must not be blank");
    }

    @Test
    void shouldRejectInvalidParameterTypeAndTimeout() {
        // Given
        var parameterTypes = new ArrayList<>(List.of("java.lang.String"));
        parameterTypes.add(" ");

        // When & Then
        assertThatThrownBy(() -> new InvocationRequest("request-1", "route-1", "dubbo", "cn.example.Service",
                "invoke", parameterTypes, Duration.ofSeconds(3), Map.of(), Map.of(), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes must not contain blank values");
        assertThatThrownBy(() -> new InvocationRequest("request-1", "route-1", "dubbo", "cn.example.Service",
                "invoke", List.of(), Duration.ZERO, Map.of(), Map.of(), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
        assertThatThrownBy(() -> new InvocationRequest("request-1", "route-1", "dubbo", "cn.example.Service",
                "invoke", List.of(), Duration.ofSeconds(-1), Map.of(), Map.of(), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
    }

    @Test
    void shouldRejectNullPayload() {
        // When & Then
        assertThatThrownBy(() -> new InvocationRequest("request-1", "route-1", "dubbo", "cn.example.Service",
                "invoke", List.of(), Duration.ofSeconds(3), Map.of(), Map.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }

    private static InvocationRequest validRequest() {
        return new InvocationRequest("request-1", "route-1", "dubbo", "cn.example.Service", "invoke",
                List.of("java.lang.String"), Duration.ofSeconds(3), Map.of(), Map.of(), new byte[]{1, 2, 3});
    }

    private static InvocationRequest requestWithText(String requestId, String routeId, String protocol,
                                                     String service, String operation) {
        return new InvocationRequest(requestId, routeId, protocol, service, operation, List.of(),
                Duration.ofSeconds(3), Map.of(), Map.of(), new byte[0]);
    }
}
