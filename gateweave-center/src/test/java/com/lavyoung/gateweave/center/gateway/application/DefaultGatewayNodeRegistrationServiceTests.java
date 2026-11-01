package com.lavyoung.gateweave.center.gateway.application;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultGatewayNodeRegistrationServiceTests {

    private static final Instant REGISTERED_AT = Instant.parse("2026-10-31T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(REGISTERED_AT, ZoneOffset.UTC);

    @Test
    void shouldRegisterOnlineNodeAtControlPlaneTime() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeRegistrationService service = new DefaultGatewayNodeRegistrationService(
                repository, FIXED_CLOCK);
        GatewayNodeRegistration registration = registration();
        GatewayNode expectedNode = GatewayNode.registered(registration, REGISTERED_AT);
        GatewayNodeRegistrationResult expectedResult = new GatewayNodeRegistrationResult(
                expectedNode, GatewayNodeRegistrationOutcome.REGISTERED);
        when(repository.registerOrRefresh(expectedNode)).thenReturn(expectedResult);

        // When
        GatewayNodeRegistrationResult result = service.register(registration);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(repository).registerOrRefresh(expectedNode);
    }

    @Test
    void shouldPropagateRepositoryFailure() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeRegistrationService service = new DefaultGatewayNodeRegistrationService(
                repository, FIXED_CLOCK);
        GatewayNodeRegistration registration = registration();
        GatewayNode node = GatewayNode.registered(registration, REGISTERED_AT);
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        when(repository.registerOrRefresh(node)).thenThrow(failure);

        // When & Then
        assertThatThrownBy(() -> service.register(registration)).isSameAs(failure);
    }

    @Test
    void shouldRejectMissingDependenciesAndRegistration() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        DefaultGatewayNodeRegistrationService service = new DefaultGatewayNodeRegistrationService(
                repository, FIXED_CLOCK);

        // When & Then
        assertThatThrownBy(() -> new DefaultGatewayNodeRegistrationService(null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayNodeRepository must not be null");
        assertThatThrownBy(() -> new DefaultGatewayNodeRegistrationService(repository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clock must not be null");
        assertThatThrownBy(() -> service.register(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("registration must not be null");
        verifyNoInteractions(repository);
    }

    private static GatewayNodeRegistration registration() {
        return new GatewayNodeRegistration(
                "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:8080"));
    }
}
