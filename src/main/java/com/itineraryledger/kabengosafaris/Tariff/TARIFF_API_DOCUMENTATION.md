# Tariff API Documentation

## Overview
The Tariff API provides endpoints for managing tariff definitions such as "Park Entry Fee", "Conservation Fee", "Concession Fee", etc. Tariffs are base entities that define fee types and their charging basis. They can be linked to parks via the ParkTariff join entity to create park-specific tariff assignments.

**Base URL**: `/api/tariffs`

**Required Permissions**:
- `PERM_CREATE_TARIFF` - Create new tariffs
- `PERM_READ_TARIFF` - View tariffs
- `PERM_UPDATE_TARIFF` - Update existing tariffs
- `PERM_DELETE_TARIFF` - Delete tariffs

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Create Tariff](#1-create-tariff)
   - [Update Tariff](#2-update-tariff)
   - [Delete Tariffs](#3-delete-tariffs)
   - [Get Tariff by ID](#4-get-tariff-by-id)
   - [Get Tariff by Slug](#5-get-tariff-by-slug)
   - [Get All Tariffs](#6-get-all-tariffs-with-filters)
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

**Note**: `PER_PERSON` charging basis requires age category for rate lookups. Other charging bases do not require age category.

### Tariff Object (TariffDTO)
```json
{
  "id": "string (obfuscated ID for security)",
  "name": "string",
  "slug": "string (URL-friendly identifier)",
  "chargingBasis": "ChargingBasis enum",
  "chargingBasisDisplayName": "string (e.g., 'Per Person', 'Per Vehicle')",
  "description": "string",
  "requiresAgeCategory": "boolean (true if chargingBasis is PER_PERSON)",
  "isActive": "boolean",
  "isSystem": "boolean (system tariffs cannot be deleted)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
  "parkCount": "integer (optional, number of parks linked to this tariff)"
}
```

---

## Endpoints

### 1. Create Tariff

**Endpoint**: `POST /api/tariffs`

**Permission**: `PERM_CREATE_TARIFF`

**Description**: Creates a new tariff in the system.

#### Request Body (CreateTariffDTO)
```json
{
  "name": "Park Entry Fee",
  "slug": "park-entry-fee",
  "chargingBasis": "PER_PERSON",
  "description": "Standard entry fee for national parks",
  "internalNotes": "This tariff applies to all national parks",
  "isActive": true
}
```

**Required Fields**:
- `name` (string, not blank, max 150 characters, must be unique)
- `chargingBasis` (ChargingBasis enum)

**Optional Fields**:
- `slug` - If not provided, will be auto-generated from the name
- `description` - Brief description of the tariff
- `internalNotes` - Internal notes visible to staff only
- `isActive` - Defaults to true if not provided

#### Success Response (201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Tariff created successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "name": "Park Entry Fee",
    "slug": "park-entry-fee",
    "chargingBasis": "PER_PERSON",
    "chargingBasisDisplayName": "Per Person",
    "description": "Standard entry fee for national parks",
    "requiresAgeCategory": true,
    "isActive": true,
    "isSystem": false,
    "createdAt": "2026-01-12T10:30:00",
    "updatedAt": "2026-01-12T10:30:00"
  },
  "timestamp": "2026-01-12T10:30:00"
}
```

#### Error Responses
**400 Bad Request** - Validation errors or duplicate name/slug
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Tariff with name 'Park Entry Fee' already exists",
  "errorCode": "TARIFF_NAME_EXISTS",
  "timestamp": "2026-01-12T10:30:00"
}
```

---

### 2. Update Tariff

**Endpoint**: `PUT /api/tariffs/{id}`

**Permission**: `PERM_UPDATE_TARIFF`

**Description**: Updates an existing tariff. Only provided fields will be updated (partial update).

#### Path Parameters
- `id` (required): The obfuscated tariff ID

#### Request Body (UpdateTariffDTO)
```json
{
  "description": "Updated description",
  "chargingBasis": "PER_VEHICLE",
  "isActive": false
}
```

**Note**: All fields are optional. Only include fields you want to update.

**Warning**: Changing `chargingBasis` may affect existing rates linked to this tariff.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tariff updated successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "name": "Park Entry Fee",
    "slug": "park-entry-fee",
    "chargingBasis": "PER_VEHICLE",
    "chargingBasisDisplayName": "Per Vehicle",
    "description": "Updated description",
    "requiresAgeCategory": false,
    "isActive": false,
    "isSystem": false,
    "createdAt": "2026-01-12T10:30:00",
    "updatedAt": "2026-01-12T11:00:00"
  },
  "timestamp": "2026-01-12T11:00:00"
}
```

#### Error Responses
**404 Not Found** - Tariff doesn't exist
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Tariff not found",
  "errorCode": "TARIFF_NOT_FOUND",
  "timestamp": "2026-01-12T11:00:00"
}
```

**400 Bad Request** - Name/slug already exists
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Tariff with name 'Conservation Fee' already exists",
  "errorCode": "TARIFF_NAME_EXISTS",
  "timestamp": "2026-01-12T11:00:00"
}
```

---

### 3. Delete Tariffs

**Endpoint**: `DELETE /api/tariffs`

**Permission**: `PERM_DELETE_TARIFF`

**Description**: Deletes one or more tariffs by their obfuscated IDs.

**Note**: System tariffs (`isSystem: true`) cannot be deleted.

#### Request Body
```json
["xY9Kp2Lm", "aB3Cd4Ef", "pQ7Rs8Tv"]
```

**Note**: Send an array of obfuscated tariff IDs to delete.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 tariff(s) deleted successfully",
  "data": null,
  "timestamp": "2026-01-12T12:00:00"
}
```

#### Partial Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 tariff(s) deleted successfully",
  "data": null,
  "timestamp": "2026-01-12T12:00:00"
}
```

#### Error Responses
**400 Bad Request** - Attempting to delete system tariff
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete system tariff",
  "errorCode": "SYSTEM_TARIFF_DELETE_BLOCKED",
  "timestamp": "2026-01-12T12:00:00"
}
```

**500 Internal Server Error** - Deletion failed
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to delete tariffs",
  "errorCode": "TARIFFS_DELETE_FAILED",
  "timestamp": "2026-01-12T12:00:00"
}
```

---

### 4. Get Tariff by ID

**Endpoint**: `GET /api/tariffs/{id}`

**Permission**: `PERM_READ_TARIFF`

**Description**: Retrieves a single tariff by its obfuscated ID.

#### Path Parameters
- `id` (required): The obfuscated tariff ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tariff retrieved successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "name": "Park Entry Fee",
    "slug": "park-entry-fee",
    "chargingBasis": "PER_PERSON",
    "chargingBasisDisplayName": "Per Person",
    "description": "Standard entry fee for national parks",
    "requiresAgeCategory": true,
    "isActive": true,
    "isSystem": false,
    "createdAt": "2026-01-12T10:30:00",
    "updatedAt": "2026-01-12T11:00:00"
  },
  "timestamp": "2026-01-12T13:00:00"
}
```

#### Error Responses
**400 Bad Request** - Invalid ID format
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid tariff ID",
  "errorCode": "INVALID_TARIFF_ID",
  "timestamp": "2026-01-12T13:00:00"
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Tariff not found",
  "errorCode": "TARIFF_NOT_FOUND",
  "timestamp": "2026-01-12T13:00:00"
}
```

---

### 5. Get Tariff by Slug

**Endpoint**: `GET /api/tariffs/slug/{slug}`

**Permission**: `PERM_READ_TARIFF`

**Description**: Retrieves a single tariff by its URL-friendly slug.

#### Path Parameters
- `slug` (required): The tariff slug (e.g., "park-entry-fee")

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tariff retrieved successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "name": "Park Entry Fee",
    "slug": "park-entry-fee",
    "chargingBasis": "PER_PERSON",
    "chargingBasisDisplayName": "Per Person",
    "description": "Standard entry fee for national parks",
    "requiresAgeCategory": true,
    "isActive": true,
    "isSystem": false,
    "createdAt": "2026-01-12T10:30:00",
    "updatedAt": "2026-01-12T11:00:00"
  },
  "timestamp": "2026-01-12T13:30:00"
}
```

#### Error Responses
**400 Bad Request** - Invalid slug
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Slug is required",
  "errorCode": "INVALID_SLUG",
  "timestamp": "2026-01-12T13:30:00"
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Tariff not found",
  "errorCode": "TARIFF_NOT_FOUND",
  "timestamp": "2026-01-12T13:30:00"
}
```

---

### 6. Get All Tariffs (with Filters)

**Endpoint**: `GET /api/tariffs`

**Permission**: `PERM_READ_TARIFF`

**Description**: Retrieves a paginated list of tariffs with optional filtering and sorting.

#### Query Parameters

**Filtering**:
- `name` (string, optional): Filter by name (partial match, case-insensitive)
- `slug` (string, optional): Filter by slug (partial match, case-insensitive)
- `chargingBasis` (ChargingBasis, optional): Filter by charging basis (exact match)
- `isActive` (boolean, optional): Filter by active status
- `isSystem` (boolean, optional): Filter by system status
- `keyword` (string, optional): Search across name, slug, and description

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`
- **Note**: Results are always sorted by `createdAt` field

#### Example Requests

**Basic request (all tariffs, default pagination)**:
```
GET /api/tariffs
```

**Filter by charging basis**:
```
GET /api/tariffs?chargingBasis=PER_PERSON
```

**Filter by active status with pagination**:
```
GET /api/tariffs?isActive=true&page=0&size=20
```

**Search by keyword**:
```
GET /api/tariffs?keyword=entry&sortDirection=asc
```

**Filter non-system tariffs only**:
```
GET /api/tariffs?isSystem=false&isActive=true
```

**Complex filter**:
```
GET /api/tariffs?chargingBasis=PER_PERSON&isActive=true&isSystem=false&page=0&size=15&sortDirection=desc
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tariffs retrieved successfully",
  "data": {
    "tariffs": [
      {
        "id": "xY9Kp2Lm",
        "name": "Park Entry Fee",
        "slug": "park-entry-fee",
        "chargingBasis": "PER_PERSON",
        "chargingBasisDisplayName": "Per Person",
        "description": "Standard entry fee for national parks",
        "requiresAgeCategory": true,
        "isActive": true,
        "isSystem": false,
        "createdAt": "2026-01-12T10:30:00",
        "updatedAt": "2026-01-12T10:30:00"
      },
      {
        "id": "aB3Cd4Ef",
        "name": "Conservation Fee",
        "slug": "conservation-fee",
        "chargingBasis": "PER_PERSON",
        "chargingBasisDisplayName": "Per Person",
        "description": "Fee for wildlife conservation efforts",
        "requiresAgeCategory": true,
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-11T09:00:00",
        "updatedAt": "2026-01-11T09:00:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 3
  },
  "timestamp": "2026-01-12T14:00:00"
}
```

#### Response with No Results (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Tariffs retrieved successfully",
  "data": {
    "tariffs": [],
    "currentPage": 0,
    "totalItems": 0,
    "totalPages": 0
  },
  "timestamp": "2026-01-12T14:00:00"
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
  "timestamp": "2026-01-12T10:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-01-12T10:00:00"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `TARIFF_NOT_FOUND` | 404 | Tariff with specified ID or slug not found |
| `TARIFF_NAME_EXISTS` | 400 | Tariff with the same name already exists |
| `TARIFF_SLUG_EXISTS` | 400 | Tariff with the same slug already exists |
| `INVALID_TARIFF_ID` | 400 | The provided obfuscated ID is invalid |
| `INVALID_SLUG` | 400 | The provided slug is invalid or empty |
| `SYSTEM_TARIFF_DELETE_BLOCKED` | 400 | Cannot delete system tariffs |
| `TARIFF_CREATE_FAILED` | 500 | Failed to create tariff (internal error) |
| `TARIFF_UPDATE_FAILED` | 500 | Failed to update tariff (internal error) |
| `TARIFF_FETCH_FAILED` | 500 | Failed to fetch tariff (internal error) |
| `TARIFFS_FETCH_FAILED` | 500 | Failed to fetch tariffs (internal error) |
| `TARIFFS_DELETE_FAILED` | 500 | Failed to delete tariffs (internal error) |
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
   - Create operations: `PERM_CREATE_TARIFF`
   - Read operations: `PERM_READ_TARIFF`
   - Update operations: `PERM_UPDATE_TARIFF`
   - Delete operations: `PERM_DELETE_TARIFF`

---

## Related APIs

### ParkTariff API
Tariffs can be linked to parks using the ParkTariff API. This creates a many-to-many relationship between parks and tariffs.

**Base URL**: `/api/park-tariffs`

See the ParkTariff API documentation for details on:
- Linking tariffs to parks
- Managing park-specific tariff assignments
- Querying tariffs by park

### ParkTariffRate API
Once a tariff is linked to a park, rates can be defined using the ParkTariffRate API.

**Base URL**: `/api/park-tariff-rates`

See the ParkTariffRate API documentation for details on:
- Setting rates by season, nation category, and age category
- Bulk upsert operations for efficient rate management
- Rate lookups with filtering

---

## Best Practices

1. **Use Slugs for Public URLs**: Use the slug-based endpoint (`/api/tariffs/slug/{slug}`) for public-facing URLs as they're more SEO-friendly.

2. **Use IDs for Management**: Use obfuscated IDs for administrative operations (update, delete).

3. **Understand Charging Basis**: Choose the correct `chargingBasis` as it determines:
   - How rates are calculated
   - Whether age category is required for rate lookups
   - Display formatting in the UI

4. **System Tariffs**: System tariffs (`isSystem: true`) are protected and cannot be deleted. They represent core fee types required by the system.

5. **Implement Pagination**: Always use pagination for list endpoints to improve performance.

6. **Filter Inactive Tariffs**: For public-facing applications, filter by `isActive=true` to show only active tariffs.

7. **Handle Partial Updates**: When updating, only send fields that need to be changed to avoid overwriting data.

---

## Examples

### cURL Examples

**Create a tariff**:
```bash
curl -X POST http://localhost:8080/api/tariffs \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Park Entry Fee",
    "chargingBasis": "PER_PERSON",
    "description": "Standard entry fee for national parks",
    "isActive": true
  }'
```

**Get all active tariffs**:
```bash
curl -X GET "http://localhost:8080/api/tariffs?isActive=true&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Get tariffs requiring age category**:
```bash
curl -X GET "http://localhost:8080/api/tariffs?chargingBasis=PER_PERSON" \
  -H "Authorization: Bearer <your-token>"
```

**Update a tariff**:
```bash
curl -X PUT http://localhost:8080/api/tariffs/xY9Kp2Lm \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated description",
    "isActive": false
  }'
```

**Delete tariffs**:
```bash
curl -X DELETE http://localhost:8080/api/tariffs \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '["xY9Kp2Lm", "aB3Cd4Ef"]'
```

**Get tariff by slug**:
```bash
curl -X GET http://localhost:8080/api/tariffs/slug/park-entry-fee \
  -H "Authorization: Bearer <your-token>"
```

---

## Notes

- **ID Obfuscation**: All tariff IDs are obfuscated for security. Never expose internal database IDs.
- **Slug Generation**: If not provided during creation, slugs are automatically generated from the tariff name.
- **Timestamps**: All timestamps are in ISO 8601 format (UTC).
- **Validation**: Required fields must be provided during creation. Update operations validate only non-null fields.
- **Charging Basis Impact**: The `chargingBasis` field determines whether rates require age category. PER_PERSON tariffs require age category; others do not.
- **System Protection**: System tariffs are protected from deletion to ensure core functionality is preserved.
- **Default Sorting**: Results are sorted by `createdAt` in descending order (newest first) by default.

---

## Support

For issues or questions about the Tariff API, please contact the development team or refer to the main application documentation.
