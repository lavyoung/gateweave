package com.lavyoung.gateweave.center.shared.pagination;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTests {

    @Test
    void shouldCopyContentAndCalculateTotalPages() {
        List<String> content = new ArrayList<>(List.of("one"));

        PageResult<String> page = new PageResult<>(content, new PageRequest(2, 10), 21);
        content.add("two");

        assertThat(page.content()).containsExactly("one");
        assertThat(page.pageRequest()).isEqualTo(new PageRequest(2, 10));
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThatThrownBy(() -> page.content().add("three")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnZeroTotalPagesForEmptyResult() {
        PageResult<String> page = new PageResult<>(List.of(), new PageRequest(1, 20), 0);

        assertThat(page.totalPages()).isZero();
    }

    @Test
    void shouldRejectInvalidPageResult() {
        PageRequest request = new PageRequest(1, 1);

        assertThatThrownBy(() -> new PageResult<>(null, request, 0)).isInstanceOf(NullPointerException.class)
                .hasMessage("content must not be null");
        assertThatThrownBy(() -> new PageResult<>(List.of(), null, 0)).isInstanceOf(NullPointerException.class)
                .hasMessage("pageRequest must not be null");
        assertThatThrownBy(() -> new PageResult<>(List.of(), request, -1)).isInstanceOf(
                IllegalArgumentException.class).hasMessage("totalElements must not be negative");
        assertThatThrownBy(() -> new PageResult<>(List.of("one", "two"), request, 2)).isInstanceOf(
                IllegalArgumentException.class).hasMessage("content size must not exceed pageSize");
        assertThatThrownBy(() -> new PageResult<>(List.of("one"), request, 0)).isInstanceOf(
                IllegalArgumentException.class).hasMessage("content size must not exceed totalElements");
    }
}
