package com.lavyoung.gateweave.center.release.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.QueryPublishedGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.web.GlobalExceptionHandler;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.center.web.RequestIdFilter;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigurationReleaseController.class)
@MockBean(PublishGatewayConfigurationUseCase.class)
@MockBean(QueryPublishedGatewayConfigurationUseCase.class)
@Import({GlobalExceptionHandler.class, LocalizedResultFactory.class, RequestIdFilter.class})
class ConfigurationReleaseControllerTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-02T10:15:30Z");

    private final MockMvc mockMvc;
    private final PublishGatewayConfigurationUseCase publishUseCase;
    private final QueryPublishedGatewayConfigurationUseCase queryUseCase;
    private final LocalizedResultFactory resultFactory;

    @Autowired
    ConfigurationReleaseControllerTests(
            MockMvc mockMvc, PublishGatewayConfigurationUseCase publishUseCase,
            QueryPublishedGatewayConfigurationUseCase queryUseCase, LocalizedResultFactory resultFactory) {
        this.mockMvc = mockMvc;
        this.publishUseCase = publishUseCase;
        this.queryUseCase = queryUseCase;
        this.resultFactory = resultFactory;
    }

    @AfterEach
    void resetUseCases() {
        reset(this.publishUseCase, this.queryUseCase);
    }

    @Test
    void shouldReturnCreatedSnapshotWhenConfigurationIsPublished() throws Exception {
        // Given
        GatewayConfigurationSnapshot snapshot = snapshot();
        when(this.publishUseCase.publish("default")).thenReturn(snapshot);

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-groups/default/configuration-releases"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.gatewayGroupId").value("default"))
                .andExpect(jsonPath("$.data.version").value(3))
                .andExpect(jsonPath("$.data.publishedAt").value("2026-11-02T10:15:30Z"))
                .andExpect(jsonPath("$.data.checksum").value("sha256:configuration"))
                .andExpect(jsonPath("$.data.routes").isEmpty());

        verify(this.publishUseCase).publish("default");
        verify(this.queryUseCase, never()).findLatestPublished("default");
    }

    @Test
    void shouldReturnLatestPublishedSnapshot() throws Exception {
        // Given
        GatewayConfigurationSnapshot snapshot = snapshot();
        when(this.queryUseCase.findLatestPublished("default")).thenReturn(Optional.of(snapshot));

        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/configuration-releases/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gatewayGroupId").value("default"))
                .andExpect(jsonPath("$.data.version").value(3));

        verify(this.queryUseCase).findLatestPublished("default");
        verify(this.publishUseCase, never()).publish("default");
    }

    @Test
    void shouldReturnNoContentWhenConfigurationHasNotBeenPublished() throws Exception {
        // Given
        when(this.queryUseCase.findLatestPublished("default")).thenReturn(Optional.empty());

        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/configuration-releases/latest"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(this.queryUseCase).findLatestPublished("default");
    }

    @Test
    void shouldRejectMissingDependencies() {
        // When & Then
        assertThatThrownBy(() -> new ConfigurationReleaseController(
                null, this.queryUseCase, this.resultFactory))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("publishUseCase must not be null");
        assertThatThrownBy(() -> new ConfigurationReleaseController(
                this.publishUseCase, null, this.resultFactory))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("queryUseCase must not be null");
        assertThatThrownBy(() -> new ConfigurationReleaseController(
                this.publishUseCase, this.queryUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resultFactory must not be null");
    }

    private static GatewayConfigurationSnapshot snapshot() {
        return new GatewayConfigurationSnapshot(
                "default", 3, PUBLISHED_AT, "sha256:configuration", List.of());
    }
}
