package com.lavyoung.gateweave.engine.application.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayConfigurationSynchronizerTests {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-02T12:00:00Z");
    private static final InvocationTargetConfiguration TARGET = new InvocationTargetConfiguration(
            "dubbo", "cn.example.ActivityService", "sayHi", List.of(), Duration.ofSeconds(3),
            Map.of("version", "1.0.0"));
    private static final GatewayRouteConfigurationMapper ROUTE_MAPPER = new GatewayRouteConfigurationMapper();

    @Test
    void shouldApplyLatestPublishedConfiguration() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        var routeTable = new GatewayRouteTable(List.of());
        var publishedRoute = route("route-published", "/published");
        when(controlPlaneClient.findLatestPublishedConfiguration("default"))
                .thenReturn(Optional.of(snapshot(1, publishedRoute)));
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When
        var replaced = synchronizer.synchronize("default");

        // Then
        assertThat(replaced).isTrue();
        assertThat(routeTable.currentVersion()).isEqualTo(1);
        assertThat(routeTable.match("GET", URI.create("/published"))).isPresent();
        verify(controlPlaneClient).findLatestPublishedConfiguration("default");
        verify(invocationDispatcher).prepare(TARGET);
    }

    @Test
    void shouldKeepCurrentRoutesWhenControlPlaneHasNoPublishedConfiguration() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        GatewayRoute localRoute = ROUTE_MAPPER.toDomain(route("route-local", "/local"));
        var routeTable = new GatewayRouteTable(List.of(localRoute));
        when(controlPlaneClient.findLatestPublishedConfiguration("default")).thenReturn(Optional.empty());
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When
        var replaced = synchronizer.synchronize("default");

        // Then
        assertThat(replaced).isFalse();
        assertThat(routeTable.currentVersion()).isZero();
        assertThat(routeTable.match("GET", URI.create("/local"))).containsSame(localRoute);
        verifyNoInteractions(invocationDispatcher);
    }

    @Test
    void shouldIgnoreSnapshotOlderThanCurrentVersion() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        GatewayRoute currentRoute = ROUTE_MAPPER.toDomain(route("route-current", "/current"));
        var routeTable = new GatewayRouteTable(List.of());
        routeTable.replaceIfNew(2, List.of(currentRoute));
        when(controlPlaneClient.findLatestPublishedConfiguration("default"))
                .thenReturn(Optional.of(snapshot(1, route("route-stale", "/stale"))));
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When
        var replaced = synchronizer.synchronize("default");

        // Then
        assertThat(replaced).isFalse();
        assertThat(routeTable.currentVersion()).isEqualTo(2);
        assertThat(routeTable.match("GET", URI.create("/current"))).containsSame(currentRoute);
        assertThat(routeTable.match("GET", URI.create("/stale"))).isEmpty();
        verifyNoInteractions(invocationDispatcher);
    }

    @Test
    void shouldKeepLastValidSnapshotWhenPublishedConfigurationCannotBeMapped() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        GatewayRoute currentRoute = ROUTE_MAPPER.toDomain(route("route-current", "/current"));
        var routeTable = new GatewayRouteTable(List.of());
        routeTable.replaceIfNew(1, List.of(currentRoute));
        when(controlPlaneClient.findLatestPublishedConfiguration("default"))
                .thenReturn(Optional.of(snapshot(2, route("route-invalid", "invalid"))));
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When & Then
        assertThatThrownBy(() -> synchronizer.synchronize("default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must start with '/'");
        assertThat(routeTable.currentVersion()).isEqualTo(1);
        assertThat(routeTable.match("GET", URI.create("/current"))).containsSame(currentRoute);
        verifyNoInteractions(invocationDispatcher);
    }

    @Test
    void shouldKeepLastValidSnapshotWhenProtocolResourcePreparationFails() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        GatewayRoute currentRoute = ROUTE_MAPPER.toDomain(route("route-current", "/current"));
        var routeTable = new GatewayRouteTable(List.of());
        routeTable.replaceIfNew(1, List.of(currentRoute));
        when(controlPlaneClient.findLatestPublishedConfiguration("default"))
                .thenReturn(Optional.of(snapshot(2, route("route-new", "/new"))));
        var failure = new IllegalStateException("reference unavailable");
        doThrow(failure).when(invocationDispatcher).prepare(TARGET);
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When & Then
        assertThatThrownBy(() -> synchronizer.synchronize("default")).isSameAs(failure);
        assertThat(routeTable.currentVersion()).isEqualTo(1);
        assertThat(routeTable.match("GET", URI.create("/current"))).containsSame(currentRoute);
        assertThat(routeTable.match("GET", URI.create("/new"))).isEmpty();
    }

    @Test
    void shouldPropagateControlPlaneFailure() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        var routeTable = new GatewayRouteTable(List.of());
        when(controlPlaneClient.findLatestPublishedConfiguration("default"))
                .thenThrow(new IllegalStateException("control plane unavailable"));
        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);

        // When & Then
        assertThatThrownBy(() -> synchronizer.synchronize("default"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("control plane unavailable");
        assertThat(routeTable.currentVersion()).isZero();
        verifyNoInteractions(invocationDispatcher);
    }

    @Test
    void shouldRejectMissingDependenciesAndGatewayGroupId() {
        // Given
        var controlPlaneClient = mock(GatewayControlPlaneClient.class);
        var invocationDispatcher = mock(ProtocolInvocationDispatcher.class);
        var routeTable = new GatewayRouteTable(List.of());

        // When & Then
        assertThatThrownBy(() -> new GatewayConfigurationSynchronizer(
                null, ROUTE_MAPPER, invocationDispatcher, routeTable))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("controlPlaneClient must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSynchronizer(
                controlPlaneClient, null, invocationDispatcher, routeTable))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeMapper must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, null, routeTable))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("invocationDispatcher must not be null");
        assertThatThrownBy(() -> new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeTable must not be null");

        var synchronizer = new GatewayConfigurationSynchronizer(
                controlPlaneClient, ROUTE_MAPPER, invocationDispatcher, routeTable);
        assertThatThrownBy(() -> synchronizer.synchronize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        verifyNoInteractions(controlPlaneClient);
    }

    private static GatewayConfigurationSnapshot snapshot(long version, RouteConfiguration... routes) {
        return new GatewayConfigurationSnapshot(
                "default", version, PUBLISHED_AT, "sha256:snapshot-%d".formatted(version), List.of(routes));
    }

    private static RouteConfiguration route(String routeId, String path) {
        return new RouteConfiguration(
                routeId, path, "GET", "activity-service", RouteAuthenticationMode.ANONYMOUS, TARGET, Map.of());
    }
}
