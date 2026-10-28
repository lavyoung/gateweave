package com.lavyoung.gateweave.engine.application.session;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewaySessionTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);

    @Test
    void shouldBindRequestCommandAndMatchedRoute() {
        // Given
        var command = command();
        var route = route();

        // When
        var session = new GatewaySession(command, route);

        // Then
        assertThat(session.command()).isSameAs(command);
        assertThat(session.route()).isSameAs(route);
    }

    @Test
    void shouldRejectMissingCommandOrRoute() {
        // Given
        var command = command();
        var route = route();

        // When & Then
        assertThatThrownBy(() -> new GatewaySession(null, route))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
        assertThatThrownBy(() -> new GatewaySession(command, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("route must not be null");
    }

    private static GatewayRequestCommand command() {
        return new GatewayRequestCommand("request-1", "POST", URI.create("/sayHi"), EMPTY_HEADERS,
                new HttpParameterValues(Map.of()), new byte[0]);
    }

    private static GatewayRoute route() {
        return new GatewayRoute("route-say-hi", "/sayHi", "POST", "greeting-service", target(), Map.of());
    }

    private static InvocationTarget target() {
        return new InvocationTarget("dubbo", "cn.example.GreetingService", "sayHi",
                List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
    }
}
