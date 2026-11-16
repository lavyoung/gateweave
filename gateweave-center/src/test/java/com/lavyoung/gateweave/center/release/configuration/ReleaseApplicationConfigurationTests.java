package com.lavyoung.gateweave.center.release.configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

import com.lavyoung.gateweave.center.configuration.ControlPlaneClockConfiguration;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventOutbox;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventPublisher;
import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.QueryPublishedGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.internal.ConfigurationPublishedEventDispatcher;
import com.lavyoung.gateweave.center.release.application.internal.DefaultGatewayConfigurationPublicationService;
import com.lavyoung.gateweave.center.release.lifecycle.ConfigurationPublishedEventDispatchScheduler;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseApplicationConfigurationTests {

    @Test
    void shouldAssemblePublicationUseCaseWithSharedControlPlaneClock() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        when(repository.findLatestPublished("default")).thenReturn(Optional.empty());
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                ReleaseApplicationConfiguration.class)
                        .withBean(GatewayRouteConfigurationQuery.class, () -> query)
                        .withBean(ConfigurationReleaseRepository.class, () -> repository)
                        .withBean(ConfigurationPublishedEventOutbox.class, () -> outbox)
                        .withBean(ConfigurationPublishedEventPublisher.class, () -> publisher);

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(PublishGatewayConfigurationUseCase.class);
            assertThat(context).hasSingleBean(QueryPublishedGatewayConfigurationUseCase.class);
            assertThat(context).hasSingleBean(ConfigurationPublishedEventDispatcher.class);
            assertThat(context).doesNotHaveBean(ConfigurationPublishedEventDispatchScheduler.class);
            assertThat(context.getBean(PublishGatewayConfigurationUseCase.class)).isInstanceOf(
                    DefaultGatewayConfigurationPublicationService.class);
            QueryPublishedGatewayConfigurationUseCase queryUseCase =
                    context.getBean(QueryPublishedGatewayConfigurationUseCase.class);
            assertThat(queryUseCase.findLatestPublished("default")).isEmpty();
            verify(repository).findLatestPublished("default");
        });
    }

    @Test
    void shouldAssembleDispatchSchedulerWhenDeliveryIsEnabled() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked") ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        Clock schedulerClock = Clock.fixed(Instant.parse("2026-11-16T02:00:00Z"), ZoneOffset.UTC);
        when(taskScheduler.getClock()).thenReturn(schedulerClock);
        doReturn(scheduledFuture).when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
                eq(schedulerClock.instant().plusSeconds(2)), eq(Duration.ofSeconds(2)));
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                ReleaseApplicationConfiguration.class)
                        .withBean(GatewayRouteConfigurationQuery.class, () -> query)
                        .withBean(ConfigurationReleaseRepository.class, () -> repository)
                        .withBean(ConfigurationPublishedEventOutbox.class, () -> outbox)
                        .withBean(ConfigurationPublishedEventPublisher.class, () -> publisher)
                        .withBean("controlPlaneTaskScheduler", TaskScheduler.class, () -> taskScheduler)
                        .withPropertyValues("gateweave.configuration-events.delivery.enabled=true",
                                "gateweave.configuration-events.delivery.dispatch-interval=2s");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigurationPublishedEventDispatchScheduler.class);
            assertThat(context.getBean(ConfigurationEventDeliveryProperties.class).dispatchInterval())
                    .isEqualTo(Duration.ofSeconds(2));
        });
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
                eq(schedulerClock.instant().plusSeconds(2)), eq(Duration.ofSeconds(2)));
        verify(scheduledFuture).cancel(false);
    }

    @Test
    void shouldNotAssembleDispatchSchedulerWhenDeliveryIsDisabled() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                ReleaseApplicationConfiguration.class)
                        .withBean(GatewayRouteConfigurationQuery.class, () -> query)
                        .withBean(ConfigurationReleaseRepository.class, () -> repository)
                        .withBean(ConfigurationPublishedEventOutbox.class, () -> outbox)
                        .withBean(ConfigurationPublishedEventPublisher.class, () -> publisher)
                        .withPropertyValues("gateweave.configuration-events.delivery.enabled=false");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ConfigurationPublishedEventDispatchScheduler.class);
        });
    }

    @Test
    void shouldFailAssemblyWhenRouteConfigurationQueryIsMissing() {
        // Given
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                ReleaseApplicationConfiguration.class)
                        .withBean(ConfigurationReleaseRepository.class, () -> repository)
                        .withBean(ConfigurationPublishedEventOutbox.class, () -> outbox)
                        .withBean(ConfigurationPublishedEventPublisher.class, () -> publisher);

        // When & Then
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailAssemblyWhenConfigurationReleaseRepositoryIsMissing() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationPublishedEventOutbox outbox = mock(ConfigurationPublishedEventOutbox.class);
        ConfigurationPublishedEventPublisher publisher = mock(ConfigurationPublishedEventPublisher.class);
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                ReleaseApplicationConfiguration.class)
                        .withBean(GatewayRouteConfigurationQuery.class, () -> query)
                        .withBean(ConfigurationPublishedEventOutbox.class, () -> outbox)
                        .withBean(ConfigurationPublishedEventPublisher.class, () -> publisher);

        // When & Then
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
