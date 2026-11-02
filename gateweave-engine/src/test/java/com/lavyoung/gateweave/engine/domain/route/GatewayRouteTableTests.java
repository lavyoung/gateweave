package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    void shouldStartWithLocalConfigurationVersionZero() {
        // Given
        var routeTable = new GatewayRouteTable(List.of(route("route-local", "/local", "GET")));

        // When
        var currentVersion = routeTable.currentVersion();

        // Then
        assertThat(currentVersion).isZero();
    }

    @Test
    void shouldReplaceRoutesWhenPublishedVersionIsNew() {
        // Given
        var localRoute = route("route-local", "/local", "GET");
        var publishedRoute = route("route-published", "/published", "GET");
        var routeTable = new GatewayRouteTable(List.of(localRoute));

        // When
        var replaced = routeTable.replaceIfNew(1, List.of(publishedRoute));

        // Then
        assertThat(replaced).isTrue();
        assertThat(routeTable.currentVersion()).isEqualTo(1);
        assertThat(routeTable.match("GET", URI.create("/local"))).isEmpty();
        assertThat(routeTable.match("GET", URI.create("/published"))).containsSame(publishedRoute);
    }

    @Test
    void shouldIgnoreSameAndOlderPublishedVersions() {
        // Given
        var currentRoute = route("route-current", "/current", "GET");
        var staleRoute = route("route-stale", "/stale", "GET");
        var routeTable = new GatewayRouteTable(List.of());
        routeTable.replaceIfNew(2, List.of(currentRoute));

        // When
        var sameVersionReplaced = routeTable.replaceIfNew(2, List.of(staleRoute));
        var olderVersionReplaced = routeTable.replaceIfNew(1, List.of(staleRoute));

        // Then
        assertThat(sameVersionReplaced).isFalse();
        assertThat(olderVersionReplaced).isFalse();
        assertThat(routeTable.currentVersion()).isEqualTo(2);
        assertThat(routeTable.match("GET", URI.create("/current"))).containsSame(currentRoute);
        assertThat(routeTable.match("GET", URI.create("/stale"))).isEmpty();
    }

    @Test
    void shouldKeepLastValidSnapshotWhenReplacementIsInvalid() {
        // Given
        var currentRoute = route("route-current", "/current", "GET");
        var duplicateRoute = route("route-duplicate", "/duplicate", "GET");
        var routeTable = new GatewayRouteTable(List.of());
        routeTable.replaceIfNew(1, List.of(currentRoute));

        // When & Then
        assertThatThrownBy(() -> routeTable.replaceIfNew(2, List.of(duplicateRoute, duplicateRoute)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate routeId: route-duplicate");
        assertThat(routeTable.currentVersion()).isEqualTo(1);
        assertThat(routeTable.match("GET", URI.create("/current"))).containsSame(currentRoute);
    }

    @Test
    void shouldKeepHighestVersionWhenReplacementsAreConcurrent() throws Exception {
        // Given
        var routeTable = new GatewayRouteTable(List.of());
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            // When
            List<Future<Boolean>> replacements = executor.invokeAll(List.of(
                    () -> routeTable.replaceIfNew(1, List.of(route("route-v1", "/v1", "GET"))),
                    () -> routeTable.replaceIfNew(2, List.of(route("route-v2", "/v2", "GET"))),
                    () -> routeTable.replaceIfNew(3, List.of(route("route-v3", "/v3", "GET"))),
                    () -> routeTable.replaceIfNew(4, List.of(route("route-v4", "/v4", "GET")))));
            for (Future<Boolean> replacement : replacements) {
                replacement.get();
            }

            // Then
            assertThat(routeTable.currentVersion()).isEqualTo(4);
            assertThat(routeTable.match("GET", URI.create("/v4"))).isPresent();
            assertThat(routeTable.match("GET", URI.create("/v1"))).isEmpty();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldKeepPublishedSnapshotWhenSourceListChanges() {
        // Given
        var publishedRoute = route("route-published", "/published", "GET");
        var sourceRoutes = new ArrayList<>(List.of(publishedRoute));
        var routeTable = new GatewayRouteTable(List.of());

        // When
        routeTable.replaceIfNew(1, sourceRoutes);
        sourceRoutes.clear();

        // Then
        assertThat(routeTable.match("GET", URI.create("/published"))).containsSame(publishedRoute);
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
        return new GatewayRoute(routeId, path, method, "activity-service", RouteAuthenticationPolicy.ANONYMOUS,
                DUBBO_TARGET, Map.of());
    }
}
