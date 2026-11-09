package com.lavyoung.gateweave.contract.gateway;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeHeartbeatRequestTests {

    private static final UUID LEASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldCreateGatewayNodeHeartbeatRequest() {
        // When
        GatewayNodeHeartbeatRequest request = new GatewayNodeHeartbeatRequest(LEASE_ID, 7);

        // Then
        assertThat(request.leaseId()).isEqualTo(LEASE_ID);
        assertThat(request.currentConfigVersion()).isEqualTo(7);
    }

    @Test
    void shouldAllowZeroConfigurationVersionBeforeFirstConfigurationIsApplied() {
        // When
        GatewayNodeHeartbeatRequest request = new GatewayNodeHeartbeatRequest(LEASE_ID, 0);

        // Then
        assertThat(request.currentConfigVersion()).isZero();
    }

    @Test
    void shouldRejectMissingLeaseId() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeHeartbeatRequest(null, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseId must not be null");
    }

    @Test
    void shouldRejectNegativeConfigurationVersion() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeHeartbeatRequest(LEASE_ID, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentConfigVersion must not be negative");
    }
}
