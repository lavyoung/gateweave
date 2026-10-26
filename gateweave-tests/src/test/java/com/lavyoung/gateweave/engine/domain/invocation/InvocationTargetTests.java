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
        var parameterTypes = new ArrayList<>(List.of("java.lang.String"));
        var attributes = new HashMap<>(Map.of("version", "1.0.0"));

        // When
        var target = new InvocationTarget("DUBBO", "cn.example.ActivityBooth", "sayHi", parameterTypes,
                Duration.ofSeconds(3), attributes);
        parameterTypes.add("java.lang.Integer");
        attributes.put("group", "changed");

        // Then
        assertThat(target.protocol()).isEqualTo("dubbo");
        assertThat(target.parameterTypes()).containsExactly("java.lang.String");
        assertThat(target.attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
        assertThatThrownBy(() -> target.parameterTypes().add("java.lang.Long"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> target.attributes().put("group", "default"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectInvalidInvocationTarget() {
        // When & Then
        assertThatThrownBy(() -> new InvocationTarget(" ", "service", "operation", List.of(),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
        assertThatThrownBy(() -> new InvocationTarget("dubbo", "service", "operation", List.of(" "),
                Duration.ofSeconds(3), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes must not contain blank values");
        assertThatThrownBy(() -> new InvocationTarget("dubbo", "service", "operation", List.of(),
                Duration.ZERO, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout must be greater than zero");
    }
}
