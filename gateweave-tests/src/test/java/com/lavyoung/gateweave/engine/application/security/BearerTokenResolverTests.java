package com.lavyoung.gateweave.engine.application.security;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerTokenResolverTests {

    private final BearerTokenResolver resolver = new BearerTokenResolver();

    @Test
    void shouldResolveStandardBearerTokenIgnoringHeaderAndSchemeCase() {
        // Given
        HttpHeaders standardHeaders = headers(Map.of("Authorization", List.of("Bearer signed-jwt")));
        HttpHeaders lowercaseHeaders = headers(Map.of("authorization", List.of("bearer another-jwt")));

        // When & Then
        assertThat(this.resolver.resolve(standardHeaders)).contains("signed-jwt");
        assertThat(this.resolver.resolve(lowercaseHeaders)).contains("another-jwt");
    }

    @Test
    void shouldAllowOptionalWhitespaceAroundHeaderAndMultipleSchemeSpaces() {
        // Given
        HttpHeaders headers = headers(Map.of("Authorization", List.of("  Bearer    signed-jwt  ")));

        // When & Then
        assertThat(this.resolver.resolve(headers)).contains("signed-jwt");
    }

    @Test
    void shouldRejectMissingOrRepeatedAuthorizationHeader() {
        // Given
        HttpHeaders missingHeaders = headers(Map.of());
        HttpHeaders repeatedHeaders = headers(
                Map.of("Authorization", List.of("Bearer first-jwt", "Bearer second-jwt")));

        // When & Then
        assertThat(this.resolver.resolve(missingHeaders)).isEmpty();
        assertThat(this.resolver.resolve(repeatedHeaders)).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedOrMalformedAuthorizationHeader() {
        // Given
        HttpHeaders basicHeaders = headers(Map.of("Authorization", List.of("Basic credentials")));
        HttpHeaders missingTokenHeaders = headers(Map.of("Authorization", List.of("Bearer")));
        HttpHeaders tabSeparatorHeaders = headers(Map.of("Authorization", List.of("Bearer\tsigned-jwt")));
        HttpHeaders whitespaceTokenHeaders = headers(Map.of("Authorization", List.of("Bearer signed jwt")));

        // When & Then
        assertThat(this.resolver.resolve(basicHeaders)).isEmpty();
        assertThat(this.resolver.resolve(missingTokenHeaders)).isEmpty();
        assertThat(this.resolver.resolve(tabSeparatorHeaders)).isEmpty();
        assertThat(this.resolver.resolve(whitespaceTokenHeaders)).isEmpty();
    }

    @Test
    void shouldRejectNullHeaders() {
        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("headers must not be null");
    }

    private static HttpHeaders headers(Map<String, List<String>> valuesByName) {
        return HttpHeaders.of(valuesByName, (name, value) -> true);
    }
}
