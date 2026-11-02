package com.lavyoung.gateweave.center.release.application.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.InvocationParameterConfiguration;
import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultGatewayConfigurationPublicationServiceTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-02T10:15:30Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC);

    @Test
    void shouldPublishCurrentRouteCandidatesAtControlPlaneTime() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        DefaultGatewayConfigurationPublicationService service =
                new DefaultGatewayConfigurationPublicationService(query, repository, FIXED_CLOCK);
        List<RouteConfiguration> routes = List.of(route());
        GatewayConfigurationSnapshot expectedSnapshot = new GatewayConfigurationSnapshot(
                "default", 1, PUBLISHED_AT, "sha256:configuration", routes);
        when(query.findCandidates("default")).thenReturn(routes);
        when(repository.publish("default", routes, PUBLISHED_AT)).thenReturn(expectedSnapshot);

        // When
        GatewayConfigurationSnapshot snapshot = service.publish("default");

        // Then
        assertThat(snapshot).isSameAs(expectedSnapshot);
        verify(query).findCandidates("default");
        verify(repository).publish("default", routes, PUBLISHED_AT);
    }

    @Test
    void shouldPublishEmptyCandidateSet() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        DefaultGatewayConfigurationPublicationService service =
                new DefaultGatewayConfigurationPublicationService(query, repository, FIXED_CLOCK);
        List<RouteConfiguration> routes = List.of();
        GatewayConfigurationSnapshot expectedSnapshot = new GatewayConfigurationSnapshot(
                "default", 1, PUBLISHED_AT, "sha256:empty", routes);
        when(query.findCandidates("default")).thenReturn(routes);
        when(repository.publish("default", routes, PUBLISHED_AT)).thenReturn(expectedSnapshot);

        // When
        GatewayConfigurationSnapshot snapshot = service.publish("default");

        // Then
        assertThat(snapshot).isSameAs(expectedSnapshot);
        verify(repository).publish("default", routes, PUBLISHED_AT);
    }

    @Test
    void shouldPropagateCandidateQueryFailureWithoutCallingRepository() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        DefaultGatewayConfigurationPublicationService service =
                new DefaultGatewayConfigurationPublicationService(query, repository, FIXED_CLOCK);
        IllegalStateException failure = new IllegalStateException("candidate query failed");
        when(query.findCandidates("default")).thenThrow(failure);

        // When & Then
        assertThatThrownBy(() -> service.publish("default")).isSameAs(failure);
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);

        // When & Then
        assertThatThrownBy(() -> new DefaultGatewayConfigurationPublicationService(null, repository, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeConfigurationQuery must not be null");
        assertThatThrownBy(() -> new DefaultGatewayConfigurationPublicationService(query, null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configurationReleaseRepository must not be null");
        assertThatThrownBy(() -> new DefaultGatewayConfigurationPublicationService(query, repository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clock must not be null");
    }

    @Test
    void shouldRejectInvalidGatewayGroupIdBeforeQueryingCandidates() {
        // Given
        GatewayRouteConfigurationQuery query = mock(GatewayRouteConfigurationQuery.class);
        ConfigurationReleaseRepository repository = mock(ConfigurationReleaseRepository.class);
        DefaultGatewayConfigurationPublicationService service =
                new DefaultGatewayConfigurationPublicationService(query, repository, FIXED_CLOCK);

        // When & Then
        assertThatThrownBy(() -> service.publish(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> service.publish(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
        verifyNoInteractions(query, repository);
    }

    private static RouteConfiguration route() {
        InvocationTargetConfiguration target = new InvocationTargetConfiguration(
                "dubbo", "cn.example.ActivityBooth", "sayHi",
                List.of(new InvocationParameterConfiguration("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
        return new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", RouteAuthenticationMode.REQUIRED,
                target, Map.of());
    }
}
