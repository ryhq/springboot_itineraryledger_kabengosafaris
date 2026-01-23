# Customer Phone API Documentation

## Overview

The Customer Phone API provides endpoints for managing customer phone numbers. Each customer can have multiple phone numbers with different types, WhatsApp status, and one designated as primary.

## Base URL

```
/api/customer-phones
```

## Authentication

All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_CUSTOMER_PHONE` - Create customer phones
- `PERM_READ_CUSTOMER_PHONE` - View customer phones
- `PERM_UPDATE_CUSTOMER_PHONE` - Update customer phones
- `PERM_DELETE_CUSTOMER_PHONE` - Delete customer phones

---

## Phone Types

| Type | Display Name | Description |
|------|--------------|-------------|
| MOBILE | Mobile | Mobile/cell phone number |
| HOME | Home | Home landline number |
| WORK | Work | Work/office phone number |
| FAX | Fax | Fax number |
| EMERGENCY | Emergency | Emergency contact number |
| OTHER | Other | Other phone type |

---

## Endpoints

### 1. Create Customer Phone

Creates a new phone number for a customer.

**Endpoint:** `POST /api/customer-phones`

**Permission:** `PERM_CREATE_CUSTOMER_PHONE`

**Request Body:**

```json
{
  "customerId": "abc123xyz",
  "phoneNumber": "+1234567890",
  "countryCode": "+1",
  "phoneType": "MOBILE",
  "isPrimary": true,
  "isWhatsApp": true,
  "isActive": true,
  "label": "Personal Mobile"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | String | Yes | Obfuscated customer ID |
| phoneNumber | String | Yes | Phone number (max 50 chars) |
| countryCode | String | No | Country code e.g., +1, +44 (max 10 chars) |
| phoneType | PhoneType | Yes | Type of phone (MOBILE, WORK, etc.) |
| isPrimary | Boolean | No | Set as primary phone (default: false) |
| isWhatsApp | Boolean | No | WhatsApp enabled (default: false) |
| isActive | Boolean | No | Active status (default: true) |
| label | String | No | Custom label (max 100 chars) |

**Notes:**
- If `isPrimary` is true, all other phones for the customer will be marked as non-primary
- Phone numbers are stored as provided (consider including country code in the number or separately)

**Success Response (201):**

```json
{
  "status": 201,
  "message": "Customer phone created successfully",
  "data": {
    "id": "phone123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "phoneNumber": "+1234567890",
    "countryCode": "+1",
    "phoneType": "MOBILE",
    "phoneTypeDisplayName": "Mobile",
    "phoneTypeDescription": "Mobile/cell phone number",
    "isPrimary": true,
    "isWhatsApp": true,
    "isActive": true,
    "label": "Personal Mobile",
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
| 400 | INVALID_PHONE_NUMBER | Phone number format is invalid |
| 400 | PHONE_NUMBER_TOO_LONG | Phone number exceeds 50 characters |

---

### 2. Update Customer Phone

Updates an existing customer phone.

**Endpoint:** `PUT /api/customer-phones/{idObfuscated}`

**Permission:** `PERM_UPDATE_CUSTOMER_PHONE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated phone ID |

**Request Body:**

All fields are optional. Only provided fields will be updated.

```json
{
  "phoneNumber": "+1987654321",
  "countryCode": "+1",
  "phoneType": "WORK",
  "isPrimary": false,
  "isWhatsApp": false,
  "isActive": true,
  "label": "Work Phone"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| phoneNumber | String | No | New phone number |
| countryCode | String | No | New country code |
| phoneType | PhoneType | No | New phone type |
| isPrimary | Boolean | No | Set as primary phone |
| isWhatsApp | Boolean | No | WhatsApp enabled |
| isActive | Boolean | No | Active status |
| label | String | No | Custom label |

**Notes:**
- At least one field must be provided for update
- If `isPrimary` is set to true, all other phones for the customer will be marked as non-primary

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer phone updated successfully",
  "data": {
    "id": "phone123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "phoneNumber": "+1987654321",
    "countryCode": "+1",
    "phoneType": "WORK",
    "phoneTypeDisplayName": "Work",
    "isPrimary": false,
    "isWhatsApp": false,
    "isActive": true,
    "label": "Work Phone",
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

---

### 3. Delete Customer Phones

Deletes one or more customer phones.

**Endpoint:** `DELETE /api/customer-phones`

**Permission:** `PERM_DELETE_CUSTOMER_PHONE`

**Request Body:**

```json
["phone123abc", "phone456def", "phone789ghi"]
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "3 customer phone(s) deleted successfully",
  "data": null
}
```

---

### 4. Get Customer Phone by ID

Retrieves a single customer phone by its obfuscated ID.

**Endpoint:** `GET /api/customer-phones/{idObfuscated}`

**Permission:** `PERM_READ_CUSTOMER_PHONE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated phone ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer phone retrieved successfully",
  "data": {
    "id": "phone123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "phoneNumber": "+1234567890",
    "countryCode": "+1",
    "phoneType": "MOBILE",
    "phoneTypeDisplayName": "Mobile",
    "phoneTypeDescription": "Mobile/cell phone number",
    "isPrimary": true,
    "isWhatsApp": true,
    "isActive": true,
    "label": "Personal Mobile",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (404):**

```json
{
  "status": 404,
  "message": "Customer phone not found",
  "errorCode": "CUSTOMER_PHONE_NOT_FOUND"
}
```

---

### 5. Get All Customer Phones

Retrieves a paginated list of customer phones with optional filtering.

**Endpoint:** `GET /api/customer-phones`

**Permission:** `PERM_READ_CUSTOMER_PHONE`

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| customerId | String | null | Filter by customer ID (optional) |
| phoneNumber | String | null | Filter by phone number (partial match) |
| phoneType | PhoneType | null | Filter by phone type |
| isPrimary | Boolean | null | Filter by primary status |
| isWhatsApp | Boolean | null | Filter by WhatsApp status |
| isActive | Boolean | null | Filter by active status |
| label | String | null | Filter by label (partial match) |
| keyword | String | null | Search across phone number and label |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Example Request:**

```
GET /api/customer-phones?phoneType=MOBILE&isWhatsApp=true&page=0&size=20
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer phones retrieved successfully",
  "data": {
    "phones": [
      {
        "id": "phone123abc",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "phoneNumber": "+1234567890",
        "countryCode": "+1",
        "phoneType": "MOBILE",
        "phoneTypeDisplayName": "Mobile",
        "isPrimary": true,
        "isWhatsApp": true,
        "isActive": true,
        "label": "Personal Mobile"
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

### 6. Get Phones for a Specific Customer

Retrieves all phones for a specific customer with optional filtering.

**Endpoint:** `GET /api/customer-phones/customer/{customerId}`

**Permission:** `PERM_READ_CUSTOMER_PHONE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| customerId | String | Required obfuscated customer ID |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| phoneNumber | String | null | Filter by phone number (partial match) |
| phoneType | PhoneType | null | Filter by phone type |
| isPrimary | Boolean | null | Filter by primary status |
| isWhatsApp | Boolean | null | Filter by WhatsApp status |
| isActive | Boolean | null | Filter by active status |
| label | String | null | Filter by label (partial match) |
| keyword | String | null | Search across phone number and label |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Example Request:**

```
GET /api/customer-phones/customer/abc123xyz?isWhatsApp=true
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer phones retrieved successfully",
  "data": {
    "phones": [
      {
        "id": "phone123abc",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "phoneNumber": "+1234567890",
        "countryCode": "+1",
        "phoneType": "MOBILE",
        "isPrimary": true,
        "isWhatsApp": true,
        "isActive": true
      },
      {
        "id": "phone456def",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "phoneNumber": "+1987654321",
        "countryCode": "+1",
        "phoneType": "WORK",
        "isPrimary": false,
        "isWhatsApp": false,
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
- `INVALID_PHONE_ID` - Invalid obfuscated phone ID
- `INVALID_CUSTOMER_ID` - Invalid customer ID
- `INVALID_PHONE_NUMBER` - Invalid phone number format
- `PHONE_NUMBER_TOO_LONG` - Phone number exceeds maximum length
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
  "message": "Customer phone not found",
  "errorCode": "CUSTOMER_PHONE_NOT_FOUND"
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
- [Customer Email API](./CUSTOMER_EMAIL_API_DOCUMENTATION.md) - Manage customer emails
- [Customer Note API](./CUSTOMER_NOTE_API_DOCUMENTATION.md) - Manage customer notes
