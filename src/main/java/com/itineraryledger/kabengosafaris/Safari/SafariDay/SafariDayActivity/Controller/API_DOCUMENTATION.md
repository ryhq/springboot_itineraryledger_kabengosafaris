# Safari Day Activity Management API Documentation

## Overview

The Safari Day Activity Management API provides endpoints for managing activities within individual safari days. Activities represent standalone experiences that are not tied to a specific park, such as city tours, cultural visits, airport transfers, and more.

Key features:
- **Safari state validation**: Activities can only be modified when safari is in an editable state
- **Operational tracking**: Track activity completion, actual times, feedback, and skipped activities
- **Auto-ordering**: Sort order is automatically assigned based on creation order
- **Reorder support**: Activities can be reordered via drag-and-drop UI operations
- **Optional activities**: Activities can be marked as optional add-ons
- **Pricing flexibility**: Activities can be included or excluded from the safari price

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Safari Day Activity API](#safari-day-activity-api)
   - [Create Activity](#1-create-activity)
   - [Get All Activities](#2-get-all-activities)
   - [Get Activity by ID](#3-get-activity-by-id)
   - [Update Activity](#4-update-activity)
   - [Delete Activities](#5-delete-activities)
   - [Reorder Activities](#6-reorder-activities)

2. [Data Models](#data-models)
3. [Business Rules](#business-rules)
4. [Error Codes](#error-codes)
5. [Examples](#examples)

---

## Safari Day Activity API

Base URL: `/api/safaris/{safariId}/days/{dayId}/activities`

### 1. Create Activity

Creates a new activity for a safari day. The sort order is automatically determined based on existing activities (first activity = 1, subsequent activities increment).

**Endpoint:** `POST /api/safaris/{safariId}/days/{dayId}/activities`

**Permission Required:** `PERM_CREATE_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
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
| `startTime` | string | No | Planned start time (e.g., "09:00") |
| `endTime` | string | No | Planned end time (e.g., "11:30") |
| `notes` | string | No | Additional notes about this activity |
| `isIncludedInPrice` | boolean | No | Whether included in safari price (default: true) |
| `isOptional` | boolean | No | Whether this is an optional add-on (default: false) |

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Safari day activity created successfully",
  "data": {
    "id": "sda123xyz",
    "safariDayId": "sday456abc",
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
    "isCompleted": false,
    "completedAt": null,
    "actualStartTime": null,
    "actualEndTime": null,
    "feedback": null,
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Activity is a park activity
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity 'Game Drive' is a park activity. Use park activities endpoint instead.",
    "errorCode": "ACTIVITY_IS_PARK_LINKED",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Activity not active
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Activity 'Old Tour' is not active",
    "errorCode": "ACTIVITY_NOT_ACTIVE",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Safari not found
- **404 Not Found** - Safari day not found
- **404 Not Found** - Activity not found

**Notes:**
- Safari must be in an editable state (not COMPLETED, CANCELLED, etc.)
- `sortOrder` is auto-assigned (not provided in request)
- Activity must be a standalone activity (not linked to any park)
- `activityId` refers to the base Activity entity, not the SafariDayActivity
- Safari-specific tracking fields (isCompleted, actualStartTime, etc.) are initialized to defaults

---

### 2. Get All Activities

Retrieves all activities for a safari day, ordered by sort order.

**Endpoint:** `GET /api/safaris/{safariId}/days/{dayId}/activities`

**Permission Required:** `PERM_READ_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day activities retrieved successfully",
  "data": [
    {
      "id": "sda123xyz",
      "safariDayId": "sday456abc",
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
      "isCompleted": true,
      "completedAt": "2024-01-15T09:05:00",
      "actualStartTime": "08:10",
      "actualEndTime": "09:05",
      "feedback": "Driver was punctual and professional",
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2024-01-10T10:30:00"
    },
    {
      "id": "sda124xyz",
      "safariDayId": "sday456abc",
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
      "isCompleted": false,
      "completedAt": null,
      "actualStartTime": null,
      "actualEndTime": null,
      "feedback": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2024-01-10T10:45:00"
    },
    {
      "id": "sda125xyz",
      "safariDayId": "sday456abc",
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
      "isCompleted": false,
      "completedAt": null,
      "actualStartTime": null,
      "actualEndTime": null,
      "feedback": null,
      "isSkipped": true,
      "skipReason": "Weather conditions not suitable",
      "createdAt": "2024-01-10T11:00:00"
    }
  ],
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Day does not belong to safari
- **404 Not Found** - Safari day not found

---

### 3. Get Activity by ID

Retrieves a specific activity by its ID.

**Endpoint:** `GET /api/safaris/{safariId}/days/{dayId}/activities/{activityId}`

**Permission Required:** `PERM_READ_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID
- `activityId` (string, required): Obfuscated safari day activity ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day activity retrieved successfully",
  "data": {
    "id": "sda123xyz",
    "safariDayId": "sday456abc",
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
    "isCompleted": true,
    "completedAt": "2024-01-15T11:35:00",
    "actualStartTime": "09:05",
    "actualEndTime": "11:35",
    "feedback": "Excellent tour guide, learned a lot about coffee production",
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2024-01-10T10:30:00"
  },
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Activity does not belong to this day
- **400 Bad Request** - Day does not belong to safari
- **404 Not Found** - Safari day activity not found

---

### 4. Update Activity

Updates an existing activity. Only provided fields are updated (partial update).

**Important:** This endpoint has two modes of operation:
1. **Planning Updates**: Modify activity details (requires safari to be editable)
2. **Operational Updates**: Track activity completion and actual times (allowed even when safari is not editable)

**Endpoint:** `PUT /api/safaris/{safariId}/days/{dayId}/activities/{activityId}`

**Permission Required:** `PERM_UPDATE_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID
- `activityId` (string, required): Obfuscated safari day activity ID

**Request Body (Planning Update):**
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

**Request Body (Operational Update - During Safari):**
```json
{
  "isCompleted": true,
  "actualStartTime": "09:05",
  "actualEndTime": "11:35",
  "feedback": "Excellent experience, guide was very knowledgeable"
}
```

**Request Body (Mark as Skipped):**
```json
{
  "isSkipped": true,
  "skipReason": "Guest feeling unwell"
}
```

**Request Body Fields:**

| Field | Type | Required | Update Type | Description |
|-------|------|----------|-------------|-------------|
| `durationHours` | decimal | No | Planning | Duration in hours |
| `startTime` | string | No | Planning | Planned start time |
| `endTime` | string | No | Planning | Planned end time |
| `notes` | string | No | Planning | Additional notes |
| `isIncludedInPrice` | boolean | No | Planning | Whether included in price |
| `isOptional` | boolean | No | Planning | Whether optional |
| `isCompleted` | boolean | No | Operational | Whether activity was completed |
| `actualStartTime` | string | No | Operational | Actual time activity started |
| `actualEndTime` | string | No | Operational | Actual time activity ended |
| `feedback` | string | No | Operational | Guest feedback |
| `isSkipped` | boolean | No | Operational | Whether activity was skipped |
| `skipReason` | string | No | Operational | Reason for skipping |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day activity updated successfully",
  "data": {
    "id": "sda123xyz",
    "safariDayId": "sday456abc",
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
    "isCompleted": true,
    "completedAt": "2024-01-15T11:35:00",
    "actualStartTime": "08:35",
    "actualEndTime": "11:35",
    "feedback": "Excellent experience",
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2024-01-10T10:30:00"
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Safari not editable (for planning updates only)
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-15T14:00:00"
  }
  ```

- **400 Bad Request** - Invalid ID format
- **400 Bad Request** - Activity does not belong to this day
- **400 Bad Request** - Day does not belong to safari
- **404 Not Found** - Safari day activity not found

**Notes:**
- **Planning updates** require safari to be editable
- **Operational updates** (isCompleted, actualStartTime, etc.) can be made even when safari is not editable
- `sortOrder` cannot be updated directly - use the **Reorder** endpoint
- `activityId` (base activity) cannot be changed after creation
- Only provided fields are updated; omitted fields remain unchanged
- When `isCompleted` is set to `true`, `completedAt` is automatically set to current timestamp
- When `isCompleted` is set to `false`, `completedAt` is cleared

---

### 5. Delete Activities

Deletes multiple activities from a safari day in a single request. Requires safari to be in editable state.

**Endpoint:** `DELETE /api/safaris/{safariId}/days/{dayId}/activities`

**Permission Required:** `PERM_DELETE_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
["sda123xyz", "sda124xyz", "sda125xyz"]
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

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-15T15:00:00"
  }
  ```

- **400 Bad Request** - Invalid ID format

**Notes:**
- Safari must be in an editable state
- Only activities belonging to the specified day are deleted
- Invalid or non-existent activity IDs are silently skipped
- Activities that don't belong to the day are skipped (logged as warning)
- The response indicates how many activities were actually deleted
- Remaining activities are automatically renumbered (1, 2, 3, ...)

---

### 6. Reorder Activities

Reorders safari day activities based on a new order provided. This endpoint is designed for drag-and-drop UI operations. Requires safari to be in editable state.

**Endpoint:** `POST /api/safaris/{safariId}/days/{dayId}/activities/reorder`

**Permission Required:** `PERM_UPDATE_SAFARI_DAY_ACTIVITY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
{
  "activityOrder": [
    { "activityId": "sda125xyz", "expectedSortOrder": 1 },
    { "activityId": "sda123xyz", "expectedSortOrder": 2 },
    { "activityId": "sda124xyz", "expectedSortOrder": 3 }
  ]
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityOrder` | array | Yes | List of activity order items |
| `activityOrder[].activityId` | string | Yes | Obfuscated safari day activity ID |
| `activityOrder[].expectedSortOrder` | integer | No | Expected new sort order (for validation) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities reordered successfully",
  "data": [
    {
      "id": "sda125xyz",
      "activityName": "Hot Air Balloon Safari",
      "sortOrder": 1,
      "isCompleted": false
    },
    {
      "id": "sda123xyz",
      "activityName": "Airport Transfer",
      "sortOrder": 2,
      "isCompleted": true
    },
    {
      "id": "sda124xyz",
      "activityName": "Coffee Plantation Tour",
      "sortOrder": 3,
      "isCompleted": false
    }
  ],
  "timestamp": "2024-01-15T16:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

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
- **400 Bad Request** - Duplicate activity IDs
- **400 Bad Request** - Activity does not belong to day
- **400 Bad Request** - Missing activities
- **400 Bad Request** - Expected order mismatch
- **400 Bad Request** - No activities to reorder

**Notes:**
- Safari must be in an editable state
- The `activityOrder` list **must contain ALL activities** of the day
- Position in the list determines the new `sortOrder` (first item = 1)
- If the order is unchanged, returns success with "Activity order unchanged" message
- `expectedSortOrder` is optional and used for additional validation

---

## Data Models

### SafariDayActivityDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated safari day activity ID |
| `safariDayId` | string | Obfuscated safari day ID |
| `activityId` | string | Obfuscated base activity ID |
| `activityName` | string | Name of the base activity |
| `activitySlug` | string | URL-friendly slug of the activity |
| `sortOrder` | integer | Order of the activity within the day |
| `durationHours` | decimal | Planned duration in hours |
| `startTime` | string | Planned start time (e.g., "09:00") |
| `endTime` | string | Planned end time (e.g., "11:30") |
| `notes` | string | Additional notes |
| `isIncludedInPrice` | boolean | Whether included in safari price |
| `isOptional` | boolean | Whether this is an optional add-on |
| `isCompleted` | boolean | Whether activity was completed |
| `completedAt` | datetime | When activity was marked as completed |
| `actualStartTime` | string | Actual time activity started |
| `actualEndTime` | string | Actual time activity ended |
| `feedback` | string | Guest feedback about the activity |
| `isSkipped` | boolean | Whether activity was skipped |
| `skipReason` | string | Reason for skipping the activity |
| `createdAt` | datetime | Creation timestamp |

### CreateSafariDayActivityDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | string | Yes | Obfuscated base activity ID |
| `durationHours` | decimal | No | Duration in hours |
| `startTime` | string | No | Planned start time |
| `endTime` | string | No | Planned end time |
| `notes` | string | No | Additional notes |
| `isIncludedInPrice` | boolean | No | Whether included in price (default: true) |
| `isOptional` | boolean | No | Whether optional (default: false) |

### UpdateSafariDayActivityDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `durationHours` | decimal | No | Duration in hours |
| `startTime` | string | No | Planned start time |
| `endTime` | string | No | Planned end time |
| `notes` | string | No | Additional notes |
| `isIncludedInPrice` | boolean | No | Whether included in price |
| `isOptional` | boolean | No | Whether optional |
| `isCompleted` | boolean | No | Whether completed |
| `actualStartTime` | string | No | Actual start time |
| `actualEndTime` | string | No | Actual end time |
| `feedback` | string | No | Guest feedback |
| `isSkipped` | boolean | No | Whether skipped |
| `skipReason` | string | No | Skip reason |

**Note:** `sortOrder` and `activityId` are not included - they cannot be directly updated.

### ReorderSafariDayActivitiesDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityOrder` | array | Yes | List of ActivityOrderItem objects |

### ActivityOrderItem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `activityId` | string | Yes | Obfuscated safari day activity ID |
| `expectedSortOrder` | integer | No | Expected new sort order (for validation) |

---

## Business Rules

### Safari State Validation

Safari activities can only be modified when the safari is in an **editable state**:

**Editable States:**
- DRAFT
- CONFIRMED
- IN_PROGRESS

**Non-Editable States:**
- COMPLETED
- CANCELLED
- ARCHIVED

**Exception:** Operational updates (marking as completed, adding feedback, tracking actual times) can be made even when safari is not editable.

### Planning vs Operational Updates

| Update Type | Fields | Safari State Required | Use Case |
|-------------|--------|----------------------|-----------|
| **Planning** | durationHours, startTime, endTime, notes, isIncludedInPrice, isOptional | Editable | Pre-safari planning |
| **Operational** | isCompleted, actualStartTime, actualEndTime, feedback, isSkipped, skipReason | Any | During/after safari execution |

### Activity Completion

When `isCompleted` is set to `true`:
- `completedAt` is automatically set to current timestamp
- This records when the activity was marked as complete

When `isCompleted` is set to `false`:
- `completedAt` is cleared (set to null)
- Use this to undo accidental completion marking

### Activity Types

**Standalone Activities** (managed by this API):
- Airport transfers
- City tours
- Cultural visits
- Coffee plantation tours
- Maasai village visits

**Park Activities** (managed by SafariDayParkActivity API):
- Game drives
- Park-specific walking safaris
- Wildlife viewing activities

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_ID` | One or more provided IDs are invalid or malformed |
| `SAFARI_NOT_FOUND` | Safari with the specified ID does not exist |
| `SAFARI_DAY_NOT_FOUND` | Day with the specified ID does not exist |
| `SAFARI_DAY_ACTIVITY_NOT_FOUND` | Activity with the specified ID does not exist |
| `ACTIVITY_NOT_FOUND` | Base activity with the specified ID does not exist |
| `SAFARI_NOT_EDITABLE` | Safari cannot be edited in its current state |
| `DAY_SAFARI_MISMATCH` | Day does not belong to the specified safari |
| `ACTIVITY_DAY_MISMATCH` | Activity does not belong to the specified day |
| `ACTIVITY_NOT_ACTIVE` | The base activity is not active |
| `ACTIVITY_IS_PARK_LINKED` | Activity is linked to parks, use park activities endpoint |
| `ACTIVITY_COUNT_MISMATCH` | Reorder list does not contain all activities |
| `INVALID_ACTIVITY_ID_FORMAT` | One or more activity IDs have invalid format |
| `DUPLICATE_ACTIVITY_IDS` | Reorder list contains duplicate activity IDs |
| `MISSING_ACTIVITY_IDS` | Reorder list is missing some activities |
| `EXPECTED_ORDER_MISMATCH` | Expected sort order does not match position |
| `NO_ACTIVITIES_TO_REORDER` | Day has no activities to reorder |
| `SAFARI_DAY_ACTIVITY_CREATE_FAILED` | Server error during activity creation |
| `SAFARI_DAY_ACTIVITY_UPDATE_FAILED` | Server error during activity update |
| `SAFARI_DAY_ACTIVITIES_DELETE_FAILED` | Server error during activity deletion |
| `SAFARI_DAY_ACTIVITIES_FETCH_FAILED` | Server error during activities retrieval |
| `SAFARI_DAY_ACTIVITIES_REORDER_FAILED` | Server error during reorder operation |

---

## Examples

### Example 1: Planning Safari Day 1 Activities

**Scenario:** Planning arrival day activities for a safari starting January 20, 2024

**Step 1: Add Airport Transfer**
```http
POST /api/safaris/saf456abc/days/sday001/activities
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
POST /api/safaris/saf456abc/days/sday001/activities
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

---

### Example 2: Tracking Activity Completion During Safari

**Scenario:** Guest completes airport transfer on actual safari day

**Request:**
```http
PUT /api/safaris/saf456abc/days/sday001/activities/sda001
Authorization: Bearer <token>
Content-Type: application/json

{
  "isCompleted": true,
  "actualStartTime": "14:10",
  "actualEndTime": "15:05",
  "feedback": "Driver was punctual and vehicle was comfortable"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day activity updated successfully",
  "data": {
    "id": "sda001",
    "activityName": "Airport Transfer",
    "isCompleted": true,
    "completedAt": "2024-01-20T15:06:00",
    "actualStartTime": "14:10",
    "actualEndTime": "15:05",
    "feedback": "Driver was punctual and vehicle was comfortable"
  }
}
```

**Notes:**
- This update can be made even if safari is in COMPLETED state
- `completedAt` is automatically set when `isCompleted` = true

---

### Example 3: Marking Activity as Skipped

**Scenario:** Guest feels unwell and skips the city tour

**Request:**
```http
PUT /api/safaris/saf456abc/days/sday001/activities/sda002
Authorization: Bearer <token>
Content-Type: application/json

{
  "isSkipped": true,
  "skipReason": "Guest feeling unwell, resting at hotel"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day activity updated successfully",
  "data": {
    "id": "sda002",
    "activityName": "Arusha City Tour",
    "isCompleted": false,
    "isSkipped": true,
    "skipReason": "Guest feeling unwell, resting at hotel"
  }
}
```

---

### Example 4: Reordering Activities Before Safari Starts

**Scenario:** Change the order of planned activities

**Request:**
```http
POST /api/safaris/saf456abc/days/sday001/activities/reorder
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityOrder": [
    { "activityId": "sda001", "expectedSortOrder": 1 },
    { "activityId": "sda003", "expectedSortOrder": 2 },
    { "activityId": "sda002", "expectedSortOrder": 3 }
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
      "id": "sda001",
      "activityName": "Airport Transfer",
      "sortOrder": 1
    },
    {
      "id": "sda003",
      "activityName": "Maasai Village Visit",
      "sortOrder": 2
    },
    {
      "id": "sda002",
      "activityName": "Arusha City Tour",
      "sortOrder": 3
    }
  ]
}
```

---

### Example 5: Attempting to Modify Completed Safari (Error)

**Scenario:** Try to add activity to a completed safari

**Request:**
```http
POST /api/safaris/saf_completed/days/sday001/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "activityId": "act_new_tour",
  "durationHours": 2.0,
  "startTime": "10:00",
  "endTime": "12:00"
}
```

**Response:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE",
  "timestamp": "2024-01-25T10:00:00"
}
```

**Notes:**
- Planning operations (create, delete, reorder) require editable state
- Operational updates (completion tracking) are still allowed

---

## Best Practices

### 1. Activity Management

- **Safari State**: Check safari state before planning operations
- **Operational Updates**: Use operational fields during/after safari execution
- **Unique Per Day**: Activities can be added multiple times if needed
- **Standalone vs Park**: Use this API only for non-park activities

### 2. Tracking Completion

- **Mark as Completed**: Set `isCompleted: true` when activity finishes
- **Record Actual Times**: Track `actualStartTime` and `actualEndTime` for analysis
- **Collect Feedback**: Use `feedback` field to capture guest impressions
- **Handle Skips**: Use `isSkipped` and `skipReason` for transparency

### 3. Optional Activities

- **Mark Clearly**: Use `isOptional: true` for add-on experiences
- **Price Exclusion**: Optional activities typically have `isIncludedInPrice: false`
- **Examples**: Hot air balloon safaris, spa treatments, cultural visits

### 4. Timing

- **Realistic Schedules**: Ensure planned times don't overlap
- **Duration Accuracy**: Match `durationHours` with the time window
- **Time Format**: Use 24-hour format (e.g., "06:00", "18:30")
- **Actual vs Planned**: Track both for operational insights

### 5. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Handle `SAFARI_NOT_EDITABLE` gracefully for completed safaris
- Invalid IDs in bulk operations are silently skipped

---

## Common Activity Types for Safaris

| Activity Type | Typical Duration | Usually Optional | Trackable |
|---------------|------------------|------------------|-----------|
| Airport Transfer | 1-2 hours | No | Yes |
| City Tour | 2-4 hours | No | Yes |
| Coffee Plantation Tour | 2-3 hours | Sometimes | Yes |
| Maasai Village Visit | 2-3 hours | Yes | Yes |
| Hot Air Balloon Safari | 3-4 hours | Yes | Yes |
| Walking Safari | 2-4 hours | Sometimes | Yes |
| Cultural Experience | 1-3 hours | Yes | Yes |
| Hotel Transfer | 0.5-1 hour | No | Yes |
| Night Safari | 2-3 hours | Yes | Yes |

---

## Related APIs

- **Safari API:** `/api/safaris` - Main safari management
- **Safari Day API:** `/api/safaris/{safariId}/days` - Day management
- **Safari Day Park Activity API:** `/api/safaris/{safariId}/days/{dayId}/park-activities` - Park-specific activities
- **Activity API:** `/api/activities` - Base activity management

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Create, read, update activities
- Bulk delete activities with state validation
- Reorder activities with comprehensive validation
- Optional activity support
- Safari state validation
- Operational tracking (completion, feedback, actual times)
- Skip tracking with reasons

---

## Support

For technical support or questions about the Safari Day Activity Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
