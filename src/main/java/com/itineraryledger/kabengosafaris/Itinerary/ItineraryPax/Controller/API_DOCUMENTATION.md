# Itinerary Pax Management API Documentation

## Overview

The Itinerary Pax Management API provides endpoints for managing passenger categories within safari itineraries. Passenger categories define how many travelers of different nation and age groups will participate in the safari.

Each pax entry combines:
- **Nation Category**: Resident, Non-Resident, East African, etc.
- **Age Category**: Adult, Child, Infant, etc.
- **Count**: Number of passengers in this category

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Itinerary Pax API](#itinerary-pax-api)
   - [Upsert Pax](#1-upsert-pax)
   - [Get All Pax](#2-get-all-pax)
   - [Delete Pax](#3-delete-pax)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)

---

## Itinerary Pax API

Base URL: `/api/itineraries/{itineraryId}/pax`

### 1. Upsert Pax

Creates new pax entries or updates existing ones based on nation/age category combination. This is an idempotent operation - if a pax entry with the same nation and age category already exists for the itinerary, it will be updated; otherwise, a new entry is created.

**Endpoint:** `POST /api/itineraries/{itineraryId}/pax`

**Permission Required:** `PERM_UPDATE_ITINERARY_PAX`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
[
  {
    "nationCategoryId": "nat123xyz",
    "ageCategoryId": "age456abc",
    "count": 2,
    "notes": "Couple traveling together"
  },
  {
    "nationCategoryId": "nat123xyz",
    "ageCategoryId": "age789def",
    "count": 1,
    "notes": "Child age 8"
  },
  {
    "nationCategoryId": "nat789ghi",
    "ageCategoryId": "age456abc",
    "count": 2,
    "notes": "Local residents joining"
  }
]
```

**Request Body Fields (per item):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nationCategoryId` | string | Yes | Obfuscated nation category ID |
| `ageCategoryId` | string | Yes | Obfuscated age category ID |
| `count` | integer | Yes | Number of passengers (min: 1) |
| `notes` | string | No | Optional notes for this pax entry |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 created, 1 updated",
  "data": [
    {
      "id": "pax123xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "Couple traveling together",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax124xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age789def",
      "ageCategoryName": "Child",
      "count": 1,
      "notes": "Child age 8",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax125xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat789ghi",
      "nationCategoryName": "Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "Local residents joining",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
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
- This is a **bulk operation** - multiple pax entries can be created/updated in a single request
- The combination of `nationCategoryId` + `ageCategoryId` is unique per itinerary
- If a nation or age category is not found, that entry is silently skipped (logged as warning)
- The response message indicates how many entries were created vs updated

---

### 2. Get All Pax

Retrieves all passenger category entries for an itinerary.

**Endpoint:** `GET /api/itineraries/{itineraryId}/pax`

**Permission Required:** `PERM_READ_ITINERARY_PAX`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved 3 pax categories (total: 5 passengers)",
  "data": [
    {
      "id": "pax123xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "Couple traveling together",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax124xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age789def",
      "ageCategoryName": "Child",
      "count": 1,
      "notes": "Child age 8",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax125xyz",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat789ghi",
      "nationCategoryName": "Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "Local residents joining",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "timestamp": "2024-01-15T11:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid itinerary ID
- **404 Not Found** - Itinerary not found

**Notes:**
- The response message includes both the number of pax categories and the total passenger count
- Total passengers is the sum of all `count` values

---

### 3. Delete Pax

Deletes multiple passenger category entries by their IDs.

**Endpoint:** `DELETE /api/itineraries/{itineraryId}/pax`

**Permission Required:** `PERM_DELETE_ITINERARY_PAX`

**Path Parameters:**
- `itineraryId` (string, required): Obfuscated itinerary ID

**Request Body:**
```json
["pax123xyz", "pax124xyz"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 pax entry(ies) deleted successfully",
  "data": null,
  "timestamp": "2024-01-15T12:00:00"
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
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

- **500 Internal Server Error** - Deletion failed
  ```json
  {
    "success": false,
    "statusCode": 500,
    "message": "Failed to delete itinerary pax",
    "errorCode": "ITINERARY_PAX_DELETE_FAILED",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

**Notes:**
- Only pax entries belonging to the specified itinerary will be deleted
- Pax entries that don't belong to the itinerary are silently skipped
- Invalid pax IDs are silently skipped (logged as warning)
- The response indicates how many entries were actually deleted

---

## Data Models

### ItineraryPaxDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated pax entry ID |
| `itineraryId` | string | Obfuscated itinerary ID |
| `nationCategoryId` | string | Obfuscated nation category ID |
| `nationCategoryName` | string | Name of the nation category (e.g., "Resident", "Non-Resident") |
| `ageCategoryId` | string | Obfuscated age category ID |
| `ageCategoryName` | string | Name of the age category (e.g., "Adult", "Child") |
| `count` | integer | Number of passengers in this category |
| `notes` | string | Optional notes |
| `createdAt` | datetime | Creation timestamp |
| `updatedAt` | datetime | Last update timestamp |

### UpsertItineraryPaxDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nationCategoryId` | string | Yes | Obfuscated nation category ID |
| `ageCategoryId` | string | Yes | Obfuscated age category ID |
| `count` | integer | Yes | Number of passengers (min: 1) |
| `notes` | string | No | Optional notes |

### Common Nation Categories

| Category | Description |
|----------|-------------|
| Resident | Citizens or permanent residents of the country |
| Non-Resident | Foreign visitors |
| East African | Citizens of East African Community countries |

### Common Age Categories

| Category | Typical Age Range |
|----------|-------------------|
| Adult | 18+ years |
| Child | 5-17 years |
| Infant | 0-4 years |
| Senior | 60+ years |

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_ITINERARY_ID` | The provided itinerary ID is invalid or malformed |
| `ITINERARY_NOT_FOUND` | Itinerary with the specified ID does not exist |
| `ITINERARY_PAX_UPSERT_FAILED` | Failed to upsert pax entries due to server error |
| `ITINERARY_PAX_FETCH_FAILED` | Failed to fetch pax entries due to server error |
| `ITINERARY_PAX_DELETE_FAILED` | Failed to delete pax entries due to server error |

---

## Examples

### Example 1: Setting Up Pax for a Family Safari

A family of 4 (2 adults, 2 children) from abroad:

**Request:**
```http
POST /api/itineraries/itn456abc/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_adult_456",
    "count": 2,
    "notes": "Parents"
  },
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_child_789",
    "count": 2,
    "notes": "Children ages 10 and 12"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 created, 0 updated",
  "data": [
    {
      "id": "pax001",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_adult_456",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "Parents",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax002",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_child_789",
      "ageCategoryName": "Child",
      "count": 2,
      "notes": "Children ages 10 and 12",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### Example 2: Mixed Group with Residents and Non-Residents

A group tour with both local and international travelers:

**Request:**
```http
POST /api/itineraries/itn789def/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_res_001",
    "ageCategoryId": "age_adult_456",
    "count": 3,
    "notes": "Local participants"
  },
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_adult_456",
    "count": 5,
    "notes": "International participants"
  },
  {
    "nationCategoryId": "nat_ea_002",
    "ageCategoryId": "age_adult_456",
    "count": 2,
    "notes": "East African community members"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 created, 0 updated",
  "data": [
    {
      "id": "pax101",
      "nationCategoryName": "Resident",
      "ageCategoryName": "Adult",
      "count": 3,
      "notes": "Local participants"
    },
    {
      "id": "pax102",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Adult",
      "count": 5,
      "notes": "International participants"
    },
    {
      "id": "pax103",
      "nationCategoryName": "East African",
      "ageCategoryName": "Adult",
      "count": 2,
      "notes": "East African community members"
    }
  ]
}
```

### Example 3: Updating Pax Count

If the number of children changes from 2 to 3, simply upsert with the same nation/age category:

**Request:**
```http
POST /api/itineraries/itn456abc/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_child_789",
    "count": 3,
    "notes": "Updated: Children ages 8, 10, and 12"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "0 created, 1 updated",
  "data": [
    {
      "id": "pax002",
      "itineraryId": "itn456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_child_789",
      "ageCategoryName": "Child",
      "count": 3,
      "notes": "Updated: Children ages 8, 10, and 12",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T11:00:00"
    }
  ]
}
```

### Example 4: Retrieving Total Pax Summary

**Request:**
```http
GET /api/itineraries/itn456abc/pax
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved 2 pax categories (total: 5 passengers)",
  "data": [
    {
      "id": "pax001",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Adult",
      "count": 2
    },
    {
      "id": "pax002",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Child",
      "count": 3
    }
  ]
}
```

---

## Best Practices

### 1. Pax Management

- **Use Bulk Upsert**: Always use the bulk endpoint even for single entries for consistency
- **Update via Upsert**: To update counts, simply upsert with the same nation/age category combination
- **Validate Categories**: Ensure nation and age category IDs exist before upserting

### 2. Pricing Considerations

- Different nation categories typically have different park entry fees
- Different age categories may have different pricing (e.g., children may be free or discounted)
- Pax data is essential for accurate cost calculations

### 3. Itinerary Completeness

- An itinerary typically needs at least one pax entry to be considered complete for publishing
- Verify pax entries reflect the actual group composition

### 4. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Invalid category references are silently skipped - check response data for actual upserted entries

---

## Related APIs

- **Itinerary API:** `/api/itineraries` - Main itinerary management
- **Pax Nation Category API:** `/api/pax-nation-categories` - Nation category management
- **Pax Age Category API:** `/api/pax-age-categories` - Age category management

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Bulk upsert operation for pax entries
- Get all pax entries for an itinerary
- Bulk delete pax entries

---

## Support

For technical support or questions about the Itinerary Pax Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
