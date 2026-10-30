package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayJwtPropertiesTests {

    private static final String ISSUER = "gateweave-issuer";
    private static final String AUDIENCE = "gateweave-engine";
    private static final String VERIFICATION_KEY_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Test
    void shouldBindApplicationYamlWithExternalVerificationKey() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "jwt-test", Map.of("GATEWEAVE_JWT_VERIFICATION_KEY_BASE64", VERIFICATION_KEY_BASE64)));
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.security.jwt", GatewayJwtProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.security.jwt properties must be present"));

        // Then
        assertThat(properties).isEqualTo(new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ofSeconds(30), 4096));
    }

    @Test
    void shouldCreateValidJwtProperties() {
        // When
        var properties = new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ofSeconds(30), 4096);

        // Then
        assertThat(properties.issuer()).isEqualTo(ISSUER);
        assertThat(properties.audience()).isEqualTo(AUDIENCE);
        assertThat(properties.verificationKeyBase64()).isEqualTo(VERIFICATION_KEY_BASE64);
        assertThat(properties.clockSkew()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.maximumTokenLength()).isEqualTo(4096);
    }

    @Test
    void shouldRejectMissingValues() {
        // When & Then
        assertThatThrownBy(() -> new GatewayJwtProperties(
                null, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("issuer must not be null");
        assertThatThrownBy(() -> new GatewayJwtProperties(
                ISSUER, null, VERIFICATION_KEY_BASE64, Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("audience must not be null");
        assertThatThrownBy(() -> new GatewayJwtProperties(ISSUER, AUDIENCE, null, Duration.ZERO, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("verificationKeyBase64 must not be null");
        assertThatThrownBy(() -> new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, null, 4096))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clockSkew must not be null");
    }

    @Test
    void shouldRejectBlankValues() {
        // When & Then
        assertThatThrownBy(() -> new GatewayJwtProperties(
                " ", AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ZERO, 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("issuer must not be blank");
        assertThatThrownBy(() -> new GatewayJwtProperties(
                ISSUER, " ", VERIFICATION_KEY_BASE64, Duration.ZERO, 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("audience must not be blank");
        assertThatThrownBy(() -> new GatewayJwtProperties(ISSUER, AUDIENCE, " ", Duration.ZERO, 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("verificationKeyBase64 must not be blank");
    }

    @Test
    void shouldRejectInvalidClockSkewOrTokenLength() {
        // When & Then
        assertThatThrownBy(() -> new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ofMillis(-1), 4096))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clockSkew must not be negative");
        assertThatThrownBy(() -> new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumTokenLength must be greater than zero");
    }

    @Test
    void shouldHideVerificationKeyFromStringRepresentation() {
        // Given
        var properties = new GatewayJwtProperties(
                ISSUER, AUDIENCE, VERIFICATION_KEY_BASE64, Duration.ZERO, 4096);

        // When
        String description = properties.toString();

        // Then
        assertThat(description).contains("verificationKeyBase64=PROTECTED");
        assertThat(description).doesNotContain(VERIFICATION_KEY_BASE64);
    }
}
