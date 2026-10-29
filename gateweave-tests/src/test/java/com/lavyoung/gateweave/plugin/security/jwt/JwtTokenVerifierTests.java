package com.lavyoung.gateweave.plugin.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenVerifierTests {

    private static final String ISSUER = "gateweave-issuer";
    private static final String AUDIENCE = "gateweave-engine";
    private static final String SUBJECT = "user-10001";
    private static final SecretKey VERIFICATION_KEY = Jwts.SIG.HS256.key().build();
    private static final int MAXIMUM_TOKEN_LENGTH = 4096;

    @Test
    void shouldReturnAuthenticatedPrincipalForValidToken() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        String token = signedToken(VERIFICATION_KEY, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().plusSeconds(300), null);

        // When
        var principal = verifier.authenticate(token);

        // Then
        assertThat(principal).isPresent().get().extracting("subject").isEqualTo(SUBJECT);
    }

    @Test
    void shouldAllowConfiguredClockSkew() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ofSeconds(5));
        String token = signedToken(VERIFICATION_KEY, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().minusSeconds(1), null);

        // When & Then
        assertThat(verifier.authenticate(token)).isPresent();
    }

    @Test
    void shouldRejectTamperedTokenOrWrongVerificationKey() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        SecretKey wrongKey = Jwts.SIG.HS256.key().build();
        String validToken = signedToken(VERIFICATION_KEY, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().plusSeconds(300), null);
        String tamperedToken = tamperSignature(validToken);
        String wrongKeyToken = signedToken(wrongKey, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().plusSeconds(300), null);

        // When & Then
        assertThat(verifier.authenticate(tamperedToken)).isEmpty();
        assertThat(verifier.authenticate(wrongKeyToken)).isEmpty();
    }

    @Test
    void shouldRejectExpiredOrNotYetValidToken() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        String expiredToken = signedToken(VERIFICATION_KEY, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().minusSeconds(30), null);
        String futureToken = signedToken(VERIFICATION_KEY, ISSUER, AUDIENCE, SUBJECT,
                Instant.now().plusSeconds(300), Instant.now().plusSeconds(30));

        // When & Then
        assertThat(verifier.authenticate(expiredToken)).isEmpty();
        assertThat(verifier.authenticate(futureToken)).isEmpty();
    }

    @Test
    void shouldRejectMissingExpirationOrSubject() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        String missingExpiration = baseBuilder(ISSUER, AUDIENCE, SUBJECT)
                .signWith(VERIFICATION_KEY, Jwts.SIG.HS256)
                .compact();
        String missingSubject = baseBuilder(ISSUER, AUDIENCE, null)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(VERIFICATION_KEY, Jwts.SIG.HS256)
                .compact();

        // When & Then
        assertThat(verifier.authenticate(missingExpiration)).isEmpty();
        assertThat(verifier.authenticate(missingSubject)).isEmpty();
    }

    @Test
    void shouldRejectWrongIssuerOrAudience() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        String wrongIssuer = signedToken(VERIFICATION_KEY, "other-issuer", AUDIENCE, SUBJECT,
                Instant.now().plusSeconds(300), null);
        String wrongAudience = signedToken(VERIFICATION_KEY, ISSUER, "other-audience", SUBJECT,
                Instant.now().plusSeconds(300), null);

        // When & Then
        assertThat(verifier.authenticate(wrongIssuer)).isEmpty();
        assertThat(verifier.authenticate(wrongAudience)).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedOrUnsecuredAlgorithm() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);
        SecretKey hs384Key = Jwts.SIG.HS384.key().build();
        String hs384Token = baseBuilder(ISSUER, AUDIENCE, SUBJECT)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(hs384Key, Jwts.SIG.HS384)
                .compact();
        String unsecuredToken = baseBuilder(ISSUER, AUDIENCE, SUBJECT)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .compact();

        // When & Then
        assertThat(verifier.authenticate(hs384Token)).isEmpty();
        assertThat(verifier.authenticate(unsecuredToken)).isEmpty();
    }

    @Test
    void shouldRejectBlankMalformedOrOversizedToken() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);

        // When & Then
        assertThat(verifier.authenticate(" ")).isEmpty();
        assertThat(verifier.authenticate("not-a-jwt")).isEmpty();
        assertThat(verifier.authenticate("a".repeat(MAXIMUM_TOKEN_LENGTH + 1))).isEmpty();
    }

    @Test
    void shouldRejectMissingPolicyOrToken() {
        // Given
        JwtTokenVerifier verifier = verifier(Duration.ZERO);

        // When & Then
        assertThatThrownBy(() -> new JwtTokenVerifier(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("policy must not be null");
        assertThatThrownBy(() -> verifier.authenticate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("token must not be null");
    }

    private static JwtTokenVerifier verifier(Duration clockSkew) {
        JwtVerificationPolicy policy = new JwtVerificationPolicy(
                VERIFICATION_KEY, ISSUER, AUDIENCE, clockSkew, MAXIMUM_TOKEN_LENGTH);
        return new JwtTokenVerifier(policy);
    }

    private static String signedToken(SecretKey key, String issuer, String audience, String subject,
            Instant expiration, Instant notBefore) {

        JwtBuilder builder = baseBuilder(issuer, audience, subject).expiration(Date.from(expiration));
        if (notBefore != null) {
            builder.notBefore(Date.from(notBefore));
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    private static JwtBuilder baseBuilder(String issuer, String audience, String subject) {
        JwtBuilder builder = Jwts.builder().issuer(issuer).audience().add(audience).and();
        if (subject != null) {
            builder.subject(subject);
        }
        return builder;
    }

    private static String tamperSignature(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char firstCharacter = token.charAt(signatureStart);
        char replacement = firstCharacter == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    }
}
