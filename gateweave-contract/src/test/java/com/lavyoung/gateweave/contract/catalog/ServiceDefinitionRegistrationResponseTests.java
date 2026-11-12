package com.lavyoung.gateweave.contract.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDefinitionRegistrationResponseTests {

    @Test
    void shouldPreserveRegistrationResult() {
        // When
        ServiceDefinitionRegistrationResponse response =
                new ServiceDefinitionRegistrationResponse("activity-service", CatalogRegistrationOutcome.CREATED);

        // Then
        assertThat(response.serviceId()).isEqualTo("activity-service");
        assertThat(response.outcome()).isEqualTo(CatalogRegistrationOutcome.CREATED);
    }

    @Test
    void shouldRejectMissingServiceId() {
        // When & Then
        assertThatThrownBy(
                () -> new ServiceDefinitionRegistrationResponse(null, CatalogRegistrationOutcome.CREATED))
                .isInstanceOf(NullPointerException.class).hasMessage("serviceId must not be null");
    }

    @Test
    void shouldRejectBlankServiceId() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationResponse(" ", CatalogRegistrationOutcome.CREATED))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("serviceId must not be blank");
    }

    @Test
    void shouldRejectMissingRegistrationOutcome() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationResponse("activity-service", null)).isInstanceOf(
                NullPointerException.class).hasMessage("outcome must not be null");
    }
}
