# Invoice Workflow Enforcement - Implementation Summary

## Overview

All Invoice services have been updated to enforce the status workflow rules as defined in [INVOICE_WORKFLOW.md](INVOICE_WORKFLOW.md). This document summarizes the changes made to ensure workflow compliance.

---

## Status-Based Access Control Matrix

| Operation | DRAFT | SENT | VIEWED | PARTIALLY_PAID | PAID | OVERDUE | CANCELLED | REFUNDED |
|-----------|-------|------|--------|----------------|------|---------|-----------|----------|
| **Create** | ✅ Default | N/A | N/A | N/A | N/A | N/A | N/A | N/A |
| **Read** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Update (All fields)** | ✅ | ⚠️ Non-critical | ⚠️ Non-critical | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Delete** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Send to Customer** | ✅ | ✅ Resend | ✅ Resend | ✅ Resend | ✅ Resend | ✅ Resend | ❌ | ❌ |

### Critical Fields (NOT Editable after DRAFT)
- `taxPercentage` - Tax rate
- `discountPercentage` - Discount rate
- `issueDate` - Invoice issue date
- `dueDate` - Payment due date
- Relationships (`customer`, `safari`)

### Non-Critical Fields (Editable in SENT/VIEWED)
- `description` - Invoice description
- `customerNotes` - Customer-facing notes
- `internalNotes` - Internal notes
- `paymentTerms` - Payment terms text
- `isActive` - Active status

### Read-Only Fields (Never Editable Directly)
- `status` - Use workflow endpoints
- `sentDate` - Set by workflow
- `paidDate` - Set by workflow
- `amountsPaid` - Set by payment recording
- `balances` - Calculated automatically

---

## Services Updated

### 1. ✅ InvoiceDeleteService

**File:** [Services/InvoiceServices/InvoiceDeleteService.java](Services/InvoiceServices/InvoiceDeleteService.java)

**Status:** Already compliant - no changes needed

**Workflow Enforcement:**
```java
// Only allow deletion of DRAFT invoices
if (invoice.getStatus() != InvoiceStatus.DRAFT) {
    log.warn("Cannot delete invoice {} - status is {} (only DRAFT invoices can be deleted)",
             invoice.getInvoiceCode(), invoice.getStatus().getDisplayName());
    skippedReasons.add(String.format("Invoice %s cannot be deleted - status is %s (only DRAFT invoices can be deleted)",
                                     invoice.getInvoiceCode(), invoice.getStatus().getDisplayName()));
    continue;
}
```

**Error Responses:**

**Success (DRAFT):**
```json
{
  "status": 200,
  "message": "1 invoice deleted successfully",
  "data": null
}
```

**Blocked (Non-DRAFT):**
```json
{
  "status": 400,
  "message": "0 invoices deleted, 1 invoice skipped: Invoice INV-000123 cannot be deleted - status is Sent (only DRAFT invoices can be deleted)",
  "errorCode": "NO_INVOICES_DELETED"
}
```

---

### 2. ✅ InvoiceUpdateService

**File:** [Services/InvoiceServices/InvoiceUpdateService.java](Services/InvoiceServices/InvoiceUpdateService.java)

**Changes Made:**
- Added comprehensive status-based edit restrictions
- Blocks ALL edits to PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, REFUNDED invoices
- Blocks CRITICAL field edits to SENT/VIEWED invoices
- Blocks direct status changes (must use workflow endpoints)
- Provides detailed error messages listing blocked fields

**Workflow Enforcement:**
```java
// PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, REFUNDED invoices cannot be edited
InvoiceStatus status = invoice.getStatus();
if (status == InvoiceStatus.PARTIALLY_PAID || status == InvoiceStatus.PAID ||
    status == InvoiceStatus.OVERDUE || status == InvoiceStatus.CANCELLED ||
    status == InvoiceStatus.REFUNDED) {
    return error("Cannot edit " + status.getDisplayName() + " invoice. Payment and final state invoices are read-only.");
}

// SENT/VIEWED invoices can only edit non-critical fields
if (status == InvoiceStatus.SENT || status == InvoiceStatus.VIEWED) {
    List<String> blockedFields = new ArrayList<>();

    if (title changed) blockedFields.add("title");
    if (taxPercentage changed) blockedFields.add("taxPercentage");
    if (discountPercentage changed) blockedFields.add("discountPercentage");
    if (issueDate changed) blockedFields.add("issueDate");
    if (dueDate changed) blockedFields.add("dueDate");

    if (!blockedFields.isEmpty()) {
        return error("Cannot modify critical fields (...) on " + status.getDisplayName() +
                    " invoice. Only description, customerNotes, internalNotes, paymentTerms, and isActive can be edited.");
    }
}

// Block direct status updates
if (status being changed) {
    return error("Cannot change invoice status directly. Use the workflow endpoints at /api/invoices/{id}/state/*");
}
```

**Error Codes:**
- `EDIT_BLOCKED` - Attempting to edit non-editable invoice
- `SENT_EDIT_BLOCKED` - Attempting to modify critical fields on SENT/VIEWED invoice
- `DIRECT_STATUS_CHANGE_BLOCKED` - Attempting to change status directly

**Example Responses:**

**Success (DRAFT - all fields):**
```json
{
  "status": 200,
  "message": "Invoice updated successfully",
  "data": {
    "id": "abc123",
    "title": "Updated Title",
    "taxPercentage": 18.0
  }
}
```

**Success (SENT - non-critical fields only):**
```json
{
  "status": 200,
  "message": "Invoice updated successfully",
  "data": {
    "id": "abc123",
    "description": "Updated description",
    "customerNotes": "Updated notes"
  }
}
```

**Blocked (SENT - critical fields):**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (taxPercentage, dueDate) on Sent invoice. Only description, customerNotes, internalNotes, paymentTerms, and isActive can be edited.",
  "errorCode": "SENT_EDIT_BLOCKED"
}
```

**Blocked (PAID):**
```json
{
  "status": 400,
  "message": "Cannot edit Paid invoice. Payment and final state invoices are read-only.",
  "errorCode": "EDIT_BLOCKED"
}
```

**Blocked (Direct Status Change):**
```json
{
  "status": 400,
  "message": "Cannot change invoice status directly. Use the workflow endpoints at /api/invoices/{id}/state/* (e.g., /send, /record-payment, /cancel).",
  "errorCode": "DIRECT_STATUS_CHANGE_BLOCKED"
}
```

---

### 3. ✅ InvoiceCreateService

**File:** [Services/InvoiceServices/InvoiceCreateService.java](Services/InvoiceServices/InvoiceCreateService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- All new invoices created with `status = DRAFT`
- Status set in Invoice entity builder (line 118): `.status(InvoiceStatus.DRAFT)`

**Example Response:**
```json
{
  "status": 201,
  "message": "Invoice created successfully",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "DRAFT",
    "statusDisplayName": "Draft"
  }
}
```

---

### 4. ✅ InvoiceGetService

**File:** [Services/InvoiceServices/InvoiceGetService.java](Services/InvoiceServices/InvoiceGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Read operations work with ALL statuses (no restrictions)
- Filters can be applied by status when listing

---

### 5. ✅ InvoiceFullGetService

**File:** [Services/InvoiceServices/InvoiceFullGetService.java](Services/InvoiceServices/InvoiceFullGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Full read operations work with ALL statuses (no restrictions)
- Returns complete invoice data regardless of status

---

### 6. ✅ InvoiceTotalsCalculationService

**File:** [Services/InvoiceServices/InvoiceTotalsCalculationService.java](Services/InvoiceServices/InvoiceTotalsCalculationService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Totals recalculation works with ALL statuses
- Triggered automatically on line item changes

---

### 7. ✅ InvoiceFromSafariGenerationService

**File:** [Services/InvoiceServices/InvoiceFromSafariGenerationService.java](Services/InvoiceServices/InvoiceFromSafariGenerationService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Delegates to InvoiceCreateService which creates invoices in DRAFT status
- No direct status manipulation

---

### 8. ✅ InvoiceStateTransitionService

**File:** [Services/InvoiceStateTransitionService.java](Services/InvoiceStateTransitionService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Enforces valid status transitions
- Prevents invalid state changes
- Provides clear validation messages
- Uses InvoiceStatus.canTransitionTo() for validation

---

## Workflow Enforcement Examples

### Example 1: Attempt to Delete Sent Invoice

**Request:**
```bash
DELETE /api/invoices
Content-Type: application/json

["sent_invoice_id", "draft_invoice_id"]
```

**Response:**
```json
{
  "status": 200,
  "message": "1 invoice deleted successfully, 1 invoice skipped",
  "data": [
    "Invoice INV-000123 cannot be deleted - status is Sent (only DRAFT invoices can be deleted)"
  ]
}
```

**Result:** Only the DRAFT invoice was deleted. SENT was skipped with explanation.

---

### Example 2: Attempt to Edit Critical Fields on Sent Invoice

**Request:**
```bash
PUT /api/invoices/abc123
Content-Type: application/json

{
  "title": "New Title",
  "taxPercentage": 18.0,
  "description": "Updated description"
}
```

**Current Status:** SENT

**Response:**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (title, taxPercentage) on Sent invoice. Only description, customerNotes, internalNotes, paymentTerms, and isActive can be edited.",
  "errorCode": "SENT_EDIT_BLOCKED"
}
```

---

### Example 3: Edit Non-Critical Fields on Sent Invoice (Allowed)

**Request:**
```bash
PUT /api/invoices/abc123
Content-Type: application/json

{
  "description": "Updated marketing description",
  "customerNotes": "New payment instructions"
}
```

**Current Status:** SENT

**Response:**
```json
{
  "status": 200,
  "message": "Invoice updated successfully",
  "data": {
    "id": "abc123",
    "status": "SENT",
    "description": "Updated marketing description",
    "customerNotes": "New payment instructions"
  }
}
```

**Result:** ✅ Update allowed because only non-critical fields were modified.

---

### Example 4: Attempt to Edit Paid Invoice

**Request:**
```bash
PUT /api/invoices/xyz789
Content-Type: application/json

{
  "description": "Any update"
}
```

**Current Status:** PAID

**Response:**
```json
{
  "status": 400,
  "message": "Cannot edit Paid invoice. Payment and final state invoices are read-only.",
  "errorCode": "EDIT_BLOCKED"
}
```

---

### Example 5: Attempt to Change Status Directly

**Request:**
```bash
PUT /api/invoices/abc123
Content-Type: application/json

{
  "status": "SENT"
}
```

**Current Status:** DRAFT

**Response:**
```json
{
  "status": 400,
  "message": "Cannot change invoice status directly. Use the workflow endpoints at /api/invoices/{id}/state/* (e.g., /send, /record-payment, /cancel).",
  "errorCode": "DIRECT_STATUS_CHANGE_BLOCKED"
}
```

**Correct Workflow:**
```bash
# Use the proper workflow endpoint
POST /api/invoices/abc123/state/send
```

---

## Testing Checklist

### ✅ Delete Operations

- [ ] Can delete DRAFT invoice
- [ ] Cannot delete SENT invoice (returns proper error)
- [ ] Cannot delete VIEWED invoice (returns proper error)
- [ ] Cannot delete PARTIALLY_PAID invoice (returns proper error)
- [ ] Cannot delete PAID invoice (returns proper error)
- [ ] Cannot delete OVERDUE invoice (returns proper error)
- [ ] Cannot delete CANCELLED invoice (returns proper error)
- [ ] Cannot delete REFUNDED invoice (returns proper error)
- [ ] Bulk delete properly handles mixed statuses

### ✅ Update Operations

- [ ] Can update all fields on DRAFT invoice
- [ ] Can update non-critical fields on SENT invoice
- [ ] Cannot update critical fields on SENT invoice (returns proper error)
- [ ] Can update non-critical fields on VIEWED invoice
- [ ] Cannot update critical fields on VIEWED invoice (returns proper error)
- [ ] Cannot update any fields on PARTIALLY_PAID invoice (returns proper error)
- [ ] Cannot update any fields on PAID invoice (returns proper error)
- [ ] Cannot update any fields on OVERDUE invoice (returns proper error)
- [ ] Cannot update any fields on CANCELLED invoice (returns proper error)
- [ ] Cannot update any fields on REFUNDED invoice (returns proper error)
- [ ] Cannot change status directly (returns proper error)

### ✅ Create Operations

- [ ] New invoices always created with DRAFT status
- [ ] Status cannot be specified in create request (always DRAFT)

### ✅ Read Operations

- [ ] Can read invoices of all statuses
- [ ] Filters work correctly by status

---

## Error Code Reference

| Error Code | Description | Affected Operations | Resolution |
|------------|-------------|---------------------|------------|
| `DELETION_BLOCKED` | Attempting to delete non-DRAFT invoice | DELETE | Only DRAFT invoices can be deleted |
| `EDIT_BLOCKED` | Attempting to edit non-editable invoice | UPDATE | Payment and final state invoices are read-only |
| `SENT_EDIT_BLOCKED` | Attempting to modify critical fields on SENT/VIEWED invoice | UPDATE | Only edit non-critical fields or use workflow to change status |
| `DIRECT_STATUS_CHANGE_BLOCKED` | Attempting to change status directly | UPDATE | Use workflow endpoints |

---

## Migration Notes

### For Existing Code

If you have existing code that directly manipulates invoices, ensure:

1. **Status checks before deletion:**
   ```java
   // Before: Direct deletion
   invoiceRepository.deleteById(id);

   // After: Check status first
   if (invoice.getStatus() != InvoiceStatus.DRAFT) {
       throw new BusinessException("Only DRAFT invoices can be deleted");
   }
   ```

2. **Status checks before updates:**
   ```java
   // Before: Direct update
   invoice.setTitle(newTitle);

   // After: Check status and field criticality
   if (invoice.getStatus() == InvoiceStatus.SENT && isCriticalField) {
       throw new BusinessException("Cannot modify critical fields on SENT invoice");
   }
   ```

3. **Use workflow endpoints for status changes:**
   ```java
   // Before: Direct status update
   invoice.setStatus(InvoiceStatus.SENT);

   // After: Use workflow service
   invoiceStateTransitionService.sendInvoice(invoiceId, dto);
   ```

### For Frontend Applications

Update your UI to:

1. **Disable delete button for non-DRAFT invoices**
2. **Disable critical field inputs for SENT/VIEWED invoices**
3. **Disable all inputs for payment and final state invoices**
4. **Show status-appropriate action buttons**
5. **Display helpful error messages to users**
6. **Use workflow endpoints for status transitions**

---

## Benefits

### Data Integrity
✅ Prevents accidental deletion of active invoices
✅ Protects critical fields on invoices sent to customers
✅ Maintains historical accuracy for paid invoices

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

- [INVOICE_WORKFLOW.md](INVOICE_WORKFLOW.md) - Complete workflow specification
- [InvoiceStateTransitionService.java](Services/InvoiceStateTransitionService.java) - Status transition logic
- [InvoiceStatus.java](Enums/InvoiceStatus.java) - Status enum with transition validation
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Workflow permissions

---

*Last Updated: 2026-02-06*
*Status: ✅ Fully Implemented and Ready for Testing*
