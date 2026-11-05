package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.contract.web.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpGatewayControlPlaneClientTests {

    private static final RegisterGatewayNodeRequest REGISTRATION_REQUEST = new RegisterGatewayNodeRequest(
            "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));

    @Test
    void shouldRegisterNodeThroughHttpApi() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        JsonNode responseData = JsonNodeFactory.instance.objectNode();
        when(httpApi.registerNode(REGISTRATION_REQUEST))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Result.success(responseData)));
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When
        client.registerNode(REGISTRATION_REQUEST);

        // Then
        verify(httpApi).registerNode(REGISTRATION_REQUEST);
    }

    @Test
    void shouldRejectFailedRegistrationEnvelope() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        JsonNode responseData = JsonNodeFactory.instance.objectNode();
        Result<JsonNode> failure = new Result<>(false, "GATEWAY-0001", "Gateway group missing", responseData);
        when(httpApi.registerNode(REGISTRATION_REQUEST))
                .thenReturn(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(failure));
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When & Then
        assertThatThrownBy(() -> client.registerNode(REGISTRATION_REQUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gateway node registration failed with status 422 and code GATEWAY-0001");
    }

    @Test
    void shouldWrapRegistrationTransportFailure() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        ResourceAccessException failure = new ResourceAccessException("connection refused");
        when(httpApi.registerNode(REGISTRATION_REQUEST)).thenThrow(failure);
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When & Then
        assertThatThrownBy(() -> client.registerNode(REGISTRATION_REQUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gateway node registration request failed")
                .hasCause(failure);
    }

    @Test
    void shouldReturnLatestPublishedConfiguration() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        GatewayConfigurationSnapshot snapshot = snapshot();
        when(httpApi.findLatestPublishedConfiguration("default"))
                .thenReturn(ResponseEntity.ok(Result.success(snapshot)));
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When
        Optional<GatewayConfigurationSnapshot> result = client.findLatestPublishedConfiguration(" default ");

        // Then
        assertThat(result).contains(snapshot);
        verify(httpApi).findLatestPublishedConfiguration("default");
    }

    @Test
    void shouldReturnEmptyWhenNoConfigurationHasBeenPublished() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        when(httpApi.findLatestPublishedConfiguration("default")).thenReturn(ResponseEntity.noContent().build());
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When
        Optional<GatewayConfigurationSnapshot> result = client.findLatestPublishedConfiguration("default");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectFailedConfigurationEnvelope() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        Result<GatewayConfigurationSnapshot> failure = new Result<>(
                false, "CONFIGURATION_UNAVAILABLE", "Configuration unavailable", snapshot());
        when(httpApi.findLatestPublishedConfiguration("default"))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(failure));
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When & Then
        assertThatThrownBy(() -> client.findLatestPublishedConfiguration("default"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gateway configuration query failed with status 503 and code CONFIGURATION_UNAVAILABLE");
    }

    @Test
    void shouldWrapConfigurationTransportFailure() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        ResourceAccessException failure = new ResourceAccessException("connection refused");
        when(httpApi.findLatestPublishedConfiguration("default")).thenThrow(failure);
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When & Then
        assertThatThrownBy(() -> client.findLatestPublishedConfiguration("default"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gateway configuration query request failed")
                .hasCause(failure);
    }

    @Test
    void shouldRejectInvalidArguments() {
        // Given
        ControlPlaneHttpApi httpApi = mock(ControlPlaneHttpApi.class);
        HttpGatewayControlPlaneClient client = new HttpGatewayControlPlaneClient(httpApi);

        // When & Then
        assertThatThrownBy(() -> new HttpGatewayControlPlaneClient(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("controlPlaneHttpApi must not be null");
        assertThatThrownBy(() -> client.registerNode(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
        assertThatThrownBy(() -> client.findLatestPublishedConfiguration(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
    }

    private static GatewayConfigurationSnapshot snapshot() {
        return new GatewayConfigurationSnapshot(
                "default", 1, Instant.parse("2026-11-02T00:00:00Z"), "sha256:configuration", List.of());
    }
}
