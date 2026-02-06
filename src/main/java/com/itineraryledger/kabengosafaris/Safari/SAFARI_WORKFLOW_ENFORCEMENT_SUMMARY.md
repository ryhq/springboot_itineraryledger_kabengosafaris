# Safari Workflow Enforcement Summary

**Last Updated:** 2026-02-06
**Safari Workflow Version:** 14-State Simplified Workflow

## Overview

This document summarizes how the Safari workflow is enforced across all CRUD services and controllers. It ensures that business rules are consistently applied and safaris maintain data integrity throughout their lifecycle.

---

## Table of Contents

1. [Workflow States](#workflow-states)
2. [Enforcement Strategy](#enforcement-strategy)
3. [State-Based Access Control](#state-based-access-control)
4. [Service-by-Service Enforcement](#service-by-service-enforcement)
5. [Controller Enforcement](#controller-enforcement)
6. [Error Codes Reference](#error-codes-reference)
7. [Testing Recommendations](#testing-recommendations)

---

## Workflow States

### Core Journey States (9)
1. **DRAFT** - Safari booking is being prepared
2. **PENDING_APPROVAL** - Submitted, awaiting management approval
3. **APPROVED** - Approved by management
4. **CONFIRMED** - Confirmed with client and suppliers
5. **PENDING_PAYMENT** - Awaiting deposit or full payment
6. **FULLY_PAID** - All payments received
7. **IN_PROGRESS** - Safari is currently running
8. **COMPLETED** - Safari has successfully ended
9. **CLOSED** - Safari fully completed with all post-trip tasks done

### Exception/Special States (5)
10. **ON_HOLD** - Safari temporarily paused (with reason)
11. **CANCELLED** - Safari has been cancelled (with reason)
12. **REFUND_PENDING** - Refund process initiated, awaiting completion
13. **REFUND_COMPLETE** - Refund has been fully issued
14. **DISPUTED** - Client has raised a dispute, under investigation

---

## Enforcement Strategy

Safari workflow enforcement is implemented at **three layers**:

### Layer 1: Entity-Level Business Logic
- `Safari` entity methods: `isEditable()`, `isCancellable()`
- `SafariState` enum methods: `isEditable()`, `isCancellable()`, `isTerminal()`, `isActive()`
- Provides basic state classification

### Layer 2: Service-Level Enforcement
- `SafariUpdateService` - Enforces state-based edit restrictions
- `SafariDeleteService` - Enforces state-based deletion restrictions
- `SafariStateTransitionService` - Enforces valid state transitions
- Granular field-level validation based on current state

### Layer 3: Controller-Level Access Control
- `SafariController` - Standard CRUD operations with permission checks
- `SafariStateTransitionController` - Workflow operations with specific permissions
- Spring Security `@PreAuthorize` annotations for permission-based access

---

## State-Based Access Control

### Editability Matrix

| State | Fully Editable | Limited Fields | Notes Only | Read-Only |
|-------|:--------------:|:--------------:|:----------:|:---------:|
| **DRAFT** | ✅ | - | - | - |
| **PENDING_APPROVAL** | ✅ | - | - | - |
| **APPROVED** | - | ✅ (Non-critical) | - | - |
| **CONFIRMED** | - | ✅ (Logistics) | - | - |
| **PENDING_PAYMENT** | - | ✅ (Payment-related) | - | - |
| **FULLY_PAID** | - | - | ✅ (internalNotes) | - |
| **IN_PROGRESS** | - | - | ✅ (internalNotes) | - |
| **COMPLETED** | - | - | ✅ (review notes) | - |
| **CLOSED** | - | - | - | ✅ |
| **ON_HOLD** | ✅ | - | - | - |
| **CANCELLED** | - | - | ✅ (internalNotes) | - |
| **REFUND_PENDING** | - | - | ✅ (internalNotes) | - |
| **REFUND_COMPLETE** | - | - | - | ✅ |
| **DISPUTED** | - | - | ✅ (investigation notes) | - |

### Deletability Matrix

| State | Can Delete | Reason |
|-------|:----------:|--------|
| **DRAFT** | ✅ | Not yet submitted |
| **PENDING_APPROVAL** | ✅ | Not yet approved |
| **APPROVED** | ❌ | Management has approved - cannot delete |
| **CONFIRMED** | ❌ | Confirmed with client/suppliers |
| **PENDING_PAYMENT** | ❌ | Financial commitment exists |
| **FULLY_PAID** | ❌ | Payments received |
| **IN_PROGRESS** | ❌ | Safari is running |
| **COMPLETED** | ❌ | Historical record |
| **CLOSED** | ❌ | Archived |
| **ON_HOLD** | ❌ | Release from hold or cancel first |
| **CANCELLED** | ❌ | Historical record for cancellation |
| **REFUND_PENDING** | ❌ | Refund in process |
| **REFUND_COMPLETE** | ❌ | Financial record |
| **DISPUTED** | ❌ | Under investigation |

### Field-Level Edit Restrictions

#### APPROVED State - Non-Critical Fields Only
**Blocked Fields:**
- `startDate` - Critical booking detail
- `name` - Primary identifier

**Allowed Fields:**
- `carCount`, `description`, `highlights`
- `startLocation`, `endLocation`
- `specialRequests`, `dietaryRequirements`
- `emergencyContact`, `internalNotes`

#### CONFIRMED State - Logistics Fields Only
**Blocked Fields:**
- `startDate` - Dates are locked
- `name` - Name is locked
- `description`, `highlights` - Marketing content locked

**Allowed Fields:**
- `carCount` - Logistics adjustment
- `startLocation`, `endLocation` - Pick-up/drop-off details
- `specialRequests`, `dietaryRequirements`
- `emergencyContact`, `internalNotes`

#### PENDING_PAYMENT State - Payment-Related Fields
**Blocked Fields:**
- `startDate` - Core booking detail
- `name` - Primary identifier
- `description`, `highlights` - Marketing content

**Allowed Fields:**
- `carCount` - Logistics adjustment
- `startLocation`, `endLocation`
- `specialRequests`, `dietaryRequirements`
- `emergencyContact`, `internalNotes`

#### FULLY_PAID, IN_PROGRESS, COMPLETED, CANCELLED, REFUND_PENDING, DISPUTED - Notes Only
**Blocked Fields:**
- All core fields

**Allowed Fields:**
- `internalNotes` only

#### CLOSED, REFUND_COMPLETE - Read-Only
**Blocked Fields:**
- ALL fields

**Allowed Fields:**
- None (completely read-only)

---

## Service-by-Service Enforcement

### SafariUpdateService

**Location:** `Safari/Services/SafariUpdateService.java`

**Enforcement Logic:**

```java
// WORKFLOW ENFORCEMENT: Check state-based edit restrictions
SafariState state = safari.getState();

// 1. FULLY_PAID - Notes only
if (state == SafariState.FULLY_PAID) {
    // Block all fields except internalNotes
    // Error: "FULLY_PAID_EDIT_BLOCKED"
}

// 2. IN_PROGRESS - Notes only
if (state == SafariState.IN_PROGRESS) {
    // Block all fields except internalNotes
    // Error: "IN_PROGRESS_EDIT_BLOCKED"
}

// 3. COMPLETED - Notes only
if (state == SafariState.COMPLETED) {
    // Block all fields except internalNotes
    // Error: "COMPLETED_EDIT_BLOCKED"
}

// 4. CLOSED - Completely read-only
if (state == SafariState.CLOSED) {
    // Block ALL changes
    // Error: "CLOSED_EDIT_BLOCKED"
}

// 5. CANCELLED - Notes only
if (state == SafariState.CANCELLED) {
    // Block all fields except internalNotes
    // Error: "CANCELLED_EDIT_BLOCKED"
}

// 6. REFUND_PENDING - Notes only
if (state == SafariState.REFUND_PENDING) {
    // Block all fields except internalNotes
    // Error: "REFUND_PENDING_EDIT_BLOCKED"
}

// 7. REFUND_COMPLETE - Completely read-only
if (state == SafariState.REFUND_COMPLETE) {
    // Block ALL changes
    // Error: "REFUND_COMPLETE_EDIT_BLOCKED"
}

// 8. DISPUTED - Investigation notes only
if (state == SafariState.DISPUTED) {
    // Block all fields except internalNotes
    // Error: "DISPUTED_EDIT_BLOCKED"
}

// 9. APPROVED - Non-critical fields only
if (state == SafariState.APPROVED) {
    // Block: startDate, name
    // Allow: carCount, locations, requests, notes
    // Error: "APPROVED_CRITICAL_EDIT_BLOCKED"
}

// 10. CONFIRMED - Limited logistics fields
if (state == SafariState.CONFIRMED) {
    // Block: startDate, name, description, highlights
    // Allow: carCount, locations, special requests, notes
    // Error: "CONFIRMED_EDIT_BLOCKED"
}

// 11. PENDING_PAYMENT - Payment-related fields
if (state == SafariState.PENDING_PAYMENT) {
    // Block: startDate, name, description, highlights
    // Allow: carCount, locations, special requests, notes
    // Error: "PENDING_PAYMENT_EDIT_BLOCKED"
}

// 12-14. DRAFT, PENDING_APPROVAL, ON_HOLD - Fully editable
// No restrictions
```

**API Response Examples:**

```json
// Blocked edit attempt on FULLY_PAID safari
{
  "status": 400,
  "message": "Cannot modify fields (name, startDate, carCount) on FULLY_PAID safari. Only internalNotes can be edited. Use workflow endpoints to manage the safari.",
  "errorCode": "FULLY_PAID_EDIT_BLOCKED",
  "data": null
}

// Blocked critical field edit on APPROVED safari
{
  "status": 400,
  "message": "Cannot modify critical fields (startDate, name) on APPROVED safari. Revert to DRAFT first.",
  "errorCode": "APPROVED_CRITICAL_EDIT_BLOCKED",
  "data": null
}
```

---

### SafariDeleteService

**Location:** `Safari/Services/SafariDeleteService.java`

**Enforcement Logic:**

```java
// WORKFLOW ENFORCEMENT: Only allow deletion of DRAFT and PENDING_APPROVAL
SafariState state = safari.getState();

// 1. APPROVED, CONFIRMED, PENDING_PAYMENT, FULLY_PAID - Cannot delete approved/confirmed bookings
if (state == SafariState.APPROVED || state == SafariState.CONFIRMED ||
    state == SafariState.PENDING_PAYMENT || state == SafariState.FULLY_PAID) {
    // Error: "Cannot delete approved/confirmed bookings"
    skipped++;
}

// 2. IN_PROGRESS - Safari is running
if (state == SafariState.IN_PROGRESS) {
    // Error: "Cannot delete safari - safari is currently IN_PROGRESS"
    skipped++;
}

// 3. COMPLETED, CLOSED - Historical record
if (state == SafariState.COMPLETED || state == SafariState.CLOSED) {
    // Error: "Cannot delete safari - safari has ended"
    skipped++;
}

// 4. CANCELLED, REFUND_PENDING, REFUND_COMPLETE - Financial/cancellation records
if (state == SafariState.CANCELLED || state == SafariState.REFUND_PENDING ||
    state == SafariState.REFUND_COMPLETE) {
    // Error: "Cannot delete safari - safari has been cancelled/refunded"
    skipped++;
}

// 5. DISPUTED - Under investigation
if (state == SafariState.DISPUTED) {
    // Error: "Cannot delete safari - safari is DISPUTED and under investigation"
    skipped++;
}

// 6. ON_HOLD - Must release or cancel first
if (state == SafariState.ON_HOLD) {
    // Error: "Cannot delete safari - safari is ON_HOLD (release from hold or cancel first)"
    skipped++;
}

// Only DRAFT and PENDING_APPROVAL reach here - allowed to delete
```

**API Response Examples:**

```json
// Attempt to delete APPROVED safari
{
  "status": 400,
  "message": "0 safaris deleted. 1 safari skipped: Cannot delete safari SAF-000123 - state is Approved (cannot delete approved/confirmed bookings)",
  "errorCode": "NO_SAFARIS_DELETED",
  "data": [
    "Cannot delete safari SAF-000123 - state is Approved (cannot delete approved/confirmed bookings)"
  ]
}

// Mixed batch delete (1 DRAFT, 1 CONFIRMED)
{
  "status": 200,
  "message": "1 safari deleted successfully. 1 safari skipped",
  "errorCode": null,
  "data": [
    "Cannot delete safari SAF-000456 - state is Confirmed (cannot delete approved/confirmed bookings)"
  ]
}
```

---

### SafariStateTransitionService

**Location:** `Safari/Services/SafariStateTransitionService.java`

**Enforcement Logic:**

Each workflow method validates:
1. **Current state requirements** - Safari must be in specific state(s)
2. **Business rule validation** - Required data must be present
3. **State transition legality** - Target state must be reachable
4. **Required fields** - Reason, dates, or flags as needed

**Example: submitForApproval()**

```java
// Validate current state
Set<SafariState> allowedStates = Set.of(SafariState.DRAFT);
if (!allowedStates.contains(safari.getState())) {
    return error(400, "Cannot submit from " + safari.getState(), "INVALID_STATE_TRANSITION");
}

// Validate business rules
if (safari.getItinerary() == null) {
    return error(400, "Safari must have an itinerary", "MISSING_ITINERARY");
}
if (safari.getCustomer() == null) {
    return error(400, "Safari must have a customer", "MISSING_CUSTOMER");
}
if (safari.getStartDate() == null || safari.getEndDate() == null) {
    return error(400, "Safari must have dates", "MISSING_DATES");
}
if (safari.getPaxList() == null || safari.getPaxList().isEmpty()) {
    return error(400, "Safari must have at least one pax", "MISSING_PAX");
}

// Execute transition
safari.changeState(SafariState.PENDING_APPROVAL, "Submitted for approval");
```

**Example: holdSafari()**

```java
// Require holdReason enum
if (dto == null || dto.getHoldReason() == null) {
    return error(400, "holdReason is required", "MISSING_HOLD_REASON");
}

// Require detailed reason
if (dto.getReason() == null || dto.getReason().isBlank()) {
    return error(400, "reason is required", "MISSING_REASON");
}

// Store previous state for returning from hold
safari.setPreviousState(safari.getState());

// Format reason with enum context
String fullReason = String.format("[%s] %s",
    dto.getHoldReason().getDisplayName(),
    dto.getReason());

// Execute transition
safari.changeState(SafariState.ON_HOLD, fullReason);
```

**Example: recordPayment()**

```java
// Validate current state
Set<SafariState> allowedStates = Set.of(SafariState.PENDING_PAYMENT);
if (!allowedStates.contains(safari.getState())) {
    return error(400, "Can only record payment from PENDING_PAYMENT", "INVALID_STATE_TRANSITION");
}

// Determine target state based on isFullPayment flag
boolean isFullyPaid = dto != null && dto.getIsFullPayment() != null
    ? dto.getIsFullPayment()
    : false;

SafariState targetState = isFullyPaid
    ? SafariState.FULLY_PAID
    : SafariState.PENDING_PAYMENT;

String reason = isFullyPaid
    ? "Full payment received"
    : "Partial payment received";

// Execute transition
safari.changeState(targetState, reason);
```

---

## Controller Enforcement

### SafariController

**Location:** `Safari/Controller/SafariController.java`

**Base URL:** `/api/safaris`

**Enforcement:**
- Standard CRUD operations
- Permission-based access control via `@PreAuthorize`
- Delegates workflow enforcement to services

**Key Endpoints:**
- `POST /api/safaris` - Create safari (requires `PERM_CREATE_SAFARI`)
- `GET /api/safaris/{id}` - Get safari (requires `PERM_READ_SAFARI`)
- `PUT /api/safaris/{id}` - Update safari (requires `PERM_UPDATE_SAFARI`, enforced by SafariUpdateService)
- `DELETE /api/safaris` - Delete safaris (requires `PERM_DELETE_SAFARI`, enforced by SafariDeleteService)

---

### SafariStateTransitionController

**Location:** `Safari/Controller/SafariStateTransitionController.java`

**Base URL:** `/api/safaris/{id}/state`

**Enforcement:**
- Workflow-specific operations
- Fine-grained permission checks per transition
- Each endpoint maps to a specific business operation

**Workflow Endpoints (14 total):**

#### Core Journey - Booking (4 endpoints)
1. `POST /submit-for-approval` - DRAFT → PENDING_APPROVAL (requires `PERM_SUBMIT_SAFARI_FOR_APPROVAL`)
2. `POST /approve` - PENDING_APPROVAL → APPROVED (requires `PERM_APPROVE_SAFARI`)
3. `POST /reject` - PENDING_APPROVAL → DRAFT (requires `PERM_REJECT_SAFARI`)
4. `POST /confirm` - APPROVED → CONFIRMED (requires `PERM_CONFIRM_SAFARI`)

#### Core Journey - Payment (1 endpoint)
5. `POST /record-payment` - Payment handling (requires `PERM_RECORD_SAFARI_PAYMENT`)
   - PENDING_PAYMENT → PENDING_PAYMENT (partial)
   - PENDING_PAYMENT → FULLY_PAID (full)

#### Core Journey - Operational (2 endpoints)
6. `POST /start` - FULLY_PAID → IN_PROGRESS (requires `PERM_START_SAFARI`)
7. `POST /complete` - IN_PROGRESS → COMPLETED (requires `PERM_COMPLETE_SAFARI`)

#### Core Journey - Post-Safari (1 endpoint)
8. `POST /close` - COMPLETED → CLOSED (requires `PERM_CLOSE_SAFARI`)

#### Exception States - Hold Management (2 endpoints)
9. `POST /hold` - Any → ON_HOLD (requires `PERM_HOLD_SAFARI`)
10. `POST /release-hold` - ON_HOLD → previous/target state (requires `PERM_RELEASE_SAFARI_HOLD`)

#### Exception States - Cancellation (1 endpoint)
11. `POST /cancel` - Any → CANCELLED (requires `PERM_CANCEL_SAFARI`)

#### Exception States - Refund (2 endpoints)
12. `POST /initiate-refund` - CANCELLED → REFUND_PENDING (requires `PERM_INITIATE_SAFARI_REFUND`)
13. `POST /record-refund` - Refund handling (requires `PERM_RECORD_SAFARI_REFUND`)
    - REFUND_PENDING → REFUND_PENDING (partial)
    - REFUND_PENDING → REFUND_COMPLETE (final)

#### Exception States - Dispute (2 endpoints)
14. `POST /mark-disputed` - Any → DISPUTED (requires `PERM_MARK_SAFARI_DISPUTED`)
15. `POST /resolve-dispute` - DISPUTED → resolution state (requires `PERM_RESOLVE_SAFARI_DISPUTE`)

---

## Error Codes Reference

### Update Service Error Codes

| Error Code | Description | HTTP Status | When It Occurs |
|------------|-------------|:-----------:|----------------|
| `INVALID_SAFARI_ID` | Invalid obfuscated ID | 400 | ID decoding fails |
| `SAFARI_NOT_FOUND` | Safari does not exist | 404 | Safari not found in database |
| `FULLY_PAID_EDIT_BLOCKED` | Cannot edit FULLY_PAID safari | 400 | Attempting to edit non-notes fields on FULLY_PAID safari |
| `IN_PROGRESS_EDIT_BLOCKED` | Cannot edit IN_PROGRESS safari | 400 | Attempting to edit non-notes fields on running safari |
| `COMPLETED_EDIT_BLOCKED` | Cannot edit COMPLETED safari | 400 | Attempting to edit non-notes fields on completed safari |
| `CLOSED_EDIT_BLOCKED` | Cannot edit CLOSED safari | 400 | Attempting any edit on closed safari |
| `CANCELLED_EDIT_BLOCKED` | Cannot edit CANCELLED safari | 400 | Attempting to edit non-notes fields on cancelled safari |
| `REFUND_PENDING_EDIT_BLOCKED` | Cannot edit REFUND_PENDING safari | 400 | Attempting to edit non-notes fields during refund |
| `REFUND_COMPLETE_EDIT_BLOCKED` | Cannot edit REFUND_COMPLETE safari | 400 | Attempting any edit on completed refund |
| `DISPUTED_EDIT_BLOCKED` | Cannot edit DISPUTED safari | 400 | Attempting to edit non-notes fields on disputed safari |
| `APPROVED_CRITICAL_EDIT_BLOCKED` | Cannot edit critical fields on APPROVED | 400 | Attempting to edit name/startDate on approved safari |
| `CONFIRMED_EDIT_BLOCKED` | Cannot edit restricted fields on CONFIRMED | 400 | Attempting to edit core booking fields on confirmed safari |
| `PENDING_PAYMENT_EDIT_BLOCKED` | Cannot edit restricted fields on PENDING_PAYMENT | 400 | Attempting to edit core fields during payment phase |
| `START_DATE_IN_PAST` | Start date cannot be in the past | 400 | Attempting to set past start date |
| `SAFARI_UPDATE_FAILED` | Generic update failure | 500 | Unexpected error during update |

### Delete Service Error Codes

| Error Code | Description | HTTP Status | When It Occurs |
|------------|-------------|:-----------:|----------------|
| `NO_SAFARIS_DELETED` | All deletions blocked | 400 | No safaris could be deleted due to state restrictions |
| `DELETION_BLOCKED` | Deletion not allowed | 400 | Specific safari cannot be deleted due to state |
| `SAFARIS_DELETE_FAILED` | Generic delete failure | 500 | Unexpected error during deletion |

### State Transition Service Error Codes

| Error Code | Description | HTTP Status | When It Occurs |
|------------|-------------|:-----------:|----------------|
| `INVALID_STATE_TRANSITION` | Transition not allowed from current state | 400 | Invalid source state for operation |
| `MISSING_ITINERARY` | Safari missing itinerary | 400 | Submit for approval without itinerary |
| `MISSING_CUSTOMER` | Safari missing customer | 400 | Submit for approval without customer |
| `MISSING_DATES` | Safari missing dates | 400 | Submit for approval without dates |
| `MISSING_PAX` | Safari missing passengers | 400 | Submit for approval without pax |
| `MISSING_HOLD_REASON` | Hold reason enum required | 400 | Attempting to hold without holdReason |
| `MISSING_CANCELLATION_REASON` | Cancellation reason enum required | 400 | Attempting to cancel without cancellationReason |
| `MISSING_REASON` | Detailed reason text required | 400 | Missing required reason field |
| `SAFARI_TRANSITION_FAILED` | Generic transition failure | 500 | Unexpected error during state change |

---

## Testing Recommendations

### Unit Testing

**SafariUpdateService Tests:**
```java
@Test
void testCannotEditCriticalFieldsOnApprovedSafari() {
    // Given: Safari in APPROVED state
    Safari safari = createSafari(SafariState.APPROVED);

    // When: Attempt to edit startDate
    UpdateSafariDTO dto = new UpdateSafariDTO();
    dto.setStartDate(LocalDate.now().plusDays(10));

    ResponseEntity<ApiResponse<?>> response = updateService.updateSafari(
        obfuscate(safari.getId()), dto
    );

    // Then: Update is blocked
    assertEquals(400, response.getStatusCodeValue());
    ApiResponse<?> body = response.getBody();
    assertEquals("APPROVED_CRITICAL_EDIT_BLOCKED", body.getErrorCode());
}

@Test
void testCanEditNotesOnFullyPaidSafari() {
    // Given: Safari in FULLY_PAID state
    Safari safari = createSafari(SafariState.FULLY_PAID);

    // When: Edit internalNotes only
    UpdateSafariDTO dto = new UpdateSafariDTO();
    dto.setInternalNotes("Updated payment confirmation received");

    ResponseEntity<ApiResponse<?>> response = updateService.updateSafari(
        obfuscate(safari.getId()), dto
    );

    // Then: Update succeeds
    assertEquals(200, response.getStatusCodeValue());
}
```

**SafariDeleteService Tests:**
```java
@Test
void testCanDeleteDraftSafari() {
    // Given: Safari in DRAFT state
    Safari safari = createSafari(SafariState.DRAFT);

    // When: Attempt to delete
    ResponseEntity<ApiResponse<?>> response = deleteService.deleteSafaris(
        List.of(obfuscate(safari.getId()))
    );

    // Then: Deletion succeeds
    assertEquals(200, response.getStatusCodeValue());
}

@Test
void testCannotDeleteApprovedSafari() {
    // Given: Safari in APPROVED state
    Safari safari = createSafari(SafariState.APPROVED);

    // When: Attempt to delete
    ResponseEntity<ApiResponse<?>> response = deleteService.deleteSafaris(
        List.of(obfuscate(safari.getId()))
    );

    // Then: Deletion is blocked
    assertEquals(400, response.getStatusCodeValue());
    ApiResponse<?> body = response.getBody();
    assertEquals("NO_SAFARIS_DELETED", body.getErrorCode());
}
```

### Integration Testing

**Workflow Transition Tests:**
```java
@Test
@WithMockUser(authorities = {"PERM_SUBMIT_SAFARI_FOR_APPROVAL"})
void testCompleteBookingWorkflow() throws Exception {
    // 1. Create DRAFT safari
    Safari safari = createCompleteSafari();

    // 2. Submit for approval
    mvc.perform(post("/api/safaris/" + obfuscate(safari.getId()) + "/state/submit-for-approval"))
       .andExpect(status().isOk());

    // 3. Approve
    mvc.perform(post("/api/safaris/" + obfuscate(safari.getId()) + "/state/approve"))
       .andExpect(status().isOk());

    // 4. Confirm
    mvc.perform(post("/api/safaris/" + obfuscate(safari.getId()) + "/state/confirm"))
       .andExpect(status().isOk());

    // 5. Record full payment
    SafariStateTransitionDTO dto = new SafariStateTransitionDTO();
    dto.setIsFullPayment(true);

    mvc.perform(post("/api/safaris/" + obfuscate(safari.getId()) + "/state/record-payment")
       .contentType(MediaType.APPLICATION_JSON)
       .content(objectMapper.writeValueAsString(dto)))
       .andExpect(status().isOk());

    // 6. Verify final state
    Safari updated = safariRepository.findById(safari.getId()).orElseThrow();
    assertEquals(SafariState.FULLY_PAID, updated.getState());
}
```

**Edit Restriction Tests:**
```java
@Test
@WithMockUser(authorities = {"PERM_UPDATE_SAFARI"})
void testEditRestrictionsAcrossStates() throws Exception {
    // Test each state's edit restrictions
    for (SafariState state : SafariState.values()) {
        Safari safari = createSafari(state);

        UpdateSafariDTO dto = new UpdateSafariDTO();
        dto.setName("New Name");
        dto.setCarCount(5);

        mvc.perform(put("/api/safaris/" + obfuscate(safari.getId()))
           .contentType(MediaType.APPLICATION_JSON)
           .content(objectMapper.writeValueAsString(dto)))
           .andExpect(result -> {
               int status = result.getResponse().getStatus();

               // Verify expected behavior for each state
               if (state == SafariState.DRAFT ||
                   state == SafariState.PENDING_APPROVAL ||
                   state == SafariState.ON_HOLD) {
                   assertEquals(200, status, "Should allow edit in " + state);
               } else if (state == SafariState.CLOSED ||
                          state == SafariState.REFUND_COMPLETE) {
                   assertEquals(400, status, "Should block all edits in " + state);
               }
           });
    }
}
```

### Manual Testing Scenarios

**Scenario 1: Happy Path - Complete Safari Lifecycle**
1. Create DRAFT safari with all required data
2. Submit for approval → PENDING_APPROVAL
3. Approve → APPROVED
4. Confirm → CONFIRMED
5. Record full payment → FULLY_PAID
6. Start safari → IN_PROGRESS
7. Complete safari → COMPLETED
8. Close safari → CLOSED

**Scenario 2: Hold and Resume**
1. Create and approve safari → APPROVED
2. Put on hold (PENDING_DOCUMENTS) → ON_HOLD
3. Verify can still edit while on hold
4. Release from hold → APPROVED
5. Continue workflow

**Scenario 3: Cancellation with Refund**
1. Create and fully pay safari → FULLY_PAID
2. Cancel (BY_CLIENT with refund) → CANCELLED
3. Initiate refund → REFUND_PENDING
4. Record partial refund → REFUND_PENDING
5. Record final refund → REFUND_COMPLETE

**Scenario 4: Dispute Resolution**
1. Complete safari → COMPLETED
2. Mark as disputed → DISPUTED
3. Resolve dispute → CLOSED (or REFUND_PENDING/REFUND_COMPLETE)

**Scenario 5: Edit Restriction Validation**
1. Create safari in various states
2. Attempt to edit different fields
3. Verify correct blocking behavior
4. Verify error messages are clear and actionable

---

## Migration Notes

When migrating from the old 24-state system:

### Database Changes Required
- Add `previousState` column (for ON_HOLD tracking)
- Add `holdReason` column (stores SafariHoldReason enum)
- Add `cancellationReason` column (stores SafariCancellationReason enum)

### State Mapping
See SAFARI_WORKFLOW.md "Migration from 24-State System" section for complete mapping table.

### Code Changes
1. Update all service method calls to use new workflow endpoints
2. Replace multiple cancellation methods with single `cancelSafari()` + reason
3. Replace multiple payment methods with single `recordPayment()` + flag
4. Remove references to removed states (READY, PENDING_REVIEW, etc.)

### Frontend Changes
1. Update state transition buttons based on new workflow
2. Add reason dropdowns for hold/cancel operations
3. Update status badges for new state names
4. Modify forms to handle isFullPayment and isFinalRefund flags

---

## Related Documentation

- **SAFARI_WORKFLOW.md** - Complete workflow specification with API endpoints
- **SafariState.java** - Safari state enum definition
- **SafariHoldReason.java** - Hold reason enum
- **SafariCancellationReason.java** - Cancellation reason enum
- **custom-permissions.json** - Permission definitions for workflow operations

---

*This document is maintained by the Kabengo Safaris development team.*
*For questions or clarifications, consult SAFARI_WORKFLOW.md or reach out to the tech lead.*
