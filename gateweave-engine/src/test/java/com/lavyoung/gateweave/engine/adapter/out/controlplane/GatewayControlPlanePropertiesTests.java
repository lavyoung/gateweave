package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayControlPlanePropertiesTests {

    @Test
    void shouldBindControlPlaneProperties() {
        // Given
        Map<String, Object> source = Map.of(
                "gateweave.control-plane.base-uri", "http://127.0.0.1:8080",
                "gateweave.control-plane.node-id", "node-1",
                "gateweave.control-plane.gateway-group-id", "default",
                "gateweave.control-plane.node-name", "Gateway Node 1",
                "gateweave.control-plane.advertised-address", "http://127.0.0.1:7397",
                "gateweave.control-plane.request-timeout", "3s");

        // When
        GatewayControlPlaneProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("gateweave.control-plane", GatewayControlPlaneProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.control-plane properties must be present"));

        // Then
        assertThat(properties.baseUri()).isEqualTo(URI.create("http://127.0.0.1:8080"));
        assertThat(properties.nodeId()).isEqualTo("node-1");
        assertThat(properties.gatewayGroupId()).isEqualTo("default");
        assertThat(properties.nodeName()).isEqualTo("Gateway Node 1");
        assertThat(properties.advertisedAddress()).isEqualTo(URI.create("http://127.0.0.1:7397"));
        assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void shouldRejectInvalidControlPlaneBaseUri() {
        // When & Then
        assertThatThrownBy(() -> properties(URI.create("/relative")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseUri must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> properties(URI.create("ftp://127.0.0.1:8080")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseUri must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> properties(URI.create("http://user:password@127.0.0.1:8080")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseUri must not contain user information");
        assertThatThrownBy(() -> properties(URI.create("http://127.0.0.1:8080?env=test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseUri must not contain query or fragment");
    }

    @Test
    void shouldRequireEveryConfigurationValue() {
        // When & Then
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                null, "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"),
                Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("baseUri must not be null");
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), null, "default", "Gateway Node 1",
                URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeId must not be null");
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), "node-1", null, "Gateway Node 1",
                URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), "node-1", "default", null,
                URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeName must not be null");
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), "node-1", "default", "Gateway Node 1", null,
                Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("advertisedAddress must not be null");
        assertThatThrownBy(() -> new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), "node-1", "default", "Gateway Node 1",
                URI.create("http://127.0.0.1:7397"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestTimeout must not be null");
    }

    @Test
    void shouldRejectNonPositiveRequestTimeout() {
        // When & Then
        assertThatThrownBy(() -> properties(URI.create("http://127.0.0.1:8080"), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeout must be greater than zero");
        assertThatThrownBy(() -> properties(URI.create("http://127.0.0.1:8080"), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeout must be greater than zero");
    }

    @Test
    void shouldLeaveNodeRegistrationRulesToControlPlaneDomain() {
        // When
        GatewayControlPlaneProperties properties = new GatewayControlPlaneProperties(
                URI.create("http://127.0.0.1:8080"), "n".repeat(65), "g".repeat(65), "name".repeat(40),
                URI.create("ftp://127.0.0.1:7397"), Duration.ofSeconds(3));

        // Then
        assertThat(properties.nodeId()).hasSize(65);
        assertThat(properties.gatewayGroupId()).hasSize(65);
        assertThat(properties.nodeName()).hasSize(160);
        assertThat(properties.advertisedAddress()).hasScheme("ftp");
    }

    private static GatewayControlPlaneProperties properties(URI baseUri) {
        return properties(baseUri, Duration.ofSeconds(3));
    }

    private static GatewayControlPlaneProperties properties(URI baseUri, Duration requestTimeout) {
        return new GatewayControlPlaneProperties(
                baseUri, "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"),
                requestTimeout);
    }
}
