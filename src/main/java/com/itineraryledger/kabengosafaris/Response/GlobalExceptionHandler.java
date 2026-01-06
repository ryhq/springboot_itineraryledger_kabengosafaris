package com.itineraryledger.kabengosafaris.Response;

import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
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
 * 2. Database exceptions (Data integrity, constraints, truncation)
 * 3. Security exceptions (Authentication & Authorization)
 * 4. Spring validation exceptions
 * 5. HTTP-related exceptions
 * 6. Generic exceptions
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

    // ==================== Database Exceptions ====================

    /**
     * Handle DataIntegrityViolationException (constraint violations, unique key violations, etc.)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request) {

        log.error("Data integrity violation: {}", ex.getMessage(), ex);

        String message = "Database constraint violation occurred";
        String errorCode = ErrorCode.DATABASE_ERROR.getCode();

        // Check for specific constraint violations
        String exceptionMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        Throwable rootCause = ex.getRootCause();
        String rootMessage = rootCause != null && rootCause.getMessage() != null
            ? rootCause.getMessage().toLowerCase() : "";

        // Check for duplicate key violations
        if (exceptionMessage.contains("duplicate") || exceptionMessage.contains("unique") ||
            rootMessage.contains("duplicate") || rootMessage.contains("unique")) {
            message = "A record with this information already exists";
            errorCode = ErrorCode.DUPLICATE_ENTRY.getCode();
        }
        // Check for foreign key constraint violations
        else if (exceptionMessage.contains("foreign key") || rootMessage.contains("foreign key")) {
            message = "Cannot perform operation due to related records in the system";
            errorCode = ErrorCode.FOREIGN_KEY_VIOLATION.getCode();
        }
        // Check for NOT NULL constraint violations
        else if (exceptionMessage.contains("not null") || rootMessage.contains("not null")) {
            message = "Required field cannot be empty";
            errorCode = ErrorCode.REQUIRED_FIELD_MISSING.getCode();
        }
        // Check for data truncation (data too long for column)
        else if (exceptionMessage.contains("data truncation") || exceptionMessage.contains("data too long") ||
                 rootMessage.contains("data truncation") || rootMessage.contains("data too long")) {

            // Extract column name if possible
            String columnName = extractColumnName(rootMessage, exceptionMessage);
            message = columnName != null
                ? String.format("Data too long for field '%s'. Please provide shorter input.", columnName)
                : "Data too long for one or more fields. Please provide shorter input.";
            errorCode = ErrorCode.DATA_TOO_LONG.getCode();
        }

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                errorCode
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle DuplicateKeyException (specific duplicate key violations)
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(
            DuplicateKeyException ex,
            WebRequest request) {

        log.warn("Duplicate key violation: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.CONFLICT.value(),
                "A record with this information already exists",
                ErrorCode.DUPLICATE_ENTRY.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle SQLException (low-level database errors)
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Void>> handleSQLException(
            SQLException ex,
            WebRequest request) {

        log.error("SQL exception occurred: SQLState={}, ErrorCode={}, Message={}",
                ex.getSQLState(), ex.getErrorCode(), ex.getMessage(), ex);

        String message = "Database error occurred";
        String errorCode = ErrorCode.DATABASE_ERROR.getCode();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // Handle specific SQL error codes
        int sqlErrorCode = ex.getErrorCode();
        // String sqlState = ex.getSQLState();

        // MySQL/MariaDB error codes
        if (sqlErrorCode == 1062) { // Duplicate entry
            message = "A record with this information already exists";
            errorCode = ErrorCode.DUPLICATE_ENTRY.getCode();
            status = HttpStatus.CONFLICT;
        } else if (sqlErrorCode == 1406) { // Data too long
            String columnName = extractColumnName(ex.getMessage(), "");
            message = columnName != null
                ? String.format("Data too long for field '%s'. Please provide shorter input.", columnName)
                : "Data too long for one or more fields. Please provide shorter input.";
            errorCode = ErrorCode.DATA_TOO_LONG.getCode();
            status = HttpStatus.BAD_REQUEST;
        } else if (sqlErrorCode == 1451 || sqlErrorCode == 1452) { // Foreign key constraint
            message = "Cannot perform operation due to related records in the system";
            errorCode = ErrorCode.FOREIGN_KEY_VIOLATION.getCode();
            status = HttpStatus.BAD_REQUEST;
        } else if (sqlErrorCode == 1048) { // Column cannot be null
            message = "Required field cannot be empty";
            errorCode = ErrorCode.REQUIRED_FIELD_MISSING.getCode();
            status = HttpStatus.BAD_REQUEST;
        }

        ApiResponse<Void> response = ApiResponse.error(
                status.value(),
                message,
                errorCode
        );

        return new ResponseEntity<>(response, status);
    }

    /**
     * Handle generic DataAccessException (catch-all for database errors)
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(
            DataAccessException ex,
            WebRequest request) {

        log.error("Data access exception: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database operation failed. Please try again later.",
                ErrorCode.DATABASE_ERROR.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Security Exceptions ====================

    /**
     * Handle AuthorizationDeniedException (Spring Security 6.x)
     * This is thrown when @PreAuthorize checks fail
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex,
            WebRequest request) {

        log.warn("Authorization denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to perform this action",
                ErrorCode.INSUFFICIENT_PERMISSIONS.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle AccessDeniedException (Legacy Spring Security)
     * Fallback for older authorization exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to access this resource",
                ErrorCode.FORBIDDEN.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle AuthenticationException
     * This is thrown when authentication fails (invalid credentials, etc.)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication failed. Please check your credentials.",
                ErrorCode.UNAUTHORIZED.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
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

    /**
     * Extract column name from error message
     * Handles formats like: "Data too long for column 'column_name' at row 1"
     */
    private String extractColumnName(String... messages) {
        for (String message : messages) {
            if (message == null || message.isEmpty()) {
                continue;
            }

            // Try to extract column name from patterns like: "column 'name'" or "for column `name`"
            int columnIndex = message.indexOf("column");
            if (columnIndex != -1) {
                String afterColumn = message.substring(columnIndex);

                // Look for quoted column name
                int startQuote = -1;
                int endQuote = -1;

                // Try single quotes
                startQuote = afterColumn.indexOf("'");
                if (startQuote != -1) {
                    endQuote = afterColumn.indexOf("'", startQuote + 1);
                }

                // Try backticks if single quotes not found
                if (startQuote == -1) {
                    startQuote = afterColumn.indexOf("`");
                    if (startQuote != -1) {
                        endQuote = afterColumn.indexOf("`", startQuote + 1);
                    }
                }

                // Try double quotes if backticks not found
                if (startQuote == -1) {
                    startQuote = afterColumn.indexOf("\"");
                    if (startQuote != -1) {
                        endQuote = afterColumn.indexOf("\"", startQuote + 1);
                    }
                }

                if (startQuote != -1 && endQuote != -1 && endQuote > startQuote) {
                    String columnName = afterColumn.substring(startQuote + 1, endQuote);
                    // Convert snake_case to human-readable format
                    return columnName.replace("_", " ");
                }
            }
        }
        return null;
    }
}
