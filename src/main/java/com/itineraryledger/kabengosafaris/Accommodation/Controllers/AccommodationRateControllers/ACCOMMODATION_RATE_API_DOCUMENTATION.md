# Accommodation Rate API Documentation

This document describes the REST API endpoints for managing accommodation rates.

---

## Table of Contents

1. [Overview](#overview)
2. [Base URL](#base-url)
3. [Authentication](#authentication)
4. [Response Format](#response-format)
5. [Endpoints](#endpoints)
   - [Get Rate by ID](#1-get-rate-by-id)
   - [Get All Rates](#2-get-all-rates)
   - [Update Rate](#3-update-rate)
   - [Delete Rates](#4-delete-rates)
   - [Bulk Upsert Rates](#5-bulk-upsert-rates)
6. [Data Types](#data-types)
7. [Error Codes](#error-codes)
8. [Validation Rules](#validation-rules)

---

## Overview

The Accommodation Rate API manages pricing for accommodations based on:

- **Accommodation** - The property being charged for
- **Season** - Accommodation-specific pricing season (High, Low, Shoulder, etc.)
- **Room Type** - Bed configuration (Single, Double, Twin, etc.)
- **Room Standard** - Room quality level (Standard, Deluxe, Suite, etc.)
- **Board Type** - Meal plan (Room Only, B&B, Half Board, Full Board, etc.)

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Accommodation Rate** | Rate determined by the combination of Accommodation + Season + Room Type + Room Standard + Board Type |
| **Accommodation-Specific Dimensions** | All four dimensions (Season, Room Type, Room Standard, Board Type) belong to the specific accommodation |
| **Rack Rate** | The price charged to the customer (revenue) |
| **STO Rate** | The cost we pay on behalf of the customer (expense/tour operator rate) |
| **Profit Amount** | Rack Rate - STO Rate (the profit we make per transaction) |
| **Profit Percentage** | (Profit Amount / Rack Rate) × 100 |

> **Business Rule:** Rack Rate must always be greater than or equal to STO Rate. We charge the customer at least what we pay on their behalf.

### Rate Dimensions

| Dimension | Description | Example Values |
|-----------|-------------|----------------|
| **Season** | Accommodation-specific pricing period | High Season, Low Season, Shoulder Season |
| **Room Type** | Bed configuration | Single Room, Double Room, Twin Room, Triple Room |
| **Room Standard** | Room quality level | Standard, Deluxe, Suite, Presidential |
| **Board Type** | Meal plan included | Room Only, B&B, Half Board, Full Board, All Inclusive |

---

## Base URL

```
/api/accommodation-rates
```

---

## Authentication

All endpoints require authentication via JWT token in the `Authorization` header.

```
Authorization: Bearer <token>
```

---

## Response Format

All responses follow this structure:

### Success Response

```json
{
  "status": 200,
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response

```json
{
  "status": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE"
}
```

---

## Endpoints

### 1. Get Rate by ID

Retrieves a single rate by its ID.

```
GET /api/accommodation-rates/{id}
```

**Permission Required:** `PERM_READ_ACCOMMODATION_RATE`

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated rate ID |

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Rate retrieved successfully",
  "data": {
    "id": "rate123",
    "accommodationId": "acc123",
    "accommodationName": "Serena Hotel",
    "seasonId": "season123",
    "seasonName": "High Season",
    "seasonType": "HIGH_SEASON",
    "roomTypeId": "rt123",
    "roomTypeName": "Double Room",
    "bedConfiguration": "1 King Bed",
    "roomStandardId": "rs123",
    "roomStandardName": "Deluxe",
    "boardTypeId": "bt123",
    "boardTypeName": "Full Board",
    "rackRate": 350.00,
    "stoRate": 300.00,
    "currency": "USD",
    "profitAmount": 50.00,
    "profitPercentage": 14.29,
    "notes": "Optional notes",
    "isActive": true,
    "createdAt": "2026-01-12T10:30:00",
    "updatedAt": "2026-01-12T10:30:00"
  }
}
```

#### Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `INVALID_ID` | Invalid rate ID format |
| 404 | `RATE_NOT_FOUND` | Rate not found |

---

### 2. Get All Rates

Retrieves rates with optional filtering and pagination.

```
GET /api/accommodation-rates
```

**Permission Required:** `PERM_READ_ACCOMMODATION_RATE`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accommodationId` | String | No | - | Filter by accommodation |
| `seasonId` | String | No | - | Filter by season |
| `roomTypeId` | String | No | - | Filter by room type |
| `roomStandardId` | String | No | - | Filter by room standard |
| `boardTypeId` | String | No | - | Filter by board type |
| `isActive` | Boolean | No | - | Filter by active status |
| `page` | Integer | No | `0` | Page number (0-indexed) |
| `size` | Integer | No | `10` | Page size |
| `sortDirection` | String | No | `desc` | Sort direction: `asc` or `desc` (always sorts by `createdAt`) |

#### Example Requests

```bash
# Get all rates for an accommodation
GET /api/accommodation-rates?accommodationId=acc123

# Get rates for a specific season
GET /api/accommodation-rates?accommodationId=acc123&seasonId=s1

# Get rates for a specific room configuration
GET /api/accommodation-rates?accommodationId=acc123&roomTypeId=rt1&roomStandardId=rs1

# Get rates with pagination
GET /api/accommodation-rates?accommodationId=acc123&page=0&size=20

# Filter by multiple dimensions
GET /api/accommodation-rates?accommodationId=acc123&seasonId=s1&roomTypeId=rt1&boardTypeId=bt1

# Get only active rates
GET /api/accommodation-rates?accommodationId=acc123&isActive=true
```

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Rates retrieved successfully",
  "data": {
    "rates": [
      {
        "id": "rate123",
        "accommodationId": "acc123",
        "accommodationName": "Serena Hotel",
        "seasonId": "season123",
        "seasonName": "High Season",
        "seasonType": "HIGH_SEASON",
        "roomTypeId": "rt123",
        "roomTypeName": "Double Room",
        "bedConfiguration": "1 King Bed",
        "roomStandardId": "rs123",
        "roomStandardName": "Deluxe",
        "boardTypeId": "bt123",
        "boardTypeName": "Full Board",
        "rackRate": 350.00,
        "stoRate": 300.00,
        "currency": "USD",
        "profitAmount": 50.00,
        "profitPercentage": 14.29,
        "notes": null,
        "isActive": true,
        "createdAt": "2026-01-12T10:30:00",
        "updatedAt": "2026-01-12T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 27,
    "totalPages": 3
  }
}
```

#### Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `INVALID_ACCOMMODATION_ID` | Invalid accommodation ID format |
| 400 | `INVALID_SEASON_ID` | Invalid season ID format |
| 400 | `INVALID_ROOM_TYPE_ID` | Invalid room type ID format |
| 400 | `INVALID_ROOM_STANDARD_ID` | Invalid room standard ID format |
| 400 | `INVALID_BOARD_TYPE_ID` | Invalid board type ID format |

---

### 3. Update Rate

Updates an existing rate. Only provided fields will be updated.

```
PUT /api/accommodation-rates/{id}
```

**Permission Required:** `PERM_UPDATE_ACCOMMODATION_RATE`

> **Note:** The unique key fields (`accommodationId`, `seasonId`, `roomTypeId`, `roomStandardId`, `boardTypeId`) cannot be changed. To modify these, delete the old rate and create a new one.

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated rate ID |

#### Request Body

```json
{
  "rackRate": 375.00,
  "stoRate": 320.00,
  "clearStoRate": false,
  "currency": "USD",
  "notes": "Updated notes",
  "isActive": true
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `rackRate` | BigDecimal | No | New rack rate (must be positive) |
| `stoRate` | BigDecimal | No | New STO rate (must be positive) |
| `clearStoRate` | Boolean | No | Set to `true` to remove the STO rate |
| `currency` | String | No | New currency code |
| `notes` | String | No | New notes |
| `isActive` | Boolean | No | New active status |

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Rate updated successfully",
  "data": {
    "id": "rate123",
    "accommodationId": "acc123",
    "accommodationName": "Serena Hotel",
    "rackRate": 375.00,
    "stoRate": 320.00,
    "currency": "USD",
    "notes": "Updated notes",
    "isActive": true,
    "updatedAt": "2026-01-12T11:00:00"
  }
}
```

#### Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `INVALID_ID` | Invalid rate ID format |
| 404 | `RATE_NOT_FOUND` | Rate not found |

---

### 4. Delete Rates

Deletes multiple rates by their IDs.

```
DELETE /api/accommodation-rates
```

**Permission Required:** `PERM_DELETE_ACCOMMODATION_RATE`

#### Request Body

```json
["rate123", "rate456", "rate789"]
```

Array of obfuscated rate IDs to delete.

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Deleted 3 rates successfully",
  "data": {
    "deleted": 3,
    "failed": 0,
    "failedIds": []
  }
}
```

#### Partial Success Response (200 OK)

When some IDs fail to delete:

```json
{
  "status": 200,
  "message": "Deleted 2 rates, 1 failed",
  "data": {
    "deleted": 2,
    "failed": 1,
    "failedIds": ["rate789"]
  }
}
```

---

### 5. Bulk Upsert Rates

Creates or updates multiple rates in a single request. The operation automatically determines whether to create or update based on the unique key combination (accommodation + season + room type + room standard + board type).

```
POST /api/accommodation-rates/bulk-upsert
```

**Permission Required:** `PERM_UPDATE_ACCOMMODATION_RATE`

#### Request Body

```json
[
  {
    "accommodationId": "acc123",
    "seasonId": "s1",
    "roomTypeId": "rt1",
    "roomStandardId": "rs1",
    "boardTypeId": "bt1",
    "rackRate": 350.00,
    "stoRate": 300.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  },
  {
    "accommodationId": "acc123",
    "seasonId": "s1",
    "roomTypeId": "rt1",
    "roomStandardId": "rs2",
    "boardTypeId": "bt1",
    "rackRate": 450.00,
    "stoRate": 380.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  }
]
```

#### Request Fields (per item)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `seasonId` | String | Yes | Obfuscated season ID (must belong to the accommodation) |
| `roomTypeId` | String | Yes | Obfuscated room type ID (must belong to the accommodation) |
| `roomStandardId` | String | Yes | Obfuscated room standard ID (must belong to the accommodation) |
| `boardTypeId` | String | Yes | Obfuscated board type ID (must belong to the accommodation) |
| `rackRate` | BigDecimal | Yes | Rack rate - price charged to customer (must be positive, >= stoRate) |
| `stoRate` | BigDecimal | No | STO rate - cost paid on behalf of customer (must be positive if provided, <= rackRate) |
| `currency` | String | Yes | ISO 4217 currency code (e.g., USD, EUR, TZS, KES) |
| `notes` | String | No | Optional notes |
| `isActive` | Boolean | No | Active status (defaults to `true`) |

#### How It Works

The upsert operation automatically determines create vs update:
- **Create**: If no existing rate matches the unique key combination
- **Update**: If an existing rate matches the unique key combination

The unique key for matching is:
- `accommodationId` + `seasonId` + `roomTypeId` + `roomStandardId` + `boardTypeId`

> **Note:** To delete rates, use the [Delete Rates](#4-delete-rates) endpoint.

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Bulk upsert completed",
  "data": {
    "totalProcessed": 3,
    "created": 2,
    "updated": 1,
    "failed": 0,
    "successful": 3,
    "errors": []
  }
}
```

#### Partial Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Bulk upsert completed",
  "data": {
    "totalProcessed": 4,
    "created": 1,
    "updated": 1,
    "failed": 2,
    "successful": 2,
    "errors": [
      "Season does not belong to accommodation: High Season",
      "Rack rate (300.00) cannot be less than STO rate (350.00)"
    ]
  }
}
```

---

## Data Types

### AccommodationRateDTO (Response)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated rate ID |
| `accommodationId` | String | Obfuscated accommodation ID |
| `accommodationName` | String | Accommodation name |
| `seasonId` | String | Obfuscated season ID |
| `seasonName` | String | Season name |
| `seasonType` | String | Season type (HIGH_SEASON, LOW_SEASON, etc.) |
| `roomTypeId` | String | Obfuscated room type ID |
| `roomTypeName` | String | Room type name |
| `bedConfiguration` | String | Bed configuration description |
| `roomStandardId` | String | Obfuscated room standard ID |
| `roomStandardName` | String | Room standard name |
| `boardTypeId` | String | Obfuscated board type ID |
| `boardTypeName` | String | Board type name |
| `rackRate` | BigDecimal | Rack rate - price charged to customer (revenue) |
| `stoRate` | BigDecimal | STO rate - cost paid on behalf of customer (expense) |
| `currency` | String | ISO 4217 currency code |
| `profitAmount` | BigDecimal | Calculated: Rack Rate - STO Rate |
| `profitPercentage` | BigDecimal | Calculated: (Profit Amount / Rack Rate) × 100 |
| `notes` | String | Optional notes |
| `isActive` | Boolean | Active status |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `INVALID_ID` | 400 | Invalid rate ID format |
| `INVALID_ACCOMMODATION_ID` | 400 | Invalid accommodation ID format |
| `INVALID_SEASON_ID` | 400 | Invalid season ID format |
| `INVALID_ROOM_TYPE_ID` | 400 | Invalid room type ID format |
| `INVALID_ROOM_STANDARD_ID` | 400 | Invalid room standard ID format |
| `INVALID_BOARD_TYPE_ID` | 400 | Invalid board type ID format |
| `ACCOMMODATION_NOT_FOUND` | 404 | Accommodation not found |
| `SEASON_NOT_FOUND` | 404 | Season not found |
| `ROOM_TYPE_NOT_FOUND` | 404 | Room type not found |
| `ROOM_STANDARD_NOT_FOUND` | 404 | Room standard not found |
| `BOARD_TYPE_NOT_FOUND` | 404 | Board type not found |
| `RATE_NOT_FOUND` | 404 | Rate not found |
| `SEASON_NOT_FOR_ACCOMMODATION` | 400 | Season does not belong to the accommodation |
| `ROOM_TYPE_NOT_FOR_ACCOMMODATION` | 400 | Room type does not belong to the accommodation |
| `ROOM_STANDARD_NOT_FOR_ACCOMMODATION` | 400 | Room standard does not belong to the accommodation |
| `BOARD_TYPE_NOT_FOR_ACCOMMODATION` | 400 | Board type does not belong to the accommodation |
| `RATE_ALREADY_EXISTS` | 400 | Rate already exists for this combination |
| `INVALID_CURRENCY` | 400 | Invalid ISO 4217 currency code |
| `RACK_RATE_LESS_THAN_STO` | 400 | Rack rate cannot be less than STO rate |
| `FETCH_FAILED` | 500 | Internal error fetching data |
| `CREATE_FAILED` | 500 | Internal error creating rate |
| `UPDATE_FAILED` | 500 | Internal error updating rate |
| `DELETE_FAILED` | 500 | Internal error deleting rate |

---

## Validation Rules

### Dimension Ownership

All four dimensions (Season, Room Type, Room Standard, Board Type) must belong to the specified accommodation:
1. `seasonId` must reference a season that belongs to the accommodation
2. `roomTypeId` must reference a room type that belongs to the accommodation
3. `roomStandardId` must reference a room standard that belongs to the accommodation
4. `boardTypeId` must reference a board type that belongs to the accommodation

### Rate Values

- `rackRate` must be positive (> 0)
- `stoRate` must be positive (> 0) if provided
- `rackRate` must be >= `stoRate` (cannot charge less than what we pay)
- `currency` must be a valid ISO 4217 currency code (e.g., USD, EUR, TZS, KES)
- `isActive` defaults to `true` if not provided

### Rate Relationship

| Field | Meaning | Example |
|-------|---------|---------|
| `rackRate` | Price charged to customer | $350.00 |
| `stoRate` | Cost we pay on customer's behalf | $300.00 |
| `profitAmount` | rackRate - stoRate | $50.00 |
| `profitPercentage` | (profitAmount / rackRate) × 100 | 14.29% |

### Unique Constraint

Each rate must have a unique combination of:
- `accommodationId`
- `seasonId`
- `roomTypeId`
- `roomStandardId`
- `boardTypeId`

Attempting to create a duplicate will return `RATE_ALREADY_EXISTS` error.

---

## Related Documentation

- [Accommodation Rate Matrix API](ACCOMMODATION_RATE_MATRIX_API_DOCUMENTATION.md) - For rendering rate input grids
