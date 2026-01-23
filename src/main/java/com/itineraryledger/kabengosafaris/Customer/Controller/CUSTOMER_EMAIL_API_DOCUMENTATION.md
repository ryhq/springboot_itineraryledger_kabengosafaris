# Customer Email API Documentation

## Overview

The Customer Email API provides endpoints for managing customer email addresses. Each customer can have multiple email addresses with different types and one designated as primary.

## Base URL

```
/api/customer-emails
```

## Authentication

All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_CUSTOMER_EMAIL` - Create customer emails
- `PERM_READ_CUSTOMER_EMAIL` - View customer emails
- `PERM_UPDATE_CUSTOMER_EMAIL` - Update customer emails
- `PERM_DELETE_CUSTOMER_EMAIL` - Delete customer emails

---

## Email Types

| Type | Display Name | Description |
|------|--------------|-------------|
| PERSONAL | Personal | Personal email address |
| WORK | Work | Work/business email address |
| BOOKING | Booking | Email for booking confirmations |
| BILLING | Billing | Email for invoices and billing |
| EMERGENCY | Emergency | Emergency contact email |
| OTHER | Other | Other email type |

---

## Endpoints

### 1. Create Customer Email

Creates a new email address for a customer.

**Endpoint:** `POST /api/customer-emails`

**Permission:** `PERM_CREATE_CUSTOMER_EMAIL`

**Request Body:**

```json
{
  "customerId": "abc123xyz",
  "email": "john.work@company.com",
  "emailType": "WORK",
  "isPrimary": false,
  "isActive": true,
  "label": "Work Email"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | String | Yes | Obfuscated customer ID |
| email | String | Yes | Valid email address (max 255 chars) |
| emailType | EmailType | Yes | Type of email (PERSONAL, WORK, etc.) |
| isPrimary | Boolean | No | Set as primary email (default: false) |
| isActive | Boolean | No | Active status (default: true) |
| label | String | No | Custom label (max 100 chars) |

**Notes:**
- Email addresses must be unique across the system
- Email format is validated using RFC 5322 standard
- If `isPrimary` is true, all other emails for the customer will be marked as non-primary

**Success Response (201):**

```json
{
  "status": 201,
  "message": "Customer email created successfully",
  "data": {
    "id": "email123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "email": "john.work@company.com",
    "emailType": "WORK",
    "emailTypeDisplayName": "Work",
    "emailTypeDescription": "Work/business email address",
    "isPrimary": false,
    "isActive": true,
    "label": "Work Email",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Responses:**

| Code | Error Code | Description |
|------|------------|-------------|
| 400 | INVALID_CUSTOMER_ID | Invalid or malformed customer ID |
| 400 | CUSTOMER_NOT_FOUND | Customer does not exist |
| 400 | DUPLICATE_EMAIL | Email already exists in the system |
| 400 | INVALID_EMAIL_FORMAT | Email format is invalid |
| 400 | EMAIL_TOO_LONG | Email exceeds 254 characters |

---

### 2. Update Customer Email

Updates an existing customer email.

**Endpoint:** `PUT /api/customer-emails/{idObfuscated}`

**Permission:** `PERM_UPDATE_CUSTOMER_EMAIL`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated email ID |

**Request Body:**

All fields are optional. Only provided fields will be updated.

```json
{
  "email": "john.new@company.com",
  "emailType": "WORK",
  "isPrimary": true,
  "isActive": true,
  "label": "Primary Work Email"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | No | New email address |
| emailType | EmailType | No | New email type |
| isPrimary | Boolean | No | Set as primary email |
| isActive | Boolean | No | Active status |
| label | String | No | Custom label |

**Notes:**
- At least one field must be provided for update
- If changing the email address, it must be unique
- If `isPrimary` is set to true, all other emails for the customer will be marked as non-primary

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer email updated successfully",
  "data": {
    "id": "email123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "email": "john.new@company.com",
    "emailType": "WORK",
    "emailTypeDisplayName": "Work",
    "isPrimary": true,
    "isActive": true,
    "label": "Primary Work Email",
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

---

### 3. Delete Customer Emails

Deletes one or more customer emails.

**Endpoint:** `DELETE /api/customer-emails`

**Permission:** `PERM_DELETE_CUSTOMER_EMAIL`

**Request Body:**

```json
["email123abc", "email456def", "email789ghi"]
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "3 customer email(s) deleted successfully",
  "data": null
}
```

---

### 4. Get Customer Email by ID

Retrieves a single customer email by its obfuscated ID.

**Endpoint:** `GET /api/customer-emails/{idObfuscated}`

**Permission:** `PERM_READ_CUSTOMER_EMAIL`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated email ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer email retrieved successfully",
  "data": {
    "id": "email123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "email": "john.doe@example.com",
    "emailType": "PERSONAL",
    "emailTypeDisplayName": "Personal",
    "emailTypeDescription": "Personal email address",
    "isPrimary": true,
    "isActive": true,
    "label": "Main Email",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (404):**

```json
{
  "status": 404,
  "message": "Customer email not found",
  "errorCode": "CUSTOMER_EMAIL_NOT_FOUND"
}
```

---

### 5. Get All Customer Emails

Retrieves a paginated list of customer emails with optional filtering.

**Endpoint:** `GET /api/customer-emails`

**Permission:** `PERM_READ_CUSTOMER_EMAIL`

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| customerId | String | null | Filter by customer ID (optional) |
| email | String | null | Filter by email address (partial match) |
| emailType | EmailType | null | Filter by email type |
| isPrimary | Boolean | null | Filter by primary status |
| isActive | Boolean | null | Filter by active status |
| label | String | null | Filter by label (partial match) |
| keyword | String | null | Search across email and label |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Example Request:**

```
GET /api/customer-emails?emailType=WORK&isActive=true&page=0&size=20
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer emails retrieved successfully",
  "data": {
    "emails": [
      {
        "id": "email123abc",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "email": "john.doe@example.com",
        "emailType": "WORK",
        "emailTypeDisplayName": "Work",
        "isPrimary": true,
        "isActive": true,
        "label": "Main Work Email"
      }
    ],
    "currentPage": 0,
    "totalItems": 50,
    "totalPages": 5,
    "pageSize": 10
  }
}
```

---

### 6. Get Emails for a Specific Customer

Retrieves all emails for a specific customer with optional filtering.

**Endpoint:** `GET /api/customer-emails/customer/{customerId}`

**Permission:** `PERM_READ_CUSTOMER_EMAIL`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| customerId | String | Required obfuscated customer ID |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| email | String | null | Filter by email address (partial match) |
| emailType | EmailType | null | Filter by email type |
| isPrimary | Boolean | null | Filter by primary status |
| isActive | Boolean | null | Filter by active status |
| label | String | null | Filter by label (partial match) |
| keyword | String | null | Search across email and label |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Example Request:**

```
GET /api/customer-emails/customer/abc123xyz?isPrimary=true
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer emails retrieved successfully",
  "data": {
    "emails": [
      {
        "id": "email123abc",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "email": "john.doe@example.com",
        "emailType": "PERSONAL",
        "isPrimary": true,
        "isActive": true
      },
      {
        "id": "email456def",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "email": "john.work@company.com",
        "emailType": "WORK",
        "isPrimary": false,
        "isActive": true
      }
    ],
    "currentPage": 0,
    "totalItems": 2,
    "totalPages": 1,
    "pageSize": 10
  }
}
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request

```json
{
  "status": 400,
  "message": "Validation error message",
  "errorCode": "ERROR_CODE"
}
```

Common error codes:
- `INVALID_EMAIL_ID` - Invalid obfuscated email ID
- `INVALID_CUSTOMER_ID` - Invalid customer ID
- `DUPLICATE_EMAIL` - Email already exists
- `INVALID_EMAIL_FORMAT` - Invalid email format
- `EMAIL_TOO_LONG` - Email exceeds maximum length
- `NO_FIELDS_TO_UPDATE` - No fields provided for update

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "Authentication required",
  "errorCode": "UNAUTHORIZED"
}
```

### 403 Forbidden

```json
{
  "status": 403,
  "message": "Access denied",
  "errorCode": "FORBIDDEN"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Customer email not found",
  "errorCode": "CUSTOMER_EMAIL_NOT_FOUND"
}
```

### 500 Internal Server Error

```json
{
  "status": 500,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_ERROR"
}
```

---

## Related APIs

- [Customer API](./CUSTOMER_API_DOCUMENTATION.md) - Manage customers
- [Customer Phone API](./CUSTOMER_PHONE_API_DOCUMENTATION.md) - Manage customer phones
- [Customer Note API](./CUSTOMER_NOTE_API_DOCUMENTATION.md) - Manage customer notes
