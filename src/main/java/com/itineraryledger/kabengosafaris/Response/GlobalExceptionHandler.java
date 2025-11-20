package com.itineraryledger.kabengosafaris.Response;

import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST endpoints.
 *
 * This class handles exceptions across the entire application and returns
 * standardized API responses. It ensures consistent error handling and logging.
 *
 * Exception Handling Order:
 * 1. Custom business exceptions (RegistrationException, etc.)
 * 2. Spring validation exceptions
 * 3. HTTP-related exceptions
 * 4. Generic exceptions
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== Custom Business Exceptions ====================

    /**
     * Handle RegistrationException
     */
    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegistrationException(
            RegistrationException ex,
            WebRequest request) {

        log.warn("Registration exception: {}", ex.getMessage());

        String errorCode = determineRegistrationErrorCode(ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                errorCode
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                ErrorCode.INVALID_INPUT.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {

        log.warn("Illegal state exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                ErrorCode.INVALID_STATE.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // ==================== Spring Validation Exceptions ====================

    /**
     * Handle validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation exception: {}", ex.getBindingResult().getFieldError());

        List<ApiResponse.FieldError> fieldErrors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.add(ApiResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
        );

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                ErrorCode.VALIDATION_ERROR.getCode(),
                fieldErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle type mismatch in request parameters
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        log.warn("Type mismatch exception: {} - {}", ex.getName(), ex.getMessage());

        String message = String.format("Invalid type for parameter '%s': expected %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                ErrorCode.INVALID_INPUT.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle missing or malformed request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        log.warn("Request body not readable: {}", ex.getMessage());

        String message = "Required request body is missing or malformed";
        if (ex.getMessage() != null && ex.getMessage().contains("required")) {
            message = "Required request body is missing";
        }

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                ErrorCode.REQUIRED_FIELD_MISSING.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle unsupported HTTP request method
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        log.warn("HTTP method not supported: {} - Supported methods: {}",
                ex.getMethod(), ex.getSupportedHttpMethods());

        String supportedMethods = ex.getSupportedHttpMethods() != null
                ? String.join(", ", ex.getSupportedHttpMethods().stream()
                    .map(Object::toString)
                    .toArray(String[]::new))
                : "GET, POST, PUT, DELETE";

        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), supportedMethods);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message,
                ErrorCode.INVALID_INPUT.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // ==================== HTTP-Related Exceptions ====================

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            WebRequest request) {

        log.warn("Resource not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.NOT_FOUND.value(),
                "Endpoint not found",
                ErrorCode.RESOURCE_NOT_FOUND.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
            NullPointerException ex,
            WebRequest request) {

        log.error("Null pointer exception", ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                ErrorCode.INTERNAL_SERVER_ERROR.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Generic Exception Handler ====================

    /**
     * Catch-all exception handler for any unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex,
            WebRequest request) {

        log.error("Unhandled exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                ErrorCode.UNKNOWN_ERROR.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Helper Methods ====================

    /**
     * Determine error code based on registration exception message
     */
    private String determineRegistrationErrorCode(String message) {
        if (message == null) {
            return ErrorCode.VALIDATION_ERROR.getCode();
        }

        message = message.toLowerCase();

        if (message.contains("email")) {
            if (message.contains("already")) {
                return ErrorCode.DUPLICATE_EMAIL.getCode();
            } else if (message.contains("invalid")) {
                return ErrorCode.INVALID_EMAIL.getCode();
            }
        }

        if (message.contains("username")) {
            if (message.contains("already")) {
                return ErrorCode.DUPLICATE_USERNAME.getCode();
            } else if (message.contains("invalid")) {
                return ErrorCode.INVALID_USERNAME.getCode();
            }
        }

        if (message.contains("password")) {
            return ErrorCode.INVALID_PASSWORD.getCode();
        }

        if (message.contains("required")) {
            return ErrorCode.REQUIRED_FIELD_MISSING.getCode();
        }

        return ErrorCode.VALIDATION_ERROR.getCode();
    }
}
