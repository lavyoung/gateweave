package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GatewayNodeLifecycleTests {

    private static final RegisterGatewayNodeRequest REGISTRATION_REQUEST = new RegisterGatewayNodeRequest(
            "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:7397"));

    @Test
    void shouldRegisterNodeAndStartOnlyOnce() {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(controlPlaneClient, properties());

        // When
        lifecycle.start();
        lifecycle.start();

        // Then
        assertThat(lifecycle.isRunning()).isTrue();
        verify(controlPlaneClient, times(1)).registerNode(REGISTRATION_REQUEST);

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldRegisterOnlyOnceWhenStartedConcurrently() throws Exception {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        CountDownLatch registrationStarted = new CountDownLatch(1);
        CountDownLatch releaseRegistration = new CountDownLatch(1);
        doAnswer(invocation -> {
            registrationStarted.countDown();
            assertThat(releaseRegistration.await(2, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(controlPlaneClient).registerNode(REGISTRATION_REQUEST);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(controlPlaneClient, properties());
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When & Then
        try {
            Future<?> firstStart = executor.submit(lifecycle::start);
            assertThat(registrationStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> secondStart = executor.submit(lifecycle::start);
            releaseRegistration.countDown();
            firstStart.get(3, TimeUnit.SECONDS);
            secondStart.get(3, TimeUnit.SECONDS);

            verify(controlPlaneClient, times(1)).registerNode(REGISTRATION_REQUEST);
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
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);
        IllegalStateException failure = new IllegalStateException("control plane unavailable");
        doThrow(failure).when(controlPlaneClient).registerNode(REGISTRATION_REQUEST);
        GatewayNodeLifecycle lifecycle = new GatewayNodeLifecycle(controlPlaneClient, properties());

        // When & Then
        assertThatThrownBy(lifecycle::start).isSameAs(failure);
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldRejectMissingDependencies() {
        // Given
        GatewayControlPlaneClient controlPlaneClient = mock(GatewayControlPlaneClient.class);

        // When & Then
        assertThatThrownBy(() -> new GatewayNodeLifecycle(null, properties()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("controlPlaneClient must not be null");
        assertThatThrownBy(() -> new GatewayNodeLifecycle(controlPlaneClient, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties must not be null");
    }

    private static GatewayControlPlaneProperties properties() {
        return new GatewayControlPlaneProperties(URI.create("http://127.0.0.1:8080"), "node-1", "default",
                "Gateway Node 1", URI.create("http://127.0.0.1:7397"), Duration.ofSeconds(2));
    }
}
