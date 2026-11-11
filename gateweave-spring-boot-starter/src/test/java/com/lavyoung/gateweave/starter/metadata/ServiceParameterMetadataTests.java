package com.lavyoung.gateweave.starter.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceParameterMetadataTests {

    @Test
    void shouldNormalizeParameterMetadata() {
        // When
        ServiceParameterMetadata metadata = new ServiceParameterMetadata(0, " userId ", " java.lang.String ");

        // Then
        assertThat(metadata.position()).isZero();
        assertThat(metadata.name()).isEqualTo("userId");
        assertThat(metadata.typeName()).isEqualTo("java.lang.String");
    }

    @Test
    void shouldRejectInvalidPositionAndText() {
        // When & Then
        assertThatThrownBy(() -> new ServiceParameterMetadata(-1, "userId", "java.lang.String")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("position must not be negative");
        assertThatThrownBy(() -> new ServiceParameterMetadata(0, " ", "java.lang.String")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("name must not be blank");
        assertThatThrownBy(() -> new ServiceParameterMetadata(0, "userId", " ")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("typeName must not be blank");
    }

    @Test
    void shouldRejectMissingText() {
        // When & Then
        assertThatThrownBy(() -> new ServiceParameterMetadata(0, null, "java.lang.String")).isInstanceOf(
                NullPointerException.class).hasMessage("name must not be null");
        assertThatThrownBy(() -> new ServiceParameterMetadata(0, "userId", null)).isInstanceOf(
                NullPointerException.class).hasMessage("typeName must not be null");
    }
}
