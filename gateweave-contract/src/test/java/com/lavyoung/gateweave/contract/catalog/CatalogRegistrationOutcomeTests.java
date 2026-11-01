package com.lavyoung.gateweave.contract.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogRegistrationOutcomeTests {

    @Test
    void shouldExposeOnlyCreatedAndUnchangedOutcomes() {
        // When & Then
        assertThat(CatalogRegistrationOutcome.values()).containsExactly(
                CatalogRegistrationOutcome.CREATED, CatalogRegistrationOutcome.UNCHANGED);
    }
}
