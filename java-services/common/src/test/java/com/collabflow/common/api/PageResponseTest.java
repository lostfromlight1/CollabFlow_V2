package com.collabflow.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    void ofComputesTotalPages() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 10, 25);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(25);
    }

    @Test
    void ofHandlesEmptySize() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 0, 0);
        assertThat(page.totalPages()).isZero();
    }
}
