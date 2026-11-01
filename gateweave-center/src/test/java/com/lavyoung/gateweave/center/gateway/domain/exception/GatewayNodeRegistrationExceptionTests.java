package com.lavyoung.gateweave.center.gateway.domain.exception;

import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;

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
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.responseCode()).isSameAs(GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND);
        assertThat(exception.arguments()).containsExactly("missing-group");
        assertThat(exception.gatewayGroupId()).isEqualTo("missing-group");
        assertThat(exception).hasMessage("网关分组 missing-group 不存在");
    }

    @Test
    void shouldPreserveConflictingAdvertisedAddress() {
        // Given
        URI address = URI.create("http://127.0.0.1:8080");

        // When
        GatewayNodeAddressConflictException exception = new GatewayNodeAddressConflictException(address);

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.responseCode()).isSameAs(GatewayResponseCode.GATEWAY_NODE_ADDRESS_CONFLICT);
        assertThat(exception.arguments()).containsExactly("http://127.0.0.1:8080");
        assertThat(exception.advertisedAddress()).isEqualTo(address);
        assertThat(exception).hasMessage("网关节点地址 http://127.0.0.1:8080 已被占用");
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
