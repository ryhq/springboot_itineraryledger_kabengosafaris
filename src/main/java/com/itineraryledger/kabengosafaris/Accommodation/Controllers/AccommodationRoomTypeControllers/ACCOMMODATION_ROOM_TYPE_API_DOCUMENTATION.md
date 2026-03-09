# Accommodation Room Type API Documentation

This document provides detailed information about the Accommodation Room Type API endpoints, including request/response formats and examples.

## Base URL
```
/api/accommodation-room-types
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION_ROOM_TYPE` - Create room types
- `PERM_READ_ACCOMMODATION_ROOM_TYPE` - Read room types
- `PERM_UPDATE_ACCOMMODATION_ROOM_TYPE` - Update room types
- `PERM_DELETE_ACCOMMODATION_ROOM_TYPE` - Delete room types

---

## Endpoints

### 1. Create Accommodation Room Type

**POST** `/api/accommodation-room-types`

Creates a new room type for an accommodation.

**Required Permission:** `PERM_CREATE_ACCOMMODATION_ROOM_TYPE`

**Request Body:**
```json
{
  "accommodationId": "encoded_accommodation_id",
  "name": "Double Room",
  "bedConfiguration": "1 King Bed",
  "maxOccupancy": 2,
  "minOccupancy": 1,
  "description": "Comfortable room with a king-sized bed",
  "isActive": true
}
```

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Accommodation room type created successfully",
  "data": {
    "id": "encoded_room_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Double Room",
    "bedConfiguration": "1 King Bed",
    "maxOccupancy": 2,
    "minOccupancy": 1,
    "description": "Comfortable room with a king-sized bed",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID or duplicate room type name
- `404 Not Found` - Accommodation not found
- `500 Internal Server Error` - Server error

---

### 2. Update Accommodation Room Type

**PUT** `/api/accommodation-room-types/{id}`

Updates an existing room type.

**Required Permission:** `PERM_UPDATE_ACCOMMODATION_ROOM_TYPE`

**Path Parameters:**
- `id` - Encoded room type ID

**Request Body:**
```json
{
  "name": "Deluxe Double Room",
  "bedConfiguration": "1 King Bed with Premium Bedding",
  "maxOccupancy": 3,
  "minOccupancy": 1,
  "description": "Luxurious room with a king-sized bed and premium amenities",
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodation room type updated successfully",
  "data": {
    "id": "encoded_room_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Deluxe Double Room",
    "bedConfiguration": "1 King Bed with Premium Bedding",
    "maxOccupancy": 3,
    "minOccupancy": 1,
    "description": "Luxurious room with a king-sized bed and premium amenities",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:30:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid room type ID or duplicate name
- `404 Not Found` - Room type not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Accommodation Room Types

**DELETE** `/api/accommodation-room-types`

Deletes one or more room types by their IDs.

**Required Permission:** `PERM_DELETE_ACCOMMODATION_ROOM_TYPE`

**Request Body:**
```json
[
  "encoded_room_type_id_1",
  "encoded_room_type_id_2"
]
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "2 room type(s) deleted successfully",
  "data": null
}
```

**Error Responses:**
- `400 Bad Request` - No IDs provided
- `500 Internal Server Error` - Server error

---

### 4. Get Accommodation Room Type by ID

**GET** `/api/accommodation-room-types/{id}`

Retrieves a single room type by its ID.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_TYPE`

**Path Parameters:**
- `id` - Encoded room type ID

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Room type retrieved successfully",
  "data": {
    "id": "encoded_room_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Double Room",
    "bedConfiguration": "1 King Bed",
    "maxOccupancy": 2,
    "minOccupancy": 1,
    "description": "Comfortable room with a king-sized bed",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid room type ID
- `404 Not Found` - Room type not found
- `500 Internal Server Error` - Server error

---

### 5. Get All Accommodation Room Types

**GET** `/api/accommodation-room-types`

Retrieves all room types with optional filters and pagination. Accommodation ID is optional.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_TYPE`

**Query Parameters:**
- `accommodationId` (optional) - Filter by accommodation ID
- `name` (optional) - Filter by name (partial match)
- `minOccupancy` (optional) - Filter by minimum occupancy (greater than or equal to)
- `maxOccupancy` (optional) - Filter by maximum occupancy (less than or equal to)
- `isActive` (optional) - Filter by active status (true/false)
- `keyword` (optional) - Search keyword across multiple fields
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDirection` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-room-types?minOccupancy=2&isActive=true&page=0&size=10&sortDirection=desc
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Room types retrieved successfully",
  "data": {
    "roomTypes": [
      {
        "id": "encoded_room_type_id",
        "accommodationId": "encoded_accommodation_id",
        "accommodationName": "Serengeti Safari Lodge",
        "name": "Double Room",
        "bedConfiguration": "1 King Bed",
        "maxOccupancy": 2,
        "minOccupancy": 1,
        "description": "Comfortable room with a king-sized bed",
        "isActive": true,
        "createdAt": "2026-01-06T12:00:00",
        "updatedAt": "2026-01-06T12:00:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 1,
    "totalPages": 1
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID
- `500 Internal Server Error` - Server error

---

### 6. Get Room Types for Specific Accommodation

**GET** `/api/accommodation-room-types/accommodation/{accommodationId}`

Retrieves all room types for a specific accommodation. Accommodation ID is required.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_TYPE`

**Path Parameters:**
- `accommodationId` - Required accommodation ID

**Query Parameters:**
- `name` (optional) - Filter by name (partial match)
- `minOccupancy` (optional) - Filter by minimum occupancy (greater than or equal to)
- `maxOccupancy` (optional) - Filter by maximum occupancy (less than or equal to)
- `isActive` (optional) - Filter by active status (true/false)
- `keyword` (optional) - Search keyword
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDirection` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-room-types/accommodation/encoded_accommodation_id?isActive=true&page=0&size=10
```

**Success Response (200 OK):**
Same structure as "Get All Accommodation Room Types" response.

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID
- `500 Internal Server Error` - Server error

---

### 7. Get Unique Room Types by Name

**GET** `/api/accommodation-room-types/unique`

Retrieves unique room types based on name. Returns one room type per unique name, sorted alphabetically. This is useful for dropdowns where users can select existing room type names to apply to new accommodations.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_TYPE`

**Use Case:**
When creating a room type for a new accommodation, users can select from existing room type names (e.g., "Single Room", "Double Room", "Twin Room") instead of manually typing the name. If multiple room types have the same name, only one is returned (the one with the lowest ID), sorted alphabetically by name.

**Uniqueness Criteria:**
Room types are considered unique based on the `name` field only.

**Example Request:**
```
GET /api/accommodation-room-types/unique
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Unique room types retrieved successfully",
  "data": [
    {
      "id": "encoded_room_type_id_1",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Serengeti Safari Lodge",
      "name": "Double Room",
      "bedConfiguration": "1 Queen Bed",
      "maxOccupancy": 2,
      "minOccupancy": 1,
      "description": "Comfortable double room with queen-sized bed",
      "isActive": true,
      "createdAt": "2026-01-06T12:00:00",
      "updatedAt": "2026-01-06T12:00:00"
    },
    {
      "id": "encoded_room_type_id_2",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Ngorongoro Crater Lodge",
      "name": "Single Room",
      "bedConfiguration": "1 Single Bed",
      "maxOccupancy": 1,
      "minOccupancy": 1,
      "description": "Cozy single room perfect for solo travelers",
      "isActive": true,
      "createdAt": "2026-01-05T10:00:00",
      "updatedAt": "2026-01-05T10:00:00"
    },
    {
      "id": "encoded_room_type_id_3",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Tarangire Tented Camp",
      "name": "Twin Room",
      "bedConfiguration": "2 Single Beds",
      "maxOccupancy": 2,
      "minOccupancy": 1,
      "description": "Twin room with two separate single beds",
      "isActive": true,
      "createdAt": "2026-01-04T15:00:00",
      "updatedAt": "2026-01-04T15:00:00"
    }
  ]
}
```

**Response Details:**
- Only active room types (`isActive = true`) are included
- Results are sorted alphabetically by name
- Returns a simple array (not paginated) for easy dropdown population
- Each unique name appears only once

**Error Responses:**
- `500 Internal Server Error` - Server error

---

## Data Models

### CreateAccommodationRoomTypeDTO
```json
{
  "accommodationId": "string (required)",
  "name": "string (required)",
  "bedConfiguration": "string (optional)",
  "maxOccupancy": "integer (optional)",
  "minOccupancy": "integer (optional, default: 1)",
  "description": "string (optional)",
  "isActive": "boolean (optional, default: true)"
}
```

### UpdateAccommodationRoomTypeDTO
```json
{
  "name": "string (optional)",
  "bedConfiguration": "string (optional)",
  "maxOccupancy": "integer (optional)",
  "minOccupancy": "integer (optional)",
  "description": "string (optional)",
  "isActive": "boolean (optional)"
}
```

### AccommodationRoomTypeDTO
```json
{
  "id": "string",
  "accommodationId": "string",
  "accommodationName": "string",
  "name": "string",
  "bedConfiguration": "string",
  "maxOccupancy": "integer",
  "minOccupancy": "integer",
  "description": "string",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## Common Room Type Examples

### Single Room
```json
{
  "name": "Single Room",
  "bedConfiguration": "1 Single Bed",
  "maxOccupancy": 1,
  "minOccupancy": 1,
  "description": "Compact room perfect for solo travelers",
  "isActive": true
}
```

### Double Room
```json
{
  "name": "Double Room",
  "bedConfiguration": "1 King Bed",
  "maxOccupancy": 2,
  "minOccupancy": 1,
  "description": "Comfortable room with a king-sized bed",
  "isActive": true
}
```

### Twin Room
```json
{
  "name": "Twin Room",
  "bedConfiguration": "2 Single Beds",
  "maxOccupancy": 2,
  "minOccupancy": 1,
  "description": "Room with two separate single beds",
  "isActive": true
}
```

### Triple Room
```json
{
  "name": "Triple Room",
  "bedConfiguration": "1 King Bed + 1 Single Bed",
  "maxOccupancy": 3,
  "minOccupancy": 2,
  "description": "Spacious room accommodating up to three guests",
  "isActive": true
}
```

### Family Room
```json
{
  "name": "Family Room",
  "bedConfiguration": "1 King Bed + 2 Single Beds",
  "maxOccupancy": 4,
  "minOccupancy": 3,
  "description": "Large room perfect for families with children",
  "isActive": true
}
```

### Suite
```json
{
  "name": "Suite",
  "bedConfiguration": "1 King Bed in Master Bedroom + 2 Single Beds in Second Bedroom",
  "maxOccupancy": 5,
  "minOccupancy": 2,
  "description": "Multi-room suite with separate sleeping areas",
  "isActive": true
}
```

### Studio
```json
{
  "name": "Studio",
  "bedConfiguration": "1 Queen Bed + Sofa Bed",
  "maxOccupancy": 3,
  "minOccupancy": 1,
  "description": "Open-plan room with combined living and sleeping area",
  "isActive": true
}
```

### Dormitory
```json
{
  "name": "Dormitory",
  "bedConfiguration": "6 Bunk Beds (12 beds total)",
  "maxOccupancy": 12,
  "minOccupancy": 1,
  "description": "Shared room with multiple beds for budget travelers",
  "isActive": true
}
```

---

## Notes

- All IDs in requests and responses are obfuscated for security
- Room type names must be unique per accommodation
- The `minOccupancy` defaults to 1 if not specified
- Timestamps are in ISO 8601 format
- Pagination uses 0-indexed page numbers
- All text fields support UTF-8 characters for international use
- Room type defines the physical bed configuration (single, double, twin, etc.)
- This is different from Room Standard which defines the quality/category level
