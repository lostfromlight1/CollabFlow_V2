package com.collabflow.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Unified error envelope for every Java service API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        String errorCode,
        String path,
        Instant timestamp,
        List<FieldErrorItem> errors
) {
    public record FieldErrorItem(String field, String message) {}

    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(false, message, errorCode, path, Instant.now(), null);
    }

    public static ErrorResponse of(
            String errorCode,
            String message,
            String path,
            List<FieldErrorItem> errors
    ) {
        return new ErrorResponse(false, message, errorCode, path, Instant.now(), errors);
    }
}
