package com.itineraryledger.kabengosafaris.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified API Response wrapper for all endpoints.
 *
 * This class standardizes all API responses across the application, making it easier
 * for clients to parse and handle responses consistently.
 *
 * Example Success Response:
 * {
 *   "success": true,
 *   "statusCode": 200,
 *   "message": "User registered successfully",
 *   "data": {...},
 *   "timestamp": "2025-11-19T10:30:45"
 * }
 *
 * Example Error Response:
 * {
 *   "success": false,
 *   "statusCode": 400,
 *   "message": "Password is too weak",
 *   "errorCode": "VALIDATION_ERROR",
 *   "errors": [{
 *     "field": "password",
 *     "message": "Must contain uppercase letters"
 *   }],
 *   "timestamp": "2025-11-19T10:30:45"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates whether the request was successful
     */
    private boolean success;

    /**
     * HTTP status code
     */
    private int statusCode;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * Response data (null for error responses)
     */
    private T data;

    /**
     * Error code for programmatic error handling
     */
    private String errorCode;

    /**
     * Detailed validation errors
     */
    private java.util.List<FieldError> errors;

    /**
     * Timestamp when response was generated
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a redirect response with data
     */
    public static <T> ApiResponse<T> redirect(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success(int statusCode, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(int statusCode, String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with validation errors
     */
    public static <T> ApiResponse<T> error(int statusCode, String message, String errorCode, java.util.List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Nested class for field-level validation errors
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        /**
         * Name of the field with error
         */
        private String field;

        /**
         * Error message for the field
         */
        private String message;
    }
}
