package com.lavyoung.gateweave.contract.gateway;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterGatewayNodeRequestTests {

    @Test
    void shouldPreserveRegistrationFields() {
        // When
        RegisterGatewayNodeRequest request = new RegisterGatewayNodeRequest(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));

        // Then
        assertThat(request.nodeId()).isEqualTo("node-1");
        assertThat(request.gatewayGroupId()).isEqualTo("default");
        assertThat(request.nodeName()).isEqualTo("Gateway Node 1");
        assertThat(request.advertisedAddress()).isEqualTo(URI.create("http://127.0.0.1:7397"));
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        // When & Then
        assertThatThrownBy(() -> request(null, "default", "Gateway Node 1", "http://127.0.0.1:7397"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeId must not be null");
        assertThatThrownBy(() -> request("node-1", null, "Gateway Node 1", "http://127.0.0.1:7397"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> request("node-1", "default", null, "http://127.0.0.1:7397"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeName must not be null");
        assertThatThrownBy(() -> new RegisterGatewayNodeRequest("node-1", "default", "Gateway Node 1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("advertisedAddress must not be null");
    }

    @Test
    void shouldLeaveNodeBusinessRulesToControlPlaneDomain() {
        // When
        RegisterGatewayNodeRequest request = request(" ", "g".repeat(65), " ", "ftp://127.0.0.1:7397");

        // Then
        assertThat(request.nodeId()).isBlank();
        assertThat(request.gatewayGroupId()).hasSize(65);
        assertThat(request.nodeName()).isBlank();
        assertThat(request.advertisedAddress()).hasScheme("ftp");
    }

    private static RegisterGatewayNodeRequest request(
            String nodeId, String gatewayGroupId, String nodeName, String advertisedAddress) {
        return new RegisterGatewayNodeRequest(
                nodeId, gatewayGroupId, nodeName,
                advertisedAddress == null ? null : URI.create(advertisedAddress));
    }
}
