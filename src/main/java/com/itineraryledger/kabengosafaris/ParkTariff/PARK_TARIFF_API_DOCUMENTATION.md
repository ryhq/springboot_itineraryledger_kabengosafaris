# Park-Tariff API Documentation

## Overview
The Park-Tariff API manages the many-to-many relationships between parks and tariffs. It enables linking tariffs (fee types like "Park Entry Fee", "Conservation Fee") to specific parks and retrieving park-tariff associations with filtering and pagination.

**Base URL**: `/api/park-tariffs`

**Required Permissions**:
- `PERM_READ_PARK` + `PERM_READ_TARIFF` - View park-tariff relationships
- `PERM_UPDATE_PARK` + `PERM_UPDATE_TARIFF` - Create, update, or delete relationships

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Get Tariffs for Park](#1-get-tariffs-for-park)
   - [Get Parks for Tariff](#2-get-parks-for-tariff)
   - [Bulk Upsert Park-Tariff Relationships](#3-bulk-upsert-park-tariff-relationships)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)

---

## Data Models

### ParkTariffUpsertDTO (Request)
```json
{
  "parkId": "string (obfuscated park ID, required)",
  "tariffId": "string (obfuscated tariff ID, required)",
  "notes": "string (optional relationship notes)",
  "status": "boolean (required: true = create/update, false = delete)"
}
```

### ParkTariffUpsertResponseDTO (Response)
```json
{
  "totalProcessed": "integer (total operations attempted)",
  "successful": "integer (operations completed successfully)",
  "failed": "integer (operations that failed)",
  "errors": ["array of error messages for failed operations"]
}
```

### TariffWithNotesDTO (Response)
```json
{
  "id": "string (obfuscated tariff ID)",
  "name": "string",
  "slug": "string (URL-friendly identifier)",
  "chargingBasis": "ChargingBasis enum (PER_PERSON, PER_VEHICLE, etc.)",
  "chargingBasisDisplayName": "string (e.g., 'Per Person')",
  "description": "string",
  "requiresAgeCategory": "boolean",
  "isActive": "boolean",
  "isSystem": "boolean",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
  "notes": "string (park-specific notes from the relationship)"
}
```

### ParkWithNotesDTO (Response)
```json
{
  "id": "string (obfuscated park ID)",
  "name": "string",
  "slug": "string",
  "parkType": "ParkType enum",
  "region": "string",
  "district": "string",
  "location": "string",
  "latitude": "number",
  "longitude": "number",
  "elevation": "string",
  "size": "string",
  "shortDescription": "string",
  "fullDescription": "string",
  "history": "string",
  "ecosystem": "string",
  "wildlife": "string",
  "vegetation": "string",
  "primaryImage": "string",
  "bestTimeToVisit": "string",
  "openingHours": "string",
  "accessInformation": "string",
  "tags": "array of strings",
  "isActive": "boolean",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
  "notes": "string (tariff-specific notes from the relationship)"
}
```

---

## Endpoints

### 1. Get Tariffs for Park

**Endpoint**: `GET /api/park-tariffs/parks/{parkIdObfuscated}/tariffs`

**Permission**: `PERM_READ_PARK` + `PERM_READ_TARIFF`

**Description**: Retrieves tariffs associated with a specific park. Can return either assigned tariffs or unassigned tariffs (available for assignment).

#### Path Parameters
- `parkIdObfuscated` (required): The obfuscated park ID

#### Query Parameters

**Assignment Filter**:
- `assigned` (boolean, optional, default: true):
  - `true` or omitted: Returns tariffs **assigned** to this park (includes relationship notes)
  - `false`: Returns tariffs **not assigned** to this park (available for assignment)

**Filtering**:
- `name` (string, optional): Filter by tariff name (partial match)
- `slug` (string, optional): Filter by tariff slug (partial match)
- `chargingBasis` (ChargingBasis, optional): Filter by charging basis
  - Values: `PER_PERSON`, `PER_VEHICLE`, `PER_GROUP`, `PER_DAY`, `PER_HOUR`, `PER_SESSION`, `FLAT_RATE`
- `isActive` (boolean, optional): Filter by active status
- `isSystem` (boolean, optional): Filter by system status
- `keyword` (string, optional): Search across tariff name, slug, and description

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortDirection` (string, optional, default: "asc"): Sort direction (`asc` or `desc`)
- **Note**: Results are sorted by tariff `name`

#### Example Requests

**Get tariffs assigned to a park**:
```
GET /api/park-tariffs/parks/xY9Kp2Lm/tariffs
```

**Get unassigned tariffs (available for assignment)**:
```
GET /api/park-tariffs/parks/xY9Kp2Lm/tariffs?assigned=false
```

**Filter by charging basis with pagination**:
```
GET /api/park-tariffs/parks/xY9Kp2Lm/tariffs?chargingBasis=PER_PERSON&page=0&size=20
```

**Search unassigned tariffs by keyword**:
```
GET /api/park-tariffs/parks/xY9Kp2Lm/tariffs?assigned=false&keyword=camping&isActive=true
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
        "id": "aB3Cd4Ef",
        "name": "Park Entry Fee",
        "slug": "park-entry-fee",
        "chargingBasis": "PER_PERSON",
        "chargingBasisDisplayName": "Per Person",
        "description": "Standard entry fee for national parks",
        "requiresAgeCategory": true,
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-10T09:00:00",
        "updatedAt": "2026-01-10T09:00:00",
        "notes": "Special rate applies during migration season"
      },
      {
        "id": "pQ7Rs8Tv",
        "name": "Conservation Fee",
        "slug": "conservation-fee",
        "chargingBasis": "PER_PERSON",
        "chargingBasisDisplayName": "Per Person",
        "description": "Fee for wildlife conservation efforts",
        "requiresAgeCategory": true,
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-10T09:00:00",
        "updatedAt": "2026-01-10T09:00:00",
        "notes": null
      }
    ],
    "currentPage": 0,
    "totalItems": 8,
    "totalPages": 1
  },
  "timestamp": "2026-01-12T14:30:00"
}
```

**Note**: The `notes` field is only populated when `assigned=true` (default). When `assigned=false`, notes will be null since no relationship exists.

#### Error Responses

**400 Bad Request** - Park not found
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Park not found",
  "errorCode": "PARK_NOT_FOUND",
  "timestamp": "2026-01-12T14:30:00"
}
```

---

### 2. Get Parks for Tariff

**Endpoint**: `GET /api/park-tariffs/tariffs/{tariffIdObfuscated}/parks`

**Permission**: `PERM_READ_PARK` + `PERM_READ_TARIFF`

**Description**: Retrieves parks associated with a specific tariff. Can return either assigned parks or unassigned parks (available for assignment).

#### Path Parameters
- `tariffIdObfuscated` (required): The obfuscated tariff ID

#### Query Parameters

**Assignment Filter**:
- `assigned` (boolean, optional, default: true):
  - `true` or omitted: Returns parks **assigned** to this tariff (includes relationship notes)
  - `false`: Returns parks **not assigned** to this tariff (available for assignment)

**Filtering**:
- `name` (string, optional): Filter by park name (partial match)
- `slug` (string, optional): Filter by park slug (partial match)
- `parkType` (ParkType, optional): Filter by park type
  - Values: `NATIONAL_PARK`, `GAME_RESERVE`, `CONSERVATION_AREA`, `MARINE_PARK`, `FOREST_RESERVE`, `WILDLIFE_MANAGEMENT_AREA`
- `region` (string, optional): Filter by region (partial match)
- `district` (string, optional): Filter by district (partial match)
- `location` (string, optional): Filter by location (partial match)
- `parkSize` (string, optional): Filter by park size (partial match)
- `isActive` (boolean, optional): Filter by active status
- `keyword` (string, optional): Search across multiple park fields

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortDirection` (string, optional, default: "desc"): Sort direction (`asc` or `desc`)
- **Note**: Results are sorted by `createdAt`

#### Example Requests

**Get parks with a specific tariff assigned**:
```
GET /api/park-tariffs/tariffs/aB3Cd4Ef/parks
```

**Get parks without this tariff (available for assignment)**:
```
GET /api/park-tariffs/tariffs/aB3Cd4Ef/parks?assigned=false
```

**Filter by park type and region**:
```
GET /api/park-tariffs/tariffs/aB3Cd4Ef/parks?parkType=NATIONAL_PARK&region=Northern
```

**Search unassigned parks**:
```
GET /api/park-tariffs/tariffs/aB3Cd4Ef/parks?assigned=false&keyword=serengeti&isActive=true
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
        "id": "xY9Kp2Lm",
        "name": "Serengeti National Park",
        "slug": "serengeti-national-park",
        "parkType": "NATIONAL_PARK",
        "region": "Mara, Simiyu, Arusha",
        "district": "Serengeti, Bunda, Ngorongoro",
        "location": "Northern Tanzania",
        "latitude": -2.3333,
        "longitude": 34.8333,
        "elevation": "920-1850m",
        "size": "14,763 km²",
        "shortDescription": "Tanzania's oldest and most popular national park",
        "fullDescription": "...",
        "history": "...",
        "ecosystem": "Savanna grassland",
        "wildlife": "Big Five, Great Migration",
        "vegetation": "Acacia woodlands, grasslands",
        "primaryImage": "https://example.com/serengeti.jpg",
        "bestTimeToVisit": "June to October",
        "openingHours": "06:00 - 18:00",
        "accessInformation": "Via Arusha or Mwanza",
        "tags": ["safari", "migration", "big-five"],
        "isActive": true,
        "createdAt": "2026-01-05T08:00:00",
        "updatedAt": "2026-01-10T12:00:00",
        "notes": "Premium rates apply during Great Migration"
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2
  },
  "timestamp": "2026-01-12T15:00:00"
}
```

#### Error Responses

**400 Bad Request** - Tariff not found
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Tariff not found",
  "errorCode": "TARIFF_NOT_FOUND",
  "timestamp": "2026-01-12T15:00:00"
}
```

---

### 3. Bulk Upsert Park-Tariff Relationships

**Endpoint**: `POST /api/park-tariffs/upsert`

**Permission**: `PERM_UPDATE_PARK` + `PERM_UPDATE_TARIFF`

**Description**: Creates, updates, or deletes multiple park-tariff relationships in a single request. This is the primary method for managing which tariffs are assigned to which parks.

#### Request Body
```json
[
  {
    "parkId": "xY9Kp2Lm",
    "tariffId": "aB3Cd4Ef",
    "notes": "Special migration season rates apply",
    "status": true
  },
  {
    "parkId": "xY9Kp2Lm",
    "tariffId": "pQ7Rs8Tv",
    "notes": null,
    "status": true
  },
  {
    "parkId": "mN5Op6Qr",
    "tariffId": "aB3Cd4Ef",
    "notes": null,
    "status": false
  }
]
```

**Required Fields**:
- `parkId` (string): Obfuscated park ID
- `tariffId` (string): Obfuscated tariff ID
- `status` (boolean): Operation type
  - `true`: Create new relationship or update existing (updates notes)
  - `false`: Delete the relationship

**Optional Fields**:
- `notes` (string): Park-specific notes for this tariff relationship (e.g., special conditions, seasonal variations)

#### Operation Behavior

| Status | Relationship Exists | Action |
|--------|---------------------|--------|
| `true` | No | Create new relationship |
| `true` | Yes | Update notes on existing relationship |
| `false` | No | Error: Relationship does not exist |
| `false` | Yes | Delete the relationship |

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Bulk upsert completed",
  "data": {
    "totalProcessed": 3,
    "successful": 2,
    "failed": 1,
    "errors": [
      "Relationship does not exist for park: Lake Manyara National Park, tariff: Park Entry Fee"
    ]
  },
  "timestamp": "2026-01-12T16:00:00"
}
```

#### Partial Success Response (200 OK)
Even if some operations fail, the endpoint returns 200 OK with details about failures:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Bulk upsert completed",
  "data": {
    "totalProcessed": 5,
    "successful": 3,
    "failed": 2,
    "errors": [
      "Park not found: invalidParkId123",
      "Tariff not found: invalidTariffId456"
    ]
  },
  "timestamp": "2026-01-12T16:00:00"
}
```

#### Error Types in Response

| Error Message Pattern | Cause |
|----------------------|-------|
| `Invalid ID format for park: {id}, tariff: {id}` | One or both IDs couldn't be decoded |
| `Park not found: {parkId}` | Park with given ID doesn't exist |
| `Tariff not found: {tariffId}` | Tariff with given ID doesn't exist |
| `Relationship does not exist for park: {name}, tariff: {name}` | Tried to delete non-existent relationship |
| `Error processing relationship: {details}` | Unexpected error during processing |

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
| `PARK_NOT_FOUND` | 400 | Park with specified ID not found |
| `TARIFF_NOT_FOUND` | 400 | Tariff with specified ID not found |
| `FETCH_FAILED` | 500 | Failed to fetch data (internal error) |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `PERMISSION_DENIED` | 403 | User lacks required permissions |
| `UNAUTHORIZED` | 401 | User is not authenticated |

---

## Authentication & Authorization

All endpoints require:
1. **Authentication**: Valid JWT token in the `Authorization` header
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Authorization**: User must have BOTH permissions:
   - Read operations: `PERM_READ_PARK` AND `PERM_READ_TARIFF`
   - Write operations: `PERM_UPDATE_PARK` AND `PERM_UPDATE_TARIFF`

---

## Use Cases

### Use Case 1: Assigning Tariffs to a New Park

When setting up a new park, assign relevant tariffs:

```bash
# 1. Get all available tariffs (not yet assigned to this park)
curl -X GET "http://localhost:8080/api/park-tariffs/parks/xY9Kp2Lm/tariffs?assigned=false" \
  -H "Authorization: Bearer <token>"

# 2. Assign selected tariffs to the park
curl -X POST http://localhost:8080/api/park-tariffs/upsert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[
    {"parkId": "xY9Kp2Lm", "tariffId": "aB3Cd4Ef", "status": true, "notes": "Standard rates"},
    {"parkId": "xY9Kp2Lm", "tariffId": "pQ7Rs8Tv", "status": true},
    {"parkId": "xY9Kp2Lm", "tariffId": "jK1Lm2No", "status": true}
  ]'
```

### Use Case 2: Managing Tariff Assignments for a Tariff

When rolling out a new tariff across parks:

```bash
# 1. Get parks that don't have this tariff yet
curl -X GET "http://localhost:8080/api/park-tariffs/tariffs/aB3Cd4Ef/parks?assigned=false&parkType=NATIONAL_PARK" \
  -H "Authorization: Bearer <token>"

# 2. Assign the tariff to selected parks
curl -X POST http://localhost:8080/api/park-tariffs/upsert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[
    {"parkId": "xY9Kp2Lm", "tariffId": "aB3Cd4Ef", "status": true},
    {"parkId": "mN5Op6Qr", "tariffId": "aB3Cd4Ef", "status": true},
    {"parkId": "sT9Uv0Wx", "tariffId": "aB3Cd4Ef", "status": true}
  ]'
```

### Use Case 3: Updating Relationship Notes

Update notes on existing park-tariff relationships:

```bash
curl -X POST http://localhost:8080/api/park-tariffs/upsert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "parkId": "xY9Kp2Lm",
      "tariffId": "aB3Cd4Ef",
      "notes": "Premium rates apply during Great Migration (July-October)",
      "status": true
    }
  ]'
```

### Use Case 4: Removing Tariff from Parks

Remove a tariff from specific parks:

```bash
curl -X POST http://localhost:8080/api/park-tariffs/upsert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '[
    {"parkId": "xY9Kp2Lm", "tariffId": "aB3Cd4Ef", "status": false},
    {"parkId": "mN5Op6Qr", "tariffId": "aB3Cd4Ef", "status": false}
  ]'
```

---

## Best Practices

1. **Use Bulk Operations**: Always use the bulk upsert endpoint even for single operations. It provides consistent response handling and supports batch processing.

2. **Check Assignment Status**: Use the `assigned` parameter to efficiently find:
   - Assigned tariffs/parks (`assigned=true` or omit)
   - Available tariffs/parks for assignment (`assigned=false`)

3. **Handle Partial Failures**: The bulk upsert endpoint may have partial success. Always check `successful` vs `failed` counts and review `errors` array.

4. **Use Notes for Context**: Leverage the `notes` field to document park-specific tariff information like seasonal variations, special conditions, or rate exceptions.

5. **Filter Effectively**: Use filtering parameters to narrow results rather than fetching all data and filtering client-side.

6. **Pagination**: Always implement pagination for list endpoints. Default page size is 10, but you can request up to larger sizes as needed.

---

## Related APIs

### Tariff API
Manage tariff definitions (fee types).

**Base URL**: `/api/tariffs`

See Tariff API documentation for:
- Creating/updating/deleting tariffs
- Managing tariff metadata and charging basis

### Park API
Manage park information.

**Base URL**: `/api/parks`

See Park API documentation for:
- Creating/updating/deleting parks
- Managing park details and metadata

### ParkTariffRate API
Define pricing rates for park-tariff combinations.

**Base URL**: `/api/park-tariff-rates`

See ParkTariffRate API documentation for:
- Setting rates by season, nation category, and age category
- Bulk upsert operations for rate management

---

## Notes

- **ID Obfuscation**: All IDs are obfuscated for security. Never expose internal database IDs.
- **Audit Logging**: All upsert operations are logged for audit trail purposes.
- **Transaction Safety**: Bulk upsert operations are transactional - individual items may fail without affecting others.
- **Relationship Notes**: Notes are relationship-specific. The same tariff can have different notes for different parks.
- **Sorting Defaults**: Tariffs sort by name (asc), Parks sort by createdAt (desc).

---

## Support

For issues or questions about the Park-Tariff API, please contact the development team or refer to the main application documentation.
