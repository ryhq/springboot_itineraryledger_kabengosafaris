# Safari Pax Management API Documentation

## Overview

The Safari Pax Management API provides endpoints for managing passenger categories within safari bookings. Passenger categories define how many travelers of different nation and age groups will actually participate in the safari.

Unlike itinerary pax (which are template-based), safari pax represents the actual confirmed passengers for a specific safari booking and can include safari-specific requirements like wheelchair accessibility, dietary needs, etc.

Each pax entry combines:
- **Nation Category**: Resident, Non-Resident, East African, etc.
- **Age Category**: Adult, Child, Infant, etc.
- **Count**: Number of passengers in this category
- **Special Requirements**: Safari-specific passenger needs

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Safari Pax API](#safari-pax-api)
   - [Upsert Pax](#1-upsert-pax)
   - [Get All Pax](#2-get-all-pax)
   - [Delete Pax](#3-delete-pax)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)
5. [Business Rules](#business-rules)

---

## Safari Pax API

Base URL: `/api/safaris/{safariId}/pax`

### 1. Upsert Pax

Creates new pax entries or updates existing ones based on nation/age category combination. This is an idempotent operation - if a pax entry with the same nation and age category already exists for the safari, it will be updated; otherwise, a new entry is created.

**Important:** Safari must be in an editable state for this operation to succeed.

**Endpoint:** `POST /api/safaris/{safariId}/pax`

**Permission Required:** `PERM_UPDATE_SAFARI_PAX`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID

**Request Body:**
```json
[
  {
    "nationCategoryId": "nat123xyz",
    "ageCategoryId": "age456abc",
    "count": 2,
    "specialRequirements": "Wheelchair accessible vehicle required for one passenger",
    "notes": "Couple celebrating anniversary"
  },
  {
    "nationCategoryId": "nat123xyz",
    "ageCategoryId": "age789def",
    "count": 2,
    "specialRequirements": "Child seat required, vegetarian meals",
    "notes": "Twin girls age 8"
  },
  {
    "nationCategoryId": "nat789ghi",
    "ageCategoryId": "age456abc",
    "count": 1,
    "notes": "Tour guide joining group"
  }
]
```

**Request Body Fields (per item):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nationCategoryId` | string | Yes | Obfuscated nation category ID |
| `ageCategoryId` | string | Yes | Obfuscated age category ID |
| `count` | integer | Yes | Number of passengers (min: 1) |
| `specialRequirements` | string | No | Special needs for this pax group (accessibility, dietary, etc.) |
| `notes` | string | No | Optional notes for this pax entry |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 created, 1 updated",
  "data": [
    {
      "id": "pax123xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "specialRequirements": "Wheelchair accessible vehicle required for one passenger",
      "notes": "Couple celebrating anniversary",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax124xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age789def",
      "ageCategoryName": "Child",
      "count": 2,
      "specialRequirements": "Child seat required, vegetarian meals",
      "notes": "Twin girls age 8",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax125xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat789ghi",
      "nationCategoryName": "Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 1,
      "notes": "Tour guide joining group",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid safari ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid safari ID",
    "errorCode": "INVALID_SAFARI_ID",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **400 Bad Request** - Safari not editable
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Safari cannot be edited in state: Completed",
    "errorCode": "SAFARI_NOT_EDITABLE",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

- **404 Not Found** - Safari not found
  ```json
  {
    "success": false,
    "statusCode": 404,
    "message": "Safari not found",
    "errorCode": "SAFARI_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
  }
  ```

**Notes:**
- This is a **bulk operation** - multiple pax entries can be created/updated in a single request
- The combination of `nationCategoryId` + `ageCategoryId` is unique per safari
- Safari must be in an editable state (e.g., DRAFT, CONFIRMED, PENDING_DEPOSIT)
- If a nation or age category is not found, that entry is silently skipped (logged as warning)
- The response message indicates how many entries were created vs updated
- Special requirements are particularly important for vehicle allocation and meal planning

---

### 2. Get All Pax

Retrieves all passenger category entries for a safari.

**Endpoint:** `GET /api/safaris/{safariId}/pax`

**Permission Required:** `PERM_READ_SAFARI_PAX`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved 3 pax categories (total: 5 passengers)",
  "data": [
    {
      "id": "pax123xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 2,
      "specialRequirements": "Wheelchair accessible vehicle required for one passenger",
      "notes": "Couple celebrating anniversary",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax124xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat123xyz",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age789def",
      "ageCategoryName": "Child",
      "count": 2,
      "specialRequirements": "Child seat required, vegetarian meals",
      "notes": "Twin girls age 8",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax125xyz",
      "safariId": "saf456abc",
      "nationCategoryId": "nat789ghi",
      "nationCategoryName": "Resident",
      "ageCategoryId": "age456abc",
      "ageCategoryName": "Adult",
      "count": 1,
      "notes": "Tour guide joining group",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "timestamp": "2024-01-15T11:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid safari ID
- **404 Not Found** - Safari not found

**Notes:**
- The response message includes both the number of pax categories and the total passenger count
- Total passengers is the sum of all `count` values
- This endpoint does NOT require the safari to be editable

---

### 3. Delete Pax

Deletes multiple passenger category entries by their IDs.

**Endpoint:** `DELETE /api/safaris/{safariId}/pax`

**Permission Required:** `PERM_DELETE_SAFARI_PAX`

**Path Parameters:**
- `safariId` (string, required): Obfuscated safari ID

**Request Body:**
```json
["pax123xyz", "pax124xyz"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 pax entry(ies) deleted successfully",
  "data": null,
  "timestamp": "2024-01-15T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid safari ID
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Invalid safari ID",
    "errorCode": "INVALID_SAFARI_ID",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

- **500 Internal Server Error** - Deletion failed
  ```json
  {
    "success": false,
    "statusCode": 500,
    "message": "Failed to delete safari pax",
    "errorCode": "SAFARI_PAX_DELETE_FAILED",
    "timestamp": "2024-01-15T12:00:00"
  }
  ```

**Notes:**
- Only pax entries belonging to the specified safari will be deleted
- Pax entries that don't belong to the safari are silently skipped
- Invalid pax IDs are silently skipped (logged as warning)
- The response indicates how many entries were actually deleted
- This operation triggers audit logging for each deleted entry

---

## Data Models

### SafariPaxDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated pax entry ID |
| `safariId` | string | Obfuscated safari ID |
| `nationCategoryId` | string | Obfuscated nation category ID |
| `nationCategoryName` | string | Name of the nation category (e.g., "Resident", "Non-Resident") |
| `ageCategoryId` | string | Obfuscated age category ID |
| `ageCategoryName` | string | Name of the age category (e.g., "Adult", "Child") |
| `count` | integer | Number of passengers in this category |
| `specialRequirements` | string | Special needs (wheelchair access, dietary, child seats, etc.) |
| `notes` | string | Optional notes |
| `createdAt` | datetime | Creation timestamp |
| `updatedAt` | datetime | Last update timestamp |

### UpsertSafariPaxDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nationCategoryId` | string | Yes | Obfuscated nation category ID |
| `ageCategoryId` | string | Yes | Obfuscated age category ID |
| `count` | integer | Yes | Number of passengers (min: 1) |
| `specialRequirements` | string | No | Special needs for this pax group |
| `notes` | string | No | Optional notes |

### Common Nation Categories

| Category | Description | Typical Use Case |
|----------|-------------|------------------|
| Resident | Citizens or permanent residents of the country | Local tourists, lower park fees |
| Non-Resident | Foreign visitors | International tourists, higher park fees |
| East African | Citizens of East African Community countries | Regional tourists, special rates |

### Common Age Categories

| Category | Typical Age Range | Pricing Impact |
|----------|-------------------|----------------|
| Adult | 18+ years | Full price |
| Child | 5-17 years | Reduced price or discounted |
| Infant | 0-4 years | Often free |
| Senior | 60+ years | May have special rates |

### Common Special Requirements

| Requirement Type | Examples |
|------------------|----------|
| **Accessibility** | Wheelchair accessible vehicle, mobility assistance, hearing/visual aids |
| **Dietary** | Vegetarian, vegan, gluten-free, halal, kosher, allergies |
| **Medical** | Medication storage, oxygen supply, medical equipment |
| **Child Safety** | Child seats, booster seats, baby supplies |
| **Religious** | Prayer times, modest dress requirements, dietary restrictions |

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_SAFARI_ID` | The provided safari ID is invalid or malformed |
| `SAFARI_NOT_FOUND` | Safari with the specified ID does not exist |
| `SAFARI_NOT_EDITABLE` | Safari is in a state that doesn't allow pax modifications |
| `SAFARI_PAX_UPSERT_FAILED` | Failed to upsert pax entries due to server error |
| `SAFARI_PAX_FETCH_FAILED` | Failed to fetch pax entries due to server error |
| `SAFARI_PAX_DELETE_FAILED` | Failed to delete pax entries due to server error |

---

## Examples

### Example 1: Setting Up Pax for a Family Safari

A family of 5 (2 adults, 3 children) from abroad with special requirements:

**Request:**
```http
POST /api/safaris/saf456abc/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_adult_456",
    "count": 2,
    "specialRequirements": "One adult requires wheelchair accessible vehicle",
    "notes": "Parents - 50th wedding anniversary"
  },
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_child_789",
    "count": 3,
    "specialRequirements": "3 child seats needed, all vegetarian meals",
    "notes": "Children ages 6, 8, and 10"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 created, 0 updated",
  "data": [
    {
      "id": "pax001",
      "safariId": "saf456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_adult_456",
      "ageCategoryName": "Adult",
      "count": 2,
      "specialRequirements": "One adult requires wheelchair accessible vehicle",
      "notes": "Parents - 50th wedding anniversary",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "pax002",
      "safariId": "saf456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_child_789",
      "ageCategoryName": "Child",
      "count": 3,
      "specialRequirements": "3 child seats needed, all vegetarian meals",
      "notes": "Children ages 6, 8, and 10",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### Example 2: Mixed Group Safari with Special Requirements

A group tour with both local and international travelers:

**Request:**
```http
POST /api/safaris/saf789def/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_res_001",
    "ageCategoryId": "age_adult_456",
    "count": 3,
    "specialRequirements": "Halal meals required",
    "notes": "Local Tanzanian participants"
  },
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_adult_456",
    "count": 5,
    "notes": "International participants from Europe"
  },
  {
    "nationCategoryId": "nat_ea_002",
    "ageCategoryId": "age_adult_456",
    "count": 2,
    "specialRequirements": "Gluten-free meals",
    "notes": "East African community members from Kenya"
  },
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_senior_999",
    "count": 2,
    "specialRequirements": "Mobility assistance, ground-floor accommodations",
    "notes": "Senior couple requiring special care"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "4 created, 0 updated",
  "data": [
    {
      "id": "pax101",
      "nationCategoryName": "Resident",
      "ageCategoryName": "Adult",
      "count": 3,
      "specialRequirements": "Halal meals required",
      "notes": "Local Tanzanian participants"
    },
    {
      "id": "pax102",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Adult",
      "count": 5,
      "notes": "International participants from Europe"
    },
    {
      "id": "pax103",
      "nationCategoryName": "East African",
      "ageCategoryName": "Adult",
      "count": 2,
      "specialRequirements": "Gluten-free meals",
      "notes": "East African community members from Kenya"
    },
    {
      "id": "pax104",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Senior",
      "count": 2,
      "specialRequirements": "Mobility assistance, ground-floor accommodations",
      "notes": "Senior couple requiring special care"
    }
  ]
}
```

### Example 3: Updating Pax Count and Requirements

If passenger count changes or requirements are updated:

**Request:**
```http
POST /api/safaris/saf456abc/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_child_789",
    "count": 4,
    "specialRequirements": "4 child seats needed, all vegetarian meals, one child has peanut allergy",
    "notes": "Updated: Added one more child age 5"
  }
]
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "0 created, 1 updated",
  "data": [
    {
      "id": "pax002",
      "safariId": "saf456abc",
      "nationCategoryId": "nat_nonres_123",
      "nationCategoryName": "Non-Resident",
      "ageCategoryId": "age_child_789",
      "ageCategoryName": "Child",
      "count": 4,
      "specialRequirements": "4 child seats needed, all vegetarian meals, one child has peanut allergy",
      "notes": "Updated: Added one more child age 5",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T14:00:00"
    }
  ]
}
```

### Example 4: Retrieving Total Pax Summary

**Request:**
```http
GET /api/safaris/saf456abc/pax
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved 2 pax categories (total: 6 passengers)",
  "data": [
    {
      "id": "pax001",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Adult",
      "count": 2,
      "specialRequirements": "One adult requires wheelchair accessible vehicle"
    },
    {
      "id": "pax002",
      "nationCategoryName": "Non-Resident",
      "ageCategoryName": "Child",
      "count": 4,
      "specialRequirements": "4 child seats needed, all vegetarian meals, one child has peanut allergy"
    }
  ]
}
```

### Example 5: Attempting to Update Non-Editable Safari

**Request:**
```http
POST /api/safaris/saf999xyz/pax
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "nationCategoryId": "nat_nonres_123",
    "ageCategoryId": "age_adult_456",
    "count": 2
  }
]
```

**Response:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari cannot be edited in state: Completed",
  "errorCode": "SAFARI_NOT_EDITABLE",
  "timestamp": "2024-01-15T15:00:00"
}
```

---

## Business Rules

### 1. Safari State Requirements

**Editable States:**
- DRAFT
- PENDING_APPROVAL
- APPROVED
- CONFIRMED
- PENDING_DEPOSIT
- DEPOSIT_PAID
- ON_HOLD
- POSTPONED
- RESCHEDULING
- PENDING_DOCUMENTS
- PENDING_AVAILABILITY

**Non-Editable States:**
- COMPLETED
- CLOSED
- CANCELLED
- CANCELLED_BY_CLIENT
- CANCELLED_BY_OPERATOR
- CANCELLED_FORCE_MAJEURE

### 2. Pax Management Rules

1. **Unique Combinations**: Each safari can only have ONE pax entry per nation/age category combination
2. **Minimum Count**: Count must be at least 1 (validated at DTO level)
3. **Category Validation**: Both nation and age categories must exist in the system
4. **Safari Ownership**: Pax entries can only be deleted if they belong to the specified safari

### 3. Special Requirements Guidelines

**Format Recommendations:**
- Use clear, concise language
- List multiple requirements separated by commas
- Include severity level for medical needs
- Specify exact quantities (e.g., "3 child seats" not "child seats")

**Common Patterns:**
```
"Wheelchair accessible vehicle required"
"Vegetarian meals, gluten-free options"
"Child seat (age 4), booster seat (age 7)"
"Medical: Requires refrigerated insulin storage"
"Prayer times: 5 daily prayers, halal meals"
```

### 4. Capacity Planning

- Use pax data to determine:
  - Number of vehicles needed
  - Type of vehicles (standard vs accessible)
  - Meal planning quantities
  - Accommodation room allocation
  - Park entry permit applications

---

## Best Practices

### 1. Pax Management

- **Use Bulk Upsert**: Always use the bulk endpoint even for single entries for consistency
- **Update via Upsert**: To update counts or requirements, simply upsert with the same nation/age category combination
- **Validate Categories**: Ensure nation and age category IDs exist before upserting
- **Document Special Requirements**: Be thorough and specific with special requirements

### 2. Special Requirements

- **Early Communication**: Capture special requirements as early as possible in the booking process
- **Specificity**: Be specific about quantities and exact needs
- **Updates**: Update requirements immediately when they change
- **Vehicle Allocation**: Use requirements data to allocate appropriate vehicles

### 3. Pricing Considerations

- Different nation categories typically have different park entry fees
- Different age categories may have different pricing (children may be free or discounted)
- Pax data is essential for accurate cost calculations and invoicing
- Safari pax determines final billing amounts

### 4. Operational Planning

- **Vehicle Capacity**: Calculate total passengers to determine vehicle needs
- **Meal Planning**: Use count + dietary requirements for meal preparation
- **Accommodation**: Use pax data for room allocation and bed configurations
- **Park Permits**: Submit pax details for park entry permits

### 5. Error Handling

- Check `success` field in response
- Use `errorCode` for programmatic error handling
- Invalid category references are silently skipped - check response data for actual upserted entries
- Verify safari is in editable state before attempting updates

### 6. Audit Trail

- All upsert and delete operations are automatically logged via `@AuditLogAnnotation`
- Audit logs capture: action, user, timestamp, entity type, and entity ID
- Use audit logs for compliance and tracking changes

---

## Related APIs

- **Safari API:** `/api/safaris` - Main safari booking management
- **Itinerary API:** `/api/itineraries` - Template itineraries (source for safaris)
- **Itinerary Pax API:** `/api/itineraries/{itineraryId}/pax` - Template pax configuration
- **Pax Nation Category API:** `/api/pax-nation-categories` - Nation category management
- **Pax Age Category API:** `/api/pax-age-categories` - Age category management
- **Safari State Transition API:** `/api/safaris/{safariId}/state/*` - Safari state management

---

## Integration Notes

### Safari Creation Flow

1. **Itinerary Pax → Safari Pax**: When a safari is created from an itinerary, pax entries are copied from ItineraryPax to SafariPax
2. **Initial State**: Copied pax entries start with count from itinerary, no special requirements
3. **Customization**: Use upsert endpoint to update counts and add special requirements specific to the booking

### Workflow Integration

```
1. Create Safari from Itinerary
   └─> Pax automatically copied from itinerary template

2. Customize Safari Pax
   └─> Update counts via upsert
   └─> Add special requirements
   └─> Adjust for actual confirmed passengers

3. Confirm Safari
   └─> Pax data frozen for operational planning

4. Pre-Safari Operations
   └─> Use pax data for vehicle allocation
   └─> Use pax data for meal planning
   └─> Use pax data for park permit applications

5. Post-Safari
   └─> Pax data becomes read-only for historical records
```

---

## Changelog

### Version 1.0.0 (2024-01-15)
- Initial API documentation
- Bulk upsert operation for safari pax entries
- Get all pax entries for a safari
- Bulk delete safari pax entries
- Support for safari-specific special requirements field
- Safari state validation for editable operations

---

## Support

For technical support or questions about the Safari Pax Management API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
