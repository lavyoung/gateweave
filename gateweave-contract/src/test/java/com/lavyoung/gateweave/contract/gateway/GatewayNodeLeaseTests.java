package com.lavyoung.gateweave.contract.gateway;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeLeaseTests {

    private static final UUID LEASE_ID = UUID.fromString("9f692504-a95a-4d97-9286-40c793f24d4e");
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    private static final Duration LEASE_TIMEOUT = Duration.ofSeconds(30);

    @Test
    void shouldCreateGatewayNodeLease() {
        // When
        GatewayNodeLease lease = new GatewayNodeLease("node-1", LEASE_ID, HEARTBEAT_INTERVAL, LEASE_TIMEOUT);

        // Then
        assertThat(lease.nodeId()).isEqualTo("node-1");
        assertThat(lease.leaseId()).isEqualTo(LEASE_ID);
        assertThat(lease.heartbeatInterval()).isEqualTo(HEARTBEAT_INTERVAL);
        assertThat(lease.leaseTimeout()).isEqualTo(LEASE_TIMEOUT);
    }

    @Test
    void shouldRejectBlankNodeId() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLease(" ", LEASE_ID, HEARTBEAT_INTERVAL, LEASE_TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeId must not be blank");
    }

    @Test
    void shouldRejectNonPositiveHeartbeatInterval() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, Duration.ZERO, LEASE_TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("heartbeatInterval must be positive");
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, Duration.ofSeconds(-1), LEASE_TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("heartbeatInterval must be positive");
    }

    @Test
    void shouldRejectLeaseTimeoutNotGreaterThanHeartbeatInterval() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("leaseTimeout must be greater than heartbeatInterval");
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, HEARTBEAT_INTERVAL, Duration.ofSeconds(9)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("leaseTimeout must be greater than heartbeatInterval");
    }

    @Test
    void shouldRejectNullFields() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLease(null, LEASE_ID, HEARTBEAT_INTERVAL, LEASE_TIMEOUT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeId must not be null");
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", null, HEARTBEAT_INTERVAL, LEASE_TIMEOUT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseId must not be null");
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, null, LEASE_TIMEOUT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("heartbeatInterval must not be null");
        assertThatThrownBy(() -> new GatewayNodeLease("node-1", LEASE_ID, HEARTBEAT_INTERVAL, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseTimeout must not be null");
    }
}
