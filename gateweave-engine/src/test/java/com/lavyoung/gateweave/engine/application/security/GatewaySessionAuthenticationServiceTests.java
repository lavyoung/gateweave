package com.lavyoung.gateweave.engine.application.security;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.RouteAuthenticationPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

class GatewaySessionAuthenticationServiceTests {

    private static final String TOKEN = "signed-jwt";

    @Test
    void shouldAllowAnonymousRouteWithoutCallingAuthenticator() {
        // Given
        var authenticator = mock(GatewayAuthenticator.class);
        var service = service(authenticator);
        var session = session(RouteAuthenticationPolicy.ANONYMOUS, headers(Map.of()));

        // When
        boolean authenticated = service.authenticate(session);

        // Then
        assertThat(authenticated).isTrue();
        verifyNoInteractions(authenticator);
    }

    @Test
    void shouldAllowRequiredRouteWithValidBearerToken() {
        // Given
        var authenticator = mock(GatewayAuthenticator.class);
        var service = service(authenticator);
        var session = session(RouteAuthenticationPolicy.REQUIRED,
                headers(Map.of("Authorization", List.of("Bearer " + TOKEN))));
        given(authenticator.authenticate(TOKEN))
                .willReturn(Optional.of(new AuthenticatedPrincipal("user-10001")));

        // When
        boolean authenticated = service.authenticate(session);

        // Then
        assertThat(authenticated).isTrue();
        then(authenticator).should().authenticate(TOKEN);
    }

    @Test
    void shouldRejectRequiredRouteWithMissingOrMalformedBearerToken() {
        // Given
        var authenticator = mock(GatewayAuthenticator.class);
        var service = service(authenticator);
        var missingTokenSession = session(RouteAuthenticationPolicy.REQUIRED, headers(Map.of()));
        var malformedTokenSession = session(RouteAuthenticationPolicy.REQUIRED,
                headers(Map.of("Authorization", List.of("Basic credentials"))));

        // When & Then
        assertThat(service.authenticate(missingTokenSession)).isFalse();
        assertThat(service.authenticate(malformedTokenSession)).isFalse();
        then(authenticator).should(never()).authenticate(TOKEN);
        verifyNoInteractions(authenticator);
    }

    @Test
    void shouldRejectRequiredRouteWhenAuthenticatorRejectsToken() {
        // Given
        var authenticator = mock(GatewayAuthenticator.class);
        var service = service(authenticator);
        var session = session(RouteAuthenticationPolicy.REQUIRED,
                headers(Map.of("Authorization", List.of("Bearer " + TOKEN))));
        given(authenticator.authenticate(TOKEN)).willReturn(Optional.empty());

        // When & Then
        assertThat(service.authenticate(session)).isFalse();
        then(authenticator).should().authenticate(TOKEN);
    }

    @Test
    void shouldPropagateUnexpectedAuthenticatorFailure() {
        // Given
        var authenticator = mock(GatewayAuthenticator.class);
        var service = service(authenticator);
        var session = session(RouteAuthenticationPolicy.REQUIRED,
                headers(Map.of("Authorization", List.of("Bearer " + TOKEN))));
        given(authenticator.authenticate(TOKEN)).willThrow(new IllegalStateException("authentication unavailable"));

        // When & Then
        assertThatThrownBy(() -> service.authenticate(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("authentication unavailable");
    }

    @Test
    void shouldRejectMissingDependenciesAndSession() {
        // Given
        var tokenResolver = new BearerTokenResolver();
        var authenticator = mock(GatewayAuthenticator.class);
        var service = new GatewaySessionAuthenticationService(tokenResolver, authenticator);

        // When & Then
        assertThatThrownBy(() -> new GatewaySessionAuthenticationService(null, authenticator))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("tokenResolver must not be null");
        assertThatThrownBy(() -> new GatewaySessionAuthenticationService(tokenResolver, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticator must not be null");
        assertThatThrownBy(() -> service.authenticate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("session must not be null");
    }

    private static GatewaySessionAuthenticationService service(GatewayAuthenticator authenticator) {
        return new GatewaySessionAuthenticationService(new BearerTokenResolver(), authenticator);
    }

    private static GatewaySession session(RouteAuthenticationPolicy policy, HttpHeaders headers) {
        var command = new GatewayRequestCommand("request-1", "GET", URI.create("/sayHi"), headers,
                new HttpParameterValues(Map.of()), new byte[0]);
        var target = new InvocationTarget(
                "dubbo", "cn.example.ActivityBooth", "sayHi", List.of(), Duration.ofSeconds(3), Map.of());
        var route = new GatewayRoute(
                "route-say-hi", "/sayHi", "GET", "activity-service", policy, target, Map.of());
        return new GatewaySession(command, route);
    }

    private static HttpHeaders headers(Map<String, List<String>> valuesByName) {
        return HttpHeaders.of(valuesByName, (name, value) -> true);
    }
}
