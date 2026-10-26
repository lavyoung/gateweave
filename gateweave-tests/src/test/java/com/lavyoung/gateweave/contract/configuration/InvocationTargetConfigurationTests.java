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
        var parameterTypes = new ArrayList<>(List.of("java.lang.String"));
        var attributes = new HashMap<>(Map.of("version", "1.0.0"));

        // When
        var target = new InvocationTargetConfiguration("dubbo", "cn.example.ActivityBooth", "sayHi",
                parameterTypes, Duration.ofSeconds(3), attributes);
        parameterTypes.add("java.lang.Integer");
        attributes.put("group", "changed");

        // Then
        assertThat(target.protocol()).isEqualTo("dubbo");
        assertThat(target.service()).isEqualTo("cn.example.ActivityBooth");
        assertThat(target.operation()).isEqualTo("sayHi");
        assertThat(target.parameterTypes()).containsExactly("java.lang.String");
        assertThat(target.timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(target.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThatThrownBy(() -> target.parameterTypes().add("java.lang.Long"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> target.attributes().put("group", "default"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectBlankRequiredText() {
        // When & Then
        assertThatThrownBy(() -> new InvocationTargetConfiguration(" ", "service", "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", " ", "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("service must not be blank");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", " ", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operation must not be blank");
    }

    @Test
    void shouldRejectInvalidParameterTypes() {
        // Given
        var parameterTypesContainingNull = new ArrayList<String>();
        parameterTypesContainingNull.add(null);

        // When & Then
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation",
                List.of(" "), Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes must not contain blank values");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation",
                parameterTypesContainingNull, Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        // When & Then
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", List.of(),
                Duration.ZERO, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", List.of(),
                Duration.ofMillis(-1), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
    }

    @Test
    void shouldRejectMissingRequiredProperties() {
        // When & Then
        assertThatThrownBy(() -> new InvocationTargetConfiguration(null, "service", "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("protocol must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", null, "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("service must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", null, List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operation must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", null,
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameterTypes must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", List.of(),
                null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("timeout must not be null");
        assertThatThrownBy(() -> new InvocationTargetConfiguration("dubbo", "service", "operation", List.of(),
                Duration.ofSeconds(3), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
    }
}
