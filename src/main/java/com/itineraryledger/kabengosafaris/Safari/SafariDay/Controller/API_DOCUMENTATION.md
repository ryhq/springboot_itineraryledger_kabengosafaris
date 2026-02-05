# Safari Day Management API Documentation

## Overview

The Safari Day Management API provides endpoints for managing individual days within actual safari bookings. Safari days are copied from itinerary days when a safari is created, but include additional real-world operational fields.

Key features:
- **Actual dates**: Each safari day has a concrete calendar date (calculated from safari start date)
- **Auto-recalculated dates**: When days are reordered, dates are automatically recalculated
- **Safari-specific fields**: Weather notes, actual times, driver notes, and operational details
- **State validation**: Days can only be edited when the safari is in an editable state
- **Temporal awareness**: Days know if they are past, present, or future

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Safari Day API](#safari-day-api)
   - [Update Day](#1-update-day)
   - [Reorder Days](#2-reorder-days)

2. [Data Models](#data-models)
3. [Business Rules](#business-rules)
4. [Error Codes](#error-codes)
5. [Examples](#examples)

---

## Safari Day API

Base URL: `/api/safaris/{safariId}/days`

### 1. Update Day

Updates the details of a specific safari day. This endpoint allows updating content, activities, locations, and safari-specific operational fields.

**Important:** `dayNumber`, `dayTag`, and `actualDate` cannot be changed via this endpoint. Use the reorder endpoint to change day ordering, which will automatically recalculate dates.

**Endpoint:** `PUT /api/safaris/{safariId}/days/{dayId}`

**Permission Required:** `PERM_UPDATE_SAFARI_DAY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
{
  "title": "Tarangire National Park - Full Day Game Drive",
  "description": "Full day exploring Tarangire's elephant herds and ancient baobab trees.",
  "morningActivities": "Early morning game drive focusing on the Tarangire River area where elephants congregate.",
  "afternoonActivities": "Picnic lunch under acacia trees, followed by afternoon game drive to southern circuits.",
  "eveningActivities": "Sunset viewing from Silale Swamp viewpoint, return to lodge.",
  "wildlifeHighlights": "Large elephant herds (100+ individuals), lions, leopards, cheetahs, buffalo, zebra, wildebeest, giraffes",
  "scenicHighlights": "Ancient baobab trees, Tarangire River, Silale Swamp panoramas",
  "specialNotes": "Bring binoculars for bird watching - over 550 species recorded",
  "startLocation": "Tarangire Sopa Lodge",
  "endLocation": "Tarangire Sopa Lodge",
  "distanceKm": 80,
  "isOvernight": true,
  "mealsIncluded": "B,L,D",
  "internalNotes": "Client requested focus on photography - allow extra time at wildlife sightings",
  "weatherNotes": "Clear morning, afternoon clouds expected around 3pm, no rain predicted",
  "actualStartTime": "06:30",
  "actualEndTime": "18:45",
  "driverNotes": "Vehicle 4x4-003 assigned, fuel topped up, spare tire checked, client prefers window seats"
}
```

**Request Body Fields:**

All fields are optional (null values mean no update for that field):

| Field | Type | Description |
|-------|------|-------------|
| `title` | string | Day title (max 200 chars) |
| `description` | string | Detailed day description |
| `morningActivities` | string | Morning activities description |
| `afternoonActivities` | string | Afternoon activities description |
| `eveningActivities` | string | Evening activities description |
| `wildlifeHighlights` | string | Wildlife that may be seen |
| `scenicHighlights` | string | Scenic highlights of the day |
| `specialNotes` | string | Special notes or instructions |
| `startLocation` | string | Starting location for the day |
| `endLocation` | string | Ending location for the day |
| `distanceKm` | integer | Driving distance in kilometers |
| `isOvernight` | boolean | Whether this is an overnight stay |
| `mealsIncluded` | string | Meals included (e.g., "B,L,D") |
| `internalNotes` | string | Internal staff notes (not shown to clients) |
| `weatherNotes` | string | **Safari-specific**: Weather observations or predictions |
| `actualStartTime` | string | **Safari-specific**: Actual start time (HH:mm format) |
| `actualEndTime` | string | **Safari-specific**: Actual end time (HH:mm format) |
| `driverNotes` | string | **Safari-specific**: Driver/guide notes for this day |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day updated successfully",
  "data": {
    "id": "sday789xyz",
    "safariId": "saf123abc",
    "dayNumber": 2,
    "dayTag": "Day 2",
    "actualDate": "2024-06-16",
    "isPast": false,
    "isToday": false,
    "isFuture": true,
    "title": "Tarangire National Park - Full Day Game Drive",
    "description": "Full day exploring Tarangire's elephant herds and ancient baobab trees.",
    "morningActivities": "Early morning game drive focusing on the Tarangire River area where elephants congregate.",
    "afternoonActivities": "Picnic lunch under acacia trees, followed by afternoon game drive to southern circuits.",
    "eveningActivities": "Sunset viewing from Silale Swamp viewpoint, return to lodge.",
    "wildlifeHighlights": "Large elephant herds (100+ individuals), lions, leopards, cheetahs, buffalo, zebra, wildebeest, giraffes",
    "scenicHighlights": "Ancient baobab trees, Tarangire River, Silale Swamp panoramas",
    "specialNotes": "Bring binoculars for bird watching - over 550 species recorded",
    "startLocation": "Tarangire Sopa Lodge",
    "endLocation": "Tarangire Sopa Lodge",
    "distanceKm": 80,
    "isOvernight": true,
    "mealsIncluded": "B,L,D",
    "internalNotes": "Client requested focus on photography - allow extra time at wildlife sightings",
    "weatherNotes": "Clear morning, afternoon clouds expected around 3pm, no rain predicted",
    "actualStartTime": "06:30",
    "actualEndTime": "18:45",
    "driverNotes": "Vehicle 4x4-003 assigned, fuel topped up, spare tire checked, client prefers window seats",
    "activitiesCount": 3,
    "parksCount": 1,
    "accommodationsCount": 1,
    "createdAt": "2024-01-20T14:30:00",
    "updatedAt": "2024-01-22T09:15:00"
  },
  "timestamp": "2024-01-22T09:15:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid ID",
    "errorCode": "INVALID_ID",
    "timestamp": "2024-01-22T09:15:00"
  }
  ```

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-22T09:15:00"
  }
  ```

- **400 Bad Request** - Day does not belong to safari
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day does not belong to this safari",
    "errorCode": "DAY_SAFARI_MISMATCH",
    "timestamp": "2024-01-22T09:15:00"
  }
  ```

- **404 Not Found** - Safari not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Safari not found",
    "errorCode": "SAFARI_NOT_FOUND",
    "timestamp": "2024-01-22T09:15:00"
  }
  ```

- **404 Not Found** - Safari day not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Safari day not found",
    "errorCode": "SAFARI_DAY_NOT_FOUND",
    "timestamp": "2024-01-22T09:15:00"
  }
  ```

**Notes:**
- Only fields provided in the request body will be updated
- Fields with `null` values are ignored (no update)
- Safari must be in an editable state (DRAFT, CONFIRMED, APPROVED, etc.)
- `dayNumber`, `dayTag`, and `actualDate` are managed automatically and cannot be changed here

---

### 2. Reorder Days

Reorders safari days based on drag-and-drop UI operations. When days are reordered, their `actualDate` fields are automatically recalculated based on the safari's start date.

**Critical Feature:** Date recalculation formula:
```
actualDate = safari.startDate + (dayNumber - 1)
```

This ensures that if a safari starts on June 15th:
- Day 1 → June 15th
- Day 2 → June 16th
- Day 3 → June 17th
- etc.

**Endpoint:** `POST /api/safaris/{safariId}/days/reorder`

**Permission Required:** `PERM_UPDATE_SAFARI_DAY`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID

**Request Body:**
```json
{
  "dayOrder": [
    {
      "dayId": "sday124xyz",
      "expectedDayNumber": 1
    },
    {
      "dayId": "sday123xyz",
      "expectedDayNumber": 2
    },
    {
      "dayId": "sday125xyz",
      "expectedDayNumber": 3
    }
  ]
}
```

**Request Body Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dayOrder` | array | Yes | List of day reorder items in the desired new order |
| `dayOrder[].dayId` | string | Yes | Obfuscated day ID |
| `dayOrder[].expectedDayNumber` | integer | No | Expected new day number (for validation/confirmation) |

**Important Validation Rules:**
1. **Complete list required**: Must include ALL days (no missing days)
2. **No duplicates**: Each day ID can only appear once
3. **No foreign days**: All day IDs must belong to this safari
4. **Count match**: Number of days in request must match existing days count
5. **Expected numbers**: If provided, `expectedDayNumber` must match position in list (1-indexed)

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Days reordered successfully with updated dates",
  "data": [
    {
      "id": "sday124xyz",
      "safariId": "saf123abc",
      "dayNumber": 1,
      "dayTag": "Day 1",
      "actualDate": "2024-06-15",
      "title": "Arusha to Tarangire",
      "isPast": false,
      "isToday": false,
      "isFuture": true,
      "startLocation": "Arusha",
      "endLocation": "Tarangire National Park",
      "createdAt": "2024-01-20T14:30:00",
      "updatedAt": "2024-01-22T11:00:00"
    },
    {
      "id": "sday123xyz",
      "safariId": "saf123abc",
      "dayNumber": 2,
      "dayTag": "Day 2",
      "actualDate": "2024-06-16",
      "title": "Arrival in Arusha",
      "isPast": false,
      "isToday": false,
      "isFuture": true,
      "startLocation": "Kilimanjaro Airport",
      "endLocation": "Arusha",
      "createdAt": "2024-01-20T14:15:00",
      "updatedAt": "2024-01-22T11:00:00"
    },
    {
      "id": "sday125xyz",
      "safariId": "saf123abc",
      "dayNumber": 3,
      "dayTag": "Day 3",
      "actualDate": "2024-06-17",
      "title": "Tarangire to Serengeti",
      "isPast": false,
      "isToday": false,
      "isFuture": true,
      "startLocation": "Tarangire National Park",
      "endLocation": "Serengeti National Park",
      "createdAt": "2024-01-20T14:45:00",
      "updatedAt": "2024-01-22T11:00:00"
    }
  ],
  "timestamp": "2024-01-22T11:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid safari ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid safari ID format",
    "errorCode": "INVALID_SAFARI_ID",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - No days to reorder
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari has no days to reorder",
    "errorCode": "NO_DAYS_TO_REORDER",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Day count mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day order list must contain exactly 7 days. Received: 5",
    "errorCode": "DAY_COUNT_MISMATCH",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Invalid day ID format
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid day ID format(s): sday999xxx, null/empty",
    "errorCode": "INVALID_DAY_ID_FORMAT",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Duplicate day IDs
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Duplicate day ID(s) in reorder list: sday123xyz",
    "errorCode": "DUPLICATE_DAY_IDS",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Day doesn't belong to safari
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day ID(s) do not belong to this safari: sday999xyz",
    "errorCode": "DAY_SAFARI_MISMATCH",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Missing days
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Missing day ID(s) in reorder list: sday126xyz, sday127xyz",
    "errorCode": "MISSING_DAY_IDS",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **400 Bad Request** - Expected number mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Expected day number mismatches: Day sday123xyz: expected 3, but position is 2",
    "errorCode": "EXPECTED_NUMBER_MISMATCH",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

- **404 Not Found** - Safari not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Safari not found",
    "errorCode": "SAFARI_NOT_FOUND",
    "timestamp": "2024-01-22T11:00:00"
  }
  ```

**Reorder Algorithm:**

1. **Validation Phase**:
   - Verify safari exists and is editable
   - Decode all day IDs (detect invalid formats)
   - Check for duplicate day IDs
   - Verify all days belong to this safari
   - Ensure no missing days (complete list)
   - Validate expected day numbers if provided

2. **Two-Pass Update** (to avoid unique constraint violations):
   - **Pass 1**: Set all day numbers to temporary negative values (-1, -2, -3, ...)
   - **Pass 2**: Set final day numbers (1, 2, 3, ...) AND recalculate dates

3. **Date Recalculation**:
   - For each day: `actualDate = safari.startDate + (dayNumber - 1)`
   - Example: Safari starts June 15th → Day 1: June 15, Day 2: June 16, Day 3: June 17

4. **Auto-regeneration**:
   - `dayTag` is auto-regenerated (e.g., "Day 1", "Day 2")
   - Updated timestamp is set

**Notes:**
- If the order hasn't changed, a 200 response is returned with message "Day order unchanged"
- All validations must pass before any database changes are made
- The operation is transactional - either all days are reordered or none are
- Date recalculation happens automatically - cannot be skipped

---

## Data Models

### SafariDayDTO

Response model for safari day data:

```json
{
  "id": "string (obfuscated)",
  "safariId": "string (obfuscated)",
  "dayNumber": "integer",
  "dayTag": "string (auto-generated, e.g., 'Day 1')",
  "title": "string",
  "actualDate": "date (YYYY-MM-DD)",
  "isPast": "boolean (calculated)",
  "isToday": "boolean (calculated)",
  "isFuture": "boolean (calculated)",
  "description": "string",
  "morningActivities": "string",
  "afternoonActivities": "string",
  "eveningActivities": "string",
  "wildlifeHighlights": "string",
  "scenicHighlights": "string",
  "specialNotes": "string",
  "startLocation": "string",
  "endLocation": "string",
  "distanceKm": "integer",
  "isOvernight": "boolean",
  "mealsIncluded": "string",
  "internalNotes": "string",
  "weatherNotes": "string (safari-specific)",
  "actualStartTime": "string (HH:mm format)",
  "actualEndTime": "string (HH:mm format)",
  "driverNotes": "string (safari-specific)",
  "activitiesCount": "integer (calculated)",
  "parksCount": "integer (calculated)",
  "accommodationsCount": "integer (calculated)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

**Key Field Details:**

| Field | Description | Editable? |
|-------|-------------|-----------|
| `actualDate` | Concrete calendar date for this day | No (managed by reorder) |
| `isPast` | True if actualDate is before today | No (auto-calculated) |
| `isToday` | True if actualDate is today | No (auto-calculated) |
| `isFuture` | True if actualDate is after today | No (auto-calculated) |
| `dayNumber` | Sequential number (1, 2, 3, ...) | No (managed by reorder) |
| `dayTag` | Display tag ("Day 1", "Day 2", ...) | No (auto-generated) |
| `weatherNotes` | Weather observations or predictions | Yes (via update endpoint) |
| `actualStartTime` | When activities actually started | Yes (via update endpoint) |
| `actualEndTime` | When activities actually ended | Yes (via update endpoint) |
| `driverNotes` | Driver/guide operational notes | Yes (via update endpoint) |
| `activitiesCount` | Number of activities scheduled | No (calculated) |
| `parksCount` | Number of parks to visit | No (calculated) |
| `accommodationsCount` | Number of accommodations | No (calculated) |

### UpdateSafariDayDTO

Request model for updating a safari day (all fields optional):

```json
{
  "title": "string",
  "description": "string",
  "morningActivities": "string",
  "afternoonActivities": "string",
  "eveningActivities": "string",
  "wildlifeHighlights": "string",
  "scenicHighlights": "string",
  "specialNotes": "string",
  "startLocation": "string",
  "endLocation": "string",
  "distanceKm": "integer",
  "isOvernight": "boolean",
  "mealsIncluded": "string",
  "internalNotes": "string",
  "weatherNotes": "string",
  "actualStartTime": "string",
  "actualEndTime": "string",
  "driverNotes": "string"
}
```

### ReorderSafariDaysDTO

Request model for reordering days:

```json
{
  "dayOrder": [
    {
      "dayId": "string (obfuscated, required)",
      "expectedDayNumber": "integer (optional)"
    }
  ]
}
```

---

## Business Rules

### Safari State Validation

Safari days can only be updated when the safari is in an **editable state**:

**Editable States:**
- `DRAFT` - Initial booking draft
- `CONFIRMED` - Booking confirmed with client
- `APPROVED` - Internally approved
- `PENDING_DEPOSIT` - Awaiting deposit payment
- `DEPOSIT_PAID` - Deposit received
- `FULLY_PAID` - All payments received
- `READY` - Ready to commence
- `IN_PROGRESS` - Safari currently running
- `ON_HOLD` - Temporarily on hold
- `PENDING_DOCUMENTS` - Awaiting required documents
- `PENDING_AVAILABILITY` - Checking availability
- `RESCHEDULING` - Changing dates

**Non-Editable States (updates blocked):**
- `COMPLETED` - Safari finished
- `CANCELLED` - Cancelled booking
- `CANCELLED_BY_CLIENT` - Client cancellation
- `CANCELLED_BY_OPERATOR` - Operator cancellation
- `CANCELLED_FORCE_MAJEURE` - Force majeure cancellation
- `CLOSED` - Administratively closed

### Date Recalculation Rules

When safari days are reordered:

1. **Base calculation**: `actualDate = safari.startDate + (dayNumber - 1)`
2. **Example** (safari starts June 15, 2024):
   - Day 1 → 2024-06-15
   - Day 2 → 2024-06-16
   - Day 3 → 2024-06-17
   - Day 7 → 2024-06-21

3. **Temporal flags recalculated**:
   - `isPast` = actualDate < today
   - `isToday` = actualDate = today
   - `isFuture` = actualDate > today

4. **Safari duration constraint**:
   - `safari.endDate` should equal `safari.startDate + (totalDays - 1)`
   - Last day's actualDate should match safari.endDate

### Field Update Behavior

**Selective Updates (Update Day endpoint):**
- Only fields included in request body are updated
- Fields with `null` values are **ignored** (no update)
- To clear a field, send empty string `""`
- Fields not in request body remain unchanged

**Auto-Managed Fields (cannot be manually updated):**
- `id` - System-generated
- `safariId` - Set at creation
- `dayNumber` - Managed by reorder endpoint
- `dayTag` - Auto-generated from dayNumber
- `actualDate` - Calculated from safari.startDate + (dayNumber - 1)
- `isPast`, `isToday`, `isFuture` - Calculated from actualDate
- `activitiesCount`, `parksCount`, `accommodationsCount` - Calculated from relationships
- `createdAt` - Set at creation
- `updatedAt` - Auto-updated on save

---

## Error Codes

| Error Code | HTTP Status | Description | Common Causes |
|------------|-------------|-------------|---------------|
| `INVALID_SAFARI_ID` | 400 | Invalid safari ID format | Malformed obfuscated ID |
| `INVALID_ID` | 400 | Invalid ID format | Malformed day ID or safari ID |
| `SAFARI_NOT_FOUND` | 404 | Safari doesn't exist | Wrong ID or deleted safari |
| `SAFARI_DAY_NOT_FOUND` | 404 | Day doesn't exist | Wrong day ID or deleted day |
| `SAFARI_NOT_EDITABLE` | 400 | Safari state blocks editing | Safari completed/cancelled |
| `DAY_SAFARI_MISMATCH` | 400 | Day doesn't belong to safari | Cross-safari day access attempt |
| `NO_DAYS_TO_REORDER` | 400 | Safari has no days | Empty safari |
| `DAY_COUNT_MISMATCH` | 400 | Wrong number of days in reorder | Missing or extra days in list |
| `INVALID_DAY_ID_FORMAT` | 400 | Malformed day ID(s) | Invalid obfuscated ID |
| `DUPLICATE_DAY_IDS` | 400 | Same day appears multiple times | Duplicate entries in reorder list |
| `MISSING_DAY_IDS` | 400 | Some days not in reorder list | Incomplete day list |
| `EXPECTED_NUMBER_MISMATCH` | 400 | Position doesn't match expected | Client-server state mismatch |
| `SAFARI_DAY_UPDATE_FAILED` | 500 | Server error during update | Database error, system issue |
| `SAFARI_DAYS_REORDER_FAILED` | 500 | Server error during reorder | Database error, system issue |

---

## Examples

### Example 1: Update Safari Day with Operational Details

**Scenario:** Update a safari day with actual start/end times and driver notes after the day is completed.

**Request:**
```bash
PUT /api/safaris/saf789xyz/days/sday456abc
Content-Type: application/json
Authorization: Bearer <token>

{
  "actualStartTime": "06:15",
  "actualEndTime": "19:30",
  "weatherNotes": "Clear morning, light rain 15:00-15:45 near Seronera, clear evening",
  "driverNotes": "Spotted large lion pride at Seronera River crossing (14 individuals). Clients very happy. Vehicle performed well, no issues. Fuel: 3/4 tank remaining."
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day updated successfully",
  "data": {
    "id": "sday456abc",
    "safariId": "saf789xyz",
    "dayNumber": 3,
    "dayTag": "Day 3",
    "actualDate": "2024-06-17",
    "isPast": true,
    "isToday": false,
    "isFuture": false,
    "title": "Serengeti Central - Full Day Game Drive",
    "actualStartTime": "06:15",
    "actualEndTime": "19:30",
    "weatherNotes": "Clear morning, light rain 15:00-15:45 near Seronera, clear evening",
    "driverNotes": "Spotted large lion pride at Seronera River crossing (14 individuals). Clients very happy. Vehicle performed well, no issues. Fuel: 3/4 tank remaining.",
    "updatedAt": "2024-06-17T20:15:00"
  }
}
```

---

### Example 2: Reorder Safari Days with Date Recalculation

**Scenario:** Client requests to swap Day 1 (arrival) and Day 2 (park visit) due to flight changes. Safari starts June 15th, 2024.

**Before Reorder:**
- Day 1: Arrival in Arusha (June 15) → sday101
- Day 2: Tarangire National Park (June 16) → sday102
- Day 3: Lake Manyara (June 17) → sday103

**Desired After Reorder:**
- Day 1: Tarangire National Park (June 15) → sday102
- Day 2: Arrival in Arusha (June 16) → sday101
- Day 3: Lake Manyara (June 17) → sday103

**Request:**
```bash
POST /api/safaris/saf789xyz/days/reorder
Content-Type: application/json
Authorization: Bearer <token>

{
  "dayOrder": [
    {
      "dayId": "sday102",
      "expectedDayNumber": 1
    },
    {
      "dayId": "sday101",
      "expectedDayNumber": 2
    },
    {
      "dayId": "sday103",
      "expectedDayNumber": 3
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Days reordered successfully with updated dates",
  "data": [
    {
      "id": "sday102",
      "safariId": "saf789xyz",
      "dayNumber": 1,
      "dayTag": "Day 1",
      "actualDate": "2024-06-15",
      "title": "Tarangire National Park",
      "startLocation": "Arusha",
      "endLocation": "Tarangire",
      "updatedAt": "2024-01-25T11:30:00"
    },
    {
      "id": "sday101",
      "safariId": "saf789xyz",
      "dayNumber": 2,
      "dayTag": "Day 2",
      "actualDate": "2024-06-16",
      "title": "Arrival in Arusha",
      "startLocation": "Kilimanjaro Airport",
      "endLocation": "Arusha",
      "updatedAt": "2024-01-25T11:30:00"
    },
    {
      "id": "sday103",
      "safariId": "saf789xyz",
      "dayNumber": 3,
      "dayTag": "Day 3",
      "actualDate": "2024-06-17",
      "title": "Lake Manyara",
      "startLocation": "Tarangire",
      "endLocation": "Lake Manyara",
      "updatedAt": "2024-01-25T11:30:00"
    }
  ]
}
```

**Key Changes:**
- Day numbers updated: sday102 became Day 1, sday101 became Day 2
- Dates recalculated: sday102 now has actualDate = June 15 (was June 16)
- Day tags regenerated: "Day 1", "Day 2", "Day 3"
- All three days show updated timestamps

---

### Example 3: Attempting to Update Non-Editable Safari

**Scenario:** Trying to update a day on a completed safari (should fail).

**Request:**
```bash
PUT /api/safaris/saf999xyz/days/sday777abc
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Updated Title",
  "description": "Updated description"
}
```

**Response:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE",
  "timestamp": "2024-06-25T14:30:00"
}
```

**Resolution:** Safari state must be changed back to an editable state (e.g., from COMPLETED to IN_PROGRESS) before days can be updated. This requires appropriate permissions and business justification.

---

### Example 4: Reorder Validation Error - Missing Days

**Scenario:** Attempting to reorder days but accidentally omitting one day from the list.

**Request:**
```bash
POST /api/safaris/saf789xyz/days/reorder
Content-Type: application/json
Authorization: Bearer <token>

{
  "dayOrder": [
    {
      "dayId": "sday101",
      "expectedDayNumber": 1
    },
    {
      "dayId": "sday102",
      "expectedDayNumber": 2
    }
  ]
}
```

**Response (assuming safari has 3 days):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Day order list must contain exactly 3 days. Received: 2",
  "errorCode": "DAY_COUNT_MISMATCH",
  "timestamp": "2024-01-25T11:30:00"
}
```

**Resolution:** Include ALL days in the reorder request. Fetch the current day list first to ensure completeness.

---

### Example 5: Update Multiple Fields Selectively

**Scenario:** Update only weather notes and driver notes, leaving all other fields unchanged.

**Request:**
```bash
PUT /api/safaris/saf789xyz/days/sday456abc
Content-Type: application/json
Authorization: Bearer <token>

{
  "weatherNotes": "Sunny, 28°C, light breeze, excellent visibility",
  "driverNotes": "Vehicle maintenance completed before departure. All safety equipment checked. Client preferences: prefer off-road routes."
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari day updated successfully",
  "data": {
    "id": "sday456abc",
    "safariId": "saf789xyz",
    "dayNumber": 3,
    "dayTag": "Day 3",
    "actualDate": "2024-06-17",
    "title": "Serengeti Central - Full Day Game Drive",
    "description": "Explore the Seronera Valley...",
    "weatherNotes": "Sunny, 28°C, light breeze, excellent visibility",
    "driverNotes": "Vehicle maintenance completed before departure. All safety equipment checked. Client preferences: prefer off-road routes.",
    "updatedAt": "2024-06-16T08:45:00"
  }
}
```

**Note:** Only `weatherNotes`, `driverNotes`, and `updatedAt` changed. All other fields (title, description, etc.) remained unchanged.

---

## Integration Notes

### Workflow: Itinerary → Safari Days

1. **Itinerary Days Created**: Template days are created in an itinerary (no concrete dates)
2. **Safari Creation**: When a safari is created from an itinerary with a start date:
   - Each itinerary day is copied to a safari day
   - `actualDate` is calculated: `safari.startDate + (dayNumber - 1)`
   - Day numbers and tags are preserved
3. **Safari Day Updates**: Safari-specific fields can be updated as the safari progresses
4. **Reordering**: If day order changes, dates are automatically recalculated

### Best Practices

**For Updates:**
- Only send fields that need updating (selective updates)
- Use `actualStartTime`/`actualEndTime` after the day completes
- Record weather conditions for operational records
- Add driver notes for vehicle tracking and client preferences
- Use `internalNotes` for staff-only information

**For Reordering:**
- Always fetch current day list before reordering to ensure completeness
- Include `expectedDayNumber` for validation (optional but recommended)
- Check safari state before attempting reorder
- Understand that dates will be recalculated - communicate this to clients
- Consider impact on linked accommodations and park bookings when reordering

**Date Management:**
- Never manually update `actualDate` - use reorder endpoint
- Verify safari start date is correct before creating days
- If safari dates change, consider using reorder to trigger date recalculation
- Monitor `isPast`/`isToday`/`isFuture` flags for operational awareness

---

## Permissions

All endpoints require the user to have the appropriate permission:

| Endpoint | Permission Required | Description |
|----------|---------------------|-------------|
| `PUT /days/{dayId}` | `PERM_UPDATE_SAFARI_DAY` | Update safari day details |
| `POST /days/reorder` | `PERM_UPDATE_SAFARI_DAY` | Reorder safari days |

**Permission Assignment:**
- Typically assigned to roles: Safari Operations Manager, Safari Guide, Operations Staff
- Read-only roles should NOT have this permission
- Administrative roles (ADMIN) have all permissions by default

---

## Related APIs

- **Safari API**: `/api/safaris` - Manage safari bookings
- **Safari Pax API**: `/api/safaris/{safariId}/pax` - Manage passenger details
- **Safari Day Parks API**: `/api/safaris/{safariId}/days/{dayId}/parks` - Manage parks for a day
- **Safari Day Activities API**: `/api/safaris/{safariId}/days/{dayId}/activities` - Manage activities for a day
- **Safari Day Accommodations API**: `/api/safaris/{safariId}/days/{dayId}/accommodations` - Manage accommodations for a day

---

## Support

For technical support or questions about this API:
- Review error codes and messages in responses
- Check safari state before attempting updates
- Verify all day IDs are valid and belong to the safari
- Ensure dates are being calculated correctly after reorder operations

**Common Issues:**
- **"Safari not editable"**: Safari must be in an editable state (not COMPLETED or CANCELLED)
- **"Day count mismatch"**: Reorder request must include ALL days, no more, no less
- **"Missing day IDs"**: Ensure all existing days are included in reorder list
- **"Dates seem wrong"**: Dates are calculated from safari.startDate - verify safari start date is correct
