package com.lavyoung.gateweave.center.gateway.configuration;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeLeasePropertiesTests {

    @Test
    void shouldCreateGatewayNodeLeaseProperties() {
        // Given
        Duration heartbeatInterval = Duration.ofSeconds(10);
        Duration leaseTimeout = Duration.ofSeconds(30);
        Duration expirationScanInterval = Duration.ofSeconds(5);

        // When
        GatewayNodeLeaseProperties properties =
                new GatewayNodeLeaseProperties(heartbeatInterval, leaseTimeout, expirationScanInterval);

        // Then
        assertThat(properties.heartbeatInterval()).isEqualTo(heartbeatInterval);
        assertThat(properties.leaseTimeout()).isEqualTo(leaseTimeout);
        assertThat(properties.expirationScanInterval()).isEqualTo(expirationScanInterval);
    }

    @Test
    void shouldRejectMissingValues() {
        // Given
        Duration heartbeatInterval = Duration.ofSeconds(10);
        Duration leaseTimeout = Duration.ofSeconds(30);
        Duration scanInterval = Duration.ofSeconds(5);

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLeaseProperties(null, leaseTimeout, scanInterval)).isInstanceOf(
                NullPointerException.class).hasMessage("heartbeatInterval must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseProperties(heartbeatInterval, null, scanInterval)).isInstanceOf(
                NullPointerException.class).hasMessage("leaseTimeout must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseProperties(heartbeatInterval, leaseTimeout, null)).isInstanceOf(
                NullPointerException.class).hasMessage("expirationScanInterval must not be null");
    }

    @Test
    void shouldRejectNonPositiveExpirationScanInterval() {
        // Given
        Duration heartbeatInterval = Duration.ofSeconds(10);
        Duration leaseTimeout = Duration.ofSeconds(30);

        // When & Then
        assertThatThrownBy(
                () -> new GatewayNodeLeaseProperties(heartbeatInterval, leaseTimeout, Duration.ZERO)).isInstanceOf(
                IllegalArgumentException.class).hasMessage("expirationScanInterval must be positive");
        assertThatThrownBy(() -> new GatewayNodeLeaseProperties(heartbeatInterval, leaseTimeout,
                Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expirationScanInterval must be positive");
    }
}
