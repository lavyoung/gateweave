package com.lavyoung.gateweave.contract.catalog;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationParameterRequest;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterServiceDefinitionRequestTests {

    @Test
    void shouldPreserveCompleteServiceDefinition() {
        // Given
        ServiceOperationParameterRequest parameter =
                new ServiceOperationParameterRequest(0, "name", "java.lang.String");
        ServiceOperationRequest operation =
                new ServiceOperationRequest("say-hello", "com.example.HelloService", "sayHello", List.of(parameter));

        // When
        RegisterServiceDefinitionRequest request = new RegisterServiceDefinitionRequest("hello-service",
                "hello-application", "dubbo", "production", "1.0.0", List.of(operation));

        // Then
        assertThat(request.serviceId()).isEqualTo("hello-service");
        assertThat(request.applicationName()).isEqualTo("hello-application");
        assertThat(request.protocol()).isEqualTo("dubbo");
        assertThat(request.namespace()).isEqualTo("production");
        assertThat(request.version()).isEqualTo("1.0.0");
        assertThat(request.operations()).containsExactly(operation);
    }

    @Test
    void shouldCreateImmutableCollectionSnapshots() {
        // Given
        List<ServiceOperationParameterRequest> parameters = new ArrayList<>();
        parameters.add(new ServiceOperationParameterRequest(0, "name", "java.lang.String"));
        ServiceOperationRequest operation =
                new ServiceOperationRequest("say-hello", "com.example.HelloService", "sayHello", parameters);
        List<ServiceOperationRequest> operations = new ArrayList<>();
        operations.add(operation);

        // When
        RegisterServiceDefinitionRequest request =
                new RegisterServiceDefinitionRequest("hello-service", "hello-application", "dubbo", "", "",
                        operations);
        parameters.clear();
        operations.clear();

        // Then
        assertThat(operation.parameters()).hasSize(1);
        assertThat(request.operations()).containsExactly(operation);
        assertThatThrownBy(() -> operation.parameters().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.operations().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectMissingServiceFields() {
        // When & Then
        assertThatThrownBy(() -> request(null, "hello-application", "dubbo", "", "", List.of())).isInstanceOf(
                NullPointerException.class).hasMessage("serviceId must not be null");
        assertThatThrownBy(() -> request("hello-service", null, "dubbo", "", "", List.of())).isInstanceOf(
                NullPointerException.class).hasMessage("applicationName must not be null");
        assertThatThrownBy(() -> request("hello-service", "hello-application", null, "", "", List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("protocol must not be null");
        assertThatThrownBy(() -> request("hello-service", "hello-application", "dubbo", null, "", List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("namespace must not be null");
        assertThatThrownBy(() -> request("hello-service", "hello-application", "dubbo", "", null, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("version must not be null");
        assertThatThrownBy(() -> request("hello-service", "hello-application", "dubbo", "", "", null))
                .isInstanceOf(NullPointerException.class).hasMessage("operations must not be null");
    }

    @Test
    void shouldRejectMissingOperationAndParameterFields() {
        // When & Then
        assertThatThrownBy(() -> new ServiceOperationRequest(null, "com.example.HelloService", "sayHello", List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("operationId must not be null");
        assertThatThrownBy(() -> new ServiceOperationRequest("say-hello", null, "sayHello", List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("interfaceName must not be null");
        assertThatThrownBy(() -> new ServiceOperationRequest("say-hello", "com.example.HelloService", null, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("operationName must not be null");
        assertThatThrownBy(() -> new ServiceOperationRequest("say-hello", "com.example.HelloService", "sayHello", null))
                .isInstanceOf(NullPointerException.class).hasMessage("parameters must not be null");
        assertThatThrownBy(() -> new ServiceOperationParameterRequest(null, "name", "java.lang.String"))
                .isInstanceOf(NullPointerException.class).hasMessage("position must not be null");
        assertThatThrownBy(() -> new ServiceOperationParameterRequest(0, null, "java.lang.String"))
                .isInstanceOf(NullPointerException.class).hasMessage("name must not be null");
        assertThatThrownBy(() -> new ServiceOperationParameterRequest(0, "name", null))
                .isInstanceOf(NullPointerException.class).hasMessage("typeName must not be null");
    }

    @Test
    void shouldLeaveCatalogBusinessRulesToControlPlaneDomain() {
        // Given
        ServiceOperationParameterRequest parameter = new ServiceOperationParameterRequest(-1, " ", " ");
        ServiceOperationRequest operation = new ServiceOperationRequest(" ", " ", " ", List.of(parameter));

        // When
        RegisterServiceDefinitionRequest request = request(" ", " ", " ", " ", " ", List.of(operation));

        // Then
        assertThat(request.serviceId()).isBlank();
        assertThat(request.operations().get(0).parameters().get(0).position()).isNegative();
    }

    private static RegisterServiceDefinitionRequest request(String serviceId, String applicationName, String protocol,
            String namespace, String version, List<ServiceOperationRequest> operations) {
        return new RegisterServiceDefinitionRequest(serviceId, applicationName, protocol, namespace, version,
                operations);
    }
}
