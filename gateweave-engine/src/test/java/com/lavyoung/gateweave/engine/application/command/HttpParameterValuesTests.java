package com.lavyoung.gateweave.engine.application.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpParameterValuesTests {

    @Test
    void shouldCreateImmutableSnapshotsForRepeatedParameters() {
        // Given
        var tags = new ArrayList<>(List.of("java", "gateway"));
        var source = new HashMap<String, List<String>>();
        source.put("tag", tags);

        // When
        var parameters = new HttpParameterValues(source);
        tags.add("netty");
        source.put("page", List.of("1"));

        // Then
        assertThat(parameters.values()).containsOnly(Map.entry("tag", List.of("java", "gateway")));
        assertThatThrownBy(() -> parameters.values().put("page", List.of("1")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> parameters.values().get("tag").add("netty"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldCreateImmutableSnapshotForEmptyParameters() {
        // Given
        var source = new HashMap<String, List<String>>();

        // When
        var parameters = new HttpParameterValues(source);
        source.put("changed", List.of("true"));

        // Then
        assertThat(parameters.values()).isEmpty();
        assertThatThrownBy(() -> parameters.values().put("changed", List.of("true")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowEmptyTextValue() {
        // When
        var parameters = new HttpParameterValues(Map.of("name", List.of("")));

        // Then
        assertThat(parameters.values()).containsOnly(Map.entry("name", List.of("")));
    }

    @Test
    void shouldRejectNullOrBlankParameterName() {
        // Given
        var nullName = new HashMap<String, List<String>>();
        nullName.put(null, List.of("value"));

        // When & Then
        assertThatThrownBy(() -> new HttpParameterValues(nullName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("values must not contain null or blank names");
        assertThatThrownBy(() -> new HttpParameterValues(Map.of(" ", List.of("value"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("values must not contain null or blank names");
    }

    @Test
    void shouldRejectNullEmptyOrNullContainingValueList() {
        // Given
        var nullList = new HashMap<String, List<String>>();
        nullList.put("name", null);
        var nullValue = new ArrayList<String>();
        nullValue.add(null);

        // When & Then
        assertThatThrownBy(() -> new HttpParameterValues(nullList))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("values must not contain null or empty value lists");
        assertThatThrownBy(() -> new HttpParameterValues(Map.of("name", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("values must not contain null or empty value lists");
        assertThatThrownBy(() -> new HttpParameterValues(Map.of("name", nullValue)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("values must not contain null parameter values");
    }

    @Test
    void shouldRejectNullMap() {
        // When & Then
        assertThatThrownBy(() -> new HttpParameterValues(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("values must not be null");
    }
}
