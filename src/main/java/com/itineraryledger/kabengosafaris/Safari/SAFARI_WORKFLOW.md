# Safari Workflow System - Complete Reference

**Version:** 2.0 (Simplified from 24 to 14 states)
**Last Updated:** 2026-02-06
**Status:** ✅ Fully Implemented

---

## Table of Contents

1. [Overview](#overview)
2. [State Machine Diagram](#state-machine-diagram)
3. [State Definitions](#state-definitions)
4. [API Endpoints](#api-endpoints)
5. [Business Rules](#business-rules)
6. [Permissions](#permissions)
7. [Integration Points](#integration-points)
8. [Testing Scenarios](#testing-scenarios)
9. [Error Handling](#error-handling)

---

## Overview

### Purpose

The Safari workflow system manages the complete lifecycle of a safari booking from initial draft through execution to post-trip closure. It tracks:

- **Booking progression** (draft → approval → confirmation)
- **Payment tracking** (pending → fully paid)
- **Operational execution** (in progress → completed → closed)
- **Exception handling** (holds, cancellations, refunds, disputes)

### Dual-Tracking System

Safaris use **two separate but complementary tracking mechanisms**:

1. **SafariState** (14 states, manual) - Booking/operational workflow states
   - Manually controlled by staff actions
   - Represents business decisions and booking progress
   - This document focuses on SafariState

2. **SafariPhase** (13 phases, automatic) - Time-based lifecycle phases
   - Automatically calculated from start/end dates
   - Represents temporal urgency (FAR_FUTURE → UPCOMING → IN_PROGRESS → PAST)
   - No workflow transitions needed

This separation allows SafariState to be simpler and focus purely on business workflow.

---

## State Machine Diagram

###

 Core Journey (Happy Path)

```
┌──────────────────────────────────────────────────────────────────────┐
│                         CORE JOURNEY (9 states)                        │
└──────────────────────────────────────────────────────────────────────┘

┌─────────┐    POST /submit-for-approval    ┌──────────────────┐
│  DRAFT  │ ──────────────────────────────► │ PENDING_APPROVAL │
└─────────┘                                  └─────────┬────────┘
                                                       │
                                                       │ POST /approve
                                                       │
                                                       ▼
                                             ┌──────────────┐
                                             │   APPROVED   │
                                             └──────┬───────┘
                                                    │
                                                    │ POST /confirm
                                                    │
                                                    ▼
                                            ┌───────────────┐
                                            │  CONFIRMED    │
                                            └───────┬───────┘
                                                    │
                                                    │ POST /request-payment
                                                    │
                                                    ▼
                                         ┌─────────────────────┐
                                         │  PENDING_PAYMENT    │
                                         └──────────┬──────────┘
                                                    │
                                                    │ POST /record-payment
                                                    │
                                                    ▼
                                           ┌────────────────┐
                                           │  FULLY_PAID    │
                                           └────────┬───────┘
                                                    │
                                                    │ POST /start
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │ IN_PROGRESS  │
                                            └──────┬───────┘
                                                   │
                                                   │ POST /complete
                                                   │
                                                   ▼
                                            ┌────────────┐
                                            │ COMPLETED  │
                                            └─────┬──────┘
                                                  │
                                                  │ POST /close
                                                  │
                                                  ▼
                                            ┌─────────┐
                                            │ CLOSED  │
                                            └─────────┘
```

### Exception/Special States

```
┌──────────────────────────────────────────────────────────────────────┐
│                    EXCEPTION/SPECIAL (5 states)                        │
└──────────────────────────────────────────────────────────────────────┘

┌────────────┐     POST /hold (with reason)      ┌──────────┐
│  Any State │ ──────────────────────────────────►│ ON_HOLD  │
│ (editable) │                                     └────┬─────┘
└────────────┘                                          │
                                                        │ POST /release-hold
                                                        │
                                                        ▼
                                                  (returns to previous
                                                   or specified state)

┌────────────┐     POST /cancel (with reason)    ┌───────────┐
│  Any State │ ──────────────────────────────────►│ CANCELLED │
│(cancellable)│                                    └─────┬─────┘
└────────────┘                                           │
                                                         │ POST /initiate-refund
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ REFUND_PENDING  │
                                                └────────┬────────┘
                                                         │
                                                         │ POST /record-refund
                                                         │
                                                         ▼
                                                ┌──────────────────┐
                                                │ REFUND_COMPLETE  │
                                                └──────────────────┘

┌────────────┐     POST /mark-disputed          ┌──────────┐
│  Post-trip │ ──────────────────────────────────►│ DISPUTED │
│   States   │                                    └────┬─────┘
└────────────┘                                         │
                                                       │ POST /resolve-dispute
                                                       │
                                                       ▼
                                                 (returns to CLOSED
                                                  or REFUND_PENDING)
```

---

## State Definitions

### Core Journey States (9)

#### 1. DRAFT
- **Description:** Safari booking is being prepared, not yet submitted for approval
- **Editable:** ✅ Yes - All fields
- **Deletable:** ✅ Yes
- **Cancellable:** ✅ Yes
- **Next States:** PENDING_APPROVAL, ON_HOLD, CANCELLED
- **Typical Duration:** Variable (days to weeks)

#### 2. PENDING_APPROVAL
- **Description:** Safari booking submitted, awaiting management approval
- **Editable:** ✅ Yes - Limited fields
- **Deletable:** ✅ Yes
- **Cancellable:** ✅ Yes
- **Next States:** APPROVED, DRAFT (reject), ON_HOLD, CANCELLED
- **Typical Duration:** 1-3 days

#### 3. APPROVED
- **Description:** Safari booking approved by management
- **Editable:** ✅ Yes - Non-critical fields
- **Deletable:** ❌ No
- **Cancellable:** ✅ Yes
- **Next States:** CONFIRMED, ON_HOLD, CANCELLED
- **Typical Duration:** 1-7 days

#### 4. CONFIRMED
- **Description:** Safari confirmed with client and suppliers
- **Editable:** ✅ Yes - Limited fields
- **Deletable:** ❌ No
- **Cancellable:** ✅ Yes
- **Next States:** PENDING_PAYMENT, ON_HOLD, CANCELLED
- **Typical Duration:** Variable

#### 5. PENDING_PAYMENT
- **Description:** Awaiting deposit or full payment from client
- **Editable:** ✅ Yes - Payment-related fields
- **Deletable:** ❌ No
- **Cancellable:** ✅ Yes
- **Next States:** FULLY_PAID, ON_HOLD, CANCELLED
- **Typical Duration:** 1-30 days
- **Note:** Replaces PENDING_DEPOSIT and DEPOSIT_PAID states

#### 6. FULLY_PAID
- **Description:** All payments received, safari ready to commence
- **Editable:** ❌ No - Read-only except notes
- **Deletable:** ❌ No
- **Cancellable:** ✅ Yes (with refund)
- **Next States:** IN_PROGRESS, ON_HOLD, CANCELLED
- **Typical Duration:** Days/weeks until start date

#### 7. IN_PROGRESS
- **Description:** Safari is currently running
- **Editable:** ❌ No - Critical operational data only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No (use DISPUTED if issues arise)
- **Next States:** COMPLETED, DISPUTED
- **Typical Duration:** Safari duration (days to weeks)
- **SafariPhase:** Automatically calculated (DAY_ONE, EARLY_DAYS, MID_SAFARI, FINAL_DAYS, LAST_DAY)

#### 8. COMPLETED
- **Description:** Safari has successfully ended
- **Editable:** ❌ No - Notes/review fields only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No
- **Next States:** CLOSED, DISPUTED
- **Typical Duration:** 1-7 days (post-trip tasks)

#### 9. CLOSED
- **Description:** Safari fully completed with all post-trip tasks done
- **Editable:** ❌ No - Read-only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No
- **Next States:** DISPUTED (only exception)
- **Terminal:** ✅ Yes

### Exception/Special States (5)

#### 10. ON_HOLD
- **Description:** Safari temporarily paused (see `holdReason` for details)
- **Editable:** ✅ Yes
- **Deletable:** ❌ No
- **Cancellable:** ✅ Yes
- **Next States:** Previous state, CANCELLED, DISPUTED
- **Hold Reasons:**
  - `PENDING_DOCUMENTS` - Awaiting visas, permits, etc.
  - `PENDING_AVAILABILITY` - Awaiting accommodation/activity confirmation
  - `RESCHEDULING` - Safari dates being changed
  - `CLIENT_REQUEST` - On hold at client's request
  - `PAYMENT_ISSUE` - Hold due to payment concerns
  - `OPERATIONAL_ISSUE` - Hold due to logistical issues
  - `OTHER` - Other reason (see notes)

#### 11. CANCELLED
- **Description:** Safari has been cancelled (see `cancellationReason` for details)
- **Editable:** ❌ No - Notes only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No (already cancelled)
- **Next States:** REFUND_PENDING
- **Terminal:** ✅ Yes (unless refund needed)
- **Cancellation Reasons:**
  - `BY_CLIENT` - Cancelled at client's request
  - `BY_OPERATOR` - Cancelled by tour operator
  - `FORCE_MAJEURE` - Unforeseen circumstances (natural disaster, political unrest)
  - `PAYMENT_FAILURE` - Non-payment or payment issues
  - `NO_AVAILABILITY` - Lack of accommodation/activity availability
  - `OTHER` - Other reason (see notes)

#### 12. REFUND_PENDING
- **Description:** Refund process initiated, awaiting completion
- **Editable:** ❌ No - Refund tracking fields only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No
- **Next States:** REFUND_COMPLETE, DISPUTED
- **Typical Duration:** Variable (depends on payment method)
- **Note:** Track partial refunds with `refundAmount` field, not separate state

#### 13. REFUND_COMPLETE
- **Description:** Refund has been fully issued
- **Editable:** ❌ No - Read-only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No
- **Next States:** DISPUTED (only exception)
- **Terminal:** ✅ Yes

#### 14. DISPUTED
- **Description:** Client has raised a dispute or complaint, under investigation
- **Editable:** ❌ No - Investigation notes only
- **Deletable:** ❌ No
- **Cancellable:** ❌ No
- **Next States:** CLOSED, REFUND_PENDING, REFUND_COMPLETE (resolution)
- **Typical Duration:** Variable (depends on investigation)

---

## API Endpoints

### Base Path: `/api/safaris`

All endpoints require authentication and appropriate permissions.

### 1. Submit for Approval

**Endpoint:** `POST /api/safaris/{id}/submit-for-approval`
**Permission:** `PERM_SUBMIT_SAFARI_FOR_APPROVAL`
**From States:** DRAFT
**To State:** PENDING_APPROVAL
**Required:** Safari must have itinerary, customer, dates, and at least one pax

**Request Body:**
```json
{
  "reason": "Safari is ready for approval"  // Optional
}
```

**Success Response (200):**
```json
{
  "status": 200,
  "message": "Safari submitted for approval successfully",
  "data": {
    "id": "abc123",
    "code": "SAF-2024-001",
    "state": "PENDING_APPROVAL",
    "stateDisplayName": "Pending Approval",
    "stateReason": "Safari is ready for approval"
  }
}
```

**Error Response (400):**
```json
{
  "status": 400,
  "message": "Cannot submit from state: Confirmed",
  "errorCode": "INVALID_STATE_TRANSITION"
}
```

---

### 2. Approve Safari

**Endpoint:** `POST /api/safaris/{id}/approve`
**Permission:** `PERM_APPROVE_SAFARI`
**From States:** PENDING_APPROVAL
**To State:** APPROVED

**Request Body:**
```json
{
  "reason": "All requirements met, approved for booking"  // Optional
}
```

---

### 3. Reject Safari

**Endpoint:** `POST /api/safaris/{id}/reject`
**Permission:** `PERM_REJECT_SAFARI`
**From States:** PENDING_APPROVAL
**To State:** DRAFT
**Required:** Reason is mandatory

**Request Body:**
```json
{
  "reason": "Missing accommodation details for Day 3"  // Required
}
```

---

### 4. Confirm Safari

**Endpoint:** `POST /api/safaris/{id}/confirm`
**Permission:** `PERM_CONFIRM_SAFARI`
**From States:** APPROVED
**To State:** CONFIRMED

**Request Body:**
```json
{
  "reason": "Confirmed with client and all suppliers"  // Optional
}
```

---

### 5. Request Payment

**Endpoint:** `POST /api/safaris/{id}/request-payment`
**Permission:** `PERM_REQUEST_SAFARI_PAYMENT`
**From States:** CONFIRMED
**To State:** PENDING_PAYMENT

**Request Body:**
```json
{
  "paymentType": "DEPOSIT",  // or "FULL"
  "amountDue": 5000.00,
  "currency": "USD",
  "dueDate": "2024-12-31",
  "reason": "Deposit payment requested"  // Optional
}
```

---

### 6. Record Payment

**Endpoint:** `POST /api/safaris/{id}/record-payment`
**Permission:** `PERM_RECORD_SAFARI_PAYMENT`
**From States:** PENDING_PAYMENT
**To State:** FULLY_PAID (if complete), or stays in PENDING_PAYMENT

**Request Body:**
```json
{
  "amountPaid": 5000.00,
  "currency": "USD",
  "paymentMethod": "BANK_TRANSFER",
  "paymentReference": "TXN-123456",
  "paidDate": "2024-12-15",
  "reason": "Deposit payment received"  // Optional
}
```

---

### 7. Start Safari

**Endpoint:** `POST /api/safaris/{id}/start`
**Permission:** `PERM_START_SAFARI`
**From States:** FULLY_PAID
**To State:** IN_PROGRESS
**Required:** Safari start date must be today or in the past

**Request Body:**
```json
{
  "reason": "Safari commenced on schedule"  // Optional
}
```

---

### 8. Complete Safari

**Endpoint:** `POST /api/safaris/{id}/complete`
**Permission:** `PERM_COMPLETE_SAFARI`
**From States:** IN_PROGRESS
**To State:** COMPLETED
**Required:** Safari end date must be today or in the past

**Request Body:**
```json
{
  "reason": "Safari completed successfully"  // Optional
}
```

---

### 9. Close Safari

**Endpoint:** `POST /api/safaris/{id}/close`
**Permission:** `PERM_CLOSE_SAFARI`
**From States:** COMPLETED
**To State:** CLOSED

**Request Body:**
```json
{
  "reason": "All post-trip tasks completed"  // Optional
}
```

---

### 10. Hold Safari

**Endpoint:** `POST /api/safaris/{id}/hold`
**Permission:** `PERM_HOLD_SAFARI`
**From States:** DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, PENDING_PAYMENT, FULLY_PAID
**To State:** ON_HOLD
**Required:** Reason and holdReason are mandatory

**Request Body:**
```json
{
  "holdReason": "PENDING_DOCUMENTS",  // Required enum
  "reason": "Awaiting visa approval for 2 guests"  // Required details
}
```

**Hold Reason Options:**
- `PENDING_DOCUMENTS`
- `PENDING_AVAILABILITY`
- `RESCHEDULING`
- `CLIENT_REQUEST`
- `PAYMENT_ISSUE`
- `OPERATIONAL_ISSUE`
- `OTHER`

---

### 11. Release Hold

**Endpoint:** `POST /api/safaris/{id}/release-hold`
**Permission:** `PERM_RELEASE_SAFARI_HOLD`
**From States:** ON_HOLD
**To State:** Previous state or specified state

**Request Body:**
```json
{
  "targetState": "CONFIRMED",  // Optional, defaults to state before hold
  "reason": "Documents received, proceeding with booking"  // Optional
}
```

---

### 12. Cancel Safari

**Endpoint:** `POST /api/safaris/{id}/cancel`
**Permission:** `PERM_CANCEL_SAFARI`
**From States:** DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, PENDING_PAYMENT, FULLY_PAID, ON_HOLD
**To State:** CANCELLED
**Required:** Reason and cancellationReason are mandatory

**Request Body:**
```json
{
  "cancellationReason": "BY_CLIENT",  // Required enum
  "reason": "Client family emergency, unable to travel"  // Required details
}
```

**Cancellation Reason Options:**
- `BY_CLIENT`
- `BY_OPERATOR`
- `FORCE_MAJEURE`
- `PAYMENT_FAILURE`
- `NO_AVAILABILITY`
- `OTHER`

---

### 13. Initiate Refund

**Endpoint:** `POST /api/safaris/{id}/initiate-refund`
**Permission:** `PERM_INITIATE_SAFARI_REFUND`
**From States:** CANCELLED
**To State:** REFUND_PENDING

**Request Body:**
```json
{
  "refundAmount": 4500.00,
  "currency": "USD",
  "refundReason": "Cancellation within refund period",
  "reason": "Processing refund less 10% cancellation fee"  // Optional
}
```

---

### 14. Record Refund

**Endpoint:** `POST /api/safaris/{id}/record-refund`
**Permission:** `PERM_RECORD_SAFARI_REFUND`
**From States:** REFUND_PENDING
**To State:** REFUND_COMPLETE (if full), or stays in REFUND_PENDING (if partial)

**Request Body:**
```json
{
  "amountRefunded": 4500.00,
  "currency": "USD",
  "refundMethod": "BANK_TRANSFER",
  "refundReference": "REF-123456",
  "refundedDate": "2024-12-20",
  "isFinalRefund": true,  // True for complete refund
  "reason": "Full refund processed"  // Optional
}
```

---

### 15. Mark Disputed

**Endpoint:** `POST /api/safaris/{id}/mark-disputed`
**Permission:** `PERM_MARK_SAFARI_DISPUTED`
**From States:** COMPLETED, CLOSED, CANCELLED, REFUND_PENDING, REFUND_COMPLETE
**To State:** DISPUTED
**Required:** Reason is mandatory

**Request Body:**
```json
{
  "reason": "Client disputes quality of accommodation on Day 3"  // Required
}
```

---

### 16. Resolve Dispute

**Endpoint:** `POST /api/safaris/{id}/resolve-dispute`
**Permission:** `PERM_RESOLVE_SAFARI_DISPUTE`
**From States:** DISPUTED
**To State:** CLOSED, REFUND_PENDING, or REFUND_COMPLETE (depending on resolution)
**Required:** Reason and targetState are mandatory

**Request Body:**
```json
{
  "targetState": "REFUND_PENDING",  // Required
  "resolutionSummary": "Agreed to 25% partial refund for accommodation issue",  // Required
  "reason": "Dispute resolved, processing partial refund"  // Optional
}
```

---

## Business Rules

### Payment Tracking

**PENDING_PAYMENT State:**
- Replaces separate PENDING_DEPOSIT and DEPOSIT_PAID states
- Track payment progress with database fields:
  - `depositAmount` - Deposit amount due
  - `depositPaid` - Deposit paid flag
  - `depositPaidDate` - When deposit was received
  - `totalAmountDue` - Total safari cost
  - `totalAmountPaid` - Total amount paid so far
  - `fullyPaidDate` - When fully paid
- Transition to FULLY_PAID only when `totalAmountPaid >= totalAmountDue`

### Hold Management

**ON_HOLD State:**
- Must specify `holdReason` enum value
- Store previous state for easy resumption
- Can cancel from ON_HOLD without resuming
- Example database fields:
  - `state` = ON_HOLD
  - `holdReason` - Enum (PENDING_DOCUMENTS, RESCHEDULING, etc.)
  - `stateReason` - Text details
  - `previousState` - State to return to on release

### Cancellation & Refunds

**Cancellation Process:**
1. Cancel safari → CANCELLED (must specify `cancellationReason`)
2. If refund applicable → Initiate refund → REFUND_PENDING
3. Process refund(s) → Record payment → REFUND_COMPLETE

**Refund Tracking:**
- REFUND_PENDING can have multiple partial refunds
- Track with database fields:
  - `refundAmountDue` - Total refund amount approved
  - `refundAmountPaid` - Total refund amount issued so far
  - `refundPayments` - JSON array of refund transactions
- Only transition to REFUND_COMPLETE when fully refunded

### Dispute Handling

**DISPUTED State:**
- Can only be entered from post-trip or cancellation states
- Investigation happens in DISPUTED state
- Resolution transitions to:
  - CLOSED - If no refund needed
  - REFUND_PENDING - If partial/full refund agreed
  - REFUND_COMPLETE - If refund already processed

### Edit Restrictions

**By State:**
- **DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, PENDING_PAYMENT, ON_HOLD:** Editable (varying field restrictions)
- **FULLY_PAID:** Notes/comments only
- **IN_PROGRESS:** Critical operational fields + notes
- **COMPLETED:** Notes/review fields only
- **CLOSED, CANCELLED, REFUND_PENDING, REFUND_COMPLETE, DISPUTED:** Read-only except investigation/resolution notes

### Deletion Restrictions

**Can Delete:**
- DRAFT
- PENDING_APPROVAL (with caution)

**Cannot Delete:**
- APPROVED and beyond (all states)
- Rationale: These represent confirmed bookings with financial/operational implications

---

## Permissions

All Safari workflow permissions are defined in `custom-permissions.json`.

### Booking Workflow Permissions

| Permission Name | Action | Entity | Description |
|----------------|--------|--------|-------------|
| `SUBMIT_SAFARI_FOR_APPROVAL` | UPDATE | SAFARI | Submit safari for approval |
| `APPROVE_SAFARI` | UPDATE | SAFARI | Approve safari booking |
| `REJECT_SAFARI` | UPDATE | SAFARI | Reject safari booking |
| `CONFIRM_SAFARI` | UPDATE | SAFARI | Confirm safari with client |

### Payment Workflow Permissions

| Permission Name | Action | Entity | Description |
|----------------|--------|--------|-------------|
| `REQUEST_SAFARI_PAYMENT` | UPDATE | SAFARI | Request deposit/full payment |
| `RECORD_SAFARI_PAYMENT` | UPDATE | SAFARI | Record payment received |

### Operational Workflow Permissions

| Permission Name | Action | Entity | Description |
|----------------|--------|--------|-------------|
| `START_SAFARI` | UPDATE | SAFARI | Start safari execution |
| `COMPLETE_SAFARI` | UPDATE | SAFARI | Mark safari as completed |
| `CLOSE_SAFARI` | UPDATE | SAFARI | Close safari (final state) |

### Exception Handling Permissions

| Permission Name | Action | Entity | Description |
|----------------|--------|--------|-------------|
| `HOLD_SAFARI` | UPDATE | SAFARI | Put safari on hold |
| `RELEASE_SAFARI_HOLD` | UPDATE | SAFARI | Release safari from hold |
| `CANCEL_SAFARI` | CANCEL | SAFARI | Cancel safari |
| `INITIATE_SAFARI_REFUND` | UPDATE | SAFARI | Initiate refund process |
| `RECORD_SAFARI_REFUND` | UPDATE | SAFARI | Record refund payment |
| `MARK_SAFARI_DISPUTED` | UPDATE | SAFARI | Mark safari as disputed |
| `RESOLVE_SAFARI_DISPUTE` | UPDATE | SAFARI | Resolve safari dispute |

### Recommended Role Assignments

**Safari Manager:**
- All permissions except APPROVE_SAFARI

**Safari Director:**
- All permissions

**Safari Coordinator:**
- SUBMIT_SAFARI_FOR_APPROVAL
- CONFIRM_SAFARI
- START_SAFARI
- COMPLETE_SAFARI
- HOLD_SAFARI
- RELEASE_SAFARI_HOLD

**Finance Manager:**
- REQUEST_SAFARI_PAYMENT
- RECORD_SAFARI_PAYMENT
- INITIATE_SAFARI_REFUND
- RECORD_SAFARI_REFUND

---

## Integration Points

### 1. Quote → Safari Conversion

**Trigger:** Quote with status ACCEPTED converted to Safari
**Initial State:** DRAFT
**Process:**
- Create Safari from accepted Quote
- Copy itinerary, customer, dates, pax
- Set state = DRAFT
- Quote changes to CONVERTED status

### 2. Itinerary Dependency

**Rule:** Safari must reference a PUBLISHED Itinerary
**Validation:** Checked at creation and before CONFIRM transition

### 3. Payment Gateway Integration

**States:** PENDING_PAYMENT → FULLY_PAID
**Integration Points:**
- Payment request generation
- Payment status webhook handling
- Payment confirmation recording

### 4. Email Notifications

**Trigger States:**
- PENDING_APPROVAL → Email to approvers
- APPROVED → Email to coordinators
- CONFIRMED → Email to customer
- PENDING_PAYMENT → Email payment request to customer
- FULLY_PAID → Email confirmation to customer
- IN_PROGRESS → Email start notification
- COMPLETED → Email thank you + review request
- CANCELLED → Email cancellation notice
- REFUND_COMPLETE → Email refund confirmation

### 5. SafariPhase Auto-Calculation

**Trigger:** Date changes or time progression
**Process:**
- SafariPhase calculated independently
- No impact on SafariState
- Used for UI urgency indicators

---

## Testing Scenarios

### Happy Path Test

1. Create Safari → DRAFT
2. Submit for approval → PENDING_APPROVAL
3. Approve → APPROVED
4. Confirm → CONFIRMED
5. Request payment → PENDING_PAYMENT
6. Record deposit → PENDING_PAYMENT (partial)
7. Record full payment → FULLY_PAID
8. Start safari → IN_PROGRESS
9. Complete safari → COMPLETED
10. Close safari → CLOSED

**Expected:** All transitions succeed with 200 responses

### Hold/Resume Test

1. Safari in CONFIRMED state
2. Hold with reason PENDING_DOCUMENTS → ON_HOLD
3. Release hold → CONFIRMED
4. Continue normal flow

**Expected:** Safari returns to previous state

### Cancellation with Refund Test

1. Safari in FULLY_PAID state
2. Cancel with reason BY_CLIENT → CANCELLED
3. Initiate refund → REFUND_PENDING
4. Record partial refund → REFUND_PENDING
5. Record final refund → REFUND_COMPLETE

**Expected:** Refund tracking accurate, terminal state reached

### Dispute Resolution Test

1. Safari in COMPLETED state
2. Mark disputed → DISPUTED
3. Resolve dispute with partial refund → REFUND_PENDING
4. Record refund → REFUND_COMPLETE

**Expected:** Dispute resolved, refund issued

### Invalid Transition Tests

1. Try to start safari from PENDING_PAYMENT → 400 error
2. Try to cancel safari in IN_PROGRESS → 400 error
3. Try to delete CONFIRMED safari → 400 error
4. Try to edit CLOSED safari → 400 error

**Expected:** All return appropriate error codes

---

## Error Handling

### Common Error Codes

| Error Code | HTTP Status | Description | Resolution |
|-----------|-------------|-------------|------------|
| `INVALID_STATE_TRANSITION` | 400 | Attempting invalid state change | Check workflow diagram for valid transitions |
| `SAFARI_NOT_FOUND` | 404 | Safari ID not found | Verify safari exists |
| `INVALID_SAFARI_ID` | 400 | Malformed obfuscated ID | Check ID format |
| `REASON_REQUIRED` | 400 | State change requires reason | Provide reason in request body |
| `VALIDATION_FAILED` | 400 | Safari doesn't meet state requirements | Complete required fields |
| `PERMISSION_DENIED` | 403 | User lacks required permission | Grant appropriate role/permission |
| `STATE_TRANSITION_FAILED` | 500 | Unexpected server error | Check logs, retry |

### Error Response Format

```json
{
  "status": 400,
  "message": "Cannot transition from In Progress to Draft",
  "errorCode": "INVALID_STATE_TRANSITION",
  "timestamp": "2024-12-20T10:30:00Z"
}
```

### Validation Rules

**Submit for Approval:**
- Must have itinerary (PUBLISHED)
- Must have customer
- Must have start date and end date
- Must have at least one pax
- Must have at least one day

**Approve Safari:**
- Previous state must be PENDING_APPROVAL
- Optional approval notes

**Start Safari:**
- Must be FULLY_PAID
- Start date must be today or in past

**Complete Safari:**
- Must be IN_PROGRESS
- End date must be today or in past

---

## Migration from 24-State System

### Mapping Old States to New States

| Old State | New State | Additional Field |
|-----------|-----------|------------------|
| DRAFT | DRAFT | - |
| PENDING_APPROVAL | PENDING_APPROVAL | - |
| APPROVED | APPROVED | - |
| CONFIRMED | CONFIRMED | - |
| PENDING_DEPOSIT | PENDING_PAYMENT | depositPaid = false |
| DEPOSIT_PAID | PENDING_PAYMENT | depositPaid = true |
| FULLY_PAID | FULLY_PAID | - |
| READY | FULLY_PAID | (SafariPhase shows time urgency) |
| IN_PROGRESS | IN_PROGRESS | - |
| COMPLETED | COMPLETED | - |
| PENDING_REVIEW | COMPLETED | reviewRequested = true |
| CLOSED | CLOSED | - |
| ON_HOLD | ON_HOLD | holdReason = OTHER |
| PENDING_DOCUMENTS | ON_HOLD | holdReason = PENDING_DOCUMENTS |
| PENDING_AVAILABILITY | ON_HOLD | holdReason = PENDING_AVAILABILITY |
| POSTPONED | ON_HOLD | holdReason = RESCHEDULING |
| RESCHEDULING | ON_HOLD | holdReason = RESCHEDULING |
| CANCELLATION_REQUESTED | CANCELLED | (direct to cancelled) |
| CANCELLED | CANCELLED | cancellationReason = OTHER |
| CANCELLED_BY_CLIENT | CANCELLED | cancellationReason = BY_CLIENT |
| CANCELLED_BY_OPERATOR | CANCELLED | cancellationReason = BY_OPERATOR |
| CANCELLED_FORCE_MAJEURE | CANCELLED | cancellationReason = FORCE_MAJEURE |
| REFUND_PENDING | REFUND_PENDING | - |
| REFUND_PARTIAL | REFUND_PENDING | (track amount paid) |
| REFUND_COMPLETE | REFUND_COMPLETE | - |
| DISPUTED | DISPUTED | - |
| UNDER_INVESTIGATION | DISPUTED | investigationStatus field |

### Migration SQL Script

```sql
-- Add new enum fields to safari table
ALTER TABLE safaris
ADD COLUMN hold_reason VARCHAR(50),
ADD COLUMN cancellation_reason VARCHAR(50),
ADD COLUMN previous_state VARCHAR(50);

-- Migrate PENDING_DEPOSIT and DEPOSIT_PAID to PENDING_PAYMENT
UPDATE safaris
SET state = 'PENDING_PAYMENT', deposit_paid = FALSE
WHERE state = 'PENDING_DEPOSIT';

UPDATE safaris
SET state = 'PENDING_PAYMENT', deposit_paid = TRUE
WHERE state = 'DEPOSIT_PAID';

-- Migrate READY to FULLY_PAID (time urgency now in SafariPhase)
UPDATE safaris
SET state = 'FULLY_PAID'
WHERE state = 'READY';

-- Migrate PENDING_REVIEW to COMPLETED
UPDATE safaris
SET state = 'COMPLETED', review_requested = TRUE
WHERE state = 'PENDING_REVIEW';

-- Migrate hold states to ON_HOLD with reasons
UPDATE safaris
SET state = 'ON_HOLD', hold_reason = 'PENDING_DOCUMENTS'
WHERE state = 'PENDING_DOCUMENTS';

UPDATE safaris
SET state = 'ON_HOLD', hold_reason = 'PENDING_AVAILABILITY'
WHERE state = 'PENDING_AVAILABILITY';

UPDATE safaris
SET state = 'ON_HOLD', hold_reason = 'RESCHEDULING'
WHERE state IN ('POSTPONED', 'RESCHEDULING');

-- Migrate cancellation states to CANCELLED with reasons
UPDATE safaris
SET state = 'CANCELLED', cancellation_reason = 'BY_CLIENT'
WHERE state IN ('CANCELLED_BY_CLIENT', 'CANCELLATION_REQUESTED');

UPDATE safaris
SET state = 'CANCELLED', cancellation_reason = 'BY_OPERATOR'
WHERE state = 'CANCELLED_BY_OPERATOR';

UPDATE safaris
SET state = 'CANCELLED', cancellation_reason = 'FORCE_MAJEURE'
WHERE state = 'CANCELLED_FORCE_MAJEURE';

-- Migrate REFUND_PARTIAL to REFUND_PENDING
UPDATE safaris
SET state = 'REFUND_PENDING'
WHERE state = 'REFUND_PARTIAL';

-- Migrate UNDER_INVESTIGATION to DISPUTED
UPDATE safaris
SET state = 'DISPUTED'
WHERE state = 'UNDER_INVESTIGATION';
```

---

## Related Files

- [SafariState.java](Enums/SafariState.java) - State enum definition
- [SafariPhase.java](Enums/SafariPhase.java) - Phase enum (time-based)
- [SafariHoldReason.java](Enums/SafariHoldReason.java) - Hold reason enum
- [SafariCancellationReason.java](Enums/SafariCancellationReason.java) - Cancellation reason enum
- [SafariStateTransitionService.java](Services/SafariStateTransitionService.java) - State transition logic
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Workflow permissions

---

**Document Version:** 2.0
**Implementation Status:** ✅ Complete
**Last Review:** 2026-02-06
