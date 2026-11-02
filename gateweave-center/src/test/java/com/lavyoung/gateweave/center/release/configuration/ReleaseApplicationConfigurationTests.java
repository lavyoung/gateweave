package com.lavyoung.gateweave.center.release.configuration;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.center.configuration.ControlPlaneClockConfiguration;
import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.QueryPublishedGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.internal.DefaultGatewayConfigurationPublicationService;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseApplicationConfigurationTests {

    @Test
    void shouldAssemblePublicationUseCaseWithSharedControlPlaneClock() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        when(repository.findLatestPublished("default")).thenReturn(Optional.empty());
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(
                        ControlPlaneClockConfiguration.class, ReleaseApplicationConfiguration.class)
                .withBean(GatewayRouteConfigurationQuery.class, () -> query)
                .withBean(ConfigurationReleaseRepository.class, () -> repository);

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(PublishGatewayConfigurationUseCase.class);
            assertThat(context).hasSingleBean(QueryPublishedGatewayConfigurationUseCase.class);
            assertThat(context.getBean(PublishGatewayConfigurationUseCase.class))
                    .isInstanceOf(DefaultGatewayConfigurationPublicationService.class);
            QueryPublishedGatewayConfigurationUseCase queryUseCase =
                    context.getBean(QueryPublishedGatewayConfigurationUseCase.class);
            assertThat(queryUseCase.findLatestPublished("default")).isEmpty();
            verify(repository).findLatestPublished("default");
        });
    }

    @Test
    void shouldFailAssemblyWhenRouteConfigurationQueryIsMissing() {
        // Given
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(
                        ControlPlaneClockConfiguration.class, ReleaseApplicationConfiguration.class)
                .withBean(ConfigurationReleaseRepository.class, () -> repository);

        // When & Then
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailAssemblyWhenConfigurationReleaseRepositoryIsMissing() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(
                        ControlPlaneClockConfiguration.class, ReleaseApplicationConfiguration.class)
                .withBean(GatewayRouteConfigurationQuery.class, () -> query);

        // When & Then
        contextRunner.run(context -> assertThat(context).hasFailed());
    }
}
