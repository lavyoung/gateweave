package com.lavyoung.gateweave.center.route.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteParameterMappingTests {

    @Test
    void shouldCreateRouteParameterMapping() {
        // Given & When
        RouteParameterMapping mapping = new RouteParameterMapping(0, "userName");

        // Then
        assertThat(mapping.position()).isZero();
        assertThat(mapping.requestName()).isEqualTo("userName");
    }

    @Test
    void shouldRejectNegativePosition() {
        // When & Then
        assertThatThrownBy(() -> new RouteParameterMapping(-1, "userName"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("position must not be negative");
    }

    @Test
    void shouldRejectMissingRequestName() {
        // When & Then
        assertThatThrownBy(() -> new RouteParameterMapping(0, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestName must not be null");
    }

    @Test
    void shouldRejectBlankRequestName() {
        // When & Then
        assertThatThrownBy(() -> new RouteParameterMapping(0, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestName must not be blank");
    }
}
