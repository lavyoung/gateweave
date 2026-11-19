package com.lavyoung.gateweave.center.shared.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTests {

    @Test
    void shouldCalculateOffsetFromOneBasedPageNumber() {
        PageRequest firstPage = new PageRequest(1, 20);
        PageRequest thirdPage = new PageRequest(3, 20);

        assertThat(firstPage.offset()).isZero();
        assertThat(thirdPage.offset()).isEqualTo(40);
    }

    @Test
    void shouldAcceptMaximumPageSize() {
        PageRequest request = new PageRequest(1, 100);

        assertThat(request.pageNumber()).isEqualTo(1);
        assertThat(request.pageSize()).isEqualTo(100);
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> new PageRequest(0, 20)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageNumber must be greater than zero");
        assertThatThrownBy(() -> new PageRequest(1, 0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be between 1 and 100");
        assertThatThrownBy(() -> new PageRequest(1, 101)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be between 1 and 100");
    }
}
