package com.lavyoung.gateweave.center.catalog.adapter.in.web;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.catalog.adapter.in.web.RegisterServiceDefinitionRequest.ServiceOperationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterServiceDefinitionRequestTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidServiceDefinition() {
        // Given
        RegisterServiceDefinitionRequest request = validRequest();

        // When
        Set<ConstraintViolation<RegisterServiceDefinitionRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldValidateNestedOperationAndParameter() {
        // Given
        RegisterServiceDefinitionRequest.ServiceOperationParameterRequest parameter =
                new RegisterServiceDefinitionRequest.ServiceOperationParameterRequest(-1, "", "");
        ServiceOperationRequest operation = new ServiceOperationRequest("", "", "", List.of(parameter));
        RegisterServiceDefinitionRequest request = new RegisterServiceDefinitionRequest(
                "activity-service", "activity-application", "dubbo", "", "", List.of(operation));

        // When
        Set<ConstraintViolation<RegisterServiceDefinitionRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                "operations[0].operationId",
                "operations[0].interfaceName",
                "operations[0].operationName",
                "operations[0].parameters[0].position",
                "operations[0].parameters[0].name",
                "operations[0].parameters[0].typeName");
    }

    @Test
    void shouldRejectMissingRequiredCollectionsAndFields() {
        // Given
        RegisterServiceDefinitionRequest request = new RegisterServiceDefinitionRequest(
                null, null, null, null, null, null);

        // When
        Set<ConstraintViolation<RegisterServiceDefinitionRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "serviceId", "applicationName", "protocol", "namespace", "version", "operations");
    }

    private static RegisterServiceDefinitionRequest validRequest() {
        RegisterServiceDefinitionRequest.ServiceOperationParameterRequest parameter =
                new RegisterServiceDefinitionRequest.ServiceOperationParameterRequest(0, "name", "java.lang.String");
        ServiceOperationRequest operation = new ServiceOperationRequest(
                "activity-query", "cn.example.ActivityService", "query", List.of(parameter));
        return new RegisterServiceDefinitionRequest(
                "activity-service", "activity-application", "dubbo", "production", "1.0.0", List.of(operation));
    }
}
