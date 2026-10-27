package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DubboReferenceOptionsTests {

    @Test
    void shouldParseVersionFromTargetAttributes() {
        // When
        var options = DubboReferenceOptions.from(Map.of("version", "1.0.0"));

        // Then
        assertThat(options.version()).isEqualTo("1.0.0");
    }

    @Test
    void shouldRejectMissingAttributesOrVersion() {
        // When & Then
        assertThatThrownBy(() -> DubboReferenceOptions.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
        assertThatThrownBy(() -> DubboReferenceOptions.from(Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("version must not be null");
    }

    @Test
    void shouldRejectBlankVersion() {
        // When & Then
        assertThatThrownBy(() -> DubboReferenceOptions.from(Map.of("version", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must not be blank");
    }
}
