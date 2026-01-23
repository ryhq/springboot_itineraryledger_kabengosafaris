# Customer API Documentation

## Overview

The Customer API provides endpoints for managing customer data in the Kabengo Safaris system. It supports three types of customers: Individual, Corporate, and Travel Agent.

## Base URL

```
/api/customers
```

## Authentication

All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_CUSTOMER` - Create customers
- `PERM_READ_CUSTOMER` - View customers
- `PERM_UPDATE_CUSTOMER` - Update and deactivate/reactivate customers
- `PERM_DELETE_CUSTOMER` - Delete customers

---

## Endpoints

### 1. Create Customer

Creates a new customer with optional emails and phones.

**Endpoint:** `POST /api/customers`

**Permission:** `PERM_CREATE_CUSTOMER`

**Request Body:**

```json
{
  "customerType": "INDIVIDUAL",
  "title": "Mr",
  "firstName": "John",
  "lastName": "Doe",
  "companyName": null,
  "emails": [
    {
      "email": "john.doe@example.com",
      "emailType": "PERSONAL",
      "isPrimary": true,
      "label": "Main Email"
    }
  ],
  "phones": [
    {
      "phoneNumber": "+1234567890",
      "countryCode": "+1",
      "phoneType": "MOBILE",
      "isPrimary": true,
      "isWhatsApp": true,
      "label": "Mobile"
    }
  ],
  "nationality": "American",
  "residency": "USA",
  "passportNumber": "AB1234567",
  "passportExpiry": "2028-12-31",
  "dateOfBirth": "1985-05-15",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "country": "USA",
  "postalCode": "10001",
  "preferredLanguage": "en",
  "preferredCurrency": "USD",
  "source": "WEBSITE",
  "referredBy": null,
  "dietaryRequirements": "Vegetarian",
  "medicalConditions": null,
  "specialRequests": "Prefer morning activities",
  "interests": "[\"wildlife\", \"photography\"]",
  "internalNotes": "VIP client from referral",
  "isVip": true,
  "isActive": true
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerType | CustomerType | Yes | INDIVIDUAL, CORPORATE, or TRAVEL_AGENT |
| title | String | No | Mr, Mrs, Ms, Dr, etc. (max 10 chars) |
| firstName | String | Conditional | Required for INDIVIDUAL (max 100 chars) |
| lastName | String | Conditional | Required for INDIVIDUAL (max 100 chars) |
| companyName | String | Conditional | Required for CORPORATE/TRAVEL_AGENT (max 200 chars) |
| emails | Array | No | List of customer emails |
| phones | Array | No | List of customer phones |
| nationality | String | No | Customer nationality (max 100 chars) |
| residency | String | No | Country of residence (max 100 chars) |
| passportNumber | String | No | Passport number (max 50 chars) |
| passportExpiry | Date | No | Passport expiry date (YYYY-MM-DD) |
| dateOfBirth | Date | No | Date of birth (YYYY-MM-DD) |
| address | String | No | Street address |
| city | String | No | City (max 100 chars) |
| state | String | No | State/Province (max 100 chars) |
| country | String | No | Country (max 100 chars) |
| postalCode | String | No | Postal/ZIP code (max 20 chars) |
| preferredLanguage | String | No | ISO language code (default: "en") |
| preferredCurrency | String | No | ISO currency code (default: "USD") |
| source | CustomerSource | No | How customer was acquired |
| referredBy | String | No | Referral source (max 200 chars) |
| dietaryRequirements | String | No | Dietary restrictions |
| medicalConditions | String | No | Medical conditions to note |
| specialRequests | String | No | Special requests or preferences |
| interests | String | No | JSON array of interests |
| internalNotes | String | No | Internal staff notes |
| isVip | Boolean | No | VIP status (default: false) |
| isActive | Boolean | No | Active status (default: true) |

**Customer Types:**
- `INDIVIDUAL` - Individual person
- `CORPORATE` - Corporate client
- `TRAVEL_AGENT` - Travel agency

**Customer Sources:**
- `WEBSITE` - Company website
- `REFERRAL` - Customer referral
- `SOCIAL_MEDIA` - Social media channels
- `TRADE_SHOW` - Trade shows/events
- `DIRECT` - Direct contact
- `PARTNER` - Partner company
- `REPEAT` - Returning customer
- `OTHER` - Other sources

**Success Response (201):**

```json
{
  "status": 201,
  "message": "Customer created successfully",
  "data": {
    "id": "abc123xyz",
    "code": "CUS-000001",
    "customerType": "INDIVIDUAL",
    "customerTypeDisplayName": "Individual",
    "title": "Mr",
    "firstName": "John",
    "lastName": "Doe",
    "displayName": "John Doe",
    "primaryEmail": "john.doe@example.com",
    "primaryPhone": "+1234567890",
    "isActive": true,
    "isVip": true,
    "canBook": true,
    "emailCount": 1,
    "phoneCount": 1,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

### 2. Get Customer by ID

Retrieves a single customer by their obfuscated ID.

**Endpoint:** `GET /api/customers/{id}`

**Permission:** `PERM_READ_CUSTOMER`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated customer ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer retrieved successfully",
  "data": {
    "id": "abc123xyz",
    "code": "CUS-000001",
    "customerType": "INDIVIDUAL",
    "customerTypeDisplayName": "Individual",
    "customerTypeDescription": "Individual customer",
    "title": "Mr",
    "firstName": "John",
    "lastName": "Doe",
    "displayName": "John Doe",
    "primaryEmail": "john.doe@example.com",
    "primaryPhone": "+1234567890",
    "nationality": "American",
    "residency": "USA",
    "passportNumber": "AB1234567",
    "passportExpiry": "2028-12-31",
    "passportExpiringSoon": false,
    "dateOfBirth": "1985-05-15",
    "address": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "country": "USA",
    "postalCode": "10001",
    "fullAddress": "123 Main Street, New York, NY 10001, USA",
    "preferredLanguage": "en",
    "preferredCurrency": "USD",
    "source": "WEBSITE",
    "sourceDisplayName": "Website",
    "dietaryRequirements": "Vegetarian",
    "specialRequests": "Prefer morning activities",
    "interests": "[\"wildlife\", \"photography\"]",
    "isVip": true,
    "isBlacklisted": false,
    "isActive": true,
    "canBook": true,
    "totalBookings": 5,
    "totalSpent": 15000.00,
    "lastBookingDate": "2024-01-10T14:00:00",
    "emailCount": 2,
    "phoneCount": 1,
    "documentCount": 3,
    "noteCount": 5,
    "createdAt": "2023-06-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (404):**

```json
{
  "status": 404,
  "message": "Customer not found",
  "errorCode": "CUSTOMER_NOT_FOUND"
}
```

---

### 3. Get All Customers

Retrieves a paginated list of customers with optional filtering.

**Endpoint:** `GET /api/customers`

**Permission:** `PERM_READ_CUSTOMER`

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| name | String | null | Filter by name (partial match) |
| email | String | null | Filter by email (partial match) |
| phone | String | null | Filter by phone (partial match) |
| code | String | null | Filter by customer code |
| customerType | CustomerType | null | Filter by customer type |
| source | CustomerSource | null | Filter by acquisition source |
| nationality | String | null | Filter by nationality |
| country | String | null | Filter by country |
| city | String | null | Filter by city |
| isActive | Boolean | null | Filter by active status |
| isVip | Boolean | null | Filter by VIP status |
| isBlacklisted | Boolean | null | Filter by blacklist status |
| hasBookings | Boolean | null | Filter by booking history |
| minTotalSpent | BigDecimal | null | Minimum total spent |
| maxTotalSpent | BigDecimal | null | Maximum total spent |
| keyword | String | null | Search across multiple fields |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortBy | String | createdAt | Sort field |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Example Request:**

```
GET /api/customers?customerType=INDIVIDUAL&isVip=true&page=0&size=20&sortBy=lastName&sortDirection=asc
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customers retrieved successfully",
  "data": {
    "customers": [
      {
        "id": "abc123xyz",
        "code": "CUS-000001",
        "customerType": "INDIVIDUAL",
        "displayName": "John Doe",
        "primaryEmail": "john.doe@example.com",
        "primaryPhone": "+1234567890",
        "nationality": "American",
        "isVip": true,
        "isActive": true,
        "totalBookings": 5,
        "totalSpent": 15000.00
      }
    ],
    "currentPage": 0,
    "totalItems": 150,
    "totalPages": 15,
    "pageSize": 10
  }
}
```

---

### 4. Update Customer

Updates an existing customer's information.

**Endpoint:** `PUT /api/customers/{id}`

**Permission:** `PERM_UPDATE_CUSTOMER`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated customer ID |

**Request Body:**

All fields are optional. Only provided fields will be updated.

```json
{
  "firstName": "Jonathan",
  "lastName": "Doe",
  "city": "Los Angeles",
  "state": "CA",
  "isVip": true
}
```

**Note:** Emails and phones are NOT updated through this endpoint. Use the CustomerEmail and CustomerPhone APIs instead.

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer updated successfully",
  "data": {
    "id": "abc123xyz",
    "code": "CUS-000001",
    "displayName": "Jonathan Doe",
    "city": "Los Angeles",
    "state": "CA",
    "isVip": true,
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

---

### 5. Delete Customers

Permanently deletes one or more customers.

**Endpoint:** `DELETE /api/customers`

**Permission:** `PERM_DELETE_CUSTOMER`

**Request Body:**

```json
["abc123xyz", "def456uvw", "ghi789rst"]
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "3 customer(s) deleted successfully",
  "data": null
}
```

---

### 6. Deactivate Customer

Soft deletes a customer by setting their active status to false.

**Endpoint:** `POST /api/customers/{id}/deactivate`

**Permission:** `PERM_UPDATE_CUSTOMER`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated customer ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer deactivated successfully",
  "data": {
    "id": "abc123xyz",
    "isActive": false,
    "canBook": false,
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

---

### 7. Reactivate Customer

Reactivates a previously deactivated customer.

**Endpoint:** `POST /api/customers/{id}/reactivate`

**Permission:** `PERM_UPDATE_CUSTOMER`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated customer ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer reactivated successfully",
  "data": {
    "id": "abc123xyz",
    "isActive": true,
    "canBook": true,
    "updatedAt": "2024-01-15T11:00:00"
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
- `INVALID_CUSTOMER_ID` - Invalid obfuscated ID
- `VALIDATION_ERROR` - Field validation failed
- `CUSTOMER_TYPE_MISMATCH` - Customer type requirements not met

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
  "message": "Customer not found",
  "errorCode": "CUSTOMER_NOT_FOUND"
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

- [Customer Email API](../CustomerEmail/Controller/CUSTOMER_EMAIL_API_DOCUMENTATION.md) - Manage customer emails
- [Customer Phone API](../CustomerPhone/Controller/CUSTOMER_PHONE_API_DOCUMENTATION.md) - Manage customer phones
- [Customer Note API](./CUSTOMER_NOTE_API_DOCUMENTATION.md) - Manage customer notes
