package com.lavyoung.gateweave.center.gateway.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeTests {

    private static final Instant REGISTERED_AT = Instant.parse("2026-11-07T00:00:00Z");

    @Test
    void shouldCreateRegisteredNodeWithNewLease() {
        // Given
        GatewayNodeRegistration registration = registration();

        // When
        GatewayNode node = GatewayNode.registered(registration, REGISTERED_AT);

        // Then
        assertThat(node.registration()).isEqualTo(registration);
        assertThat(node.status()).isEqualTo(GatewayNodeStatus.ONLINE);
        assertThat(node.leaseId()).isNotNull();
        assertThat(node.lastHeartbeatAt()).isEqualTo(REGISTERED_AT);
        assertThat(node.currentConfigVersion()).isZero();
    }

    @Test
    void shouldIssueDifferentLeaseForEachRegistration() {
        // When
        GatewayNode first = GatewayNode.registered(registration(), REGISTERED_AT);
        GatewayNode second = GatewayNode.registered(registration(), REGISTERED_AT);

        // Then
        assertThat(first.leaseId()).isNotEqualTo(second.leaseId());
    }

    @Test
    void shouldRejectMissingLease() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNode(
                registration(), GatewayNodeStatus.ONLINE, null, REGISTERED_AT, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseId must not be null");
    }

    @Test
    void shouldRejectNegativeConfigurationVersion() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNode(
                registration(), GatewayNodeStatus.ONLINE, UUID.randomUUID(), REGISTERED_AT, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentConfigVersion must not be negative");
    }

    private static GatewayNodeRegistration registration() {
        return new GatewayNodeRegistration(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));
    }
}
