# Itinerary Day Park Tariff API Documentation

REST API for managing tariffs within a park visit.

## Base URL

```
/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs
```

## Authentication

All endpoints require authentication via Bearer token and appropriate permissions.

---

## Endpoints

### Add Park Tariffs

Adds one or more tariffs to a park visit.

**URL:** `POST /api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs`

**Permission:** `PERM_UPDATE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
[
  {
    "parkId": "abc123xyz",
    "tariffId": "def456uvw",
    "notes": "Adult entry fee",
    "isIncludedInPrice": true
  },
  {
    "parkId": "abc123xyz",
    "tariffId": "ghi789rst",
    "notes": "Vehicle fee",
    "isIncludedInPrice": false
  }
]
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| parkId | String | Yes | Obfuscated park ID (must match the parent park visit's park) |
| tariffId | String | Yes | Obfuscated tariff ID (must be a valid tariff for this park) |
| notes | String | No | Additional notes |
| isIncludedInPrice | Boolean | No | Whether tariff is included in itinerary price (default: true) |

**Success Response (201):**
```json
{
  "status": 201,
  "message": "2 park tariff(s) added successfully",
  "data": [
    {
      "id": "jkl012mno",
      "itineraryDayParkId": "parkVisitId123",
      "parkId": "abc123xyz",
      "parkName": "Serengeti National Park",
      "tariffId": "def456uvw",
      "tariffName": "Adult Entry Fee",
      "notes": "Adult entry fee",
      "isIncludedInPrice": true,
      "createdAt": "2025-01-18T10:30:00"
    },
    {
      "id": "pqr345stu",
      "itineraryDayParkId": "parkVisitId123",
      "parkId": "abc123xyz",
      "parkName": "Serengeti National Park",
      "tariffId": "ghi789rst",
      "tariffName": "Vehicle Fee",
      "notes": "Vehicle fee",
      "isIncludedInPrice": false,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_PARK_VISIT_ID | Invalid park visit ID format |
| 400 | INVALID_PARK_ID | Invalid park ID format |
| 400 | INVALID_TARIFF_ID | Invalid tariff ID format |
| 400 | PARK_ID_MISMATCH | Park ID doesn't match the park visit's park |
| 400 | TARIFF_NOT_AVAILABLE_FOR_PARK | Tariff is not available for this park |
| 404 | PARK_VISIT_NOT_FOUND | Park visit not found |
| 404 | TARIFF_NOT_FOUND | Tariff not found |
| 500 | PARK_TARIFFS_ADD_FAILED | Failed to add park tariffs |

---

### Get Park Tariffs

Retrieves all tariffs for a specific park visit.

**URL:** `GET /api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs`

**Permission:** `PERM_READ_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Park tariffs retrieved successfully",
  "data": [
    {
      "id": "jkl012mno",
      "itineraryDayParkId": "parkVisitId123",
      "parkId": "abc123xyz",
      "parkName": "Serengeti National Park",
      "tariffId": "def456uvw",
      "tariffName": "Adult Entry Fee",
      "notes": "Adult entry fee",
      "isIncludedInPrice": true,
      "createdAt": "2025-01-18T10:30:00"
    },
    {
      "id": "pqr345stu",
      "itineraryDayParkId": "parkVisitId123",
      "parkId": "abc123xyz",
      "parkName": "Serengeti National Park",
      "tariffId": "ghi789rst",
      "tariffName": "Vehicle Fee",
      "notes": "Vehicle fee",
      "isIncludedInPrice": false,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_PARK_VISIT_ID | Invalid park visit ID format |
| 404 | PARK_VISIT_NOT_FOUND | Park visit not found |
| 500 | PARK_TARIFFS_FETCH_FAILED | Failed to fetch park tariffs |

---

### Get Single Park Tariff

Retrieves a specific tariff from a park visit.

**URL:** `GET /api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs/{tariffId}`

**Permission:** `PERM_READ_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |
| tariffId | String | Obfuscated park tariff entry ID |

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Park tariff retrieved successfully",
  "data": {
    "id": "jkl012mno",
    "itineraryDayParkId": "parkVisitId123",
    "parkId": "abc123xyz",
    "parkName": "Serengeti National Park",
    "tariffId": "def456uvw",
    "tariffName": "Adult Entry Fee",
    "notes": "Adult entry fee",
    "isIncludedInPrice": true,
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_PARK_VISIT_ID | Invalid park visit ID format |
| 400 | INVALID_TARIFF_ID | Invalid tariff ID format |
| 400 | OWNERSHIP_MISMATCH | Tariff does not belong to this park visit |
| 404 | PARK_VISIT_NOT_FOUND | Park visit not found |
| 404 | PARK_TARIFF_NOT_FOUND | Park tariff not found |
| 500 | PARK_TARIFF_FETCH_FAILED | Failed to fetch park tariff |

---

### Delete Park Tariffs

Deletes one or more tariffs from a park visit.

**URL:** `DELETE /api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/tariffs`

**Permission:** `PERM_UPDATE_ITINERARY_DAY_PARK`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| parkVisitId | String | Obfuscated park visit ID |

**Request Body:**
```json
["jkl012mno", "pqr345stu"]
```

**Request Body:** Array of obfuscated park tariff entry IDs to delete.

**Success Response (200):**
```json
{
  "status": 200,
  "message": "2 park tariff(s) deleted successfully",
  "data": null
}
```

**Error Responses:**
| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | INVALID_PARK_VISIT_ID | Invalid park visit ID format |
| 500 | PARK_TARIFFS_DELETE_FAILED | Failed to delete park tariffs |

---

## Related APIs

- **Parent - Park Visits:** `/api/itineraries/{itineraryId}/days/{dayId}/parks`
- **Sibling - Park Activities:** `/api/itineraries/{itineraryId}/days/{dayId}/parks/{parkVisitId}/activities`

## Notes

- All IDs are obfuscated for security
- Tariffs must be valid tariffs associated with the park being visited
- The `parkId` in the request body must match the park of the parent park visit
- The `isIncludedInPrice` flag indicates whether the tariff cost is part of the itinerary package price
- Unlike activities, tariffs do not have a `sortOrder` field as they are not ordered
