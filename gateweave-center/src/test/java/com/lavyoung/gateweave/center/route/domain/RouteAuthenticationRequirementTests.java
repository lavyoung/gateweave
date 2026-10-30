package com.lavyoung.gateweave.center.route.domain;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;

import static org.assertj.core.api.Assertions.assertThat;

class RouteAuthenticationRequirementTests {

    @Test
    void shouldExposeStablePersistenceValues() {
        // When & Then
        assertThat(RouteAuthenticationRequirement.values()).containsExactly(
                RouteAuthenticationRequirement.ANONYMOUS, RouteAuthenticationRequirement.REQUIRED);
        assertThat(RouteAuthenticationRequirement.valueOf("ANONYMOUS"))
                .isEqualTo(RouteAuthenticationRequirement.ANONYMOUS);
        assertThat(RouteAuthenticationRequirement.valueOf("REQUIRED"))
                .isEqualTo(RouteAuthenticationRequirement.REQUIRED);
    }

    @Test
    void shouldRemainIndependentFromPublishedContractType() {
        // When & Then
        assertThat(RouteAuthenticationRequirement.class).isNotEqualTo(RouteAuthenticationMode.class);
        assertThat(RouteAuthenticationRequirement.REQUIRED.name()).isEqualTo(RouteAuthenticationMode.REQUIRED.name());
    }
}
