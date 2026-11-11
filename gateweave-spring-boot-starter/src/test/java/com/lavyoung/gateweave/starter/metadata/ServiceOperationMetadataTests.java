package com.lavyoung.gateweave.starter.metadata;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceOperationMetadataTests {

    @Test
    void shouldNormalizeOperationAndCopyParameters() {
        // Given
        List<ServiceParameterMetadata> parameters = new ArrayList<>();
        parameters.add(new ServiceParameterMetadata(0, "userId", "java.lang.String"));

        // When
        ServiceOperationMetadata metadata =
                new ServiceOperationMetadata(" find-user ", " com.example.UserService ", " findUser ", parameters);
        parameters.clear();

        // Then
        assertThat(metadata.operationId()).isEqualTo("find-user");
        assertThat(metadata.interfaceName()).isEqualTo("com.example.UserService");
        assertThat(metadata.operationName()).isEqualTo("findUser");
        assertThat(metadata.parameters()).hasSize(1);
        assertThatThrownBy(() -> metadata.parameters()
                .add(new ServiceParameterMetadata(1, "tenantId", "java.lang.String"))).isInstanceOf(
                UnsupportedOperationException.class);
    }

    @Test
    void shouldAcceptOperationWithoutParameters() {
        // When
        ServiceOperationMetadata metadata =
                new ServiceOperationMetadata("health-check", "com.example.HealthService", "health", List.of());

        // Then
        assertThat(metadata.parameters()).isEmpty();
    }

    @Test
    void shouldRejectDiscontinuousPositionsAndDuplicatedNames() {
        // Given
        ServiceParameterMetadata first = new ServiceParameterMetadata(0, "userId", "java.lang.String");
        ServiceParameterMetadata misplaced = new ServiceParameterMetadata(2, "tenantId", "java.lang.String");
        ServiceParameterMetadata duplicated = new ServiceParameterMetadata(1, "userId", "java.lang.Long");

        // When & Then
        assertThatThrownBy(() -> new ServiceOperationMetadata("find-user", "com.example.UserService", "findUser",
                List.of(first, misplaced))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter positions must be continuous from zero");
        assertThatThrownBy(() -> new ServiceOperationMetadata("find-user", "com.example.UserService", "findUser",
                List.of(first, duplicated))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter names must not be duplicated");
    }

    @Test
    void shouldRejectInvalidOperationIdentity() {
        // When & Then
        assertThatThrownBy(
                () -> new ServiceOperationMetadata(" ", "com.example.UserService", "findUser", List.of())).isInstanceOf(
                IllegalArgumentException.class).hasMessage("operationId must not be blank");
        assertThatThrownBy(() -> new ServiceOperationMetadata("find-user", " ", "findUser", List.of())).isInstanceOf(
                IllegalArgumentException.class).hasMessage("interfaceName must not be blank");
        assertThatThrownBy(() -> new ServiceOperationMetadata("find-user", "com.example.UserService", " ",
                List.of())).isInstanceOf(IllegalArgumentException.class).hasMessage("operationName must not be blank");
    }
}
