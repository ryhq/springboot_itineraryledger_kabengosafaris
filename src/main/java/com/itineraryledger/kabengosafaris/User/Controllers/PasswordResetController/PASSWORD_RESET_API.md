# Password Reset API

Public endpoints for password reset functionality.

**Base URL:** `/api/auth`

**Authentication:** Not required (public endpoints)

---

## 1. Request Password Reset (Forgot Password)

Request a password reset email. This endpoint is designed to prevent email enumeration - it always returns success regardless of whether the email exists.

### Endpoint

```
POST /api/auth/forgot-password
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | String | Yes | User's email address |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "If an account exists with this email, you will receive a password reset link shortly.",
  "data": null
}
```

### Error Responses

| Status | Error Code | Message |
|--------|------------|---------|
| 400 | `PASSWORD_RESET_ERROR` | Email is required |

### Example

```bash
curl -X POST "https://api.example.com/api/auth/forgot-password?email=user@example.com"
```

### Security Note

This endpoint always returns success to prevent email enumeration attacks. The actual email is only sent if:
- The email exists in the system
- The account is enabled
- The account is not locked

---

## 2. Reset Password

Reset the user's password using the token received via email.

### Endpoint

```
POST /api/auth/reset-password
```

### Request Body

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "newPassword": "NewSecurePassword123!"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `token` | String | Yes | JWT password reset token from the email |
| `newPassword` | String | Yes | New password (must meet security requirements) |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Password reset successfully. You can now log in with your new password.",
  "data": null
}
```

### Error Responses

| Status | Error Code | Message |
|--------|------------|---------|
| 400 | `PASSWORD_RESET_ERROR` | Password reset token is required |
| 400 | `PASSWORD_RESET_ERROR` | New password is required |
| 400 | `PASSWORD_RESET_ERROR` | Invalid or expired password reset token |
| 400 | `PASSWORD_RESET_ERROR` | Invalid token type for password reset |
| 400 | `PASSWORD_RESET_ERROR` | User not found |
| 400 | `PASSWORD_RESET_ERROR` | Password must be at least 8 characters |

### Example

```bash
curl -X POST "https://api.example.com/api/auth/reset-password" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "newPassword": "NewSecurePassword123!"
  }'
```

---

## Flow

1. User requests password reset via `POST /api/auth/forgot-password?email=...`
2. System sends password reset email with token (if email exists and account is valid)
3. User clicks reset link and enters new password
4. Client submits `POST /api/auth/reset-password` with token and new password
5. Password updated → User can log in with new password
