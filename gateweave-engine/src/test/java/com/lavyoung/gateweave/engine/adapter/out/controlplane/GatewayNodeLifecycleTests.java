package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.service.GatewayConfigurationSynchronizer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayNodeLifecycleTests {

    private static final RegisterGatewayNodeRequest REGISTRATION_REQUEST = new RegisterGatewayNodeRequest(
            "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));

    @Test
    void shouldRegisterNodeSynchronizeConfigurationAndStartOnlyOnce() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());

        // When
        lifecycle.start();
        lifecycle.start();

        // Then
        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(lifecycle.getPhase()).isEqualTo(Integer.MIN_VALUE);
        verify(leaseManager, times(1)).start(REGISTRATION_REQUEST);
        verify(synchronizer, times(1)).synchronize("default");
        var invocationOrder = inOrder(leaseManager, synchronizer);
        invocationOrder.verify(leaseManager).start(REGISTRATION_REQUEST);
        invocationOrder.verify(synchronizer).synchronize("default");

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
        verify(leaseManager).stop();
    }

    @Test
    void shouldRegisterOnlyOnceWhenStartedConcurrently() throws Exception {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        CountDownLatch registrationStarted = new CountDownLatch(1);
        CountDownLatch releaseRegistration = new CountDownLatch(1);
        doAnswer(invocation -> {
            registrationStarted.countDown();
            assertThat(releaseRegistration.await(2, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(leaseManager).start(REGISTRATION_REQUEST);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When & Then
        try {
            Future<?> firstStart = executor.submit(lifecycle::start);
            assertThat(registrationStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> secondStart = executor.submit(lifecycle::start);
            releaseRegistration.countDown();
            firstStart.get(3, TimeUnit.SECONDS);
            secondStart.get(3, TimeUnit.SECONDS);

            verify(leaseManager, times(1)).start(REGISTRATION_REQUEST);
            verify(synchronizer, times(1)).synchronize("default");
            assertThat(lifecycle.isRunning()).isTrue();
        } finally {
            releaseRegistration.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void shouldRemainStoppedWhenRegistrationFails() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        IllegalStateException failure = new IllegalStateException("control plane unavailable");
        doThrow(failure).when(leaseManager).start(REGISTRATION_REQUEST);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());

        // When & Then
        assertThatThrownBy(lifecycle::start).isSameAs(failure);
        assertThat(lifecycle.isRunning()).isFalse();
        verifyNoInteractions(synchronizer);
    }

    @Test
    void shouldRemainStoppedWhenConfigurationSynchronizationFails() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        IllegalStateException failure = new IllegalStateException("configuration unavailable");
        when(synchronizer.synchronize("default")).thenThrow(failure);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());

        // When & Then
        assertThatThrownBy(lifecycle::start).isSameAs(failure);
        assertThat(lifecycle.isRunning()).isFalse();
        verify(leaseManager).start(REGISTRATION_REQUEST);
        verify(leaseManager).stop();
    }

    @Test
    void shouldRefreshConfigurationOnlyWhileLifecycleIsRunning() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());

        // When
        lifecycle.refreshConfiguration();

        // Then
        verifyNoInteractions(leaseManager, synchronizer);

        // When
        lifecycle.start();
        lifecycle.refreshConfiguration();
        lifecycle.stop();
        lifecycle.refreshConfiguration();

        // Then
        verify(leaseManager).start(REGISTRATION_REQUEST);
        verify(leaseManager).stop();
        verify(synchronizer, times(2)).synchronize("default");
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldRemainRunningAndAllowNextRefreshAfterOneRefreshFails() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);
        IllegalStateException failure = new IllegalStateException("configuration unavailable");
        when(synchronizer.synchronize("default")).thenReturn(true).thenThrow(failure).thenReturn(false);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(leaseManager, synchronizer, properties());
        lifecycle.start();

        // When & Then
        assertThatThrownBy(lifecycle::refreshConfiguration).isSameAs(failure);
        assertThat(lifecycle.isRunning()).isTrue();

        // When
        lifecycle.refreshConfiguration();

        // Then
        assertThat(lifecycle.isRunning()).isTrue();
        verify(synchronizer, times(3)).synchronize("default");
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        GatewayNodeLeaseManager leaseManager = mock(GatewayNodeLeaseManager.class);
        GatewayConfigurationSynchronizer synchronizer = mock(GatewayConfigurationSynchronizer.class);

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLifecycle(null, synchronizer, properties()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("leaseManager must not be null");
        assertThatThrownBy(() -> new GatewayNodeLifecycle(leaseManager, null, properties()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configurationSynchronizer must not be null");
        assertThatThrownBy(() -> new GatewayNodeLifecycle(leaseManager, synchronizer, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties must not be null");
    }

    private static GatewayControlPlaneProperties properties() {
        return new GatewayControlPlaneProperties(URI.create("http://127.0.0.1:8080"), "node-1", "default",
                "Gateway Node 1", URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(2), Duration.ofHours(1));
    }
}
