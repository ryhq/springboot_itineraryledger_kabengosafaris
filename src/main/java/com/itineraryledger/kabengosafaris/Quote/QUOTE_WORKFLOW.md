# Quote Status Workflow

## Overview

The Quote module implements a comprehensive status workflow system that manages the lifecycle of safari quotations from initial draft through customer acceptance and conversion to bookings.

## Status Lifecycle

### Status States

```
DRAFT → READY → SENT → [ACCEPTED/REJECTED/EXPIRED] → [CONVERTED/CANCELLED]
  ↑       ↓       ↓              ↓                           ↓
  └───────┴───────┴──────────────┴───────────────────────────┘
```

| Status | Description | Can Edit? | Can Delete? | Can Send to Customer? |
|--------|-------------|-----------|-------------|-----------------------|
| **DRAFT** | Quote is being prepared | ✅ Yes | ✅ Yes | ❌ No |
| **READY** | Complete and ready to send | ⚠️ Limited | ⚠️ With caution | ✅ Yes |
| **SENT** | Sent to customer for review | ⚠️ Very limited | ❌ No | ✅ Resend only |
| **ACCEPTED** | Customer accepted the quote | ❌ No | ❌ No | ✅ Resend only |
| **REJECTED** | Customer rejected the quote | ❌ No | ⚠️ Yes | ❌ No |
| **EXPIRED** | Validity period has passed | ❌ No | ⚠️ Yes | ❌ No |
| **CANCELLED** | Cancelled by company | ❌ No | ⚠️ Yes | ❌ No |
| **CONVERTED** | Converted to booking/safari | ❌ No | ❌ No | ✅ Resend only |

---

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    QUOTE LIFECYCLE                              │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐
│  DRAFT   │ ◄─────────────────────────┐
└─────┬────┘                           │
      │                                │
      │ POST /api/quotes/{id}/mark-ready
      │ Permission: PERM_READY_QUOTE   │
      │                                │
      ▼                                │
┌──────────┐                           │
│  READY   │                           │
└─────┬────┘                           │
      │                                │
      │ POST /api/quotes/{id}/send     │
      │ Permission: PERM_SEND_QUOTE    │
      │                                │
      ▼                                │
┌──────────┐                           │
│   SENT   │                           │
└─────┬────┘                           │
      │                                │
      ├──────────────────┐             │
      │                  │             │
      │ Customer Action  │             │
      │                  │             │
      ▼                  ▼             │
┌──────────┐      ┌──────────┐         │
│ ACCEPTED │      │ REJECTED │         │
└─────┬────┘      └─────┬────┘         │
      │                  │             │
      │                  │             │
      ▼                  └─────────────┘
┌──────────┐
│CONVERTED │
└──────────┘


ADDITIONAL TRANSITIONS:

┌──────────┐    POST /api/quotes/{id}/cancel       ┌──────────┐
│   ANY    │ ──────────────────────────────────►   │CANCELLED │
└──────────┘    Permission: PERM_CANCEL_QUOTE      └──────────┘

┌──────────┐    POST /api/quotes/{id}/revert-to-draft    ┌──────────┐
│  READY   │ ──────────────────────────────────────────► │  DRAFT   │
└──────────┘    Permission: PERM_REVERT_QUOTE_TO_DRAFT   └──────────┘

┌──────────┐    POST /api/quotes/{id}/revert-to-draft    ┌──────────┐
│   SENT   │ ──────────────────────────────────────────► │  DRAFT   │
└──────────┘    Permission: PERM_REVERT_QUOTE_TO_DRAFT   └──────────┘

┌──────────┐    Automatic (via scheduled job or check)   ┌──────────┐
│   SENT   │ ──────────────────────────────────────────► │ EXPIRED  │
└──────────┘    When validity date passes                └──────────┘

┌──────────┐    POST /api/quotes/{id}/convert      ┌──────────┐
│ ACCEPTED │ ──────────────────────────────────►   │CONVERTED │
└──────────┘    Permission: PERM_CONVERT_QUOTE     └──────────┘
```

---

## API Endpoints

### 1. Mark Quote as Ready

**Endpoint:** `POST /api/quotes/{id}/mark-ready`
**Permission:** `PERM_READY_QUOTE`
**Description:** Mark a quote as ready to send to customer

**Allowed Transitions:**
- DRAFT → READY ✅

**Validation:**
- Must be in DRAFT status
- Must have valid itinerary
- Must have valid customer
- Must have at least one line item
- Must have grand totals calculated

**Response:**
```json
{
  "status": 200,
  "message": "Quote marked as ready",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "READY",
    "statusDisplayName": "Ready"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Only DRAFT quotes can be marked as READY",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

```json
{
  "status": 400,
  "message": "Quote does not meet requirements. Must have itinerary, customer, and line items.",
  "errorCode": "INCOMPLETE_QUOTE"
}
```

---

### 2. Send Quote to Customer

**Endpoint:** `POST /api/quotes/{id}/send`
**Permission:** `PERM_SEND_QUOTE`
**Description:** Send quote to customer (marks as SENT)

**Allowed Transitions:**
- DRAFT → SENT ✅ (if requirements met)
- READY → SENT ✅
- SENT → SENT ✅ (resend)
- ACCEPTED → SENT ✅ (resend)
- CONVERTED → SENT ✅ (resend)

**Side Effects:**
- Sets `sentDate` to current date
- May trigger email notification to customer
- Generates/regenerates PDF if not exists

**Response:**
```json
{
  "status": 200,
  "message": "Quote sent to customer successfully",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "SENT",
    "sentDate": "2024-02-06"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot send REJECTED quote",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

### 3. Mark Quote as Accepted

**Endpoint:** `POST /api/quotes/{id}/accept`
**Permission:** `PERM_ACCEPT_QUOTE`
**Description:** Mark quote as accepted by customer

**Allowed Transitions:**
- SENT → ACCEPTED ✅

**Side Effects:**
- Records acceptance date
- May trigger internal notifications

**Response:**
```json
{
  "status": 200,
  "message": "Quote marked as accepted",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "ACCEPTED"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Only SENT quotes can be marked as ACCEPTED",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

### 4. Mark Quote as Rejected

**Endpoint:** `POST /api/quotes/{id}/reject`
**Permission:** `PERM_REJECT_QUOTE`
**Description:** Mark quote as rejected by customer

**Allowed Transitions:**
- SENT → REJECTED ✅

**Request Body (optional):**
```json
{
  "rejectionReason": "Price too high"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Quote marked as rejected",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "REJECTED"
  }
}
```

---

### 5. Mark Quote as Expired

**Endpoint:** `POST /api/quotes/{id}/expire`
**Permission:** `PERM_EXPIRE_QUOTE`
**Description:** Manually mark quote as expired

**Allowed Transitions:**
- SENT → EXPIRED ✅
- READY → EXPIRED ✅

**Note:** This can also happen automatically when validity date passes

**Response:**
```json
{
  "status": 200,
  "message": "Quote marked as expired",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "EXPIRED"
  }
}
```

---

### 6. Cancel Quote

**Endpoint:** `POST /api/quotes/{id}/cancel`
**Permission:** `PERM_CANCEL_QUOTE`
**Description:** Cancel a quote (can be from any status except CONVERTED)

**Allowed Transitions:**
- ANY (except CONVERTED) → CANCELLED ✅

**Request Body (optional):**
```json
{
  "cancellationReason": "Customer no longer interested"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Quote cancelled successfully",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "CANCELLED"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot cancel CONVERTED quote",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

### 7. Revert Quote to Draft

**Endpoint:** `POST /api/quotes/{id}/revert-to-draft`
**Permission:** `PERM_REVERT_QUOTE_TO_DRAFT`
**Description:** Revert a quote back to DRAFT status

**Allowed Transitions:**
- READY → DRAFT ✅
- SENT → DRAFT ✅

**Use Cases:**
- Need to make significant changes before sending
- Customer requested major revisions
- Realized quote has errors

**Response:**
```json
{
  "status": 200,
  "message": "Quote reverted to draft",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "DRAFT"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot revert ACCEPTED quote to DRAFT. Create a new version instead.",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

### 8. Convert Quote to Booking

**Endpoint:** `POST /api/quotes/{id}/convert`
**Permission:** `PERM_CONVERT_QUOTE`
**Description:** Convert an accepted quote to a booking/safari

**Allowed Transitions:**
- ACCEPTED → CONVERTED ✅

**Side Effects:**
- May create Safari/Booking entity
- Locks quote from further edits
- Records conversion date

**Response:**
```json
{
  "status": 200,
  "message": "Quote converted to booking successfully",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "CONVERTED",
    "safariId": "xyz789"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Only ACCEPTED quotes can be converted to bookings",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

## Status Requirements

### Requirements to Mark as READY

A quote can be marked as READY only if:

✅ Status is DRAFT
✅ Has valid `itinerary` (not null)
✅ Has valid `customer` (not null)
✅ Has at least one line item
✅ Has grand totals calculated

### Requirements to SEND

A quote can be sent to customer if:

✅ Status is DRAFT, READY, SENT, ACCEPTED, or CONVERTED
✅ Meets all READY requirements
✅ Has valid customer email
✅ Has valid validity date (not expired)

### Requirements to ACCEPT

✅ Status is SENT
✅ Customer has reviewed the quote

### Requirements to CONVERT

✅ Status is ACCEPTED
✅ All required booking information present
✅ Itinerary is PUBLISHED

---

## Permissions

### Standard CRUD Permissions
- `PERM_CREATE_QUOTE` - Create new quotes
- `PERM_READ_QUOTE` - View quote data
- `PERM_UPDATE_QUOTE` - Edit quote fields
- `PERM_DELETE_QUOTE` - Delete quotes

### Workflow Permissions
- `PERM_READY_QUOTE` - Mark quote as ready
- `PERM_SEND_QUOTE` - Send quote to customer
- `PERM_ACCEPT_QUOTE` - Mark quote as accepted
- `PERM_REJECT_QUOTE` - Mark quote as rejected
- `PERM_EXPIRE_QUOTE` - Mark quote as expired
- `PERM_CANCEL_QUOTE` - Cancel quote
- `PERM_REVERT_QUOTE_TO_DRAFT` - Revert to draft
- `PERM_CONVERT_QUOTE` - Convert to booking

### Recommended Role Assignments

| Role | Permissions |
|------|-------------|
| **Quote Creator** | CREATE, READ, UPDATE, READY, REVERT_TO_DRAFT |
| **Quote Manager** | All CREATE/READ/UPDATE + READY, SEND, REVERT_TO_DRAFT, CANCEL |
| **Sales Manager** | All permissions except DELETE |
| **Operations Director** | All permissions including ACCEPT, CONVERT |
| **Admin** | All permissions |

---

## Business Rules

### 1. Draft Quotes
- Can be freely edited
- Can be deleted
- Cannot be sent to customer until marked READY
- Not visible to customers

### 2. Ready Quotes
- Limited editing (only non-critical fields)
- Can be sent to customer
- Can be reverted to DRAFT if changes needed
- Should be reviewed before sending

### 3. Sent Quotes
- Very limited editing (only display fields)
- Cannot be deleted
- Can be resent to customer
- Can be reverted to DRAFT for major revisions
- Automatically expires when validity date passes

### 4. Accepted Quotes
- Read-only (cannot edit)
- Cannot be deleted
- Can be resent to customer
- Should be converted to booking
- Cannot be reverted (create new version instead)

### 5. Rejected Quotes
- Read-only (cannot edit)
- Can be deleted for cleanup
- Cannot be sent again
- Create new version if customer reconsiders

### 6. Expired Quotes
- Read-only (cannot edit)
- Can be deleted for cleanup
- Cannot be sent again
- Create new version with updated validity date

### 7. Cancelled Quotes
- Read-only (cannot edit)
- Can be deleted for cleanup
- Cannot be sent or reactivated
- Maintains audit trail

### 8. Converted Quotes
- Read-only (cannot edit)
- Cannot be deleted (historical record)
- Can be resent for customer reference
- Linked to Safari/Booking

---

## Edit Restrictions

### Critical Fields (Cannot edit after READY)
- `itinerary` - Linked itinerary
- `customer` - Linked customer
- Line items (prices, quantities)
- Grand totals

### Non-Critical Fields (Can edit in READY/SENT)
- `title` - Quote title
- `description` - Quote description
- `validityDate` - Validity date (within reason)
- `paymentTerms` - Payment terms text
- `notes` - Internal notes
- `customerNotes` - Customer-facing notes

### Display-Only Fields (Can edit in SENT)
- `description` - Quote description
- `customerNotes` - Customer-facing notes

---

## Integration Points

### Safari/Booking Module
When converting a quote to a booking:
```java
// Only accepted quotes can be converted
if (quote.getStatus() != QuoteStatus.ACCEPTED) {
    throw new BusinessException("Only accepted quotes can be converted to bookings");
}

// Itinerary must be published
if (quote.getItinerary().getStatus() != ItineraryStatus.PUBLISHED) {
    throw new BusinessException("Cannot convert quote - itinerary is not published");
}
```

### Email/Notification Module
When sending a quote:
```java
// Generate PDF
QuoteDocument pdf = pdfGenerator.generate(quote);

// Send email to customer
emailService.sendQuoteEmail(
    quote.getCustomer().getEmail(),
    quote.getTitle(),
    pdf
);

// Update status
quote.setStatus(QuoteStatus.SENT);
quote.setSentDate(LocalDate.now());
```

### Versioning
When customer requests major changes to SENT/ACCEPTED quote:
```java
// Don't revert - create new version instead
Quote newVersion = quoteVersionService.createNewVersion(quote);
newVersion.setStatus(QuoteStatus.DRAFT);
// Link versions
newVersion.setPreviousVersion(quote);
```

---

## Audit Trail

All status transitions are logged via `@AuditLogAnnotation`:
- **Action:** The permission/action performed (e.g., SEND_QUOTE)
- **Entity Type:** Quote
- **Entity ID:** Obfuscated quote ID
- **User:** Current authenticated user
- **Timestamp:** When the action occurred
- **Additional Data:** Reason, notes, or metadata

---

## Testing Scenarios

### Scenario 1: Happy Path - Draft to Converted

```bash
# 1. Create quote (status: DRAFT)
POST /api/quotes
{
  "title": "7-Day Serengeti Safari Quote",
  "itineraryId": "abc123",
  "customerId": "xyz789"
}

# 2. Add line items
POST /api/quotes/{id}/line-items

# 3. Mark as ready
POST /api/quotes/{id}/mark-ready
# Response: status = READY

# 4. Send to customer
POST /api/quotes/{id}/send
# Response: status = SENT, sentDate = 2024-02-06

# 5. Customer accepts
POST /api/quotes/{id}/accept
# Response: status = ACCEPTED

# 6. Convert to booking
POST /api/quotes/{id}/convert
# Response: status = CONVERTED, safariId = "safari123"
```

### Scenario 2: Revert for Major Changes

```bash
# SENT quote needs major revisions
POST /api/quotes/{id}/revert-to-draft
# Response: status = DRAFT

# Make changes
PUT /api/quotes/{id}

# Mark as ready again
POST /api/quotes/{id}/mark-ready
# Response: status = READY

# Resend to customer
POST /api/quotes/{id}/send
# Response: status = SENT
```

### Scenario 3: Customer Rejects

```bash
# Customer rejects quote
POST /api/quotes/{id}/reject
{
  "rejectionReason": "Price too high"
}
# Response: status = REJECTED

# Create new version with lower prices
POST /api/quotes
# ... (create new quote as revision)
```

### Scenario 4: Quote Expires

```bash
# Check for expired quotes (scheduled job)
GET /api/quotes?status=SENT&validityBefore=2024-02-06

# Manually expire quote
POST /api/quotes/{id}/expire
# Response: status = EXPIRED
```

---

## Error Handling

### Common Error Codes
- `QUOTE_NOT_FOUND` (404) - Quote ID not found
- `INVALID_STATUS_TRANSITION` (400) - Cannot perform requested transition
- `INCOMPLETE_QUOTE` (400) - Does not meet requirements
- `ALREADY_SENT` (400) - Quote already sent
- `ALREADY_ACCEPTED` (400) - Quote already accepted
- `ALREADY_CANCELLED` (400) - Quote already cancelled
- `ALREADY_CONVERTED` (400) - Quote already converted
- `QUOTE_EXPIRED` (400) - Quote validity has expired
- `EDIT_BLOCKED` (400) - Cannot edit in current status
- `DELETE_BLOCKED` (400) - Cannot delete in current status

---

## Files

### Entity
- [Quote.java](Entity/Quote.java) - Main entity with status enum and validation logic

### Service
- [QuoteStatusService.java](Services/QuoteStatusService.java) - All status transition logic

### Controller
- [QuoteController.java](Controller/QuoteController.java) - REST endpoints for status management

### Configuration
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Custom workflow permissions

---

## Future Enhancements

### Planned Features
1. **Automatic Expiry** - Scheduled job to auto-expire quotes
2. **Version Control** - Track quote versions and revisions
3. **Approval Workflow** - Require manager approval before sending
4. **Customer Portal** - Allow customers to accept/reject online
5. **Email Templates** - Customizable email templates for sending
6. **PDF Customization** - Multiple PDF template options
7. **Status Notifications** - Email/SMS notifications on status changes
8. **Bulk Operations** - Send/cancel multiple quotes at once

---

*Last Updated: 2026-02-06*
