package com.lavyoung.gateweave.center.catalog.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceOperationTests {

    private static final ServiceOperationParameter NAME_PARAMETER =
            new ServiceOperationParameter(0, "name", "java.lang.String");
    private static final ServiceOperationParameter AGE_PARAMETER =
            new ServiceOperationParameter(1, "age", "java.lang.Integer");

    @Test
    void shouldCreateOperationWithImmutableParameterSnapshot() {
        // Given
        List<ServiceOperationParameter> source = new ArrayList<>(List.of(NAME_PARAMETER));

        // When
        ServiceOperation operation = new ServiceOperation("activity-say-hi", "activity-service",
                "cn.example.ActivityBooth", "sayHi", ServiceCatalogStatus.ACTIVE, 2L, source);
        source.add(AGE_PARAMETER);

        // Then
        assertThat(operation.parameters()).containsExactly(NAME_PARAMETER);
        assertThatThrownBy(() -> operation.parameters().add(AGE_PARAMETER))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(operation.revision()).isEqualTo(2L);
    }

    @Test
    void shouldAllowOperationWithoutParameters() {
        // When
        ServiceOperation operation = new ServiceOperation("activity-health", "activity-service",
                "cn.example.ActivityBooth", "health", ServiceCatalogStatus.ACTIVE, 0L, List.of());

        // Then
        assertThat(operation.parameters()).isEmpty();
    }

    @Test
    void shouldRejectMissingReferences() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperation(null, "service", "interface", "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operationId must not be null");
        assertThatThrownBy(() -> new ServiceOperation("operation", null, "interface", "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceId must not be null");
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", null, "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("interfaceName must not be null");
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", "interface", null,
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operationName must not be null");
        assertThatThrownBy(() -> new ServiceOperation(
                "operation", "service", "interface", "operation", null, 0L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status must not be null");
        assertThatThrownBy(() -> new ServiceOperation(
                "operation", "service", "interface", "operation", ServiceCatalogStatus.ACTIVE, 0L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameters must not be null");
    }

    @Test
    void shouldRejectBlankIdentifiersAndNames() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperation(" ", "service", "interface", "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationId must not be blank");
        assertThatThrownBy(() -> new ServiceOperation("operation", " ", "interface", "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceId must not be blank");
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", " ", "operation",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("interfaceName must not be blank");
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", "interface", " ",
                ServiceCatalogStatus.ACTIVE, 0L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationName must not be blank");
    }

    @Test
    void shouldRejectNegativeRevision() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", "interface", "invoke",
                ServiceCatalogStatus.ACTIVE, -1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revision must not be negative");
    }

    @Test
    void shouldRejectNonContinuousParameterPositions() {
        // Given
        ServiceOperationParameter secondParameter = new ServiceOperationParameter(2, "age", "java.lang.Integer");

        // When & Then
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", "interface", "invoke",
                ServiceCatalogStatus.ACTIVE, 0L, List.of(NAME_PARAMETER, secondParameter)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter positions must be continuous from zero");
    }

    @Test
    void shouldRejectDuplicatedParameterNames() {
        // Given
        ServiceOperationParameter duplicatedName = new ServiceOperationParameter(1, "name", "java.lang.Integer");

        // When & Then
        assertThatThrownBy(() -> new ServiceOperation("operation", "service", "interface", "invoke",
                ServiceCatalogStatus.ACTIVE, 0L, List.of(NAME_PARAMETER, duplicatedName)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter names must not be duplicated");
    }

    @Test
    void shouldExposeStableServiceCatalogStatuses() {
        // When & Then
        assertThat(ServiceCatalogStatus.values())
                .containsExactly(ServiceCatalogStatus.ACTIVE, ServiceCatalogStatus.DISABLED);
    }
}
