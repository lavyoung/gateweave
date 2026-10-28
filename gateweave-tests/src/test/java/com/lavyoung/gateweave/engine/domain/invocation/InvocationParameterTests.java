package com.lavyoung.gateweave.engine.domain.invocation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvocationParameterTests {

    @Test
    void shouldDescribeNamedParameterType() {
        // When
        var parameter = new InvocationParameter("userId", "java.lang.String");

        // Then
        assertThat(parameter.name()).isEqualTo("userId");
        assertThat(parameter.typeName()).isEqualTo("java.lang.String");
    }

    @Test
    void shouldRejectNullNameOrTypeName() {
        // When & Then
        assertThatThrownBy(() -> new InvocationParameter(null, "java.lang.String"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name must not be null");
        assertThatThrownBy(() -> new InvocationParameter("userId", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("typeName must not be null");
    }

    @Test
    void shouldRejectBlankNameOrTypeName() {
        // When & Then
        assertThatThrownBy(() -> new InvocationParameter(" ", "java.lang.String"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> new InvocationParameter("userId", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("typeName must not be blank");
    }
}
