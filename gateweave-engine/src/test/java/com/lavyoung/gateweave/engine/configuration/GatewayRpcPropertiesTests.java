package com.lavyoung.gateweave.engine.configuration;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.plugin.invoker.rpc.DubboDiscoveryMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRpcPropertiesTests {

    @Test
    void shouldCreateRpcProperties() {
        // When
        var properties = new GatewayRpcProperties(
                "gateweave-engine", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE);

        // Then
        assertThat(properties.applicationName()).isEqualTo("gateweave-engine");
        assertThat(properties.registryAddress()).isEqualTo("zookeeper://127.0.0.1:2181");
        assertThat(properties.discoveryMode()).isEqualTo(DubboDiscoveryMode.INTERFACE);
    }

    @Test
    void shouldRejectMissingValues() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRpcProperties(
                null, "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("applicationName must not be null");
        assertThatThrownBy(() -> new GatewayRpcProperties("gateweave-engine", null, DubboDiscoveryMode.INTERFACE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("registryAddress must not be null");
        assertThatThrownBy(() -> new GatewayRpcProperties("gateweave-engine", "zookeeper://127.0.0.1:2181", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("discoveryMode must not be null");
    }

    @Test
    void shouldRejectBlankValues() {
        // When & Then
        assertThatThrownBy(() -> new GatewayRpcProperties(
                " ", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("applicationName must not be blank");
        assertThatThrownBy(() -> new GatewayRpcProperties("gateweave-engine", " ", DubboDiscoveryMode.INTERFACE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("registryAddress must not be blank");
    }
}
