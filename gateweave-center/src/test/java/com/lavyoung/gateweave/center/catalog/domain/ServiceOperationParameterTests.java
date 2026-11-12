package com.lavyoung.gateweave.center.catalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceOperationParameterTests {

    @Test
    void shouldCreateOrderedServiceOperationParameter() {
        // Given & When
        ServiceOperationParameter parameter = new ServiceOperationParameter(0, "name", "java.lang.String");

        // Then
        assertThat(parameter.position()).isZero();
        assertThat(parameter.name()).isEqualTo("name");
        assertThat(parameter.typeName()).isEqualTo("java.lang.String");
    }

    @Test
    void shouldRejectNegativePosition() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperationParameter(-1, "name", "java.lang.String")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("position must not be negative");
    }

    @Test
    void shouldRejectMissingName() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperationParameter(0, null, "java.lang.String")).isInstanceOf(
                NullPointerException.class).hasMessage("name must not be null");
        assertThatThrownBy(() -> new ServiceOperationParameter(0, " ", "java.lang.String")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("name must not be blank");
    }

    @Test
    void shouldRejectMissingTypeName() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperationParameter(0, "name", null)).isInstanceOf(
                NullPointerException.class).hasMessage("typeName must not be null");
        assertThatThrownBy(() -> new ServiceOperationParameter(0, "name", " ")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("typeName must not be blank");
    }

    @Test
    void shouldAllowMaximumFieldLengths() {
        // When
        ServiceOperationParameter parameter =
                new ServiceOperationParameter(0, "n".repeat(128), "t".repeat(256));

        // Then
        assertThat(parameter.name()).hasSize(128);
        assertThat(parameter.typeName()).hasSize(256);
    }

    @Test
    void shouldRejectValuesExceedingStorageLengths() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperationParameter(0, "n".repeat(129), "java.lang.String"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not exceed 128 characters");
        assertThatThrownBy(() -> new ServiceOperationParameter(0, "name", "t".repeat(257)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("typeName must not exceed 256 characters");
    }
}
