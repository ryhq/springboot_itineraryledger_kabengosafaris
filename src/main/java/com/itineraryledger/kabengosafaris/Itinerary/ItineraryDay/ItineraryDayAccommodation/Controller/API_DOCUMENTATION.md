# Itinerary Day Accommodation API Documentation

Base URL: `/api/itineraries/{itineraryId}/days/{dayId}/accommodations`

## Overview

This API manages accommodations for specific days in an itinerary. Each accommodation entry includes room configuration (type, standard, board type) and supports both primary and alternative accommodation options.

**Pax Capacity Validation:** When creating or updating primary (non-alternative) accommodations, the system validates that the total room capacity meets or exceeds the total passenger count configured for the itinerary.

---

## Endpoints

### 1. Create Accommodation

Creates a new accommodation entry for an itinerary day.

**Endpoint:** `POST /api/itineraries/{itineraryId}/days/{dayId}/accommodations`

**Permission:** `PERM_CREATE_ITINERARY_DAY_ACCOMMODATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Request Body:**
```json
{
  "accommodationId": "abc123",
  "roomTypeId": "def456",
  "roomStandardId": "ghi789",
  "boardTypeId": "jkl012",
  "roomCount": 2,
  "isAlternative": false,
  "notes": "Ocean view rooms preferred"
}
```

**Request Body Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| accommodationId | String | Yes | Obfuscated ID of the accommodation |
| roomTypeId | String | Yes | Obfuscated ID of the room type (e.g., Single, Double, Twin) |
| roomStandardId | String | Yes | Obfuscated ID of the room standard (e.g., Standard, Deluxe, Suite) |
| boardTypeId | String | Yes | Obfuscated ID of the board type (e.g., Room Only, B&B, Half Board, Full Board) |
| roomCount | Integer | No | Number of rooms (default: 1, minimum: 1) |
| isAlternative | Boolean | No | Whether this is an alternative/backup option (default: false) |
| notes | String | No | Additional notes for this accommodation |

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Accommodation created successfully",
  "data": {
    "id": "mno345",
    "itineraryDayId": "dayId123",
    "accommodationId": "abc123",
    "accommodationName": "Serena Lodge",
    "accommodationSlug": "serena-lodge",
    "roomTypeId": "def456",
    "roomTypeName": "Double Room",
    "roomStandardId": "ghi789",
    "roomStandardName": "Deluxe",
    "boardTypeId": "jkl012",
    "boardTypeName": "Full Board",
    "roomCount": 2,
    "isAlternative": false,
    "notes": "Ocean view rooms preferred",
    "createdAt": "2026-01-18T10:30:00",
    "updatedAt": "2026-01-18T10:30:00"
  }
}
```

**Error Responses:**
| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_ID | Invalid obfuscated ID format |
| 400 | DAY_ITINERARY_MISMATCH | Day does not belong to the specified itinerary |
| 400 | INSUFFICIENT_ACCOMMODATION_CAPACITY | Total room capacity is less than total pax count |
| 404 | ITINERARY_DAY_NOT_FOUND | Itinerary day not found |
| 404 | ACCOMMODATION_NOT_FOUND | Accommodation not found |
| 404 | ROOM_TYPE_NOT_FOUND | Room type not found |
| 404 | ROOM_STANDARD_NOT_FOUND | Room standard not found |
| 404 | BOARD_TYPE_NOT_FOUND | Board type not found |
| 500 | ACCOMMODATION_CREATE_FAILED | Internal server error |

---

### 2. Get Accommodations

Retrieves all accommodations for an itinerary day.

**Endpoint:** `GET /api/itineraries/{itineraryId}/days/{dayId}/accommodations`

**Permission:** `PERM_READ_ITINERARY_DAY_ACCOMMODATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodations retrieved successfully",
  "data": [
    {
      "id": "mno345",
      "itineraryDayId": "dayId123",
      "accommodationId": "abc123",
      "accommodationName": "Serena Lodge",
      "accommodationSlug": "serena-lodge",
      "roomTypeId": "def456",
      "roomTypeName": "Double Room",
      "roomStandardId": "ghi789",
      "roomStandardName": "Deluxe",
      "boardTypeId": "jkl012",
      "boardTypeName": "Full Board",
      "roomCount": 2,
      "isAlternative": false,
      "notes": "Ocean view rooms preferred",
      "createdAt": "2026-01-18T10:30:00",
      "updatedAt": "2026-01-18T10:30:00"
    },
    {
      "id": "pqr678",
      "itineraryDayId": "dayId123",
      "accommodationId": "xyz999",
      "accommodationName": "Safari Camp",
      "accommodationSlug": "safari-camp",
      "roomTypeId": "def456",
      "roomTypeName": "Double Room",
      "roomStandardId": "ghi789",
      "roomStandardName": "Standard",
      "boardTypeId": "jkl012",
      "boardTypeName": "Full Board",
      "roomCount": 2,
      "isAlternative": true,
      "notes": "Alternative if Serena is full",
      "createdAt": "2026-01-18T10:35:00",
      "updatedAt": "2026-01-18T10:35:00"
    }
  ]
}
```

**Error Responses:**
| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_ID | Invalid obfuscated ID format |
| 400 | DAY_ITINERARY_MISMATCH | Day does not belong to the specified itinerary |
| 404 | ITINERARY_DAY_NOT_FOUND | Itinerary day not found |

---

### 3. Update Accommodation

Updates an existing accommodation entry. Only `roomCount`, `isAlternative`, and `notes` can be modified. To change the core accommodation configuration (accommodation, room type, room standard, board type), delete and recreate the entry.

**Endpoint:** `PUT /api/itineraries/{itineraryId}/days/{dayId}/accommodations/{accommodationId}`

**Permission:** `PERM_UPDATE_ITINERARY_DAY_ACCOMMODATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| accommodationId | String | Obfuscated accommodation entry ID |

**Request Body:**
```json
{
  "roomCount": 3,
  "isAlternative": false,
  "notes": "Updated notes - confirmed booking"
}
```

**Request Body Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| roomCount | Integer | No | Number of rooms (minimum: 1 if provided) |
| isAlternative | Boolean | No | Whether this is an alternative/backup option |
| notes | String | No | Additional notes (can be set to null to clear) |

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodation updated successfully",
  "data": {
    "id": "mno345",
    "itineraryDayId": "dayId123",
    "accommodationId": "abc123",
    "accommodationName": "Serena Lodge",
    "accommodationSlug": "serena-lodge",
    "roomTypeId": "def456",
    "roomTypeName": "Double Room",
    "roomStandardId": "ghi789",
    "roomStandardName": "Deluxe",
    "boardTypeId": "jkl012",
    "boardTypeName": "Full Board",
    "roomCount": 3,
    "isAlternative": false,
    "notes": "Updated notes - confirmed booking",
    "createdAt": "2026-01-18T10:30:00",
    "updatedAt": "2026-01-18T11:00:00"
  }
}
```

**Error Responses:**
| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_ID | Invalid obfuscated ID format |
| 400 | ACCOMMODATION_DAY_MISMATCH | Accommodation does not belong to this day |
| 400 | DAY_ITINERARY_MISMATCH | Day does not belong to this itinerary |
| 400 | INSUFFICIENT_ACCOMMODATION_CAPACITY | Total room capacity is less than total pax count |
| 404 | ACCOMMODATION_NOT_FOUND | Accommodation entry not found |
| 500 | ACCOMMODATION_UPDATE_FAILED | Internal server error |

---

### 4. Delete Single Accommodation

Deletes a single accommodation entry from an itinerary day.

**Endpoint:** `DELETE /api/itineraries/{itineraryId}/days/{dayId}/accommodations/{accommodationId}`

**Permission:** `PERM_DELETE_ITINERARY_DAY_ACCOMMODATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |
| accommodationId | String | Obfuscated accommodation entry ID |

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodation deleted successfully",
  "data": null
}
```

**Error Responses:**
| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_ID | Invalid obfuscated ID format |
| 400 | ACCOMMODATION_DAY_MISMATCH | Accommodation does not belong to this day |
| 400 | DAY_ITINERARY_MISMATCH | Day does not belong to this itinerary |
| 404 | ACCOMMODATION_NOT_FOUND | Accommodation entry not found |
| 500 | ACCOMMODATION_DELETE_FAILED | Internal server error |

---

### 5. Delete Multiple Accommodations

Deletes multiple accommodation entries from an itinerary day in a single request.

**Endpoint:** `DELETE /api/itineraries/{itineraryId}/days/{dayId}/accommodations`

**Permission:** `PERM_DELETE_ITINERARY_DAY_ACCOMMODATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |
| dayId | String | Obfuscated day ID |

**Request Body:**
```json
["mno345", "pqr678", "stu901"]
```

**Request Body:** Array of obfuscated accommodation entry IDs to delete.

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "3 accommodations deleted successfully",
  "data": null
}
```

**Error Responses:**
| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_ID | Invalid obfuscated ID format |
| 400 | ACCOMMODATION_DAY_MISMATCH | One or more accommodations do not belong to this day |
| 400 | DAY_ITINERARY_MISMATCH | Day does not belong to this itinerary |
| 404 | ITINERARY_DAY_NOT_FOUND | Itinerary day not found |
| 500 | ACCOMMODATION_DELETE_FAILED | Internal server error |

---

## Data Models

### ItineraryDayAccommodationDTO (Response)

| Field | Type | Description |
|-------|------|-------------|
| id | String | Obfuscated accommodation entry ID |
| itineraryDayId | String | Obfuscated day ID |
| accommodationId | String | Obfuscated accommodation ID |
| accommodationName | String | Name of the accommodation |
| accommodationSlug | String | URL-friendly slug of the accommodation |
| roomTypeId | String | Obfuscated room type ID |
| roomTypeName | String | Name of the room type (e.g., "Double Room") |
| roomStandardId | String | Obfuscated room standard ID |
| roomStandardName | String | Name of the room standard (e.g., "Deluxe") |
| boardTypeId | String | Obfuscated board type ID |
| boardTypeName | String | Name of the board type (e.g., "Full Board") |
| roomCount | Integer | Number of rooms booked |
| isAlternative | Boolean | Whether this is a backup/alternative option |
| notes | String | Additional notes |
| createdAt | DateTime | Creation timestamp |
| updatedAt | DateTime | Last update timestamp |

---

## Business Rules

1. **Mandatory Room Configuration:** All room configuration fields (roomTypeId, roomStandardId, boardTypeId) are required for complete pricing information.

2. **Primary vs Alternative Accommodations:**
   - Primary accommodations (`isAlternative: false`) are the main lodging options
   - Alternative accommodations (`isAlternative: true`) serve as backup options
   - Pax capacity validation only applies to primary accommodations

3. **Pax Capacity Validation:**
   - When creating or updating primary accommodations, the system validates that:
   - Total capacity = sum of (roomCount × maxOccupancy) for all primary accommodations
   - Total capacity must be >= total pax count for the itinerary
   - Validation is skipped if no pax is configured or room types don't have maxOccupancy set

4. **Immutable Core Configuration:** The accommodation, room type, room standard, and board type cannot be changed after creation. To modify these, delete and recreate the accommodation entry.

5. **Notes Field:** Can be set to `null` to clear existing notes during update.
