package com.lavyoung.gateweave.starter.registration;

import java.util.List;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServiceRegistrationRunnerTests {

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ServiceRegistrationClient registrationClient = mock(ServiceRegistrationClient.class);

        // When & Then
        assertThatThrownBy(() -> new ServiceRegistrationRunner(null, registrationClient))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("metadataRegistry must not be null");
        assertThatThrownBy(() -> new ServiceRegistrationRunner(registry, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("registrationClient must not be null");
    }

    @Test
    void shouldSkipRegistrationWhenNoServiceMetadataWasCollected() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ServiceRegistrationClient registrationClient = mock(ServiceRegistrationClient.class);
        ServiceRegistrationRunner runner = new ServiceRegistrationRunner(registry, registrationClient);

        // When
        runner.run(mock(ApplicationArguments.class));

        // Then
        verifyNoInteractions(registrationClient);
    }

    @Test
    void shouldRegisterCollectedServicesInStableOrder() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ServiceMetadata payment = metadata("payment-service");
        ServiceMetadata activity = metadata("activity-service");
        registry.accept(payment);
        registry.accept(activity);
        ServiceRegistrationClient registrationClient = mock(ServiceRegistrationClient.class);
        when(registrationClient.register(any(ServiceMetadata.class)))
                .thenReturn(CatalogRegistrationOutcome.CREATED, CatalogRegistrationOutcome.UNCHANGED);
        ServiceRegistrationRunner runner = new ServiceRegistrationRunner(registry, registrationClient);

        // When
        runner.run(mock(ApplicationArguments.class));

        // Then
        InOrder registrationOrder = inOrder(registrationClient);
        registrationOrder.verify(registrationClient).register(activity);
        registrationOrder.verify(registrationClient).register(payment);
        registrationOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldPropagateFailureAndStopRegisteringRemainingServices() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ServiceMetadata activity = metadata("activity-service");
        ServiceMetadata payment = metadata("payment-service");
        registry.accept(payment);
        registry.accept(activity);
        ServiceRegistrationClient registrationClient = mock(ServiceRegistrationClient.class);
        IllegalStateException failure = new IllegalStateException("registration failed");
        when(registrationClient.register(activity)).thenThrow(failure);
        ServiceRegistrationRunner runner = new ServiceRegistrationRunner(registry, registrationClient);

        // When & Then
        assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class))).isSameAs(failure);
        verify(registrationClient).register(activity);
        verify(registrationClient, never()).register(payment);
    }

    private static ServiceMetadata metadata(String serviceId) {
        return new ServiceMetadata(serviceId, "test-application", "dubbo", "", "1.0.0", List.of());
    }
}
