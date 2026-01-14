# Park-Activity Relationship API Documentation

## Overview

The Park-Activity API manages the many-to-many relationship between parks and activities. This allows:
- A park to have multiple activities
- An activity to be available in multiple parks
- Park-specific notes for each activity
- Unique constraints to prevent duplicate associations

## Implementation Details

### Database Structure
- **Join Table**: `parks_activities`
- **Composite Primary Key**: `(park_id, activity_id)`
- **Unique Constraint**: Prevents duplicate park-activity combinations
- **Cascade**: `CascadeType.ALL` with `orphanRemoval = true`
- **Independent Entities**: Deleting a park or activity doesn't affect the other

### Key Features
1. **Filtering by Park**: Get all activities available at a specific park OR activities NOT assigned to a park
2. **Filtering by Activity**: Get all parks that offer a specific activity OR parks NOT assigned to an activity
3. **Bulk Upsert**: Create, update, or delete multiple relationships in one request
4. **Notes Field**: Store park-specific notes for each activity
5. **Full Pagination**: All list endpoints support pagination and sorting
6. **Assignment Toggle**: Single `assigned` parameter controls whether to show assigned or unassigned items

---

## API Endpoints

### 1. Get Activities for a Park

**Endpoint**: `GET /api/park-activities/parks/{parkIdObfuscated}/activities`

**Description**: Retrieve all activities available at a specific park with filtering, pagination, and sorting. The response includes park-specific notes for each activity.

**Permissions**: `PERM_READ_PARK` and `PERM_READ_ACTIVITY`

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| parkIdObfuscated | String | Yes | Obfuscated park ID |

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| assigned | Boolean | No | true | Filter by assignment status: true = activities assigned to this park, false = activities NOT assigned to this park |
| name | String | No | - | Filter by activity name (partial match) |
| slug | String | No | - | Filter by activity slug (partial match) |
| hasTariff | Boolean | No | - | Filter by tariff status |
| isWebActive | Boolean | No | - | Filter by web active status |
| chargingBasis | Enum | No | - | Filter by charging basis (PER_PERSON, PER_VEHICLE, etc.) |
| isActive | Boolean | No | - | Filter by active status |
| keyword | String | No | - | Search across multiple fields |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 10 | Page size |
| sortDirection | String | No | desc | Sort direction (asc/desc), always sorts by createdAt |

**Success Response (200)**:
```json
{
    "status": 200,
    "message": "Activities retrieved successfully",
    "data": {
        "activities": [
            {
                "id": "abc123",
                "name": "Game Drive Safari",
                "slug": "game-drive-safari",
                "hasTariff": true,
                "isWebActive": true,
                "chargingBasis": "PER_PERSON",
                "description": "Experience wildlife in their natural habitat",
                "detailedDescription": "...",
                "minimumAge": 5,
                "maximumParticipants": 6,
                "equipmentRequired": "Binoculars, camera...",
                "seasonAvailability": "Year-round",
                "primaryImage": null,
                "tags": "Safari, Wildlife, Big Five",
                "safetyInformation": "Remain seated...",
                "isActive": true,
                "createdAt": "2025-12-25T10:00:00",
                "updatedAt": "2025-12-25T10:00:00",
                "notes": "Available from 6 AM to 6 PM daily"
            }
        ],
        "currentPage": 0,
        "totalItems": 15,
        "totalPages": 2
    }
}
```

**Error Responses**:
- `400 Bad Request`: Invalid park ID or park not found
- `500 Internal Server Error`: Server error

---

### 2. Get Parks for an Activity

**Endpoint**: `GET /api/park-activities/activities/{activityIdObfuscated}/parks`

**Description**: Retrieve all parks where a specific activity is available with filtering, pagination, and sorting. The response includes activity-specific notes for each park.

**Permissions**: `PERM_READ_PARK` and `PERM_READ_ACTIVITY`

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| activityIdObfuscated | String | Yes | Obfuscated activity ID |

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| assigned | Boolean | No | true | Filter by assignment status: true = parks assigned to this activity, false = parks NOT assigned to this activity |
| name | String | No | - | Filter by park name (partial match) |
| slug | String | No | - | Filter by park slug (partial match) |
| parkType | Enum | No | - | Filter by park type (NATIONAL_PARK, GAME_RESERVE, etc.) |
| region | String | No | - | Filter by region (partial match) |
| district | String | No | - | Filter by district (partial match) |
| location | String | No | - | Filter by location (partial match) |
| parkSize | String | No | - | Filter by park size (partial match) |
| isActive | Boolean | No | - | Filter by active status |
| keyword | String | No | - | Search across multiple fields |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 10 | Page size |
| sortDirection | String | No | desc | Sort direction (asc/desc), always sorts by createdAt |

**Success Response (200)**:
```json
{
    "status": 200,
    "message": "Parks retrieved successfully",
    "data": {
        "parks": [
            {
                "id": "xyz789",
                "name": "Serengeti National Park",
                "slug": "serengeti-national-park",
                "parkType": "NATIONAL_PARK",
                "region": "Mara",
                "district": "Serengeti",
                "location": "Northern Tanzania",
                "latitude": -2.3333333,
                "longitude": 34.8333333,
                "elevation": "920m - 1,850m",
                "size": "14,763 km²",
                "shortDescription": "Tanzania's oldest national park...",
                "fullDescription": "...",
                "history": "...",
                "ecosystem": "...",
                "wildlife": "...",
                "vegetation": "...",
                "primaryImage": null,
                "bestTimeToVisit": "June-October",
                "openingHours": "6:00 AM - 6:00 PM",
                "accessInformation": "...",
                "tags": "Big Five, Great Migration",
                "isActive": true,
                "createdAt": "2025-12-25T09:00:00",
                "updatedAt": "2025-12-25T09:00:00",
                "notes": "Best for this activity during dry season"
            }
        ],
        "currentPage": 0,
        "totalItems": 8,
        "totalPages": 1
    }
}
```

**Error Responses**:
- `400 Bad Request`: Invalid activity ID or activity not found
- `500 Internal Server Error`: Server error

---

### 3. Bulk Upsert Park-Activity Relationships

**Endpoint**: `POST /api/park-activities/upsert`

**Description**: Create, update, or delete multiple park-activity relationships in a single request. The operation processes all relationships and returns a summary with any errors encountered.

**Permissions**: `PERM_UPDATE_PARK` and `PERM_UPDATE_ACTIVITY`

**Request Body**:
```json
[
    {
        "activityId": "abc123",
        "parkId": "xyz789",
        "notes": "Available from 6 AM to 6 PM daily",
        "status": true
    },
    {
        "activityId": "def456",
        "parkId": "xyz789",
        "status": false
    },
    {
        "activityId": "ghi789",
        "parkId": "xyz789",
        "notes": "Requires advance booking",
        "status": true
    }
]
```

**Field Descriptions**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| activityId | String | Yes | Obfuscated activity ID |
| parkId | String | Yes | Obfuscated park ID |
| notes | String | No | Park-specific notes (only used when status is true) |
| status | Boolean | Yes | true = create/update relationship, false = delete relationship |

**Success Response (200)**:
```json
{
    "status": 200,
    "message": "Bulk upsert completed",
    "data": {
        "totalProcessed": 3,
        "successful": 2,
        "failed": 1,
        "errors": [
            "Relationship does not exist for park: Serengeti National Park, activity: Deep Sea Diving"
        ]
    }
}
```

**Response Field Descriptions**:
| Field | Type | Description |
|-------|------|-------------|
| totalProcessed | Integer | Total number of relationships processed |
| successful | Integer | Number of successful operations |
| failed | Integer | Number of failed operations |
| errors | Array[String] | List of error messages for failed operations |

**Possible Errors in Response**:
- `"Invalid ID format for park: {parkId}, activity: {activityId}"` - Invalid ID format
- `"Park not found: {parkId}"` - Park doesn't exist
- `"Activity not found: {activityId}"` - Activity doesn't exist
- `"Relationship does not exist for park: {parkName}, activity: {activityName}"` - Attempted to delete non-existent relationship
- `"Error processing relationship: {error message}"` - General processing error

**Error Responses**:
- `400 Bad Request`: Invalid request body or validation errors
- `500 Internal Server Error`: Server error

---

## Common Patterns

### Assignment Filter
The `assigned` parameter controls whether to show assigned or unassigned items:
```
GET /api/park-activities/parks/{id}/activities?assigned=true    # Activities assigned to this park (default)
GET /api/park-activities/parks/{id}/activities?assigned=false   # Activities NOT assigned to this park
GET /api/park-activities/activities/{id}/parks?assigned=true    # Parks assigned to this activity (default)
GET /api/park-activities/activities/{id}/parks?assigned=false   # Parks NOT assigned to this activity
```

### Filtering
All GET endpoints support multiple filter parameters that can be combined:
```
GET /api/park-activities/parks/{id}/activities?assigned=true&hasTariff=true&isActive=true
GET /api/park-activities/activities/{id}/parks?assigned=false&region=Arusha&parkType=NATIONAL_PARK
```

### Pagination
All list endpoints return paginated results:
```
GET /api/park-activities/parks/{id}/activities?page=0&size=20
```

### Sorting
All list endpoints sort by `createdAt` with configurable direction:
```
GET /api/park-activities/parks/{id}/activities?sortDirection=asc
```

### Search
Use the `keyword` parameter for broad searches:
```
GET /api/park-activities/parks/{id}/activities?keyword=wildlife
GET /api/park-activities/activities/{id}/parks?keyword=serengeti
```

---

## Implementation Notes

### Notes Field Behavior
- The `notes` field in responses contains park-specific or activity-specific information
- When getting activities for a park, `notes` describes how the activity works in that specific park
- When getting parks for an activity, `notes` describes park-specific details for that activity
- The `notes` field is optional and may be `null`
- **Important**: The `notes` field is only included when `assigned=true` (default). When `assigned=false` (unassigned items), the `notes` field will always be `null` since there is no relationship

### Upsert Operation Logic
1. **status: true** - Create or Update
   - If relationship exists: Updates the `notes` field
   - If relationship doesn't exist: Creates new relationship with provided `notes`
2. **status: false** - Delete
   - If relationship exists: Deletes the relationship
   - If relationship doesn't exist: Adds error to response but continues processing

### Error Handling
- The upsert endpoint never fails completely
- Each relationship is processed individually
- Errors are collected and returned in the response
- Successful operations are committed even if some operations fail

### Audit Logging
- All upsert operations are logged with `UPSERT_PARK_ACTIVITIES` action
- Individual creates/updates/deletes are logged in the service layer

---

## Example Use Cases

### 1. Display All Activities for a Park Detail Page
```bash
GET /api/park-activities/parks/xyz789/activities?isWebActive=true&isActive=true&size=50
```

### 2. Show All Parks Where Mountain Climbing is Available
```bash
GET /api/park-activities/activities/abc123/parks?isActive=true
```

### 3. Assign Multiple Activities to a New Park
```json
POST /api/park-activities/upsert
[
    {"activityId": "act1", "parkId": "park1", "notes": "Available daily", "status": true},
    {"activityId": "act2", "parkId": "park1", "notes": "Seasonal only", "status": true},
    {"activityId": "act3", "parkId": "park1", "status": true}
]
```

### 4. Update Notes for Existing Relationships
```json
POST /api/park-activities/upsert
[
    {"activityId": "act1", "parkId": "park1", "notes": "Updated availability info", "status": true}
]
```

### 5. Remove Multiple Activities from a Park
```json
POST /api/park-activities/upsert
[
    {"activityId": "act1", "parkId": "park1", "status": false},
    {"activityId": "act2", "parkId": "park1", "status": false}
]
```

### 6. Get Unassigned Activities for a Park (for adding new activities)
```bash
GET /api/park-activities/parks/xyz789/activities?assigned=false&isActive=true
```
This returns all active activities that are NOT yet assigned to the specified park, useful for showing available activities to add.

### 7. Get Unassigned Parks for an Activity (for expanding activity reach)
```bash
GET /api/park-activities/activities/abc123/parks?assigned=false&isActive=true&region=Arusha
```
This returns all active parks in Arusha region that do NOT yet offer the specified activity, useful for identifying expansion opportunities.
