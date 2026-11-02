package com.lavyoung.gateweave.contract.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayConfigurationSnapshotTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-02T10:15:30Z");

    @Test
    void shouldCreateImmutableConfigurationSnapshot() {
        // Given
        var sourceRoutes = new ArrayList<>(List.of(route()));

        // When
        var snapshot = new GatewayConfigurationSnapshot(
                "default", 7, PUBLISHED_AT, "sha256:configuration", sourceRoutes);
        sourceRoutes.clear();

        // Then
        assertThat(snapshot.gatewayGroupId()).isEqualTo("default");
        assertThat(snapshot.version()).isEqualTo(7);
        assertThat(snapshot.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(snapshot.checksum()).isEqualTo("sha256:configuration");
        assertThat(snapshot.routes()).containsExactly(route());
        assertThatThrownBy(() -> snapshot.routes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowEmptyRouteSnapshot() {
        // When
        var snapshot = new GatewayConfigurationSnapshot(
                "default", 1, PUBLISHED_AT, "sha256:empty", List.of());

        // Then
        assertThat(snapshot.routes()).isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredProperties() {
        // Given
        var routesWithNull = new ArrayList<RouteConfiguration>();
        routesWithNull.add(null);

        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                null, 1, PUBLISHED_AT, "sha256:configuration", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                "default", 1, null, "sha256:configuration", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("publishedAt must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot("default", 1, PUBLISHED_AT, null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("checksum must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                "default", 1, PUBLISHED_AT, "sha256:configuration", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routes must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                "default", 1, PUBLISHED_AT, "sha256:configuration", routesWithNull))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectBlankGatewayGroupIdOrChecksum() {
        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                " ", 1, PUBLISHED_AT, "sha256:configuration", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot("default", 1, PUBLISHED_AT, " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("checksum must not be blank");
    }

    @Test
    void shouldRejectNonPositiveVersion() {
        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                "default", 0, PUBLISHED_AT, "sha256:configuration", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be greater than zero");
        assertThatThrownBy(() -> new GatewayConfigurationSnapshot(
                "default", -1, PUBLISHED_AT, "sha256:configuration", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be greater than zero");
    }

    private static RouteConfiguration route() {
        var target = new InvocationTargetConfiguration(
                "dubbo", "cn.example.ActivityBooth", "sayHi",
                List.of(new InvocationParameterConfiguration("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
        return new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", RouteAuthenticationMode.REQUIRED,
                target, Map.of());
    }
}
