# Park Tariff Rate API Documentation

This document describes the REST API endpoints for managing park tariff rates.

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

The Park Tariff Rate API manages pricing for park tariffs based on:

- **Park** - The park being charged for (always required)
- **Tariff** - The tariff type (e.g., Park Entry Fee, Concession Fee)
- **Season** - Pricing season (High, Low, Shoulder, etc.)
- **Nation Category** - Passenger nationality (EAC, Non-EAC, Expatriate, etc.)
- **Age Category** - Passenger age group (only for `PER_PERSON` charging basis)

> **Note:** Unlike Activity Tariff Rates, Park Tariff Rates are **always park-specific**. There are no global rates for park tariffs.

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Rack Rate** | The price charged to the customer (revenue) |
| **STO Rate** | The cost we pay on behalf of the customer (expense) |
| **Profit Amount** | Rack Rate - STO Rate (the profit we make per transaction) |
| **Profit Percentage** | (Profit Amount / Rack Rate) × 100 |
| **Charging Basis** | How the tariff is charged: `PER_PERSON`, `PER_VEHICLE`, `PER_GROUP`, etc. |

> **Business Rule:** Rack Rate must always be greater than or equal to STO Rate. We charge the customer at least what we pay on their behalf.

### Charging Basis & Age Categories

| Charging Basis | Age Category Required |
|----------------|----------------------|
| `PER_PERSON` | **Yes** - must provide `ageCategoryId` |
| `PER_VEHICLE` | **No** - `ageCategoryId` must be null |
| `PER_GROUP` | **No** - `ageCategoryId` must be null |
| `PER_DAY` | **No** - `ageCategoryId` must be null |
| `PER_HOUR` | **No** - `ageCategoryId` must be null |
| `PER_SESSION` | **No** - `ageCategoryId` must be null |
| `FLAT_RATE` | **No** - `ageCategoryId` must be null |

---

## Base URL

```
/api/park-tariff-rates
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
GET /api/park-tariff-rates/{id}
```

**Permission Required:** `PERM_READ_PARK_TARIFF_RATE`

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
    "parkId": "park123",
    "parkName": "Serengeti National Park",
    "tariffId": "tariff123",
    "tariffName": "Park Entry Fee",
    "tariffChargingBasis": "Per Person",
    "seasonId": "season123",
    "seasonName": "High Season",
    "seasonType": "High Season",
    "nationCategoryId": "nation123",
    "nationCategoryName": "EAC",
    "ageCategoryId": "age123",
    "ageCategoryName": "Adult",
    "ageCategoryAgeRange": "15+ years",
    "rackRate": 70.00,
    "stoRate": 60.00,
    "currency": "USD",
    "profitAmount": 10.00,
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
GET /api/park-tariff-rates
```

**Permission Required:** `PERM_READ_PARK_TARIFF_RATE`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `parkId` | String | No | - | Filter by park |
| `tariffId` | String | No | - | Filter by tariff |
| `seasonId` | String | No | - | Filter by season |
| `nationCategoryId` | String | No | - | Filter by nation category |
| `ageCategoryId` | String | No | - | Filter by age category |
| `isActive` | Boolean | No | - | Filter by active status |
| `page` | Integer | No | `0` | Page number (0-indexed) |
| `size` | Integer | No | `10` | Page size |
| `sortDirection` | String | No | `desc` | Sort direction: `asc` or `desc` (always sorts by `createdAt`) |

#### Example Requests

```bash
# Get all rates for a park
GET /api/park-tariff-rates?parkId=park123

# Get all rates for a tariff
GET /api/park-tariff-rates?tariffId=tariff123

# Get rates for a specific park-tariff combination
GET /api/park-tariff-rates?parkId=park123&tariffId=tariff123

# Get rates with pagination
GET /api/park-tariff-rates?parkId=park123&page=0&size=20

# Filter by season and nation category
GET /api/park-tariff-rates?parkId=park123&seasonId=s1&nationCategoryId=nc1

# Get only active rates
GET /api/park-tariff-rates?parkId=park123&isActive=true
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
        "parkId": "park123",
        "parkName": "Serengeti National Park",
        "tariffId": "tariff123",
        "tariffName": "Park Entry Fee",
        "tariffChargingBasis": "Per Person",
        "seasonId": "season123",
        "seasonName": "High Season",
        "seasonType": "High Season",
        "nationCategoryId": "nation123",
        "nationCategoryName": "EAC",
        "ageCategoryId": "age123",
        "ageCategoryName": "Adult",
        "ageCategoryAgeRange": "15+ years",
        "rackRate": 70.00,
        "stoRate": 60.00,
        "currency": "USD",
        "profitAmount": 10.00,
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
| 400 | `INVALID_PARK_ID` | Invalid park ID format |
| 400 | `INVALID_TARIFF_ID` | Invalid tariff ID format |
| 400 | `INVALID_SEASON_ID` | Invalid season ID format |
| 400 | `INVALID_NATION_CATEGORY_ID` | Invalid nation category ID format |
| 400 | `INVALID_AGE_CATEGORY_ID` | Invalid age category ID format |

---

### 3. Update Rate

Updates an existing rate. Only provided fields will be updated.

```
PUT /api/park-tariff-rates/{id}
```

**Permission Required:** `PERM_UPDATE_PARK_TARIFF_RATE`

> **Note:** The unique key fields (`parkId`, `tariffId`, `seasonId`, `nationCategoryId`, `ageCategoryId`) cannot be changed. To modify these, delete the old rate and create a new one.

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated rate ID |

#### Request Body

```json
{
  "rackRate": 75.00,
  "stoRate": 65.00,
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
    "parkId": "park123",
    "parkName": "Serengeti National Park",
    "tariffId": "tariff123",
    "tariffName": "Park Entry Fee",
    "rackRate": 75.00,
    "stoRate": 65.00,
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
DELETE /api/park-tariff-rates
```

**Permission Required:** `PERM_DELETE_PARK_TARIFF_RATE`

#### Request Body

```json
["rate123", "rate456", "rate789"]
```

Array of obfuscated rate IDs to delete.

#### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "3 rate(s) deleted successfully",
  "data": null
}
```

#### Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `NO_IDS` | No rate IDs provided |
| 400 | `INVALID_IDS` | One or more rate IDs are invalid |
| 400 | `RATES_NOT_FOUND` | No rates found to delete |

---

### 5. Bulk Upsert Rates

Creates or updates multiple rates in a single request. The operation automatically determines whether to create or update based on the unique key combination (park + tariff + season + nation category + age category).

```
POST /api/park-tariff-rates/bulk-upsert
```

**Permission Required:** `PERM_UPDATE_PARK_TARIFF_RATE`

#### Request Body

```json
[
  {
    "parkId": "park123",
    "tariffId": "tariff123",
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac1",
    "rackRate": 70.00,
    "stoRate": 60.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  },
  {
    "parkId": "park123",
    "tariffId": "tariff123",
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac2",
    "rackRate": 35.00,
    "stoRate": 30.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  }
]
```

#### Request Fields (per item)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `parkId` | String | Yes | Obfuscated park ID |
| `tariffId` | String | Yes | Obfuscated tariff ID |
| `seasonId` | String | Yes | Obfuscated season ID |
| `nationCategoryId` | String | Yes | Obfuscated nation category ID |
| `ageCategoryId` | String | Conditional | Required for `PER_PERSON` tariffs |
| `rackRate` | BigDecimal | Yes | Rack rate - price charged to customer (must be positive, >= stoRate) |
| `stoRate` | BigDecimal | Yes | STO rate - cost paid on behalf of customer (must be positive, <= rackRate) |
| `currency` | String | Yes | ISO 4217 currency code (e.g., USD, EUR, TZS, KES) |
| `notes` | String | No | Optional notes |
| `isActive` | Boolean | No | Active status (defaults to `true`) |

#### How It Works

The upsert operation automatically determines create vs update:
- **Create**: If no existing rate matches the unique key combination
- **Update**: If an existing rate matches the unique key combination

The unique key for matching is:
- `parkId` + `tariffId` + `seasonId` + `nationCategoryId` + `ageCategoryId`

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
      "Age category required for PER_PERSON tariff: Park Entry Fee",
      "Rack rate (40.00) cannot be less than STO rate (50.00)"
    ]
  }
}
```

---

## Data Types

### ParkTariffRateDTO (Response)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated rate ID |
| `parkId` | String | Obfuscated park ID |
| `parkName` | String | Park name |
| `tariffId` | String | Obfuscated tariff ID |
| `tariffName` | String | Tariff name |
| `tariffChargingBasis` | String | Charging basis display name |
| `seasonId` | String | Obfuscated season ID |
| `seasonName` | String | Season name |
| `seasonType` | String | Season type display name |
| `nationCategoryId` | String | Obfuscated nation category ID |
| `nationCategoryName` | String | Nation category name |
| `ageCategoryId` | String | Obfuscated age category ID (null for non-PER_PERSON) |
| `ageCategoryName` | String | Age category name (null for non-PER_PERSON) |
| `ageCategoryAgeRange` | String | Age range display (e.g., "15+ years") |
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
| `INVALID_PARK_ID` | 400 | Invalid park ID format |
| `INVALID_TARIFF_ID` | 400 | Invalid tariff ID format |
| `INVALID_SEASON_ID` | 400 | Invalid season ID format |
| `INVALID_NATION_CATEGORY_ID` | 400 | Invalid nation category ID format |
| `INVALID_AGE_CATEGORY_ID` | 400 | Invalid age category ID format |
| `PARK_NOT_FOUND` | 404 | Park not found |
| `TARIFF_NOT_FOUND` | 404 | Tariff not found |
| `SEASON_NOT_FOUND` | 404 | Season not found |
| `NATION_CATEGORY_NOT_FOUND` | 404 | Nation category not found |
| `AGE_CATEGORY_NOT_FOUND` | 404 | Age category not found |
| `RATE_NOT_FOUND` | 404 | Rate not found |
| `PARK_TARIFF_NOT_FOUND` | 400 | Park-tariff relationship does not exist |
| `TARIFF_NO_CHARGING_BASIS` | 400 | Tariff has no charging basis set |
| `AGE_CATEGORY_REQUIRED` | 400 | Age category required for PER_PERSON tariffs |
| `AGE_CATEGORY_NOT_ALLOWED` | 400 | Age category not allowed for non-PER_PERSON tariffs |
| `RATE_ALREADY_EXISTS` | 400 | Rate already exists for this combination |
| `INVALID_CURRENCY` | 400 | Invalid ISO 4217 currency code |
| `RACK_RATE_LESS_THAN_STO` | 400 | Rack rate cannot be less than STO rate |
| `NO_IDS` | 400 | No rate IDs provided for deletion |
| `INVALID_IDS` | 400 | One or more rate IDs are invalid |
| `RATES_NOT_FOUND` | 400 | No rates found to delete |
| `FETCH_FAILED` | 500 | Internal error fetching data |
| `UPDATE_FAILED` | 500 | Internal error updating rate |
| `DELETE_FAILED` | 500 | Internal error deleting rate |

---

## Validation Rules

### Park-Tariff Relationship

A rate can only be created if:
1. The park exists
2. The tariff exists
3. The park-tariff relationship exists (the tariff is associated with the park)
4. The tariff has a `chargingBasis` set

### Age Category Validation

| Charging Basis | Age Category |
|----------------|--------------|
| `PER_PERSON` | **Required** - must provide valid `ageCategoryId` |
| All others | **Forbidden** - `ageCategoryId` must be null |

### Rate Values

- `rackRate` must be positive (> 0)
- `stoRate` must be positive (> 0)
- `rackRate` must be >= `stoRate` (cannot charge less than what we pay)
- `currency` must be a valid ISO 4217 currency code (e.g., USD, EUR, TZS, KES)
- `isActive` defaults to `true` if not provided

### Rate Relationship

| Field | Meaning | Example |
|-------|---------|---------|
| `rackRate` | Price charged to customer | $70.00 |
| `stoRate` | Cost we pay on customer's behalf | $60.00 |
| `profitAmount` | rackRate - stoRate | $10.00 |
| `profitPercentage` | (profitAmount / rackRate) × 100 | 14.29% |

### Unique Constraint

Each rate must have a unique combination of:
- `parkId`
- `tariffId`
- `seasonId`
- `nationCategoryId`
- `ageCategoryId` (or null for non-PER_PERSON)

Attempting to create a duplicate will return `RATE_ALREADY_EXISTS` error.

---

## Related Documentation

- [Park Tariff Rate Matrix API](PARK_TARIFF_RATE_MATRIX_API_DOCUMENTATION.md) - For rendering rate input grids
