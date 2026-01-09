# Season Management API Documentation

## Overview

The Season Management API provides comprehensive endpoints for managing seasons and season periods in the Kabengo Safaris tourism platform. The API supports two types of seasons:

1. **Global Seasons**: Not tied to any accommodation, used by parks for general pricing
2. **Accommodation-Specific Seasons**: Tied to specific accommodations for their pricing management

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Season API](#season-api)
   - [Create Season](#1-create-season)
   - [Update Season](#2-update-season)
   - [Get Season by ID](#3-get-season-by-id)
   - [Get All Seasons](#4-get-all-seasons)
   - [Delete Seasons](#5-delete-seasons)

2. [Season Period API](#season-period-api)
   - [Create Season Period](#1-create-season-period)
   - [Update Season Period](#2-update-season-period)
   - [Get Season Period by ID](#3-get-season-period-by-id)
   - [Get All Season Periods](#4-get-all-season-periods)
   - [Delete Season Periods](#5-delete-season-periods)

3. [Data Models](#data-models)
4. [Error Codes](#error-codes)
5. [Examples](#examples)

---

## Season API

Base URL: `/api/seasons`

### 1. Create Season

Creates a new season (global or accommodation-specific).

**Endpoint:** `POST /api/seasons`

**Permission Required:** `PERM_CREATE_SEASON`

**Request Body:**
```json
{
  "name": "High Season",
  "seasonType": "HIGH_SEASON",
  "description": "Peak tourism period",
  "accommodationId": "abc123xyz",  // Optional: omit or null for global season
  "isActive": true,
  "seasonPeriods": [  // Optional: can be added later
    {
      "startDate": "06-01",  // MM-DD format
      "endDate": "08-31",
      "year": 2024,  // Optional: null for recurring periods
      "notes": "Summer season",
      "isActive": true
    }
  ]
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Season created successfully",
  "data": {
    "id": "xyz789abc",
    "name": "High Season",
    "seasonType": "HIGH_SEASON",
    "description": "Peak tourism period",
    "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
    "isActive": true,
    "accommodationId": "abc123xyz",
    "accommodationName": "Safari Lodge",
    "seasonPeriods": [
      {
        "id": "per123xyz",
        "seasonId": "xyz789abc",
        "seasonName": "High Season",
        "startDate": "06-01",
        "endDate": "08-31",
        "year": 2024,
        "notes": "Summer season",
        "isActive": true,
        "isRecurring": false,
        "isYearWrapping": false,
        "durationDays": 91,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid input data
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Season name is required",
    "errorCode": "MISSING_SEASON_NAME",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Duplicate season name
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Season name already exists for this accommodation",
    "errorCode": "DUPLICATE_SEASON_NAME",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Accommodation not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Accommodation not found",
    "errorCode": "ACCOMMODATION_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

**Notes:**
- To create a **global season**, omit `accommodationId` or set it to `null`
- To create an **accommodation-specific season**, provide valid `accommodationId`
- Season name must be unique within the accommodation (or globally for global seasons)
- `seasonPeriods` can be provided at creation or added separately later

---

### 2. Update Season

Updates an existing season.

**Endpoint:** `PUT /api/seasons/{id}`

**Permission Required:** `PERM_UPDATE_SEASON`

**Path Parameters:**
- `id` (string, required): Obfuscated season ID

**Request Body:**
```json
{
  "name": "High Season Updated",
  "seasonType": "PEAK_SEASON",
  "description": "Updated peak tourism period",
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Season updated successfully",
  "data": {
    "id": "xyz789abc",
    "name": "High Season Updated",
    "seasonType": "PEAK_SEASON",
    "description": "Updated peak tourism period",
    "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
    "isActive": true,
    "accommodationId": "abc123xyz",
    "accommodationName": "Safari Lodge",
    "seasonPeriods": [...],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  },
  "timestamp": "2024-01-15T11:45:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Season not found
- **400 Bad Request** - Duplicate season name

**Notes:**
- Cannot change season from global to accommodation-specific or vice versa
- Cannot update accommodation association after creation
- Season name must remain unique within its scope

---

### 3. Get Season by ID

Retrieves a single season by its ID.

**Endpoint:** `GET /api/seasons/{id}`

**Permission Required:** `PERM_READ_SEASON`

**Path Parameters:**
- `id` (string, required): Obfuscated season ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Season retrieved successfully",
  "data": {
    "id": "xyz789abc",
    "name": "High Season",
    "seasonType": "HIGH_SEASON",
    "description": "Peak tourism period",
    "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
    "isActive": true,
    "accommodationId": "abc123xyz",
    "accommodationName": "Safari Lodge",
    "seasonPeriods": [
      {
        "id": "per123xyz",
        "seasonId": "xyz789abc",
        "seasonName": "High Season",
        "startDate": "06-01",
        "endDate": "08-31",
        "year": 2024,
        "notes": "Summer season",
        "isActive": true,
        "isRecurring": false,
        "isYearWrapping": false,
        "durationDays": 91,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Season not found

---

### 4. Get All Seasons

Retrieves all seasons with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/seasons`

**Permission Required:** `PERM_READ_SEASON`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `name` | string | No | - | Filter by name (partial match, case-insensitive) |
| `seasonType` | enum | No | - | Filter by season type (HIGH_SEASON, LOW_SEASON, etc.) |
| `isActive` | boolean | No | - | Filter by active status |
| `isGlobal` | boolean | No | - | Filter by global status (true = global, false = accommodation-specific) |
| `isSystem` | boolean | No | - | Filter by system status (true = system/protected, false = user-created) |
| `accommodationId` | string | No | - | Filter by accommodation ID (for accommodation-specific seasons) |
| `description` | string | No | - | Filter by description (partial match, case-insensitive) |
| `keyword` | string | No | - | Search keyword across name and description |
| `hasPeriods` | boolean | No | - | Filter seasons that have periods (true) or no periods (false) |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 10 | Page size |
| `sortDirection` | string | No | desc | Sort direction (asc or desc) by createdAt |

**Example Requests:**

1. Get all global seasons:
   ```
   GET /api/seasons?isGlobal=true
   ```

2. Get all seasons for a specific accommodation:
   ```
   GET /api/seasons?accommodationId=abc123xyz
   ```

3. Search for seasons by keyword:
   ```
   GET /api/seasons?keyword=summer&page=0&size=20
   ```

4. Get active high seasons:
   ```
   GET /api/seasons?seasonType=HIGH_SEASON&isActive=true
   ```

5. Get seasons with periods:
   ```
   GET /api/seasons?hasPeriods=true
   ```

6. Get system seasons only (protected core seasons):
   ```
   GET /api/seasons?isSystem=true
   ```

7. Get user-created seasons (non-system):
   ```
   GET /api/seasons?isSystem=false
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Seasons retrieved successfully",
  "data": {
    "seasons": [
      {
        "id": "xyz789abc",
        "name": "High Season",
        "seasonType": "HIGH_SEASON",
        "description": "Peak tourism period",
        "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
        "isActive": true,
        "accommodationId": "abc123xyz",
        "accommodationName": "Safari Lodge",
        "seasonPeriods": [...],
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T11:45:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 3
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Notes:**
- All filters are optional and can be combined
- Results are sorted by `createdAt` timestamp
- Use `isGlobal=true` to get only global seasons
- Use `isGlobal=false` to get only accommodation-specific seasons
- Use `accommodationId` to get seasons for a specific accommodation

---

### 5. Delete Seasons

Deletes multiple seasons by their IDs.

**Endpoint:** `DELETE /api/seasons`

**Permission Required:** `PERM_DELETE_SEASON`

**Request Body:**
```json
["xyz789abc", "def456ghi", "jkl123mno"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deleted 2 seasons. 1 global seasons were protected from deletion.",
  "data": null,
  "timestamp": "2024-01-15T12:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Empty ID list
- **500 Internal Server Error** - Deletion failed

**Important Notes:**
- **Global seasons are protected from deletion** and will be skipped
- Associated season periods are automatically deleted (cascade)
- The response indicates how many seasons were deleted and how many global seasons were protected
- Accommodation-specific seasons can be deleted normally

---

## Season Period API

Base URL: `/api/season-periods`

### 1. Create Season Period

Creates a new season period for an existing season.

**Endpoint:** `POST /api/season-periods`

**Permission Required:** `PERM_CREATE_SEASON_PERIOD`

**Request Body:**
```json
{
  "seasonId": "xyz789abc",
  "startDate": "06-01",  // MM-DD format
  "endDate": "08-31",
  "year": 2024,  // Optional: null for recurring periods
  "notes": "Summer season period",
  "isActive": true
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Season period created successfully",
  "data": {
    "id": "per123xyz",
    "seasonId": "xyz789abc",
    "seasonName": "High Season",
    "startDate": "06-01",
    "endDate": "08-31",
    "year": 2024,
    "notes": "Summer season period",
    "isActive": true,
    "isRecurring": false,
    "isYearWrapping": false,
    "durationDays": 91,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid season ID
- **404 Not Found** - Season not found
- **400 Bad Request** - Invalid date range
- **400 Bad Request** - Overlapping period exists

**Notes:**
- `startDate` and `endDate` use MM-DD format (year-agnostic)
- `year` can be null for recurring annual periods
- `isRecurring` is automatically determined (true if year is null)
- `isYearWrapping` is automatically calculated (e.g., Dec 15 - Jan 15)
- `durationDays` is automatically calculated
- Overlapping periods for the same season and year are not allowed

---

### 2. Update Season Period

Updates an existing season period.

**Endpoint:** `PUT /api/season-periods/{id}`

**Permission Required:** `PERM_UPDATE_SEASON_PERIOD`

**Path Parameters:**
- `id` (string, required): Obfuscated season period ID

**Request Body:**
```json
{
  "startDate": "06-15",
  "endDate": "09-15",
  "year": 2024,
  "notes": "Extended summer season",
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Season period updated successfully",
  "data": {
    "id": "per123xyz",
    "seasonId": "xyz789abc",
    "seasonName": "High Season",
    "startDate": "06-15",
    "endDate": "09-15",
    "year": 2024,
    "notes": "Extended summer season",
    "isActive": true,
    "isRecurring": false,
    "isYearWrapping": false,
    "durationDays": 92,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T13:00:00"
  },
  "timestamp": "2024-01-15T13:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Season period not found
- **400 Bad Request** - Invalid date range
- **400 Bad Request** - Overlapping period exists

**Notes:**
- Cannot change the associated season
- Overlapping validation excludes the current period being updated

---

### 3. Get Season Period by ID

Retrieves a single season period by its ID.

**Endpoint:** `GET /api/season-periods/{id}`

**Permission Required:** `PERM_READ_SEASON_PERIOD`

**Path Parameters:**
- `id` (string, required): Obfuscated season period ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Season period retrieved successfully",
  "data": {
    "id": "per123xyz",
    "seasonId": "xyz789abc",
    "seasonName": "High Season",
    "startDate": "06-01",
    "endDate": "08-31",
    "year": 2024,
    "notes": "Summer season period",
    "isActive": true,
    "isRecurring": false,
    "isYearWrapping": false,
    "durationDays": 91,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Season period not found

---

### 4. Get All Season Periods

Retrieves all season periods with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/season-periods`

**Permission Required:** `PERM_READ_SEASON_PERIOD`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `seasonId` | string | No | - | Filter by season ID (obfuscated) |
| `isActive` | boolean | No | - | Filter by active status |
| `year` | integer | No | - | Filter by specific year |
| `isRecurring` | boolean | No | - | Filter recurring periods (year is null) |
| `notes` | string | No | - | Filter by notes (partial match, case-insensitive) |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 10 | Page size |
| `sortDirection` | string | No | asc | Sort direction (asc or desc) by startDate |

**Example Requests:**

1. Get all periods for a specific season:
   ```
   GET /api/season-periods?seasonId=xyz789abc
   ```

2. Get all recurring periods (annual):
   ```
   GET /api/season-periods?isRecurring=true
   ```

3. Get periods for a specific year:
   ```
   GET /api/season-periods?year=2024&isActive=true
   ```

4. Get active periods for a season:
   ```
   GET /api/season-periods?seasonId=xyz789abc&isActive=true
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Season periods retrieved successfully",
  "data": {
    "seasonPeriods": [
      {
        "id": "per123xyz",
        "seasonId": "xyz789abc",
        "seasonName": "High Season",
        "startDate": "06-01",
        "endDate": "08-31",
        "year": 2024,
        "notes": "Summer season period",
        "isActive": true,
        "isRecurring": false,
        "isYearWrapping": false,
        "durationDays": 91,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2
  },
  "timestamp": "2024-01-15T14:30:00"
}
```

**Notes:**
- All filters are optional and can be combined
- Results are sorted by `startDate` (earliest to latest by default)
- Use `isRecurring=true` to get only recurring annual periods
- Use `isRecurring=false` to get only year-specific periods

---

### 5. Delete Season Periods

Deletes multiple season periods by their IDs.

**Endpoint:** `DELETE /api/season-periods`

**Permission Required:** `PERM_DELETE_SEASON_PERIOD`

**Request Body:**
```json
["per123xyz", "per456abc", "per789def"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deleted 3 season periods successfully",
  "data": null,
  "timestamp": "2024-01-15T15:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Empty ID list
- **500 Internal Server Error** - Deletion failed

**Notes:**
- Multiple periods can be deleted in a single request
- The response indicates how many periods were successfully deleted

---

## Data Models

### Season Types

Available season types:

| Type | Display Name | Description |
|------|--------------|-------------|
| `HIGH_SEASON` | High Season | High demand period with premium pricing |
| `LOW_SEASON` | Low Season | Off-peak period with reduced pricing |
| `PEAK_SEASON` | Peak Season | Highest demand period with maximum pricing |
| `SHOULDER_SEASON` | Shoulder Season | Moderate demand period between peak and low |
| `FESTIVE_SEASON` | Festive Season | Special holiday and festive periods |
| `SPECIAL_EVENT` | Special Event | Specific events or occasions |
| `STANDARD` | Standard | Regular year-round pricing |
| `CUSTOM` | Custom | Custom defined season |

### Date Format

Season periods use `MonthDay` format for year-agnostic date ranges:

- **Format:** `MM-DD` (e.g., "06-01" for June 1st, "12-25" for December 25th)
- **Month:** 01-12
- **Day:** 01-31 (depending on month)

### Recurring Periods

Periods can be **recurring** (annual) or **year-specific**:

- **Recurring Period:** `year` field is `null`, applies every year
  - Example: "06-01" to "08-31" (June 1 - August 31 every year)

- **Year-Specific Period:** `year` field has a value, applies only to that year
  - Example: "06-01" to "08-31", year 2024 (only for 2024)

### Year-Wrapping Periods

Periods can wrap across year boundaries:

- Example: "12-15" to "01-15" (December 15 - January 15)
- `isYearWrapping` field is automatically set to `true`
- `durationDays` is calculated correctly across year boundaries

---

## Error Codes

### Season Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_SEASON_ID` | The provided season ID is invalid or malformed |
| `SEASON_NOT_FOUND` | Season with the specified ID does not exist |
| `MISSING_SEASON_NAME` | Season name is required but not provided |
| `DUPLICATE_SEASON_NAME` | Season name already exists for this accommodation |
| `DUPLICATE_GLOBAL_SEASON_NAME` | Global season name already exists |
| `ACCOMMODATION_NOT_FOUND` | Accommodation with the specified ID does not exist |
| `INVALID_ACCOMMODATION_ID` | The provided accommodation ID is invalid |
| `SEASON_CREATE_FAILED` | Failed to create season due to server error |
| `SEASON_UPDATE_FAILED` | Failed to update season due to server error |
| `SEASON_FETCH_FAILED` | Failed to fetch season(s) due to server error |
| `SEASON_DELETE_FAILED` | Failed to delete season(s) due to server error |

### Season Period Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_SEASON_PERIOD_ID` | The provided season period ID is invalid |
| `SEASON_PERIOD_NOT_FOUND` | Season period with the specified ID does not exist |
| `MISSING_SEASON_ID` | Season ID is required but not provided |
| `INVALID_DATE_RANGE` | Start date must be before end date |
| `OVERLAPPING_PERIOD` | Another period already exists for this date range |
| `SEASON_PERIOD_CREATE_FAILED` | Failed to create season period due to server error |
| `SEASON_PERIOD_UPDATE_FAILED` | Failed to update season period due to server error |
| `SEASON_PERIOD_FETCH_FAILED` | Failed to fetch season period(s) due to server error |
| `SEASON_PERIODS_FETCH_FAILED` | Failed to fetch season periods due to server error |
| `SEASON_PERIOD_DELETE_FAILED` | Failed to delete season period(s) due to server error |

---

## Examples

### Example 1: Creating a Global Season

Create a global high season that applies to all parks:

**Request:**
```http
POST /api/seasons
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Global High Season",
  "seasonType": "HIGH_SEASON",
  "description": "Peak tourism season for all parks",
  "isActive": true,
  "seasonPeriods": [
    {
      "startDate": "06-01",
      "endDate": "08-31",
      "year": null,
      "notes": "Recurring annual high season",
      "isActive": true
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Season created successfully",
  "data": {
    "id": "glb123xyz",
    "name": "Global High Season",
    "seasonType": "HIGH_SEASON",
    "description": "Peak tourism season for all parks",
    "isGlobal": true,
    "isActive": true,
    "accommodationId": null,
    "accommodationName": null,
    "seasonPeriods": [
      {
        "id": "per456abc",
        "seasonId": "glb123xyz",
        "seasonName": "Global High Season",
        "startDate": "06-01",
        "endDate": "08-31",
        "year": null,
        "notes": "Recurring annual high season",
        "isActive": true,
        "isRecurring": true,
        "isYearWrapping": false,
        "durationDays": 91,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Example 2: Creating an Accommodation-Specific Season

Create a season for a specific accommodation:

**Request:**
```http
POST /api/seasons
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Lodge Peak Season",
  "seasonType": "PEAK_SEASON",
  "description": "Peak season for Safari Lodge",
  "accommodationId": "acc789xyz",
  "isActive": true
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Season created successfully",
  "data": {
    "id": "sea123abc",
    "name": "Lodge Peak Season",
    "seasonType": "PEAK_SEASON",
    "description": "Peak season for Safari Lodge",
    "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
    "isActive": true,
    "accommodationId": "acc789xyz",
    "accommodationName": "Safari Lodge",
    "seasonPeriods": [],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Example 3: Adding a Year-Wrapping Period

Create a season period that wraps across the year boundary:

**Request:**
```http
POST /api/season-periods
Authorization: Bearer <token>
Content-Type: application/json

{
  "seasonId": "glb123xyz",
  "startDate": "12-15",
  "endDate": "01-15",
  "year": null,
  "notes": "Holiday season spanning year end",
  "isActive": true
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Season period created successfully",
  "data": {
    "id": "per789xyz",
    "seasonId": "glb123xyz",
    "seasonName": "Global High Season",
    "startDate": "12-15",
    "endDate": "01-15",
    "year": null,
    "notes": "Holiday season spanning year end",
    "isActive": true,
    "isRecurring": true,
    "isYearWrapping": true,
    "durationDays": 31,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Example 4: Filtering Seasons

Get all active high seasons for accommodation:

**Request:**
```http
GET /api/seasons?seasonType=HIGH_SEASON&isActive=true&isGlobal=false&page=0&size=20
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Seasons retrieved successfully",
  "data": {
    "seasons": [
      {
        "id": "sea123abc",
        "name": "Lodge High Season",
        "seasonType": "HIGH_SEASON",
        "isGlobal": false,
    "isSystem": false,
    "isSystem": false,
        "isActive": true,
        "accommodationId": "acc789xyz",
        "accommodationName": "Safari Lodge",
        "seasonPeriods": [...]
      }
    ],
    "currentPage": 0,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

### Example 5: Complex Period Filtering

Get all recurring active periods for 2024:

**Request:**
```http
GET /api/season-periods?isRecurring=false&year=2024&isActive=true&sortDirection=asc
Authorization: Bearer <token>
```

---

## Best Practices

### 1. Season Management

- **Global vs Accommodation-Specific:**
  - Use global seasons for park-wide pricing
  - Use accommodation-specific seasons for individual property pricing
  - Global seasons cannot be deleted (they are protected)

### 2. Period Management

- **Recurring Periods:**
  - Use `year: null` for annually recurring periods (e.g., summer season every year)
  - Use specific year for one-time or year-specific periods

- **Date Ranges:**
  - Ensure start date is before end date
  - For year-wrapping periods (e.g., Dec-Jan), the system handles the logic automatically
  - Avoid overlapping periods for the same season and year

### 3. Filtering

- **Combine Filters:**
  - Combine multiple filters for precise results
  - Example: `?isGlobal=true&isActive=true&seasonType=HIGH_SEASON`

- **Use Pagination:**
  - Always use pagination for listing endpoints
  - Default page size is 10, adjust as needed

### 4. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Display `message` field to users for user-friendly errors

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Complete CRUD operations for seasons and season periods
- Global and accommodation-specific season support
- Specification-based filtering
- Year-wrapping period support

---

## Support

For technical support or questions about the Season Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
