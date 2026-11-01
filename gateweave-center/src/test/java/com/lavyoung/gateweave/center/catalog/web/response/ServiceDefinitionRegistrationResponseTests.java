package com.lavyoung.gateweave.center.catalog.web.response;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDefinitionRegistrationResponseTests {

    @Test
    void shouldCreateRegistrationResponse() {
        // When
        ServiceDefinitionRegistrationResponse response = new ServiceDefinitionRegistrationResponse(
                "activity-service", CatalogRegistrationOutcome.CREATED);

        // Then
        assertThat(response.serviceId()).isEqualTo("activity-service");
        assertThat(response.outcome()).isEqualTo(CatalogRegistrationOutcome.CREATED);
    }

    @Test
    void shouldRejectNullServiceId() {
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationResponse(
                null, CatalogRegistrationOutcome.CREATED))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceId must not be null");
    }

    @Test
    void shouldRejectBlankServiceId() {
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationResponse(
                " ", CatalogRegistrationOutcome.CREATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceId must not be blank");
    }

    @Test
    void shouldRejectNullOutcome() {
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationResponse("activity-service", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("outcome must not be null");
    }
}
