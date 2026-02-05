# Safari API Documentation

## Overview
The Safari API provides endpoints for managing safari bookings created from itinerary templates. Safaris represent actual bookings with concrete dates, customers, and state tracking throughout the booking lifecycle.

**Base URL:** `/api/safaris`

**Key Concepts:**
- **Safari**: An actual safari booking instance created from an itinerary template
- **Safari State**: Booking/operational state (manually managed) - e.g., DRAFT, CONFIRMED, IN_PROGRESS, COMPLETED
- **Safari Phase**: Time-based phase (automatically calculated) - e.g., UPCOMING, STARTING_SOON, IN_PROGRESS, PAST
- **Customer Relationship**: Every safari must be linked to a customer
- **Itinerary Template**: Safaris are created from published itinerary templates
- **Date Recalculation**: When start date changes, all safari day dates are automatically recalculated

---

## Table of Contents
1. [Authentication & Permissions](#authentication--permissions)
2. [Data Transfer Objects (DTOs)](#data-transfer-objects-dtos)
3. [Safari States & Phases](#safari-states--phases)
4. [API Endpoints](#api-endpoints)
5. [Business Rules](#business-rules)
6. [Common Use Cases](#common-use-cases)
7. [Error Handling](#error-handling)

---

## Authentication & Permissions

All endpoints require authentication and specific permissions:

| Permission | Description |
|-----------|-------------|
| `PERM_CREATE_SAFARI` | Create new safari bookings |
| `PERM_READ_SAFARI` | View safari details and lists |
| `PERM_UPDATE_SAFARI` | Update safari information |
| `PERM_DELETE_SAFARI` | Delete safari bookings |

---

## Data Transfer Objects (DTOs)

### CreateSafariFromItineraryDTO
Request DTO for creating a safari from an itinerary template.

```json
{
  "itineraryId": "string (required)",           // Obfuscated itinerary ID
  "customerId": "string (required)",            // Obfuscated customer ID
  "startDate": "2026-06-15 (required)",         // Safari start date (ISO format, cannot be in past)
  "name": "string (optional)",                  // Custom safari name (defaults to itinerary name)
  "description": "string (optional)",           // Custom description
  "specialRequests": "string (optional)",       // Special requests for the booking
  "dietaryRequirements": "string (optional)",   // Dietary requirements
  "emergencyContact": "string (optional)"       // Emergency contact information
}
```

**Validation Rules:**
- `itineraryId`: Required, must be valid and point to PUBLISHED itinerary
- `customerId`: Required, must be valid, customer must be active and not blacklisted
- `startDate`: Required, cannot be in the past
- Other fields are optional

---

### UpdateSafariDTO
Request DTO for updating safari details.

```json
{
  "name": "string (optional)",
  "startDate": "2026-06-20 (optional)",         // Cannot be in past; triggers date recalculation
  "carCount": 2 (optional),
  "description": "string (optional)",
  "highlights": "string (optional)",
  "startLocation": "string (optional)",
  "endLocation": "string (optional)",
  "specialRequests": "string (optional)",
  "dietaryRequirements": "string (optional)",
  "internalNotes": "string (optional)",
  "emergencyContact": "string (optional)"
}
```

**Important Notes:**
- All fields are optional (partial updates supported)
- `startDate`: If changed, all safari day dates are automatically recalculated
- `totalDays` and `totalNights` cannot be updated (inherited from itinerary)
- Safari must be in editable state (see business rules)

---

### SafariDTO
Response DTO containing complete safari information.

```json
{
  "id": "string",                               // Obfuscated safari ID
  "name": "string",
  "code": "string",                             // Auto-generated (e.g., SAF-5D4N-1023)
  "slug": "string",

  // References
  "itineraryId": "string",
  "itineraryName": "string",
  "itineraryCode": "string",
  "customerId": "string",
  "customerName": "string",
  "customerCode": "string",

  // State Information (Manually Managed)
  "state": "CONFIRMED",                         // Current booking state
  "stateDisplayName": "Confirmed",
  "stateDescription": "Safari confirmed with client and suppliers",
  "stateReason": "string",
  "stateChangedAt": "2026-02-01T10:30:00",

  // Phase Information (Auto-Calculated)
  "phase": "UPCOMING",                          // Time-based phase
  "phaseDisplayName": "Upcoming",
  "phaseDescription": "Safari starts in 8-30 days",
  "phaseUrgencyLevel": 2,                       // 1-5, 5 being most urgent
  "phaseColorCode": "blue",

  // Dates & Duration
  "startDate": "2026-06-15",
  "endDate": "2026-06-19",
  "totalDays": 5,
  "totalNights": 4,
  "carCount": 2,

  // Details
  "description": "string",
  "highlights": "string",
  "startLocation": "Arusha",
  "endLocation": "Arusha",
  "specialRequests": "string",
  "dietaryRequirements": "string",
  "emergencyContact": "string",

  // Status Flags (Computed)
  "isActive": true,
  "isEditable": true,
  "isCancellable": true,
  "hasStarted": false,
  "hasEnded": false,
  "isInProgress": false,
  "isUrgentPhase": false,

  // Time Calculations
  "daysUntilStart": 45,                         // Days until start (negative if started)
  "daysSinceEnd": -45,                          // Days since end (negative if not ended)
  "currentDayNumber": null,                     // Current day number (1-5) if in progress

  // Counts
  "totalPaxCount": 4,                           // Total passengers
  "totalDaysCount": 5,                          // Number of safari days

  // Audit Information
  "createdById": "string",
  "createdByUsername": "string",
  "createdByFullName": "string",
  "updatedById": "string",
  "updatedByUsername": "string",
  "updatedByFullName": "string",

  // Timestamps
  "createdAt": "2026-02-01T10:00:00",
  "updatedAt": "2026-02-01T15:30:00"
}
```

---

## Safari States & Phases

### Safari States (Manually Managed)

**Booking States:**
- `DRAFT` - Being prepared, not yet submitted
- `PENDING_APPROVAL` - Submitted, awaiting approval
- `APPROVED` - Approved by management
- `CONFIRMED` - Confirmed with client and suppliers

**Payment States:**
- `PENDING_DEPOSIT` - Awaiting deposit payment
- `DEPOSIT_PAID` - Deposit received
- `FULLY_PAID` - All payments received

**Operational States:**
- `READY` - Ready to commence
- `IN_PROGRESS` - Currently running

**Completion States:**
- `COMPLETED` - Successfully ended
- `PENDING_REVIEW` - Awaiting post-trip review
- `CLOSED` - Fully completed

**Hold/Pause States:**
- `ON_HOLD` - Temporarily paused
- `PENDING_DOCUMENTS` - Awaiting documents
- `PENDING_AVAILABILITY` - Awaiting availability confirmation

**Reschedule States:**
- `POSTPONED` - Rescheduled to later date
- `RESCHEDULING` - Dates being changed

**Cancellation States:**
- `CANCELLATION_REQUESTED` - Client requested cancellation
- `CANCELLED` - Cancelled (general)
- `CANCELLED_BY_CLIENT` - Cancelled by client
- `CANCELLED_BY_OPERATOR` - Cancelled by operator
- `CANCELLED_FORCE_MAJEURE` - Cancelled due to unforeseen circumstances

**Refund States:**
- `REFUND_PENDING` - Refund pending
- `REFUND_PARTIAL` - Partial refund issued
- `REFUND_COMPLETE` - Full refund issued

**Dispute States:**
- `DISPUTED` - Client raised dispute
- `UNDER_INVESTIGATION` - Dispute being investigated

---

### Safari Phases (Auto-Calculated)

**Pre-Safari Phases:**
- `FAR_FUTURE` - Starts in 30+ days (urgency: 1, color: gray)
- `UPCOMING` - Starts in 8-30 days (urgency: 2, color: blue)
- `STARTING_SOON` - Starts in 3-7 days (urgency: 3, color: orange)
- `IMMINENT` - Starts in 1-2 days (urgency: 4, color: red)
- `TODAY` - Starts today (urgency: 5, color: green)

**Active Phases:**
- `DAY_ONE` - First day (urgency: 5, color: green)
- `EARLY_DAYS` - Days 2-3 (urgency: 4, color: green)
- `MID_SAFARI` - Middle days (urgency: 4, color: green)
- `FINAL_DAYS` - Near end (urgency: 4, color: green)
- `LAST_DAY` - Final day (urgency: 4, color: green)

**Post-Safari Phases:**
- `JUST_ENDED` - Ended within 7 days (urgency: 3, color: teal)
- `RECENTLY_ENDED` - Ended 8-30 days ago (urgency: 2, color: gray)
- `PAST` - Ended 30+ days ago (urgency: 1, color: lightgray)

---

## API Endpoints

### 1. Create Safari from Itinerary

**Endpoint:** `POST /api/safaris`

**Permission:** `PERM_CREATE_SAFARI`

**Request Body:** `CreateSafariFromItineraryDTO`

**Request Example:**
```json
{
  "itineraryId": "Xy9pQ2mN",
  "customerId": "Ab3cD4eF",
  "startDate": "2026-06-15",
  "name": "Smith Family Safari - June 2026",
  "specialRequests": "Vegetarian meals for 2 guests",
  "dietaryRequirements": "2 vegetarian, 2 gluten-free",
  "emergencyContact": "+255 123 456 789"
}
```

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Safari created successfully",
  "data": {
    // SafariDTO object
    "id": "Mn8oP7qR",
    "code": "SAF-5D4N-1023",
    "state": "DRAFT",
    "phase": "FAR_FUTURE",
    // ... other fields
  }
}
```

**Validation Rules:**
1. Itinerary must exist and be PUBLISHED
2. Customer must exist, be active, and not blacklisted
3. Start date cannot be in the past
4. Safari structure (days, pax, activities) is deep-copied from itinerary
5. All safari day dates are calculated from start date
6. Auto-generates safari code after creation

**Errors:**
- `400 INVALID_ITINERARY_ID` - Invalid itinerary ID format
- `404 ITINERARY_NOT_FOUND` - Itinerary doesn't exist
- `400 ITINERARY_NOT_PUBLISHED` - Only published itineraries can be used
- `400 INVALID_CUSTOMER_ID` - Invalid customer ID format
- `404 CUSTOMER_NOT_FOUND` - Customer doesn't exist
- `400 CUSTOMER_CANNOT_BOOK` - Customer is inactive or blacklisted
- `400 START_DATE_REQUIRED` - Start date is missing
- `400 START_DATE_IN_PAST` - Start date cannot be in the past

---

### 2. Get Safari by ID

**Endpoint:** `GET /api/safaris/{id}`

**Permission:** `PERM_READ_SAFARI`

**Path Parameters:**
- `id` (string, required): Obfuscated safari ID

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Safari retrieved successfully",
  "data": {
    // Complete SafariDTO object
  }
}
```

**Errors:**
- `400 INVALID_SAFARI_ID` - Invalid ID format
- `404 SAFARI_NOT_FOUND` - Safari doesn't exist

---

### 3. Get Safari by Code

**Endpoint:** `GET /api/safaris/code/{code}`

**Permission:** `PERM_READ_SAFARI`

**Path Parameters:**
- `code` (string, required): Safari code (e.g., "SAF-5D4N-1023")

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Safari retrieved successfully",
  "data": {
    // Complete SafariDTO object
  }
}
```

**Errors:**
- `404 SAFARI_NOT_FOUND` - Safari with this code doesn't exist

---

### 4. Get All Safaris (with Filtering & Pagination)

**Endpoint:** `GET /api/safaris`

**Permission:** `PERM_READ_SAFARI`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | string | - | Filter by name (partial match, case-insensitive) |
| `code` | string | - | Filter by code (partial match) |
| `state` | SafariState | - | Filter by booking state |
| `phase` | SafariPhase | - | Filter by time-based phase |
| `startLocation` | string | - | Filter by start location |
| `endLocation` | string | - | Filter by end location |
| `startDateFrom` | date | - | Filter safaris starting from this date (ISO format) |
| `startDateTo` | date | - | Filter safaris starting before this date (ISO format) |
| `isActive` | boolean | - | Filter by active status |
| `keyword` | string | - | Search across name, code, description, locations |
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 10 | Page size (max 100) |
| `sortDirection` | string | desc | Sort direction: `asc` or `desc` (sorts by createdAt) |

**Example Request:**
```
GET /api/safaris?state=CONFIRMED&phase=UPCOMING&page=0&size=20&sortDirection=asc
```

**Success Response (200 OK):**
```json
{
  "content": [
    // Array of SafariDTO objects
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 97,
  "last": false,
  "size": 20,
  "number": 0,
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

**Filter Combinations:**
```
# Urgent safaris that need attention
GET /api/safaris?phase=STARTING_SOON&state=CONFIRMED

# Safaris for a specific customer (use keyword search with customer code)
GET /api/safaris?keyword=CUS-000123

# Completed safaris in the last 30 days
GET /api/safaris?state=COMPLETED&startDateFrom=2026-01-01

# All active safaris currently in progress
GET /api/safaris?state=IN_PROGRESS&phase=DAY_ONE,EARLY_DAYS,MID_SAFARI
```

---

### 5. Update Safari

**Endpoint:** `PUT /api/safaris/{id}`

**Permission:** `PERM_UPDATE_SAFARI`

**Path Parameters:**
- `id` (string, required): Obfuscated safari ID

**Request Body:** `UpdateSafariDTO`

**Request Example:**
```json
{
  "name": "Updated Safari Name",
  "startDate": "2026-06-20",
  "specialRequests": "Additional vegetarian meal",
  "internalNotes": "Customer requested later start date"
}
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Safari updated successfully",
  "data": {
    // Updated SafariDTO object
  }
}
```

**Important Behaviors:**
1. **Start Date Change**: When `startDate` is updated, all safari day `actualDate` fields are automatically recalculated
2. **End Date Recalculation**: `endDate` is automatically recalculated based on `totalDays`
3. **Partial Updates**: Only provided fields are updated
4. **Audit Tracking**: `updatedBy` and `updatedAt` are automatically set

**Validation Rules:**
1. Safari must be in editable state (see business rules)
2. Start date cannot be in the past
3. `totalDays` and `totalNights` cannot be changed (inherited from itinerary)

**Errors:**
- `400 INVALID_SAFARI_ID` - Invalid ID format
- `404 SAFARI_NOT_FOUND` - Safari doesn't exist
- `400 SAFARI_NOT_EDITABLE` - Safari state doesn't allow editing
- `400 START_DATE_IN_PAST` - Start date cannot be in the past

---

## Business Rules

### Safari Creation Rules
1. **Itinerary Must Be Published**: Only PUBLISHED itineraries can be used to create safaris
2. **Customer Validation**: Customer must be active and not blacklisted (`customer.canBook() == true`)
3. **Start Date**: Cannot be in the past (must be today or future date)
4. **Structure Deep Copy**: Safari inherits complete structure from itinerary (days, activities, parks, accommodations, pax)
5. **Date Calculation**: Each safari day gets `actualDate = startDate + (dayNumber - 1)`
6. **Initial State**: New safaris start in `DRAFT` state
7. **Code Generation**: Auto-generated after save: `SAF-{totalDays}D{totalNights}N-{1000+id}`

### Safari Update Rules
1. **Editable States**: Safari can only be edited in these states:
   - DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED
   - PENDING_DEPOSIT, DEPOSIT_PAID, ON_HOLD
   - POSTPONED, RESCHEDULING
   - PENDING_DOCUMENTS, PENDING_AVAILABILITY

2. **Non-Editable States**: Cannot edit when:
   - FULLY_PAID, READY, IN_PROGRESS
   - COMPLETED, PENDING_REVIEW, CLOSED
   - Any cancellation or refund state
   - DISPUTED, UNDER_INVESTIGATION

3. **Start Date Changes**:
   - Cannot set to past date
   - Triggers automatic recalculation of:
     - End date (`startDate + totalDays - 1`)
     - All safari day `actualDate` fields
   - Logs the date change for audit trail

4. **Immutable Fields**:
   - `totalDays` (inherited from itinerary)
   - `totalNights` (inherited from itinerary)
   - `customer` (cannot reassign to different customer)
   - `itinerary` (cannot change source template)

### Safari Cancellation Rules
1. **Cancellable States**:
   - DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED
   - PENDING_DEPOSIT, DEPOSIT_PAID, FULLY_PAID
   - READY, ON_HOLD, POSTPONED
   - PENDING_DOCUMENTS, PENDING_AVAILABILITY

2. **Non-Cancellable States**:
   - IN_PROGRESS (safari already started)
   - Already cancelled states
   - Terminal states (CLOSED, REFUND_COMPLETE)

### Date & Phase Calculations
1. **End Date**: Always `startDate + totalDays - 1`
2. **Safari Phase**: Automatically calculated based on:
   - Pre-safari: Days until start
   - During safari: Current day number
   - Post-safari: Days since end
3. **Phase updates**: Happen in real-time (not stored, computed on-the-fly)

---

## Common Use Cases

### Use Case 1: Create Safari for New Customer Booking
```bash
# 1. Create safari from published itinerary
POST /api/safaris
{
  "itineraryId": "Xy9pQ2mN",
  "customerId": "Ab3cD4eF",
  "startDate": "2026-07-15",
  "specialRequests": "Anniversary celebration - need romantic dinner setup"
}

# Response: Safari created with code SAF-5D4N-1025 in DRAFT state
```

### Use Case 2: Update Safari Start Date
```bash
# Customer wants to postpone safari by 5 days
PUT /api/safaris/Mn8oP7qR
{
  "startDate": "2026-07-20"
}

# Result:
# - Safari start date: 2026-07-20
# - Safari end date: 2026-07-24 (recalculated)
# - Day 1 actualDate: 2026-07-20
# - Day 2 actualDate: 2026-07-21
# - Day 3 actualDate: 2026-07-22
# - etc. (all day dates recalculated automatically)
```

### Use Case 3: Find Urgent Safaris
```bash
# Get safaris starting within next 7 days
GET /api/safaris?phase=STARTING_SOON,IMMINENT,TODAY&state=CONFIRMED&sortDirection=asc

# Use for operations dashboard to show upcoming departures
```

### Use Case 4: Find Safaris for Specific Customer
```bash
# Search by customer code
GET /api/safaris?keyword=CUS-000145

# Or get all active safaris for customer
GET /api/safaris?keyword=CUS-000145&state=CONFIRMED,IN_PROGRESS,READY
```

### Use Case 5: Operations Dashboard - Today's Safaris
```bash
# Get safaris starting today
GET /api/safaris?phase=TODAY&state=READY,CONFIRMED

# Get safaris currently in progress
GET /api/safaris?state=IN_PROGRESS
```

### Use Case 6: Review Recent Completions
```bash
# Get safaris completed in last 7 days
GET /api/safaris?phase=JUST_ENDED&state=COMPLETED,PENDING_REVIEW

# Use for post-trip follow-up and review collection
```

### Use Case 7: Financial Tracking
```bash
# Get safaris awaiting deposit
GET /api/safaris?state=PENDING_DEPOSIT

# Get fully paid safaris
GET /api/safaris?state=FULLY_PAID
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "status": 400,
  "message": "Human-readable error message",
  "error": "ERROR_CODE"
}
```

### Common Error Codes

#### Safari Creation Errors
| Code | Status | Description |
|------|--------|-------------|
| `INVALID_ITINERARY_ID` | 400 | Itinerary ID format is invalid |
| `ITINERARY_NOT_FOUND` | 404 | Itinerary doesn't exist |
| `ITINERARY_NOT_PUBLISHED` | 400 | Only published itineraries can be used |
| `INVALID_CUSTOMER_ID` | 400 | Customer ID format is invalid |
| `CUSTOMER_NOT_FOUND` | 404 | Customer doesn't exist |
| `CUSTOMER_CANNOT_BOOK` | 400 | Customer is inactive or blacklisted |
| `START_DATE_REQUIRED` | 400 | Start date is missing |
| `START_DATE_IN_PAST` | 400 | Start date cannot be in the past |
| `SAFARI_CREATE_FAILED` | 500 | Internal error during creation |

#### Safari Retrieval Errors
| Code | Status | Description |
|------|--------|-------------|
| `INVALID_SAFARI_ID` | 400 | Safari ID format is invalid |
| `SAFARI_NOT_FOUND` | 404 | Safari doesn't exist |

#### Safari Update Errors
| Code | Status | Description |
|------|--------|-------------|
| `SAFARI_NOT_EDITABLE` | 400 | Safari state doesn't allow editing |
| `START_DATE_IN_PAST` | 400 | Cannot set start date to past |
| `SAFARI_UPDATE_FAILED` | 500 | Internal error during update |

### Error Handling Examples

**Invalid Customer:**
```json
{
  "status": 400,
  "message": "Customer cannot book safaris. Customer may be inactive or blacklisted.",
  "error": "CUSTOMER_CANNOT_BOOK"
}
```

**Start Date in Past:**
```json
{
  "status": 400,
  "message": "Start date cannot be in the past. Provided: 2026-01-15, Today: 2026-02-02",
  "error": "START_DATE_IN_PAST"
}
```

**Safari Not Editable:**
```json
{
  "status": 400,
  "message": "Safari cannot be edited in state: Completed",
  "error": "SAFARI_NOT_EDITABLE"
}
```

---

## Additional Notes

### Audit Logging
All create and update operations are automatically logged with:
- Action type (CREATE_SAFARI, UPDATE_SAFARI)
- User who performed the action
- Timestamp
- Entity details

### Performance Considerations
1. **Pagination**: Always use pagination for list endpoints
2. **Filtering**: Apply filters to reduce result set size
3. **Phase Calculation**: Computed on-the-fly; consider caching for high-traffic scenarios
4. **Date Recalculation**: Updating start date triggers updates to all safari days; use transactions

### State Transitions
For changing safari state (e.g., DRAFT → CONFIRMED), use the dedicated Safari State Transition API at `/api/safaris/{id}/state/*` (see SafariStateTransitionController documentation).

### Related APIs
- **Safari State Transitions**: `/api/safaris/{id}/state/*`
- **Itinerary API**: `/api/itineraries` (for source templates)
- **Customer API**: `/api/customers` (for customer management)
- **Safari Days API**: `/api/safari-days` (for detailed day management)
- **Safari Pax API**: `/api/safari-pax` (for passenger management)

---

## Version History
- **v1.0** - Initial API release
  - Create safari from itinerary
  - Get safari by ID/code
  - List safaris with filtering
  - Update safari details
  - Automatic date recalculation on start date change
  - Customer relationship requirement
  - Audit tracking (createdBy, updatedBy)
