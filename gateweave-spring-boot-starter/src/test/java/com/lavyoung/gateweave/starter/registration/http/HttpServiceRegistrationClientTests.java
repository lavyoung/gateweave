package com.lavyoung.gateweave.starter.registration.http;

import java.util.List;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest;
import com.lavyoung.gateweave.contract.catalog.ServiceDefinitionRegistrationResponse;
import com.lavyoung.gateweave.contract.web.Result;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceOperationMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceParameterMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpServiceRegistrationClientTests {

    private final ServiceRegistrationHttpApi httpApi = mock(ServiceRegistrationHttpApi.class);
    private final HttpServiceRegistrationClient client = new HttpServiceRegistrationClient(this.httpApi);

    @Test
    void shouldMapCompleteMetadataAndReturnCreatedOutcome() {
        // Given
        ServiceMetadata metadata = metadata("activity-service", "1.0.0");
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class))).thenReturn(
                response(HttpStatus.CREATED, "activity-service", CatalogRegistrationOutcome.CREATED));

        // When
        CatalogRegistrationOutcome outcome = this.client.register(metadata);

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.CREATED);
        ArgumentCaptor<RegisterServiceDefinitionRequest> requestCaptor =
                ArgumentCaptor.forClass(RegisterServiceDefinitionRequest.class);
        verify(this.httpApi).register(requestCaptor.capture());
        RegisterServiceDefinitionRequest request = requestCaptor.getValue();
        assertThat(request.serviceId()).isEqualTo("activity-service");
        assertThat(request.applicationName()).isEqualTo("test-application");
        assertThat(request.protocol()).isEqualTo("dubbo");
        assertThat(request.namespace()).isEqualTo("production");
        assertThat(request.version()).isEqualTo("1.0.0");
        assertThat(request.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.operationId()).isEqualTo("activity-query");
            assertThat(operation.interfaceName()).isEqualTo("com.example.ActivityService");
            assertThat(operation.operationName()).isEqualTo("query");
            assertThat(operation.parameters()).singleElement().satisfies(parameter -> {
                assertThat(parameter.position()).isZero();
                assertThat(parameter.name()).isEqualTo("name");
                assertThat(parameter.typeName()).isEqualTo("java.lang.String");
            });
        });
    }

    @Test
    void shouldReturnUnchangedOutcomeForIdenticalDefinition() {
        // Given
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class))).thenReturn(
                response(HttpStatus.OK, "activity-service", CatalogRegistrationOutcome.UNCHANGED));

        // When
        CatalogRegistrationOutcome outcome = this.client.register(metadata("activity-service", "1.0.0"));

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.UNCHANGED);
    }

    @Test
    void shouldRejectMissingDependenciesAndMetadata() {
        // When & Then
        assertThatThrownBy(() -> new HttpServiceRegistrationClient(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("httpApi must not be null");
        assertThatThrownBy(() -> this.client.register(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("metadata must not be null");
    }

    @Test
    void shouldRejectUnsuccessfulOrMissingResponse() {
        // Given
        ServiceDefinitionRegistrationResponse registration = new ServiceDefinitionRegistrationResponse(
                "activity-service", CatalogRegistrationOutcome.CREATED);
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Result.success(registration)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        // When & Then
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration failed with status 502 and code SUCCESS");
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration failed with status 201 and code missing-result");
    }

    @Test
    void shouldRejectFailureResult() {
        // Given
        ServiceDefinitionRegistrationResponse registration = new ServiceDefinitionRegistrationResponse(
                "activity-service", CatalogRegistrationOutcome.CREATED);
        Result<ServiceDefinitionRegistrationResponse> failure =
                new Result<>(false, "CATALOG-0002", "conflict", registration);
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(failure));

        // When & Then
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration failed with status 422 and code CATALOG-0002");
    }

    @Test
    void shouldRejectUnexpectedServiceIdAndStatusOutcomeMismatch() {
        // Given
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class)))
                .thenReturn(response(HttpStatus.CREATED, "payment-service", CatalogRegistrationOutcome.CREATED))
                .thenReturn(response(HttpStatus.OK, "activity-service", CatalogRegistrationOutcome.CREATED));

        // When & Then
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration response contains an unexpected serviceId");
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration response status does not match outcome: CREATED");
    }

    @Test
    void shouldTranslateHttpClientFailure() {
        // Given
        RestClientException failure = new RestClientException("connection refused");
        when(this.httpApi.register(any(RegisterServiceDefinitionRequest.class))).thenThrow(failure);

        // When & Then
        assertThatThrownBy(() -> this.client.register(metadata("activity-service", "1.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service registration request failed")
                .hasCause(failure);
    }

    private static ResponseEntity<Result<ServiceDefinitionRegistrationResponse>> response(HttpStatus status,
            String serviceId, CatalogRegistrationOutcome outcome) {
        ServiceDefinitionRegistrationResponse registration =
                new ServiceDefinitionRegistrationResponse(serviceId, outcome);
        return ResponseEntity.status(status).body(Result.success(registration));
    }

    private static ServiceMetadata metadata(String serviceId, String version) {
        ServiceParameterMetadata parameter = new ServiceParameterMetadata(0, "name", "java.lang.String");
        ServiceOperationMetadata operation =
                new ServiceOperationMetadata("activity-query", "com.example.ActivityService", "query",
                        List.of(parameter));
        return new ServiceMetadata(serviceId, "test-application", "dubbo", "production", version,
                List.of(operation));
    }
}
