package com.lavyoung.gateweave.center.route.application;

import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutePageQueryTests {

    @Test
    void shouldPreserveGatewayGroupAndPagination() {
        // Given
        PageRequest pageRequest = new PageRequest(3, 20);

        // When
        RoutePageQuery query = new RoutePageQuery("default", pageRequest);

        // Then
        assertThat(query.gatewayGroupId()).isEqualTo("default");
        assertThat(query.pageRequest()).isSameAs(pageRequest);
    }

    @Test
    void shouldRejectMissingGatewayGroup() {
        // When & Then
        PageRequest pageRequest = new PageRequest(1, 20);

        assertThatThrownBy(() -> new RoutePageQuery(null, pageRequest)).isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new RoutePageQuery(" ", pageRequest)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
        assertThatThrownBy(() -> new RoutePageQuery("default", null)).isInstanceOf(NullPointerException.class)
                .hasMessage("pageRequest must not be null");
    }
}
