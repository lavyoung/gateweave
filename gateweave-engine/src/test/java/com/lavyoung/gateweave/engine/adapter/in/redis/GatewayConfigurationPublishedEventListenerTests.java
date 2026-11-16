package com.lavyoung.gateweave.engine.adapter.in.redis;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GatewayConfigurationPublishedEventListenerTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-16T14:30:00Z");

    @Test
    void shouldCoalesceEventsReceivedBeforeRefreshTaskRuns() {
        // Given
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        Executor executor = mock(Executor.class);
        GatewayConfigurationPublishedEventListener listener =
                new GatewayConfigurationPublishedEventListener(synchronizer, executor, "default");

        // When
        listener.onMessage(event("default", 1));
        listener.onMessage(event("default", 2));

        // Then
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, times(1)).execute(taskCaptor.capture());
        taskCaptor.getValue().run();
        verify(synchronizer, times(1)).synchronize("default");
    }

    @Test
    void shouldRefreshAgainWhenEventArrivesDuringCurrentRefresh() {
        // Given
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        Executor executor = mock(Executor.class);
        AtomicReference<GatewayConfigurationPublishedEventListener> listenerReference = new AtomicReference<>();
        doAnswer(invocation -> {
            listenerReference.get().onMessage(event("default", 2));
            return true;
        }).doReturn(true).when(synchronizer).synchronize("default");
        GatewayConfigurationPublishedEventListener listener =
                new GatewayConfigurationPublishedEventListener(synchronizer, executor, "default");
        listenerReference.set(listener);
        listener.onMessage(event("default", 1));
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(taskCaptor.capture());

        // When
        taskCaptor.getValue().run();

        // Then
        verify(synchronizer, times(2)).synchronize("default");
        verify(executor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void shouldIgnoreEventForUnrelatedGatewayGroup() {
        // Given
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        Executor executor = mock(Executor.class);
        GatewayConfigurationPublishedEventListener listener =
                new GatewayConfigurationPublishedEventListener(synchronizer, executor, "default");

        // When
        listener.onMessage(event("another-group", 1));

        // Then
        verifyNoInteractions(synchronizer, executor);
    }

    @Test
    void shouldAllowLaterNotificationAfterExecutorRejectsTask() {
        // Given
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        Executor executor = mock(Executor.class);
        AtomicReference<Runnable> acceptedTask = new AtomicReference<>();
        doThrow(new RejectedExecutionException("executor saturated")).doAnswer(invocation -> {
            acceptedTask.set(invocation.getArgument(0));
            return null;
        }).when(executor).execute(any(Runnable.class));
        GatewayConfigurationPublishedEventListener listener =
                new GatewayConfigurationPublishedEventListener(synchronizer, executor, " default ");

        // When & Then
        assertThatThrownBy(() -> listener.onMessage(event("default", 1)))
                .isInstanceOf(RejectedExecutionException.class).hasMessage("executor saturated");
        listener.onMessage(event("default", 2));
        acceptedTask.get().run();
        verify(executor, times(2)).execute(any(Runnable.class));
        verify(synchronizer).synchronize("default");
    }

    @Test
    void shouldRejectMissingEventAndConstructorArguments() {
        // Given
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        Executor executor = mock(Executor.class);
        GatewayConfigurationPublishedEventListener listener =
                new GatewayConfigurationPublishedEventListener(synchronizer, executor, "default");

        // When & Then
        assertThatThrownBy(() -> listener.onMessage(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("event must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEventListener(null, executor, "default"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configurationSynchronizer must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEventListener(synchronizer, null, "default"))
                .isInstanceOf(NullPointerException.class).hasMessage("executor must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEventListener(synchronizer, executor, null))
                .isInstanceOf(NullPointerException.class).hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationPublishedEventListener(synchronizer, executor, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("gatewayGroupId must not be blank");
        verifyNoInteractions(synchronizer, executor);
    }

    private static GatewayConfigurationPublishedEvent event(String gatewayGroupId, long version) {
        return new GatewayConfigurationPublishedEvent(UUID.randomUUID(), gatewayGroupId, version, PUBLISHED_AT);
    }
}
