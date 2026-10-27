package com.lavyoung.gateweave.engine.application.session;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGatewaySessionFactoryTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);
    private static final GatewayRoute ROUTE = new GatewayRoute(
            "route-say-hi", "/sayHi", "POST", "greeting-service", target(), Map.of());
    private static final GatewayRouteTable ROUTE_TABLE = new GatewayRouteTable(List.of(ROUTE));

    @Test
    void shouldOpenSessionWhenRouteMatches() {
        // Given
        var factory = new DefaultGatewaySessionFactory(ROUTE_TABLE);
        var command = command("POST", "/sayHi");

        // When
        var session = factory.openSession(command);

        // Then
        assertThat(session).isPresent();
        assertThat(session.orElseThrow().command()).isSameAs(command);
        assertThat(session.orElseThrow().route()).isSameAs(ROUTE);
    }

    @Test
    void shouldReturnEmptyWhenPathDoesNotMatch() {
        // Given
        var factory = new DefaultGatewaySessionFactory(ROUTE_TABLE);

        // When
        var session = factory.openSession(command("POST", "/missing"));

        // Then
        assertThat(session).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenMethodDoesNotMatch() {
        // Given
        var factory = new DefaultGatewaySessionFactory(ROUTE_TABLE);

        // When
        var session = factory.openSession(command("GET", "/sayHi"));

        // Then
        assertThat(session).isEmpty();
    }

    @Test
    void shouldRejectMissingRouteTableOrCommand() {
        // Given
        var factory = new DefaultGatewaySessionFactory(ROUTE_TABLE);

        // When & Then
        assertThatThrownBy(() -> new DefaultGatewaySessionFactory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeTable must not be null");
        assertThatThrownBy(() -> factory.openSession(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
    }

    private static GatewayRequestCommand command(String method, String uri) {
        return new GatewayRequestCommand("request-1", method, URI.create(uri), EMPTY_HEADERS, new byte[0]);
    }

    private static InvocationTarget target() {
        return new InvocationTarget("dubbo", "cn.example.GreetingService", "sayHi",
                List.of("java.lang.String"), Duration.ofSeconds(3), Map.of("version", "1.0.0"));
    }
}
