package com.lavyoung.gateweave.center.route.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutePageQueryTests {

    @Test
    void shouldCalculateOffsetFromOneBasedPageNumber() {
        // Given
        RoutePageQuery firstPage = new RoutePageQuery("default", 1, 20);
        RoutePageQuery thirdPage = new RoutePageQuery("default", 3, 20);

        // When & Then
        assertThat(firstPage.offset()).isZero();
        assertThat(thirdPage.offset()).isEqualTo(40L);
    }

    @Test
    void shouldAcceptMaximumPageSize() {
        // When
        RoutePageQuery query = new RoutePageQuery("default", 1, 100);

        // Then
        assertThat(query.pageSize()).isEqualTo(100);
    }

    @Test
    void shouldRejectMissingGatewayGroup() {
        // When & Then
        assertThatThrownBy(() -> new RoutePageQuery(null, 1, 20))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("gatewayGroupId must not be null");
        assertThatThrownBy(() -> new RoutePageQuery(" ", 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayGroupId must not be blank");
    }

    @Test
    void shouldRejectInvalidPagination() {
        // When & Then
        assertThatThrownBy(() -> new RoutePageQuery("default", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageNumber must be greater than zero");
        assertThatThrownBy(() -> new RoutePageQuery("default", 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be between 1 and 100");
        assertThatThrownBy(() -> new RoutePageQuery("default", 1, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be between 1 and 100");
    }
}
