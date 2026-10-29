package com.lavyoung.gateweave.engine.domain.route;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteAuthenticationPolicyTests {

    @Test
    void shouldDefineAnonymousAndRequiredPolicies() {
        // When & Then
        assertThat(RouteAuthenticationPolicy.values())
                .containsExactly(RouteAuthenticationPolicy.ANONYMOUS, RouteAuthenticationPolicy.REQUIRED);
    }
}
