package com.lavyoung.gateweave.contract.configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteConfigurationTests {

    private static final InvocationTargetConfiguration DUBBO_TARGET = new InvocationTargetConfiguration(
            "dubbo", "cn.example.ActivityBooth", "sayHi",
            List.of(new InvocationParameterConfiguration("name", "java.lang.String")), Duration.ofSeconds(3),
            Map.of("version", "1.0.0"));

    @Test
    void shouldCreateRouteWithImmutableAttributes() {
        // Given
        var sourceAttributes = new HashMap<>(Map.of("authentication", "required"));

        // When
        var route = new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", DUBBO_TARGET, sourceAttributes);
        sourceAttributes.put("rate-limit", "changed");

        // Then
        assertThat(route.routeId()).isEqualTo("route-say-hi");
        assertThat(route.path()).isEqualTo("/sayHi");
        assertThat(route.method()).isEqualTo("GET");
        assertThat(route.serviceId()).isEqualTo("activity-service");
        assertThat(route.target()).isSameAs(DUBBO_TARGET);
        assertThat(route.attributes()).containsExactlyEntriesOf(Map.of("authentication", "required"));
        assertThatThrownBy(() -> route.attributes().put("rate-limit", "10"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectBlankRouteProperties() {
        // When & Then
        assertThatThrownBy(() -> new RouteConfiguration(
                " ", "/sayHi", "GET", "activity-service", DUBBO_TARGET, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("routeId must not be blank");
        assertThatThrownBy(() -> new RouteConfiguration(
                "route-say-hi", " ", "GET", "activity-service", DUBBO_TARGET, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must not be blank");
        assertThatThrownBy(() -> new RouteConfiguration(
                "route-say-hi", "/sayHi", " ", "activity-service", DUBBO_TARGET, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method must not be blank");
        assertThatThrownBy(() -> new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", " ", DUBBO_TARGET, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceId must not be blank");
    }

    @Test
    void shouldRejectMissingTargetAndAttributes() {
        // When & Then
        assertThatThrownBy(() -> new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("target must not be null");
        assertThatThrownBy(() -> new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", DUBBO_TARGET, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
    }
}
