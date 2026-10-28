package com.lavyoung.gateweave.contract.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationParameterConfigurationTests {

    @Test
    void shouldDescribeNamedParameterType() {
        // When
        var parameter = new InvocationParameterConfiguration("userId", "java.lang.String");

        // Then
        assertThat(parameter.name()).isEqualTo("userId");
        assertThat(parameter.typeName()).isEqualTo("java.lang.String");
    }

    @Test
    void shouldRejectNullNameOrTypeName() {
        // When & Then
        assertThatThrownBy(() -> new InvocationParameterConfiguration(null, "java.lang.String"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name must not be null");
        assertThatThrownBy(() -> new InvocationParameterConfiguration("userId", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("typeName must not be null");
    }

    @Test
    void shouldRejectBlankNameOrTypeName() {
        // When & Then
        assertThatThrownBy(() -> new InvocationParameterConfiguration(" ", "java.lang.String"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> new InvocationParameterConfiguration("userId", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("typeName must not be blank");
    }
}
