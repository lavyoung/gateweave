package com.lavyoung.gateweave.center.gateway.web;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

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

import com.lavyoung.gateweave.center.web.GlobalExceptionHandler;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.web.RequestIdFilter;
import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayNodeAddressConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GatewayNodeRegistrationController.class)
@MockBean(RegisterGatewayNodeUseCase.class)
@Import({GlobalExceptionHandler.class, LocalizedResultFactory.class, RequestIdFilter.class})
class GatewayNodeRegistrationControllerTests {

    private static final Instant HEARTBEAT_AT = Instant.parse("2026-10-31T12:00:00Z");

    private final MockMvc mockMvc;
    private final RegisterGatewayNodeUseCase registerGatewayNodeUseCase;
    private final LocalizedResultFactory resultFactory;

    @Autowired
    GatewayNodeRegistrationControllerTests(
            MockMvc mockMvc, RegisterGatewayNodeUseCase registerGatewayNodeUseCase,
            LocalizedResultFactory resultFactory) {
        this.mockMvc = mockMvc;
        this.registerGatewayNodeUseCase = registerGatewayNodeUseCase;
        this.resultFactory = resultFactory;
    }

    @AfterEach
    void resetUseCase() {
        reset(this.registerGatewayNodeUseCase);
    }

    @Test
    void shouldReturnCreatedWhenNodeIsRegistered() throws Exception {
        // Given
        when(this.registerGatewayNodeUseCase.register(any(GatewayNodeRegistration.class)))
                .thenReturn(result(GatewayNodeRegistrationOutcome.REGISTERED));

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nodeId").value("node-1"))
                .andExpect(jsonPath("$.data.gatewayGroupId").value("default"))
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.outcome").value("REGISTERED"))
                .andExpect(jsonPath("$.data.currentConfigVersion").value(0));

        ArgumentCaptor<GatewayNodeRegistration> registrationCaptor =
                ArgumentCaptor.forClass(GatewayNodeRegistration.class);
        verify(this.registerGatewayNodeUseCase).register(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue()).isEqualTo(registration());
    }

    @Test
    void shouldReturnOkWhenNodeIsRefreshed() throws Exception {
        // Given
        when(this.registerGatewayNodeUseCase.register(any(GatewayNodeRegistration.class)))
                .thenReturn(result(GatewayNodeRegistrationOutcome.REFRESHED));

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("REFRESHED"));
    }

    @Test
    void shouldRejectInvalidRequestBeforeCallingUseCase() throws Exception {
        // Given
        String requestBody = """
                {
                  "nodeId": " ",
                  "gatewayGroupId": "default",
                  "nodeName": " ",
                  "advertisedAddress": "http://127.0.0.1:8080"
                }
                """;

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-validation")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-validation"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-0001"))
                .andExpect(jsonPath("$.message").value("Request parameters are invalid"))
                .andExpect(jsonPath("$.data.path").value("/api/v1/gateway-nodes"))
                .andExpect(jsonPath("$.data.requestId").value("request-validation"));

        verify(this.registerGatewayNodeUseCase, never()).register(any(GatewayNodeRegistration.class));
    }

    @Test
    void shouldRejectUnreadableAddressBeforeCallingUseCase() throws Exception {
        // Given
        String requestBody = """
                {
                  "nodeId": "node-1",
                  "gatewayGroupId": "default",
                  "nodeName": "Gateway Node 1",
                  "advertisedAddress": "http://["
                }
                """;

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0002"));

        verify(this.registerGatewayNodeUseCase, never()).register(any(GatewayNodeRegistration.class));
    }

    @Test
    void shouldRejectAddressThatViolatesDomainRules() throws Exception {
        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody("default", "ftp://127.0.0.1:8080")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-0001"))
                .andExpect(jsonPath("$.message").value("Request parameters are invalid"));

        verify(this.registerGatewayNodeUseCase, never()).register(any(GatewayNodeRegistration.class));
    }

    @Test
    void shouldReturnLocalizedBusinessFailureWhenGatewayGroupDoesNotExist() throws Exception {
        // Given
        when(this.registerGatewayNodeUseCase.register(any(GatewayNodeRegistration.class)))
                .thenThrow(new GatewayGroupNotFoundException("missing-group"));

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-group-missing")
                        .content(requestBody("missing-group", "http://127.0.0.1:8080")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-group-missing"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GATEWAY-0001"))
                .andExpect(jsonPath("$.message").value("Gateway group missing-group does not exist"))
                .andExpect(jsonPath("$.data.path").value("/api/v1/gateway-nodes"))
                .andExpect(jsonPath("$.data.requestId").value("request-group-missing"));
    }

    @Test
    void shouldReturnLocalizedBusinessFailureWhenAdvertisedAddressIsOccupied() throws Exception {
        // Given
        URI address = URI.create("http://127.0.0.1:8080");
        when(this.registerGatewayNodeUseCase.register(any(GatewayNodeRegistration.class)))
                .thenThrow(new GatewayNodeAddressConflictException(address));

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-address-conflict")
                        .content(requestBody("default", address.toASCIIString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-address-conflict"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GATEWAY-0002"))
                .andExpect(jsonPath("$.message").value("网关节点地址 http://127.0.0.1:8080 已被占用"))
                .andExpect(jsonPath("$.data.path").value("/api/v1/gateway-nodes"))
                .andExpect(jsonPath("$.data.requestId").value("request-address-conflict"));
    }

    @Test
    void shouldRejectMissingDependencies() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeRegistrationController(null, this.resultFactory))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("registerGatewayNodeUseCase must not be null");
        assertThatThrownBy(() -> new GatewayNodeRegistrationController(this.registerGatewayNodeUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resultFactory must not be null");
    }

    private static GatewayNodeRegistrationResult result(GatewayNodeRegistrationOutcome outcome) {
        GatewayNode node = new GatewayNode(
                registration(), GatewayNodeStatus.ONLINE,
                UUID.fromString("00000000-0000-0000-0000-000000000001"), HEARTBEAT_AT, 0);
        return new GatewayNodeRegistrationResult(node, outcome);
    }

    private static GatewayNodeRegistration registration() {
        return new GatewayNodeRegistration(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:8080"));
    }

    private static String validRequestBody() {
        return requestBody("default", "http://127.0.0.1:8080");
    }

    private static String requestBody(String gatewayGroupId, String advertisedAddress) {
        return """
                {
                  "nodeId": "node-1",
                  "gatewayGroupId": "%s",
                  "nodeName": "Gateway Node 1",
                  "advertisedAddress": "%s"
                }
                """.formatted(gatewayGroupId, advertisedAddress);
    }
}
