package com.lavyoung.gateweave.center.gateway.configuration;

import com.lavyoung.gateweave.center.gateway.application.ExpireGatewayNodeLeasesUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

class GatewayNodeLeaseExpirationSchedulerTests {

    private static final Instant CURRENT_TIME = Instant.parse("2026-11-10T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(CURRENT_TIME, ZoneOffset.UTC);
    private static final Duration SCAN_INTERVAL = Duration.ofSeconds(5);

    @Test
    void shouldScheduleExpirationAfterConfiguredIntervalAndExecuteUseCase() {
        // Given
        ExpireGatewayNodeLeasesUseCase useCase = mock(ExpireGatewayNodeLeasesUseCase.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.getClock()).thenReturn(FIXED_CLOCK);
        doReturn(scheduledFuture).when(taskScheduler).scheduleWithFixedDelay(
                any(Runnable.class), eq(CURRENT_TIME.plus(SCAN_INTERVAL)), eq(SCAN_INTERVAL));
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);
        GatewayNodeLeaseExpirationScheduler scheduler =
                new GatewayNodeLeaseExpirationScheduler(useCase, taskScheduler, SCAN_INTERVAL);

        // When
        scheduler.start();

        // Then
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).scheduleWithFixedDelay(
                taskCaptor.capture(), eq(CURRENT_TIME.plus(SCAN_INTERVAL)), eq(SCAN_INTERVAL));
        assertThat(scheduler.isRunning()).isTrue();
        taskCaptor.getValue().run();
        verify(useCase).expire();
    }

    @Test
    void shouldStartOnceAndCancelScheduledTaskIdempotently() {
        // Given
        ExpireGatewayNodeLeasesUseCase useCase = mock(ExpireGatewayNodeLeasesUseCase.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.getClock()).thenReturn(FIXED_CLOCK);
        doReturn(scheduledFuture).when(taskScheduler).scheduleWithFixedDelay(
                any(Runnable.class), any(Instant.class), eq(SCAN_INTERVAL));
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);
        GatewayNodeLeaseExpirationScheduler scheduler =
                new GatewayNodeLeaseExpirationScheduler(useCase, taskScheduler, SCAN_INTERVAL);

        // When
        scheduler.start();
        scheduler.start();
        scheduler.stop();
        scheduler.stop();

        // Then
        verify(taskScheduler, times(1)).scheduleWithFixedDelay(
                any(Runnable.class), eq(CURRENT_TIME.plus(SCAN_INTERVAL)), eq(SCAN_INTERVAL));
        verify(scheduledFuture, times(1)).cancel(false);
        assertThat(scheduler.isRunning()).isFalse();
        verifyNoInteractions(useCase);
    }

    @Test
    void shouldContinueSchedulingAfterOneExpirationFails() throws Exception {
        // Given
        AtomicInteger invocations = new AtomicInteger();
        CountDownLatch secondInvocation = new CountDownLatch(1);
        ExpireGatewayNodeLeasesUseCase useCase = () -> {
            if (invocations.incrementAndGet() == 1) {
                throw new IllegalStateException("expected expiration failure");
            }
            secondInvocation.countDown();
            return 0;
        };
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        taskScheduler.initialize();
        GatewayNodeLeaseExpirationScheduler scheduler = new GatewayNodeLeaseExpirationScheduler(
                useCase, taskScheduler, Duration.ofMillis(10));

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
    void shouldRejectMissingDependencies() {
        // Given
        ExpireGatewayNodeLeasesUseCase useCase = mock(ExpireGatewayNodeLeasesUseCase.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLeaseExpirationScheduler(null, taskScheduler, SCAN_INTERVAL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("expireGatewayNodeLeasesUseCase must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseExpirationScheduler(useCase, null, SCAN_INTERVAL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("taskScheduler must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseExpirationScheduler(useCase, taskScheduler, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("scanInterval must not be null");
        assertThat(new GatewayNodeLeaseExpirationScheduler(useCase, taskScheduler, SCAN_INTERVAL).getPhase())
                .isEqualTo(Integer.MAX_VALUE - 100);
        verifyNoInteractions(useCase, taskScheduler);
    }
}
