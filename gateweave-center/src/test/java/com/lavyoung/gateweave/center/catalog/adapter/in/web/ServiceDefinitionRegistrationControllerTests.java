package com.lavyoung.gateweave.center.catalog.adapter.in.web;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lavyoung.gateweave.center.adapter.in.web.GlobalExceptionHandler;
import com.lavyoung.gateweave.center.adapter.in.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.adapter.in.web.RequestIdFilter;
import com.lavyoung.gateweave.center.catalog.application.port.in.RegisterServiceDefinitionUseCase;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogStatus;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperation;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperationParameter;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.error.CatalogResponseCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceDefinitionRegistrationController.class)
@MockBean(RegisterServiceDefinitionUseCase.class)
@Import({GlobalExceptionHandler.class, LocalizedResultFactory.class, RequestIdFilter.class})
class ServiceDefinitionRegistrationControllerTests {

    private static final String ENDPOINT = "/api/v1/catalog/services";

    private final MockMvc mockMvc;
    private final RegisterServiceDefinitionUseCase registerServiceDefinitionUseCase;
    private final LocalizedResultFactory resultFactory;

    @Autowired
    ServiceDefinitionRegistrationControllerTests(
            MockMvc mockMvc,
            RegisterServiceDefinitionUseCase registerServiceDefinitionUseCase,
            LocalizedResultFactory resultFactory) {
        this.mockMvc = mockMvc;
        this.registerServiceDefinitionUseCase = registerServiceDefinitionUseCase;
        this.resultFactory = resultFactory;
    }

    @AfterEach
    void resetUseCase() {
        reset(this.registerServiceDefinitionUseCase);
    }

    @Test
    void shouldReturnCreatedAndMapCompleteDefinition() throws Exception {
        // Given
        when(this.registerServiceDefinitionUseCase.register(any(ServiceDefinition.class)))
                .thenReturn(CatalogRegistrationOutcome.CREATED);

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.serviceId").value("activity-service"))
                .andExpect(jsonPath("$.data.outcome").value("CREATED"));

        ArgumentCaptor<ServiceDefinition> definitionCaptor = ArgumentCaptor.forClass(ServiceDefinition.class);
        verify(this.registerServiceDefinitionUseCase).register(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue()).isEqualTo(expectedDefinition());
    }

    @Test
    void shouldReturnOkWhenDefinitionIsUnchanged() throws Exception {
        // Given
        when(this.registerServiceDefinitionUseCase.register(any(ServiceDefinition.class)))
                .thenReturn(CatalogRegistrationOutcome.UNCHANGED);

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceId").value("activity-service"))
                .andExpect(jsonPath("$.data.outcome").value("UNCHANGED"));
    }

    @Test
    void shouldRejectInvalidNestedParameterBeforeCallingUseCase() throws Exception {
        // Given
        String requestBody = validRequestBody().replace("\"position\": 0", "\"position\": -1");

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0001"))
                .andExpect(jsonPath("$.message").value("Request parameters are invalid"));

        verify(this.registerServiceDefinitionUseCase, never()).register(any(ServiceDefinition.class));
    }

    @Test
    void shouldRejectDiscontinuousParameterPositionsBeforeCallingUseCase() throws Exception {
        // Given
        String requestBody = validRequestBody().replace("\"position\": 0", "\"position\": 1");

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0001"));

        verify(this.registerServiceDefinitionUseCase, never()).register(any(ServiceDefinition.class));
    }

    @Test
    void shouldReturnLocalizedBusinessFailureForDefinitionConflict() throws Exception {
        // Given
        when(this.registerServiceDefinitionUseCase.register(any(ServiceDefinition.class)))
                .thenThrow(new BusinessException(
                        CatalogResponseCode.SERVICE_DEFINITION_CONFLICT, "activity-service"));

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-catalog-conflict")
                        .content(validRequestBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-catalog-conflict"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATALOG-0002"))
                .andExpect(jsonPath("$.message")
                        .value("Service activity-service already exists with a different definition"))
                .andExpect(jsonPath("$.data.path").value(ENDPOINT))
                .andExpect(jsonPath("$.data.requestId").value("request-catalog-conflict"));
    }

    @Test
    void shouldRejectMissingDependencies() {
        // When & Then
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationController(null, this.resultFactory))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("registerServiceDefinitionUseCase must not be null");
        assertThatThrownBy(() -> new ServiceDefinitionRegistrationController(
                this.registerServiceDefinitionUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resultFactory must not be null");
    }

    private static ServiceDefinition expectedDefinition() {
        ServiceOperationParameter parameter = new ServiceOperationParameter(0, "name", "java.lang.String");
        ServiceOperation operation = new ServiceOperation(
                "activity-query",
                "activity-service",
                "cn.example.ActivityService",
                "query",
                ServiceCatalogStatus.ACTIVE,
                0L,
                List.of(parameter));
        return new ServiceDefinition(
                "activity-service",
                "activity-application",
                "dubbo",
                "production",
                "1.0.0",
                ServiceCatalogStatus.ACTIVE,
                0L,
                List.of(operation));
    }

    private static String validRequestBody() {
        return """
                {
                  "serviceId": "activity-service",
                  "applicationName": "activity-application",
                  "protocol": "dubbo",
                  "namespace": "production",
                  "version": "1.0.0",
                  "operations": [
                    {
                      "operationId": "activity-query",
                      "interfaceName": "cn.example.ActivityService",
                      "operationName": "query",
                      "parameters": [
                        {
                          "position": 0,
                          "name": "name",
                          "typeName": "java.lang.String"
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
