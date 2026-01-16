# Itinerary Day Activity Management API Documentation

## Overview

The Itinerary Day Activity Management API provides endpoints for managing activities within individual safari itinerary days. Activities represent standalone experiences that are not tied to a specific park, such as city tours, cultural visits, airport transfers, and more.

Key features:
- **Auto-ordering**: Sort order is automatically assigned based on creation order
- **Reorder support**: Activities can be reordered via drag-and-drop UI operations
- **Optional activities**: Activities can be marked as optional add-ons
- **Pricing flexibility**: Activities can be included or excluded from the itinerary price

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Itinerary Day Activity API](#itinerary-day-activity-api)
   - [Create Activity](#1-create-activity)
   - [Get All Activities](#2-get-all-activities)
   - [Get Activity by ID](#3-get-activity-by-id)
   - [Update Activity](#4-update-activity)
   - [Delete Activities](#5-delete-activities)
   - [Reorder Activities](#6-reorder-activities)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)

---

## Itinerary Day Activity API

Base URL: `/api/itineraries/{itineraryId}/days/{dayId}/activities`

### 1. Create Activity

Creates a new activity for an itinerary day. The sort order is automatically determined based on existing activities (first activity = 1, subsequent activities increment).

**Endpoint:** `POST /api/itineraries/{itineraryId}/days/{dayId}/activities`

**Permission Required:** `PERM_CREATE_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
{
  "activityId": "act123xyz",
  "durationHours": 2.5,
  "startTime": "09:00",
  "endTime": "11:30",
  "notes": "Includes traditional coffee ceremony",
  "isIncludedInPrice": true,
  "isOptional": false
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | string | Yes | Obfuscated base activity ID |
| `durationHours` | decimal | No | Duration in hours (e.g., 2.5) |
| `startTime` | string | No | Start time (e.g., "09:00") |
| `endTime` | string | No | End time (e.g., "11:30") |
| `notes` | string | No | Additional notes about this activity |
| `isIncludedInPrice` | boolean | No | Whether included in itinerary price (default: true) |
| `isOptional` | boolean | No | Whether this is an optional add-on (default: false) |

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Itinerary day activity created successfully",
  "data": {
    "id": "ida123xyz",
    "itineraryDayId": "day456abc",
    "activityId": "act123xyz",
    "activityName": "Coffee Plantation Tour",
    "activitySlug": "coffee-plantation-tour",
    "sortOrder": 1,
    "durationHours": 2.5,
    "startTime": "09:00",
    "endTime": "11:30",
    "notes": "Includes traditional coffee ceremony",
    "isIncludedInPrice": true,
    "isOptional": false,
    "createdAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid ID format",
    "errorCode": "INVALID_ID",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Day does not belong to itinerary
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day does not belong to this itinerary",
    "errorCode": "DAY_ITINERARY_MISMATCH",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Activity already exists for this day
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity already exists for this day",
    "errorCode": "ACTIVITY_ALREADY_EXISTS",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Itinerary day not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Itinerary day not found",
    "errorCode": "ITINERARY_DAY_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Activity not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Activity not found",
    "errorCode": "ACTIVITY_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

**Notes:**
- `sortOrder` is auto-assigned (not provided in request)
- The same base activity cannot be added twice to the same day
- `activityId` refers to the base Activity entity, not the ItineraryDayActivity

---

### 2. Get All Activities

Retrieves all activities for an itinerary day, ordered by sort order.

**Endpoint:** `GET /api/itineraries/{itineraryId}/days/{dayId}/activities`

**Permission Required:** `PERM_READ_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day activities retrieved successfully",
  "data": [
    {
      "id": "ida123xyz",
      "itineraryDayId": "day456abc",
      "activityId": "act123xyz",
      "activityName": "Airport Transfer",
      "activitySlug": "airport-transfer",
      "sortOrder": 1,
      "durationHours": 1.0,
      "startTime": "08:00",
      "endTime": "09:00",
      "notes": "Pickup from Kilimanjaro International Airport",
      "isIncludedInPrice": true,
      "isOptional": false,
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": "ida124xyz",
      "itineraryDayId": "day456abc",
      "activityId": "act456def",
      "activityName": "Coffee Plantation Tour",
      "activitySlug": "coffee-plantation-tour",
      "sortOrder": 2,
      "durationHours": 2.5,
      "startTime": "10:00",
      "endTime": "12:30",
      "notes": "Includes traditional coffee ceremony",
      "isIncludedInPrice": true,
      "isOptional": false,
      "createdAt": "2024-01-15T10:45:00"
    },
    {
      "id": "ida125xyz",
      "itineraryDayId": "day456abc",
      "activityId": "act789ghi",
      "activityName": "Hot Air Balloon Safari",
      "activitySlug": "hot-air-balloon-safari",
      "sortOrder": 3,
      "durationHours": 3.0,
      "startTime": "05:30",
      "endTime": "08:30",
      "notes": "Optional sunrise balloon experience",
      "isIncludedInPrice": false,
      "isOptional": true,
      "createdAt": "2024-01-15T11:00:00"
    }
  ],
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Day does not belong to itinerary
- **404 Not Found** - Itinerary day not found

---

### 3. Get Activity by ID

Retrieves a specific activity by its ID.

**Endpoint:** `GET /api/itineraries/{itineraryId}/days/{dayId}/activities/{activityId}`

**Permission Required:** `PERM_READ_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID
- `activityId` (string, required): Obfuscated itinerary day activity ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day activity retrieved successfully",
  "data": {
    "id": "ida123xyz",
    "itineraryDayId": "day456abc",
    "activityId": "act123xyz",
    "activityName": "Coffee Plantation Tour",
    "activitySlug": "coffee-plantation-tour",
    "sortOrder": 1,
    "durationHours": 2.5,
    "startTime": "09:00",
    "endTime": "11:30",
    "notes": "Includes traditional coffee ceremony",
    "isIncludedInPrice": true,
    "isOptional": false,
    "createdAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Activity does not belong to this day
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity does not belong to this day",
    "errorCode": "ACTIVITY_DAY_MISMATCH",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```
- **400 Bad Request** - Day does not belong to itinerary
- **404 Not Found** - Itinerary day activity not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Itinerary day activity not found",
    "errorCode": "ITINERARY_DAY_ACTIVITY_NOT_FOUND",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

---

### 4. Update Activity

Updates an existing activity. Only provided fields are updated (partial update).

**Endpoint:** `PUT /api/itineraries/{itineraryId}/days/{dayId}/activities/{activityId}`

**Permission Required:** `PERM_UPDATE_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID
- `activityId` (string, required): Obfuscated itinerary day activity ID

**Request Body:**
```json
{
  "durationHours": 3.0,
  "startTime": "08:30",
  "endTime": "11:30",
  "notes": "Extended tour with lunch included",
  "isIncludedInPrice": true,
  "isOptional": false
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `durationHours` | decimal | No | Duration in hours |
| `startTime` | string | No | Start time |
| `endTime` | string | No | End time |
| `notes` | string | No | Additional notes |
| `isIncludedInPrice` | boolean | No | Whether included in price |
| `isOptional` | boolean | No | Whether optional |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day activity updated successfully",
  "data": {
    "id": "ida123xyz",
    "itineraryDayId": "day456abc",
    "activityId": "act123xyz",
    "activityName": "Coffee Plantation Tour",
    "activitySlug": "coffee-plantation-tour",
    "sortOrder": 1,
    "durationHours": 3.0,
    "startTime": "08:30",
    "endTime": "11:30",
    "notes": "Extended tour with lunch included",
    "isIncludedInPrice": true,
    "isOptional": false,
    "createdAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Activity does not belong to this day
- **400 Bad Request** - Day does not belong to itinerary
- **404 Not Found** - Itinerary day activity not found

**Notes:**
- `sortOrder` cannot be updated directly - use the **Reorder** endpoint
- `activityId` (base activity) cannot be changed after creation
- Only provided fields are updated; omitted fields remain unchanged

---

### 5. Delete Activities

Deletes multiple activities from an itinerary day in a single request.

**Endpoint:** `DELETE /api/itineraries/{itineraryId}/days/{dayId}/activities`

**Permission Required:** `PERM_DELETE_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
["ida123xyz", "ida124xyz", "ida125xyz"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 activity(ies) deleted successfully",
  "data": null,
  "timestamp": "2024-01-15T15:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format

**Notes:**
- Only activities belonging to the specified day are deleted
- Invalid or non-existent activity IDs are silently skipped
- Activities that don't belong to the day are skipped (logged as warning)
- The response indicates how many activities were actually deleted

---

### 6. Reorder Activities

Reorders itinerary day activities based on a new order provided. This endpoint is designed for drag-and-drop UI operations.

**Endpoint:** `POST /api/itineraries/{itineraryId}/days/{dayId}/activities/reorder`

**Permission Required:** `PERM_UPDATE_ITINERARY_DAY_ACTIVITY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
{
  "activityOrder": [
    { "activityId": "ida125xyz", "expectedSortOrder": 1 },
    { "activityId": "ida123xyz", "expectedSortOrder": 2 },
    { "activityId": "ida124xyz", "expectedSortOrder": 3 }
  ]
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityOrder` | array | Yes | List of activity order items |
| `activityOrder[].activityId` | string | Yes | Obfuscated itinerary day activity ID |
| `activityOrder[].expectedSortOrder` | integer | No | Expected new sort order (for validation) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities reordered successfully",
  "data": [
    {
      "id": "ida125xyz",
      "activityName": "Hot Air Balloon Safari",
      "sortOrder": 1
    },
    {
      "id": "ida123xyz",
      "activityName": "Airport Transfer",
      "sortOrder": 2
    },
    {
      "id": "ida124xyz",
      "activityName": "Coffee Plantation Tour",
      "sortOrder": 3
    }
  ],
  "timestamp": "2024-01-15T16:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Activity count mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity order list must contain exactly 5 activities. Received: 3",
    "errorCode": "ACTIVITY_COUNT_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Invalid activity ID format
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid activity ID format(s): abc123, xyz789",
    "errorCode": "INVALID_ACTIVITY_ID_FORMAT",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Duplicate activity IDs
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Duplicate activity ID(s) in reorder list: ida123xyz",
    "errorCode": "DUPLICATE_ACTIVITY_IDS",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Activity does not belong to day
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity ID(s) do not belong to this day: ida999xyz",
    "errorCode": "ACTIVITY_DAY_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Missing activities
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Missing activity ID(s) in reorder list: ida126xyz",
    "errorCode": "MISSING_ACTIVITY_IDS",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Expected order mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Expected sort order mismatches: Activity ida123xyz: expected 1, but position is 2",
    "errorCode": "EXPECTED_ORDER_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - No activities to reorder
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day has no activities to reorder",
    "errorCode": "NO_ACTIVITIES_TO_REORDER",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

**Notes:**
- The `activityOrder` list **must contain ALL activities** of the day
- Position in the list determines the new `sortOrder` (first item = 1)
- If the order is unchanged, returns success with "Activity order unchanged" message
- `expectedSortOrder` is optional and used for additional validation

---

## Data Models

### ItineraryDayActivityDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated itinerary day activity ID |
| `itineraryDayId` | string | Obfuscated itinerary day ID |
| `activityId` | string | Obfuscated base activity ID |
| `activityName` | string | Name of the base activity |
| `activitySlug` | string | URL-friendly slug of the activity |
| `sortOrder` | integer | Order of the activity within the day |
| `durationHours` | decimal | Duration in hours |
| `startTime` | string | Start time (e.g., "09:00") |
| `endTime` | string | End time (e.g., "11:30") |
| `notes` | string | Additional notes |
| `isIncludedInPrice` | boolean | Whether included in itinerary price |
| `isOptional` | boolean | Whether this is an optional add-on |
| `createdAt` | datetime | Creation timestamp |

### CreateItineraryDayActivityDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | string | Yes | Obfuscated base activity ID |
| `durationHours` | decimal | No | Duration in hours |
| `startTime` | string | No | Start time |
| `endTime` | string | No | End time |
| `notes` | string | No | Additional notes |
| `isIncludedInPrice` | boolean | No | Whether included in price (default: true) |
| `isOptional` | boolean | No | Whether optional (default: false) |

### UpdateItineraryDayActivityDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `durationHours` | decimal | No | Duration in hours |
| `startTime` | string | No | Start time |
| `endTime` | string | No | End time |
| `notes` | string | No | Additional notes |
| `isIncludedInPrice` | boolean | No | Whether included in price |
| `isOptional` | boolean | No | Whether optional |

**Note:** `sortOrder` and `activityId` are not included - they cannot be directly updated.

### ReorderItineraryDayActivitiesDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityOrder` | array | Yes | List of ActivityOrderItem objects |

### ActivityOrderItem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | string | Yes | Obfuscated itinerary day activity ID |
| `expectedSortOrder` | integer | No | Expected new sort order (for validation) |

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_ID` | One or more provided IDs are invalid or malformed |
| `ITINERARY_DAY_NOT_FOUND` | Day with the specified ID does not exist |
| `ITINERARY_DAY_ACTIVITY_NOT_FOUND` | Activity with the specified ID does not exist |
| `ACTIVITY_NOT_FOUND` | Base activity with the specified ID does not exist |
| `DAY_ITINERARY_MISMATCH` | Day does not belong to the specified itinerary |
| `ACTIVITY_DAY_MISMATCH` | Activity does not belong to the specified day |
| `ACTIVITY_ALREADY_EXISTS` | The same base activity already exists for this day |
| `ACTIVITY_COUNT_MISMATCH` | Reorder list does not contain all activities |
| `INVALID_ACTIVITY_ID_FORMAT` | One or more activity IDs have invalid format |
| `DUPLICATE_ACTIVITY_IDS` | Reorder list contains duplicate activity IDs |
| `MISSING_ACTIVITY_IDS` | Reorder list is missing some activities |
| `EXPECTED_ORDER_MISMATCH` | Expected sort order does not match position |
| `NO_ACTIVITIES_TO_REORDER` | Day has no activities to reorder |
| `ITINERARY_DAY_ACTIVITY_CREATE_FAILED` | Server error during activity creation |
| `ITINERARY_DAY_ACTIVITY_UPDATE_FAILED` | Server error during activity update |
| `ITINERARY_DAY_ACTIVITIES_DELETE_FAILED` | Server error during activity deletion |
| `ITINERARY_DAY_ACTIVITIES_FETCH_FAILED` | Server error during activities retrieval |
| `ITINERARY_DAY_ACTIVITIES_REORDER_FAILED` | Server error during reorder operation |

---

## Examples

### Example 1: Adding Activities to Day 1 (Arrival Day)

**Step 1: Add Airport Transfer**
```http
POST /api/itineraries/itn456abc/days/day001/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityId": "act_airport_transfer",
  "durationHours": 1.0,
  "startTime": "14:00",
  "endTime": "15:00",
  "notes": "Pickup from Kilimanjaro International Airport",
  "isIncludedInPrice": true,
  "isOptional": false
}
```

**Response:** Activity created with `sortOrder: 1`

**Step 2: Add City Tour**
```http
POST /api/itineraries/itn456abc/days/day001/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityId": "act_arusha_city_tour",
  "durationHours": 3.0,
  "startTime": "15:30",
  "endTime": "18:30",
  "notes": "Explore Arusha town and local market",
  "isIncludedInPrice": true,
  "isOptional": false
}
```

**Response:** Activity created with `sortOrder: 2`

**Step 3: Add Optional Cultural Visit**
```http
POST /api/itineraries/itn456abc/days/day001/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityId": "act_maasai_village",
  "durationHours": 2.0,
  "startTime": "16:00",
  "endTime": "18:00",
  "notes": "Visit traditional Maasai village",
  "isIncludedInPrice": false,
  "isOptional": true
}
```

**Response:** Activity created with `sortOrder: 3`

---

### Example 2: Reordering Activities

After review, the city tour should come before the optional Maasai visit:

**Request:**
```http
POST /api/itineraries/itn456abc/days/day001/activities/reorder
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityOrder": [
    { "activityId": "ida001", "expectedSortOrder": 1 },
    { "activityId": "ida003", "expectedSortOrder": 2 },
    { "activityId": "ida002", "expectedSortOrder": 3 }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities reordered successfully",
  "data": [
    {
      "id": "ida001",
      "activityName": "Airport Transfer",
      "sortOrder": 1
    },
    {
      "id": "ida003",
      "activityName": "Maasai Village Visit",
      "sortOrder": 2
    },
    {
      "id": "ida002",
      "activityName": "Arusha City Tour",
      "sortOrder": 3
    }
  ]
}
```

---

### Example 3: Updating Activity Details

**Request:**
```http
PUT /api/itineraries/itn456abc/days/day001/activities/ida002
Authorization: Bearer <token>
Content-Type: application/json

{
  "durationHours": 4.0,
  "startTime": "14:00",
  "endTime": "18:00",
  "notes": "Extended city tour including Arusha National Museum and local craft market"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day activity updated successfully",
  "data": {
    "id": "ida002",
    "activityName": "Arusha City Tour",
    "sortOrder": 3,
    "durationHours": 4.0,
    "startTime": "14:00",
    "endTime": "18:00",
    "notes": "Extended city tour including Arusha National Museum and local craft market",
    "isIncludedInPrice": true,
    "isOptional": false
  }
}
```

---

### Example 4: Bulk Delete Activities

**Request:**
```http
DELETE /api/itineraries/itn456abc/days/day001/activities
Authorization: Bearer <token>
Content-Type: application/json

["ida002", "ida003"]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 activity(ies) deleted successfully",
  "data": null
}
```

---

## Best Practices

### 1. Activity Management

- **Use Bulk Delete**: Always use the bulk endpoint for deleting activities
- **Validate Base Activity**: Ensure the base activity ID exists before creating
- **Unique Per Day**: The same base activity can only be added once per day

### 2. Optional Activities

- **Mark Clearly**: Use `isOptional: true` for add-on experiences
- **Price Exclusion**: Optional activities typically have `isIncludedInPrice: false`
- **Examples**: Hot air balloon safaris, spa treatments, cultural visits

### 3. Timing

- **Realistic Schedules**: Ensure start/end times don't overlap
- **Duration Accuracy**: Match `durationHours` with the time window
- **Time Format**: Use 24-hour format (e.g., "06:00", "18:30")

### 4. Reorder Operations

- **Include All Activities**: Reorder request must include every activity of the day
- **Use expectedSortOrder**: Optional but helpful for validation
- **Preserve Positions**: If no reorder needed, the API returns success without changes

### 5. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Invalid IDs in bulk operations are silently skipped

---

## Common Activity Types for Arusha Safaris

| Activity Type | Typical Duration | Usually Optional |
|---------------|------------------|------------------|
| Airport Transfer | 1-2 hours | No |
| City Tour | 2-4 hours | No |
| Coffee Plantation Tour | 2-3 hours | Sometimes |
| Maasai Village Visit | 2-3 hours | Yes |
| Hot Air Balloon Safari | 3-4 hours | Yes |
| Walking Safari | 2-4 hours | Sometimes |
| Cultural Experience | 1-3 hours | Yes |
| Hotel Transfer | 0.5-1 hour | No |
| Game Drive | 3-6 hours | No |
| Night Safari | 2-3 hours | Yes |

---

## Related APIs

- **Itinerary API:** `/api/itineraries` - Main itinerary management
- **Itinerary Day API:** `/api/itineraries/{itineraryId}/days` - Day management
- **Activity API:** `/api/activities` - Base activity management

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Create, read, update activities
- Bulk delete activities
- Reorder activities with comprehensive validation
- Optional activity support
- Price inclusion flags

---

## Support

For technical support or questions about the Itinerary Day Activity Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
