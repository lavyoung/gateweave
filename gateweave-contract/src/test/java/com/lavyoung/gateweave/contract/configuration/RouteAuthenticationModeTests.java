package com.lavyoung.gateweave.contract.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteAuthenticationModeTests {

    @Test
    void shouldExposeStableConfigurationNames() {
        // When & Then
        assertThat(RouteAuthenticationMode.values())
                .containsExactly(RouteAuthenticationMode.ANONYMOUS, RouteAuthenticationMode.REQUIRED);
        assertThat(RouteAuthenticationMode.valueOf("ANONYMOUS")).isEqualTo(RouteAuthenticationMode.ANONYMOUS);
        assertThat(RouteAuthenticationMode.valueOf("REQUIRED")).isEqualTo(RouteAuthenticationMode.REQUIRED);
    }
}
