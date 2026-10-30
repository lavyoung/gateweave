package com.lavyoung.gateweave.plugin.invoker.http;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpInvocationOptionsTests {

    @Test
    void shouldParseAndNormalizeMethodFromTargetAttributes() {
        // When
        var options = HttpInvocationOptions.from(Map.of("method", "post"));

        // Then
        assertThat(options.method()).isEqualTo("POST");
    }

    @Test
    void shouldRejectMissingAttributesOrMethod() {
        // When & Then
        assertThatThrownBy(() -> HttpInvocationOptions.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attributes must not be null");
        assertThatThrownBy(() -> HttpInvocationOptions.from(Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("method must not be null");
    }

    @Test
    void shouldRejectBlankMethod() {
        // When & Then
        assertThatThrownBy(() -> HttpInvocationOptions.from(Map.of("method", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("method must not be blank");
    }
}
