package com.lavyoung.gateweave.starter.configuration;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GateWeaveRegistrationPropertiesTests {

    @Test
    void shouldBindRegistrationProperties() {
        // Given
        Map<String, Object> source =
                Map.of("gateweave.registration.base-uri", "https://center.example.com/control-plane",
                        "gateweave.registration.connect-timeout", "3s", "gateweave.registration.request-timeout",
                        "8s");

        // When
        GateWeaveRegistrationProperties properties = bind(source);

        // Then
        assertThat(properties.baseUri()).isEqualTo(URI.create("https://center.example.com/control-plane"));
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void shouldApplyDefaultTimeouts() {
        // When
        GateWeaveRegistrationProperties properties =
                bind(Map.of("gateweave.registration.base-uri", "http://127.0.0.1:8080"));

        // Then
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void shouldRejectUnsafeControlPlaneEndpoint() {
        // When & Then
        assertThatThrownBy(() -> properties(URI.create("ftp://center.example.com"), Duration.ofSeconds(2),
                Duration.ofSeconds(5))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> properties(URI.create("http://user:password@center.example.com"),
                Duration.ofSeconds(2), Duration.ofSeconds(5))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must not contain user information");
    }

    @Test
    void shouldRequireEveryConfigurationValue() {
        // When & Then
        assertThatThrownBy(() -> properties(null, Duration.ofSeconds(2), Duration.ofSeconds(5))).isInstanceOf(
                NullPointerException.class).hasMessage("baseUri must not be null");
        assertThatThrownBy(() -> properties(URI.create("http://127.0.0.1:8080"), null, Duration.ofSeconds(5)))
                .isInstanceOf(NullPointerException.class).hasMessage("connectTimeout must not be null");
        assertThatThrownBy(() -> properties(URI.create("http://127.0.0.1:8080"), Duration.ofSeconds(2), null))
                .isInstanceOf(NullPointerException.class).hasMessage("requestTimeout must not be null");
    }

    @Test
    void shouldRejectNonPositiveTimeouts() {
        // Given
        URI baseUri = URI.create("http://127.0.0.1:8080");

        // When & Then
        assertThatThrownBy(() -> properties(baseUri, Duration.ZERO, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectTimeout must be greater than zero");
        assertThatThrownBy(() -> properties(baseUri, Duration.ofSeconds(-1), Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectTimeout must be greater than zero");
        assertThatThrownBy(() -> properties(baseUri, Duration.ofSeconds(2), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeout must be greater than zero");
        assertThatThrownBy(() -> properties(baseUri, Duration.ofSeconds(2), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestTimeout must be greater than zero");
    }

    private static GateWeaveRegistrationProperties bind(Map<String, Object> source) {
        return new Binder(new MapConfigurationPropertySource(source)).bind("gateweave.registration",
                        GateWeaveRegistrationProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.registration properties must be present"));
    }

    private static GateWeaveRegistrationProperties properties(URI baseUri, Duration connectTimeout,
            Duration requestTimeout) {
        return new GateWeaveRegistrationProperties(baseUri, connectTimeout, requestTimeout);
    }
}
