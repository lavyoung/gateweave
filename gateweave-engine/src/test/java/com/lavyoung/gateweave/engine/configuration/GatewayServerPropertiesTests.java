package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayServerPropertiesTests {

    @Test
    void shouldBindDefaultApplicationYaml() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.server", GatewayServerProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.server properties must be present"));

        // Then
        assertThat(properties).isEqualTo(new GatewayServerProperties("0.0.0.0", 7397, 1_048_576, 1, 4, 1024));
    }

    @Test
    void shouldOverrideApplicationYamlWithEnvironmentVariables() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var overrides = new MapPropertySource("serverOverrides", Map.of(
                "GATEWEAVE_SERVER_HOST", "127.0.0.1",
                "GATEWEAVE_SERVER_PORT", "8397",
                "GATEWEAVE_SERVER_MAX_CONTENT_LENGTH", "2097152",
                "GATEWEAVE_SERVER_BOSS_THREADS", "2",
                "GATEWEAVE_SERVER_WORKER_THREADS", "8",
                "GATEWEAVE_SERVER_CONNECTION_BACKLOG", "2048"));
        environment.getPropertySources().addFirst(overrides);
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.server", GatewayServerProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.server properties must be present"));

        // Then
        assertThat(properties).isEqualTo(new GatewayServerProperties("127.0.0.1", 8397, 2_097_152, 2, 8, 2048));
    }

    @Test
    void shouldCreateValidServerProperties() {
        // When
        var properties = new GatewayServerProperties("0.0.0.0", 7397, 1_048_576, 1, 4, 1024);

        // Then
        assertThat(properties.host()).isEqualTo("0.0.0.0");
        assertThat(properties.port()).isEqualTo(7397);
        assertThat(properties.maxContentLength()).isEqualTo(1_048_576);
        assertThat(properties.bossThreads()).isEqualTo(1);
        assertThat(properties.workerThreads()).isEqualTo(4);
        assertThat(properties.connectionBacklog()).isEqualTo(1024);
    }

    @Test
    void shouldRejectMissingOrBlankHost() {
        // When & Then
        assertThatThrownBy(() -> new GatewayServerProperties(null, 7397, 1_048_576, 1, 4, 1024))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("host must not be null");
        assertThatThrownBy(() -> new GatewayServerProperties(" ", 7397, 1_048_576, 1, 4, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("host must not be blank");
    }

    @Test
    void shouldRejectPortOutsideTcpRange() {
        // When & Then
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 0, 1_048_576, 1, 4, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("port must be between 1 and 65535");
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 65_536, 1_048_576, 1, 4, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("port must be between 1 and 65535");
    }

    @Test
    void shouldRejectNonPositiveServerLimits() {
        // When & Then
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 7397, 0, 1, 4, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxContentLength must be greater than zero");
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 7397, 1_048_576, 0, 4, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bossThreads must be greater than zero");
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 7397, 1_048_576, 1, 0, 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workerThreads must be greater than zero");
        assertThatThrownBy(() -> new GatewayServerProperties("0.0.0.0", 7397, 1_048_576, 1, 4, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectionBacklog must be greater than zero");
    }
}
