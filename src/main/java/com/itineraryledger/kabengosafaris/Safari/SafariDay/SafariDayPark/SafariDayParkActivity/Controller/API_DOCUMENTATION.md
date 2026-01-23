# Safari Day Park Activity API Documentation

REST API for managing activities within a safari park visit.

## Base URL

```
/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities
```

## Authentication

All endpoints require authentication via Bearer token and appropriate permissions.

---

## Endpoints

### Add Park Activities

Adds one or more activities to a safari park visit. The `sortOrder` is automatically determined based on existing activities. The same activity can be added multiple times (e.g., morning and evening game drives).

**URL:** `POST /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission:** `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
[
  {
    "parkId": "park123abc",
    "activityId": "abc123xyz",
    "durationHours": 2.5,
    "startTime": "06:00",
    "endTime": "08:30",
    "notes": "Morning game drive",
    "isIncludedInPrice": true
  },
  {
    "parkId": "park123abc",
    "activityId": "abc123xyz",
    "durationHours": 2.0,
    "startTime": "16:00",
    "endTime": "18:00",
    "notes": "Evening game drive",
    "isIncludedInPrice": true
  }
]
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| parkId | String | Yes | Obfuscated park ID (must match parent park visit) |
| activityId | String | Yes | Obfuscated activity ID (must be a valid ParkActivity for this park) |
| durationHours | BigDecimal | No | Duration in hours |
| startTime | String | No | Start time (e.g., "06:00") |
| endTime | String | No | End time (e.g., "08:30") |
| notes | String | No | Additional notes |
| isIncludedInPrice | Boolean | No | Whether activity is included in safari price (default: true) |

**Success Response (201):**
```json
{
  "status": 201,
  "message": "2 activities added",
  "data": [
    {
      "id": "ghi789rst",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    },
    {
      "id": "jkl012mno",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 2,
      "durationHours": 2.0,
      "startTime": "16:00",
      "endTime": "18:00",
      "notes": "Evening game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid park visit ID format |
| 404 | SAFARI_PARK_VISIT_NOT_FOUND | Safari park visit not found |
| 500 | SAFARI_PARK_ACTIVITIES_ADD_FAILED | Failed to add safari park activities |

---

### Get Park Activities

Retrieves all activities for a specific safari park visit, ordered by `sortOrder`.

**URL:** `GET /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission:** `PERM_READ_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Safari park activities retrieved",
  "data": [
    {
      "id": "ghi789rst",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": true,
      "completedAt": "2025-01-18T08:45:00",
      "actualDurationHours": 2.75,
      "sightingsNotes": "Spotted leopard with cubs near watering hole",
      "guestExperience": "Excellent experience, guests loved it",
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid park visit ID format |
| 500 | FETCH_FAILED | Failed to fetch safari park activities |

---

### Get Single Park Activity

Retrieves a specific activity from a safari park visit.

**URL:** `GET /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/{activityId}`

**Permission:** `PERM_READ_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |
| activityId | String | Obfuscated park activity entry ID |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Safari park activity retrieved",
  "data": {
    "id": "ghi789rst",
    "safariDayParkId": "parkVisitId123",
    "parkId": "park123abc",
    "parkName": "Serengeti National Park",
    "activityId": "abc123xyz",
    "activityName": "Game Drive",
    "sortOrder": 1,
    "durationHours": 2.5,
    "startTime": "06:00",
    "endTime": "08:30",
    "notes": "Morning game drive",
    "isIncludedInPrice": true,
    "isCompleted": false,
    "completedAt": null,
    "actualDurationHours": null,
    "sightingsNotes": null,
    "guestExperience": null,
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid ID format |
| 400 | OWNERSHIP_MISMATCH | Activity does not belong to this park visit |
| 404 | SAFARI_PARK_ACTIVITY_NOT_FOUND | Safari park activity not found |
| 500 | FETCH_FAILED | Failed to fetch safari park activity |

---

### Update Park Activity

Updates a specific activity within a safari park visit.

**URL:** `PUT /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/{activityId}`

**Permission:** `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |
| activityId | String | Obfuscated park activity entry ID |

**Request Body:**
```json
{
  "durationHours": 3.0,
  "startTime": "05:30",
  "endTime": "08:30",
  "notes": "Extended morning game drive with breakfast",
  "isIncludedInPrice": true
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| durationHours | BigDecimal | No | Duration in hours |
| startTime | String | No | Start time (e.g., "05:30") |
| endTime | String | No | End time (e.g., "08:30") |
| notes | String | No | Additional notes |
| isIncludedInPrice | Boolean | No | Whether activity is included in safari price |

**Note:** The `parkActivity` (park + activity combination) cannot be changed after creation. To change the activity, delete this record and create a new one. The `sortOrder` is handled by the reorder endpoint. Safari-specific tracking fields (isCompleted, actualDurationHours, sightingsNotes, guestExperience, isSkipped, skipReason) are updated via separate tracking endpoints.

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Safari park activity updated successfully",
  "data": {
    "id": "ghi789rst",
    "safariDayParkId": "parkVisitId123",
    "parkId": "park123abc",
    "parkName": "Serengeti National Park",
    "activityId": "abc123xyz",
    "activityName": "Game Drive",
    "sortOrder": 1,
    "durationHours": 3.0,
    "startTime": "05:30",
    "endTime": "08:30",
    "notes": "Extended morning game drive with breakfast",
    "isIncludedInPrice": true,
    "isCompleted": false,
    "completedAt": null,
    "actualDurationHours": null,
    "sightingsNotes": null,
    "guestExperience": null,
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid ID format |
| 400 | ACTIVITY_PARK_VISIT_MISMATCH | Activity does not belong to this park visit |
| 404 | SAFARI_PARK_ACTIVITY_NOT_FOUND | Safari park activity not found |
| 500 | SAFARI_DAY_PARK_ACTIVITY_UPDATE_FAILED | Failed to update safari park activity |

---

### Reorder Park Activities

Reorders activities within a safari park visit. Uses a two-pass approach to avoid unique constraint violations.

**URL:** `POST /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/reorder`

**Permission:** `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
{
  "activityOrder": [
    {
      "activityId": "jkl012mno",
      "expectedSortOrder": 1
    },
    {
      "activityId": "ghi789rst",
      "expectedSortOrder": 2
    }
  ]
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| activityOrder | Array | Yes | List of activities in desired order |
| activityOrder[].activityId | String | Yes | Obfuscated park activity entry ID |
| activityOrder[].expectedSortOrder | Integer | No | Expected position (for validation) |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Activities reordered successfully",
  "data": [
    {
      "id": "jkl012mno",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.0,
      "startTime": "16:00",
      "endTime": "18:00",
      "notes": "Evening game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    },
    {
      "id": "ghi789rst",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 2,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_PARK_VISIT_ID | Invalid park visit ID format |
| 400 | INVALID_ACTIVITY_ID_FORMAT | Invalid activity ID format in reorder list |
| 400 | ACTIVITY_COUNT_MISMATCH | Number of IDs doesn't match existing activities |
| 400 | DUPLICATE_ACTIVITY_IDS | Duplicate activity IDs in reorder list |
| 400 | ACTIVITY_PARK_VISIT_MISMATCH | One or more activities don't belong to this park visit |
| 400 | MISSING_ACTIVITY_IDS | Not all activities are included in reorder list |
| 400 | EXPECTED_ORDER_MISMATCH | Expected sort order doesn't match position |
| 400 | NO_ACTIVITIES_TO_REORDER | Safari park visit has no activities to reorder |
| 404 | SAFARI_PARK_VISIT_NOT_FOUND | Safari park visit not found |
| 500 | SAFARI_PARK_ACTIVITIES_REORDER_FAILED | Failed to reorder activities |

---

### Delete Park Activities

Deletes one or more activities from a safari park visit.

**URL:** `DELETE /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission:** `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
["ghi789rst", "jkl012mno"]
```

**Request Body:** Array of obfuscated park activity entry IDs to delete.

**Success Response (200):**
```json
{
  "status": 200,
  "message": "2 activities deleted",
  "data": null
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid ID format |
| 500 | DELETE_FAILED | Failed to delete safari park activities |

---

## Response DTO Fields

| Field | Type | Description |
|-------|------|-------------|
| id | String | Obfuscated park activity entry ID |
| safariDayParkId | String | Obfuscated parent park visit ID |
| parkId | String | Obfuscated park ID |
| parkName | String | Name of the park |
| activityId | String | Obfuscated activity ID |
| activityName | String | Name of the activity |
| sortOrder | Integer | Display order within the park visit |
| durationHours | BigDecimal | Planned duration in hours |
| startTime | String | Start time (e.g., "06:00") |
| endTime | String | End time (e.g., "08:30") |
| notes | String | Additional notes |
| isIncludedInPrice | Boolean | Whether activity is included in safari price |
| isCompleted | Boolean | Whether the activity was completed |
| completedAt | LocalDateTime | When the activity was completed |
| actualDurationHours | BigDecimal | Actual time spent on the activity |
| sightingsNotes | String | Notable sightings during this activity |
| guestExperience | String | Guest feedback about this activity |
| isSkipped | Boolean | Whether the activity was skipped |
| skipReason | String | Reason for skipping the activity |
| createdAt | LocalDateTime | Creation timestamp |

---

## Related APIs

- **Parent - Park Visits:** `/api/safaris/{safariId}/days/{dayId}/parks`
- **Sibling - Park Tariffs:** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs`

## Notes

- All IDs are obfuscated for security
- `sortOrder` is automatically managed - no need to specify when creating
- **Duplicate activities allowed:** The same activity can be added multiple times per park visit (e.g., morning and evening game drives)
- Activities must be valid ParkActivities associated with the park being visited
- The `parkId` in the create request must match the parent park visit's park
- Reordering uses a two-pass approach (temporary negative values, then final values) to avoid unique constraint violations
- The `isIncludedInPrice` flag indicates whether the activity cost is part of the safari package price
- `startTime` and `endTime` use 24-hour format strings (e.g., "06:00", "18:30")
- Safari-specific tracking fields (isCompleted, actualDurationHours, sightingsNotes, guestExperience, isSkipped, skipReason) are managed separately during safari execution
