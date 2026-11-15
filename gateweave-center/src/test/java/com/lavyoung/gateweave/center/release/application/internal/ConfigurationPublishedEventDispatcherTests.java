package com.lavyoung.gateweave.center.release.application.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventOutbox;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConfigurationPublishedEventDispatcherTests {

    private static final Instant DISPATCHED_AT = Instant.parse("2026-11-15T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(DISPATCHED_AT, ZoneOffset.UTC);

    @Test
    void shouldPublishAndConfirmEveryClaimedEvent() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ConfigurationPublishedEventDispatcher dispatcher =
                new ConfigurationPublishedEventDispatcher(outbox, publisher, FIXED_CLOCK);
        GatewayConfigurationPublishedEvent first = event("default", 1);
        GatewayConfigurationPublishedEvent second = event("default", 2);
        when(outbox.claimDue(DISPATCHED_AT)).thenReturn(List.of(first, second));

        // When
        int publishedCount = dispatcher.dispatch();

        // Then
        assertThat(publishedCount).isEqualTo(2);
        verify(publisher).publish(first);
        verify(publisher).publish(second);
        verify(outbox).markPublished(first.eventId(), DISPATCHED_AT);
        verify(outbox).markPublished(second.eventId(), DISPATCHED_AT);
    }

    @Test
    void shouldContinueWithRemainingEventsAfterPublicationFailure() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ConfigurationPublishedEventDispatcher dispatcher =
                new ConfigurationPublishedEventDispatcher(outbox, publisher, FIXED_CLOCK);
        GatewayConfigurationPublishedEvent failed = event("default", 1);
        GatewayConfigurationPublishedEvent published = event("default", 2);
        when(outbox.claimDue(DISPATCHED_AT)).thenReturn(List.of(failed, published));
        doThrow(new IllegalStateException("redis unavailable")).when(publisher).publish(failed);

        // When
        int publishedCount = dispatcher.dispatch();

        // Then
        assertThat(publishedCount).isEqualTo(1);
        verify(outbox).markFailed(failed.eventId(), DISPATCHED_AT,
                "java.lang.IllegalStateException: redis unavailable");
        verify(outbox, never()).markPublished(failed.eventId(), DISPATCHED_AT);
        verify(publisher).publish(published);
        verify(outbox).markPublished(published.eventId(), DISPATCHED_AT);
    }

    @Test
    void shouldUseExceptionTypeWhenPublicationFailureHasNoMessage() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ConfigurationPublishedEventDispatcher dispatcher =
                new ConfigurationPublishedEventDispatcher(outbox, publisher, FIXED_CLOCK);
        GatewayConfigurationPublishedEvent event = event("default", 1);
        when(outbox.claimDue(DISPATCHED_AT)).thenReturn(List.of(event));
        doThrow(new IllegalStateException()).when(publisher).publish(event);

        // When
        int publishedCount = dispatcher.dispatch();

        // Then
        assertThat(publishedCount).isZero();
        verify(outbox).markFailed(event.eventId(), DISPATCHED_AT, "java.lang.IllegalStateException");
    }

    @Test
    void shouldReturnZeroWithoutCallingPublisherWhenNoEventIsDue() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ConfigurationPublishedEventDispatcher dispatcher =
                new ConfigurationPublishedEventDispatcher(outbox, publisher, FIXED_CLOCK);
        when(outbox.claimDue(DISPATCHED_AT)).thenReturn(List.of());

        // When
        int publishedCount = dispatcher.dispatch();

        // Then
        assertThat(publishedCount).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldPropagateConfirmationFailureWithoutRecordingPublicationFailure() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ConfigurationPublishedEventDispatcher dispatcher =
                new ConfigurationPublishedEventDispatcher(outbox, publisher, FIXED_CLOCK);
        GatewayConfigurationPublishedEvent event = event("default", 1);
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(outbox.claimDue(DISPATCHED_AT)).thenReturn(List.of(event));
        doThrow(failure).when(outbox).markPublished(event.eventId(), DISPATCHED_AT);

        // When & Then
        assertThatThrownBy(dispatcher::dispatch).isSameAs(failure);
        verify(publisher).publish(event);
        verify(outbox, never()).markFailed(any(), any(), any());
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);

        // When & Then
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatcher(null, publisher, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class).hasMessage("outbox must not be null");
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatcher(outbox, null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class).hasMessage("publisher must not be null");
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatcher(outbox, publisher, null))
                .isInstanceOf(NullPointerException.class).hasMessage("clock must not be null");
    }

    private static GatewayConfigurationPublishedEvent event(String gatewayGroupId, long version) {
        return new GatewayConfigurationPublishedEvent(UUID.randomUUID(), gatewayGroupId, version, DISPATCHED_AT);
    }
}
