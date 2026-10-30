package com.lavyoung.gateweave.center.route.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteStatusTests {

    @Test
    void shouldExposeStablePersistenceValues() {
        // When & Then
        assertThat(RouteStatus.values()).containsExactly(RouteStatus.ENABLED, RouteStatus.DISABLED);
        assertThat(RouteStatus.valueOf("ENABLED")).isEqualTo(RouteStatus.ENABLED);
        assertThat(RouteStatus.valueOf("DISABLED")).isEqualTo(RouteStatus.DISABLED);
    }
}
