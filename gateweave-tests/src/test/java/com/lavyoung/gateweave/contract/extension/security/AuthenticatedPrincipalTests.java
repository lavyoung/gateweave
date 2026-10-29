package com.lavyoung.gateweave.contract.extension.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedPrincipalTests {

    @Test
    void shouldCreateAuthenticatedPrincipal() {
        // When
        var principal = new AuthenticatedPrincipal("user-10001");

        // Then
        assertThat(principal.subject()).isEqualTo("user-10001");
    }

    @Test
    void shouldRejectMissingOrBlankSubject() {
        // When & Then
        assertThatThrownBy(() -> new AuthenticatedPrincipal(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null");
        assertThatThrownBy(() -> new AuthenticatedPrincipal(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subject must not be blank");
    }
}
