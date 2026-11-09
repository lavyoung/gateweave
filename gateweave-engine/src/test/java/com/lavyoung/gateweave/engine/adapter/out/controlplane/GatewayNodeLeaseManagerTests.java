package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import com.lavyoung.gateweave.contract.gateway.GatewayNodeLease;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayNodeLeaseManagerTests {

    private static final Instant REGISTERED_AT = Instant.parse("2026-11-09T12:00:00Z");
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    private static final Clock FIXED_CLOCK = Clock.fixed(REGISTERED_AT, ZoneOffset.UTC);
    private static final RegisterGatewayNodeRequest REGISTRATION_REQUEST = new RegisterGatewayNodeRequest(
            "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));
    private static final GatewayNodeLease NODE_LEASE = new GatewayNodeLease(
            "node-1", UUID.fromString("00000000-0000-0000-0000-000000000001"),
            HEARTBEAT_INTERVAL, Duration.ofSeconds(30));

    @Test
    void shouldRegisterScheduleHeartbeatAndStopOnlyOnce() {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        GatewayRouteTable routeTable = mock(GatewayRouteTable.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> heartbeatTask = mock(ScheduledFuture.class);
        when(controlPlaneClient.registerNode(REGISTRATION_REQUEST)).thenReturn(NODE_LEASE);
        when(routeTable.currentVersion()).thenReturn(7L);
        when(taskScheduler.getClock()).thenReturn(FIXED_CLOCK);
        doReturn(heartbeatTask).when(taskScheduler).scheduleWithFixedDelay(
                any(Runnable.class), eq(REGISTERED_AT.plus(HEARTBEAT_INTERVAL)), eq(HEARTBEAT_INTERVAL));
        GatewayNodeLeaseManager manager = new GatewayNodeLeaseManager(
                controlPlaneClient, routeTable, taskScheduler);

        // When
        manager.start(REGISTRATION_REQUEST);
        manager.start(REGISTRATION_REQUEST);

        // Then
        ArgumentCaptor<Runnable> heartbeatCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(controlPlaneClient, times(1)).registerNode(REGISTRATION_REQUEST);
        verify(taskScheduler, times(1)).scheduleWithFixedDelay(
                heartbeatCaptor.capture(), eq(REGISTERED_AT.plus(HEARTBEAT_INTERVAL)), eq(HEARTBEAT_INTERVAL));

        // When
        heartbeatCaptor.getValue().run();
        manager.stop();

        // Then
        verify(controlPlaneClient).renewNodeLease("node-1", NODE_LEASE.leaseId(), 7);
        verify(heartbeatTask).cancel(false);
    }

    @Test
    void shouldNotScheduleHeartbeatWhenRegistrationFails() {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        GatewayRouteTable routeTable = mock(GatewayRouteTable.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        IllegalStateException failure = new IllegalStateException("registration failed");
        doThrow(failure).when(controlPlaneClient).registerNode(REGISTRATION_REQUEST);
        GatewayNodeLeaseManager manager = new GatewayNodeLeaseManager(
                controlPlaneClient, routeTable, taskScheduler);

        // When & Then
        assertThatThrownBy(() -> manager.start(REGISTRATION_REQUEST)).isSameAs(failure);
        verify(taskScheduler, never()).scheduleWithFixedDelay(
                any(Runnable.class), any(Instant.class), any(Duration.class));
        verifyNoInteractions(routeTable);
    }

    @Test
    void shouldRejectMissingDependenciesAndRequest() {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        GatewayRouteTable routeTable = mock(GatewayRouteTable.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GatewayNodeLeaseManager manager = new GatewayNodeLeaseManager(
                controlPlaneClient, routeTable, taskScheduler);

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLeaseManager(null, routeTable, taskScheduler))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("controlPlaneClient must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseManager(controlPlaneClient, null, taskScheduler))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeTable must not be null");
        assertThatThrownBy(() -> new GatewayNodeLeaseManager(controlPlaneClient, routeTable, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("taskScheduler must not be null");
        assertThatThrownBy(() -> manager.start(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
        verifyNoInteractions(controlPlaneClient, routeTable, taskScheduler);
    }
}
