package com.lavyoung.gateweave.contract.configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationTargetConfigurationTests {

    @Test
    void shouldCreateImmutableInvocationTarget() {
        // Given
        var parameters = new ArrayList<>(List.of(parameter("name", "java.lang.String")));
        var attributes = new HashMap<>(Map.of("version", "1.0.0"));

        // When
        var target = new InvocationTargetConfiguration("dubbo", "cn.example.ActivityBooth", "sayHi",
                parameters, Duration.ofSeconds(3), attributes);
        parameters.add(parameter("age", "java.lang.Integer"));
        attributes.put("group", "changed");

        // Then
        assertThat(target.protocol()).isEqualTo("dubbo");
        assertThat(target.service()).isEqualTo("cn.example.ActivityBooth");
        assertThat(target.operation()).isEqualTo("sayHi");
        assertThat(target.parameters()).containsExactly(parameter("name", "java.lang.String"));
        assertThat(target.timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(target.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThatThrownBy(() -> target.parameters().add(parameter("age", "java.lang.Integer")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> target.attributes().put("group", "default"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectDuplicateParameterNames() {
        // Given
        var parameters = List.of(parameter("userId", "java.lang.String"),
                parameter("userId", "java.lang.Long"));

        // When & Then
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", parameters,
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameters must not contain duplicate names");
    }

    @Test
    void shouldRejectBlankRequiredText() {
        // When & Then
        assertThatThrownBy(() -> target(" ", "service", "operation", List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
        assertThatThrownBy(() -> target("dubbo", " ", "operation", List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("service must not be blank");
        assertThatThrownBy(() -> target("dubbo", "service", " ", List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operation must not be blank");
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        // When & Then
        assertThatThrownBy(() -> target("dubbo", "service", "operation", List.of(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
        assertThatThrownBy(() -> target("dubbo", "service", "operation", List.of(), Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
    }

    @Test
    void shouldUseEmptyParametersWhenParametersAreNotConfigured() {
        // When
        var target = target("http", "http://127.0.0.1:8080", "/users", null, Duration.ofSeconds(3));

        // Then
        assertThat(target.parameters()).isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredProperties() {
        // When & Then
        assertThatThrownBy(() -> target(null, "service", "operation", List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("protocol must not be null");
        assertThatThrownBy(() -> target("dubbo", null, "operation", List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("service must not be null");
        assertThatThrownBy(() -> target("dubbo", "service", null, List.of(), Duration.ofSeconds(3)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operation must not be null");
        assertThatThrownBy(() -> target("dubbo", "service", "operation", List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("timeout must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", List.of(),
                Duration.ofSeconds(3), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
    }

    private static InvocationParameterConfiguration parameter(String name, String typeName) {
        return new InvocationParameterConfiguration(name, typeName);
    }

    private static InvocationTargetConfiguration target(String protocol, String service, String operation,
            List<InvocationParameterConfiguration> parameters, Duration timeout) {
        return new InvocationTargetConfiguration(protocol, service, operation, parameters, timeout, Map.of());
    }
}
