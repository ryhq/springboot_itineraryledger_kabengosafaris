# Itinerary Day Park API Documentation

REST API for managing park visits within itinerary days.

## Base URL

```
/api/itineraries/{itineraryId}/days/{dayId}/parks
```

## Authentication

All endpoints require authentication via Bearer token and appropriate permissions.

---

## Endpoints

### Create Park Visit

Creates a new park visit for a specific day in an itinerary. The `sortOrder` is automatically determined based on existing park visits.

**URL:** `POST /api/itineraries/{itineraryId}/days/{dayId}/parks`

**Permission:** `PERM_CREATE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Request Body:**
```json
{
  "parkId": "abc123xyz",
  "entryType": "DAY_TRIP",
  "arrivalTime": "08:00",
  "departureTime": "17:00",
  "notes": "Morning game drive included"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| parkId | String | Yes | Obfuscated park ID |
| entryType | String | Yes | Type of park visit: `TRANSIT`, `DAY_TRIP`, or `SLEEP_OVER` |
| arrivalTime | String | No | Arrival time (e.g., "08:00") |
| departureTime | String | No | Departure time (e.g., "17:00") |
| notes | String | No | Additional notes |

**Entry Types:**
| Value | Display Name | Description |
|-------|--------------|-------------|
| TRANSIT | Transit | Passing through without extended stay |
| DAY_TRIP | Day Trip | Full day visit without overnight stay |
| SLEEP_OVER | Sleep Over | Overnight stay in the park |

**Success Response (201):**
```json
{
  "status": 201,
  "message": "Park visit created successfully",
  "data": {
    "id": "def456uvw",
    "itineraryDayId": "dayId123",
    "parkId": "abc123xyz",
    "parkName": "Serengeti National Park",
    "parkSlug": "serengeti-national-park",
    "entryType": "DAY_TRIP",
    "entryTypeDisplayName": "Day Trip",
    "sortOrder": 1,
    "arrivalTime": "08:00",
    "departureTime": "17:00",
    "notes": "Morning game drive included",
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ITINERARY_ID | Invalid itinerary ID format |
| 400 | INVALID_DAY_ID | Invalid day ID format |
| 400 | INVALID_PARK_ID | Invalid park ID format |
| 404 | ITINERARY_NOT_FOUND | Itinerary not found |
| 404 | DAY_NOT_FOUND | Day not found |
| 404 | PARK_NOT_FOUND | Park not found |

---

### Get Park Visits

Retrieves all park visits for a specific day, ordered by `sortOrder`.

**URL:** `GET /api/itineraries/{itineraryId}/days/{dayId}/parks`

**Permission:** `PERM_READ_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Park visits retrieved successfully",
  "data": [
    {
      "id": "def456uvw",
      "itineraryDayId": "dayId123",
      "parkId": "abc123xyz",
      "parkName": "Serengeti National Park",
      "parkSlug": "serengeti-national-park",
      "entryType": "DAY_TRIP",
      "entryTypeDisplayName": "Day Trip",
      "sortOrder": 1,
      "arrivalTime": "08:00",
      "departureTime": "17:00",
      "notes": "Morning game drive included",
      "createdAt": "2025-01-18T10:30:00"
    },
    {
      "id": "ghi789rst",
      "itineraryDayId": "dayId123",
      "parkId": "jkl012mno",
      "parkName": "Ngorongoro Crater",
      "parkSlug": "ngorongoro-crater",
      "entryType": "TRANSIT",
      "entryTypeDisplayName": "Transit",
      "sortOrder": 2,
      "arrivalTime": "18:00",
      "departureTime": null,
      "notes": null,
      "createdAt": "2025-01-18T11:00:00"
    }
  ]
}
```

---

### Update Park Visit

Updates an existing park visit. Only provided fields will be updated.

**URL:** `PUT /api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}`

**Permission:** `PERM_UPDATE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
{
  "entryType": "SLEEP_OVER",
  "arrivalTime": "14:00",
  "departureTime": null,
  "notes": "Overnight stay at lodge"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| entryType | String | No | Type of park visit |
| arrivalTime | String | No | Arrival time |
| departureTime | String | No | Departure time |
| notes | String | No | Additional notes |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Park visit updated successfully",
  "data": {
    "id": "def456uvw",
    "itineraryDayId": "dayId123",
    "parkId": "abc123xyz",
    "parkName": "Serengeti National Park",
    "parkSlug": "serengeti-national-park",
    "entryType": "SLEEP_OVER",
    "entryTypeDisplayName": "Sleep Over",
    "sortOrder": 1,
    "arrivalTime": "14:00",
    "departureTime": null,
    "notes": "Overnight stay at lodge",
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid ID format |
| 400 | OWNERSHIP_MISMATCH | Park visit does not belong to the specified day/itinerary |
| 404 | PARK_VISIT_NOT_FOUND | Park visit not found |

---

### Reorder Park Visits

Reorders park visits within a day. Uses a two-pass approach to avoid unique constraint violations.

**URL:** `POST /api/itineraries/{itineraryId}/days/{dayId}/parks/reorder`

**Permission:** `PERM_UPDATE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Request Body:**
```json
{
  "parkVisitOrder": [
    {
      "parkVisitId": "ghi789rst",
      "expectedSortOrder": 1
    },
    {
      "parkVisitId": "def456uvw",
      "expectedSortOrder": 2
    }
  ]
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| parkVisitOrder | Array | Yes | List of park visits in desired order |
| parkVisitOrder[].parkVisitId | String | Yes | Obfuscated park visit ID |
| parkVisitOrder[].expectedSortOrder | Integer | No | Expected position (for validation) |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Park visits reordered successfully",
  "data": [
    {
      "id": "ghi789rst",
      "parkName": "Ngorongoro Crater",
      "sortOrder": 1,
      ...
    },
    {
      "id": "def456uvw",
      "parkName": "Serengeti National Park",
      "sortOrder": 2,
      ...
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ID | Invalid ID format |
| 400 | PARK_VISIT_COUNT_MISMATCH | Number of IDs doesn't match existing park visits |
| 400 | PARK_VISIT_NOT_IN_DAY | One or more park visits don't belong to this day |

---

### Delete Park Visits

Deletes one or more park visits. Remaining park visits are automatically renumbered to maintain sequential `sortOrder`.

**URL:** `DELETE /api/itineraries/{itineraryId}/days/{dayId}/parks`

**Permission:** `PERM_DELETE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Request Body:**
```json
["def456uvw", "ghi789rst"]
```

**Request Body:** Array of obfuscated park visit IDs to delete.

**Success Response (200):**
```json
{
  "status": 200,
  "message": "2 park visit(s) deleted successfully",
  "data": null
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_ITINERARY_ID | Invalid itinerary ID format |
| 500 | PARK_VISITS_DELETE_FAILED | Failed to delete park visits |

---

## Related APIs

- **Park Activities:** `/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/activities`
- **Park Tariffs:** `/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs`

## Notes

- All IDs are obfuscated for security
- `sortOrder` is automatically managed - no need to specify when creating
- Deletion automatically renumbers remaining items to maintain sequential order
- Reordering uses a two-pass approach (temporary negative values, then final values) to avoid unique constraint violations
