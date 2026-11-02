package com.lavyoung.gateweave.center.route.persistence;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证路由属性 JSON 与持久化字符串属性之间的防腐转换。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
class RouteAttributesJsonReaderTests {

    private final RouteAttributesJsonReader reader = new RouteAttributesJsonReader(new ObjectMapper());

    @Test
    void shouldReadImmutableStringAttributes() {
        // Given
        String json = """
                {"version":"1.0.0","group":"production"}
                """;

        // When
        Map<String, String> attributes = this.reader.read(json, "route-1", "target_attributes");

        // Then
        assertThat(attributes).containsOnly(
                Map.entry("version", "1.0.0"), Map.entry("group", "production"));
        assertThatThrownBy(() -> attributes.put("timeout", "3000"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectMalformedJson() {
        // Given
        String json = "not-json";

        // When & Then
        assertThatThrownBy(() -> this.reader.read(json, "route-1", "target_attributes"))
                .isInstanceOf(DataRetrievalFailureException.class)
                .hasMessage("Unable to read target_attributes for route route-1")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectNullJsonValue() {
        // Given
        String json = "null";

        // When & Then
        assertThatThrownBy(() -> this.reader.read(json, "route-1", "route_attributes"))
                .isInstanceOf(DataRetrievalFailureException.class)
                .hasMessage("Unable to read route_attributes for route route-1")
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectMissingObjectMapper() {
        // When & Then
        assertThatThrownBy(() -> new RouteAttributesJsonReader(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("objectMapper must not be null");
    }
}
