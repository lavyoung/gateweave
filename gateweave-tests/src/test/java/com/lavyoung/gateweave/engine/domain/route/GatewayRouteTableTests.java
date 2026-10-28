package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRouteTableTests {

    private static final InvocationTarget DUBBO_TARGET = new InvocationTarget(
            "dubbo", "cn.example.ActivityBooth", "sayHi",
            List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
            Map.of("version", "1.0.0"));

    @Test
    void shouldReturnEmptyWhenRouteTableHasNoRoutes() {
        // Given
        var routeTable = new GatewayRouteTable(List.of());

        // When
        var matchedRoute = routeTable.match("GET", URI.create("/sayHi"));

        // Then
        assertThat(matchedRoute).isEmpty();
    }

    @Test
    void shouldMatchRouteIgnoringMethodCaseAndQuery() {
        // Given
        var expectedRoute = route("route-say-hi", "/sayHi", "GET");
        var routeTable = new GatewayRouteTable(List.of(expectedRoute));

        // When
        var matchedRoute = routeTable.match("get", URI.create("/sayHi?name=gateweave"));

        // Then
        assertThat(matchedRoute).containsSame(expectedRoute);
    }

    @Test
    void shouldReturnEmptyWhenNoRouteMatches() {
        // Given
        var routeTable = new GatewayRouteTable(List.of(route("route-say-hi", "/sayHi", "GET")));

        // When & Then
        assertThat(routeTable.match("POST", URI.create("/sayHi"))).isEmpty();
        assertThat(routeTable.match("GET", URI.create("/other"))).isEmpty();
    }

    @Test
    void shouldRejectDuplicateRouteId() {
        // Given
        var firstRoute = route("route-say-hi", "/sayHi", "GET");
        var secondRoute = route("route-say-hi", "/goodbye", "POST");

        // When & Then
        assertThatThrownBy(() -> new GatewayRouteTable(List.of(firstRoute, secondRoute)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate routeId: route-say-hi");
    }

    @Test
    void shouldRejectDuplicateMethodAndPath() {
        // Given
        var firstRoute = route("route-say-hi-v1", "/sayHi", "GET");
        var secondRoute = route("route-say-hi-v2", "/sayHi", "get");

        // When & Then
        assertThatThrownBy(() -> new GatewayRouteTable(List.of(firstRoute, secondRoute)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate route match: GET /sayHi");
    }

    @Test
    void shouldKeepSnapshotWhenSourceListChanges() {
        // Given
        var sourceRoutes = new ArrayList<>(List.of(route("route-say-hi", "/sayHi", "GET")));
        var routeTable = new GatewayRouteTable(sourceRoutes);

        // When
        sourceRoutes.clear();

        // Then
        assertThat(routeTable.match("GET", URI.create("/sayHi"))).isPresent();
    }

    @Test
    void shouldRejectNullRoutesAndMatchArguments() {
        // Given
        var routeTable = new GatewayRouteTable(List.of());
        var routesWithNull = new ArrayList<GatewayRoute>();
        routesWithNull.add(null);

        // When & Then
        assertThatThrownBy(() -> new GatewayRouteTable(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routes must not be null");
        assertThatThrownBy(() -> new GatewayRouteTable(routesWithNull))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> routeTable.match(null, URI.create("/sayHi")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestMethod must not be null");
        assertThatThrownBy(() -> routeTable.match("GET", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestUri must not be null");
    }

    private static GatewayRoute route(String routeId, String path, String method) {
        return new GatewayRoute(routeId, path, method, "activity-service", DUBBO_TARGET, Map.of());
    }
}
