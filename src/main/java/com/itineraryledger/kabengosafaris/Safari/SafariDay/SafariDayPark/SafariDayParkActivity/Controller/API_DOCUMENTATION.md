# Safari Day Park Activity API Documentation

## Overview

The Safari Day Park Activity API allows managing activities within safari park visits. This API supports creating, reading, updating, deleting, and reordering park activities with comprehensive Safari-specific operational tracking features.

**Base URL Pattern**: `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Key Features**:
- CRUD operations for park activities
- Reordering with comprehensive validation
- Safari state validation (editable states: DRAFT, CONFIRMED, IN_PROGRESS)
- Safari-specific operational tracking (completion status, sightings, guest experience)
- Dual update modes: Planning updates vs Operational updates
- Automatic sortOrder assignment and renumbering
- Duplicate activities allowed (e.g., morning and evening game drives)

---

## Endpoints

### 1. Add Park Activities

Create one or more activities for a safari park visit. The sortOrder is automatically assigned based on existing activities. Duplicate activities are allowed.

**Endpoint**: `POST /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID

**Request Body**:
```json
[
  {
    "parkId": "park123abc",
    "activityId": "abc123xyz",
    "durationHours": 2.5,
    "startTime": "06:00",
    "endTime": "08:30",
    "notes": "Morning game drive",
    "isIncludedInPrice": true
  },
  {
    "parkId": "park123abc",
    "activityId": "abc123xyz",
    "durationHours": 2.0,
    "startTime": "16:00",
    "endTime": "18:00",
    "notes": "Evening game drive",
    "isIncludedInPrice": true
  }
]
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| parkId | String | Yes | Obfuscated park ID (must match parent park visit) |
| activityId | String | Yes | Obfuscated activity ID (must be a valid ParkActivity for this park) |
| durationHours | BigDecimal | No | Duration in hours |
| startTime | String | No | Start time (e.g., "06:00") |
| endTime | String | No | End time (e.g., "08:30") |
| notes | String | No | Additional notes |
| isIncludedInPrice | Boolean | No | Whether activity is included in safari price (default: true) |

**Response**: `201 Created`
```json
{
  "success": true,
  "statusCode": 201,
  "message": "2 activities added",
  "data": [
    {
      "id": "ghi789rst",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Validation Rules**:
- Safari must exist and be in editable state (DRAFT, CONFIRMED, IN_PROGRESS)
- Park visit must exist and belong to the safari
- ParkActivity must exist (valid combination of park and activity)

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '[
    {
      "parkId": "park123",
      "activityId": "act456",
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Early morning game drive",
      "isIncludedInPrice": true
    }
  ]'
```

---

### 2. Get Park Activities

Retrieve all activities for a safari park visit, ordered by sortOrder.

**Endpoint**: `GET /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission**: `PERM_READ_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari park activities retrieved",
  "data": [
    {
      "id": "ghi789rst",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": true,
      "completedAt": "2025-01-18T08:45:00",
      "actualDurationHours": 2.75,
      "sightingsNotes": "Spotted leopard with cubs near watering hole. Also saw pride of lions, elephants, and giraffes.",
      "guestExperience": "Excellent experience! Guests were thrilled with the leopard sighting.",
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities" \
  -H "Authorization: Bearer {token}"
```

---

### 3. Get Single Park Activity

Retrieve a specific activity from a safari park visit.

**Endpoint**: `GET /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/{activityId}`

**Permission**: `PERM_READ_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID
- `activityId` (string, required) - Obfuscated activity entry ID

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari park activity retrieved",
  "data": {
    "id": "ghi789rst",
    "safariDayParkId": "parkVisitId123",
    "parkId": "park123abc",
    "parkName": "Serengeti National Park",
    "activityId": "abc123xyz",
    "activityName": "Game Drive",
    "sortOrder": 1,
    "durationHours": 2.5,
    "startTime": "06:00",
    "endTime": "08:30",
    "notes": "Morning game drive",
    "isIncludedInPrice": true,
    "isCompleted": true,
    "completedAt": "2025-01-18T08:45:00",
    "actualDurationHours": 2.75,
    "sightingsNotes": "Spotted leopard with cubs",
    "guestExperience": "Excellent experience",
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities/a012" \
  -H "Authorization: Bearer {token}"
```

---

### 4. Update Park Activity

Update an existing park activity. Supports both planning updates (require editable safari state) and operational updates (allowed anytime).

**Endpoint**: `PUT /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/{activityId}`

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID
- `activityId` (string, required) - Obfuscated activity entry ID

**Request Body** (all fields optional):
```json
{
  "durationHours": 2.75,
  "startTime": "06:00",
  "endTime": "08:45",
  "notes": "Extended morning drive",
  "isIncludedInPrice": true,
  "isCompleted": true,
  "actualDurationHours": 2.75,
  "sightingsNotes": "Spotted leopard with cubs near watering hole",
  "guestExperience": "Excellent experience, guests loved it",
  "isSkipped": false,
  "skipReason": null
}
```

**Request Fields**:
| Field | Type | Update Mode | Description |
|-------|------|-------------|-------------|
| durationHours | BigDecimal | Planning | Planned duration in hours |
| startTime | String | Planning | Planned start time |
| endTime | String | Planning | Planned end time |
| notes | String | Planning | General notes |
| isIncludedInPrice | Boolean | Planning | Whether included in safari price |
| isCompleted | Boolean | Operational | Whether activity was completed (auto-sets completedAt) |
| actualDurationHours | BigDecimal | Operational | Actual duration spent |
| sightingsNotes | String | Operational | Notable sightings during activity |
| guestExperience | String | Operational | Guest feedback/experience |
| isSkipped | Boolean | Operational | Whether activity was skipped |
| skipReason | String | Operational | Reason for skipping |

**Dual Update Modes**:
1. **Planning Updates** (require safari in editable state):
   - `durationHours`, `startTime`, `endTime`, `notes`, `isIncludedInPrice`
   - Safari must be in DRAFT, CONFIRMED, or IN_PROGRESS state

2. **Operational Updates** (allowed anytime, even if safari is completed):
   - `isCompleted`, `actualDurationHours`, `sightingsNotes`, `guestExperience`, `isSkipped`, `skipReason`
   - Can be performed during or after safari completion

**Auto-Timestamping**:
- When `isCompleted` is set to `true`, `completedAt` is automatically set to current timestamp
- When `isCompleted` is set to `false`, `completedAt` is cleared

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Safari park activity updated successfully",
  "data": {
    "id": "ghi789rst",
    "safariDayParkId": "parkVisitId123",
    "parkId": "park123abc",
    "parkName": "Serengeti National Park",
    "activityId": "abc123xyz",
    "activityName": "Game Drive",
    "sortOrder": 1,
    "durationHours": 2.75,
    "startTime": "06:00",
    "endTime": "08:45",
    "notes": "Extended morning drive",
    "isIncludedInPrice": true,
    "isCompleted": true,
    "completedAt": "2025-01-18T08:45:00",
    "actualDurationHours": 2.75,
    "sightingsNotes": "Spotted leopard with cubs near watering hole",
    "guestExperience": "Excellent experience, guests loved it",
    "isSkipped": false,
    "skipReason": null,
    "createdAt": "2025-01-18T10:30:00"
  }
}
```

**Example Request (Planning Update)**:
```bash
curl -X PUT "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities/a012" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "durationHours": 3.0,
    "notes": "Extended to 3 hours due to guest request"
  }'
```

**Example Request (Operational Update)**:
```bash
curl -X PUT "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities/a012" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "isCompleted": true,
    "actualDurationHours": 2.75,
    "sightingsNotes": "Spotted leopard with 2 cubs near watering hole. Also saw pride of 8 lions, herd of elephants.",
    "guestExperience": "Outstanding experience! Guests were thrilled with leopard sighting. Excellent guide performance."
  }'
```

---

### 5. Delete Park Activities

Delete one or more park activities from a safari park visit. After deletion, remaining activities are automatically renumbered.

**Endpoint**: `DELETE /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities`

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID

**Request Body**:
```json
[
  "activityId1",
  "activityId2",
  "activityId3"
]
```

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 activities deleted",
  "data": null
}
```

**Validation Rules**:
- Safari must be in editable state (DRAFT, CONFIRMED, IN_PROGRESS)
- Invalid or non-existent IDs are silently skipped
- Remaining activities are automatically renumbered (sortOrder: 1, 2, 3, ...)

**Example Request**:
```bash
curl -X DELETE "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '["a012", "a013", "a014"]'
```

---

### 6. Reorder Park Activities

Reorder activities within a safari park visit. Comprehensive validation ensures all activities are accounted for with no duplicates.

**Endpoint**: `POST /api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/activities/reorder`

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_ACTIVITY`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `parkVisitId` (string, required) - Obfuscated park visit ID

**Request Body**:
```json
{
  "activityOrder": [
    {
      "activityId": "a012",
      "expectedSortOrder": 1
    },
    {
      "activityId": "a013",
      "expectedSortOrder": 2
    },
    {
      "activityId": "a014",
      "expectedSortOrder": 3
    }
  ]
}
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| activityOrder | Array | Yes | List of activities in new order |
| activityOrder[].activityId | String | Yes | Obfuscated activity entry ID |
| activityOrder[].expectedSortOrder | Integer | No | Expected sort order for validation |

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activities reordered successfully",
  "data": [
    {
      "id": "a012",
      "safariDayParkId": "parkVisitId123",
      "parkId": "park123abc",
      "parkName": "Serengeti National Park",
      "activityId": "abc123xyz",
      "activityName": "Game Drive",
      "sortOrder": 1,
      "durationHours": 2.5,
      "startTime": "06:00",
      "endTime": "08:30",
      "notes": "Morning game drive",
      "isIncludedInPrice": true,
      "isCompleted": false,
      "completedAt": null,
      "actualDurationHours": null,
      "sightingsNotes": null,
      "guestExperience": null,
      "isSkipped": false,
      "skipReason": null,
      "createdAt": "2025-01-18T10:30:00"
    }
  ]
}
```

**Validation Rules**:
- Safari must be in editable state
- Activity order list must contain exactly the same count as existing activities
- All activity IDs must be valid and belong to the park visit
- No duplicate activity IDs allowed
- No missing activities allowed
- Expected sort orders (if provided) must match positions

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/safaris/s123/days/d456/parks/p789/activities/reorder" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "activityOrder": [
      {"activityId": "a013", "expectedSortOrder": 1},
      {"activityId": "a012", "expectedSortOrder": 2},
      {"activityId": "a014", "expectedSortOrder": 3}
    ]
  }'
```

---

## Data Models

### SafariDayParkActivityDTO

```typescript
{
  id: string;                      // Obfuscated activity entry ID
  safariDayParkId: string;         // Obfuscated park visit ID
  parkId: string;                  // Obfuscated park ID
  parkName: string;                // Park name
  activityId: string;              // Obfuscated activity ID
  activityName: string;            // Activity name
  sortOrder: number;               // Display order
  durationHours: number | null;    // Planned duration
  startTime: string | null;        // Planned start time (HH:mm)
  endTime: string | null;          // Planned end time (HH:mm)
  notes: string | null;            // General notes
  isIncludedInPrice: boolean;      // Whether included in safari price

  // Safari-specific operational fields
  isCompleted: boolean;            // Whether activity was completed
  completedAt: string | null;      // ISO timestamp when completed (auto-set)
  actualDurationHours: number | null;  // Actual duration spent
  sightingsNotes: string | null;   // Notable sightings during activity
  guestExperience: string | null;  // Guest feedback/experience
  isSkipped: boolean;              // Whether activity was skipped
  skipReason: string | null;       // Reason for skipping

  createdAt: string;               // ISO timestamp
}
```

### CreateSafariDayParkActivityDTO

```typescript
{
  parkId: string;                  // Required - Obfuscated park ID
  activityId: string;              // Required - Obfuscated activity ID
  durationHours?: number;          // Optional - Planned duration
  startTime?: string;              // Optional - Planned start time
  endTime?: string;                // Optional - Planned end time
  notes?: string;                  // Optional - General notes
  isIncludedInPrice?: boolean;     // Optional - Default: true
}
```

### UpdateSafariDayParkActivityDTO

```typescript
{
  // Planning fields (require editable safari)
  durationHours?: number;
  startTime?: string;
  endTime?: string;
  notes?: string;
  isIncludedInPrice?: boolean;

  // Operational fields (allowed anytime)
  isCompleted?: boolean;           // Auto-sets completedAt when true
  actualDurationHours?: number;
  sightingsNotes?: string;
  guestExperience?: string;
  isSkipped?: boolean;
  skipReason?: string;
}
```

### ReorderSafariDayParkActivitiesDTO

```typescript
{
  activityOrder: Array<{
    activityId: string;            // Required - Obfuscated activity entry ID
    expectedSortOrder?: number;    // Optional - For validation
  }>;
}
```

---

## Business Rules

### Safari State Validation

Park activities can only be created, deleted, or reordered when the safari is in an editable state. Planning updates also require editable state.

**Editable States**:
- `DRAFT` - Safari is being planned
- `CONFIRMED` - Safari is confirmed but not yet started
- `IN_PROGRESS` - Safari is currently running

**Non-Editable States**:
- `COMPLETED` - Safari has finished
- `CANCELLED` - Safari was cancelled
- `ARCHIVED` - Safari is archived

**Exception**: Operational updates (completion status, sightings, guest feedback) can be performed even when safari is in non-editable states, allowing post-safari tracking and reporting.

### Dual Update Modes

The update endpoint supports two distinct modes of operation:

**Planning Updates** (Require Editable Safari):
- Fields: `durationHours`, `startTime`, `endTime`, `notes`, `isIncludedInPrice`
- Purpose: Pre-safari planning and schedule adjustments
- Restriction: Can only be performed when safari is in editable state
- Use case: Before or during safari for planning changes

**Operational Updates** (Allowed Anytime):
- Fields: `isCompleted`, `actualDurationHours`, `sightingsNotes`, `guestExperience`, `isSkipped`, `skipReason`
- Purpose: Real-time and post-safari operational tracking
- Restriction: None - can be performed anytime, even after safari completion
- Use case: During safari for real-time tracking, after safari for reporting

### Auto-Timestamping

When updating certain fields, the system automatically sets timestamps:

- **isCompleted = true**: Automatically sets `completedAt` to current timestamp
- **isCompleted = false**: Clears `completedAt` (sets to null)
- **createdAt**: Set automatically on creation
- **updatedAt**: Updated automatically on every change (if tracked)

### Automatic Ordering

- **sortOrder** is automatically assigned when creating activities (increments from current max)
- After deletion, remaining activities are automatically renumbered (1, 2, 3, ...)
- Use the reorder endpoint to change activity sequence
- Two-pass approach prevents unique constraint violations during reordering

### Duplicate Activities Allowed

The same activity can be added multiple times to a park visit:
- Example: Morning game drive (06:00-08:30) and Evening game drive (16:00-18:00)
- Each entry is a separate record with its own ID, sortOrder, and tracking data
- Useful for activities that occur multiple times per day

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `INVALID_ID` | 400 | Invalid or malformed ID format |
| `SAFARI_PARK_VISIT_NOT_FOUND` | 404 | Park visit does not exist |
| `SAFARI_NOT_EDITABLE` | 400 | Safari is not in an editable state (for planning updates/deletions) |
| `SAFARI_PARK_ACTIVITY_NOT_FOUND` | 404 | Park activity entry does not exist |
| `ACTIVITY_PARK_VISIT_MISMATCH` | 400 | Activity does not belong to specified park visit |
| `NO_ACTIVITIES_TO_REORDER` | 400 | Park visit has no activities to reorder |
| `ACTIVITY_COUNT_MISMATCH` | 400 | Activity order list count doesn't match existing activities |
| `INVALID_PARK_VISIT_ID` | 400 | Invalid park visit ID format |
| `SAFARI_PARK_ACTIVITIES_ADD_FAILED` | 500 | Failed to add park activities |
| `SAFARI_DAY_PARK_ACTIVITY_UPDATE_FAILED` | 500 | Failed to update park activity |
| `DELETE_FAILED` | 500 | Failed to delete park activities |
| `FETCH_FAILED` | 500 | Failed to fetch park activities |

---

## Example Use Cases

### Example 1: Basic Activity Creation

**Scenario**: Add morning and evening game drives to Serengeti park visit

```bash
POST /api/safaris/s123/days/d456/parks/p789/activities
[
  {
    "parkId": "park123",
    "activityId": "act456",
    "durationHours": 2.5,
    "startTime": "06:00",
    "endTime": "08:30",
    "notes": "Morning game drive",
    "isIncludedInPrice": true
  },
  {
    "parkId": "park123",
    "activityId": "act456",
    "durationHours": 2.0,
    "startTime": "16:00",
    "endTime": "18:00",
    "notes": "Evening game drive",
    "isIncludedInPrice": true
  }
]
```

Response: Returns both activities with sortOrder 1 and 2, all operational fields initialized to defaults.

---

### Example 2: Mark Activity as Completed During Safari

**Scenario**: Update activity with completion and sightings after morning game drive

```bash
PUT /api/safaris/s123/days/d456/parks/p789/activities/a012
{
  "isCompleted": true,
  "actualDurationHours": 2.75,
  "sightingsNotes": "Spotted leopard with 2 cubs near watering hole at 07:15. Pride of 8 lions resting under acacia tree. Herd of 20 elephants crossing the river.",
  "guestExperience": "Outstanding experience! Guests were thrilled with the leopard sighting. Guide performance was excellent."
}
```

Response: Returns activity with `completedAt` automatically set to current timestamp.

**Note**: This is an operational update, so it works even if safari is completed.

---

### Example 3: Update Planning Details Before Safari

**Scenario**: Extend morning game drive duration before safari starts

```bash
PUT /api/safaris/s123/days/d456/parks/p789/activities/a012
{
  "durationHours": 3.0,
  "endTime": "09:00",
  "notes": "Extended to 3 hours per guest request"
}
```

**Requirements**: Safari must be in editable state (DRAFT, CONFIRMED, or IN_PROGRESS).

**If safari is completed**: Returns error
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE"
}
```

---

### Example 4: Record Skipped Activity

**Scenario**: Mark activity as skipped with reason

```bash
PUT /api/safaris/s123/days/d456/parks/p789/activities/a013
{
  "isSkipped": true,
  "skipReason": "Heavy rainfall made roads impassable. Activity rescheduled to next day."
}
```

Response: Returns activity with skip status recorded.

---

### Example 5: Reorder Activities

**Scenario**: Swap order of two activities

**Current order**:
1. Morning game drive (a012)
2. Walking safari (a013)
3. Evening game drive (a014)

**Desired order**:
1. Walking safari (a013)
2. Morning game drive (a012)
3. Evening game drive (a014)

```bash
POST /api/safaris/s123/days/d456/parks/p789/activities/reorder
{
  "activityOrder": [
    {"activityId": "a013", "expectedSortOrder": 1},
    {"activityId": "a012", "expectedSortOrder": 2},
    {"activityId": "a014", "expectedSortOrder": 3}
  ]
}
```

Response: Returns activities with new sortOrder values.

---

### Example 6: Bulk Delete Activities

**Scenario**: Remove all activities and start fresh

**Step 1**: Get all activities
```bash
GET /api/safaris/s123/days/d456/parks/p789/activities
```

**Step 2**: Delete all activities
```bash
DELETE /api/safaris/s123/days/d456/parks/p789/activities
["a012", "a013", "a014"]
```

Response: Returns count of deleted activities, remaining activities are renumbered.

---

### Example 7: Combined Planning and Operational Update

**Scenario**: Update both planning and operational fields (requires editable safari)

```bash
PUT /api/safaris/s123/days/d456/parks/p789/activities/a012
{
  "durationHours": 3.0,
  "notes": "Extended per guest request",
  "isCompleted": true,
  "actualDurationHours": 3.25,
  "sightingsNotes": "Amazing leopard sighting",
  "guestExperience": "Guests loved it"
}
```

**Validation**: Safari must be editable because planning fields are included. If only operational fields were provided, safari state wouldn't matter.

---

## Notes

- All IDs in the API are obfuscated for security
- The parkActivity reference (park + activity combination) cannot be changed after creation - delete and recreate if needed
- sortOrder cannot be directly updated - use the reorder endpoint
- Planning updates require editable safari state; operational updates do not
- The completedAt timestamp is automatically managed based on isCompleted value
- Duplicate activities are allowed and useful for recurring activities (e.g., multiple game drives per day)
- After deletion, remaining activities are automatically renumbered to maintain sequential sortOrder
- Reorder validation is comprehensive: checks for missing activities, duplicates, and wrong park visit associations
- Consider safari state when performing operations: planning operations need editable state, operational tracking does not
