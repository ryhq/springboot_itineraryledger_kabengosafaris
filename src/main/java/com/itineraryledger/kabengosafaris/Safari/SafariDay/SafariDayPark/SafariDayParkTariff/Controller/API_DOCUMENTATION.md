# Safari Day Park Tariff API Documentation

## Overview

The Safari Day Park Tariff API manages tariffs (fees, charges) associated with safari park visits. This module handles park entry fees, conservation fees, concession fees, and other charges that apply to specific park visits within a safari.

**Base Path**: `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs`

**Key Features**:
- Bulk tariff addition for park visits
- Payment tracking with receipt management
- Tariff waiver functionality with reason tracking
- Pax-based tariff calculations
- Dual update modes (planning vs operational)
- Safari state validation

---

## Endpoints

### 1. Add Park Tariffs (Bulk Create)

**POST** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs`

Add multiple tariffs to a safari park visit.

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_TARIFF`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated Safari ID
- `dayId` (string, required) - Obfuscated Safari Day ID
- `parkVisitId` (string, required) - Obfuscated Safari Day Park ID

**Request Body**:
```json
[
  {
    "parkId": "obfuscated_park_id",
    "tariffId": "obfuscated_tariff_id",
    "notes": "Special notes about this tariff",
    "isIncludedInPrice": true
  }
]
```

**Request Fields**:
- `parkId` (string, required) - Must match the parent park's ID
- `tariffId` (string, required) - Reference to the Tariff entity
- `notes` (string, optional) - Additional notes about this tariff
- `isIncludedInPrice` (boolean, optional, default: true) - Whether tariff is included in package price

**Response** (201 Created):
```json
{
  "status": 201,
  "message": "2 tariffs added",
  "data": [
    {
      "id": "obfuscated_tariff_entry_id",
      "safariDayParkId": "obfuscated_park_visit_id",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "tariffId": "obfuscated_tariff_id",
      "tariffName": "Park Entry Fee - Adult",
      "notes": "Standard adult entry fee",
      "isIncludedInPrice": true,
      "isPaid": false,
      "paidAt": null,
      "receiptNumber": null,
      "paymentNotes": null,
      "paxCount": null,
      "isWaived": false,
      "waiverReason": null,
      "createdAt": "2025-01-15T10:30:00"
    }
  ]
}
```

**Error Responses**:
- `400 Bad Request` - Invalid IDs or Safari not editable
- `404 Not Found` - Park visit not found
- `500 Internal Server Error` - Server error

**Business Rules**:
1. Safari must be in editable state (DRAFT, CONFIRMED, IN_PROGRESS)
2. Park ID must match the parent park visit
3. ParkTariff (Park + Tariff combination) must exist
4. Invalid entries are silently skipped (logged)
5. Successfully added tariffs are returned

---

### 2. Get All Park Tariffs

**GET** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs`

Retrieve all tariffs for a safari park visit.

**Permission**: `PERM_READ_SAFARI_DAY_PARK_TARIFF`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated Safari ID
- `dayId` (string, required) - Obfuscated Safari Day ID
- `parkVisitId` (string, required) - Obfuscated Safari Day Park ID

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Safari park tariffs retrieved",
  "data": [
    {
      "id": "obfuscated_tariff_entry_id",
      "safariDayParkId": "obfuscated_park_visit_id",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "tariffId": "obfuscated_tariff_id",
      "tariffName": "Park Entry Fee - Adult",
      "notes": "Standard adult entry fee",
      "isIncludedInPrice": true,
      "isPaid": true,
      "paidAt": "2025-01-14T08:00:00",
      "receiptNumber": "RCP-2025-001",
      "paymentNotes": "Paid via bank transfer",
      "paxCount": 4,
      "isWaived": false,
      "waiverReason": null,
      "createdAt": "2025-01-10T10:30:00"
    },
    {
      "id": "obfuscated_tariff_entry_id_2",
      "safariDayParkId": "obfuscated_park_visit_id",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "tariffId": "obfuscated_tariff_id_2",
      "tariffName": "Conservation Fee",
      "notes": null,
      "isIncludedInPrice": true,
      "isPaid": false,
      "paidAt": null,
      "receiptNumber": null,
      "paymentNotes": null,
      "paxCount": 4,
      "isWaived": false,
      "waiverReason": null,
      "createdAt": "2025-01-10T10:30:00"
    }
  ]
}
```

**Error Responses**:
- `400 Bad Request` - Invalid park visit ID
- `500 Internal Server Error` - Server error

---

### 3. Get Single Park Tariff

**GET** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs/{tariffId}`

Retrieve a single tariff entry by ID.

**Permission**: `PERM_READ_SAFARI_DAY_PARK_TARIFF`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated Safari ID
- `dayId` (string, required) - Obfuscated Safari Day ID
- `parkVisitId` (string, required) - Obfuscated Safari Day Park ID
- `tariffId` (string, required) - Obfuscated Tariff Entry ID

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Safari park tariff retrieved",
  "data": {
    "id": "obfuscated_tariff_entry_id",
    "safariDayParkId": "obfuscated_park_visit_id",
    "parkId": "obfuscated_park_id",
    "parkName": "Serengeti National Park",
    "tariffId": "obfuscated_tariff_id",
    "tariffName": "Park Entry Fee - Adult",
    "notes": "Standard adult entry fee",
    "isIncludedInPrice": true,
    "isPaid": true,
    "paidAt": "2025-01-14T08:00:00",
    "receiptNumber": "RCP-2025-001",
    "paymentNotes": "Paid via bank transfer",
    "paxCount": 4,
    "isWaived": false,
    "waiverReason": null,
    "createdAt": "2025-01-10T10:30:00"
  }
}
```

**Error Responses**:
- `400 Bad Request` - Invalid ID or ownership mismatch
- `404 Not Found` - Tariff entry not found
- `500 Internal Server Error` - Server error

---

### 4. Update Park Tariff

**PUT** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs/{tariffId}`

Update a tariff entry with dual update modes.

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_TARIFF`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated Safari ID
- `dayId` (string, required) - Obfuscated Safari Day ID
- `parkVisitId` (string, required) - Obfuscated Safari Day Park ID
- `tariffId` (string, required) - Obfuscated Tariff Entry ID

**Request Body**:
```json
{
  "notes": "Updated notes",
  "isIncludedInPrice": true,
  "isPaid": true,
  "receiptNumber": "RCP-2025-001",
  "paymentNotes": "Paid via bank transfer",
  "paxCount": 4,
  "isWaived": false,
  "waiverReason": null
}
```

**Request Fields** (all optional):

**Planning Fields** (require editable safari):
- `notes` (string) - Additional notes
- `isIncludedInPrice` (boolean) - Whether tariff is included in price

**Operational Fields** (allowed anytime):
- `isPaid` (boolean) - Payment status (auto-sets `paidAt` timestamp)
- `receiptNumber` (string) - Payment receipt number
- `paymentNotes` (string) - Payment-related notes
- `paxCount` (integer) - Number of pax this tariff applies to
- `isWaived` (boolean) - Whether tariff was waived
- `waiverReason` (string) - Reason for waiving

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Safari park tariff updated successfully",
  "data": {
    "id": "obfuscated_tariff_entry_id",
    "safariDayParkId": "obfuscated_park_visit_id",
    "parkId": "obfuscated_park_id",
    "parkName": "Serengeti National Park",
    "tariffId": "obfuscated_tariff_id",
    "tariffName": "Park Entry Fee - Adult",
    "notes": "Updated notes",
    "isIncludedInPrice": true,
    "isPaid": true,
    "paidAt": "2025-01-15T14:30:00",
    "receiptNumber": "RCP-2025-001",
    "paymentNotes": "Paid via bank transfer",
    "paxCount": 4,
    "isWaived": false,
    "waiverReason": null,
    "createdAt": "2025-01-10T10:30:00"
  }
}
```

**Error Responses**:
- `400 Bad Request` - Invalid IDs, ownership mismatch, or Safari not editable (for planning updates)
- `404 Not Found` - Tariff entry not found
- `500 Internal Server Error` - Server error

---

### 5. Delete Park Tariffs (Bulk Delete)

**DELETE** `/api/safaris/{safariId}/days/{dayId}/parks/{parkVisitId}/tariffs`

Delete multiple tariff entries by IDs.

**Permission**: `PERM_UPDATE_SAFARI_DAY_PARK_TARIFF`

**Path Parameters**:
- `safariId` (string, required) - Obfuscated Safari ID
- `dayId` (string, required) - Obfuscated Safari Day ID
- `parkVisitId` (string, required) - Obfuscated Safari Day Park ID

**Request Body**:
```json
[
  "obfuscated_tariff_entry_id_1",
  "obfuscated_tariff_entry_id_2"
]
```

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "2 tariffs deleted",
  "data": null
}
```

**Error Responses**:
- `400 Bad Request` - Invalid park visit ID or Safari not editable
- `500 Internal Server Error` - Server error

**Business Rules**:
1. Safari must be in editable state
2. Only tariffs belonging to the specified park visit are deleted
3. Invalid IDs are silently skipped
4. Returns count of successfully deleted tariffs

---

## Dual Update Modes

The Safari Day Park Tariff module implements dual update modes to separate planning updates from operational updates:

### Planning Updates
**Require Editable Safari State** (DRAFT, CONFIRMED, IN_PROGRESS)

Fields:
- `notes` - Additional notes about the tariff
- `isIncludedInPrice` - Whether tariff is included in package price

Safari state validation is enforced for these updates.

### Operational Updates
**Allowed Anytime** (even in COMPLETED, CANCELLED, ARCHIVED states)

Fields:
- `isPaid` - Payment status
- `receiptNumber` - Payment receipt number
- `paymentNotes` - Payment-related notes
- `paxCount` - Number of pax this tariff applies to
- `isWaived` - Whether tariff was waived
- `waiverReason` - Reason for waiving

These updates can be made during and after the safari without state restrictions.

---

## Safari State Validation

### Editable States
Tariffs can be **created, updated (planning), and deleted** in these states:
- **DRAFT** - Initial planning
- **CONFIRMED** - Booking confirmed, still editable
- **IN_PROGRESS** - Safari ongoing, planning changes allowed

### Non-Editable States
Only **operational updates** allowed in these states:
- **COMPLETED** - Safari finished (payment tracking still allowed)
- **CANCELLED** - Safari cancelled (records locked)
- **ARCHIVED** - Historical record (read-only except operational updates)

---

## Auto-Timestamping

The module automatically manages timestamps for operational events:

### `paidAt` Timestamp
- **Set automatically** when `isPaid` is set to `true`
- Captures exact moment of payment confirmation
- **Cleared automatically** when `isPaid` is set to `false`

**Example**:
```json
{
  "isPaid": true,
  "receiptNumber": "RCP-2025-001"
}
```
Result: `paidAt` is automatically set to current timestamp.

---

## Business Rules

### 1. Tariff Creation
- Safari must be in editable state
- Park ID must match parent park visit
- ParkTariff (Park + Tariff combination) must exist in database
- Invalid entries are skipped during bulk creation
- All tariffs default to `isPaid: false` and `isWaived: false`

### 2. Tariff Updates
- Planning updates require editable safari state
- Operational updates allowed anytime
- `parkTariff` reference cannot be changed after creation (delete and recreate instead)
- Setting `isPaid: true` auto-sets `paidAt` timestamp
- Setting `isPaid: false` clears `paidAt` timestamp

### 3. Tariff Deletion
- Safari must be in editable state
- Only tariffs belonging to specified park visit can be deleted
- Bulk deletion supported
- Invalid/unowned IDs silently skipped

### 4. Payment Tracking
- Payment status (`isPaid`) can be updated anytime
- Receipt numbers can be recorded for audit trail
- Payment notes support additional context
- Timestamps automatically managed

### 5. Tariff Waivers
- Tariffs can be marked as waived with reason
- Waiver tracking independent of payment status
- Used for complimentary entries, special arrangements, etc.

### 6. Pax-Based Tariffs
- `paxCount` field tracks how many pax this tariff applies to
- Used for per-person tariffs (e.g., "Park Entry Fee - Adult")
- Useful for cost allocation and reporting

---

## Common Use Cases

### Use Case 1: Add Standard Park Tariffs
**Scenario**: Add all standard tariffs for Serengeti park visit during safari planning.

**Request**:
```http
POST /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs
```
```json
[
  {
    "parkId": "PARK_SER",
    "tariffId": "TARIFF_ENTRY_ADULT",
    "notes": "4 adults",
    "isIncludedInPrice": true
  },
  {
    "parkId": "PARK_SER",
    "tariffId": "TARIFF_CONSERVATION",
    "notes": "Conservation fee",
    "isIncludedInPrice": true
  },
  {
    "parkId": "PARK_SER",
    "tariffId": "TARIFF_VEHICLE",
    "notes": "Land Cruiser entry",
    "isIncludedInPrice": true
  }
]
```

**Result**: All three tariffs added with `isPaid: false`, ready for payment tracking.

---

### Use Case 2: Record Payment for Multiple Tariffs
**Scenario**: Safari operator pays park fees and records payment details.

**Request** (Update each tariff):
```http
PUT /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs/TARIFF_ENTRY1
```
```json
{
  "isPaid": true,
  "receiptNumber": "SER-2025-0142",
  "paymentNotes": "Paid at main gate via M-Pesa",
  "paxCount": 4
}
```

**Result**: Tariff marked as paid, `paidAt` timestamp auto-set, receipt recorded.

---

### Use Case 3: Waive Tariff for Special Guest
**Scenario**: VIP guest gets complimentary park entry.

**Request**:
```http
PUT /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs/TARIFF_ENTRY1
```
```json
{
  "isWaived": true,
  "waiverReason": "VIP guest - complimentary entry arranged by park management"
}
```

**Result**: Tariff marked as waived with documented reason.

---

### Use Case 4: Update Tariff Notes During Planning
**Scenario**: Update tariff notes before safari starts.

**Safari State**: DRAFT (editable)

**Request**:
```http
PUT /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs/TARIFF_ENTRY1
```
```json
{
  "notes": "Updated: 2 adults + 2 children",
  "paxCount": 4
}
```

**Result**: Notes updated (planning update) and pax count updated (operational update).

---

### Use Case 5: Record Post-Safari Payment
**Scenario**: Payment recorded after safari completion.

**Safari State**: COMPLETED (non-editable for planning)

**Request**:
```http
PUT /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs/TARIFF_ENTRY1
```
```json
{
  "isPaid": true,
  "receiptNumber": "POST-PAY-001",
  "paymentNotes": "Payment settled during final accounting"
}
```

**Result**: Payment recorded successfully (operational update allowed in COMPLETED state).

---

### Use Case 6: Bulk Delete Incorrect Tariffs
**Scenario**: Wrong tariffs added, need to remove and re-add correct ones.

**Request**:
```http
DELETE /api/safaris/SAF123/days/DAY1/parks/PARK1/tariffs
```
```json
[
  "TARIFF_ENTRY1",
  "TARIFF_ENTRY2"
]
```

**Result**: Both tariffs removed, can now add correct tariffs.

---

## Error Handling

### Common Error Codes

**400 Bad Request**
```json
{
  "status": 400,
  "message": "Invalid ID format",
  "errorCode": "INVALID_ID"
}
```

**400 Bad Request - Safari Not Editable**
```json
{
  "status": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE"
}
```

**400 Bad Request - Ownership Mismatch**
```json
{
  "status": 400,
  "message": "Tariff does not belong to this park visit",
  "errorCode": "TARIFF_PARK_VISIT_MISMATCH"
}
```

**404 Not Found**
```json
{
  "status": 404,
  "message": "Safari park visit not found",
  "errorCode": "PARK_VISIT_NOT_FOUND"
}
```

**404 Not Found - Tariff Entry**
```json
{
  "status": 404,
  "message": "Safari park tariff not found",
  "errorCode": "SAFARI_PARK_TARIFF_NOT_FOUND"
}
```

**500 Internal Server Error**
```json
{
  "status": 500,
  "message": "Failed to add safari park tariffs",
  "errorCode": "SAFARI_PARK_TARIFFS_ADD_FAILED"
}
```

---

## Best Practices

### 1. Tariff Management
- Add all applicable tariffs during initial safari planning
- Use bulk operations when adding multiple tariffs
- Record pax counts for per-person tariffs
- Document special arrangements in notes

### 2. Payment Tracking
- Record payments as they occur (not in batch)
- Always include receipt numbers for audit trail
- Use payment notes for additional context
- Update pax counts when recording payments

### 3. Waiver Management
- Always provide clear waiver reasons
- Document authorization source (e.g., "Approved by Park Director")
- Use waivers for complimentary arrangements, not unpaid fees

### 4. State Management
- Understand difference between planning and operational updates
- Plan tariffs during DRAFT/CONFIRMED states
- Record payments during/after safari (IN_PROGRESS/COMPLETED)
- Don't attempt planning updates on COMPLETED safaris

### 5. Error Handling
- Handle bulk operation partial successes
- Check for SAFARI_NOT_EDITABLE errors
- Validate park ID matches before creation
- Verify tariff ownership before updates

---

## Integration Notes

### Entity Relationships
```
Safari
  └── SafariDay
      └── SafariDayPark (Park Visit)
          └── SafariDayParkTariff (This entity)
              └── ParkTariff (Park + Tariff combination)
                  ├── Park
                  └── Tariff
```

### Permissions Required
- `PERM_READ_SAFARI_DAY_PARK_TARIFF` - View tariffs
- `PERM_UPDATE_SAFARI_DAY_PARK_TARIFF` - Create, update, delete tariffs

### Related Entities
- **Safari** - Parent safari with state validation
- **SafariDay** - Specific day within safari
- **SafariDayPark** - Park visit that tariffs belong to
- **Park** - Physical park location
- **Tariff** - Tariff definition (name, type, standard rate)
- **ParkTariff** - Junction entity (which tariffs apply to which parks)

---

## Changelog

**Version 1.0** (2025-01-15)
- Initial implementation
- Dual update mode support
- Safari state validation
- Auto-timestamping for payments
- Bulk operations support
- Payment tracking functionality
- Tariff waiver management
- Pax-based tariff tracking
