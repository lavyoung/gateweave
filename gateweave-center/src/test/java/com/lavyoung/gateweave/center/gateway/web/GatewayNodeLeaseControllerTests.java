package com.lavyoung.gateweave.center.gateway.web;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lavyoung.gateweave.center.gateway.application.ReleaseGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.gateway.application.RenewGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.center.web.GlobalExceptionHandler;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.web.RequestIdFilter;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GatewayNodeLeaseController.class)
@MockBean({RenewGatewayNodeLeaseUseCase.class, ReleaseGatewayNodeLeaseUseCase.class})
@Import({GlobalExceptionHandler.class, LocalizedResultFactory.class, RequestIdFilter.class})
class GatewayNodeLeaseControllerTests {

    private static final UUID LEASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MockMvc mockMvc;
    private final RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase;
    private final ReleaseGatewayNodeLeaseUseCase releaseGatewayNodeLeaseUseCase;

    @Autowired
    GatewayNodeLeaseControllerTests(MockMvc mockMvc, RenewGatewayNodeLeaseUseCase renewGatewayNodeLeaseUseCase,
            ReleaseGatewayNodeLeaseUseCase releaseGatewayNodeLeaseUseCase) {
        this.mockMvc = mockMvc;
        this.renewGatewayNodeLeaseUseCase = renewGatewayNodeLeaseUseCase;
        this.releaseGatewayNodeLeaseUseCase = releaseGatewayNodeLeaseUseCase;
    }

    @AfterEach
    void resetUseCase() {
        reset(this.renewGatewayNodeLeaseUseCase, this.releaseGatewayNodeLeaseUseCase);
    }

    @Test
    void shouldReturnNoContentAndForwardHeartbeat() throws Exception {
        // When & Then
        this.mockMvc.perform(put("/api/v1/gateway-nodes/node-1/lease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(this.renewGatewayNodeLeaseUseCase).renew("node-1", LEASE_ID, 7);
    }

    @Test
    void shouldReturnLocalizedBusinessFailureWhenLeaseIsRejected() throws Exception {
        // Given
        doThrow(new BusinessException(GatewayResponseCode.GATEWAY_NODE_LEASE_REJECTED, "node-1"))
                .when(this.renewGatewayNodeLeaseUseCase).renew("node-1", LEASE_ID, 7);

        // When & Then
        this.mockMvc.perform(put("/api/v1/gateway-nodes/node-1/lease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-lease-rejected")
                        .content(validRequestBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-lease-rejected"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GATEWAY-0004"))
                .andExpect(jsonPath("$.message").value("The lease for gateway node node-1 is no longer valid"))
                .andExpect(jsonPath("$.data.path").value("/api/v1/gateway-nodes/node-1/lease"))
                .andExpect(jsonPath("$.data.requestId").value("request-lease-rejected"));
    }

    @Test
    void shouldRejectUnreadableHeartbeatBeforeCallingUseCase() throws Exception {
        // Given
        String requestBody = """
                {
                  "leaseId": "not-a-uuid",
                  "currentConfigVersion": 7
                }
                """;

        // When & Then
        this.mockMvc.perform(put("/api/v1/gateway-nodes/node-1/lease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0002"))
                .andExpect(jsonPath("$.message").value("Request body is invalid"));

        verify(this.renewGatewayNodeLeaseUseCase, never()).renew(anyString(), any(), anyLong());
    }

    @Test
    void shouldRejectMissingRequestBodyBeforeCallingUseCase() throws Exception {
        // When & Then
        this.mockMvc.perform(put("/api/v1/gateway-nodes/node-1/lease")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0002"));

        verify(this.renewGatewayNodeLeaseUseCase, never()).renew(anyString(), any(), anyLong());
    }

    @Test
    void shouldReturnNoContentAndReleaseLease() throws Exception {
        // When & Then
        this.mockMvc.perform(delete("/api/v1/gateway-nodes/node-1/lease")
                        .queryParam("leaseId", LEASE_ID.toString()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(this.releaseGatewayNodeLeaseUseCase).release("node-1", LEASE_ID);
    }

    @Test
    void shouldRejectMissingLeaseIdBeforeCallingReleaseUseCase() throws Exception {
        // When & Then
        this.mockMvc.perform(delete("/api/v1/gateway-nodes/node-1/lease")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0001"))
                .andExpect(jsonPath("$.message").value("Request parameters are invalid"));

        verify(this.releaseGatewayNodeLeaseUseCase, never()).release(anyString(), any());
    }

    @Test
    void shouldRejectInvalidLeaseIdBeforeCallingReleaseUseCase() throws Exception {
        // When & Then
        this.mockMvc.perform(delete("/api/v1/gateway-nodes/node-1/lease")
                        .queryParam("leaseId", "not-a-uuid")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-0001"))
                .andExpect(jsonPath("$.message").value("Request parameters are invalid"));

        verify(this.releaseGatewayNodeLeaseUseCase, never()).release(anyString(), any());
    }

    @Test
    void shouldRejectMissingRenewUseCase() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLeaseController(null, this.releaseGatewayNodeLeaseUseCase))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("renewGatewayNodeLeaseUseCase must not be null");
    }

    @Test
    void shouldRejectMissingReleaseUseCase() {
        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLeaseController(this.renewGatewayNodeLeaseUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("releaseGatewayNodeLeaseUseCase must not be null");
    }

    private static String validRequestBody() {
        return """
                {
                  "leaseId": "%s",
                  "currentConfigVersion": 7
                }
                """.formatted(LEASE_ID);
    }
}
