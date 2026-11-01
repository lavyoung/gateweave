package com.lavyoung.gateweave.center.route.web.response;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.route.application.RoutePage;
import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteParameterMapping;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutePageResponseTests {

    @Test
    void shouldConvertApplicationPageToWebResponse() {
        // Given
        RouteDefinition route = new RouteDefinition(
                "route-query", "default", "/activities", "GET", "activity-query",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofMillis(2500),
                Map.of("version", "1.0.0"), Map.of("source", "center"), RouteStatus.ENABLED, 3L,
                List.of(new RouteParameterMapping(0, "name"), new RouteParameterMapping(1, "age")));
        RoutePage page = new RoutePage(List.of(route), 2, 10, 11);

        // When
        RoutePageResponse response = RoutePageResponse.from(page);

        // Then
        assertThat(response.pageNumber()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.routes()).singleElement().satisfies(routeResponse -> {
            assertThat(routeResponse.routeId()).isEqualTo("route-query");
            assertThat(routeResponse.gatewayGroupId()).isEqualTo("default");
            assertThat(routeResponse.path()).isEqualTo("/activities");
            assertThat(routeResponse.method()).isEqualTo("GET");
            assertThat(routeResponse.serviceOperationId()).isEqualTo("activity-query");
            assertThat(routeResponse.authenticationRequirement()).isEqualTo("REQUIRED");
            assertThat(routeResponse.invocationTimeoutMillis()).isEqualTo(2500L);
            assertThat(routeResponse.targetAttributes()).containsExactly(Map.entry("version", "1.0.0"));
            assertThat(routeResponse.attributes()).containsExactly(Map.entry("source", "center"));
            assertThat(routeResponse.status()).isEqualTo("ENABLED");
            assertThat(routeResponse.revision()).isEqualTo(3L);
            assertThat(routeResponse.parameterMappings())
                    .extracting(RoutePageResponse.ParameterMappingResponse::position)
                    .containsExactly(0, 1);
            assertThat(routeResponse.parameterMappings())
                    .extracting(RoutePageResponse.ParameterMappingResponse::requestName)
                    .containsExactly("name", "age");
        });
        assertThatThrownBy(() -> response.routes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldConvertEmptyApplicationPage() {
        // Given
        RoutePage page = new RoutePage(List.of(), 1, 20, 0);

        // When
        RoutePageResponse response = RoutePageResponse.from(page);

        // Then
        assertThat(response.routes()).isEmpty();
        assertThat(response.pageNumber()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void shouldRejectMissingApplicationPage() {
        // When & Then
        assertThatThrownBy(() -> RoutePageResponse.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("page must not be null");
    }
}
