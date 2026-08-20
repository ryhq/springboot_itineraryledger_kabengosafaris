package com.itineraryledger.kabengosafaris.Response;

import com.itineraryledger.kabengosafaris.PdfDocument.Exceptions.PdfTemplateValidationException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
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
     * Handle PdfTemplateValidationException
     * Returns detailed validation errors for PDF template issues
     */
    @ExceptionHandler(PdfTemplateValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handlePdfTemplateValidationException(
            PdfTemplateValidationException ex,
            WebRequest request) {

        log.warn("PDF template validation failed: {} - Template: {}",
                ex.getMessage(), ex.getTemplateName());

        // Build detailed error response
        var validationDetails = new java.util.HashMap<String, Object>();
        validationDetails.put("templateName", ex.getTemplateName());

        // Convert validation errors to response format
        var errorDetails = ex.getErrors().stream()
                .map(error -> {
                    var detail = new java.util.HashMap<String, Object>();
                    detail.put("errorType", error.getErrorType());
                    detail.put("message", error.getMessage());
                    if (error.getLineNumber() != null) {
                        detail.put("lineNumber", error.getLineNumber());
                    }
                    if (error.getColumnNumber() != null) {
                        detail.put("columnNumber", error.getColumnNumber());
                    }
                    if (error.getSuggestion() != null) {
                        detail.put("suggestion", error.getSuggestion());
                    }
                    if (error.getContext() != null) {
                        detail.put("context", error.getContext());
                    }
                    return detail;
                })
                .toList();

        validationDetails.put("errors", errorDetails);
        validationDetails.put("errorCount", ex.getErrors().size());

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .errorCode(ErrorCode.PDF_TEMPLATE_VALIDATION_ERROR.getCode())
                .data(validationDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

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

        String permission = extractPermissionName(ex.getMessage());
        String message = permission != null
                ? String.format("You do not have the required permission: %s", permission)
                : "You do not have permission to perform this action";

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                message,
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

        String permission = extractPermissionName(ex.getMessage());
        String message = permission != null
                ? String.format("You do not have the required permission: %s", permission)
                : "You do not have permission to access this resource";

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                message,
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
     * Handle missing required request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        log.warn("Missing required parameter: {} (type: {})", ex.getParameterName(), ex.getParameterType());

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                ErrorCode.REQUIRED_FIELD_MISSING.getCode()
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
     * Handle MaxUploadSizeExceededException (file/request size exceeded)
     * Thrown when uploaded file or total request exceeds configured limits
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        log.warn("Upload size exceeded: {}", ex.getMessage());

        String message = "Upload size limit exceeded. Please reduce file size or upload fewer files at once.";

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                message,
                ErrorCode.REQUEST_SIZE_EXCEEDED.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handle MultipartException (multipart request parsing errors)
     * Thrown when multipart request cannot be parsed (size exceeded, malformed, etc.)
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(
            MultipartException ex,
            WebRequest request) {

        /*
         * The CAUSE, not just the message.
         *
         * "Failed to parse multipart servlet request" is what Spring says for every reason an upload
         * can die — a temp directory it cannot write, a size limit, a truncated body — and on its own
         * it sent two investigations down the wrong path. Whatever the browser is told, the log gets
         * the root cause.
         */
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        log.warn("Multipart exception: {} — caused by {}: {}",
            ex.getMessage(), root.getClass().getName(), root.getMessage());

        String message = "Failed to process file upload.";
        String errorCode = ErrorCode.INVALID_INPUT.getCode();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Check for size-related errors
        String exMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        Throwable cause = ex.getCause();
        String causeMessage = cause != null && cause.getMessage() != null
                ? cause.getMessage().toLowerCase() : "";

        if (exMessage.contains("size") || exMessage.contains("exceeded") ||
            causeMessage.contains("size") || causeMessage.contains("exceeded")) {
            message = "Upload size limit exceeded. Please reduce file size or upload fewer files at once.";
            errorCode = ErrorCode.REQUEST_SIZE_EXCEEDED.getCode();
            status = HttpStatus.PAYLOAD_TOO_LARGE;
        } else if (exMessage.contains("stream") || exMessage.contains("ended") ||
                   causeMessage.contains("stream") || causeMessage.contains("ended")) {
            message = "Upload interrupted. Please try again.";
        }

        ApiResponse<Void> response = ApiResponse.error(
                status.value(),
                message,
                errorCode
        );

        return new ResponseEntity<>(response, status);
    }

    /**
     * Handle AsyncRequestNotUsableException (client disconnection, broken pipe)
     * This is a non-error situation - the client simply disconnected before receiving the response.
     * Common causes: client timeout, user cancelled download, network interruption.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException ex,
            WebRequest request) {

        // Log at DEBUG level since this is expected behavior, not an error
        log.debug("Client disconnected before response completed: {}", ex.getMessage());

        // Return null - no point sending a response since the client is gone
        return null;
    }

    /**
     * Handle IOException (broken pipe, connection reset, etc.)
     * These typically occur when the client disconnects during response.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(
            IOException ex,
            WebRequest request) {

        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Check for client disconnection patterns
        if (message.contains("broken pipe") ||
            message.contains("connection reset") ||
            message.contains("connection abort") ||
            message.contains("client closed")) {

            // Log at DEBUG level - this is expected when clients disconnect
            log.debug("Client connection lost: {}", ex.getMessage());

            // Return null - no point sending a response
            return null;
        }

        // For other IO errors, treat as server error
        log.error("IO exception occurred: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An I/O error occurred while processing your request",
                ErrorCode.INTERNAL_SERVER_ERROR.getCode()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

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

    /**
     * Extract permission name from exception messages.
     * Handles patterns like:
     * - "Access Denied" with hasAuthority('PERM_READ_BOOKING_INQUIRY') in the cause
     * - "User does not have permission: PERM_READ_BOOKING_INQUIRY"
     * - "User cannot READ on BOOKING_INQUIRY"
     */
    private String extractPermissionName(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        // Pattern 1: hasAuthority('PERM_XXX') from @PreAuthorize
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("hasAuthority\\('(PERM_[A-Z_]+)'\\)")
                .matcher(message);
        if (matcher.find()) {
            return formatPermissionName(matcher.group(1));
        }

        // Pattern 2: "permission: PERM_XXX" from PermissionCheckAspect
        matcher = java.util.regex.Pattern
                .compile("permission:\\s*(PERM_[A-Z_]+)")
                .matcher(message);
        if (matcher.find()) {
            return formatPermissionName(matcher.group(1));
        }

        // Pattern 3: "cannot ACTION on ENTITY" from PermissionCheckAspect
        matcher = java.util.regex.Pattern
                .compile("cannot\\s+(\\w+)\\s+on\\s+(\\w+)")
                .matcher(message);
        if (matcher.find()) {
            return formatPermissionName("PERM_" + matcher.group(1).toUpperCase() + "_" + matcher.group(2).toUpperCase());
        }

        return null;
    }

    /**
     * Format a permission name like PERM_READ_BOOKING_INQUIRY into "Read Booking Inquiry"
     */
    private String formatPermissionName(String permName) {
        if (permName == null) return null;
        // Remove PERM_ prefix
        String clean = permName.startsWith("PERM_") ? permName.substring(5) : permName;
        // Split by underscore and title-case each word
        String[] parts = clean.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(parts[i].charAt(0));
            sb.append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
