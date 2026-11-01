package com.lavyoung.gateweave.center.catalog.application;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogStatus;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperation;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperationParameter;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogRepository;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.error.CatalogResponseCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultServiceDefinitionRegistrationServiceTests {

    @Test
    void shouldReturnCreatedOutcomeForNewServiceDefinition() {
        // Given
        ServiceCatalogRepository repository = mock(ServiceCatalogRepository.class);
        DefaultServiceDefinitionRegistrationService service =
                new DefaultServiceDefinitionRegistrationService(repository);
        ServiceDefinition definition = serviceDefinition();
        when(repository.register(definition)).thenReturn(CatalogRegistrationOutcome.CREATED);

        // When
        CatalogRegistrationOutcome outcome = service.register(definition);

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.CREATED);
        verify(repository).register(definition);
    }

    @Test
    void shouldReturnUnchangedOutcomeForIdenticalServiceDefinition() {
        // Given
        ServiceCatalogRepository repository = mock(ServiceCatalogRepository.class);
        DefaultServiceDefinitionRegistrationService service =
                new DefaultServiceDefinitionRegistrationService(repository);
        ServiceDefinition definition = serviceDefinition();
        when(repository.register(definition)).thenReturn(CatalogRegistrationOutcome.UNCHANGED);

        // When
        CatalogRegistrationOutcome outcome = service.register(definition);

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.UNCHANGED);
        verify(repository).register(definition);
    }

    @Test
    void shouldPropagateBusinessExceptionWithoutWrapping() {
        // Given
        ServiceCatalogRepository repository = mock(ServiceCatalogRepository.class);
        DefaultServiceDefinitionRegistrationService service =
                new DefaultServiceDefinitionRegistrationService(repository);
        ServiceDefinition definition = serviceDefinition();
        BusinessException failure = new BusinessException(
                CatalogResponseCode.SERVICE_DEFINITION_CONFLICT, definition.serviceId());
        when(repository.register(definition)).thenThrow(failure);

        // When & Then
        assertThatThrownBy(() -> service.register(definition)).isSameAs(failure);
    }

    @Test
    void shouldRejectMissingRepository() {
        // When & Then
        assertThatThrownBy(() -> new DefaultServiceDefinitionRegistrationService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceCatalogRepository must not be null");
    }

    @Test
    void shouldRejectMissingServiceDefinitionBeforeCallingRepository() {
        // Given
        ServiceCatalogRepository repository = mock(ServiceCatalogRepository.class);
        DefaultServiceDefinitionRegistrationService service =
                new DefaultServiceDefinitionRegistrationService(repository);

        // When & Then
        assertThatThrownBy(() -> service.register(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceDefinition must not be null");
        verifyNoInteractions(repository);
    }

    private static ServiceDefinition serviceDefinition() {
        ServiceOperationParameter parameter = new ServiceOperationParameter(0, "name", "java.lang.String");
        ServiceOperation operation = new ServiceOperation(
                "operation-1", "service-1", "com.example.GreetingService", "sayHello",
                ServiceCatalogStatus.ACTIVE, 0, List.of(parameter));
        return new ServiceDefinition(
                "service-1", "greeting-application", "dubbo", "", "1.0.0",
                ServiceCatalogStatus.ACTIVE, 0, List.of(operation));
    }
}
