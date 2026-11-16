package com.lavyoung.gateweave.center.release.lifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

import com.lavyoung.gateweave.center.release.application.internal.ConfigurationPublishedEventDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConfigurationPublishedEventDispatchSchedulerTests {

    private static final Instant CURRENT_TIME = Instant.parse("2026-11-16T01:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(CURRENT_TIME, ZoneOffset.UTC);
    private static final Duration DISPATCH_INTERVAL = Duration.ofSeconds(5);

    @Test
    void shouldScheduleDispatchAfterConfiguredIntervalAndExecuteApplicationService() {
        // Given
        ConfigurationPublishedEventDispatcher dispatcher = mock(ConfigurationPublishedEventDispatcher.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked") ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.getClock()).thenReturn(FIXED_CLOCK);
        doReturn(scheduledFuture).when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
                eq(CURRENT_TIME.plus(DISPATCH_INTERVAL)), eq(DISPATCH_INTERVAL));
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);
        ConfigurationPublishedEventDispatchScheduler scheduler =
                new ConfigurationPublishedEventDispatchScheduler(dispatcher, taskScheduler, DISPATCH_INTERVAL);

        // When
        scheduler.start();

        // Then
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(),
                eq(CURRENT_TIME.plus(DISPATCH_INTERVAL)), eq(DISPATCH_INTERVAL));
        assertThat(scheduler.isRunning()).isTrue();
        taskCaptor.getValue().run();
        verify(dispatcher).dispatch();
    }

    @Test
    void shouldStartOnceAndCancelScheduledTaskIdempotently() {
        // Given
        ConfigurationPublishedEventDispatcher dispatcher = mock(ConfigurationPublishedEventDispatcher.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked") ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.getClock()).thenReturn(FIXED_CLOCK);
        doReturn(scheduledFuture).when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
                any(Instant.class), eq(DISPATCH_INTERVAL));
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);
        ConfigurationPublishedEventDispatchScheduler scheduler =
                new ConfigurationPublishedEventDispatchScheduler(dispatcher, taskScheduler, DISPATCH_INTERVAL);

        // When
        scheduler.start();
        scheduler.start();
        scheduler.stop();
        scheduler.stop();

        // Then
        verify(taskScheduler, times(1)).scheduleWithFixedDelay(any(Runnable.class),
                eq(CURRENT_TIME.plus(DISPATCH_INTERVAL)), eq(DISPATCH_INTERVAL));
        verify(scheduledFuture, times(1)).cancel(false);
        assertThat(scheduler.isRunning()).isFalse();
        verifyNoInteractions(dispatcher);
    }

    @Test
    void shouldContinueSchedulingAfterOneDispatchFails() throws Exception {
        // Given
        AtomicInteger invocations = new AtomicInteger();
        CountDownLatch secondInvocation = new CountDownLatch(1);
        ConfigurationPublishedEventDispatcher dispatcher = mock(ConfigurationPublishedEventDispatcher.class);
        when(dispatcher.dispatch()).thenAnswer(invocation -> {
            if (invocations.incrementAndGet() == 1) {
                throw new IllegalStateException("expected dispatch failure");
            }
            secondInvocation.countDown();
            return 0;
        });
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        taskScheduler.initialize();
        ConfigurationPublishedEventDispatchScheduler scheduler =
                new ConfigurationPublishedEventDispatchScheduler(dispatcher, taskScheduler, Duration.ofMillis(10));

        try {
            // When
            scheduler.start();

            // Then
            assertThat(secondInvocation.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(invocations.get()).isGreaterThanOrEqualTo(2);
        } finally {
            scheduler.stop();
            taskScheduler.shutdown();
        }
    }

    @Test
    void shouldRejectMissingDependenciesAndInvalidInterval() {
        // Given
        ConfigurationPublishedEventDispatcher dispatcher = mock(ConfigurationPublishedEventDispatcher.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);

        // When & Then
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatchScheduler(
                null, taskScheduler, DISPATCH_INTERVAL)).isInstanceOf(NullPointerException.class)
                .hasMessage("dispatcher must not be null");
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatchScheduler(
                dispatcher, null, DISPATCH_INTERVAL)).isInstanceOf(NullPointerException.class)
                .hasMessage("taskScheduler must not be null");
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatchScheduler(dispatcher, taskScheduler, null))
                .isInstanceOf(NullPointerException.class).hasMessage("dispatchInterval must not be null");
        assertThatThrownBy(() -> new ConfigurationPublishedEventDispatchScheduler(
                dispatcher, taskScheduler, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dispatchInterval must be greater than zero");
        assertThat(new ConfigurationPublishedEventDispatchScheduler(
                dispatcher, taskScheduler, DISPATCH_INTERVAL).getPhase()).isEqualTo(Integer.MAX_VALUE - 100);
        verifyNoInteractions(dispatcher, taskScheduler);
    }
}
