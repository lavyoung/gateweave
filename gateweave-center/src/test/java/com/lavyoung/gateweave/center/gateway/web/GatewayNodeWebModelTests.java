package com.lavyoung.gateweave.center.gateway.web;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.web.request.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.center.gateway.web.response.GatewayNodeRegistrationResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodeWebModelTests {

    @Test
    void shouldAcceptValidRegistrationRequest() {
        // Given
        RegisterGatewayNodeRequest request = new RegisterGatewayNodeRequest(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:8080"));

        // When
        Set<ConstraintViolation<RegisterGatewayNodeRequest>> violations = validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectInvalidRegistrationRequestFields() {
        // Given
        RegisterGatewayNodeRequest request = new RegisterGatewayNodeRequest(
                " ", "g".repeat(65), " ", null);

        // When
        Set<ConstraintViolation<RegisterGatewayNodeRequest>> violations = validate(request);

        // Then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("nodeId", "gatewayGroupId", "nodeName", "advertisedAddress");
    }

    @Test
    void shouldConvertDomainResultToHttpResponse() {
        // Given
        Instant heartbeatAt = Instant.parse("2026-10-31T12:00:00Z");
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:8080"));
        GatewayNode node = new GatewayNode(registration, GatewayNodeStatus.ONLINE, heartbeatAt, 7);
        GatewayNodeRegistrationResult result = new GatewayNodeRegistrationResult(
                node, GatewayNodeRegistrationOutcome.REFRESHED);

        // When
        GatewayNodeRegistrationResponse response = GatewayNodeRegistrationResponse.from(result);

        // Then
        assertThat(response.nodeId()).isEqualTo("node-1");
        assertThat(response.gatewayGroupId()).isEqualTo("default");
        assertThat(response.nodeName()).isEqualTo("Gateway Node 1");
        assertThat(response.advertisedAddress()).isEqualTo("http://127.0.0.1:8080");
        assertThat(response.status()).isEqualTo("ONLINE");
        assertThat(response.outcome()).isEqualTo("REFRESHED");
        assertThat(response.lastHeartbeatAt()).isEqualTo(heartbeatAt);
        assertThat(response.currentConfigVersion()).isEqualTo(7);
    }

    @Test
    void shouldRejectMissingDomainResult() {
        // When & Then
        assertThatThrownBy(() -> GatewayNodeRegistrationResponse.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("result must not be null");
    }

    private static Set<ConstraintViolation<RegisterGatewayNodeRequest>> validate(RegisterGatewayNodeRequest request) {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            return validator.validate(request);
        }
    }
}
