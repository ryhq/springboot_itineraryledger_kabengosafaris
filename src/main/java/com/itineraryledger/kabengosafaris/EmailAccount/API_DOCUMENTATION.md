# Email Account API Documentation

## Overview
The Email Account API provides endpoints for managing email accounts used for sending emails. Each email account represents a unique SMTP configuration and can be tested, enabled/disabled, and set as the default account for sending operations.

---

## Base URL
```
/api/email-accounts
```

---

## Data Transfer Objects (DTOs)

### 1. CreateEmailAccountDTO (Request)
Used when creating a new email account. All fields are required.

```json
{
  "email": "string (email format)",
  "name": "string (unique account name)",
  "description": "string (optional - purpose of account)",
  "smtpHost": "string (e.g., smtp.gmail.com)",
  "smtpPort": "integer (1-65535, typically 25, 465, 587, 2525)",
  "smtpUsername": "string",
  "smtpPassword": "string (will be encrypted)",
  "useTls": "boolean",
  "useSsl": "boolean",
  "providerType": "integer (1=GMAIL, 2=OUTLOOK, 3=SENDGRID, 4=MAILGUN, 5=AWS_SES, 6=CUSTOM)",
  "rateLimitPerMinute": "integer (0=unlimited)",
  "maxRetryAttempts": "integer (minimum 1)",
  "retryDelaySeconds": "integer (minimum 1)"
}
```

**Example:**
```json
{
  "email": "noreply@example.com",
  "name": "Support Email",
  "description": "Used for support ticket notifications",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "noreply@example.com",
  "smtpPassword": "app_password_here",
  "useTls": true,
  "useSsl": false,
  "providerType": 1,
  "rateLimitPerMinute": 100,
  "maxRetryAttempts": 3,
  "retryDelaySeconds": 5
}
```

---

### 2. UpdateEmailAccountDTO (Request)
Used when updating an email account. All fields are optional - only include fields you want to update.

```json
{
  "email": "string (email format, optional)",
  "name": "string (optional)",
  "description": "string (optional)",
  "smtpHost": "string (optional)",
  "smtpPort": "integer (optional)",
  "smtpUsername": "string (optional)",
  "smtpPassword": "string (optional, will be encrypted)",
  "useTls": "boolean (optional)",
  "useSsl": "boolean (optional)",
  "enabled": "boolean (optional)",
  "isDefault": "boolean (optional)",
  "providerType": "integer (optional)",
  "rateLimitPerMinute": "integer (optional)",
  "maxRetryAttempts": "integer (optional)",
  "retryDelaySeconds": "integer (optional)"
}
```

**Example - Update Password:**
```json
{
  "smtpPassword": "new_password_here"
}
```

**Example - Enable Account:**
```json
{
  "enabled": true
}
```

---

### 3. EmailAccountDTO (Response)
Returned in API responses. Contains all account information except the password (which is always encrypted and never exposed).

```json
{
  "id": "string (obfuscated ID)",
  "email": "string",
  "name": "string",
  "description": "string",
  "smtpHost": "string",
  "smtpPort": "integer",
  "smtpUsername": "string",
  "useTls": "boolean",
  "useSsl": "boolean",
  "enabled": "boolean",
  "isDefault": "boolean",
  "providerType": "enum (GMAIL, OUTLOOK, SENDGRID, MAILGUN, AWS_SES, CUSTOM)",
  "rateLimitPerMinute": "integer",
  "maxRetryAttempts": "integer",
  "retryDelaySeconds": "integer",
  "verifyOnSave": "boolean",
  "lastTestedAt": "datetime (ISO 8601)",
  "lastErrorMessage": "string",
  "emailsSentCount": "long",
  "emailsFailedCount": "long",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
  "createdBy": "string",
  "updatedBy": "string"
}
```

**Example Response:**
```json
{
  "id": "encoded_id_xyz",
  "email": "noreply@example.com",
  "name": "Support Email",
  "description": "Used for support ticket notifications",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "noreply@example.com",
  "useTls": true,
  "useSsl": false,
  "enabled": true,
  "isDefault": true,
  "providerType": "GMAIL",
  "rateLimitPerMinute": 100,
  "maxRetryAttempts": 3,
  "retryDelaySeconds": 5,
  "verifyOnSave": false,
  "lastTestedAt": "2025-11-28T10:30:45",
  "lastErrorMessage": null,
  "emailsSentCount": 1250,
  "emailsFailedCount": 5,
  "createdAt": "2025-11-20T08:15:30",
  "updatedAt": "2025-11-28T10:30:45",
  "createdBy": "admin@example.com",
  "updatedBy": "admin@example.com"
}
```

---

## API Response Wrapper

All responses follow a standard format:

```json
{
  "success": "boolean",
  "statusCode": "integer (HTTP status)",
  "message": "string",
  "data": "object or array (optional)",
  "errorCode": "string (only on errors)",
  "timestamp": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Email Account
**POST** `/api/email-accounts`

Creates a new email account with SMTP configuration.

**Request Body:**
```json
{
  "email": "noreply@example.com",
  "name": "Support Email",
  "description": "Support notifications",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "noreply@example.com",
  "smtpPassword": "app_password",
  "useTls": true,
  "useSsl": false,
  "providerType": 1,
  "rateLimitPerMinute": 100,
  "maxRetryAttempts": 3,
  "retryDelaySeconds": 5
}
```

**Success Response (201):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Email account created successfully",
  "data": {
    "id": "encoded_id_123",
    "email": "noreply@example.com",
    "name": "Support Email",
    "enabled": false,
    "isDefault": false
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (400):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid"
    }
  ],
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 2. Get All Email Accounts
**GET** `/api/email-accounts`

Retrieves a paginated list of email accounts with optional filtering.

**Query Parameters:**
```
page=0 (default, 0-indexed)
size=10 (default)
enabled=true/false (optional)
isDefault=true/false (optional)
email=string (optional, partial match)
name=string (optional, partial match)
providerType=1-6 (optional)
smtpHost=string (optional, partial match)
hasErrors=true/false (optional)
sortDir=asc/desc (optional, default: asc)
```

**Example Request:**
```
GET /api/email-accounts?page=0&size=10&enabled=true&sortDir=desc
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email accounts retrieved successfully",
  "data": {
    "emailAccounts": [
      {
        "id": "encoded_id_1",
        "email": "noreply@example.com",
        "name": "Support Email",
        "enabled": true,
        "isDefault": true,
        "providerType": "GMAIL"
      },
      {
        "id": "encoded_id_2",
        "email": "alerts@example.com",
        "name": "Alert Email",
        "enabled": true,
        "isDefault": false,
        "providerType": "OUTLOOK"
      }
    ],
    "currentPage": 0,
    "totalItems": 2,
    "totalPages": 1
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 3. Get Single Email Account
**GET** `/api/email-accounts/{id}`

Retrieves details of a single email account by ID.

**Path Parameters:**
```
id = obfuscated email account ID (string)
```

**Example Request:**
```
GET /api/email-accounts/encoded_id_123
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email account retrieved successfully",
  "data": {
    "id": "encoded_id_123",
    "email": "noreply@example.com",
    "name": "Support Email",
    "description": "Support notifications",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "enabled": true,
    "isDefault": true,
    "lastTestedAt": "2025-11-28T10:30:45",
    "lastErrorMessage": null
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Email account not found",
  "errorCode": "EMAIL_ACCOUNT_NOT_FOUND",
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 4. Update Email Account
**PUT** `/api/email-accounts/{id}`

Updates specific fields of an email account. Only include fields you want to change.

**Path Parameters:**
```
id = obfuscated email account ID (string)
```

**Request Body (Example - update password):**
```json
{
  "smtpPassword": "new_password_here"
}
```

**Important Notes:**
- Sensitive field changes (password, SMTP config) automatically disable the account
- When changing sensitive fields, `enabled` is set to `false` and `isDefault` is set to `false`
- These require re-testing before re-enabling

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email account updated successfully",
  "data": {
    "id": "encoded_id_123",
    "email": "noreply@example.com",
    "name": "Support Email",
    "enabled": false,
    "isDefault": false,
    "lastErrorMessage": "Password changed - account disabled for security"
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (400):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Email already exists",
  "errorCode": "EMAIL_ALREADY_EXISTS",
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 5. Test Email Account
**POST** `/api/email-accounts/{id}/test`

Tests the SMTP connection and credentials. Uses retry logic based on account settings.

**Path Parameters:**
```
id = obfuscated email account ID (string)
```

**Request Body:**
```
(empty body)
```

**Example Request:**
```
POST /api/email-accounts/encoded_id_123/test
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email account tested successfully",
  "data": {
    "id": "encoded_id_123",
    "email": "noreply@example.com",
    "name": "Support Email",
    "enabled": true,
    "lastTestedAt": "2025-11-28T10:35:20",
    "lastErrorMessage": null
  },
  "timestamp": "2025-11-28T10:35:20"
}
```

**Error Response (400 - Connection Failed):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "SMTP connection test failed",
  "errorCode": "SMTP_TEST_FAILED",
  "data": {
    "id": "encoded_id_123",
    "lastErrorMessage": "SMTP test failed after 3 attempts: Connection timeout"
  },
  "timestamp": "2025-11-28T10:35:20"
}
```

---

### 6. Toggle Default Status
**POST** `/api/email-accounts/{id}/toggle-default?setAsDefault=true/false`

Sets or unsets an email account as the default account.

**Path Parameters:**
```
id = obfuscated email account ID (string)
```

**Query Parameters:**
```
setAsDefault=true|false (required)
  true  = Set this account as default (automatically unsets all others)
  false = Unset this account as default
```

**Validation:**
- Account must be `enabled=true` to set as default
- When setting as default, all other accounts are automatically set to `isDefault=false`
- Can unset default without any validation

**Example Request:**
```
POST /api/email-accounts/encoded_id_123/toggle-default?setAsDefault=true
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Default status updated successfully",
  "data": {
    "id": "encoded_id_123",
    "email": "noreply@example.com",
    "name": "Support Email",
    "isDefault": true,
    "enabled": true
  },
  "timestamp": "2025-11-28T10:35:20"
}
```

**Error Response (400 - Not Enabled):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot set as default: email account is not enabled",
  "errorCode": "ACCOUNT_NOT_ENABLED",
  "timestamp": "2025-11-28T10:35:20"
}
```

---

### 7. Delete Single Email Account
**DELETE** `/api/email-accounts/{id}`

Deletes a single email account by ID.

**Path Parameters:**
```
id = obfuscated email account ID (string)
```

**Validation:**
- Cannot delete if account is set as default (`isDefault=true`)
- If the account is default, returns 400 error with list of default account IDs

**Example Request:**
```
DELETE /api/email-accounts/encoded_id_123
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email accounts deleted successfully",
  "data": {
    "deletedIds": ["encoded_id_123"],
    "deletedCount": 1,
    "totalRequested": 1
  },
  "timestamp": "2025-11-28T10:40:15"
}
```

**Error Response (400 - Is Default):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete accounts: some are set as default",
  "errorCode": "CANNOT_DELETE_DEFAULT_ACCOUNTS",
  "timestamp": "2025-11-28T10:40:15"
}
```

---

### 8. Delete Multiple Email Accounts
**DELETE** `/api/email-accounts`

Deletes multiple email accounts in a single operation.

**Request Body:**
```json
[
  "encoded_id_1",
  "encoded_id_2",
  "encoded_id_3"
]
```

**Validation:**
- If ANY account in the list is set as default (`isDefault=true`), NO accounts are deleted
- Returns 400 error with list of default account IDs
- Atomic operation: all-or-nothing

**Example Request:**
```
DELETE /api/email-accounts
[
  "encoded_id_1",
  "encoded_id_2"
]
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Email accounts deleted successfully",
  "data": {
    "deletedIds": ["encoded_id_1", "encoded_id_2"],
    "deletedCount": 2,
    "totalRequested": 2
  },
  "timestamp": "2025-11-28T10:40:15"
}
```

**Error Response (400 - Has Default Accounts):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete accounts: some are set as default",
  "errorCode": "CANNOT_DELETE_DEFAULT_ACCOUNTS",
  "timestamp": "2025-11-28T10:40:15"
}
```

---

## Provider Types Reference

```
1 = GMAIL
2 = OUTLOOK
3 = SENDGRID
4 = MAILGUN
5 = AWS_SES
6 = CUSTOM
```

---

## Common SMTP Configurations

### Gmail
```
SMTP Host: smtp.gmail.com
SMTP Port: 587
Use TLS: true
Use SSL: false
Provider Type: 1
```

### Outlook / Office365
```
SMTP Host: smtp.office365.com
SMTP Port: 587
Use TLS: true
Use SSL: false
Provider Type: 2
```

### SendGrid
```
SMTP Host: smtp.sendgrid.net
SMTP Port: 587
Use TLS: true
Use SSL: false
Provider Type: 3
Username: apikey
```

### Mailgun
```
SMTP Host: smtp.mailgun.org
SMTP Port: 587
Use TLS: true
Use SSL: false
Provider Type: 4
```

---

## Error Codes

| Error Code | Status | Description |
|-----------|--------|-------------|
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `EMAIL_ALREADY_EXISTS` | 400 | Email address already registered |
| `NAME_ALREADY_EXISTS` | 400 | Account name already exists |
| `ACCOUNT_NOT_ENABLED` | 400 | Account is not enabled (required for some operations) |
| `CANNOT_DELETE_DEFAULT_ACCOUNTS` | 400 | Cannot delete default account(s) |
| `SMTP_TEST_FAILED` | 400 | SMTP connection test failed |
| `EMAIL_ACCOUNT_NOT_FOUND` | 404 | Email account not found |
| `EMAIL_ACCOUNTS_DELETE_FAILED` | 500 | Server error during deletion |

---

## Important Notes

1. **Password Encryption**: SMTP passwords are always encrypted before storage and never exposed in API responses
2. **Obfuscated IDs**: All email account IDs are obfuscated for security. Use the returned `id` field for subsequent requests
3. **Audit Logging**: All operations are logged for audit trail purposes
4. **Default Account**: Only one account can be set as default at a time
5. **Sensitive Changes**: Updating SMTP credentials automatically disables the account (requires re-testing)
6. **Atomic Deletions**: Batch delete operations are atomic (all succeed or all fail if any account is default)

---

## Example Workflow

### 1. Create an Email Account
```bash
curl -X POST http://localhost:8080/api/email-accounts \
  -H "Content-Type: application/json" \
  -d '{
    "email": "support@example.com",
    "name": "Support Team",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "support@example.com",
    "smtpPassword": "password123",
    "useTls": true,
    "useSsl": false,
    "providerType": 1,
    "rateLimitPerMinute": 50,
    "maxRetryAttempts": 3,
    "retryDelaySeconds": 5
  }'
```

### 2. Test the Email Account
```bash
curl -X POST http://localhost:8080/api/email-accounts/encoded_id_123/test
```

### 3. Set as Default
```bash
curl -X POST "http://localhost:8080/api/email-accounts/encoded_id_123/toggle-default?setAsDefault=true"
```

### 4. Retrieve All Accounts
```bash
curl -X GET "http://localhost:8080/api/email-accounts?page=0&size=10&enabled=true"
```

---

## Version
API Version: 1.0

Last Updated: 2025-11-28
