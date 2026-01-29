# Account Activation API

Public endpoints for user account activation after registration.

**Base URL:** `/api/auth`

**Authentication:** Not required (public endpoints)

---

## 1. Activate Account

Verify and activate a user account using the activation token from the registration email.

### Endpoint

```
GET /api/auth/account-activation
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `token` | String | Yes | JWT activation token from the registration email |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Account verified successfully. You can now log in.",
  "data": null
}
```

### Error Responses

| Status | Error Code | Message |
|--------|------------|---------|
| 400 | `VERIFICATION_ERROR` | Activation token is required |
| 400 | `VERIFICATION_ERROR` | Invalid or expired activation token |
| 400 | `VERIFICATION_ERROR` | Invalid token type for account verification |
| 400 | `VERIFICATION_ERROR` | User not found |
| 400 | `VERIFICATION_ERROR` | Account is already verified |

### Example

```bash
curl -X GET "https://api.example.com/api/auth/account-activation?token=eyJhbGciOiJIUzI1NiJ9..."
```

---

## 2. Resend Account Activation Email

Request a new activation email. This endpoint is designed to prevent email enumeration - it always returns success regardless of whether the email exists or the account status.

### Endpoint

```
POST /api/auth/resend-account-activation
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
  "message": "Verification email sent successfully",
  "data": null
}
```

### Error Responses

| Status | Error Code | Message |
|--------|------------|---------|
| 400 | `RESEND_VERIFICATION_ERROR` | Email is required |

### Example

```bash
curl -X POST "https://api.example.com/api/auth/resend-account-activation?email=user@example.com"
```

### Security Note

This endpoint always returns success to prevent email enumeration attacks. The actual email is only sent if:
- The email exists in the system
- The account is not yet verified
- The account is not locked

---

## Flow

1. User registers via `POST /api/auth/register`
2. System sends activation email with token
3. User clicks activation link → `GET /api/auth/account-activation?token=...`
4. If token expired, user requests new email → `POST /api/auth/resend-account-activation?email=...`
5. Account activated → User can log in
