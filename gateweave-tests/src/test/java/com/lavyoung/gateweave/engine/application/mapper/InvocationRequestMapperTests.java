package com.lavyoung.gateweave.engine.application.mapper;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationRequestMapperTests {

    @Test
    void shouldMapSessionToInvocationRequest() {
        // Given
        HttpHeaders headers = HttpHeaders.of(
                Map.of("x-request-source", List.of("mobile", "trusted")), (name, value) -> true);
        byte[] body = "GateWeave".getBytes(StandardCharsets.UTF_8);
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), headers, body);
        var target = new InvocationTarget("DUBBO", "cn.example.GreetingService", "sayHi",
                List.of("java.lang.String"), Duration.ofSeconds(3), Map.of("version", "1.0.0"));
        var route = new GatewayRoute(
                "route-say-hi", "/sayHi", "POST", "greeting-service", target, Map.of("priority", "10"));
        var session = new GatewaySession(command, route);
        var mapper = new InvocationRequestMapper();

        // When
        var request = mapper.map(session);

        // Then
        assertThat(request.requestId()).isEqualTo("request-1");
        assertThat(request.routeId()).isEqualTo("route-say-hi");
        assertThat(request.protocol()).isEqualTo("dubbo");
        assertThat(request.service()).isEqualTo("cn.example.GreetingService");
        assertThat(request.operation()).isEqualTo("sayHi");
        assertThat(request.parameterTypes()).containsExactly("java.lang.String");
        assertThat(request.timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(request.headers()).containsEntry("x-request-source", "mobile, trusted");
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThat(new String(request.payload(), StandardCharsets.UTF_8)).isEqualTo("GateWeave");
    }

    @Test
    void shouldProtectMappedPayloadFromExternalModification() {
        // Given
        byte[] sourceBody = "GateWeave".getBytes(StandardCharsets.UTF_8);
        var mapper = new InvocationRequestMapper();
        var session = session(sourceBody);

        // When
        var request = mapper.map(session);
        sourceBody[0] = 'X';
        byte[] exposedPayload = request.payload();
        exposedPayload[0] = 'Y';

        // Then
        assertThat(new String(request.payload(), StandardCharsets.UTF_8)).isEqualTo("GateWeave");
    }

    @Test
    void shouldRejectMissingSession() {
        // Given
        var mapper = new InvocationRequestMapper();

        // When & Then
        assertThatThrownBy(() -> mapper.map(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("session must not be null");
    }

    private static GatewaySession session(byte[] body) {
        HttpHeaders headers = HttpHeaders.of(Map.of(), (name, value) -> true);
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), headers, body);
        var target = new InvocationTarget("dubbo", "cn.example.GreetingService", "sayHi",
                List.of("java.lang.String"), Duration.ofSeconds(3), Map.of("version", "1.0.0"));
        var route = new GatewayRoute(
                "route-say-hi", "/sayHi", "POST", "greeting-service", target, Map.of("priority", "10"));
        return new GatewaySession(command, route);
    }
}
