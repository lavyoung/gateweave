package com.lavyoung.gateweave.contract.configuration;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayConfigurationPublishedEventTests {

    private static final UUID EVENT_ID = UUID.fromString("c181f7e5-f923-4e1f-9101-d0bf08c54047");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-15T08:00:00Z");

    @Test
    void shouldCreateConfigurationPublishedEvent() {
        // When
        GatewayConfigurationPublishedEvent event =
                new GatewayConfigurationPublishedEvent(EVENT_ID, " default ", 7, PUBLISHED_AT);

        // Then
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.gatewayGroupId()).isEqualTo("default");
        assertThat(event.version()).isEqualTo(7);
        assertThat(event.publishedAt()).isEqualTo(PUBLISHED_AT);
    }

    @Test
    void shouldRejectMissingRequiredProperties() {
        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(null, "default", 1, PUBLISHED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventId must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(EVENT_ID, null, 1, PUBLISHED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(EVENT_ID, "default", 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("publishedAt must not be null");
    }

    @Test
    void shouldRejectBlankGatewayGroupId() {
        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(EVENT_ID, " \t ", 1, PUBLISHED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
    }

    @Test
    void shouldRejectNonPositiveVersion() {
        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(EVENT_ID, "default", 0, PUBLISHED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be greater than zero");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEvent(EVENT_ID, "default", -1, PUBLISHED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be greater than zero");
    }
}
