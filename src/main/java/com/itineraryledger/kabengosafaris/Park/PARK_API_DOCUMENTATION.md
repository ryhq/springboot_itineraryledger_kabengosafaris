# Park API Documentation

## Overview
The Park API provides endpoints for managing Tanzania National Parks, Wildlife Reserves, and other protected areas. All endpoints require proper authentication and permissions.

**Base URL**: `/api/parks`

**Required Permissions**:
- `PERM_CREATE_PARK` - Create new parks
- `PERM_READ_PARK` - View parks
- `PERM_UPDATE_PARK` - Update existing parks
- `PERM_DELETE_PARK` - Delete parks

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Create Park](#1-create-park)
   - [Update Park](#2-update-park)
   - [Delete Parks](#3-delete-parks)
   - [Get Park by ID](#4-get-park-by-id)
   - [Get Park by Slug](#5-get-park-by-slug)
   - [Get All Parks](#6-get-all-parks-with-filters)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)

---

## Data Models

### ParkType Enum
```json
{
  "allowedValues": [
    "NATIONAL_PARK",
    "WILDLIFE_RESERVE",
    "GAME_RESERVE",
    "CONSERVATION_AREA",
    "MARINE_PARK",
    "FOREST_RESERVE",
    "NATURE_RESERVE"
  ]
}
```

### Park Object (ParkDTO)
```json
{
  "idObfuscated": "string (obfuscated ID for security)",
  "name": "string",
  "slug": "string (URL-friendly identifier)",
  "parkType": "ParkType enum",
  "region": "string",
  "district": "string",
  "location": "string",
  "latitude": "number (decimal)",
  "longitude": "number (decimal)",
  "elevation": "string",
  "size": "string",
  "shortDescription": "string (brief overview)",
  "fullDescription": "string (detailed description)",
  "history": "string",
  "ecosystem": "string",
  "wildlife": "string",
  "vegetation": "string",
  "primaryImage": "string (URL or path)",
  "bestTimeToVisit": "string",
  "openingHours": "string",
  "accessInformation": "string",
  "tags": "string",
  "isActive": "boolean",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Park

**Endpoint**: `POST /api/parks`

**Permission**: `PERM_CREATE_PARK`

**Description**: Creates a new park in the system.

#### Request Body (CreateParkDTO)
```json
{
  "name": "Serengeti National Park",
  "slug": "serengeti-national-park",
  "parkType": "NATIONAL_PARK",
  "region": "Mara",
  "district": "Serengeti",
  "location": "Northern Tanzania",
  "latitude": -2.3333,
  "longitude": 34.8333,
  "elevation": "920-1,850 meters",
  "size": "14,763 km²",
  "shortDescription": "One of the most famous wildlife sanctuaries in the world.",
  "fullDescription": "Serengeti National Park is a vast ecosystem in north-central Tanzania...",
  "history": "Established in 1951...",
  "ecosystem": "Grassland savanna and woodlands",
  "wildlife": "Home to the Big Five and the Great Migration",
  "vegetation": "Grassland plains, acacia woodlands, riverine forests",
  "primaryImage": "/images/parks/serengeti.jpg",
  "bestTimeToVisit": "June to October (dry season)",
  "openingHours": "6:00 AM - 6:00 PM daily",
  "accessInformation": "Accessible by road from Arusha or by air to Seronera airstrip",
  "tags": "Big Five, Great Migration, Safari, Wildlife",
  "isActive": true
}
```

**Required Fields**:
- `name` (string, not blank)
- `parkType` (ParkType enum, not null)
- `shortDescription` (string, not blank)
- `fullDescription` (string, not blank)

**Optional Fields**: All other fields are optional. If `slug` is not provided, it will be auto-generated from the `name`.

#### Success Response (201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Park created successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Serengeti National Park",
    "slug": "serengeti-national-park",
    "parkType": "NATIONAL_PARK",
    ...
    "createdAt": "2025-12-30T10:30:00",
    "updatedAt": "2025-12-30T10:30:00"
  },
  "timestamp": "2025-12-30T10:30:00"
}
```

#### Error Responses
**400 Bad Request** - Validation errors or duplicate name/slug
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Park with name 'Serengeti National Park' already exists",
  "errorCode": "PARK_NAME_EXISTS",
  "timestamp": "2025-12-30T10:30:00"
}
```

---

### 2. Update Park

**Endpoint**: `PUT /api/parks/{idObfuscated}`

**Permission**: `PERM_UPDATE_PARK`

**Description**: Updates an existing park. Only provided fields will be updated (partial update).

#### Path Parameters
- `idObfuscated` (required): The obfuscated park ID

#### Request Body (UpdateParkDTO)
```json
{
  "name": "Serengeti National Park - Updated",
  "shortDescription": "Updated description",
  "isActive": false
}
```

**Note**: All fields are optional. Only include fields you want to update.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Park updated successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Serengeti National Park - Updated",
    ...
    "updatedAt": "2025-12-30T11:00:00"
  },
  "timestamp": "2025-12-30T11:00:00"
}
```

#### Error Responses
**404 Not Found** - Park doesn't exist
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Park not found with ID: xY9Kp2Lm",
  "errorCode": "PARK_NOT_FOUND",
  "timestamp": "2025-12-30T11:00:00"
}
```

**400 Bad Request** - Name/slug already exists
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Park with name 'Ngorongoro' already exists",
  "errorCode": "PARK_NAME_EXISTS",
  "timestamp": "2025-12-30T11:00:00"
}
```

---

### 3. Delete Parks

**Endpoint**: `DELETE /api/parks`

**Permission**: `PERM_DELETE_PARK`

**Description**: Deletes one or more parks by their obfuscated IDs.

#### Request Body
```json
["xY9Kp2Lm", "aB3Cd4Ef", "pQ7Rs8Tv"]
```

**Note**: Send an array of obfuscated park IDs to delete.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully deleted 3 park(s)",
  "data": {
    "deletedCount": 3,
    "failedIds": []
  },
  "timestamp": "2025-12-30T12:00:00"
}
```

#### Partial Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deleted 2 park(s), 1 failed",
  "data": {
    "deletedCount": 2,
    "failedIds": ["invalidId123"]
  },
  "timestamp": "2025-12-30T12:00:00"
}
```

#### Error Responses
**400 Bad Request** - Empty list
```json
{
  "success": false,
  "statusCode": 400,
  "message": "No park IDs provided for deletion",
  "errorCode": "EMPTY_DELETE_LIST",
  "timestamp": "2025-12-30T12:00:00"
}
```

---

### 4. Get Park by ID

**Endpoint**: `GET /api/parks/{idObfuscated}`

**Permission**: `PERM_READ_PARK`

**Description**: Retrieves a single park by its obfuscated ID.

#### Path Parameters
- `idObfuscated` (required): The obfuscated park ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Park retrieved successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Serengeti National Park",
    "slug": "serengeti-national-park",
    "parkType": "NATIONAL_PARK",
    ...
    "createdAt": "2025-12-30T10:30:00",
    "updatedAt": "2025-12-30T11:00:00"
  },
  "timestamp": "2025-12-30T13:00:00"
}
```

#### Error Responses
**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Park not found with ID: xY9Kp2Lm",
  "errorCode": "PARK_NOT_FOUND",
  "timestamp": "2025-12-30T13:00:00"
}
```

---

### 5. Get Park by Slug

**Endpoint**: `GET /api/parks/slug/{slug}`

**Permission**: `PERM_READ_PARK`

**Description**: Retrieves a single park by its URL-friendly slug.

#### Path Parameters
- `slug` (required): The park slug (e.g., "serengeti-national-park")

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Park retrieved successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Serengeti National Park",
    "slug": "serengeti-national-park",
    ...
  },
  "timestamp": "2025-12-30T13:30:00"
}
```

#### Error Responses
**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Park not found with slug: serengeti-national-park",
  "errorCode": "PARK_NOT_FOUND",
  "timestamp": "2025-12-30T13:30:00"
}
```

---

### 6. Get All Parks (with Filters)

**Endpoint**: `GET /api/parks`

**Permission**: `PERM_READ_PARK`

**Description**: Retrieves a paginated list of parks with optional filtering and sorting.

#### Query Parameters

**Filtering**:
- `name` (string, optional): Filter by name (partial match, case-insensitive)
- `slug` (string, optional): Filter by slug (partial match, case-insensitive)
- `parkType` (ParkType, optional): Filter by park type (exact match)
- `region` (string, optional): Filter by region (partial match, case-insensitive)
- `district` (string, optional): Filter by district (partial match, case-insensitive)
- `location` (string, optional): Filter by location (partial match, case-insensitive)
- `isActive` (boolean, optional): Filter by active status
- `keyword` (string, optional): Search across name, region, district, location, and short description

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortBy` (string, optional, default: "createdAt"): Field to sort by
  - Allowed values: `name`, `slug`, `parkType`, `region`, `district`, `size`, `isActive`, `createdAt`, `updatedAt`
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`

#### Example Requests

**Basic request (all parks, default pagination)**:
```
GET /api/parks
```

**Filter by park type**:
```
GET /api/parks?parkType=NATIONAL_PARK
```

**Filter by region with pagination**:
```
GET /api/parks?region=Mara&page=0&size=20
```

**Search by keyword**:
```
GET /api/parks?keyword=wildlife&sortBy=name&sortDirection=asc
```

**Complex filter**:
```
GET /api/parks?parkType=NATIONAL_PARK&region=Northern&isActive=true&page=0&size=15&sortBy=name&sortDirection=asc
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Parks retrieved successfully",
  "data": {
    "parks": [
      {
        "idObfuscated": "xY9Kp2Lm",
        "name": "Serengeti National Park",
        "slug": "serengeti-national-park",
        "parkType": "NATIONAL_PARK",
        ...
      },
      {
        "idObfuscated": "aB3Cd4Ef",
        "name": "Ngorongoro Conservation Area",
        "slug": "ngorongoro-conservation-area",
        "parkType": "CONSERVATION_AREA",
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2,
    "pageSize": 10
  },
  "timestamp": "2025-12-30T14:00:00"
}
```

#### Response with No Results (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Parks retrieved successfully",
  "data": {
    "parks": [],
    "currentPage": 0,
    "totalItems": 0,
    "totalPages": 0,
    "pageSize": 10
  },
  "timestamp": "2025-12-30T14:00:00"
}
```

---

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2025-12-30T10:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2025-12-30T10:00:00"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `PARK_NOT_FOUND` | 404 | Park with specified ID or slug not found |
| `PARK_NAME_EXISTS` | 400 | Park with the same name already exists |
| `PARK_SLUG_EXISTS` | 400 | Park with the same slug already exists |
| `EMPTY_DELETE_LIST` | 400 | No park IDs provided for deletion |
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields, invalid format) |
| `INVALID_OBFUSCATED_ID` | 400 | The provided obfuscated ID is invalid |
| `PERMISSION_DENIED` | 403 | User lacks required permission for the operation |
| `UNAUTHORIZED` | 401 | User is not authenticated |

---

## Authentication & Authorization

All endpoints require:
1. **Authentication**: Valid JWT token in the `Authorization` header
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Authorization**: User must have the appropriate permission:
   - Create operations: `PERM_CREATE_PARK`
   - Read operations: `PERM_READ_PARK`
   - Update operations: `PERM_UPDATE_PARK`
   - Delete operations: `PERM_DELETE_PARK`

---

## Best Practices

1. **Use Slugs for Public URLs**: Use the slug-based endpoint (`/api/parks/slug/{slug}`) for public-facing URLs as they're more SEO-friendly.

2. **Use IDs for Management**: Use obfuscated IDs for administrative operations (update, delete).

3. **Leverage Keyword Search**: Use the `keyword` parameter for general search functionality across multiple fields.

4. **Implement Pagination**: Always use pagination for list endpoints to improve performance.

5. **Filter Inactive Parks**: For public-facing applications, filter by `isActive=true` to show only active parks.

6. **Cache Responses**: Consider caching park data as it changes infrequently.

7. **Handle Partial Updates**: When updating, only send fields that need to be changed to avoid overwriting data.

---

## Examples

### cURL Examples

**Create a park**:
```bash
curl -X POST http://localhost:8080/api/parks \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Serengeti National Park",
    "parkType": "NATIONAL_PARK",
    "region": "Mara",
    "shortDescription": "Famous wildlife sanctuary",
    "fullDescription": "Detailed description here...",
    "isActive": true
  }'
```

**Get all national parks**:
```bash
curl -X GET "http://localhost:8080/api/parks?parkType=NATIONAL_PARK&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Update a park**:
```bash
curl -X PUT http://localhost:8080/api/parks/xY9Kp2Lm \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "shortDescription": "Updated description",
    "isActive": false
  }'
```

**Delete parks**:
```bash
curl -X DELETE http://localhost:8080/api/parks \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '["xY9Kp2Lm", "aB3Cd4Ef"]'
```

**Get park by slug**:
```bash
curl -X GET http://localhost:8080/api/parks/slug/serengeti-national-park \
  -H "Authorization: Bearer <your-token>"
```

---

## Notes

- **ID Obfuscation**: All park IDs are obfuscated for security. Never expose internal database IDs.
- **Slug Generation**: If not provided during creation, slugs are automatically generated from the park name.
- **Timestamps**: All timestamps are in ISO 8601 format (UTC).
- **Validation**: Required fields must be provided during creation. Update operations validate only non-null fields.
- **Soft Delete**: The system may implement soft delete (setting `isActive=false`) rather than hard delete.

---

## Support

For issues or questions about the Park API, please contact the development team or refer to the main application documentation.
