package com.lavyoung.gateweave.starter.metadata;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceMetadataTests {

    @Test
    void shouldNormalizeServiceAndCopyOperations() {
        // Given
        List<ServiceOperationMetadata> operations = new ArrayList<>();
        operations.add(operation("find-user", "findUser"));

        // When
        ServiceMetadata metadata =
                new ServiceMetadata(" user-service ", " user-application ", " dubbo ", " production ", " 1.0.0 ",
                        operations);
        operations.clear();

        // Then
        assertThat(metadata.serviceId()).isEqualTo("user-service");
        assertThat(metadata.applicationName()).isEqualTo("user-application");
        assertThat(metadata.protocol()).isEqualTo("dubbo");
        assertThat(metadata.namespace()).isEqualTo("production");
        assertThat(metadata.version()).isEqualTo("1.0.0");
        assertThat(metadata.operations()).hasSize(1);
        assertThatThrownBy(() -> metadata.operations().add(operation("create-user", "createUser"))).isInstanceOf(
                UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectDuplicatedOperationIds() {
        // Given
        ServiceOperationMetadata first = operation("find-user", "findUser");
        ServiceOperationMetadata second = operation("find-user", "createUser");

        // When & Then
        assertThatThrownBy(() -> new ServiceMetadata("user-service", "user-application", "dubbo", "", "",
                List.of(first, second))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationIds must not be duplicated");
    }

    @Test
    void shouldRejectDuplicatedOperationDefinitions() {
        // Given
        ServiceOperationMetadata first = operation("find-user-by-id", "findUser");
        ServiceOperationMetadata second = operation("find-user-by-name", "findUser");

        // When & Then
        assertThatThrownBy(() -> new ServiceMetadata("user-service", "user-application", "dubbo", "", "",
                List.of(first, second))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operation definitions must not be duplicated");
    }

    @Test
    void shouldRejectInvalidServiceIdentity() {
        // When & Then
        assertThatThrownBy(() -> new ServiceMetadata(" ", "user-application", "dubbo", "", "", List.of())).isInstanceOf(
                IllegalArgumentException.class).hasMessage("serviceId must not be blank");
        assertThatThrownBy(() -> new ServiceMetadata("user-service", " ", "dubbo", "", "", List.of())).isInstanceOf(
                IllegalArgumentException.class).hasMessage("applicationName must not be blank");
        assertThatThrownBy(
                () -> new ServiceMetadata("user-service", "user-application", " ", "", "", List.of())).isInstanceOf(
                IllegalArgumentException.class).hasMessage("protocol must not be blank");
    }

    private static ServiceOperationMetadata operation(String operationId, String operationName) {
        return new ServiceOperationMetadata(operationId, "com.example.UserService", operationName, List.of());
    }
}
