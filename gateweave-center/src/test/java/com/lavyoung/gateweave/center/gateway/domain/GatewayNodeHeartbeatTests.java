package com.lavyoung.gateweave.center.gateway.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeHeartbeatTests {

    private static final UUID LEASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant RECEIVED_AT = Instant.parse("2026-11-09T12:00:00Z");

    @Test
    void shouldCreateGatewayNodeHeartbeat() {
        // When
        GatewayNodeHeartbeat heartbeat = new GatewayNodeHeartbeat(" node-1 ", LEASE_ID, RECEIVED_AT, 7);

        // Then
        assertThat(heartbeat.nodeId()).isEqualTo("node-1");
        assertThat(heartbeat.leaseId()).isEqualTo(LEASE_ID);
        assertThat(heartbeat.receivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(heartbeat.currentConfigVersion()).isEqualTo(7);
    }

    @Test
    void shouldAllowZeroConfigurationVersion() {
        // When
        GatewayNodeHeartbeat heartbeat = new GatewayNodeHeartbeat("node-1", LEASE_ID, RECEIVED_AT, 0);

        // Then
        assertThat(heartbeat.currentConfigVersion()).isZero();
    }

    @Test
    void shouldRejectBlankNodeId() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeHeartbeat(" ", LEASE_ID, RECEIVED_AT, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeId must not be blank");
    }

    @Test
    void shouldRejectNegativeConfigurationVersion() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeHeartbeat("node-1", LEASE_ID, RECEIVED_AT, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentConfigVersion must not be negative");
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeHeartbeat(null, LEASE_ID, RECEIVED_AT, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeId must not be null");
        assertThatThrownBy(() -> new GatewayNodeHeartbeat("node-1", null, RECEIVED_AT, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseId must not be null");
        assertThatThrownBy(() -> new GatewayNodeHeartbeat("node-1", LEASE_ID, null, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("receivedAt must not be null");
    }
}
