package com.lavyoung.gateweave.center.route.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteDefinitionTests {

    private static final RouteParameterMapping NAME_MAPPING = new RouteParameterMapping(0, "name");
    private static final RouteParameterMapping AGE_MAPPING = new RouteParameterMapping(1, "age");

    @Test
    void shouldCreateRouteWithNormalizedMethodAndImmutableSnapshots() {
        // Given
        Map<String, String> targetAttributes = new HashMap<>(Map.of("version", "1.0.0"));
        Map<String, String> attributes = new HashMap<>(Map.of("source", "center"));
        List<RouteParameterMapping> mappings = new ArrayList<>(List.of(NAME_MAPPING));

        // When
        RouteDefinition route = route("/sayHi", "get", Duration.ofSeconds(3), 2L,
                targetAttributes, attributes, mappings);
        targetAttributes.put("group", "test");
        attributes.put("trace", "enabled");
        mappings.add(AGE_MAPPING);

        // Then
        assertThat(route.method()).isEqualTo("GET");
        assertThat(route.targetAttributes()).containsExactly(Map.entry("version", "1.0.0"));
        assertThat(route.attributes()).containsExactly(Map.entry("source", "center"));
        assertThat(route.parameterMappings()).containsExactly(NAME_MAPPING);
        assertThat(route.revision()).isEqualTo(2L);
        assertThatThrownBy(() -> route.targetAttributes().put("group", "test"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> route.parameterMappings().add(AGE_MAPPING))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowRouteWithoutParameterMappings() {
        // When
        RouteDefinition route = route("/health", "GET", Duration.ofSeconds(1), 0L,
                Map.of(), Map.of(), List.of());

        // Then
        assertThat(route.parameterMappings()).isEmpty();
    }

    @Test
    void shouldRejectMissingReferences() {
        // When & Then
        assertThatThrownBy(() -> new RouteDefinition(null, "default", "/sayHi", "GET", "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeId must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", null, "/sayHi", "GET", "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", null, "GET", "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("path must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", null, "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("method must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET", null,
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceOperationId must not be null");
    }

    @Test
    void shouldRejectMissingPoliciesAndCollections() {
        // When & Then
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", null, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticationRequirement must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", RouteAuthenticationRequirement.REQUIRED, null, Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("invocationTimeout must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), null, Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("targetAttributes must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), null,
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                null, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status must not be null");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET",
                "activity-say-hi", RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameterMappings must not be null");
    }

    @Test
    void shouldRejectBlankRequiredValues() {
        // When & Then
        assertThatThrownBy(() -> route("/sayHi", " ", Duration.ofSeconds(3), 0L,
                Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method must not be blank");
        assertThatThrownBy(() -> new RouteDefinition(" ", "default", "/sayHi", "GET", "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("routeId must not be blank");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", " ", "/sayHi", "GET", "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
        assertThatThrownBy(() -> new RouteDefinition("route-say-hi", "default", "/sayHi", "GET", " ",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceOperationId must not be blank");
    }

    @Test
    void shouldRejectInvalidPath() {
        // When & Then
        assertThatThrownBy(() -> route(" ", "GET", Duration.ofSeconds(3), 0L, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must not be blank");
        assertThatThrownBy(() -> route("sayHi", "GET", Duration.ofSeconds(3), 0L, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must start with '/'");
    }

    @Test
    void shouldRejectInvalidTimeoutAndRevision() {
        // When & Then
        assertThatThrownBy(() -> route("/sayHi", "GET", Duration.ZERO, 0L, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invocationTimeout must be greater than zero");
        assertThatThrownBy(() -> route(
                "/sayHi", "GET", Duration.ofSeconds(-1), 0L, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invocationTimeout must be greater than zero");
        assertThatThrownBy(() -> route(
                "/sayHi", "GET", Duration.ofSeconds(3), -1L, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revision must not be negative");
    }

    @Test
    void shouldRejectNonContinuousParameterPositions() {
        // Given
        RouteParameterMapping secondMapping = new RouteParameterMapping(2, "age");

        // When & Then
        assertThatThrownBy(() -> route("/sayHi", "GET", Duration.ofSeconds(3), 0L,
                Map.of(), Map.of(), List.of(NAME_MAPPING, secondMapping)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter positions must be continuous from zero");
    }

    @Test
    void shouldRejectDuplicatedRequestParameterNames() {
        // Given
        RouteParameterMapping duplicatedName = new RouteParameterMapping(1, "name");

        // When & Then
        assertThatThrownBy(() -> route("/sayHi", "GET", Duration.ofSeconds(3), 0L,
                Map.of(), Map.of(), List.of(NAME_MAPPING, duplicatedName)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request parameter names must not be duplicated");
    }

    private static RouteDefinition route(String path, String method, Duration timeout, long revision,
            Map<String, String> targetAttributes, Map<String, String> attributes,
            List<RouteParameterMapping> mappings) {
        return new RouteDefinition("route-say-hi", "default", path, method, "activity-say-hi",
                RouteAuthenticationRequirement.REQUIRED, timeout, targetAttributes, attributes,
                RouteStatus.ENABLED, revision, mappings);
    }
}
