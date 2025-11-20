package com.itineraryledger.kabengosafaris.Response;

/**
 * Enumeration of all standardized error codes used across the application.
 *
 * These codes help clients programmatically identify and handle different types of errors.
 * Each code maps to a specific category of error with a default message.
 */
public enum ErrorCode {

    // Validation Errors (400)
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    INVALID_INPUT("INVALID_INPUT", "Invalid input provided"),
    INVALID_EMAIL("INVALID_EMAIL", "Invalid email format"),
    INVALID_PASSWORD("INVALID_PASSWORD", "Password does not meet requirements"),
    INVALID_USERNAME("INVALID_USERNAME", "Invalid username"),
    REQUIRED_FIELD_MISSING("REQUIRED_FIELD_MISSING", "Required field is missing"),
    FIELD_TOO_SHORT("FIELD_TOO_SHORT", "Field value is too short"),
    FIELD_TOO_LONG("FIELD_TOO_LONG", "Field value is too long"),

    // Authentication & Authorization Errors (401/403)
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized access"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username or password"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "JWT token has expired"),
    TOKEN_INVALID("TOKEN_INVALID", "JWT token is invalid"),
    INVALID_TOKEN("INVALID_TOKEN", "Invalid or malformed token"),
    FORBIDDEN("FORBIDDEN", "Access forbidden"),
    INSUFFICIENT_PERMISSIONS("INSUFFICIENT_PERMISSIONS", "Insufficient permissions for this action"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is locked due to failed login attempts"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Account is disabled"),

    // Conflict Errors (409)
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "Email already registered"),
    DUPLICATE_USERNAME("DUPLICATE_USERNAME", "Username already exists"),
    DUPLICATE_ENTRY("DUPLICATE_ENTRY", "This entry already exists"),
    RESOURCE_ALREADY_EXISTS("RESOURCE_ALREADY_EXISTS", "Resource already exists"),

    // Not Found Errors (404)
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource not found"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", "Permission not found"),

    // Business Logic Errors (400/409)
    BUSINESS_ERROR("BUSINESS_ERROR", "Business logic error"),
    PASSWORD_WEAK("PASSWORD_WEAK", "Password is too weak"),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "Passwords do not match"),
    INVALID_STATE("INVALID_STATE", "Invalid state for this operation"),
    OPERATION_NOT_ALLOWED("OPERATION_NOT_ALLOWED", "This operation is not allowed"),
    EXPIRED_DATA("EXPIRED_DATA", "Data has expired"),

    // Server Errors (500)
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error"),
    DATABASE_ERROR("DATABASE_ERROR", "Database error occurred"),
    ENCRYPTION_ERROR("ENCRYPTION_ERROR", "Encryption/Decryption error"),
    EMAIL_SEND_ERROR("EMAIL_SEND_ERROR", "Failed to send email"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service is currently unavailable"),

    // Configuration Errors (500)
    CONFIG_ERROR("CONFIG_ERROR", "Configuration error"),
    INVALID_CONFIGURATION("INVALID_CONFIGURATION", "Invalid configuration"),

    // Rate Limiting (429)
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "Rate limit exceeded"),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "Too many requests"),

    // Generic Error
    UNKNOWN_ERROR("UNKNOWN_ERROR", "An unknown error occurred");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
