package com.lavyoung.gateweave.plugin.security.jwt;

import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtVerificationPolicyTests {

    private static final SecretKey VERIFICATION_KEY =
            new SecretKeySpec(new byte[32], "HmacSHA256");

    @Test
    void shouldCreateJwtVerificationPolicy() {
        // When
        var policy = new JwtVerificationPolicy(
                VERIFICATION_KEY, "gateweave-issuer", "gateweave-engine", Duration.ZERO, 4096);

        // Then
        assertThat(policy.verificationKey()).isSameAs(VERIFICATION_KEY);
        assertThat(policy.issuer()).isEqualTo("gateweave-issuer");
        assertThat(policy.audience()).isEqualTo("gateweave-engine");
        assertThat(policy.clockSkew()).isZero();
        assertThat(policy.maximumTokenLength()).isEqualTo(4096);
    }

    @Test
    void shouldRejectMissingReferenceProperties() {
        // When & Then
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                (SecretKey) null, "issuer", "audience", Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("verificationKey must not be null");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, null, "audience", Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("issuer must not be null");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", null, Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("audience must not be null");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", "audience", null, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clockSkew must not be null");
    }

    @Test
    void shouldCreatePolicyFromStrongHs256KeyBytes() {
        // Given
        byte[] verificationKeyBytes = new byte[32];

        // When
        var policy = new JwtVerificationPolicy(
                verificationKeyBytes, "gateweave-issuer", "gateweave-engine", Duration.ZERO, 4096);

        // Then
        assertThat(policy.verificationKey().getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(policy.verificationKey().getEncoded()).containsExactly(verificationKeyBytes);
    }

    @Test
    void shouldRejectMissingOrWeakHs256KeyBytes() {
        // When & Then
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                (byte[]) null, "issuer", "audience", Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("verificationKeyBytes must not be null");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                new byte[31], "issuer", "audience", Duration.ZERO, 4096))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    void shouldRejectBlankIssuerOrAudience() {
        // When & Then
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, " ", "audience", Duration.ZERO, 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("issuer must not be blank");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", " ", Duration.ZERO, 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("audience must not be blank");
    }

    @Test
    void shouldRejectNegativeClockSkewOrNonPositiveMaximumLength() {
        // When & Then
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", "audience", Duration.ofSeconds(-1), 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clockSkew must not be negative");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", "audience", Duration.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumTokenLength must be greater than zero");
        assertThatThrownBy(() -> new JwtVerificationPolicy(
                VERIFICATION_KEY, "issuer", "audience", Duration.ZERO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumTokenLength must be greater than zero");
    }
}
