# Activity Tariff Rate API Documentation

This document describes the REST API endpoints for managing activity tariff rates.

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

The Activity Tariff Rate API manages pricing for activities based on:

- **Activity** - The activity being charged for
- **Park** - Optional park-specific rates (null for global rates)
- **Season** - Pricing season (High, Low, Shoulder, etc.)
- **Nation Category** - Passenger nationality (EAC, Non-EAC, Expatriate, etc.)
- **Age Category** - Passenger age group (only for `PER_PERSON` charging basis)

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Global Rate** | Rate that applies to an activity regardless of park (`parkId = null`) |
| **Park-Specific Rate** | Rate that applies only to a specific park |
| **Rack Rate** | The price charged to the customer (revenue) |
| **STO Rate** | The cost we pay on behalf of the customer (expense) |
| **Profit Amount** | Rack Rate - STO Rate (the profit we make per transaction) |
| **Profit Percentage** | (Profit Amount / Rack Rate) × 100 |
| **Charging Basis** | How the activity is charged: `PER_PERSON`, `PER_VEHICLE`, `PER_GROUP`, etc. |

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
/api/activity-tariff-rates
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
GET /api/activity-tariff-rates/{id}
```

**Permission Required:** `PERM_READ_ACTIVITY_TARIFF_RATE`

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
    "activityId": "abc123",
    "activityName": "Game Drive",
    "activityChargingBasis": "Per Person",
    "parkId": null,
    "parkName": null,
    "isGlobalRate": true,
    "seasonId": "season123",
    "seasonName": "High Season",
    "seasonType": "High Season",
    "nationCategoryId": "nation123",
    "nationCategoryName": "EAC",
    "ageCategoryId": "age123",
    "ageCategoryName": "Adult",
    "ageCategoryAgeRange": "15+ years",
    "rackRate": 50.00,
    "stoRate": 45.00,
    "currency": "USD",
    "profitAmount": 5.00,
    "profitPercentage": 10.00,
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
GET /api/activity-tariff-rates
```

**Permission Required:** `PERM_READ_ACTIVITY_TARIFF_RATE`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `activityId` | String | No | - | Filter by activity |
| `parkId` | String | No | - | Filter by park |
| `globalOnly` | Boolean | No | - | If `true`, only return global rates (no park) |
| `seasonId` | String | No | - | Filter by season |
| `nationCategoryId` | String | No | - | Filter by nation category |
| `ageCategoryId` | String | No | - | Filter by age category |
| `isActive` | Boolean | No | - | Filter by active status |
| `page` | Integer | No | `0` | Page number (0-indexed) |
| `size` | Integer | No | `10` | Page size |
| `sortDirection` | String | No | `desc` | Sort direction: `asc` or `desc` (always sorts by `createdAt`) |

#### Example Requests

```bash
# Get all rates for an activity
GET /api/activity-tariff-rates?activityId=abc123

# Get global rates only
GET /api/activity-tariff-rates?activityId=abc123&globalOnly=true

# Get rates for a specific park
GET /api/activity-tariff-rates?activityId=abc123&parkId=park123

# Get rates with pagination
GET /api/activity-tariff-rates?activityId=abc123&page=0&size=20

# Filter by season and nation category
GET /api/activity-tariff-rates?activityId=abc123&seasonId=s1&nationCategoryId=nc1

# Get only active rates
GET /api/activity-tariff-rates?activityId=abc123&isActive=true
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
        "activityId": "abc123",
        "activityName": "Game Drive",
        "activityChargingBasis": "Per Person",
        "parkId": null,
        "parkName": null,
        "isGlobalRate": true,
        "seasonId": "season123",
        "seasonName": "High Season",
        "seasonType": "High Season",
        "nationCategoryId": "nation123",
        "nationCategoryName": "EAC",
        "ageCategoryId": "age123",
        "ageCategoryName": "Adult",
        "ageCategoryAgeRange": "15+ years",
        "rackRate": 50.00,
        "stoRate": 45.00,
        "currency": "USD",
        "profitAmount": 5.00,
    "profitPercentage": 10.00,
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
| 400 | `INVALID_ACTIVITY_ID` | Invalid activity ID format |
| 400 | `INVALID_PARK_ID` | Invalid park ID format |
| 400 | `INVALID_SEASON_ID` | Invalid season ID format |
| 400 | `INVALID_NATION_CATEGORY_ID` | Invalid nation category ID format |
| 400 | `INVALID_AGE_CATEGORY_ID` | Invalid age category ID format |

---

### 3. Update Rate

Updates an existing rate. Only provided fields will be updated.

```
PUT /api/activity-tariff-rates/{id}
```

**Permission Required:** `PERM_UPDATE_ACTIVITY_TARIFF_RATE`

> **Note:** The unique key fields (`activityId`, `parkId`, `seasonId`, `nationCategoryId`, `ageCategoryId`) cannot be changed. To modify these, delete the old rate and create a new one.

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated rate ID |

#### Request Body

```json
{
  "rackRate": 55.00,
  "stoRate": 50.00,
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
    "activityId": "abc123",
    "activityName": "Game Drive",
    "rackRate": 55.00,
    "stoRate": 50.00,
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
DELETE /api/activity-tariff-rates
```

**Permission Required:** `PERM_DELETE_ACTIVITY_TARIFF_RATE`

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

Creates or updates multiple rates in a single request. The operation automatically determines whether to create or update based on the unique key combination (activity + park + season + nation category + age category).

```
POST /api/activity-tariff-rates/bulk-upsert
```

**Permission Required:** `PERM_UPDATE_ACTIVITY_TARIFF_RATE`

#### Request Body

```json
[
  {
    "activityId": "abc123",
    "parkId": null,
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac1",
    "rackRate": 50.00,
    "stoRate": 45.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  },
  {
    "activityId": "abc123",
    "parkId": null,
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac2",
    "rackRate": 25.00,
    "stoRate": 20.00,
    "currency": "USD",
    "notes": null,
    "isActive": true
  }
]
```

#### Request Fields (per item)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | String | Yes | Obfuscated activity ID |
| `parkId` | String | No | Obfuscated park ID. `null` for global rate |
| `seasonId` | String | Yes | Obfuscated season ID |
| `nationCategoryId` | String | Yes | Obfuscated nation category ID |
| `ageCategoryId` | String | Conditional | Required for `PER_PERSON` activities |
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
- `activityId` + `parkId` + `seasonId` + `nationCategoryId` + `ageCategoryId`

> **Note:** To delete rates, use the [Delete Rates](#5-delete-rates) endpoint.

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
      "Age category required for PER_PERSON activity: Game Drive",
      "Rack rate (40.00) cannot be less than STO rate (50.00)"
    ]
  }
}
```

---

## Data Types

### ActivityTariffRateDTO (Response)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated rate ID |
| `activityId` | String | Obfuscated activity ID |
| `activityName` | String | Activity name |
| `activityChargingBasis` | String | Charging basis display name |
| `parkId` | String | Obfuscated park ID (null for global) |
| `parkName` | String | Park name (null for global) |
| `isGlobalRate` | Boolean | `true` if no park assigned |
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
| `INVALID_ACTIVITY_ID` | 400 | Invalid activity ID format |
| `INVALID_PARK_ID` | 400 | Invalid park ID format |
| `INVALID_SEASON_ID` | 400 | Invalid season ID format |
| `INVALID_NATION_CATEGORY_ID` | 400 | Invalid nation category ID format |
| `INVALID_AGE_CATEGORY_ID` | 400 | Invalid age category ID format |
| `ACTIVITY_NOT_FOUND` | 404 | Activity not found |
| `PARK_NOT_FOUND` | 404 | Park not found |
| `SEASON_NOT_FOUND` | 404 | Season not found |
| `NATION_CATEGORY_NOT_FOUND` | 404 | Nation category not found |
| `AGE_CATEGORY_NOT_FOUND` | 404 | Age category not found |
| `RATE_NOT_FOUND` | 404 | Rate not found |
| `ACTIVITY_NO_TARIFF` | 400 | Activity has `hasTariff=false` |
| `ACTIVITY_NO_CHARGING_BASIS` | 400 | Activity has no charging basis |
| `AGE_CATEGORY_REQUIRED` | 400 | Age category required for PER_PERSON |
| `AGE_CATEGORY_NOT_ALLOWED` | 400 | Age category not allowed for non-PER_PERSON |
| `RATE_ALREADY_EXISTS` | 400 | Rate already exists for this combination |
| `INVALID_CURRENCY` | 400 | Invalid ISO 4217 currency code |
| `RACK_RATE_LESS_THAN_STO` | 400 | Rack rate cannot be less than STO rate |
| `FETCH_FAILED` | 500 | Internal error fetching data |
| `CREATE_FAILED` | 500 | Internal error creating rate |
| `UPDATE_FAILED` | 500 | Internal error updating rate |
| `DELETE_FAILED` | 500 | Internal error deleting rate |

---

## Validation Rules

### Activity Eligibility

An activity can only have rates if:
1. `hasTariff = true`
2. `chargingBasis` is not null

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
| `rackRate` | Price charged to customer | $50.00 |
| `stoRate` | Cost we pay on customer's behalf | $45.00 |
| `profitAmount` | rackRate - stoRate | $5.00 |
| `profitPercentage` | (profitAmount / rackRate) × 100 | 10.00% |

### Unique Constraint

Each rate must have a unique combination of:
- `activityId`
- `parkId` (or null for global)
- `seasonId`
- `nationCategoryId`
- `ageCategoryId` (or null for non-PER_PERSON)

Attempting to create a duplicate will return `RATE_ALREADY_EXISTS` error.

---

## Related Documentation

- [Activity Tariff Rate Matrix API](ACTIVITY_TARIFF_RATE_MATRIX_API_DOCUMENTATION.md) - For rendering rate input grids
