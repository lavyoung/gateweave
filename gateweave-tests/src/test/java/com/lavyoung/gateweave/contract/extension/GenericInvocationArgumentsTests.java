package com.lavyoung.gateweave.contract.extension;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericInvocationArgumentsTests {

    @Test
    void shouldCreateImmutableListSnapshots() {
        // Given
        var parameterTypes = new ArrayList<>(List.of("java.lang.String"));
        var arguments = new ArrayList<>(List.of((Object) "world"));

        // When
        var invocationArguments = new GenericInvocationArguments(parameterTypes, arguments);
        parameterTypes.set(0, "java.lang.Long");
        arguments.set(0, "changed");

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly("java.lang.String");
        assertThat(invocationArguments.arguments()).containsExactly("world");
        assertThatThrownBy(() -> invocationArguments.parameterTypes().add("java.lang.Long"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> invocationArguments.arguments().add("changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowNullArgumentValue() {
        // Given
        var arguments = new ArrayList<>();
        arguments.add(null);

        // When
        var invocationArguments = new GenericInvocationArguments(List.of("java.lang.String"), arguments);

        // Then
        assertThat(invocationArguments.arguments()).hasSize(1).first().isNull();
    }

    @Test
    void shouldRejectNullLists() {
        // When & Then
        assertThatThrownBy(() -> new GenericInvocationArguments(null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameterTypes must not be null");
        assertThatThrownBy(() -> new GenericInvocationArguments(List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("arguments must not be null");
    }

    @Test
    void shouldRejectNullOrBlankParameterType() {
        // Given
        var parameterTypesWithNull = new ArrayList<String>();
        parameterTypesWithNull.add(null);

        // When & Then
        assertThatThrownBy(() -> new GenericInvocationArguments(parameterTypesWithNull, List.of("world")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes must not contain null or blank values");
        assertThatThrownBy(() -> new GenericInvocationArguments(List.of(" "), List.of("world")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes must not contain null or blank values");
    }

    @Test
    void shouldRejectDifferentTypeAndArgumentCounts() {
        // When & Then
        assertThatThrownBy(() -> new GenericInvocationArguments(List.of("java.lang.String"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameterTypes and arguments must have the same size");
    }
}
