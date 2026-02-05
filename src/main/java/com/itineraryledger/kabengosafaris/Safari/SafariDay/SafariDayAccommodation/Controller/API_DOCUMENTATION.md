# Safari Day Accommodation API Documentation

## Overview

The Safari Day Accommodation API allows managing accommodation bookings for safari days. This API supports creating, reading, updating, and deleting accommodation entries, including room configuration (type, standard, board type) and Safari-specific operational tracking (booking confirmations, check-in/out times, guest feedback, etc.).

**Base URL Pattern**: `/api/safaris/{safariId}/days/{dayId}/accommodations`

**Key Features**:
- CRUD operations for safari day accommodations
- Primary and alternative accommodation support
- Pax capacity validation (prevents overbooking)
- Safari state validation (editable states: DRAFT, CONFIRMED, IN_PROGRESS)
- Safari-specific booking tracking (confirmation numbers, check-in/out times, room assignments, guest feedback)
- Booking status management (PENDING, CONFIRMED, CANCELLED, NO_SHOW, COMPLETED)
- Dual update modes: Planning updates (requires editable state) vs Operational updates (allowed anytime)
- Bulk deletion support

---

## Endpoints

### 1. Create Safari Day Accommodation

Create a new accommodation entry for a safari day with specified room configuration.

**Endpoint**: `POST /api/safaris/{safariId}/days/{dayId}/accommodations`

**Permission**: `PERM_CREATE_SAFARI_DAY_ACCOMMODATION`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID

**Request Body**:
```json
{
  "accommodationId": "string (required)",
  "roomTypeId": "string (required)",
  "roomStandardId": "string (required)",
  "boardTypeId": "string (required)",
  "roomCount": 1,
  "isAlternative": false,
  "notes": "string (optional)"
}
```

**Response**: `201 Created`
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Accommodation created successfully",
  "data": {
    "id": "abc123",
    "safariDayId": "day456",
    "accommodationId": "acc789",
    "accommodationName": "Serena Safari Lodge",
    "accommodationSlug": "serena-safari-lodge",
    "roomTypeId": "rt001",
    "roomTypeName": "Double Room",
    "roomStandardId": "rs001",
    "roomStandardName": "Deluxe",
    "boardTypeId": "bt001",
    "boardTypeName": "Full Board",
    "roomCount": 2,
    "isAlternative": false,
    "notes": "Rooms with lake view preferred",
    "confirmationNumber": null,
    "confirmedAt": null,
    "checkInTime": null,
    "checkOutTime": null,
    "roomNumbers": null,
    "guestFeedback": null,
    "specialArrangements": null,
    "bookingStatus": "PENDING",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Validation Rules**:
- Safari must exist and be in editable state (DRAFT, CONFIRMED, IN_PROGRESS)
- Accommodation, room type, room standard, and board type must exist
- Room count must be at least 1
- Pax capacity validation: Minimum occupancy cannot exceed total pax count (prevents overbooking)
- Only applies to primary accommodations (isAlternative = false)

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/safaris/s123/days/d456/accommodations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "accommodationId": "acc789",
    "roomTypeId": "rt001",
    "roomStandardId": "rs002",
    "boardTypeId": "bt003",
    "roomCount": 2,
    "isAlternative": false,
    "notes": "Request ground floor rooms if available"
  }'
```

---

### 2. Get Safari Day Accommodations

Retrieve all accommodations for a safari day.

**Endpoint**: `GET /api/safaris/{safariId}/days/{dayId}/accommodations`

**Permission**: `PERM_READ_SAFARI_DAY_ACCOMMODATION`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Accommodations retrieved successfully",
  "data": [
    {
      "id": "abc123",
      "safariDayId": "day456",
      "accommodationId": "acc789",
      "accommodationName": "Serena Safari Lodge",
      "accommodationSlug": "serena-safari-lodge",
      "roomTypeId": "rt001",
      "roomTypeName": "Double Room",
      "roomStandardId": "rs001",
      "roomStandardName": "Deluxe",
      "boardTypeId": "bt001",
      "boardTypeName": "Full Board",
      "roomCount": 2,
      "isAlternative": false,
      "notes": "Rooms with lake view",
      "confirmationNumber": "SER-2024-12345",
      "confirmedAt": "2024-01-15T14:30:00",
      "checkInTime": "14:30",
      "checkOutTime": "10:00",
      "roomNumbers": "101, 102",
      "guestFeedback": "Excellent service and beautiful rooms",
      "specialArrangements": "Early check-in arranged",
      "bookingStatus": "CONFIRMED",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T14:30:00"
    },
    {
      "id": "abc124",
      "safariDayId": "day456",
      "accommodationId": "acc790",
      "accommodationName": "Mara Intrepids Camp",
      "accommodationSlug": "mara-intrepids-camp",
      "roomTypeId": "rt002",
      "roomTypeName": "Twin Tent",
      "roomStandardId": "rs001",
      "roomStandardName": "Standard",
      "boardTypeId": "bt001",
      "boardTypeName": "Full Board",
      "roomCount": 1,
      "isAlternative": true,
      "notes": "Alternative option if Serena is full",
      "confirmationNumber": null,
      "confirmedAt": null,
      "checkInTime": null,
      "checkOutTime": null,
      "roomNumbers": null,
      "guestFeedback": null,
      "specialArrangements": null,
      "bookingStatus": "PENDING",
      "createdAt": "2024-01-15T10:35:00",
      "updatedAt": "2024-01-15T10:35:00"
    }
  ]
}
```

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/safaris/s123/days/d456/accommodations" \
  -H "Authorization: Bearer {token}"
```

---

### 3. Update Safari Day Accommodation

Update an existing accommodation entry. Supports both planning updates (room count, alternative status, notes) and operational updates (booking confirmation, check-in/out, guest feedback).

**Endpoint**: `PUT /api/safaris/{safariId}/days/{dayId}/accommodations/{accommodationId}`

**Permission**: `PERM_UPDATE_SAFARI_DAY_ACCOMMODATION`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `accommodationId` (string, required) - Obfuscated accommodation entry ID

**Request Body** (all fields optional):
```json
{
  "roomCount": 2,
  "isAlternative": false,
  "notes": "string (optional)",
  "confirmationNumber": "string (optional)",
  "checkInTime": "14:30",
  "checkOutTime": "10:00",
  "roomNumbers": "101, 102, 103",
  "guestFeedback": "string (optional)",
  "specialArrangements": "string (optional)",
  "bookingStatus": "CONFIRMED"
}
```

**Booking Status Values**:
- `PENDING` - Booking request sent, awaiting confirmation
- `CONFIRMED` - Booking confirmed by accommodation
- `CANCELLED` - Booking was cancelled
- `NO_SHOW` - Guest did not show up
- `COMPLETED` - Stay was completed

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Accommodation updated successfully",
  "data": {
    "id": "abc123",
    "safariDayId": "day456",
    "accommodationId": "acc789",
    "accommodationName": "Serena Safari Lodge",
    "accommodationSlug": "serena-safari-lodge",
    "roomTypeId": "rt001",
    "roomTypeName": "Double Room",
    "roomStandardId": "rs001",
    "roomStandardName": "Deluxe",
    "boardTypeId": "bt001",
    "boardTypeName": "Full Board",
    "roomCount": 3,
    "isAlternative": false,
    "notes": "Updated room count due to additional guest",
    "confirmationNumber": "SER-2024-12345",
    "confirmedAt": "2024-01-15T14:30:00",
    "checkInTime": "14:30",
    "checkOutTime": "10:00",
    "roomNumbers": "101, 102, 103",
    "guestFeedback": "Excellent stay",
    "specialArrangements": "Early check-in arranged",
    "bookingStatus": "COMPLETED",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-20T09:15:00"
  }
}
```

**Dual Update Modes**:
1. **Planning Updates** (require safari in editable state):
   - `roomCount`
   - `isAlternative`
   - `notes`

2. **Operational Updates** (allowed anytime, even if safari is completed):
   - `confirmationNumber` (auto-sets `confirmedAt` timestamp)
   - `checkInTime`
   - `checkOutTime`
   - `roomNumbers`
   - `guestFeedback`
   - `specialArrangements`
   - `bookingStatus`

**Example Request (Planning Update)**:
```bash
curl -X PUT "http://localhost:8080/api/safaris/s123/days/d456/accommodations/a789" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "roomCount": 3,
    "notes": "Added one more room for guide"
  }'
```

**Example Request (Operational Update)**:
```bash
curl -X PUT "http://localhost:8080/api/safaris/s123/days/d456/accommodations/a789" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "confirmationNumber": "SER-2024-12345",
    "bookingStatus": "CONFIRMED",
    "checkInTime": "14:30",
    "checkOutTime": "10:00",
    "roomNumbers": "101, 102, 103",
    "specialArrangements": "Early check-in arranged for 12:00"
  }'
```

---

### 4. Delete Safari Day Accommodation

Delete a single accommodation entry from a safari day.

**Endpoint**: `DELETE /api/safaris/{safariId}/days/{dayId}/accommodations/{accommodationId}`

**Permission**: `PERM_DELETE_SAFARI_DAY_ACCOMMODATION`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID
- `accommodationId` (string, required) - Obfuscated accommodation entry ID

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Accommodation deleted successfully",
  "data": null
}
```

**Validation Rules**:
- Safari must be in editable state (DRAFT, CONFIRMED, IN_PROGRESS)
- Accommodation must belong to the specified day and safari

**Example Request**:
```bash
curl -X DELETE "http://localhost:8080/api/safaris/s123/days/d456/accommodations/a789" \
  -H "Authorization: Bearer {token}"
```

---

### 5. Bulk Delete Safari Day Accommodations

Delete multiple accommodation entries at once.

**Endpoint**: `DELETE /api/safaris/{safariId}/days/{dayId}/accommodations`

**Permission**: `PERM_DELETE_SAFARI_DAY_ACCOMMODATION`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated safari ID
- `dayId` (string, required) - Obfuscated safari day ID

**Request Body**:
```json
[
  "accommodationId1",
  "accommodationId2",
  "accommodationId3"
]
```

**Response**: `200 OK`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 accommodation(s) deleted successfully",
  "data": null
}
```

**Validation Rules**:
- Safari must be in editable state
- Only accommodations belonging to the specified day and safari will be deleted
- Invalid or non-existent IDs are silently skipped

**Example Request**:
```bash
curl -X DELETE "http://localhost:8080/api/safaris/s123/days/d456/accommodations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '["a789", "a790", "a791"]'
```

---

## Data Models

### SafariDayAccommodationDTO

```typescript
{
  id: string;                    // Obfuscated accommodation entry ID
  safariDayId: string;           // Obfuscated safari day ID
  accommodationId: string;       // Obfuscated base accommodation ID
  accommodationName: string;     // Name of the accommodation
  accommodationSlug: string;     // URL-friendly slug
  roomTypeId: string;            // Obfuscated room type ID
  roomTypeName: string;          // e.g., "Single", "Double", "Twin"
  roomStandardId: string;        // Obfuscated room standard ID
  roomStandardName: string;      // e.g., "Standard", "Deluxe", "Suite"
  boardTypeId: string;           // Obfuscated board type ID
  boardTypeName: string;         // e.g., "Room Only", "B&B", "Half Board", "Full Board"
  roomCount: number;             // Number of rooms booked
  isAlternative: boolean;        // True if backup/alternative option
  notes: string | null;          // General notes

  // Safari-specific fields
  confirmationNumber: string | null;     // Booking confirmation from accommodation
  confirmedAt: string | null;            // ISO timestamp when booking was confirmed
  checkInTime: string | null;            // Actual check-in time (HH:mm)
  checkOutTime: string | null;           // Actual check-out time (HH:mm)
  roomNumbers: string | null;            // Assigned room numbers (comma-separated)
  guestFeedback: string | null;          // Guest feedback about the accommodation
  specialArrangements: string | null;    // Special arrangements or requests
  bookingStatus: string;                 // PENDING, CONFIRMED, CANCELLED, NO_SHOW, COMPLETED

  createdAt: string;             // ISO timestamp
  updatedAt: string;             // ISO timestamp
}
```

### CreateSafariDayAccommodationDTO

```typescript
{
  accommodationId: string;       // Required - Obfuscated base accommodation ID
  roomTypeId: string;            // Required - Obfuscated room type ID
  roomStandardId: string;        // Required - Obfuscated room standard ID
  boardTypeId: string;           // Required - Obfuscated board type ID
  roomCount?: number;            // Optional - Default: 1, Min: 1
  isAlternative?: boolean;       // Optional - Default: false
  notes?: string;                // Optional
}
```

### UpdateSafariDayAccommodationDTO

```typescript
{
  // Planning fields (require editable safari)
  roomCount?: number;            // Min: 1
  isAlternative?: boolean;
  notes?: string;

  // Operational fields (allowed anytime)
  confirmationNumber?: string;
  checkInTime?: string;          // Format: HH:mm
  checkOutTime?: string;         // Format: HH:mm
  roomNumbers?: string;          // Comma-separated room numbers
  guestFeedback?: string;
  specialArrangements?: string;
  bookingStatus?: string;        // PENDING, CONFIRMED, CANCELLED, NO_SHOW, COMPLETED
}
```

---

## Business Rules

### Safari State Validation

Accommodations can only be created, updated (planning fields), or deleted when the safari is in an editable state:

**Editable States**:
- `DRAFT` - Safari is being planned
- `CONFIRMED` - Safari is confirmed but not yet started
- `IN_PROGRESS` - Safari is currently running

**Non-Editable States**:
- `COMPLETED` - Safari has finished
- `CANCELLED` - Safari was cancelled
- `ARCHIVED` - Safari is archived

**Exception**: Operational updates (booking confirmation, check-in/out times, guest feedback, booking status) can be performed even when safari is in non-editable states.

### Primary vs Alternative Accommodations

- **Primary** (`isAlternative = false`): The main accommodation option for the day
- **Alternative** (`isAlternative = true`): Backup options if primary is unavailable

**Validation Differences**:
- Pax capacity validation only applies to primary accommodations
- Both types are included in accommodation lists
- Alternative accommodations are typically not confirmed until needed

### Pax Capacity Validation

When creating or updating primary accommodations, the system validates:

**Overbooking Prevention**:
- Sum of (room count × minimum occupancy) for all primary accommodations
- Must NOT exceed total safari pax count
- Prevents booking more rooms than needed

**Example**:
- Total Pax: 6
- Room Type: Double (min occupancy = 2)
- Attempting to book 4 Double rooms = 4 × 2 = 8 minimum occupancy
- **Validation Fails**: Minimum occupancy (8) > Total Pax (6)
- System prevents overbooking

**Note**: Underbooking (capacity < pax) is NOT validated at this level because users may need to add multiple accommodations progressively. This should be validated at safari level when publishing/confirming.

### Booking Status Lifecycle

Typical booking status progression:

```
PENDING → CONFIRMED → COMPLETED
                ↓
           CANCELLED / NO_SHOW
```

**Status Descriptions**:
- **PENDING**: Initial state when accommodation is added
- **CONFIRMED**: Accommodation has confirmed the booking (usually after receiving confirmation number)
- **CANCELLED**: Booking was cancelled before the stay
- **NO_SHOW**: Guest did not show up for confirmed booking
- **COMPLETED**: Stay was successfully completed

### Auto-Timestamping

When updating certain fields, the system automatically sets timestamps:

- **Confirmation Number Added**: Automatically sets `confirmedAt` to current timestamp if not already set
- **Created**: `createdAt` is set automatically on creation
- **Updated**: `updatedAt` is updated automatically on every change

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `INVALID_ID` | 400 | Invalid or malformed ID format |
| `SAFARI_DAY_NOT_FOUND` | 404 | Safari day does not exist |
| `DAY_SAFARI_MISMATCH` | 400 | Day does not belong to specified safari |
| `SAFARI_NOT_EDITABLE` | 400 | Safari is not in an editable state (for planning updates) |
| `ACCOMMODATION_NOT_FOUND` | 404 | Base accommodation does not exist |
| `ROOM_TYPE_NOT_FOUND` | 404 | Room type does not exist |
| `ROOM_STANDARD_NOT_FOUND` | 404 | Room standard does not exist |
| `BOARD_TYPE_NOT_FOUND` | 404 | Board type does not exist |
| `ACCOMMODATION_OVERBOOKING` | 400 | Room booking exceeds pax capacity |
| `INVALID_BOOKING_STATUS` | 400 | Invalid booking status value |
| `ACCOMMODATION_DAY_MISMATCH` | 400 | Accommodation does not belong to specified day |
| `OWNERSHIP_MISMATCH` | 400 | Accommodation does not belong to specified day/safari |
| `ACCOMMODATION_CREATE_FAILED` | 500 | Failed to create accommodation |
| `ACCOMMODATIONS_FETCH_FAILED` | 500 | Failed to fetch accommodations |
| `ACCOMMODATION_UPDATE_FAILED` | 500 | Failed to update accommodation |
| `ACCOMMODATION_DELETE_FAILED` | 500 | Failed to delete accommodation |
| `ACCOMMODATIONS_DELETE_FAILED` | 500 | Failed to delete multiple accommodations |

---

## Example Use Cases

### Example 1: Basic Accommodation Booking

**Scenario**: Add primary accommodation for Day 3 of safari

**Step 1**: Create accommodation
```bash
POST /api/safaris/s123/days/d456/accommodations
{
  "accommodationId": "acc789",
  "roomTypeId": "rt001",      // Double Room
  "roomStandardId": "rs002",  // Deluxe
  "boardTypeId": "bt003",     // Full Board
  "roomCount": 3,
  "isAlternative": false,
  "notes": "Prefer rooms near the pool area"
}
```

Response: Returns accommodation with `bookingStatus: "PENDING"`

---

### Example 2: Add Alternative Accommodation

**Scenario**: Add backup accommodation option in case primary is full

**Step 1**: Create alternative accommodation
```bash
POST /api/safaris/s123/days/d456/accommodations
{
  "accommodationId": "acc790",
  "roomTypeId": "rt002",      // Twin Room
  "roomStandardId": "rs001",  // Standard
  "boardTypeId": "bt003",     // Full Board
  "roomCount": 3,
  "isAlternative": true,
  "notes": "Use this if Serena is fully booked"
}
```

Response: Returns alternative accommodation with `isAlternative: true`

---

### Example 3: Confirm Booking

**Scenario**: Update accommodation with confirmation details after receiving confirmation from property

**Step 1**: Update with confirmation (operational update - allowed anytime)
```bash
PUT /api/safaris/s123/days/d456/accommodations/a789
{
  "confirmationNumber": "SER-2024-12345",
  "bookingStatus": "CONFIRMED",
  "specialArrangements": "Early check-in at 12:00 arranged"
}
```

Response: Returns accommodation with:
- `confirmationNumber: "SER-2024-12345"`
- `confirmedAt: "2024-01-15T14:30:00"` (auto-set)
- `bookingStatus: "CONFIRMED"`

---

### Example 4: Record Check-In Details

**Scenario**: Record actual check-in details and room assignments during safari

**Step 1**: Update with check-in details (operational update)
```bash
PUT /api/safaris/s123/days/d456/accommodations/a789
{
  "checkInTime": "14:30",
  "roomNumbers": "101, 102, 103"
}
```

Response: Returns accommodation with check-in details

---

### Example 5: Complete Stay with Feedback

**Scenario**: Mark accommodation as completed and add guest feedback after check-out

**Step 1**: Update with completion details (operational update)
```bash
PUT /api/safaris/s123/days/d456/accommodations/a789
{
  "checkOutTime": "10:00",
  "bookingStatus": "COMPLETED",
  "guestFeedback": "Excellent property with amazing views. Staff was very friendly and helpful. Food quality was outstanding."
}
```

Response: Returns accommodation with:
- `checkOutTime: "10:00"`
- `bookingStatus: "COMPLETED"`
- `guestFeedback` recorded

---

### Example 6: Modify Room Count Before Safari Starts

**Scenario**: Increase room count due to additional guests (planning update)

**Requirements**: Safari must be in editable state (DRAFT, CONFIRMED, or IN_PROGRESS)

**Step 1**: Update room count
```bash
PUT /api/safaris/s123/days/d456/accommodations/a789
{
  "roomCount": 4,
  "notes": "Added one more room for additional guide"
}
```

**If safari is not editable**: Returns error
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE"
}
```

---

### Example 7: Handle Overbooking Validation

**Scenario**: System prevents booking more rooms than needed for pax count

**Setup**:
- Total Safari Pax: 6
- Existing: 2 Double rooms (min occupancy = 2 each) = 4 minimum occupancy

**Step 1**: Attempt to add 2 more Double rooms
```bash
POST /api/safaris/s123/days/d456/accommodations
{
  "accommodationId": "acc789",
  "roomTypeId": "rt001",      // Double Room (min occupancy = 2)
  "roomStandardId": "rs001",
  "boardTypeId": "bt001",
  "roomCount": 2
}
```

**System calculates**:
- Existing minimum occupancy: 2 rooms × 2 = 4
- New minimum occupancy: 2 rooms × 2 = 4
- Total minimum occupancy: 4 + 4 = 8
- Total Pax: 6

**Result**: Validation fails
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Accommodation overbooking detected. Total pax: 6, Minimum room occupancy required: 8. You are booking more rooms than needed for your passengers. Please reduce room count.",
  "errorCode": "ACCOMMODATION_OVERBOOKING"
}
```

**Solution**: Reduce room count or choose room type with higher max occupancy

---

### Example 8: Bulk Delete Accommodations

**Scenario**: Remove all accommodations for a day to start fresh

**Step 1**: Get all accommodations
```bash
GET /api/safaris/s123/days/d456/accommodations
```

**Step 2**: Delete all accommodations
```bash
DELETE /api/safaris/s123/days/d456/accommodations
["a789", "a790", "a791"]
```

Response: Returns count of deleted accommodations

---

## Notes

- All IDs in the API are obfuscated for security
- Accommodation configuration (accommodation, room type, room standard, board type) cannot be changed after creation - delete and recreate if needed
- Planning updates (room count, alternative status, notes) require safari to be in editable state
- Operational updates (booking details, check-in/out, feedback) can be done anytime
- Pax capacity validation only applies to primary (non-alternative) accommodations
- The `confirmedAt` timestamp is automatically set when a confirmation number is first added
- Booking status can be updated directly via API or is automatically updated when using helper methods
- Alternative accommodations are useful for maintaining backup options but don't trigger capacity validation
- Consider safari state when planning updates vs operational tracking
