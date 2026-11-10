package com.lavyoung.gateweave.center.gateway.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeLeaseService;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeHeartbeat;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultGatewayNodeLeaseServiceTests {

    private static final Instant RECEIVED_AT = Instant.parse("2026-11-09T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(RECEIVED_AT, ZoneOffset.UTC);
    private static final Duration LEASE_TIMEOUT = Duration.ofSeconds(30);
    private static final UUID LEASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldRenewLeaseAtControlPlaneTime() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeLeaseService service =
                new DefaultGatewayNodeLeaseService(repository, FIXED_CLOCK, LEASE_TIMEOUT);
        when(repository.renewLease(any(GatewayNodeHeartbeat.class))).thenReturn(true);

        // When
        service.renew("node-1", LEASE_ID, 7);

        // Then
        ArgumentCaptor<GatewayNodeHeartbeat> heartbeatCaptor = ArgumentCaptor.forClass(GatewayNodeHeartbeat.class);
        verify(repository).renewLease(heartbeatCaptor.capture());
        GatewayNodeHeartbeat heartbeat = heartbeatCaptor.getValue();
        assertThat(heartbeat.nodeId()).isEqualTo("node-1");
        assertThat(heartbeat.leaseId()).isEqualTo(LEASE_ID);
        assertThat(heartbeat.receivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(heartbeat.currentConfigVersion()).isEqualTo(7);
    }

    @Test
    void shouldReportStableBusinessErrorWhenLeaseIsRejected() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeLeaseService service =
                new DefaultGatewayNodeLeaseService(repository, FIXED_CLOCK, LEASE_TIMEOUT);
        when(repository.renewLease(any(GatewayNodeHeartbeat.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.renew("node-1", LEASE_ID, 7))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.responseCode()).isSameAs(GatewayResponseCode.GATEWAY_NODE_LEASE_REJECTED);
                    assertThat(exception.arguments()).containsExactly("node-1");
                });
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);

        // When & Then
        assertThatThrownBy(() -> new DefaultGatewayNodeLeaseService(null, FIXED_CLOCK, LEASE_TIMEOUT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayNodeRepository must not be null");
        assertThatThrownBy(() -> new DefaultGatewayNodeLeaseService(repository, null, LEASE_TIMEOUT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clock must not be null");
        assertThatThrownBy(() -> new DefaultGatewayNodeLeaseService(repository, FIXED_CLOCK, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseTimeout must not be null");
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectInvalidHeartbeatBeforeAccessingRepository() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeLeaseService service =
                new DefaultGatewayNodeLeaseService(repository, FIXED_CLOCK, LEASE_TIMEOUT);

        // When & Then
        assertThatThrownBy(() -> service.renew(null, LEASE_ID, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("nodeId must not be null");
        assertThatThrownBy(() -> service.renew("  ", LEASE_ID, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nodeId must not be blank");
        assertThatThrownBy(() -> service.renew("node-1", null, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseId must not be null");
        assertThatThrownBy(() -> service.renew("node-1", LEASE_ID, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentConfigVersion must not be negative");
        verifyNoInteractions(repository);
    }

    @Test
    void shouldExpireLeasesUsingConfiguredTimeoutAndControlPlaneTime() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeLeaseService service =
                new DefaultGatewayNodeLeaseService(repository, FIXED_CLOCK, LEASE_TIMEOUT);
        when(repository.markExpiredLeasesOffline(RECEIVED_AT.minus(LEASE_TIMEOUT))).thenReturn(3);

        // When
        int expired = service.expire();

        // Then
        assertThat(expired).isEqualTo(3);
        verify(repository).markExpiredLeasesOffline(RECEIVED_AT.minus(LEASE_TIMEOUT));
    }
}
