# Quote API Documentation

## Overview
The Quote API provides endpoints for managing safari tour quotes. Quotes are generated for itineraries and include pricing details, validity periods, payment terms, and approval workflows. The system supports multi-currency pricing, version control, and comprehensive status tracking.

**Base URL**: `/api/quotes`

**Required Permissions**:
- `PERM_CREATE_QUOTE` - Create new quotes
- `PERM_READ_QUOTE` - View quotes
- `PERM_UPDATE_QUOTE` - Update existing quotes
- `PERM_DELETE_QUOTE` - Delete quotes

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Create Quote](#1-create-quote)
   - [Update Quote](#2-update-quote)
   - [Delete Quotes](#3-delete-quotes)
   - [Get Quote by ID](#4-get-quote-by-id)
   - [Get Quote by Code](#5-get-quote-by-code)
   - [Get All Quotes](#6-get-all-quotes-with-filters)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)
5. [Authentication & Authorization](#authentication--authorization)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

---

## Data Models

### QuoteStatus Enum
```json
{
  "allowedValues": [
    "DRAFT",
    "PENDING_APPROVAL",
    "APPROVED",
    "SENT",
    "ACCEPTED",
    "REJECTED",
    "EXPIRED",
    "CANCELLED"
  ]
}
```

**Status Descriptions**:
- `DRAFT` - Quote is being prepared and is not yet finalized
- `PENDING_APPROVAL` - Quote is awaiting approval from designated approver
- `APPROVED` - Quote has been approved and is ready to be sent to customer
- `SENT` - Quote has been sent to customer
- `ACCEPTED` - Customer has accepted the quote
- `REJECTED` - Customer has rejected the quote
- `EXPIRED` - Quote validity period has expired
- `CANCELLED` - Quote was cancelled before completion

### Status Groups
Quotes can be filtered by status groups for easier management:

| Group | Included Statuses | Description |
|-------|-------------------|-------------|
| `draft` | DRAFT | Quotes being prepared |
| `pending` | PENDING_APPROVAL | Awaiting approval |
| `active` | APPROVED, SENT | Active quotes in circulation |
| `closed` | ACCEPTED, REJECTED, EXPIRED, CANCELLED | Completed or terminated quotes |

### Price Object (Multi-Currency)
```json
{
  "USD": 5000.00,
  "TZS": 11750000.00,
  "EUR": 4500.00
}
```

### Quote Object (QuoteDTO)
```json
{
  "id": "string (obfuscated ID for security)",
  "quoteCode": "string (system-generated, e.g., QT-2024-0001)",
  "title": "string",
  "description": "string",
  "itineraryId": "string (obfuscated)",
  "itineraryCode": "string",
  "itineraryName": "string",
  "customerId": "string (obfuscated)",
  "customerName": "string",
  "customerEmail": "string",
  "subtotals": "Price object (multi-currency)",
  "taxes": "Price object (multi-currency)",
  "discounts": "Price object (multi-currency)",
  "grandTotals": "Price object (multi-currency)",
  "isStoRate": "boolean (Special Tour Operator rate)",
  "taxPercentage": "decimal",
  "discountPercentage": "decimal",
  "discountReason": "string",
  "version": "integer (system-managed version number)",
  "status": "QuoteStatus enum",
  "sentDate": "date (system-set when status changes to SENT)",
  "validFrom": "date",
  "validTo": "date",
  "isValid": "boolean",
  "depositPercentage": "decimal",
  "depositDueDate": "date",
  "fullPaymentDueDate": "date",
  "approverId": "string (obfuscated, user who should approve)",
  "approverName": "string",
  "approvedById": "string (obfuscated, user who actually approved)",
  "approvedByName": "string",
  "approvedAt": "datetime (system-set when approved)",
  "approvalNotes": "string",
  "previousVersionId": "string (obfuscated, null for first version)",
  "previousVersionCode": "string (quote code of previous version)",
  "nextVersionId": "string (obfuscated, null if no newer version)",
  "nextVersionCode": "string (quote code of next version)",
  "internalNotes": "string (not visible to customer)",
  "customerNotes": "string (visible to customer)",
  "versionNotes": "string (changelog for this version)",
  "isActive": "boolean",
  "itemCount": "long (number of quote items)",
  "documentCount": "long (number of attached documents)",
  "createdById": "string (obfuscated)",
  "createdByName": "string",
  "createdAt": "datetime (ISO 8601)",
  "updatedById": "string (obfuscated)",
  "updatedByName": "string",
  "updatedAt": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Quote

**Endpoint**: `POST /api/quotes`

**Permission**: `PERM_CREATE_QUOTE`

**Description**: Creates a new quote for a specific itinerary and customer. The quote code is automatically generated. The quote starts in DRAFT status by default.

#### Request Body (CreateQuoteDTO)
```json
{
  "title": "Northern Circuit Safari Package - June 2024",
  "description": "7-day safari covering Tarangire, Serengeti, and Ngorongoro Crater",
  "itineraryId": "xY9Kp2Lm",
  "customerId": "aB3Cd4Ef",
  "approverId": "pQ7Rs8Tv",
  "isStoRate": true,
  "taxPercentage": 18.00,
  "discountPercentage": 10.00,
  "discountReason": "Early bird booking discount",
  "validFrom": "2024-06-01",
  "validTo": "2024-12-31",
  "isValid": true,
  "depositPercentage": 30.00,
  "depositDueDate": "2024-05-15",
  "fullPaymentDueDate": "2024-05-25",
  "internalNotes": "VIP client - priority service",
  "customerNotes": "Special dietary requirements: vegetarian meals",
  "versionNotes": "Initial quote version",
  "isActive": true
}
```

**Required Fields**:
- `title` (string, not blank)
- `itineraryId` (string, obfuscated ID)
- `customerId` (string, obfuscated ID)

**Optional Fields**: All other fields are optional.

**System-Managed Fields** (auto-generated, not in request):
- `quoteCode` - Auto-generated (e.g., QT-2024-0001)
- `version` - Starts at 1
- `sentDate` - Set when status changes to SENT
- `approvedAt` - Set when status changes to APPROVED
- `subtotals`, `taxes`, `discounts`, `grandTotals` - Calculated from quote items

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote created successfully",
  "data": {
    "id": "mN4Op5Qr",
    "quoteCode": "QT-2024-0001",
    "title": "Northern Circuit Safari Package - June 2024",
    "description": "7-day safari covering Tarangire, Serengeti, and Ngorongoro Crater",
    "itineraryId": "xY9Kp2Lm",
    "itineraryCode": "IT-2024-0015",
    "itineraryName": "Northern Circuit Classic",
    "customerId": "aB3Cd4Ef",
    "customerName": "John Smith",
    "customerEmail": "john.smith@example.com",
    "subtotals": {
      "USD": 5000.00
    },
    "taxes": {
      "USD": 900.00
    },
    "discounts": {
      "USD": 500.00
    },
    "grandTotals": {
      "USD": 5400.00
    },
    "isStoRate": true,
    "taxPercentage": 18.00,
    "discountPercentage": 10.00,
    "discountReason": "Early bird booking discount",
    "version": 1,
    "status": "DRAFT",
    "sentDate": null,
    "validFrom": "2024-06-01",
    "validTo": "2024-12-31",
    "isValid": true,
    "depositPercentage": 30.00,
    "depositDueDate": "2024-05-15",
    "fullPaymentDueDate": "2024-05-25",
    "approverId": "pQ7Rs8Tv",
    "approverName": "Jane Manager",
    "approvedById": null,
    "approvedByName": null,
    "approvedAt": null,
    "approvalNotes": null,
    "previousVersionId": null,
    "previousVersionCode": null,
    "nextVersionId": null,
    "nextVersionCode": null,
    "internalNotes": "VIP client - priority service",
    "customerNotes": "Special dietary requirements: vegetarian meals",
    "versionNotes": "Initial quote version",
    "isActive": true,
    "itemCount": 0,
    "documentCount": 0,
    "createdById": "tU9Vw0Xy",
    "createdByName": "Admin User",
    "createdAt": "2024-04-15T10:30:00",
    "updatedById": null,
    "updatedByName": null,
    "updatedAt": "2024-04-15T10:30:00"
  },
  "timestamp": "2024-04-15T10:30:00"
}
```

#### Error Responses

**400 Bad Request** - Validation errors
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid itinerary ID",
  "errorCode": "INVALID_ITINERARY_ID",
  "timestamp": "2024-04-15T10:30:00"
}
```

**400 Bad Request** - Date validation error
```json
{
  "success": false,
  "statusCode": 400,
  "message": "validFrom date cannot be in the past",
  "errorCode": "INVALID_VALID_FROM_DATE",
  "timestamp": "2024-04-15T10:30:00"
}
```

**404 Not Found** - Itinerary not found
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2024-04-15T10:30:00"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to create quote",
  "errorCode": "QUOTE_CREATE_FAILED",
  "timestamp": "2024-04-15T10:30:00"
}
```

---

### 2. Update Quote

**Endpoint**: `PUT /api/quotes/{idObfuscated}`

**Permission**: `PERM_UPDATE_QUOTE`

**Description**: Updates an existing quote's metadata. Only provided fields will be updated (partial update). **Important**: This endpoint only updates metadata fields, not relationships or system-managed fields.

#### Path Parameters
- `idObfuscated` (required): The obfuscated quote ID

#### Request Body (UpdateQuoteDTO)
```json
{
  "title": "Updated Northern Circuit Safari Package",
  "description": "Updated 7-day safari itinerary",
  "status": "PENDING_APPROVAL",
  "isStoRate": true,
  "taxPercentage": 18.00,
  "discountPercentage": 15.00,
  "discountReason": "Extended early bird discount",
  "validFrom": "2024-06-01",
  "validTo": "2024-12-31",
  "isValid": true,
  "depositPercentage": 30.00,
  "depositDueDate": "2024-05-15",
  "fullPaymentDueDate": "2024-05-25",
  "internalNotes": "Updated internal notes",
  "customerNotes": "Updated customer notes",
  "approvalNotes": "Approved with conditions",
  "versionNotes": "Price adjustment for special request",
  "isActive": true
}
```

**Updatable Fields**: All fields in the request body are optional. Only include fields you want to update.

**Non-Updatable Fields** (system-managed, cannot be changed via update):
- `quoteCode` - System-generated, never changes
- `sentDate` - System-set when status changes to SENT
- `approvedAt` - System-set when status changes to APPROVED
- `version` - Incremented automatically when creating new versions
- `itineraryId` - Cannot change quote's itinerary
- `customerId` - Cannot change quote's customer
- `approverId` - Cannot change designated approver
- `approvedById` - Set by system when approved
- `previousVersionId`, `nextVersionId` - Managed by versioning system

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote updated successfully",
  "data": {
    "id": "mN4Op5Qr",
    "quoteCode": "QT-2024-0001",
    "title": "Updated Northern Circuit Safari Package",
    "description": "Updated 7-day safari itinerary",
    "status": "PENDING_APPROVAL",
    "discountPercentage": 15.00,
    "discountReason": "Extended early bird discount",
    ...
    "updatedById": "tU9Vw0Xy",
    "updatedByName": "Admin User",
    "updatedAt": "2024-04-15T15:45:00"
  },
  "timestamp": "2024-04-15T15:45:00"
}
```

#### Error Responses

**400 Bad Request** - Invalid ID
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid quote ID",
  "errorCode": "INVALID_QUOTE_ID",
  "timestamp": "2024-04-15T15:45:00"
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote not found",
  "errorCode": "QUOTE_NOT_FOUND",
  "timestamp": "2024-04-15T15:45:00"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to update quote",
  "errorCode": "QUOTE_UPDATE_FAILED",
  "timestamp": "2024-04-15T15:45:00"
}
```

---

### 3. Delete Quotes

**Endpoint**: `DELETE /api/quotes`

**Permission**: `PERM_DELETE_QUOTE`

**Description**: Deletes one or more quotes by their obfuscated IDs. This also deletes associated quote items and documents.

#### Request Body
```json
["mN4Op5Qr", "xY9Kp2Lm", "aB3Cd4Ef"]
```

**Note**: Send an array of obfuscated quote IDs to delete.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 quote(s) deleted successfully",
  "data": {
    "deletedIds": ["mN4Op5Qr", "xY9Kp2Lm", "aB3Cd4Ef"],
    "failedIds": []
  },
  "timestamp": "2024-04-15T16:00:00"
}
```

#### Partial Success Response (200 OK)
When some deletions fail:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 quote(s) deleted successfully, 1 failed",
  "data": {
    "deletedIds": ["mN4Op5Qr", "xY9Kp2Lm"],
    "failedIds": ["aB3Cd4Ef"]
  },
  "timestamp": "2024-04-15T16:00:00"
}
```

#### Error Responses

**400 Bad Request** - No IDs provided
```json
{
  "success": false,
  "statusCode": 400,
  "message": "No quote IDs provided",
  "errorCode": "NO_IDS_PROVIDED",
  "timestamp": "2024-04-15T16:00:00"
}
```

**400 Bad Request** - All deletions failed
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Failed to delete any quotes",
  "errorCode": "DELETE_FAILED",
  "timestamp": "2024-04-15T16:00:00"
}
```

---

### 4. Get Quote by ID

**Endpoint**: `GET /api/quotes/{idObfuscated}`

**Permission**: `PERM_READ_QUOTE`

**Description**: Retrieves a single quote by its obfuscated ID. Returns the complete quote object with all related data.

#### Path Parameters
- `idObfuscated` (required): The obfuscated quote ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote retrieved successfully",
  "data": {
    "id": "mN4Op5Qr",
    "quoteCode": "QT-2024-0001",
    "title": "Northern Circuit Safari Package - June 2024",
    "description": "7-day safari covering Tarangire, Serengeti, and Ngorongoro Crater",
    "itineraryId": "xY9Kp2Lm",
    "itineraryCode": "IT-2024-0015",
    "itineraryName": "Northern Circuit Classic",
    "customerId": "aB3Cd4Ef",
    "customerName": "John Smith",
    "customerEmail": "john.smith@example.com",
    "subtotals": {
      "USD": 5000.00
    },
    "taxes": {
      "USD": 900.00
    },
    "discounts": {
      "USD": 500.00
    },
    "grandTotals": {
      "USD": 5400.00
    },
    "isStoRate": true,
    "taxPercentage": 18.00,
    "discountPercentage": 10.00,
    "discountReason": "Early bird booking discount",
    "version": 1,
    "status": "APPROVED",
    "sentDate": "2024-04-16",
    "validFrom": "2024-06-01",
    "validTo": "2024-12-31",
    "isValid": true,
    "depositPercentage": 30.00,
    "depositDueDate": "2024-05-15",
    "fullPaymentDueDate": "2024-05-25",
    "approverId": "pQ7Rs8Tv",
    "approverName": "Jane Manager",
    "approvedById": "zA1Bc2De",
    "approvedByName": "Senior Manager",
    "approvedAt": "2024-04-15T14:30:00",
    "approvalNotes": "Approved",
    "previousVersionId": null,
    "previousVersionCode": null,
    "nextVersionId": null,
    "nextVersionCode": null,
    "internalNotes": "VIP client - priority service",
    "customerNotes": "Special dietary requirements: vegetarian meals",
    "versionNotes": "Initial quote version",
    "isActive": true,
    "itemCount": 15,
    "documentCount": 2,
    "createdById": "tU9Vw0Xy",
    "createdByName": "Admin User",
    "createdAt": "2024-04-15T10:30:00",
    "updatedById": "tU9Vw0Xy",
    "updatedByName": "Admin User",
    "updatedAt": "2024-04-15T15:45:00"
  },
  "timestamp": "2024-04-15T16:30:00"
}
```

#### Error Responses

**400 Bad Request** - Invalid ID
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid quote ID",
  "errorCode": "INVALID_QUOTE_ID",
  "timestamp": "2024-04-15T16:30:00"
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote not found",
  "errorCode": "QUOTE_NOT_FOUND",
  "timestamp": "2024-04-15T16:30:00"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to fetch quote",
  "errorCode": "QUOTE_FETCH_FAILED",
  "timestamp": "2024-04-15T16:30:00"
}
```

---

### 5. Get Quote by Code

**Endpoint**: `GET /api/quotes/code/{quoteCode}`

**Permission**: `PERM_READ_QUOTE`

**Description**: Retrieves a single quote by its system-generated quote code. Useful for customer-facing references.

#### Path Parameters
- `quoteCode` (required): The quote code (e.g., "QT-2024-0001")

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote retrieved successfully",
  "data": {
    "id": "mN4Op5Qr",
    "quoteCode": "QT-2024-0001",
    "title": "Northern Circuit Safari Package - June 2024",
    ...
  },
  "timestamp": "2024-04-15T17:00:00"
}
```

#### Error Responses

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote not found",
  "errorCode": "QUOTE_NOT_FOUND",
  "timestamp": "2024-04-15T17:00:00"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to fetch quote",
  "errorCode": "QUOTE_FETCH_FAILED",
  "timestamp": "2024-04-15T17:00:00"
}
```

---

### 6. Get All Quotes (with Filters)

**Endpoint**: `GET /api/quotes`

**Permission**: `PERM_READ_QUOTE`

**Description**: Retrieves a paginated list of quotes with optional filtering and sorting. Results are always sorted by `createdAt` field.

#### Query Parameters

**Filtering**:
- `quoteCode` (string, optional): Filter by quote code (partial match, case-insensitive)
- `title` (string, optional): Filter by title (partial match, case-insensitive)
- `status` (QuoteStatus, optional): Filter by exact status (DRAFT, PENDING_APPROVAL, APPROVED, SENT, ACCEPTED, REJECTED, EXPIRED, CANCELLED)
- `statusGroup` (string, optional): Filter by status group
  - `draft` - DRAFT status
  - `pending` - PENDING_APPROVAL status
  - `active` - APPROVED and SENT statuses
  - `closed` - ACCEPTED, REJECTED, EXPIRED, CANCELLED statuses
- `itineraryId` (string, optional): Filter by itinerary ID (obfuscated, exact match)
- `customerId` (string, optional): Filter by customer ID (obfuscated, exact match)
- `approverId` (string, optional): Filter by approver ID (obfuscated, exact match)
- `approvedById` (string, optional): Filter by approved by user ID (obfuscated, exact match)
- `createdById` (string, optional): Filter by created by user ID (obfuscated, exact match)
- `updatedById` (string, optional): Filter by updated by user ID (obfuscated, exact match)
- `isStoRate` (boolean, optional): Filter by STO rate flag (true/false)
- `isActive` (boolean, optional): Filter by active status (true/false)
- `validOn` (date, optional): Filter quotes valid on specific date (YYYY-MM-DD)
- `sentAfter` (date, optional): Filter quotes sent after date (YYYY-MM-DD)
- `sentBefore` (date, optional): Filter quotes sent before date (YYYY-MM-DD)
- `version` (integer, optional): Filter by version number (exact match)

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`
  - **Note**: All quotes are sorted by `createdAt` field. The sort field cannot be changed.

#### Example Requests

**Basic request (all quotes, default pagination)**:
```
GET /api/quotes
```

**Filter by status**:
```
GET /api/quotes?status=APPROVED
```

**Filter by customer with pagination**:
```
GET /api/quotes?customerId=aB3Cd4Ef&page=0&size=20
```

**Filter by status group**:
```
GET /api/quotes?statusGroup=active&sortDirection=asc
```

**Filter active quotes valid on specific date**:
```
GET /api/quotes?isActive=true&validOn=2024-06-15
```

**Complex filter - Active STO quotes for specific customer sent in date range**:
```
GET /api/quotes?customerId=aB3Cd4Ef&isStoRate=true&isActive=true&sentAfter=2024-01-01&sentBefore=2024-12-31&page=0&size=25&sortDirection=desc
```

**Filter by itinerary and status**:
```
GET /api/quotes?itineraryId=xY9Kp2Lm&statusGroup=closed
```

**Search by quote code**:
```
GET /api/quotes?quoteCode=QT-2024
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quotes retrieved successfully",
  "data": {
    "quotes": [
      {
        "id": "mN4Op5Qr",
        "quoteCode": "QT-2024-0001",
        "title": "Northern Circuit Safari Package - June 2024",
        "status": "APPROVED",
        "itineraryCode": "IT-2024-0015",
        "itineraryName": "Northern Circuit Classic",
        "customerName": "John Smith",
        "grandTotals": {
          "USD": 5400.00
        },
        "version": 1,
        "validFrom": "2024-06-01",
        "validTo": "2024-12-31",
        "isActive": true,
        "createdAt": "2024-04-15T10:30:00",
        ...
      },
      {
        "id": "pQ7Rs8Tv",
        "quoteCode": "QT-2024-0002",
        "title": "Southern Circuit Adventure",
        "status": "SENT",
        "itineraryCode": "IT-2024-0020",
        "itineraryName": "Southern Highlights",
        "customerName": "Jane Doe",
        "grandTotals": {
          "USD": 4800.00
        },
        "version": 1,
        "validFrom": "2024-07-01",
        "validTo": "2024-12-31",
        "isActive": true,
        "createdAt": "2024-04-14T09:15:00",
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 45,
    "totalPages": 3
  },
  "timestamp": "2024-04-15T18:00:00"
}
```

#### Response with No Results (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quotes retrieved successfully",
  "data": {
    "quotes": [],
    "currentPage": 0,
    "totalItems": 0,
    "totalPages": 0
  },
  "timestamp": "2024-04-15T18:00:00"
}
```

#### Error Responses

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to fetch quotes",
  "errorCode": "QUOTES_FETCH_FAILED",
  "timestamp": "2024-04-15T18:00:00"
}
```

---

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-04-15T10:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-04-15T10:00:00"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `QUOTE_NOT_FOUND` | 404 | Quote with specified ID or code not found |
| `INVALID_QUOTE_ID` | 400 | The provided obfuscated ID is invalid or malformed |
| `INVALID_ITINERARY_ID` | 400 | The provided itinerary ID is invalid |
| `INVALID_CUSTOMER_ID` | 400 | The provided customer ID is invalid |
| `ITINERARY_NOT_FOUND` | 404 | Specified itinerary does not exist |
| `CUSTOMER_NOT_FOUND` | 404 | Specified customer does not exist |
| `APPROVER_NOT_FOUND` | 404 | Specified approver user does not exist |
| `INVALID_VALID_FROM_DATE` | 400 | validFrom date is in the past or invalid |
| `INVALID_VALID_TO_DATE` | 400 | validTo date is before validFrom or invalid |
| `QUOTE_CREATE_FAILED` | 500 | Failed to create quote (internal error) |
| `QUOTE_UPDATE_FAILED` | 500 | Failed to update quote (internal error) |
| `QUOTE_FETCH_FAILED` | 500 | Failed to fetch quote (internal error) |
| `QUOTES_FETCH_FAILED` | 500 | Failed to fetch quotes list (internal error) |
| `DELETE_FAILED` | 400 | Failed to delete all requested quotes |
| `NO_IDS_PROVIDED` | 400 | Delete request sent with empty ID list |
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields, invalid format) |
| `PERMISSION_DENIED` | 403 | User lacks required permission for the operation |
| `UNAUTHORIZED` | 401 | User is not authenticated |

---

## Authentication & Authorization

All endpoints require:

1. **Authentication**: Valid JWT token in the `Authorization` header
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Authorization**: User must have the appropriate permission:
   - Create operations: `PERM_CREATE_QUOTE`
   - Read operations: `PERM_READ_QUOTE`
   - Update operations: `PERM_UPDATE_QUOTE`
   - Delete operations: `PERM_DELETE_QUOTE`

---

## Best Practices

1. **Use Quote Codes for Customer Communication**: Use the `quoteCode` (e.g., QT-2024-0001) in customer-facing communications for easy reference.

2. **Use IDs for Management**: Use obfuscated IDs for administrative operations (update, delete) in internal systems.

3. **Status Workflow**: Follow the proper status workflow:
   - DRAFT → PENDING_APPROVAL → APPROVED → SENT → ACCEPTED/REJECTED
   - Use EXPIRED for quotes past their validity period
   - Use CANCELLED for quotes terminated before completion

4. **Version Control**: When creating a new version of a quote:
   - Create a new quote with updated details
   - Link to previous version using previousVersionId
   - Increment version number
   - Keep old versions for audit trail

5. **Multi-Currency Support**: The system supports multiple currencies in price objects. Always check which currencies are present in the response.

6. **Date Validation**: Ensure validFrom and validTo dates are logical and in the future when creating quotes.

7. **STO vs Rack Rates**: Use `isStoRate=true` for Special Tour Operator rates, which may have different pricing than standard rack rates.

8. **Filter by Status Groups**: Use statusGroup filters for dashboard views:
   - `draft` - Work in progress
   - `pending` - Awaiting action
   - `active` - Current quotes
   - `closed` - Historical quotes

9. **Handle Partial Updates**: When updating, only send fields that need to be changed. Null values in the request are ignored.

10. **System-Managed Fields**: Don't attempt to update system-managed fields (quoteCode, sentDate, approvedAt, version). They are controlled by the system.

11. **Pagination**: Always use pagination for list endpoints to improve performance, especially for large datasets.

12. **Soft Deletes**: Consider using `isActive=false` instead of deletion to maintain audit history.

---

## Examples

### cURL Examples

**Create a quote**:
```bash
curl -X POST http://localhost:8080/api/quotes \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Northern Circuit Safari Package - June 2024",
    "description": "7-day safari covering Tarangire, Serengeti, and Ngorongoro Crater",
    "itineraryId": "xY9Kp2Lm",
    "customerId": "aB3Cd4Ef",
    "approverId": "pQ7Rs8Tv",
    "isStoRate": true,
    "taxPercentage": 18.00,
    "discountPercentage": 10.00,
    "discountReason": "Early bird booking discount",
    "validFrom": "2024-06-01",
    "validTo": "2024-12-31",
    "depositPercentage": 30.00,
    "depositDueDate": "2024-05-15",
    "fullPaymentDueDate": "2024-05-25",
    "isActive": true
  }'
```

**Get all active quotes**:
```bash
curl -X GET "http://localhost:8080/api/quotes?isActive=true&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Get quotes for specific customer**:
```bash
curl -X GET "http://localhost:8080/api/quotes?customerId=aB3Cd4Ef&sortDirection=desc" \
  -H "Authorization: Bearer <your-token>"
```

**Update a quote status**:
```bash
curl -X PUT http://localhost:8080/api/quotes/mN4Op5Qr \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED",
    "approvalNotes": "Approved for customer presentation"
  }'
```

**Get quote by code**:
```bash
curl -X GET http://localhost:8080/api/quotes/code/QT-2024-0001 \
  -H "Authorization: Bearer <your-token>"
```

**Filter quotes by status group**:
```bash
curl -X GET "http://localhost:8080/api/quotes?statusGroup=active&page=0&size=15" \
  -H "Authorization: Bearer <your-token>"
```

**Delete quotes**:
```bash
curl -X DELETE http://localhost:8080/api/quotes \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '["mN4Op5Qr", "xY9Kp2Lm"]'
```

**Get quotes valid on specific date**:
```bash
curl -X GET "http://localhost:8080/api/quotes?validOn=2024-06-15&isActive=true" \
  -H "Authorization: Bearer <your-token>"
```

---

## Notes

- **ID Obfuscation**: All quote IDs, customer IDs, itinerary IDs, and user IDs are obfuscated for security. Never expose internal database IDs.

- **Quote Code Generation**: Quote codes are automatically generated in the format QT-YYYY-NNNN (e.g., QT-2024-0001) and are unique system-wide.

- **Timestamps**: All timestamps are in ISO 8601 format (UTC).

- **Multi-Currency Pricing**: Price objects (subtotals, taxes, discounts, grandTotals) support multiple currencies. Each currency has its own total.

- **Version Control**: Quotes support versioning for tracking changes. The version number is managed by the system and increments with each new version.

- **Approval Workflow**: The system tracks both the designated approver (`approverId`) and the actual user who approved (`approvedById`), along with when approval occurred (`approvedAt`).

- **Soft Deletes**: While the API provides hard delete functionality, consider using `isActive=false` to maintain audit trails.

- **Date Validation**: The system validates that `validFrom` is not in the past and `validTo` is after `validFrom` when creating quotes.

- **Automatic Date Setting**: The `sentDate` is automatically set when the quote status changes to SENT. The `approvedAt` is automatically set when the quote is approved.

- **Quote Items and Documents**: The `itemCount` and `documentCount` fields show the number of associated items and documents. Use dedicated Quote Item and Quote Document APIs to manage these.

- **Internal vs Customer Notes**: Use `internalNotes` for staff-only information and `customerNotes` for information visible to customers.

- **Sorting**: All list queries are sorted by `createdAt` in descending order by default (newest first). This ensures consistent ordering and cannot be changed to other fields.

---

## Support

For issues or questions about the Quote API, please contact the development team or refer to the main application documentation.
