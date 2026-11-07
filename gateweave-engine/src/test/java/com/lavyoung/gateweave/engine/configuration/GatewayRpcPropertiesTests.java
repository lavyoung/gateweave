package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import com.lavyoung.gateweave.plugin.invoker.rpc.DubboDiscoveryMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRpcPropertiesTests {

    @Test
    void shouldBindDefaultApplicationYaml() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.rpc", GatewayRpcProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.rpc properties must be present"));

        // Then
        assertThat(properties).isEqualTo(new GatewayRpcProperties(
                "gateweave-engine", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE));
    }

    @Test
    void shouldOverrideApplicationYamlWithEnvironmentVariable() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var overrides = new MapPropertySource("rpcOverrides", Map.of(
                "GATEWEAVE_RPC_REGISTRY_ADDRESS", "zookeeper://zookeeper:2181"));
        environment.getPropertySources().addFirst(overrides);
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.rpc", GatewayRpcProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.rpc properties must be present"));

        // Then
        assertThat(properties).isEqualTo(new GatewayRpcProperties(
                "gateweave-engine", "zookeeper://zookeeper:2181", DubboDiscoveryMode.INTERFACE));
    }

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
