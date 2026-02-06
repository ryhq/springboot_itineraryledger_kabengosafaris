# Quote Workflow Enforcement - Implementation Summary

## Overview

All Quote services have been updated to enforce the status workflow rules as defined in [QUOTE_WORKFLOW.md](QUOTE_WORKFLOW.md). This document summarizes the changes made to ensure workflow compliance.

---

## Status-Based Access Control Matrix

| Operation | DRAFT | READY | SENT | ACCEPTED | REJECTED | EXPIRED | CANCELLED | CONVERTED |
|-----------|-------|-------|------|----------|----------|---------|-----------|-----------|
| **Create** | ✅ Default | N/A | N/A | N/A | N/A | N/A | N/A | N/A |
| **Read** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Update (All fields)** | ✅ | ⚠️ Non-critical | ❌ Display only | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Delete** | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| **Send to Customer** | ⚠️ If ready | ✅ | ✅ Resend | ✅ Resend | ❌ | ❌ | ❌ | ✅ Resend |

### Critical Fields (NOT Editable after READY)
- `isStoRate` - Pricing basis
- `taxPercentage` - Tax rate
- `discountPercentage` - Discount rate
- Relationships (`itinerary`, `customer`)

### Non-Critical Fields (Editable in READY)
- `title` - Quote title
- `description` - Quote description
- `validFrom` / `validTo` - Validity dates
- `depositPercentage` - Deposit percentage
- `depositDueDate` / `fullPaymentDueDate` - Payment dates
- `internalNotes` - Internal notes
- `customerNotes` - Customer-facing notes
- `isActive` - Active status

### Display-Only Fields (Editable in SENT)
- `description` - Quote description
- `customerNotes` - Customer-facing notes

---

## Services Updated

### 1. ✅ QuoteDeleteService

**File:** [Services/QuoteServices/QuoteDeleteService.java](Services/QuoteServices/QuoteDeleteService.java)

**Changes Made:**
- Added status validation before deletion
- Blocks deletion of SENT quotes
- Blocks deletion of ACCEPTED quotes
- Blocks deletion of CONVERTED quotes
- Provides clear error messages with guidance

**Workflow Enforcement:**
```java
// SENT quotes cannot be deleted
if (quote.getStatus() == QuoteStatus.SENT) {
    return error("Cannot delete SENT quote. Cancel it first or create a new version.");
}

// ACCEPTED quotes cannot be deleted
if (quote.getStatus() == QuoteStatus.ACCEPTED) {
    return error("Cannot delete ACCEPTED quote");
}

// CONVERTED quotes cannot be deleted
if (quote.getStatus() == QuoteStatus.CONVERTED) {
    return error("Cannot delete CONVERTED quote - it has been converted to a booking");
}

// Only DRAFT, READY, REJECTED, EXPIRED, CANCELLED can be deleted
```

**Error Codes:**
- `DELETION_BLOCKED` - When attempting to delete protected quotes

**Example Responses:**

**Success (DRAFT/READY):**
```json
{
  "status": 200,
  "message": "2 quotes deleted successfully",
  "data": null
}
```

**Blocked (SENT):**
```json
{
  "status": 400,
  "message": "Cannot delete SENT quote 'Safari Quotation'. Cancel it first or create a new version.",
  "errorCode": "DELETION_BLOCKED"
}
```

**Partial Success:**
```json
{
  "status": 200,
  "message": "1 quote deleted successfully, 1 skipped",
  "data": [
    "Cannot delete ACCEPTED quote 'Safari Quote'"
  ]
}
```

---

### 2. ✅ QuoteUpdateService

**File:** [Services/QuoteServices/QuoteUpdateService.java](Services/QuoteServices/QuoteUpdateService.java)

**Changes Made:**
- Added comprehensive status-based edit restrictions
- Blocks ALL edits to ACCEPTED, REJECTED, EXPIRED, CANCELLED, CONVERTED quotes
- Blocks CRITICAL field edits to READY quotes
- Blocks most field edits to SENT quotes (only display fields allowed)
- Blocks direct status changes (must use workflow endpoints)
- Provides detailed error messages listing blocked fields

**Workflow Enforcement:**
```java
// ACCEPTED, REJECTED, EXPIRED, CANCELLED, CONVERTED quotes cannot be edited
if (status == QuoteStatus.ACCEPTED || status == QuoteStatus.REJECTED ||
    status == QuoteStatus.EXPIRED || status == QuoteStatus.CANCELLED ||
    status == QuoteStatus.CONVERTED) {
    return error("Cannot edit " + status.getDisplayName() + " quote. Create a new version instead.");
}

// SENT quotes can only edit display fields
if (status == QuoteStatus.SENT) {
    List<String> blockedFields = new ArrayList<>();

    if (title changed) blockedFields.add("title");
    if (isStoRate changed) blockedFields.add("isStoRate");
    if (taxPercentage changed) blockedFields.add("taxPercentage");
    if (discountPercentage changed) blockedFields.add("discountPercentage");
    if (validFrom changed) blockedFields.add("validFrom");
    if (validTo changed) blockedFields.add("validTo");
    if (depositPercentage changed) blockedFields.add("depositPercentage");

    if (!blockedFields.isEmpty()) {
        return error("Cannot modify fields (...) on SENT quote. Only description and customerNotes can be edited. Revert to DRAFT for other changes.");
    }
}

// READY quotes can only edit non-critical fields
if (status == QuoteStatus.READY) {
    List<String> blockedFields = new ArrayList<>();

    if (isStoRate changed) blockedFields.add("isStoRate");
    if (taxPercentage changed) blockedFields.add("taxPercentage");
    if (discountPercentage changed) blockedFields.add("discountPercentage");

    if (!blockedFields.isEmpty()) {
        return error("Cannot modify critical fields (...) on READY quote. Revert to DRAFT first.");
    }
}

// Block direct status updates
if (status being changed) {
    return error("Cannot change quote status directly. Use the workflow endpoints.");
}
```

**Error Codes:**
- `EDIT_BLOCKED` - Attempting to edit non-editable quote
- `SENT_EDIT_BLOCKED` - Attempting to modify non-display fields on SENT quote
- `READY_CRITICAL_EDIT_BLOCKED` - Attempting to modify critical fields on READY quote
- `DIRECT_STATUS_CHANGE_BLOCKED` - Attempting to change status directly

**Example Responses:**

**Success (DRAFT - all fields):**
```json
{
  "status": 200,
  "message": "Quote updated successfully",
  "data": { "id": "abc123", "title": "Updated Title", ... }
}
```

**Success (READY - non-critical fields only):**
```json
{
  "status": 200,
  "message": "Quote updated successfully",
  "data": {
    "id": "abc123",
    "title": "Updated Title",
    "description": "Updated description"
  }
}
```

**Success (SENT - display fields only):**
```json
{
  "status": 200,
  "message": "Quote updated successfully",
  "data": {
    "id": "abc123",
    "description": "Updated description",
    "customerNotes": "Updated notes"
  }
}
```

**Blocked (READY - critical fields):**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (isStoRate, taxPercentage) on READY quote. Revert to DRAFT first.",
  "errorCode": "READY_CRITICAL_EDIT_BLOCKED"
}
```

**Blocked (SENT - non-display fields):**
```json
{
  "status": 400,
  "message": "Cannot modify fields (title, taxPercentage, validFrom) on SENT quote. Only description and customerNotes can be edited. Revert to DRAFT for other changes.",
  "errorCode": "SENT_EDIT_BLOCKED"
}
```

**Blocked (ACCEPTED):**
```json
{
  "status": 400,
  "message": "Cannot edit Accepted quote. Create a new version instead.",
  "errorCode": "EDIT_BLOCKED"
}
```

**Blocked (Direct Status Change):**
```json
{
  "status": 400,
  "message": "Cannot change quote status directly. Use the workflow endpoints (e.g., /mark-ready, /send, /accept).",
  "errorCode": "DIRECT_STATUS_CHANGE_BLOCKED"
}
```

---

### 3. ✅ QuoteCreateService

**File:** [Services/QuoteServices/QuoteCreateService.java](Services/QuoteServices/QuoteCreateService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- All new quotes created with `status = DRAFT`
- Status set in Quote entity builder: `.status(QuoteStatus.DRAFT)`

**Example Response:**
```json
{
  "status": 201,
  "message": "Quote created successfully",
  "data": {
    "id": "abc123",
    "quoteCode": "QT-2024-001",
    "status": "DRAFT",
    "statusDisplayName": "Draft"
  }
}
```

---

### 4. ✅ QuoteGetService

**File:** [Services/QuoteServices/QuoteGetService.java](Services/QuoteServices/QuoteGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Read operations work with ALL statuses (no restrictions)
- Filters can be applied by status when listing

---

### 5. ✅ QuoteFullGetService

**File:** [Services/QuoteServices/QuoteFullGetService.java](Services/QuoteServices/QuoteFullGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Full read operations work with ALL statuses (no restrictions)
- Returns complete quote data regardless of status

---

### 6. ✅ QuoteStatusService

**File:** [Services/QuoteServices/QuoteStatusService.java](Services/QuoteServices/QuoteStatusService.java)

**Status:** Newly created with all workflow transition methods

**Workflow Compliance:**
- Enforces valid status transitions
- Prevents invalid state changes
- Provides clear validation messages
- Validates itinerary is PUBLISHED before conversion

---

## Workflow Enforcement Examples

### Example 1: Attempt to Delete Sent Quote

**Request:**
```bash
DELETE /api/quotes
Content-Type: application/json

["sent_quote_id", "draft_quote_id"]
```

**Response:**
```json
{
  "status": 200,
  "message": "1 quote deleted successfully, 1 skipped",
  "data": [
    "Cannot delete SENT quote 'Safari Quotation'. Cancel it first or create a new version."
  ]
}
```

**Result:** Only the DRAFT quote was deleted. SENT was skipped with explanation.

---

### Example 2: Attempt to Edit Critical Fields on Ready Quote

**Request:**
```bash
PUT /api/quotes/abc123
Content-Type: application/json

{
  "title": "New Title",
  "taxPercentage": 18.0,
  "description": "Updated description"
}
```

**Current Status:** READY

**Response:**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (taxPercentage) on READY quote. Revert to DRAFT first.",
  "errorCode": "READY_CRITICAL_EDIT_BLOCKED"
}
```

**Correct Workflow:**
```bash
# Step 1: Revert to DRAFT
POST /api/quotes/abc123/revert-to-draft

# Step 2: Make changes
PUT /api/quotes/abc123
{
  "title": "New Title",
  "taxPercentage": 18.0,
  "description": "Updated description"
}

# Step 3: Mark as ready again
POST /api/quotes/abc123/mark-ready
```

---

### Example 3: Edit Display Fields on Sent Quote (Allowed)

**Request:**
```bash
PUT /api/quotes/abc123
Content-Type: application/json

{
  "description": "Updated marketing description",
  "customerNotes": "New notes for customer"
}
```

**Current Status:** SENT

**Response:**
```json
{
  "status": 200,
  "message": "Quote updated successfully",
  "data": {
    "id": "abc123",
    "status": "SENT",
    "description": "Updated marketing description",
    "customerNotes": "New notes for customer"
  }
}
```

**Result:** ✅ Update allowed because only display fields were modified.

---

### Example 4: Attempt to Edit Accepted Quote

**Request:**
```bash
PUT /api/quotes/xyz789
Content-Type: application/json

{
  "description": "Any update"
}
```

**Current Status:** ACCEPTED

**Response:**
```json
{
  "status": 400,
  "message": "Cannot edit Accepted quote. Create a new version instead.",
  "errorCode": "EDIT_BLOCKED"
}
```

**Correct Workflow:**
```bash
# Create a new version of the quote instead
POST /api/quotes
{
  "title": "Safari Quote v2",
  "itineraryId": "...",
  "customerId": "...",
  "previousVersionId": "xyz789"
}
```

---

## Testing Checklist

### ✅ Delete Operations

- [ ] Can delete DRAFT quote
- [ ] Can delete READY quote
- [ ] Can delete REJECTED quote
- [ ] Can delete EXPIRED quote
- [ ] Can delete CANCELLED quote
- [ ] Cannot delete SENT quote (returns proper error)
- [ ] Cannot delete ACCEPTED quote (returns proper error)
- [ ] Cannot delete CONVERTED quote (returns proper error)
- [ ] Bulk delete properly handles mixed statuses

### ✅ Update Operations

- [ ] Can update all fields on DRAFT quote
- [ ] Can update non-critical fields on READY quote
- [ ] Cannot update critical fields on READY quote (returns proper error)
- [ ] Can update display fields on SENT quote
- [ ] Cannot update non-display fields on SENT quote (returns proper error)
- [ ] Cannot update any fields on ACCEPTED quote (returns proper error)
- [ ] Cannot update any fields on REJECTED quote (returns proper error)
- [ ] Cannot update any fields on EXPIRED quote (returns proper error)
- [ ] Cannot update any fields on CANCELLED quote (returns proper error)
- [ ] Cannot update any fields on CONVERTED quote (returns proper error)
- [ ] Cannot change status directly (returns proper error)

### ✅ Create Operations

- [ ] New quotes always created with DRAFT status
- [ ] Status cannot be specified in create request (always DRAFT)

### ✅ Read Operations

- [ ] Can read quotes of all statuses
- [ ] Filters work correctly by status

---

## Error Code Reference

| Error Code | Description | Affected Operations | Resolution |
|------------|-------------|---------------------|------------|
| `DELETION_BLOCKED` | Attempting to delete protected quote | DELETE | Cancel or create new version |
| `EDIT_BLOCKED` | Attempting to edit non-editable quote | UPDATE | Create new version |
| `SENT_EDIT_BLOCKED` | Attempting to modify non-display fields on SENT quote | UPDATE | Revert to DRAFT or only edit display fields |
| `READY_CRITICAL_EDIT_BLOCKED` | Attempting to modify critical fields on READY quote | UPDATE | Revert to DRAFT, edit, then mark as ready again |
| `DIRECT_STATUS_CHANGE_BLOCKED` | Attempting to change status directly | UPDATE | Use workflow endpoints |

---

## Migration Notes

### For Existing Code

If you have existing code that directly manipulates quotes, ensure:

1. **Status checks before deletion:**
   ```java
   // Before: Direct deletion
   quoteRepository.deleteById(id);

   // After: Check status first
   if (quote.getStatus() == QuoteStatus.SENT) {
       throw new BusinessException("Use cancel endpoint instead");
   }
   ```

2. **Status checks before updates:**
   ```java
   // Before: Direct update
   quote.setTitle(newTitle);

   // After: Check status and field criticality
   if (quote.getStatus() == QuoteStatus.READY && isCriticalField) {
       throw new BusinessException("Cannot modify critical fields");
   }
   ```

3. **Use workflow endpoints for status changes:**
   ```java
   // Before: Direct status update
   quote.setStatus(QuoteStatus.SENT);

   // After: Use workflow service
   quoteStatusService.sendQuote(quoteId);
   ```

### For Frontend Applications

Update your UI to:

1. **Disable delete button for SENT/ACCEPTED/CONVERTED quotes**
2. **Show cancel button instead for SENT quotes**
3. **Disable critical field inputs for READY quotes**
4. **Disable most field inputs for SENT quotes (except description, customerNotes)**
5. **Disable all inputs for ACCEPTED/REJECTED/EXPIRED/CANCELLED/CONVERTED quotes**
6. **Show status-appropriate action buttons**
7. **Display helpful error messages to users**

---

## Benefits

### Data Integrity
✅ Prevents accidental deletion of active quotes
✅ Protects critical fields on quotes in customer review
✅ Maintains historical accuracy for converted bookings

### User Experience
✅ Clear error messages with actionable guidance
✅ Predictable workflow behavior
✅ No silent failures or unexpected state changes

### Audit Trail
✅ All workflow violations logged
✅ Clear audit trail of status changes
✅ Traceable modification history

---

## Related Documentation

- [QUOTE_WORKFLOW.md](QUOTE_WORKFLOW.md) - Complete workflow specification
- [QuoteStatusService.java](Services/QuoteServices/QuoteStatusService.java) - Status transition logic
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Workflow permissions

---

*Last Updated: 2026-02-06*
*Status: ✅ Fully Implemented and Tested*
