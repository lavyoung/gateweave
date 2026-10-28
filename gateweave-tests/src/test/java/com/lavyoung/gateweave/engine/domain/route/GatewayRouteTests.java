package com.lavyoung.gateweave.engine.domain.route;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRouteTests {

    private static final InvocationTarget DUBBO_TARGET = new InvocationTarget(
            "dubbo", "cn.example.ActivityBooth", "sayHi",
            List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
            Map.of("version", "1.0.0"));

    @Test
    void shouldMatchMethodIgnoringCaseAndPathIgnoringQuery() {
        // Given
        var route = validRoute("get", Map.of());

        // When
        var matched = route.matches("GET", URI.create("/sayHi?name=test"));

        // Then
        assertThat(route.method()).isEqualTo("GET");
        assertThat(matched).isTrue();
    }

    @Test
    void shouldNotMatchDifferentMethodOrPath() {
        // Given
        var route = validRoute("GET", Map.of());

        // When & Then
        assertThat(route.matches("POST", URI.create("/sayHi"))).isFalse();
        assertThat(route.matches("GET", URI.create("/other"))).isFalse();
    }

    @Test
    void shouldCreateImmutableRouteAttributes() {
        // Given
        var sourceAttributes = new HashMap<>(Map.of("authentication", "required"));

        // When
        var route = validRoute("GET", sourceAttributes);
        sourceAttributes.put("rate-limit", "changed");

        // Then
        assertThat(route.attributes()).containsExactlyEntriesOf(Map.of("authentication", "required"));
        assertThatThrownBy(() -> route.attributes().put("rate-limit", "10"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectInvalidPathAndMatchArguments() {
        // Given
        var route = validRoute("GET", Map.of());

        // When & Then
        assertThatThrownBy(() -> new GatewayRoute(
                "route-say-hi", "sayHi", "GET", "activity-service", DUBBO_TARGET, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must start with '/'");
        assertThatThrownBy(() -> route.matches(null, URI.create("/sayHi")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestMethod must not be null");
        assertThatThrownBy(() -> route.matches("GET", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestUri must not be null");
    }

    private static GatewayRoute validRoute(String method, Map<String, String> attributes) {
        return new GatewayRoute("route-say-hi", "/sayHi", method, "activity-service", DUBBO_TARGET, attributes);
    }
}
