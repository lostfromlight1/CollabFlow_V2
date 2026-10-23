package com.collabflow.common.api;

import java.util.List;

/**
 * Shared pagination envelope for list APIs (auth/platform/task/user).
 *
 * @param content    page items
 * @param page       zero-based page index
 * @param size       page size
 * @param totalElements total matching rows
 * @param totalPages computed from totalElements / size
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
