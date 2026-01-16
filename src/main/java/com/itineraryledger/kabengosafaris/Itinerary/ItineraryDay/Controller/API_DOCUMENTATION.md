# Itinerary Day Management API Documentation

## Overview

The Itinerary Day Management API provides endpoints for managing individual days within safari itineraries. Each itinerary day represents a single day of the safari, containing details about activities, locations, meals, and highlights.

Key features:
- **Auto-numbering**: Day numbers are automatically assigned based on creation order
- **Auto-tagging**: Day tags (e.g., "Day 1", "Day 2") are automatically generated
- **Reorder support**: Days can be reordered via drag-and-drop UI operations
- **Itinerary limits**: Days cannot exceed the itinerary's `totalDays` count

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Itinerary Day API](#itinerary-day-api)
   - [Create Day](#1-create-day)
   - [Get All Days](#2-get-all-days)
   - [Get Day by ID](#3-get-day-by-id)
   - [Update Day](#4-update-day)
   - [Delete Days](#5-delete-days)
   - [Reorder Days](#6-reorder-days)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)

---

## Itinerary Day API

Base URL: `/api/itineraries/{itineraryId}/days`

### 1. Create Day

Creates a new day for an itinerary. The day number is automatically determined based on existing days (first day = 1, subsequent days increment). The `dayTag` is auto-generated from the day number.

**Endpoint:** `POST /api/itineraries/{itineraryId}/days`

**Permission Required:** `PERM_CREATE_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
{
  "title": "Arrival in Arusha",
  "description": "Welcome to Tanzania! Transfer from airport to hotel.",
  "morningActivities": "Airport pickup, briefing",
  "afternoonActivities": "Hotel check-in, rest",
  "eveningActivities": "Welcome dinner",
  "wildlifeHighlights": null,
  "scenicHighlights": "Mount Meru views",
  "specialNotes": "Ensure passports are ready for registration",
  "startLocation": "Kilimanjaro International Airport",
  "endLocation": "Arusha",
  "distanceKm": 50,
  "isOvernight": true,
  "mealsIncluded": "D"
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | Yes | Day title (max 200 chars) |
| `description` | string | No | Detailed day description |
| `morningActivities` | string | No | Morning activities description |
| `afternoonActivities` | string | No | Afternoon activities description |
| `eveningActivities` | string | No | Evening activities description |
| `wildlifeHighlights` | string | No | Wildlife that may be seen |
| `scenicHighlights` | string | No | Scenic highlights of the day |
| `specialNotes` | string | No | Special notes or instructions |
| `startLocation` | string | No | Starting location for the day |
| `endLocation` | string | No | Ending location for the day |
| `distanceKm` | integer | No | Driving distance in kilometers |
| `isOvernight` | boolean | No | Whether this is an overnight stay (default: true) |
| `mealsIncluded` | string | No | Meals included (e.g., "B,L,D" for Breakfast, Lunch, Dinner) |

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Itinerary day created successfully",
  "data": {
    "id": "day123xyz",
    "itineraryId": "itn456abc",
    "dayNumber": 1,
    "dayTag": "Day 1",
    "title": "Arrival in Arusha",
    "description": "Welcome to Tanzania! Transfer from airport to hotel.",
    "morningActivities": "Airport pickup, briefing",
    "afternoonActivities": "Hotel check-in, rest",
    "eveningActivities": "Welcome dinner",
    "wildlifeHighlights": null,
    "scenicHighlights": "Mount Meru views",
    "specialNotes": "Ensure passports are ready for registration",
    "startLocation": "Kilimanjaro International Airport",
    "endLocation": "Arusha",
    "distanceKm": 50,
    "isOvernight": true,
    "mealsIncluded": "D",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid itinerary ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid itinerary ID",
    "errorCode": "INVALID_ITINERARY_ID",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Itinerary days full
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary already has all 7 days. Cannot add more days.",
    "errorCode": "ITINERARY_DAYS_FULL",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Itinerary not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Itinerary not found",
    "errorCode": "ITINERARY_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

**Notes:**
- `dayNumber` is auto-assigned (not provided in request)
- `dayTag` is auto-generated from `dayNumber` (e.g., "Day 1")
- Days cannot be added beyond the itinerary's `totalDays` limit

---

### 2. Get All Days

Retrieves all days for an itinerary, ordered by day number.

**Endpoint:** `GET /api/itineraries/{itineraryId}/days`

**Permission Required:** `PERM_READ_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary days retrieved successfully",
  "data": [
    {
      "id": "day123xyz",
      "itineraryId": "itn456abc",
      "dayNumber": 1,
      "dayTag": "Day 1",
      "title": "Arrival in Arusha",
      "description": "Welcome to Tanzania!",
      "startLocation": "Kilimanjaro International Airport",
      "endLocation": "Arusha",
      "distanceKm": 50,
      "isOvernight": true,
      "mealsIncluded": "D",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "day124xyz",
      "itineraryId": "itn456abc",
      "dayNumber": 2,
      "dayTag": "Day 2",
      "title": "Arusha to Tarangire",
      "description": "Drive to Tarangire National Park",
      "startLocation": "Arusha",
      "endLocation": "Tarangire National Park",
      "distanceKm": 120,
      "isOvernight": true,
      "mealsIncluded": "B,L,D",
      "createdAt": "2024-01-15T11:00:00",
      "updatedAt": "2024-01-15T11:00:00"
    }
  ],
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid itinerary ID
- **404 Not Found** - Itinerary not found

---

### 3. Get Day by ID

Retrieves a specific day by its ID.

**Endpoint:** `GET /api/itineraries/{itineraryId}/days/{dayId}`

**Permission Required:** `PERM_READ_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day retrieved successfully",
  "data": {
    "id": "day123xyz",
    "itineraryId": "itn456abc",
    "dayNumber": 1,
    "dayTag": "Day 1",
    "title": "Arrival in Arusha",
    "description": "Welcome to Tanzania! Transfer from airport to hotel.",
    "morningActivities": "Airport pickup, briefing",
    "afternoonActivities": "Hotel check-in, rest",
    "eveningActivities": "Welcome dinner",
    "wildlifeHighlights": null,
    "scenicHighlights": "Mount Meru views",
    "specialNotes": "Ensure passports are ready for registration",
    "startLocation": "Kilimanjaro International Airport",
    "endLocation": "Arusha",
    "distanceKm": 50,
    "isOvernight": true,
    "mealsIncluded": "D",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T12:00:00"
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
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

- **400 Bad Request** - Day does not belong to itinerary
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day does not belong to this itinerary",
    "errorCode": "DAY_ITINERARY_MISMATCH",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

- **404 Not Found** - Day not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Itinerary day not found",
    "errorCode": "ITINERARY_DAY_NOT_FOUND",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

---

### 4. Update Day

Updates an existing day. Only provided fields are updated (partial update).

**Endpoint:** `PUT /api/itineraries/{itineraryId}/days/{dayId}`

**Permission Required:** `PERM_UPDATE_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID
- `dayId` (string, required): Obfuscated day ID

**Request Body:**
```json
{
  "title": "Arrival in Arusha - Updated",
  "description": "Welcome to Tanzania! VIP transfer from airport.",
  "mealsIncluded": "L,D"
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | No | Day title (max 200 chars) |
| `description` | string | No | Detailed day description |
| `morningActivities` | string | No | Morning activities description |
| `afternoonActivities` | string | No | Afternoon activities description |
| `eveningActivities` | string | No | Evening activities description |
| `wildlifeHighlights` | string | No | Wildlife that may be seen |
| `scenicHighlights` | string | No | Scenic highlights of the day |
| `specialNotes` | string | No | Special notes or instructions |
| `startLocation` | string | No | Starting location for the day |
| `endLocation` | string | No | Ending location for the day |
| `distanceKm` | integer | No | Driving distance in kilometers |
| `isOvernight` | boolean | No | Whether this is an overnight stay |
| `mealsIncluded` | string | No | Meals included |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day updated successfully",
  "data": {
    "id": "day123xyz",
    "itineraryId": "itn456abc",
    "dayNumber": 1,
    "dayTag": "Day 1",
    "title": "Arrival in Arusha - Updated",
    "description": "Welcome to Tanzania! VIP transfer from airport.",
    "mealsIncluded": "L,D",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T14:00:00"
  },
  "timestamp": "2024-01-15T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID
- **400 Bad Request** - Day does not belong to itinerary
- **404 Not Found** - Day not found

**Notes:**
- `dayNumber` and `dayTag` are **not updatable** - they are auto-managed
- Use the **Reorder** endpoint to change day numbers
- Only provided fields are updated; omitted fields remain unchanged

---

### 5. Delete Days

Deletes one or more days from an itinerary. This endpoint handles both single and bulk deletions.

**Endpoint:** `DELETE /api/itineraries/{itineraryId}/days`

**Permission Required:** `PERM_DELETE_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
["day123xyz", "day124xyz", "day125xyz"]
```

For single deletion, pass a list with one ID:
```json
["day123xyz"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 day(s) deleted successfully",
  "data": null,
  "timestamp": "2024-01-15T15:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid itinerary ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid itinerary ID",
    "errorCode": "INVALID_ITINERARY_ID",
    "timestamp": "2024-01-15T15:00:00"
  }
  ```

**Notes:**
- Only days belonging to the specified itinerary are deleted
- Invalid or non-existent day IDs are silently skipped
- The response indicates how many days were actually deleted
- **Automatic renumbering**: After deletion, remaining days are automatically renumbered to maintain sequential order (1, 2, 3, ...)
- Day tags are also automatically updated (e.g., "Day 1", "Day 2", ...)
- Uses a two-pass approach internally to avoid unique constraint violations during renumbering

**Example - Single Deletion:**
```http
DELETE /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

["day002"]
```

**Example - Bulk Deletion:**
```http
DELETE /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

["day002", "day003", "day004"]
```

---

### 6. Reorder Days

Reorders itinerary days based on a new order provided. This endpoint is designed for drag-and-drop UI operations.

**Endpoint:** `POST /api/itineraries/{itineraryId}/days/reorder`

**Permission Required:** `PERM_UPDATE_ITINERARY_DAY`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
{
  "dayOrder": [
    { "dayId": "day125xyz", "expectedDayNumber": 1 },
    { "dayId": "day123xyz", "expectedDayNumber": 2 },
    { "dayId": "day124xyz", "expectedDayNumber": 3 }
  ]
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dayOrder` | array | Yes | List of day order items |
| `dayOrder[].dayId` | string | Yes | Obfuscated day ID |
| `dayOrder[].expectedDayNumber` | integer | No | Expected new day number (for validation) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Days reordered successfully",
  "data": [
    {
      "id": "day125xyz",
      "dayNumber": 1,
      "dayTag": "Day 1",
      "title": "New first day"
    },
    {
      "id": "day123xyz",
      "dayNumber": 2,
      "dayTag": "Day 2",
      "title": "Previously first day"
    },
    {
      "id": "day124xyz",
      "dayNumber": 3,
      "dayTag": "Day 3",
      "title": "Unchanged position"
    }
  ],
  "timestamp": "2024-01-15T16:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Day count mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day order list must contain exactly 5 days. Received: 3",
    "errorCode": "DAY_COUNT_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Invalid day ID format
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid day ID format(s): abc123, xyz789",
    "errorCode": "INVALID_DAY_ID_FORMAT",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Duplicate day IDs
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Duplicate day ID(s) in reorder list: day123xyz",
    "errorCode": "DUPLICATE_DAY_IDS",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Day does not belong to itinerary
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Day ID(s) do not belong to this itinerary: day999xyz",
    "errorCode": "DAY_ITINERARY_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Missing days
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Missing day ID(s) in reorder list: day126xyz",
    "errorCode": "MISSING_DAY_IDS",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - Expected number mismatch
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Expected day number mismatches: Day day123xyz: expected 1, but position is 2",
    "errorCode": "EXPECTED_NUMBER_MISMATCH",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

- **400 Bad Request** - No days to reorder
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Itinerary has no days to reorder",
    "errorCode": "NO_DAYS_TO_REORDER",
    "timestamp": "2024-01-15T16:00:00"
  }
  ```

**Notes:**
- The `dayOrder` list **must contain ALL days** of the itinerary
- Position in the list determines the new `dayNumber` (first item = Day 1)
- `dayTag` is automatically regenerated based on the new `dayNumber`
- If the order is unchanged, returns success with "Day order unchanged" message
- `expectedDayNumber` is optional and used for additional validation

---

## Data Models

### ItineraryDayDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated day ID |
| `itineraryId` | string | Obfuscated itinerary ID |
| `dayNumber` | integer | Day number (1-indexed, auto-assigned) |
| `dayTag` | string | Day tag (e.g., "Day 1", auto-generated) |
| `title` | string | Day title |
| `description` | string | Detailed description |
| `morningActivities` | string | Morning activities |
| `afternoonActivities` | string | Afternoon activities |
| `eveningActivities` | string | Evening activities |
| `wildlifeHighlights` | string | Wildlife highlights |
| `scenicHighlights` | string | Scenic highlights |
| `specialNotes` | string | Special notes |
| `startLocation` | string | Starting location |
| `endLocation` | string | Ending location |
| `distanceKm` | integer | Distance in kilometers |
| `isOvernight` | boolean | Overnight stay flag |
| `mealsIncluded` | string | Meals included (e.g., "B,L,D") |
| `createdAt` | datetime | Creation timestamp |
| `updatedAt` | datetime | Last update timestamp |

### CreateItineraryDayDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | Yes | Day title (max 200 chars) |
| `description` | string | No | Detailed description |
| `morningActivities` | string | No | Morning activities |
| `afternoonActivities` | string | No | Afternoon activities |
| `eveningActivities` | string | No | Evening activities |
| `wildlifeHighlights` | string | No | Wildlife highlights |
| `scenicHighlights` | string | No | Scenic highlights |
| `specialNotes` | string | No | Special notes |
| `startLocation` | string | No | Starting location |
| `endLocation` | string | No | Ending location |
| `distanceKm` | integer | No | Distance in kilometers |
| `isOvernight` | boolean | No | Overnight stay flag |
| `mealsIncluded` | string | No | Meals included |

### UpdateItineraryDayDTO

Same fields as `CreateItineraryDayDTO`, but all fields are optional. Only provided fields are updated.

**Note:** `dayNumber` and `dayTag` are not included - they cannot be directly updated.

### ReorderItineraryDaysDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dayOrder` | array | Yes | List of DayOrderItem objects |

### DayOrderItem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dayId` | string | Yes | Obfuscated day ID |
| `expectedDayNumber` | integer | No | Expected new day number (for validation) |

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_ITINERARY_ID` | The provided itinerary ID is invalid or malformed |
| `INVALID_ID` | One or more provided IDs are invalid |
| `ITINERARY_NOT_FOUND` | Itinerary with the specified ID does not exist |
| `ITINERARY_DAY_NOT_FOUND` | Day with the specified ID does not exist |
| `ITINERARY_DAYS_FULL` | Cannot add more days; itinerary has reached `totalDays` limit |
| `DAY_ITINERARY_MISMATCH` | Day does not belong to the specified itinerary |
| `DAY_COUNT_MISMATCH` | Reorder list does not contain all days |
| `INVALID_DAY_ID_FORMAT` | One or more day IDs have invalid format |
| `DUPLICATE_DAY_IDS` | Reorder list contains duplicate day IDs |
| `MISSING_DAY_IDS` | Reorder list is missing some days |
| `EXPECTED_NUMBER_MISMATCH` | Expected day number does not match position |
| `NO_DAYS_TO_REORDER` | Itinerary has no days to reorder |
| `ITINERARY_DAY_CREATE_FAILED` | Server error during day creation |
| `ITINERARY_DAY_UPDATE_FAILED` | Server error during day update |
| `ITINERARY_DAY_DELETE_FAILED` | Server error during day deletion |
| `ITINERARY_DAYS_FETCH_FAILED` | Server error during days retrieval |
| `ITINERARY_DAYS_REORDER_FAILED` | Server error during reorder operation |

---

## Examples

### Example 1: Creating a 3-Day Safari Itinerary

**Step 1: Create Day 1**
```http
POST /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Arrival in Arusha",
  "description": "Welcome to Tanzania! Transfer from airport.",
  "startLocation": "Kilimanjaro International Airport",
  "endLocation": "Arusha",
  "distanceKm": 50,
  "isOvernight": true,
  "mealsIncluded": "D"
}
```

**Response:** Day created with `dayNumber: 1`, `dayTag: "Day 1"`

**Step 2: Create Day 2**
```http
POST /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Arusha to Serengeti",
  "description": "Drive to Serengeti via Ngorongoro",
  "startLocation": "Arusha",
  "endLocation": "Serengeti National Park",
  "distanceKm": 325,
  "wildlifeHighlights": "Flamingos at Lake Manyara, wildlife en route",
  "isOvernight": true,
  "mealsIncluded": "B,L,D"
}
```

**Response:** Day created with `dayNumber: 2`, `dayTag: "Day 2"`

**Step 3: Create Day 3**
```http
POST /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Full Day Serengeti Safari",
  "description": "Full day game drive in the Serengeti",
  "startLocation": "Serengeti Camp",
  "endLocation": "Serengeti Camp",
  "distanceKm": 100,
  "wildlifeHighlights": "Big Five, wildebeest migration",
  "isOvernight": true,
  "mealsIncluded": "B,L,D"
}
```

**Response:** Day created with `dayNumber: 3`, `dayTag: "Day 3"`

---

### Example 2: Reordering Days After Review

After creating the itinerary, you decide Day 3 should actually be Day 2:

**Request:**
```http
POST /api/itineraries/itn456abc/days/reorder
Authorization: Bearer <token>
Content-Type: application/json

{
  "dayOrder": [
    { "dayId": "day001", "expectedDayNumber": 1 },
    { "dayId": "day003", "expectedDayNumber": 2 },
    { "dayId": "day002", "expectedDayNumber": 3 }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Days reordered successfully",
  "data": [
    {
      "id": "day001",
      "dayNumber": 1,
      "dayTag": "Day 1",
      "title": "Arrival in Arusha"
    },
    {
      "id": "day003",
      "dayNumber": 2,
      "dayTag": "Day 2",
      "title": "Full Day Serengeti Safari"
    },
    {
      "id": "day002",
      "dayNumber": 3,
      "dayTag": "Day 3",
      "title": "Arusha to Serengeti"
    }
  ]
}
```

---

### Example 3: Updating Day Details

**Request:**
```http
PUT /api/itineraries/itn456abc/days/day001
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "VIP Arrival in Arusha",
  "description": "Luxury VIP transfer from airport with champagne welcome",
  "specialNotes": "Guest is celebrating anniversary"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Itinerary day updated successfully",
  "data": {
    "id": "day001",
    "dayNumber": 1,
    "dayTag": "Day 1",
    "title": "VIP Arrival in Arusha",
    "description": "Luxury VIP transfer from airport with champagne welcome",
    "specialNotes": "Guest is celebrating anniversary",
    "startLocation": "Kilimanjaro International Airport",
    "endLocation": "Arusha",
    "distanceKm": 50,
    "isOvernight": true,
    "mealsIncluded": "D"
  }
}
```

---

### Example 4: Bulk Delete Days

**Request:**
```http
DELETE /api/itineraries/itn456abc/days
Authorization: Bearer <token>
Content-Type: application/json

["day002", "day003"]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 itinerary day(s) deleted successfully",
  "data": null
}
```

---

## Best Practices

### 1. Day Management

- **Sequential Creation**: Create days in order for clarity, as `dayNumber` is auto-assigned
- **Use Reorder**: To change day order, always use the reorder endpoint
- **Don't Exceed Limit**: Check itinerary's `totalDays` before adding days

### 2. Content Guidelines

- **Meaningful Titles**: Use descriptive titles like "Serengeti Game Drive" not "Day 2"
- **Consistent Meals Format**: Use "B,L,D" pattern (Breakfast, Lunch, Dinner)
- **Distance Accuracy**: Provide accurate driving distances for planning

### 3. Reorder Operations

- **Include All Days**: Reorder request must include every day of the itinerary
- **Use expectedDayNumber**: Optional but helpful for validation
- **Preserve Positions**: If no reorder needed, the API returns success without changes

### 4. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Validate day IDs before bulk operations

---

## Related APIs

- **Itinerary API:** `/api/itineraries` - Main itinerary management
- **Itinerary Pax API:** `/api/itineraries/{itineraryId}/pax` - Passenger categories

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Create, read, update, delete day operations
- Bulk delete days
- Reorder days with comprehensive validation

---

## Support

For technical support or questions about the Itinerary Day Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
