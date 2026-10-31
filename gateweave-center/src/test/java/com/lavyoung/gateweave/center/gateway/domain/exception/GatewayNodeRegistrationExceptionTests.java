package com.lavyoung.gateweave.center.gateway.domain.exception;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeRegistrationExceptionTests {

    @Test
    void shouldPreserveMissingGatewayGroupId() {
        // Given & When
        GatewayGroupNotFoundException exception = new GatewayGroupNotFoundException("missing-group");

        // Then
        assertThat(exception.gatewayGroupId()).isEqualTo("missing-group");
        assertThat(exception).hasMessage("Gateway group does not exist: missing-group");
    }

    @Test
    void shouldPreserveConflictingAdvertisedAddress() {
        // Given
        URI address = URI.create("http://127.0.0.1:8080");

        // When
        GatewayNodeAddressConflictException exception = new GatewayNodeAddressConflictException(address);

        // Then
        assertThat(exception.advertisedAddress()).isEqualTo(address);
        assertThat(exception).hasMessage("Gateway node address is already in use: http://127.0.0.1:8080");
    }

    @Test
    void shouldRejectMissingExceptionArguments() {
        // When & Then
        assertThatThrownBy(() -> new GatewayGroupNotFoundException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new GatewayNodeAddressConflictException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("advertisedAddress must not be null");
    }
}
