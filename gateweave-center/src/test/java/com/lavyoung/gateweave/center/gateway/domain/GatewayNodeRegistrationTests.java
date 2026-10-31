package com.lavyoung.gateweave.center.gateway.domain;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeRegistrationTests {

    @Test
    void shouldNormalizeTextAndKeepValidAddress() {
        // Given
        URI address = URI.create("https://gateway.example.com:7397");

        // When
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                " node-1 ", " default ", " Gateway Node 1 ", address);

        // Then
        assertThat(registration.nodeId()).isEqualTo("node-1");
        assertThat(registration.gatewayGroupId()).isEqualTo("default");
        assertThat(registration.nodeName()).isEqualTo("Gateway Node 1");
        assertThat(registration.advertisedAddress()).isEqualTo(address);
    }

    @Test
    void shouldRejectBlankTextValues() {
        // Given
        URI address = URI.create("http://127.0.0.1:7397");

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeRegistration(" ", "default", "Gateway", address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeId must not be blank");
        assertThatThrownBy(() -> new GatewayNodeRegistration("node-1", " ", "Gateway", address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
        assertThatThrownBy(() -> new GatewayNodeRegistration("node-1", "default", " ", address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeName must not be blank");
    }

    @Test
    void shouldRejectValuesExceedingDatabaseLengths() {
        // Given
        URI address = URI.create("http://127.0.0.1:7397");

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeRegistration("n".repeat(65), "default", "Gateway", address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeId length must not exceed 64");
        assertThatThrownBy(() -> new GatewayNodeRegistration("node-1", "g".repeat(65), "Gateway", address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId length must not exceed 64");
        assertThatThrownBy(() -> new GatewayNodeRegistration("node-1", "default", "n".repeat(129), address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeName length must not exceed 128");
    }

    @Test
    void shouldRejectUnsafeAdvertisedAddresses() {
        // When & Then
        assertThatThrownBy(() -> registration(URI.create("/relative")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("advertisedAddress must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> registration(URI.create("ftp://gateway.example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("advertisedAddress must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> registration(URI.create("http://user:password@gateway.example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("advertisedAddress must not contain user information");
        assertThatThrownBy(() -> registration(URI.create("http://gateway.example.com?token=secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("advertisedAddress must not contain query or fragment");
    }

    private static GatewayNodeRegistration registration(URI address) {
        return new GatewayNodeRegistration("node-1", "default", "Gateway Node 1", address);
    }
}
