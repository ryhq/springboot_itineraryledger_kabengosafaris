# Activity API Documentation

## Overview
The Activity API provides endpoints for managing tourism activities such as game drives, walking safaris, bird watching, mountain climbing, snorkeling, diving, etc. All endpoints require proper authentication and permissions.

**Base URL**: `/api/activities`

**Required Permissions**:
- `PERM_CREATE_ACTIVITY` - Create new activities
- `PERM_READ_ACTIVITY` - View activities
- `PERM_UPDATE_ACTIVITY` - Update existing activities
- `PERM_DELETE_ACTIVITY` - Delete activities

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Create Activity](#1-create-activity)
   - [Update Activity](#2-update-activity)
   - [Delete Activities](#3-delete-activities)
   - [Get Activity by ID](#4-get-activity-by-id)
   - [Get Activity by Slug](#5-get-activity-by-slug)
   - [Get All Activities](#6-get-all-activities-with-filters)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)

---

## Data Models

### ChargingBasis Enum
```json
{
  "allowedValues": [
    "PER_PERSON",
    "PER_VEHICLE",
    "PER_GROUP",
    "PER_DAY",
    "PER_HOUR",
    "PER_SESSION",
    "FLAT_RATE"
  ]
}
```

### Activity Object (ActivityDTO)
```json
{
  "idObfuscated": "string (obfuscated ID for security)",
  "name": "string",
  "slug": "string (URL-friendly identifier)",
  "hasTariff": "boolean",
  "isWebActive": "boolean",
  "chargingBasis": "ChargingBasis enum",
  "description": "string (brief overview)",
  "detailedDescription": "string (detailed description)",
  "minimumAge": "integer (minimum age requirement)",
  "maximumParticipants": "integer (maximum number of participants)",
  "equipmentRequired": "string",
  "seasonAvailability": "string",
  "primaryImage": "string (URL or path)",
  "tags": "string (comma-separated keywords)",
  "safetyInformation": "string",
  "isActive": "boolean",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Activity

**Endpoint**: `POST /api/activities`

**Permission**: `PERM_CREATE_ACTIVITY`

**Description**: Creates a new activity in the system.

#### Request Body (CreateActivityDTO)
```json
{
  "name": "Game Drive Safari",
  "slug": "game-drive-safari",
  "hasTariff": true,
  "isWebActive": true,
  "chargingBasis": "PER_PERSON",
  "description": "Experience the thrill of spotting wildlife in their natural habitat.",
  "detailedDescription": "A game drive safari is a guided tour in a specially designed 4x4 vehicle through national parks and reserves. Expert guides help you spot and learn about the Big Five and other wildlife species. Tours typically last 2-4 hours and are conducted during early morning or late afternoon when animals are most active.",
  "minimumAge": 5,
  "maximumParticipants": 6,
  "equipmentRequired": "Binoculars, camera, sunscreen, hat, water bottle",
  "seasonAvailability": "Year-round, best during dry season (June-October)",
  "primaryImage": "/images/activities/game-drive.jpg",
  "tags": "Safari, Wildlife, Big Five, Photography, Nature",
  "safetyInformation": "Stay inside the vehicle at all times. Follow guide instructions. Do not make sudden movements or loud noises.",
  "isActive": true
}
```

**Required Fields**:
- `name` (string, not blank)

**Optional Fields**: All other fields are optional. If `slug` is not provided, it will be auto-generated from the `name`.

#### Success Response (201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Activity created successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Game Drive Safari",
    "slug": "game-drive-safari",
    "hasTariff": true,
    "isWebActive": true,
    "chargingBasis": "PER_PERSON",
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
  "message": "Activity with name 'Game Drive Safari' already exists",
  "errorCode": "ACTIVITY_NAME_EXISTS",
  "timestamp": "2025-12-30T10:30:00"
}
```

---

### 2. Update Activity

**Endpoint**: `PUT /api/activities/{idObfuscated}`

**Permission**: `PERM_UPDATE_ACTIVITY`

**Description**: Updates an existing activity. Only provided fields will be updated (partial update).

#### Path Parameters
- `idObfuscated` (required): The obfuscated activity ID

#### Request Body (UpdateActivityDTO)
```json
{
  "description": "Updated description",
  "minimumAge": 8,
  "isActive": false
}
```

**Note**: All fields are optional. Only include fields you want to update.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activity updated successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Game Drive Safari",
    "description": "Updated description",
    "minimumAge": 8,
    ...
    "updatedAt": "2025-12-30T11:00:00"
  },
  "timestamp": "2025-12-30T11:00:00"
}
```

#### Error Responses
**404 Not Found** - Activity doesn't exist
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Activity not found",
  "errorCode": "ACTIVITY_NOT_FOUND",
  "timestamp": "2025-12-30T11:00:00"
}
```

**400 Bad Request** - Name/slug already exists
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Activity with name 'Walking Safari' already exists",
  "errorCode": "ACTIVITY_NAME_EXISTS",
  "timestamp": "2025-12-30T11:00:00"
}
```

---

### 3. Delete Activities

**Endpoint**: `DELETE /api/activities`

**Permission**: `PERM_DELETE_ACTIVITY`

**Description**: Deletes one or more activities by their obfuscated IDs.

#### Request Body
```json
["xY9Kp2Lm", "aB3Cd4Ef", "pQ7Rs8Tv"]
```

**Note**: Send an array of obfuscated activity IDs to delete.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 activity(ies) deleted successfully",
  "data": null,
  "timestamp": "2025-12-30T12:00:00"
}
```

#### Partial Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 activity(ies) deleted successfully",
  "data": null,
  "timestamp": "2025-12-30T12:00:00"
}
```

#### Error Responses
**500 Internal Server Error** - Deletion failed
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to delete activities",
  "errorCode": "ACTIVITIES_DELETE_FAILED",
  "timestamp": "2025-12-30T12:00:00"
}
```

---

### 4. Get Activity by ID

**Endpoint**: `GET /api/activities/{idObfuscated}`

**Permission**: `PERM_READ_ACTIVITY`

**Description**: Retrieves a single activity by its obfuscated ID.

#### Path Parameters
- `idObfuscated` (required): The obfuscated activity ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activity retrieved successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Game Drive Safari",
    "slug": "game-drive-safari",
    "hasTariff": true,
    "isWebActive": true,
    "chargingBasis": "PER_PERSON",
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
  "message": "Activity not found",
  "errorCode": "ACTIVITY_NOT_FOUND",
  "timestamp": "2025-12-30T13:00:00"
}
```

---

### 5. Get Activity by Slug

**Endpoint**: `GET /api/activities/slug/{slug}`

**Permission**: `PERM_READ_ACTIVITY`

**Description**: Retrieves a single activity by its URL-friendly slug.

#### Path Parameters
- `slug` (required): The activity slug (e.g., "game-drive-safari")

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activity retrieved successfully",
  "data": {
    "idObfuscated": "xY9Kp2Lm",
    "name": "Game Drive Safari",
    "slug": "game-drive-safari",
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
  "message": "Activity not found",
  "errorCode": "ACTIVITY_NOT_FOUND",
  "timestamp": "2025-12-30T13:30:00"
}
```

---

### 6. Get All Activities (with Filters)

**Endpoint**: `GET /api/activities`

**Permission**: `PERM_READ_ACTIVITY`

**Description**: Retrieves a paginated list of activities with optional filtering and sorting.

#### Query Parameters

**Filtering**:
- `name` (string, optional): Filter by name (partial match, case-insensitive)
- `slug` (string, optional): Filter by slug (partial match, case-insensitive)
- `hasTariff` (boolean, optional): Filter by tariff status
- `isWebActive` (boolean, optional): Filter by web active status
- `chargingBasis` (ChargingBasis, optional): Filter by charging basis (exact match)
- `isActive` (boolean, optional): Filter by active status
- `keyword` (string, optional): Search across name, description, tags, equipmentRequired, and safetyInformation

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortBy` (string, optional, default: "createdAt"): Field to sort by
  - Allowed values: `name`, `slug`, `hasTariff`, `isWebActive`, `chargingBasis`, `minimumAge`, `isActive`, `createdAt`, `updatedAt`
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`

#### Example Requests

**Basic request (all activities, default pagination)**:
```
GET /api/activities
```

**Filter by charging basis**:
```
GET /api/activities?chargingBasis=PER_PERSON
```

**Filter by tariff status with pagination**:
```
GET /api/activities?hasTariff=true&page=0&size=20
```

**Search by keyword**:
```
GET /api/activities?keyword=safari&sortBy=name&sortDirection=asc
```

**Complex filter**:
```
GET /api/activities?hasTariff=true&isWebActive=true&isActive=true&page=0&size=15&sortBy=name&sortDirection=asc
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities retrieved successfully",
  "data": {
    "activities": [
      {
        "idObfuscated": "xY9Kp2Lm",
        "name": "Game Drive Safari",
        "slug": "game-drive-safari",
        "hasTariff": true,
        "chargingBasis": "PER_PERSON",
        ...
      },
      {
        "idObfuscated": "aB3Cd4Ef",
        "name": "Walking Safari",
        "slug": "walking-safari",
        "hasTariff": true,
        "chargingBasis": "PER_GROUP",
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 3
  },
  "timestamp": "2025-12-30T14:00:00"
}
```

#### Response with No Results (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities retrieved successfully",
  "data": {
    "activities": [],
    "currentPage": 0,
    "totalItems": 0,
    "totalPages": 0
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
| `ACTIVITY_NOT_FOUND` | 404 | Activity with specified ID or slug not found |
| `ACTIVITY_NAME_EXISTS` | 400 | Activity with the same name already exists |
| `ACTIVITY_SLUG_EXISTS` | 400 | Activity with the same slug already exists |
| `INVALID_ACTIVITY_ID` | 400 | The provided obfuscated ID is invalid |
| `ACTIVITY_CREATE_FAILED` | 500 | Failed to create activity (internal error) |
| `ACTIVITY_UPDATE_FAILED` | 500 | Failed to update activity (internal error) |
| `ACTIVITY_FETCH_FAILED` | 500 | Failed to fetch activity (internal error) |
| `ACTIVITIES_FETCH_FAILED` | 500 | Failed to fetch activities (internal error) |
| `ACTIVITIES_DELETE_FAILED` | 500 | Failed to delete activities (internal error) |
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields, invalid format) |
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
   - Create operations: `PERM_CREATE_ACTIVITY`
   - Read operations: `PERM_READ_ACTIVITY`
   - Update operations: `PERM_UPDATE_ACTIVITY`
   - Delete operations: `PERM_DELETE_ACTIVITY`

---

## Best Practices

1. **Use Slugs for Public URLs**: Use the slug-based endpoint (`/api/activities/slug/{slug}`) for public-facing URLs as they're more SEO-friendly.

2. **Use IDs for Management**: Use obfuscated IDs for administrative operations (update, delete).

3. **Leverage Keyword Search**: Use the `keyword` parameter for general search functionality across multiple fields.

4. **Implement Pagination**: Always use pagination for list endpoints to improve performance.

5. **Filter Inactive Activities**: For public-facing applications, filter by `isActive=true` and `isWebActive=true` to show only visible activities.

6. **Cache Responses**: Consider caching activity data as it changes infrequently.

7. **Handle Partial Updates**: When updating, only send fields that need to be changed to avoid overwriting data.

8. **Charge Appropriately**: Use the correct `chargingBasis` to ensure accurate pricing (PER_PERSON, PER_VEHICLE, PER_GROUP, etc.).

---

## Examples

### cURL Examples

**Create an activity**:
```bash
curl -X POST http://localhost:8080/api/activities \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Game Drive Safari",
    "hasTariff": true,
    "chargingBasis": "PER_PERSON",
    "description": "Experience wildlife in their natural habitat",
    "minimumAge": 5,
    "maximumParticipants": 6,
    "isActive": true
  }'
```

**Get all activities with tariff**:
```bash
curl -X GET "http://localhost:8080/api/activities?hasTariff=true&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Update an activity**:
```bash
curl -X PUT http://localhost:8080/api/activities/xY9Kp2Lm \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated description",
    "minimumAge": 8
  }'
```

**Delete activities**:
```bash
curl -X DELETE http://localhost:8080/api/activities \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '["xY9Kp2Lm", "aB3Cd4Ef"]'
```

**Get activity by slug**:
```bash
curl -X GET http://localhost:8080/api/activities/slug/game-drive-safari \
  -H "Authorization: Bearer <your-token>"
```

---

## Notes

- **ID Obfuscation**: All activity IDs are obfuscated for security. Never expose internal database IDs.
- **Slug Generation**: If not provided during creation, slugs are automatically generated from the activity name.
- **Timestamps**: All timestamps are in ISO 8601 format (UTC).
- **Validation**: Required fields must be provided during creation. Update operations validate only non-null fields.
- **Tariff Management**: The `hasTariff` field indicates whether the activity has an associated cost. The `chargingBasis` specifies how pricing is calculated.
- **Web Visibility**: Use `isWebActive` to control whether an activity appears on the website, separate from `isActive` which controls overall system availability.

---

## Support

For issues or questions about the Activity API, please contact the development team or refer to the main application documentation.
