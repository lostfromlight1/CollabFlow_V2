package com.collabflow.common.exception;

import com.collabflow.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Shared exception → {@link ErrorResponse} mapping. Each service must component-scan
 * {@code com.collabflow.common}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
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
}
