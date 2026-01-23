# Quotation API Documentation

Base URL: `/api/quotations`

## Overview

The Quotation API provides endpoints for managing safari quotations. Quotations follow a workflow-based lifecycle from DRAFT through various states to completion.

### Quotation Status Workflow

```
DRAFT --> SENT --> VIEWED --> ACCEPTED --> (Convert to Safari)
                         |
                         +--> REJECTED

DRAFT --> CANCELLED
SENT/VIEWED --> CANCELLED
SENT/VIEWED/REJECTED --> REVISED (creates new version)
```

### Status Definitions

| Status | Description |
|--------|-------------|
| DRAFT | Quotation is being prepared and has not been sent to the customer |
| SENT | Quotation has been sent to the customer |
| VIEWED | Customer has viewed the quotation |
| ACCEPTED | Customer has accepted the quotation |
| REJECTED | Customer has rejected the quotation |
| EXPIRED | Quotation validity period has passed |
| REVISED | Quotation has been superseded by a new version |
| CANCELLED | Quotation has been cancelled by staff |

---

## Endpoints

### Create Quotation

Creates a new quotation in DRAFT status.

**Endpoint:** `POST /api/quotations`

**Permission:** `PERM_CREATE_QUOTATION`

**Request Body:**
```json
{
  "name": "Serengeti Safari Adventure",
  "customerId": "obfuscated_customer_id",
  "itineraryId": "obfuscated_itinerary_id",
  "startDate": "2025-03-15",
  "endDate": "2025-03-22",
  "totalDays": 8,
  "totalNights": 7,
  "currency": "USD",
  "exchangeRate": 1.00,
  "discountType": "PERCENTAGE",
  "discountValue": 10.00,
  "discountReason": "Early bird discount",
  "taxRate": 18.00,
  "depositPercentage": 50.00,
  "validUntil": "2025-02-28",
  "termsAndConditions": "Standard T&C apply...",
  "inclusions": "All park fees, accommodation...",
  "exclusions": "International flights, tips...",
  "internalNotes": "VIP customer - priority handling",
  "customerNotes": "Vegetarian meals required",
  "assignedToId": "obfuscated_user_id",
  "paxList": [
    {
      "nationCategoryId": "obfuscated_nation_id",
      "ageCategoryId": "obfuscated_age_id",
      "count": 2,
      "unitPrice": 1500.00,
      "notes": "2 adults"
    }
  ],
  "lineItems": [
    {
      "dayNumber": 1,
      "itemType": "ACCOMMODATION",
      "itemName": "Serengeti Serena Lodge",
      "description": "Deluxe Room with full board",
      "referenceId": "obfuscated_accommodation_id",
      "referenceType": "Accommodation",
      "quantity": 2,
      "unitOfMeasure": "night",
      "unitPrice": 350.00,
      "taxable": true,
      "isIncluded": true
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "status": 201,
  "message": "Quotation created successfully",
  "data": {
    "id": "obfuscated_id",
    "code": "QUO-00010001",
    "name": "Serengeti Safari Adventure",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    ...
  }
}
```

---

### Get Quotation by ID

Retrieves a quotation by its obfuscated ID.

**Endpoint:** `GET /api/quotations/{id}`

**Permission:** `PERM_READ_QUOTATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated quotation ID |

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "code": "QUO-00010001",
    "name": "Serengeti Safari Adventure",
    "status": "DRAFT",
    "statusDisplayName": "Draft",
    "statusDescription": "Quotation is being prepared...",
    "version": 1,
    "customerId": "obfuscated_customer_id",
    "customerDisplayName": "John Smith",
    "customerEmail": "john@example.com",
    "itineraryId": "obfuscated_itinerary_id",
    "itineraryCode": "ITN-00010001",
    "itineraryName": "Classic Serengeti",
    "startDate": "2025-03-15",
    "endDate": "2025-03-22",
    "totalDays": 8,
    "totalNights": 7,
    "daysNightsDisplay": "8 Days / 7 Nights",
    "totalPax": 2,
    "currency": "USD",
    "subtotal": 5600.00,
    "discountType": "PERCENTAGE",
    "discountValue": 10.00,
    "taxRate": 18.00,
    "taxAmount": 907.20,
    "totalAmount": 5947.20,
    "depositRequired": true,
    "depositPercentage": 50.00,
    "perPersonCost": 2973.60,
    "validUntil": "2025-02-28",
    "isExpired": false,
    "daysUntilExpiry": 45,
    "paxCount": 1,
    "lineItemCount": 4,
    "canSend": true,
    "canRevise": false,
    "canAccept": false,
    "canConvertToSafari": false,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T14:20:00"
  }
}
```

---

### Get Quotation by Code

Retrieves a quotation by its code (e.g., QUO-00010001).

**Endpoint:** `GET /api/quotations/code/{code}`

**Permission:** `PERM_READ_QUOTATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| code | String | Quotation code |

**Response:** Same as Get by ID

---

### List Quotations

Retrieves a paginated list of quotations with optional filtering.

**Endpoint:** `GET /api/quotations`

**Permission:** `PERM_READ_QUOTATION`

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| customerId | String | No | - | Filter by customer (obfuscated ID) |
| itineraryId | String | No | - | Filter by itinerary (obfuscated ID) |
| status | String | No | - | Filter by status (DRAFT, SENT, etc.) |
| assignedToId | String | No | - | Filter by assigned user |
| createdById | String | No | - | Filter by creator |
| name | String | No | - | Filter by name (partial match) |
| code | String | No | - | Filter by code (partial match) |
| startDateFrom | Date | No | - | Filter by start date (from) |
| startDateTo | Date | No | - | Filter by start date (to) |
| createdFrom | DateTime | No | - | Filter by creation date (from) |
| createdTo | DateTime | No | - | Filter by creation date (to) |
| expired | Boolean | No | - | Filter by expiration status |
| currency | String | No | - | Filter by currency |
| originalOnly | Boolean | No | - | Only show original quotations (not revisions) |
| keyword | String | No | - | Search in name, code, notes |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 10 | Page size |
| sortBy | String | No | createdAt | Sort field |
| sortDirection | String | No | desc | Sort direction (asc/desc) |

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotations retrieved successfully",
  "data": {
    "quotations": [...],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 3,
    "pageSize": 10
  }
}
```

---

### Get Customer Quotations

Retrieves all quotations for a specific customer.

**Endpoint:** `GET /api/quotations/customer/{customerId}`

**Permission:** `PERM_READ_QUOTATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| customerId | String | Obfuscated customer ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | String | No | - | Filter by status |
| page | Integer | No | 0 | Page number |
| size | Integer | No | 10 | Page size |
| sortBy | String | No | createdAt | Sort field |
| sortDirection | String | No | desc | Sort direction |

**Response:** Same format as List Quotations

---

### Update Quotation

Updates a quotation (only DRAFT status quotations can be updated).

**Endpoint:** `PUT /api/quotations/{id}`

**Permission:** `PERM_UPDATE_QUOTATION`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated quotation ID |

**Request Body:** (all fields optional)
```json
{
  "name": "Updated Safari Adventure",
  "startDate": "2025-03-20",
  "endDate": "2025-03-27",
  "totalDays": 8,
  "totalNights": 7,
  "currency": "USD",
  "exchangeRate": 1.00,
  "discountType": "FIXED",
  "discountValue": 200.00,
  "discountReason": "Loyalty discount",
  "taxRate": 18.00,
  "depositPercentage": 30.00,
  "validUntil": "2025-03-01",
  "termsAndConditions": "Updated T&C...",
  "inclusions": "Updated inclusions...",
  "exclusions": "Updated exclusions...",
  "internalNotes": "Updated notes",
  "customerNotes": "Updated customer notes",
  "assignedToId": "obfuscated_user_id"
}
```

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation updated successfully",
  "data": { ... }
}
```

**Error Responses:**
- `400 Bad Request` - Quotation is not in DRAFT status

---

### Delete Quotations

Deletes quotations (only DRAFT and CANCELLED quotations can be deleted).

**Endpoint:** `DELETE /api/quotations`

**Permission:** `PERM_DELETE_QUOTATION`

**Request Body:**
```json
["obfuscated_id_1", "obfuscated_id_2"]
```

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "2 quotation(s) deleted successfully. Skipped (not in draft/cancelled status): QUO-00010003",
  "data": null
}
```

---

## Status Transition Endpoints

### Send Quotation

Sends a quotation to the customer (DRAFT -> SENT).

**Endpoint:** `POST /api/quotations/{id}/send`

**Permission:** `PERM_SEND_QUOTATION`

**Preconditions:**
- Quotation must be in DRAFT status
- Quotation must not be expired

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation sent successfully",
  "data": {
    "id": "...",
    "status": "SENT",
    "sentAt": "2025-01-15T15:30:00",
    ...
  }
}
```

---

### Mark as Viewed

Marks a quotation as viewed by the customer (SENT -> VIEWED).

**Endpoint:** `POST /api/quotations/{id}/mark-viewed`

**Permission:** `PERM_UPDATE_QUOTATION`

**Preconditions:**
- Quotation must be in SENT status

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation marked as viewed",
  "data": {
    "status": "VIEWED",
    "viewedAt": "2025-01-16T09:00:00",
    ...
  }
}
```

---

### Accept Quotation

Marks a quotation as accepted (SENT/VIEWED -> ACCEPTED).

**Endpoint:** `POST /api/quotations/{id}/accept`

**Permission:** `PERM_UPDATE_QUOTATION`

**Preconditions:**
- Quotation must be in SENT or VIEWED status
- Quotation must not be expired

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation accepted successfully",
  "data": {
    "status": "ACCEPTED",
    "acceptedAt": "2025-01-17T14:00:00",
    "respondedAt": "2025-01-17T14:00:00",
    "canConvertToSafari": true,
    ...
  }
}
```

---

### Reject Quotation

Marks a quotation as rejected (SENT/VIEWED -> REJECTED).

**Endpoint:** `POST /api/quotations/{id}/reject`

**Permission:** `PERM_UPDATE_QUOTATION`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reason | String | No | Rejection reason |

**Preconditions:**
- Quotation must be in SENT or VIEWED status

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation rejected",
  "data": {
    "status": "REJECTED",
    "rejectedAt": "2025-01-17T14:00:00",
    "respondedAt": "2025-01-17T14:00:00",
    "rejectionReason": "Budget constraints",
    ...
  }
}
```

---

### Cancel Quotation

Cancels a quotation.

**Endpoint:** `POST /api/quotations/{id}/cancel`

**Permission:** `PERM_UPDATE_QUOTATION`

**Preconditions:**
- Quotation must NOT be in ACCEPTED or already CANCELLED status

**Response:** `200 OK`
```json
{
  "status": 200,
  "message": "Quotation cancelled successfully",
  "data": {
    "status": "CANCELLED",
    ...
  }
}
```

---

### Create Revision

Creates a new version of a quotation (original becomes REVISED, new copy is DRAFT).

**Endpoint:** `POST /api/quotations/{id}/revise`

**Permission:** `PERM_CREATE_QUOTATION`

**Preconditions:**
- Quotation must be in a status that allows revision (SENT, VIEWED, REJECTED)
- Original quotation must not be expired

**Response:** `201 Created`
```json
{
  "status": 201,
  "message": "Quotation revision created successfully",
  "data": {
    "id": "new_obfuscated_id",
    "code": "QUO-00010002",
    "status": "DRAFT",
    "version": 2,
    "parentQuotationId": "original_obfuscated_id",
    "parentQuotationCode": "QUO-00010001",
    ...
  }
}
```

---

## Data Models

### DiscountType Enum

| Value | Display Name | Description |
|-------|--------------|-------------|
| NONE | No Discount | No discount applied |
| PERCENTAGE | Percentage | Percentage-based discount |
| FIXED | Fixed Amount | Fixed amount discount |

### LineItemType Enum

| Value | Display Name | Description |
|-------|--------------|-------------|
| ACCOMMODATION | Accommodation | Hotel, lodge, or camp charges |
| PARK_FEE | Park Fee | Conservation and entrance fees |
| ACTIVITY | Activity | Safari activities and excursions |
| TRANSPORT | Transport | Vehicle and transfer costs |
| GUIDE | Guide | Guide and driver fees |
| MEAL | Meal | Additional meal charges |
| FLIGHT | Flight | Internal/bush flights |
| VISA | Visa | Visa assistance fees |
| INSURANCE | Insurance | Travel insurance |
| OTHER | Other | Miscellaneous charges |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_QUOTATION_ID | The provided quotation ID is invalid or cannot be decoded |
| QUOTATION_NOT_FOUND | The requested quotation does not exist |
| INVALID_CUSTOMER_ID | The provided customer ID is invalid |
| CUSTOMER_NOT_FOUND | The specified customer does not exist |
| CUSTOMER_NOT_ELIGIBLE | The customer cannot receive quotations (blacklisted/inactive) |
| QUOTATION_NOT_EDITABLE | Quotation is not in DRAFT status |
| QUOTATION_CANNOT_BE_SENT | Quotation cannot be sent (wrong status or expired) |
| QUOTATION_CANNOT_BE_ACCEPTED | Quotation cannot be accepted (wrong status or expired) |
| QUOTATION_CANNOT_BE_CANCELLED | Quotation cannot be cancelled in current status |
| QUOTATION_CANNOT_BE_REVISED | Quotation cannot be revised in current status |
| INVALID_STATUS_TRANSITION | The requested status transition is not allowed |
| NATION_CATEGORY_NOT_FOUND | The specified nation category does not exist |
| AGE_CATEGORY_NOT_FOUND | The specified age category does not exist |
| QUOTATION_CREATE_FAILED | Failed to create quotation |
| QUOTATION_UPDATE_FAILED | Failed to update quotation |
| QUOTATION_SEND_FAILED | Failed to send quotation |
| QUOTATION_ACCEPT_FAILED | Failed to accept quotation |
| QUOTATION_REJECT_FAILED | Failed to reject quotation |
| QUOTATION_CANCEL_FAILED | Failed to cancel quotation |
| QUOTATION_REVISE_FAILED | Failed to create quotation revision |

---

## Permissions Required

| Action | Permission |
|--------|------------|
| Create Quotation | PERM_CREATE_QUOTATION |
| Read Quotation | PERM_READ_QUOTATION |
| Update Quotation | PERM_UPDATE_QUOTATION |
| Delete Quotation | PERM_DELETE_QUOTATION |
| Send Quotation | PERM_SEND_QUOTATION |
| Convert to Safari | PERM_CONVERT_QUOTATION_TO_SAFARI |

---

## Usage Examples

### Create a Simple Quotation

```bash
curl -X POST http://localhost:8080/api/quotations \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Quick Safari Quote",
    "customerId": "abc123",
    "startDate": "2025-04-01",
    "totalDays": 5,
    "totalNights": 4,
    "currency": "USD",
    "validUntil": "2025-03-15"
  }'
```

### Send Quotation to Customer

```bash
curl -X POST http://localhost:8080/api/quotations/{id}/send \
  -H "Authorization: Bearer {token}"
```

### Accept Quotation

```bash
curl -X POST http://localhost:8080/api/quotations/{id}/accept \
  -H "Authorization: Bearer {token}"
```

### Create Revision After Rejection

```bash
curl -X POST http://localhost:8080/api/quotations/{id}/revise \
  -H "Authorization: Bearer {token}"
```
