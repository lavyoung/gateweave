package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayHttpPropertiesTests {

    @Test
    void shouldBindDefaultApplicationYaml() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.http", GatewayHttpProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.http properties must be present"));

        // Then
        assertThat(properties).isEqualTo(
                new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 256, Duration.ofSeconds(30)));
    }

    @Test
    void shouldCreateValidHttpProperties() {
        // When
        var properties = new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 256, Duration.ofSeconds(30));

        // Then
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.coreThreads()).isEqualTo(2);
        assertThat(properties.maximumThreads()).isEqualTo(8);
        assertThat(properties.queueCapacity()).isEqualTo(256);
        assertThat(properties.keepAlive()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldRejectMissingDurations() {
        // When & Then
        assertThatThrownBy(() -> new GatewayHttpProperties(null, 2, 8, 256, Duration.ofSeconds(30)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("connectTimeout must not be null");
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 256, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("keepAlive must not be null");
    }

    @Test
    void shouldRejectNonPositiveConnectTimeout() {
        // When & Then
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ZERO, 2, 8, 256, Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectTimeout must be greater than zero");
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(-1), 2, 8, 256,
                Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectTimeout must be greater than zero");
    }

    @Test
    void shouldRejectInvalidThreadCounts() {
        // When & Then
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 0, 8, 256,
                Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("coreThreads must be greater than zero");
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 4, 3, 256,
                Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumThreads must be greater than or equal to coreThreads");
    }

    @Test
    void shouldRejectInvalidQueueCapacityOrKeepAlive() {
        // When & Then
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 0,
                Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queueCapacity must be greater than zero");
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 256, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("keepAlive must be greater than zero");
        assertThatThrownBy(() -> new GatewayHttpProperties(Duration.ofSeconds(2), 2, 8, 256,
                Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("keepAlive must be greater than zero");
    }
}
