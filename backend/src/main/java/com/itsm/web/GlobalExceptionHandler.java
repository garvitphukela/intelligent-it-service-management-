package com.itsm.web;

import com.itsm.exception.ItsmException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Translates every failure into one consistent JSON error shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(Instant timestamp, int status, String code, String message, String path, Map<String, String> fieldErrors) {}

    @ExceptionHandler(ItsmException.class)
    public ResponseEntity<ApiError> domain(ItsmException ex, HttpServletRequest req) {
        if (ex.getHttpStatus() >= 500) log.error("{}: {}", ex.getCode(), ex.getMessage(), ex);
        return build(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return build(400, "INVALID_INPUT", "Validation failed: " + String.join(", ", fields.keySet()), req, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> unreadable(Exception ex, HttpServletRequest req) {
        return build(400, "INVALID_INPUT", "Malformed request: " + rootMessage(ex), req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(400, "INVALID_INPUT", ex.getMessage(), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(409, "BUSINESS_RULE_VIOLATION", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest req) {
        return build(403, "ACCESS_DENIED", "You do not have permission to perform this operation", req, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concurrent(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(409, "CONCURRENT_MODIFICATION", "The incident was modified by someone else. Refresh and try again.", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(409, "CONFLICT", "The change conflicts with existing data", req, null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> database(DataAccessException ex, HttpServletRequest req) {
        log.error("Database failure", ex);
        return build(503, "DATABASE_FAILURE", "The database is temporarily unavailable. Please retry shortly.", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> notFound(NoResourceFoundException ex, HttpServletRequest req) {
        return build(404, "NOT_FOUND", "No such endpoint", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return build(500, "INTERNAL_ERROR", "An unexpected error occurred", req, null);
    }

    private static ResponseEntity<ApiError> build(int status, String code, String message, HttpServletRequest req, Map<String, String> fields) {
        return ResponseEntity.status(HttpStatus.valueOf(status))
                .body(new ApiError(Instant.now(), status, code, message, req.getRequestURI(), fields));
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        String m = c.getMessage();
        if (m == null) return t.getClass().getSimpleName();
        return m.length() > 200 ? m.substring(0, 200) : m;
    }
}
