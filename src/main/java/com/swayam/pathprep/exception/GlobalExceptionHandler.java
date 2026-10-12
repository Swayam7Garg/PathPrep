package com.swayam.pathprep.exception;

import com.swayam.pathprep.dto.ApiErrorResponse;
import com.swayam.pathprep.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("[{}] ApiException handled: code={} - {}", traceId, ex.getCode(), ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails(),
                null,
                traceId
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(fe.getField() + ": " + fe.getDefaultMessage())
        );
        log.warn("[{}] Request validation failed: {}", traceId, details);
        ApiErrorResponse body = new ApiErrorResponse(
                "VALIDATION_FAILED",
                "Validation failed for request",
                details,
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("[{}] Malformed JSON request payload: {}", traceId, ex.getMessage());
        String detailMessage = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        ApiErrorResponse body = new ApiErrorResponse(
                "MALFORMED_REQUEST",
                "Malformed JSON request or invalid enum value",
                List.of(detailMessage),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("[{}] Parameter type mismatch on parameter '{}': {}", traceId, ex.getName(), ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                "INVALID_PARAMETER",
                "Invalid parameter '" + ex.getName() + "': " + ex.getMessage(),
                List.of(),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("[{}] Illegal argument provided: {}", traceId, ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                "INVALID_PARAMETER",
                ex.getMessage(),
                List.of(),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        String causeMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("[{}] Data integrity violation: {}", traceId, causeMsg);
        ApiErrorResponse body = new ApiErrorResponse(
                "DATA_INTEGRITY_VIOLATION",
                "Database constraint violation: " + causeMsg,
                List.of(causeMsg),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("[{}] Resource or endpoint not found: {}", traceId, ex.getResourcePath());
        ApiErrorResponse body = new ApiErrorResponse(
                "ENDPOINT_NOT_FOUND",
                "Endpoint or resource not found: " + ex.getResourcePath() + " (check for trailing spaces in URL)",
                List.of(ex.getMessage()),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.error("[{}] Unhandled unexpected internal server error", traceId, ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "An unexpected error occurred (" + ex.getClass().getSimpleName() + ")";
        ApiErrorResponse body = new ApiErrorResponse(
                "INTERNAL_ERROR",
                message,
                List.of(ex.getClass().getName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "no message")),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getTraceId(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) {
            traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        }
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }
}
