package com.lavyoung.gateweave.engine.domain.invocation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationTargetTests {

    @Test
    void shouldNormalizeProtocolAndCreateImmutableSnapshot() {
        // Given
        var parameters = new ArrayList<>(List.of(parameter("name", "java.lang.String")));
        var attributes = new HashMap<>(Map.of("version", "1.0.0"));

        // When
        var target = new InvocationTarget("DUBBO", "cn.example.ActivityBooth", "sayHi", parameters,
                Duration.ofSeconds(3), attributes);
        parameters.add(parameter("age", "java.lang.Integer"));
        attributes.put("group", "changed");

        // Then
        assertThat(target.protocol()).isEqualTo("dubbo");
        assertThat(target.parameters()).containsExactly(parameter("name", "java.lang.String"));
        assertThat(target.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThatThrownBy(() -> target.parameters().add(parameter("age", "java.lang.Integer")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> target.attributes().put("group", "default"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectDuplicateParameterNames() {
        // Given
        var parameters = List.of(parameter("userId", "java.lang.String"), parameter("userId", "java.lang.Long"));

        // When & Then
        assertThatThrownBy(() -> new InvocationTarget("dubbo", "service", "operation", parameters,
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameters must not contain duplicate names");
    }

    @Test
    void shouldRejectInvalidInvocationTarget() {
        // When & Then
        assertThatThrownBy(() -> new InvocationTarget(" ", "service", "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
        assertThatThrownBy(() -> new InvocationTarget("dubbo", "service", "operation", List.of(),
                Duration.ZERO, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
    }

    private static InvocationParameter parameter(String name, String typeName) {
        return new InvocationParameter(name, typeName);
    }
}
