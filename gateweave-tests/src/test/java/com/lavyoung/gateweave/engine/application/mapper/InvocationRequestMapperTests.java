package com.lavyoung.gateweave.engine.application.mapper;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationRequestMapperTests {

    @Test
    void shouldMapSessionToInvocationRequest() {
        // Given
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json"),
                        "x-request-source", List.of("mobile", "trusted")),
                (name, value) -> true);
        byte[] body = "\"GateWeave\"".getBytes(StandardCharsets.UTF_8);
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), headers,
                new HttpParameterValues(Map.of()), body);
        var target = new InvocationTarget("DUBBO", "cn.example.GreetingService", "sayHi",
                List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
        var route = new GatewayRoute(
                "route-say-hi", "/sayHi", "POST", "greeting-service", target, Map.of("priority", "10"));
        var session = new GatewaySession(command, route);
        var mapper = mapper();

        // When
        var request = mapper.map(session);

        // Then
        assertThat(request.requestId()).isEqualTo("request-1");
        assertThat(request.routeId()).isEqualTo("route-say-hi");
        assertThat(request.protocol()).isEqualTo("dubbo");
        assertThat(request.service()).isEqualTo("cn.example.GreetingService");
        assertThat(request.operation()).isEqualTo("sayHi");
        assertThat(request.invocationArguments().parameterTypes()).containsExactly("java.lang.String");
        assertThat(request.invocationArguments().arguments()).containsExactly("GateWeave");
        assertThat(request.timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(request.headers()).containsEntry("x-request-source", "mobile, trusted");
        assertThat(request.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThat(new String(request.payload(), StandardCharsets.UTF_8)).isEqualTo("\"GateWeave\"");
    }

    @Test
    void shouldProtectMappedPayloadFromExternalModification() {
        // Given
        byte[] sourceBody = "\"GateWeave\"".getBytes(StandardCharsets.UTF_8);
        var mapper = mapper();
        var session = session(sourceBody);

        // When
        var request = mapper.map(session);
        sourceBody[0] = 'X';
        byte[] exposedPayload = request.payload();
        exposedPayload[0] = 'Y';

        // Then
        assertThat(new String(request.payload(), StandardCharsets.UTF_8)).isEqualTo("\"GateWeave\"");
    }

    @Test
    void shouldMapFormParametersToInvocationArguments() {
        // Given
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("application/x-www-form-urlencoded")), (name, value) -> true);
        byte[] body = "name=GateWeave".getBytes(StandardCharsets.UTF_8);
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), headers,
                new HttpParameterValues(Map.of()), new HttpParameterValues(Map.of("name", List.of("GateWeave"))),
                body);
        var target = new InvocationTarget("dubbo", "cn.example.GreetingService", "sayHi",
                List.of(new InvocationParameter("name", String.class.getName())), Duration.ofSeconds(3), Map.of());
        var route = new GatewayRoute("route-say-hi", "/sayHi", "POST", "greeting-service", target, Map.of());
        var mapper = mapper();

        // When
        var request = mapper.map(new GatewaySession(command, route));

        // Then
        assertThat(request.invocationArguments().parameterTypes()).containsExactly(String.class.getName());
        assertThat(request.invocationArguments().arguments()).containsExactly("GateWeave");
        assertThat(new String(request.payload(), StandardCharsets.UTF_8)).isEqualTo("name=GateWeave");
    }

    @Test
    void shouldRejectMissingSession() {
        // Given
        var mapper = mapper();

        // When & Then
        assertThatThrownBy(() -> mapper.map(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("session must not be null");
        assertThatThrownBy(() -> new InvocationRequestMapper(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("argumentsResolver must not be null");
    }

    private static GatewaySession session(byte[] body) {
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("application/json")),
                (name, value) -> true);
        var command = new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), headers,
                new HttpParameterValues(Map.of()), body);
        var target = new InvocationTarget("dubbo", "cn.example.GreetingService", "sayHi",
                List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
        var route = new GatewayRoute(
                "route-say-hi", "/sayHi", "POST", "greeting-service", target, Map.of("priority", "10"));
        return new GatewaySession(command, route);
    }

    private static InvocationRequestMapper mapper() {
        return new InvocationRequestMapper(new GatewayInvocationArgumentsResolver(new ObjectMapper()));
    }
}
