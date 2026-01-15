# Itinerary Management API Documentation

## Overview

The Itinerary Management API provides comprehensive endpoints for managing safari itineraries in the Kabengo Safaris platform. Itineraries are skeleton/templates that define the structure of safari trips, including days, activities, parks, accommodations, and passenger categories.

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Itinerary API](#itinerary-api)
   - [Create Itinerary](#1-create-itinerary)
   - [Update Itinerary](#2-update-itinerary)
   - [Get Itinerary by ID](#3-get-itinerary-by-id)
   - [Get Itinerary by Code](#4-get-itinerary-by-code)
   - [Get All Itineraries](#5-get-all-itineraries)
   - [Delete Itineraries](#6-delete-itineraries)

2. [Status Management API](#status-management-api)
   - [Publish Itinerary](#1-publish-itinerary)
   - [Unpublish Itinerary](#2-unpublish-itinerary)
   - [Archive Itinerary](#3-archive-itinerary)
   - [Unarchive Itinerary](#4-unarchive-itinerary)

3. [Data Models](#data-models)
4. [Error Codes](#error-codes)
5. [Examples](#examples)

---

## Itinerary API

Base URL: `/api/itineraries`

### 1. Create Itinerary

Creates a new safari itinerary.

**Endpoint:** `POST /api/itineraries`

**Permission Required:** `PERM_CREATE_ITINERARY`

**Request Body:**
```json
{
  "name": "7-Day Serengeti & Ngorongoro Safari",
  "tripType": "PRIVATE",
  "budgetCategory": "LUXURY",
  "totalDays": 7,
  "totalNights": 6,
  "carCount": 1,
  "description": "An unforgettable journey through Tanzania's most iconic wildlife destinations",
  "highlights": "Big Five viewing, Great Migration, Ngorongoro Crater",
  "startLocation": "Arusha",
  "endLocation": "Arusha"
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Itinerary name (3-200 characters) |
| `tripType` | enum | No | Trip type classification |
| `budgetCategory` | enum | No | Budget category classification |
| `totalDays` | integer | Yes | Total number of days (min: 1) |
| `totalNights` | integer | No | Total nights (defaults to totalDays - 1) |
| `carCount` | integer | No | Number of vehicles (min: 1, default: 1) |
| `description` | string | No | Detailed description |
| `highlights` | string | No | Key highlights |
| `startLocation` | string | No | Starting location |
| `endLocation` | string | No | Ending location |

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Itinerary created successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti & Ngorongoro Safari",
    "code": "7D6N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "tripType": "PRIVATE",
    "tripTypeDisplayName": "Private",
    "tripTypeDescription": "Exclusive private tour for a single group",
    "budgetCategory": "LUXURY",
    "budgetCategoryDisplayName": "Luxury",
    "budgetCategoryDescription": "High-end safari with luxury lodges and top-tier services",
    "budgetCategoryTier": 4,
    "totalDays": 7,
    "totalNights": 6,
    "isDayTrip": false,
    "carCount": 1,
    "description": "An unforgettable journey through Tanzania's most iconic wildlife destinations",
    "highlights": "Big Five viewing, Great Migration, Ngorongoro Crater",
    "startLocation": "Arusha",
    "endLocation": "Arusha",
    "isActive": true,
    "totalPaxCount": 0,
    "totalDaysCount": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Validation errors
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary name is required",
    "errorCode": "VALIDATION_ERROR",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Duplicate name
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary with name '7-Day Serengeti Safari' already exists",
    "errorCode": "ITINERARY_NAME_EXISTS",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

**Notes:**
- Itinerary starts in `DRAFT` status
- A unique code is auto-generated based on days/nights (e.g., "7D6N-001")
- If `totalNights` is not provided, it defaults to `totalDays - 1`
- Name must be unique (case-insensitive)

---

### 2. Update Itinerary

Updates an existing itinerary.

**Endpoint:** `PUT /api/itineraries/{id}`

**Permission Required:** `PERM_UPDATE_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
{
  "name": "8-Day Serengeti & Ngorongoro Safari",
  "tripType": "FAMILY",
  "budgetCategory": "MID_RANGE",
  "totalDays": 8,
  "totalNights": 7,
  "carCount": 2,
  "description": "Updated description",
  "highlights": "Updated highlights",
  "startLocation": "Kilimanjaro Airport",
  "endLocation": "Arusha",
  "isActive": true
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Itinerary name (3-200 characters) |
| `tripType` | enum | No | Trip type classification |
| `budgetCategory` | enum | No | Budget category classification |
| `totalDays` | integer | No | Total number of days (min: 1) |
| `totalNights` | integer | No | Total nights (min: 0) |
| `carCount` | integer | No | Number of vehicles (min: 1) |
| `description` | string | No | Detailed description |
| `highlights` | string | No | Key highlights |
| `startLocation` | string | No | Starting location |
| `endLocation` | string | No | Ending location |
| `isActive` | boolean | No | Active status |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary updated successfully",
  "data": {
    "id": "xyz789abc",
    "name": "8-Day Serengeti & Ngorongoro Safari",
    "code": "8D7N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "tripType": "FAMILY",
    "tripTypeDisplayName": "Family",
    "tripTypeDescription": "Family-friendly tour with activities for all ages",
    "budgetCategory": "MID_RANGE",
    "budgetCategoryDisplayName": "Mid-Range",
    "budgetCategoryDescription": "Comfortable mid-range safari with quality accommodations",
    "budgetCategoryTier": 3,
    "totalDays": 8,
    "totalNights": 7,
    "isDayTrip": false,
    "carCount": 2,
    "description": "Updated description",
    "highlights": "Updated highlights",
    "startLocation": "Kilimanjaro Airport",
    "endLocation": "Arusha",
    "isActive": true,
    "totalPaxCount": 0,
    "totalDaysCount": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  },
  "timestamp": "2024-01-15T11:45:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Itinerary not found
- **400 Bad Request** - Duplicate name

**Notes:**
- Only provided fields are updated (partial update)
- If `totalDays` or `totalNights` changes, the code is regenerated
- Name must remain unique if changed

---

### 3. Get Itinerary by ID

Retrieves a single itinerary by its ID.

**Endpoint:** `GET /api/itineraries/{id}`

**Permission Required:** `PERM_READ_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary retrieved successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "tripType": "PRIVATE",
    "tripTypeDisplayName": "Private",
    "tripTypeDescription": "Exclusive private tour for a single group",
    "budgetCategory": "LUXURY",
    "budgetCategoryDisplayName": "Luxury",
    "budgetCategoryDescription": "High-end safari with luxury lodges and top-tier services",
    "budgetCategoryTier": 4,
    "totalDays": 7,
    "totalNights": 6,
    "isDayTrip": false,
    "carCount": 1,
    "description": "Safari description",
    "highlights": "Key highlights",
    "startLocation": "Arusha",
    "endLocation": "Arusha",
    "isActive": true,
    "totalPaxCount": 4,
    "totalDaysCount": 7,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid itinerary ID",
    "errorCode": "INVALID_ITINERARY_ID",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

- **404 Not Found** - Itinerary not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Itinerary not found",
    "errorCode": "ITINERARY_NOT_FOUND",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

---

### 4. Get Itinerary by Code

Retrieves a single itinerary by its unique code.

**Endpoint:** `GET /api/itineraries/code/{code}`

**Permission Required:** `PERM_READ_ITINERARY`

**Path Parameters:**
- `code` (string, required): Itinerary code (e.g., "7D6N-001")

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary retrieved successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    ...
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **404 Not Found** - Itinerary not found

**Notes:**
- Code lookup is case-insensitive

---

### 5. Get All Itineraries

Retrieves all itineraries with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/itineraries`

**Permission Required:** `PERM_READ_ITINERARY`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `name` | string | No | - | Filter by name (partial match, case-insensitive) |
| `code` | string | No | - | Filter by code (partial match, case-insensitive) |
| `status` | enum | No | - | Filter by status (DRAFT, COMPLETE, PUBLISHED, ARCHIVED) |
| `tripType` | enum | No | - | Filter by trip type |
| `budgetCategory` | enum | No | - | Filter by budget category |
| `startLocation` | string | No | - | Filter by start location (partial match) |
| `endLocation` | string | No | - | Filter by end location (partial match) |
| `totalDays` | integer | No | - | Filter by exact total days |
| `isActive` | boolean | No | - | Filter by active status |
| `isDayTrip` | boolean | No | - | Filter day trips (totalDays=1, totalNights=0) |
| `keyword` | string | No | - | Search keyword across multiple fields |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 10 | Page size |
| `sortDirection` | string | No | desc | Sort direction (asc or desc) by createdAt |

**Example Requests:**

1. Get all published itineraries:
   ```
   GET /api/itineraries?status=PUBLISHED
   ```

2. Get luxury private safaris:
   ```
   GET /api/itineraries?tripType=PRIVATE&budgetCategory=LUXURY
   ```

3. Get day trips only:
   ```
   GET /api/itineraries?isDayTrip=true
   ```

4. Search by keyword:
   ```
   GET /api/itineraries?keyword=serengeti&page=0&size=20
   ```

5. Get 7-day safaris:
   ```
   GET /api/itineraries?totalDays=7&isActive=true
   ```

6. Get itineraries starting from Arusha:
   ```
   GET /api/itineraries?startLocation=Arusha
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itineraries retrieved successfully",
  "data": {
    "itineraries": [
      {
        "id": "xyz789abc",
        "name": "7-Day Serengeti Safari",
        "code": "7D6N-001",
        "status": "DRAFT",
        "statusDisplayName": "Draft",
        "tripType": "PRIVATE",
        "tripTypeDisplayName": "Private",
        "tripTypeDescription": "Exclusive private tour for a single group",
        "budgetCategory": "LUXURY",
        "budgetCategoryDisplayName": "Luxury",
        "budgetCategoryDescription": "High-end safari with luxury lodges and top-tier services",
        "budgetCategoryTier": 4,
        "totalDays": 7,
        "totalNights": 6,
        "isDayTrip": false,
        "carCount": 1,
        "description": "Safari description",
        "highlights": "Key highlights",
        "startLocation": "Arusha",
        "endLocation": "Arusha",
        "isActive": true,
        "totalPaxCount": 4,
        "totalDaysCount": 7,
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
- Use pagination for large result sets

---

### 6. Delete Itineraries

Deletes multiple itineraries by their IDs.

**Endpoint:** `DELETE /api/itineraries`

**Permission Required:** `PERM_DELETE_ITINERARY`

**Request Body:**
```json
["xyz789abc", "def456ghi", "jkl123mno"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deleted 3 itineraries successfully",
  "data": null,
  "timestamp": "2024-01-15T12:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Empty ID list
- **500 Internal Server Error** - Deletion failed

**Notes:**
- Multiple itineraries can be deleted in a single request
- Associated data (days, pax, parks, accommodations) are automatically deleted (cascade)

---

## Status Management API

### Itinerary Status Workflow

```
DRAFT ──────► COMPLETE ──────► PUBLISHED
  ▲              │                 │
  │              │                 │
  └──────────────┴─────────────────┘
                 │
                 ▼
             ARCHIVED
                 │
                 ▼
         DRAFT or COMPLETE
```

**Status Definitions:**

| Status | Description | Can Publish | Can Edit |
|--------|-------------|-------------|----------|
| `DRAFT` | Itinerary is being created/edited | No | Yes |
| `COMPLETE` | All required data is filled | Yes | Yes |
| `PUBLISHED` | Available for booking | N/A | Limited |
| `ARCHIVED` | No longer in use | No | No |

---

### 1. Publish Itinerary

Publishes an itinerary to make it available for booking.

**Endpoint:** `POST /api/itineraries/{id}/publish`

**Permission Required:** `PERM_PUBLISH_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary published successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "PUBLISHED",
    "statusDisplayName": "Published",
    ...
  },
  "timestamp": "2024-01-15T13:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Cannot publish archived itinerary
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Cannot publish an archived itinerary",
    "errorCode": "INVALID_STATUS_TRANSITION",
    "timestamp": "2024-01-15T13:00:00"
  }
  ```

- **400 Bad Request** - Already published
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary is already published",
    "errorCode": "ALREADY_PUBLISHED",
    "timestamp": "2024-01-15T13:00:00"
  }
  ```

- **400 Bad Request** - Incomplete itinerary
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary does not meet publishing requirements. Ensure it has all days defined and at least one passenger category.",
    "errorCode": "INCOMPLETE_ITINERARY",
    "timestamp": "2024-01-15T13:00:00"
  }
  ```

**Publishing Requirements:**
- Itinerary must have all days defined (`days.size() == totalDays`)
- Itinerary must have at least one passenger category
- Each day must have a title
- Each overnight day should have at least one accommodation

---

### 2. Unpublish Itinerary

Reverts a published itinerary back to COMPLETE or DRAFT status.

**Endpoint:** `POST /api/itineraries/{id}/unpublish`

**Permission Required:** `PERM_UNPUBLISH_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary unpublished successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "COMPLETE",
    "statusDisplayName": "Complete",
    ...
  },
  "timestamp": "2024-01-15T13:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Not published
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary is not published",
    "errorCode": "NOT_PUBLISHED",
    "timestamp": "2024-01-15T13:30:00"
  }
  ```

**Notes:**
- Reverts to `COMPLETE` if still meets publishing requirements
- Reverts to `DRAFT` if no longer meets requirements

---

### 3. Archive Itinerary

Archives an itinerary to mark it as no longer in use.

**Endpoint:** `POST /api/itineraries/{id}/archive`

**Permission Required:** `PERM_ARCHIVE_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary archived successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "ARCHIVED",
    "statusDisplayName": "Archived",
    "isActive": false,
    ...
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Already archived
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary is already archived",
    "errorCode": "ALREADY_ARCHIVED",
    "timestamp": "2024-01-15T14:00:00"
  }
  ```

**Notes:**
- Sets `isActive` to `false`
- Archived itineraries cannot be published

---

### 4. Unarchive Itinerary

Restores an archived itinerary.

**Endpoint:** `POST /api/itineraries/{id}/unarchive`

**Permission Required:** `PERM_UNARCHIVE_ITINERARY`

**Path Parameters:**
- `id` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary unarchived successfully",
  "data": {
    "id": "xyz789abc",
    "name": "7-Day Serengeti Safari",
    "code": "7D6N-001",
    "status": "COMPLETE",
    "statusDisplayName": "Complete",
    "isActive": true,
    ...
  },
  "timestamp": "2024-01-15T14:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Not archived
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary is not archived",
    "errorCode": "NOT_ARCHIVED",
    "timestamp": "2024-01-15T14:30:00"
  }
  ```

**Notes:**
- Sets `isActive` to `true`
- Restores to `COMPLETE` if meets publishing requirements, otherwise `DRAFT`

---

## Data Models

### Itinerary Status

| Status | Display Name | Description |
|--------|--------------|-------------|
| `DRAFT` | Draft | Itinerary is being created/edited |
| `COMPLETE` | Complete | All required data is filled |
| `PUBLISHED` | Published | Itinerary is available for booking |
| `ARCHIVED` | Archived | Itinerary is no longer in use |

### Trip Type

| Type | Display Name | Description |
|------|--------------|-------------|
| `PRIVATE` | Private | Exclusive private tour for a single group |
| `GROUP` | Group | Shared tour with other travelers |
| `CUSTOM` | Custom | Customized tour based on client requirements |
| `HONEYMOON` | Honeymoon | Romantic honeymoon package |
| `FAMILY` | Family | Family-friendly tour with activities for all ages |
| `PHOTOGRAPHY` | Photography | Specialized photography safari |
| `ADVENTURE` | Adventure | Active adventure-focused safari |

### Budget Category

| Category | Display Name | Description | Tier |
|----------|--------------|-------------|------|
| `ULTRA_LUXURY` | Ultra Luxury | Exclusive ultra-luxury experience with premium lodges and private services | 5 |
| `LUXURY` | Luxury | High-end safari with luxury lodges and top-tier services | 4 |
| `MID_RANGE` | Mid-Range | Comfortable mid-range safari with quality accommodations | 3 |
| `BUDGET` | Budget | Affordable safari experience with basic but clean accommodations | 2 |
| `BACKPACKER` | Backpacker | Budget-friendly option for backpackers and budget travelers | 1 |

### Itinerary Code Format

The itinerary code is auto-generated in the format: `{days}D{nights}N-{sequence}`

Examples:
- `7D6N-001` - 7 days, 6 nights, sequence 1
- `3D2N-015` - 3 days, 2 nights, sequence 15
- `1D0N-003` - 1 day trip (no nights), sequence 3

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_ITINERARY_ID` | The provided itinerary ID is invalid or malformed |
| `ITINERARY_NOT_FOUND` | Itinerary with the specified ID does not exist |
| `ITINERARY_NAME_EXISTS` | Itinerary name already exists |
| `INVALID_STATUS_TRANSITION` | Cannot transition to the requested status |
| `ALREADY_PUBLISHED` | Itinerary is already published |
| `NOT_PUBLISHED` | Itinerary is not in published status |
| `ALREADY_ARCHIVED` | Itinerary is already archived |
| `NOT_ARCHIVED` | Itinerary is not in archived status |
| `INCOMPLETE_ITINERARY` | Itinerary does not meet publishing requirements |
| `ITINERARY_CREATE_FAILED` | Failed to create itinerary due to server error |
| `ITINERARY_UPDATE_FAILED` | Failed to update itinerary due to server error |
| `ITINERARY_FETCH_FAILED` | Failed to fetch itinerary due to server error |
| `ITINERARIES_FETCH_FAILED` | Failed to fetch itineraries due to server error |
| `STATUS_EVAL_FAILED` | Failed to evaluate itinerary status |
| `PUBLISH_FAILED` | Failed to publish itinerary |
| `UNPUBLISH_FAILED` | Failed to unpublish itinerary |
| `ARCHIVE_FAILED` | Failed to archive itinerary |
| `UNARCHIVE_FAILED` | Failed to unarchive itinerary |

---

## Examples

### Example 1: Creating a Luxury Private Safari

**Request:**
```http
POST /api/itineraries
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Ultimate Serengeti Experience",
  "tripType": "PRIVATE",
  "budgetCategory": "ULTRA_LUXURY",
  "totalDays": 10,
  "totalNights": 9,
  "carCount": 2,
  "description": "The ultimate safari experience with exclusive private camps and personalized service",
  "highlights": "Private bush dinners, hot air balloon safari, exclusive game drives",
  "startLocation": "Kilimanjaro International Airport",
  "endLocation": "Arusha"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Itinerary created successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Ultimate Serengeti Experience",
    "code": "10D9N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "tripType": "PRIVATE",
    "tripTypeDisplayName": "Private",
    "tripTypeDescription": "Exclusive private tour for a single group",
    "budgetCategory": "ULTRA_LUXURY",
    "budgetCategoryDisplayName": "Ultra Luxury",
    "budgetCategoryDescription": "Exclusive ultra-luxury experience with premium lodges and private services",
    "budgetCategoryTier": 5,
    "totalDays": 10,
    "totalNights": 9,
    "isDayTrip": false,
    "carCount": 2,
    "description": "The ultimate safari experience with exclusive private camps and personalized service",
    "highlights": "Private bush dinners, hot air balloon safari, exclusive game drives",
    "startLocation": "Kilimanjaro International Airport",
    "endLocation": "Arusha",
    "isActive": true,
    "totalPaxCount": 0,
    "totalDaysCount": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Example 2: Creating a Day Trip

**Request:**
```http
POST /api/itineraries
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Arusha National Park Day Trip",
  "tripType": "GROUP",
  "budgetCategory": "BUDGET",
  "totalDays": 1,
  "totalNights": 0,
  "description": "A perfect introduction to Tanzania's wildlife",
  "startLocation": "Arusha",
  "endLocation": "Arusha"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Itinerary created successfully",
  "data": {
    "id": "day123xyz",
    "name": "Arusha National Park Day Trip",
    "code": "1D0N-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "tripType": "GROUP",
    "tripTypeDisplayName": "Group",
    "tripTypeDescription": "Shared tour with other travelers",
    "budgetCategory": "BUDGET",
    "budgetCategoryDisplayName": "Budget",
    "budgetCategoryDescription": "Affordable safari experience with basic but clean accommodations",
    "budgetCategoryTier": 2,
    "totalDays": 1,
    "totalNights": 0,
    "isDayTrip": true,
    "carCount": 1,
    "description": "A perfect introduction to Tanzania's wildlife",
    "highlights": null,
    "startLocation": "Arusha",
    "endLocation": "Arusha",
    "isActive": true,
    "totalPaxCount": 0,
    "totalDaysCount": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### Example 3: Publishing an Itinerary

**Request:**
```http
POST /api/itineraries/abc123xyz/publish
Authorization: Bearer <token>
```

**Response (Success):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary published successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Ultimate Serengeti Experience",
    "code": "10D9N-001",
    "status": "PUBLISHED",
    "statusDisplayName": "Published",
    ...
  }
}
```

### Example 4: Filtering Itineraries

Get all active, published luxury safaris:

**Request:**
```http
GET /api/itineraries?status=PUBLISHED&budgetCategory=LUXURY&isActive=true&page=0&size=20
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itineraries retrieved successfully",
  "data": {
    "itineraries": [
      {
        "id": "xyz789abc",
        "name": "7-Day Luxury Serengeti Safari",
        "code": "7D6N-001",
        "status": "PUBLISHED",
        "statusDisplayName": "Published",
        "tripType": "PRIVATE",
        "tripTypeDisplayName": "Private",
        "tripTypeDescription": "Exclusive private tour for a single group",
        "budgetCategory": "LUXURY",
        "budgetCategoryDisplayName": "Luxury",
        "budgetCategoryDescription": "High-end safari with luxury lodges and top-tier services",
        "budgetCategoryTier": 4,
        "totalDays": 7,
        "totalNights": 6,
        "isDayTrip": false,
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

---

## Best Practices

### 1. Itinerary Creation

- **Complete Data:** Provide all relevant information when creating
- **Meaningful Names:** Use descriptive names that include duration and key destinations
- **Consistent Locations:** Use consistent naming for locations (e.g., always "Arusha" not "Arusha Town")

### 2. Status Management

- **Build Complete:** Ensure all days, accommodations, and pax are added before publishing
- **Review Before Publish:** Verify all data is correct before publishing
- **Archive Instead of Delete:** Archive old itineraries instead of deleting for record keeping

### 3. Filtering

- **Combine Filters:** Use multiple filters for precise results
- **Use Pagination:** Always paginate for listing endpoints
- **Use Keywords:** Use keyword search for flexible searching

### 4. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Display `message` field to users for user-friendly errors

---

## Related APIs

- **Itinerary Day API:** `/api/itineraries/{itineraryId}/days`
- **Itinerary Pax API:** `/api/itineraries/{itineraryId}/pax`
- **Itinerary Day Park API:** `/api/itineraries/{itineraryId}/days/{dayId}/parks`
- **Itinerary Day Accommodation API:** `/api/itineraries/{itineraryId}/days/{dayId}/accommodations`

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Complete CRUD operations for itineraries
- Status management endpoints (publish, unpublish, archive, unarchive)
- Trip type and budget category classification
- Day trip filtering support
- Specification-based filtering

---

## Support

For technical support or questions about the Itinerary Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
