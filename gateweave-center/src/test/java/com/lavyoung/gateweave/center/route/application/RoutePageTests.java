package com.lavyoung.gateweave.center.route.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutePageTests {

    @Test
    void shouldCreateImmutablePageAndCalculateTotalPages() {
        // Given
        RouteDefinition route = route("route-one", "/one");
        List<RouteDefinition> routes = new ArrayList<>(List.of(route));

        // When
        RoutePage page = new RoutePage(routes, 2, 2, 3);
        routes.clear();

        // Then
        assertThat(page.routes()).containsExactly(route);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThatThrownBy(() -> page.routes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldCalculateEmptyAndExactTotalPages() {
        // When
        RoutePage emptyPage = new RoutePage(List.of(), 1, 20, 0);
        RoutePage exactPage = new RoutePage(List.of(), 3, 10, 30);

        // Then
        assertThat(emptyPage.totalPages()).isZero();
        assertThat(exactPage.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldRejectMissingRoutes() {
        // Given
        List<RouteDefinition> routesWithNull = new ArrayList<>();
        routesWithNull.add(null);

        // When & Then
        assertThatThrownBy(() -> new RoutePage(null, 1, 20, 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RoutePage(routesWithNull, 1, 20, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectInvalidPageMetadata() {
        // When & Then
        assertThatThrownBy(() -> new RoutePage(List.of(), 0, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageNumber must be greater than zero");
        assertThatThrownBy(() -> new RoutePage(List.of(), 1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be greater than zero");
        assertThatThrownBy(() -> new RoutePage(List.of(), 1, 20, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalElements must not be negative");
    }

    @Test
    void shouldRejectInconsistentRouteCounts() {
        // Given
        List<RouteDefinition> twoRoutes = List.of(route("route-one", "/one"), route("route-two", "/two"));

        // When & Then
        assertThatThrownBy(() -> new RoutePage(twoRoutes, 1, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("routes size must not exceed pageSize");
        assertThatThrownBy(() -> new RoutePage(twoRoutes, 1, 2, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("routes size must not exceed totalElements");
    }

    private static RouteDefinition route(String routeId, String path) {
        return new RouteDefinition(routeId, "default", path, "GET", "activity-query",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of());
    }
}
