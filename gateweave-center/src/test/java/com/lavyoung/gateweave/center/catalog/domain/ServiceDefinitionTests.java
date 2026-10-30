package com.lavyoung.gateweave.center.catalog.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDefinitionTests {

    private static final String SERVICE_ID = "activity-service";
    private static final String INTERFACE_NAME = "cn.example.ActivityBooth";

    @Test
    void shouldCreateServiceWithImmutableOperationSnapshot() {
        // Given
        ServiceOperation sayHi = operation("activity-say-hi", SERVICE_ID, INTERFACE_NAME, "sayHi");
        ServiceOperation create = operation("activity-create", SERVICE_ID, INTERFACE_NAME, "create");
        List<ServiceOperation> source = new ArrayList<>(List.of(sayHi));

        // When
        ServiceDefinition service = new ServiceDefinition(SERVICE_ID, "activity-application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 3L, source);
        source.add(create);

        // Then
        assertThat(service.operations()).containsExactly(sayHi);
        assertThatThrownBy(() -> service.operations().add(create))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(service.revision()).isEqualTo(3L);
    }

    @Test
    void shouldAllowEmptyNamespaceVersionAndOperations() {
        // When
        ServiceDefinition service = new ServiceDefinition(SERVICE_ID, "activity-application", "http", "", "",
                ServiceCatalogStatus.ACTIVE, 0L, List.of());

        // Then
        assertThat(service.namespace()).isEmpty();
        assertThat(service.version()).isEmpty();
        assertThat(service.operations()).isEmpty();
    }

    @Test
    void shouldRejectMissingReferences() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(null, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceId must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, null, "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("applicationName must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", null, "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("protocol must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", null, "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("namespace must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", null,
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("version must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(
                SERVICE_ID, "application", "dubbo", "", "1.0.0", null, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status must not be null");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operations must not be null");
    }

    @Test
    void shouldRejectBlankRequiredValues() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(" ", "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceId must not be blank");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, " ", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("applicationName must not be blank");
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", " ", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must not be blank");
    }

    @Test
    void shouldRejectNegativeRevision() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, -1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revision must not be negative");
    }

    @Test
    void shouldRejectOperationBelongingToAnotherService() {
        // Given
        ServiceOperation foreignOperation = operation(
                "payment-create", "payment-service", "cn.example.PaymentService", "create");

        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of(foreignOperation)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operations must belong to the service");
    }

    @Test
    void shouldRejectDuplicatedOperationIds() {
        // Given
        ServiceOperation first = operation("activity-operation", SERVICE_ID, INTERFACE_NAME, "sayHi");
        ServiceOperation second = operation("activity-operation", SERVICE_ID, INTERFACE_NAME, "create");

        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationIds must not be duplicated");
    }

    @Test
    void shouldRejectDuplicatedOperationDefinitions() {
        // Given
        ServiceOperation first = operation("activity-say-hi-v1", SERVICE_ID, INTERFACE_NAME, "sayHi");
        ServiceOperation second = operation("activity-say-hi-v2", SERVICE_ID, INTERFACE_NAME, "sayHi");

        // When & Then
        assertThatThrownBy(() -> new ServiceDefinition(SERVICE_ID, "application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0L, List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operation definitions must not be duplicated");
    }

    private static ServiceOperation operation(
            String operationId, String serviceId, String interfaceName, String operationName) {
        return new ServiceOperation(operationId, serviceId, interfaceName, operationName,
                ServiceCatalogStatus.ACTIVE, 0L, List.of());
    }
}
