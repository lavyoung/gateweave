package com.lavyoung.gateweave.plugin.security.shiro;

import java.util.Optional;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class GatewayAuthenticationRealmTests {

    private static final String TOKEN = "signed-jwt";
    private static final String SUBJECT = "user-10001";

    @Test
    void shouldRestoreTrustedPrincipalWithoutRetainingCredentials() {
        // Given
        GatewayAuthenticator authenticator = mock(GatewayAuthenticator.class);
        GatewayAuthenticationRealm realm = new GatewayAuthenticationRealm(authenticator);
        given(authenticator.authenticate(TOKEN)).willReturn(Optional.of(new AuthenticatedPrincipal(SUBJECT)));

        // When
        AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(new BearerToken(TOKEN));

        // Then
        assertThat(authenticationInfo.getPrincipals().getPrimaryPrincipal())
                .isEqualTo(new AuthenticatedPrincipal(SUBJECT));
        assertThat(authenticationInfo.getCredentials()).isNull();
        then(authenticator).should().authenticate(TOKEN);
    }

    @Test
    void shouldRejectInvalidBearerTokenWithoutExposingCredentials() {
        // Given
        GatewayAuthenticator authenticator = mock(GatewayAuthenticator.class);
        GatewayAuthenticationRealm realm = new GatewayAuthenticationRealm(authenticator);
        given(authenticator.authenticate(TOKEN)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> realm.getAuthenticationInfo(new BearerToken(TOKEN)))
                .isInstanceOf(IncorrectCredentialsException.class)
                .hasMessage("bearer token is invalid")
                .hasMessageNotContaining(TOKEN);
        then(authenticator).should().authenticate(TOKEN);
    }

    @Test
    void shouldOnlySupportBearerToken() {
        // Given
        GatewayAuthenticationRealm realm = new GatewayAuthenticationRealm(token -> Optional.empty());

        // When & Then
        assertThat(realm.supports(new BearerToken(TOKEN))).isTrue();
        assertThat(realm.supports(new UsernamePasswordToken("username", "password"))).isFalse();
    }

    @Test
    void shouldRejectNullAuthenticator() {
        // When & Then
        assertThatThrownBy(() -> new GatewayAuthenticationRealm(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticator must not be null");
    }
}
