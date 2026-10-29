package com.lavyoung.gateweave.plugin.security.shiro;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.plugin.security.jwt.JwtTokenVerifier;
import com.lavyoung.gateweave.plugin.security.jwt.JwtVerificationPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

class ShiroGatewayAuthenticatorTests {

    private static final String ISSUER = "gateweave-issuer";
    private static final String AUDIENCE = "gateweave-engine";
    private static final String TOKEN = "signed-jwt";
    private static final SecretKey VERIFICATION_KEY = Jwts.SIG.HS256.key().build();

    @Test
    void shouldKeepAuthenticatedPrincipalsIsolatedBetweenRequests() {
        // Given
        ShiroGatewayAuthenticator authenticator = authenticator();
        String firstToken = signedToken(VERIFICATION_KEY, "user-10001");
        String secondToken = signedToken(VERIFICATION_KEY, "user-10002");

        // When
        Optional<AuthenticatedPrincipal> firstPrincipal = authenticator.authenticate(firstToken);
        Optional<AuthenticatedPrincipal> secondPrincipal = authenticator.authenticate(secondToken);

        // Then
        assertThat(firstPrincipal).contains(new AuthenticatedPrincipal("user-10001"));
        assertThat(secondPrincipal).contains(new AuthenticatedPrincipal("user-10002"));
    }

    @Test
    void shouldRejectTokenWithInvalidSignatureThroughCompleteAuthenticationChain() {
        // Given
        ShiroGatewayAuthenticator authenticator = authenticator();
        String token = signedToken(Jwts.SIG.HS256.key().build(), "user-10001");

        // When & Then
        assertThat(authenticator.authenticate(token)).isEmpty();
    }

    @Test
    void shouldDisableSessionCreationAndLogoutAfterSuccessfulAuthentication() {
        // Given
        SecurityManager securityManager = mock(SecurityManager.class);
        Subject subject = mock(Subject.class);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal("user-10001");
        given(securityManager.createSubject(any(SubjectContext.class))).willReturn(subject);
        given(subject.getPrincipal()).willReturn(principal);
        ShiroGatewayAuthenticator authenticator = new ShiroGatewayAuthenticator(securityManager);

        // When
        Optional<AuthenticatedPrincipal> result = authenticator.authenticate(TOKEN);

        // Then
        ArgumentCaptor<SubjectContext> contextCaptor = ArgumentCaptor.forClass(SubjectContext.class);
        ArgumentCaptor<AuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(AuthenticationToken.class);
        then(securityManager).should().createSubject(contextCaptor.capture());
        then(subject).should().login(tokenCaptor.capture());
        then(subject).should().logout();
        assertThat(contextCaptor.getValue().isSessionCreationEnabled()).isFalse();
        assertThat(tokenCaptor.getValue()).isInstanceOf(BearerToken.class);
        assertThat(((BearerToken) tokenCaptor.getValue()).getToken()).isEqualTo(TOKEN);
        assertThat(result).contains(principal);
    }

    @Test
    void shouldLogoutAfterFailedAuthentication() {
        // Given
        SecurityManager securityManager = mock(SecurityManager.class);
        Subject subject = mock(Subject.class);
        given(securityManager.createSubject(any(SubjectContext.class))).willReturn(subject);
        willThrow(new IncorrectCredentialsException()).given(subject).login(any(BearerToken.class));
        ShiroGatewayAuthenticator authenticator = new ShiroGatewayAuthenticator(securityManager);

        // When
        Optional<AuthenticatedPrincipal> result = authenticator.authenticate(TOKEN);

        // Then
        assertThat(result).isEmpty();
        then(subject).should().logout();
    }

    @Test
    void shouldRejectNullSecurityManagerOrToken() {
        // Given
        SecurityManager securityManager = mock(SecurityManager.class);
        ShiroGatewayAuthenticator authenticator = new ShiroGatewayAuthenticator(securityManager);

        // When & Then
        assertThatThrownBy(() -> new ShiroGatewayAuthenticator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("securityManager must not be null");
        assertThatThrownBy(() -> authenticator.authenticate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("token must not be null");
    }

    private ShiroGatewayAuthenticator authenticator() {
        JwtVerificationPolicy policy = new JwtVerificationPolicy(
                VERIFICATION_KEY, ISSUER, AUDIENCE, Duration.ZERO, 4096);
        JwtTokenVerifier verifier = new JwtTokenVerifier(policy);
        GatewayAuthenticationRealm realm = new GatewayAuthenticationRealm(verifier);
        return new ShiroGatewayAuthenticator(StatelessShiroSecurityManagerFactory.create(realm));
    }

    private String signedToken(SecretKey key, String subject) {
        return Jwts.builder()
                .issuer(ISSUER)
                .audience()
                .add(AUDIENCE)
                .and()
                .subject(subject)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
