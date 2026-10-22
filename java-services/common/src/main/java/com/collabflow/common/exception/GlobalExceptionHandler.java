package com.collabflow.common.exception;

import com.collabflow.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Shared exception → {@link ErrorResponse} mapping. Each service must component-scan
 * {@code com.collabflow.common}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorItem> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toItem)
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        "Validation failed",
                        request.getRequestURI(),
                        fields
                ));
    }

    /**
     * Bad JSON / wrong types in the body (e.g. truncated UUID for {@code userId}).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = fieldName(ife);
            String expected = expectedTypeLabel(ife.getTargetType());
            String message = field != null
                    ? "Invalid value for '" + field + "': expected " + expected
                    : "Invalid request body: expected " + expected;
            List<ErrorResponse.FieldErrorItem> fields = field != null
                    ? List.of(new ErrorResponse.FieldErrorItem(field, "must be a valid " + expected))
                    : null;
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            ErrorCode.BAD_REQUEST.getCode(),
                            message,
                            request.getRequestURI(),
                            fields
                    ));
        }
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.getCode(),
                        "Malformed request body",
                        request.getRequestURI()
                ));
    }

    /**
     * Wrong types in path/query (e.g. {@code /organizations/{id}} with a non-UUID).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String name = ex.getName();
        String expected = expectedTypeLabel(ex.getRequiredType());
        String message = "Invalid value for '" + name + "': expected " + expected;
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.getCode(),
                        message,
                        request.getRequestURI(),
                        List.of(new ErrorResponse.FieldErrorItem(name, "must be a valid " + expected))
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorItem> fields = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorItem(
                        v.getPropertyPath() == null ? "value" : v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        "Validation failed",
                        request.getRequestURI(),
                        fields
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(ErrorCode.CONFLICT.getStatus())
                .body(ErrorResponse.of(
                        ErrorCode.CONFLICT.getCode(),
                        "Request conflicts with existing data",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.getCode(),
                        ex.getMessage() != null ? ex.getMessage() : "Bad request",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        "Unexpected server error",
                        request.getRequestURI()
                ));
    }

    private ErrorResponse.FieldErrorItem toItem(FieldError error) {
        return new ErrorResponse.FieldErrorItem(error.getField(), error.getDefaultMessage());
    }

    private static String fieldName(InvalidFormatException ex) {
        List<JsonMappingException.Reference> path = ex.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.get(path.size() - 1).getFieldName();
    }

    private static String expectedTypeLabel(Class<?> type) {
        if (type == null) {
            return "a valid value";
        }
        if (UUID.class.equals(type)) {
            return "UUID";
        }
        return type.getSimpleName();
    }
}
