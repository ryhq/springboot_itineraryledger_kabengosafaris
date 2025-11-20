# Global Response Handling Guide

## Overview

This document explains how to use the standardized response handling system across the Kabengo Safaris application.

## Components

### 1. ApiResponse<T>
A generic wrapper class that standardizes all API responses.

**Location**: `src/main/java/com/itineraryledger/kabengosafaris/Response/ApiResponse.java`

**Features**:
- Wraps all response data
- Includes status code, message, and timestamp
- Supports validation error details
- Type-safe with generics

### 2. ErrorCode Enum
Enumeration of all possible error codes in the system.

**Location**: `src/main/java/com/itineraryledger/kabengosafaris/Response/ErrorCode.java`

**Categories**:
- Validation Errors (400)
- Authentication & Authorization (401/403)
- Conflict Errors (409)
- Not Found Errors (404)
- Business Logic Errors (400/409)
- Server Errors (500)
- Configuration Errors (500)
- Rate Limiting (429)

### 3. GlobalExceptionHandler
Centralized exception handling for all endpoints.

**Location**: `src/main/java/com/itineraryledger/kabengosafaris/Response/GlobalExceptionHandler.java`

**Handles**:
- Custom business exceptions
- Spring validation exceptions
- HTTP-related exceptions
- Generic exceptions

---

## Usage Examples

### Success Responses

#### Without Data
```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegistrationRequest request) {
    registrationService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(201, "User registered successfully"));
}
```

#### With Data
```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<User>> register(@RequestBody RegistrationRequest request) {
    User user = registrationService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(201, "User registered successfully", user));
}
```

#### List Response
```java
@GetMapping("/users")
public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(
        ApiResponse.success(200, "Users retrieved successfully", users)
    );
}
```

### Error Responses

#### Automatic Exception Handling
Exceptions thrown in controllers are automatically caught and converted to `ApiResponse` format:

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<User>> register(@RequestBody RegistrationRequest request) {
    // If registrationService.registerUser() throws RegistrationException,
    // GlobalExceptionHandler catches it and returns:
    // {
    //   "success": false,
    //   "statusCode": 400,
    //   "message": "Email already registered",
    //   "errorCode": "DUPLICATE_EMAIL",
    //   "timestamp": "2025-11-19T10:30:45"
    // }
    User user = registrationService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(201, "User registered successfully", user));
}
```

#### Manual Error Response
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
    try {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(
            ApiResponse.success(200, "User retrieved successfully", user)
        );
    } catch (Exception e) {
        ApiResponse<User> response = ApiResponse.error(
            HttpStatus.NOT_FOUND.value(),
            "User not found",
            ErrorCode.USER_NOT_FOUND.getCode()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
```

---

## Response Examples

### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "timestamp": "2025-11-19T10:30:45"
}
```

### Success Response with List (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com"
    },
    {
      "id": 2,
      "username": "jane_smith",
      "email": "jane@example.com"
    }
  ],
  "timestamp": "2025-11-19T10:30:45"
}
```

### Success Response without Data (201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "User registered successfully",
  "timestamp": "2025-11-19T10:30:45"
}
```

### Error Response - Validation (400 Bad Request)
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "password",
      "message": "Password must contain at least one uppercase letter"
    },
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ],
  "timestamp": "2025-11-19T10:30:45"
}
```

### Error Response - Not Found (404)
```json
{
  "success": false,
  "statusCode": 404,
  "message": "User not found",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2025-11-19T10:30:45"
}
```

### Error Response - Conflict (409)
```json
{
  "success": false,
  "statusCode": 409,
  "message": "Email already registered",
  "errorCode": "DUPLICATE_EMAIL",
  "timestamp": "2025-11-19T10:30:45"
}
```

### Error Response - Server Error (500)
```json
{
  "success": false,
  "statusCode": 500,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "timestamp": "2025-11-19T10:30:45"
}
```

---

## Adding Custom Exceptions

To add a custom exception to the global handler:

### 1. Create Exception Class
```java
public static class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
```

### 2. Add Handler in GlobalExceptionHandler
```java
@ExceptionHandler(CustomException.class)
public ResponseEntity<ApiResponse<Void>> handleCustomException(
        CustomException ex,
        WebRequest request) {

    log.warn("Custom exception: {}", ex.getMessage());

    ApiResponse<Void> response = ApiResponse.error(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        ErrorCode.BUSINESS_ERROR.getCode()
    );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
```

### 3. Add Error Code (if needed)
```java
// Add to ErrorCode enum
CUSTOM_ERROR("CUSTOM_ERROR", "Custom error occurred")
```

---

## Best Practices

### 1. Always Use ApiResponse for Success
```java
// Good
return ResponseEntity.ok(ApiResponse.success(200, "Success", data));

// Avoid
return ResponseEntity.ok(data);
```

### 2. Throw Exceptions for Errors
```java
// Good - Exception will be caught by GlobalExceptionHandler
if (user == null) {
    throw new RegistrationException("User not found");
}

// Avoid - Manual error response when exception can be thrown
if (user == null) {
    return ResponseEntity.notFound().build();
}
```

### 3. Use Appropriate Error Codes
```java
// Good - Specific error code
throw new RegistrationException("Email already registered"); // DUPLICATE_EMAIL

// Avoid - Generic error code
throw new RegistrationException("Invalid"); // VALIDATION_ERROR
```

### 4. Log Appropriately
- Use `log.warn()` for expected errors (invalid input, duplicate entries)
- Use `log.error()` for unexpected errors (database failures, null pointers)

### 5. Include Meaningful Messages
```java
// Good
throw new RegistrationException("Password must be at least 8 characters");

// Avoid
throw new RegistrationException("Invalid password");
```

---

## HTTP Status Codes

| Status | Use Case | Example ErrorCode |
|--------|----------|-------------------|
| 200 | Successful GET | SUCCESS |
| 201 | Successful POST | RESOURCE_CREATED |
| 204 | Successful DELETE/PUT (no content) | SUCCESS |
| 400 | Bad Request/Validation | VALIDATION_ERROR |
| 401 | Unauthorized | UNAUTHORIZED |
| 403 | Forbidden | FORBIDDEN |
| 404 | Not Found | RESOURCE_NOT_FOUND |
| 409 | Conflict | DUPLICATE_ENTRY |
| 429 | Rate Limited | RATE_LIMIT_EXCEEDED |
| 500 | Server Error | INTERNAL_SERVER_ERROR |

---

## Configuration

No additional configuration required. The `@RestControllerAdvice` annotation automatically applies to all `@RestController` classes.

### Optional: Enable Stack Traces (Development Only)
```properties
# application.properties
server.error.include-stacktrace=always
```

---

## Testing

### Test Success Response
```java
@Test
public void testRegistrationSuccess() {
    RegistrationRequest request = new RegistrationRequest(...);

    ResponseEntity<ApiResponse<User>> response =
        restTemplate.postForEntity("/api/users/register", request, ApiResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().isSuccess());
    assertEquals("User registered successfully", response.getBody().getMessage());
}
```

### Test Error Response
```java
@Test
public void testRegistrationDuplicateEmail() {
    RegistrationRequest request = new RegistrationRequest(...);

    ResponseEntity<ApiResponse<Void>> response =
        restTemplate.postForEntity("/api/users/register", request, ApiResponse.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertFalse(response.getBody().isSuccess());
    assertEquals("DUPLICATE_EMAIL", response.getBody().getErrorCode());
}
```

---

## Troubleshooting

### Exception Not Being Caught
- Ensure the exception handler method has the correct `@ExceptionHandler` annotation
- Check that the exception type matches exactly
- Verify the class is annotated with `@RestControllerAdvice`

### Response Format Incorrect
- Check that controller methods return `ResponseEntity<ApiResponse<?>>`
- Verify `ApiResponse` methods are used correctly
- Ensure timestamp is being set

### Error Code Not Recognized
- Add the error code to the `ErrorCode` enum
- Update the exception handler to return the new error code
- Document the new error code in this guide

---

## Future Enhancements

- [ ] Internationalization (i18n) support for messages
- [ ] Custom exception mappers
- [ ] Response compression
- [ ] Rate limiting integration
- [ ] Request/Response logging middleware
