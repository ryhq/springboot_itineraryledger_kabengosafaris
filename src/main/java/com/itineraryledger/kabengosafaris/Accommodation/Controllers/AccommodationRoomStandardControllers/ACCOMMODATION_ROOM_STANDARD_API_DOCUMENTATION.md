# Accommodation Room Standard API Documentation

This document provides detailed information about the Accommodation Room Standard API endpoints, including request/response formats and examples.

## Base URL
```
/api/accommodation-room-standards
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION_ROOM_STANDARD` - Create room standards
- `PERM_READ_ACCOMMODATION_ROOM_STANDARD` - Read room standards
- `PERM_UPDATE_ACCOMMODATION_ROOM_STANDARD` - Update room standards
- `PERM_DELETE_ACCOMMODATION_ROOM_STANDARD` - Delete room standards

---

## Endpoints

### 1. Create Accommodation Room Standard

**POST** `/api/accommodation-room-standards`

Creates a new room standard (room category) for an accommodation.

**Required Permission:** `PERM_CREATE_ACCOMMODATION_ROOM_STANDARD`

**Request Body:**
```json
{
  "accommodationId": "encoded_accommodation_id",
  "name": "Deluxe Garden Room",
  "description": "Spacious room with garden views and premium amenities",
  "maxOccupancy": 3,
  "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker",
  "viewType": "Garden View",
  "floorLevel": "Ground Floor",
  "isActive": true
}
```

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Accommodation room standard created successfully",
  "data": {
    "id": "encoded_room_standard_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Deluxe Garden Room",
    "description": "Spacious room with garden views and premium amenities",
    "maxOccupancy": 3,
    "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker",
    "viewType": "Garden View",
    "floorLevel": "Ground Floor",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID or duplicate room standard name
- `404 Not Found` - Accommodation not found
- `500 Internal Server Error` - Server error

---

### 2. Update Accommodation Room Standard

**PUT** `/api/accommodation-room-standards/{idObfuscated}`

Updates an existing room standard.

**Required Permission:** `PERM_UPDATE_ACCOMMODATION_ROOM_STANDARD`

**Path Parameters:**
- `idObfuscated` - Encoded room standard ID

**Request Body:**
```json
{
  "name": "Deluxe Ocean View Suite",
  "description": "Luxurious suite with panoramic ocean views",
  "maxOccupancy": 4,
  "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker, Jacuzzi",
  "viewType": "Ocean View",
  "floorLevel": "Second Floor",
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodation room standard updated successfully",
  "data": {
    "id": "encoded_room_standard_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Deluxe Ocean View Suite",
    "description": "Luxurious suite with panoramic ocean views",
    "maxOccupancy": 4,
    "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker, Jacuzzi",
    "viewType": "Ocean View",
    "floorLevel": "Second Floor",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:30:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid room standard ID or duplicate name
- `404 Not Found` - Room standard not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Accommodation Room Standards

**DELETE** `/api/accommodation-room-standards`

Deletes one or more room standards by their IDs.

**Required Permission:** `PERM_DELETE_ACCOMMODATION_ROOM_STANDARD`

**Request Body:**
```json
[
  "encoded_room_standard_id_1",
  "encoded_room_standard_id_2"
]
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "2 room standard(s) deleted successfully",
  "data": null
}
```

**Error Responses:**
- `400 Bad Request` - No IDs provided
- `500 Internal Server Error` - Server error

---

### 4. Get Accommodation Room Standard by ID

**GET** `/api/accommodation-room-standards/{idObfuscated}`

Retrieves a single room standard by its ID.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_STANDARD`

**Path Parameters:**
- `idObfuscated` - Encoded room standard ID

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Room standard retrieved successfully",
  "data": {
    "id": "encoded_room_standard_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Deluxe Garden Room",
    "description": "Spacious room with garden views and premium amenities",
    "maxOccupancy": 3,
    "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker",
    "viewType": "Garden View",
    "floorLevel": "Ground Floor",
    "isActive": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid room standard ID
- `404 Not Found` - Room standard not found
- `500 Internal Server Error` - Server error

---

### 5. Get All Accommodation Room Standards

**GET** `/api/accommodation-room-standards`

Retrieves all room standards with optional filters and pagination. Accommodation ID is optional.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_STANDARD`

**Query Parameters:**
- `accommodationId` (optional) - Filter by accommodation ID
- `name` (optional) - Filter by name (partial match)
- `minOccupancy` (optional) - Filter by minimum occupancy
- `maxOccupancy` (optional) - Filter by maximum occupancy
- `viewType` (optional) - Filter by view type (partial match)
- `isActive` (optional) - Filter by active status (true/false)
- `keyword` (optional) - Search keyword across multiple fields
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDir` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-room-standards?viewType=Garden&isActive=true&page=0&size=10&sortDir=desc
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Room standards retrieved successfully",
  "data": {
    "roomStandards": [
      {
        "id": "encoded_room_standard_id",
        "accommodationId": "encoded_accommodation_id",
        "accommodationName": "Serengeti Safari Lodge",
        "name": "Deluxe Garden Room",
        "description": "Spacious room with garden views and premium amenities",
        "maxOccupancy": 3,
        "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker",
        "viewType": "Garden View",
        "floorLevel": "Ground Floor",
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

### 6. Get Room Standards for Specific Accommodation

**GET** `/api/accommodation-room-standards/accommodation/{accommodationId}`

Retrieves all room standards for a specific accommodation. Accommodation ID is required.

**Required Permission:** `PERM_READ_ACCOMMODATION_ROOM_STANDARD`

**Path Parameters:**
- `accommodationId` - Required accommodation ID

**Query Parameters:**
- `name` (optional) - Filter by name (partial match)
- `minOccupancy` (optional) - Filter by minimum occupancy
- `maxOccupancy` (optional) - Filter by maximum occupancy
- `viewType` (optional) - Filter by view type (partial match)
- `isActive` (optional) - Filter by active status (true/false)
- `keyword` (optional) - Search keyword
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDir` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-room-standards/accommodation/encoded_accommodation_id?isActive=true&page=0&size=10
```

**Success Response (200 OK):**
Same structure as "Get All Accommodation Room Standards" response.

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID
- `500 Internal Server Error` - Server error

---

## Data Models

### CreateAccommodationRoomStandardDTO
```json
{
  "accommodationId": "string (required)",
  "name": "string (required)",
  "description": "string (optional)",
  "maxOccupancy": "integer (optional)",
  "amenities": "string (optional)",
  "viewType": "string (optional)",
  "floorLevel": "string (optional)",
  "isActive": "boolean (optional, default: true)"
}
```

### UpdateAccommodationRoomStandardDTO
```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "maxOccupancy": "integer (optional)",
  "amenities": "string (optional)",
  "viewType": "string (optional)",
  "floorLevel": "string (optional)",
  "isActive": "boolean (optional)"
}
```

### AccommodationRoomStandardDTO
```json
{
  "id": "string",
  "accommodationId": "string",
  "accommodationName": "string",
  "name": "string",
  "description": "string",
  "maxOccupancy": "integer",
  "amenities": "string",
  "viewType": "string",
  "floorLevel": "string",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## Common Room Standard Examples

### Standard Room
```json
{
  "name": "Standard Room",
  "description": "Comfortable room with essential amenities",
  "maxOccupancy": 2,
  "amenities": "WiFi, AC, TV, Safe",
  "viewType": "Garden View",
  "floorLevel": "Ground Floor",
  "isActive": true
}
```

### Deluxe Garden Room
```json
{
  "name": "Deluxe Garden Room",
  "description": "Spacious room with garden views and premium amenities",
  "maxOccupancy": 3,
  "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker",
  "viewType": "Garden View",
  "floorLevel": "First Floor",
  "isActive": true
}
```

### Suite
```json
{
  "name": "Suite",
  "description": "Luxurious suite with separate living area",
  "maxOccupancy": 4,
  "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker, Living Room, Jacuzzi",
  "viewType": "Ocean View",
  "floorLevel": "Second Floor",
  "isActive": true
}
```

### Presidential Suite
```json
{
  "name": "Presidential Suite",
  "description": "Ultimate luxury suite with premium amenities and services",
  "maxOccupancy": 6,
  "amenities": "WiFi, AC, Minibar, Balcony, Safe, Coffee Maker, Living Room, Dining Room, Jacuzzi, Private Pool",
  "viewType": "Panoramic Ocean View",
  "floorLevel": "Top Floor",
  "isActive": true
}
```

### Family Tent
```json
{
  "name": "Family Tent",
  "description": "Spacious safari tent perfect for families",
  "maxOccupancy": 5,
  "amenities": "WiFi, Fan, Private Bathroom, Safari Views",
  "viewType": "Wildlife View",
  "floorLevel": "Ground Level",
  "isActive": true
}
```

### Honeymoon Bungalow
```json
{
  "name": "Honeymoon Bungalow",
  "description": "Private bungalow with romantic ambiance",
  "maxOccupancy": 2,
  "amenities": "WiFi, AC, Minibar, Private Deck, Outdoor Shower, Four Poster Bed",
  "viewType": "Private Garden View",
  "floorLevel": "Ground Level",
  "isActive": true
}
```

---

## Notes

- All IDs in requests and responses are obfuscated for security
- Room standard names must be unique per accommodation
- Timestamps are in ISO 8601 format
- Pagination uses 0-indexed page numbers
- All text fields support UTF-8 characters for international use
