# Invoice Status Workflow

## Overview

The Invoice module implements a simplified 8-state workflow system that manages the complete invoice lifecycle from draft preparation through payment tracking and exceptional scenarios like cancellations and refunds.

## Status Lifecycle

### Status States

```
DRAFT → SENT → VIEWED → [PARTIALLY_PAID/OVERDUE] → PAID
                ↓              ↓
        [CANCELLED]    [REFUNDED]
```

| Status | Description | Can Edit? | Can Delete? | Can Send? |
|--------|-------------|-----------|-------------|-----------|
| **DRAFT** | Invoice being prepared | ✅ Yes | ✅ Yes | ✅ Yes |
| **SENT** | Sent to customer | ⚠️ Limited | ❌ No | ✅ Resend |
| **VIEWED** | Customer viewed invoice | ⚠️ Limited | ❌ No | ✅ Resend |
| **PARTIALLY_PAID** | Partial payment received | ❌ No | ❌ No | ✅ Resend |
| **PAID** | Fully paid | ❌ No | ❌ No | ✅ Resend |
| **OVERDUE** | Payment past due date | ❌ No | ❌ No | ✅ Resend |
| **CANCELLED** | Invoice cancelled | ❌ No | ❌ No | ❌ No |
| **REFUNDED** | Payment refunded | ❌ No | ❌ No | ❌ No |

---

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    INVOICE LIFECYCLE                            │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐
│  DRAFT   │
└─────┬────┘
      │
      │ POST /api/invoices/{id}/state/send
      │ Permission: PERM_SEND_INVOICE
      │
      ▼
┌──────────┐
│   SENT   │
└─────┬────┘
      │
      │ POST /api/invoices/{id}/state/mark-viewed
      │ Permission: PERM_MARK_INVOICE_VIEWED
      │
      ▼
┌──────────┐
│  VIEWED  │
└─────┬────┘
      │
      ├──────────────────┬───────────────────┐
      │                  │                   │
      │ Record Payment   │ Mark Overdue      │
      │                  │                   │
      ▼                  ▼                   ▼
┌──────────┐      ┌──────────┐       ┌──────────┐
│PARTIALLY │      │   PAID   │       │ OVERDUE  │
│  PAID    │      │          │       │          │
└─────┬────┘      └─────┬────┘       └─────┬────┘
      │                  │                   │
      │                  │                   │
      │ Full Payment     │ Refund            │ Payment
      │                  │                   │
      ▼                  ▼                   ▼
┌──────────┐      ┌──────────┐       ┌──────────┐
│   PAID   │      │ REFUNDED │       │PARTIALLY │
│          │      │          │       │  PAID    │
└──────────┘      └──────────┘       └──────────┘


ADDITIONAL TRANSITIONS:

┌──────────┐    POST /api/invoices/{id}/state/cancel    ┌──────────┐
│ DRAFT/   │ ──────────────────────────────────────►   │CANCELLED │
│ SENT/    │    Permission: PERM_CANCEL_INVOICE        └──────────┘
│ VIEWED   │
└──────────┘

┌──────────┐    POST /api/invoices/{id}/state/initiate-refund    ┌──────────┐
│  PAID/   │ ──────────────────────────────────────────────────► │ REFUNDED │
│PARTIALLY │    Permission: PERM_INITIATE_INVOICE_REFUND         └──────────┘
│  PAID    │
└──────────┘

┌──────────┐    Automatic check or manual trigger          ┌──────────┐
│ SENT/    │ ──────────────────────────────────────────►   │ OVERDUE  │
│ VIEWED/  │    POST /api/invoices/{id}/state/mark-overdue └──────────┘
│PARTIALLY │    Permission: PERM_MARK_INVOICE_OVERDUE
│  PAID    │
└──────────┘
```

---

## API Endpoints

### 1. Send Invoice to Customer

**Endpoint:** `POST /api/invoices/{id}/state/send`
**Permission:** `PERM_SEND_INVOICE`
**Description:** Send invoice to customer (marks as SENT)

**Allowed Transitions:**
- DRAFT → SENT ✅

**Side Effects:**
- Sets `sentDate` to current date
- May trigger email notification to customer

**Response:**
```json
{
  "status": 200,
  "message": "Invoice sent successfully",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "SENT",
    "sentDate": "2026-02-06"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot send invoice in state Paid",
  "errorCode": "INVALID_STATE_TRANSITION"
}
```

---

### 2. Mark Invoice as Viewed

**Endpoint:** `POST /api/invoices/{id}/state/mark-viewed`
**Permission:** `PERM_MARK_INVOICE_VIEWED`
**Description:** Mark invoice as viewed by customer

**Allowed Transitions:**
- SENT → VIEWED ✅

**Response:**
```json
{
  "status": 200,
  "message": "Invoice marked as viewed",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "VIEWED"
  }
}
```

---

### 3. Record Invoice Payment

**Endpoint:** `POST /api/invoices/{id}/state/record-payment`
**Permission:** `PERM_RECORD_INVOICE_PAYMENT`
**Description:** Record invoice payment (partial or full)

**Allowed Transitions:**
- SENT → PARTIALLY_PAID or PAID ✅
- VIEWED → PARTIALLY_PAID or PAID ✅
- PARTIALLY_PAID → PAID ✅
- OVERDUE → PARTIALLY_PAID or PAID ✅

**Request Body:**
```json
{
  "isFullPayment": true,
  "paymentReference": "TXN-12345",
  "notes": "Bank transfer received"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Payment recorded successfully",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "PAID",
    "paidDate": "2026-02-06"
  }
}
```

---

### 4. Mark Invoice as Overdue

**Endpoint:** `POST /api/invoices/{id}/state/mark-overdue`
**Permission:** `PERM_MARK_INVOICE_OVERDUE`
**Description:** Mark invoice as overdue

**Allowed Transitions:**
- SENT → OVERDUE ✅
- VIEWED → OVERDUE ✅
- PARTIALLY_PAID → OVERDUE ✅

**Validation:**
- Due date must have passed

**Response:**
```json
{
  "status": 200,
  "message": "Invoice marked as overdue",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "OVERDUE"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot mark as overdue - due date has not passed yet",
  "errorCode": "NOT_YET_OVERDUE"
}
```

---

### 5. Cancel Invoice

**Endpoint:** `POST /api/invoices/{id}/state/cancel`
**Permission:** `PERM_CANCEL_INVOICE`
**Description:** Cancel an invoice

**Allowed Transitions:**
- DRAFT → CANCELLED ✅
- SENT → CANCELLED ✅
- VIEWED → CANCELLED ✅
- PARTIALLY_PAID → CANCELLED ⚠️ (Use refund instead)
- OVERDUE → CANCELLED ✅

**Request Body:**
```json
{
  "reason": "Customer cancelled order",
  "cancellationCategory": "Customer request",
  "notes": "Cancelled via phone call"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Invoice cancelled successfully",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "CANCELLED"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot cancel invoice in state Paid. Use refund workflow for paid invoices.",
  "errorCode": "INVALID_STATE_TRANSITION"
}
```

---

### 6. Initiate Refund

**Endpoint:** `POST /api/invoices/{id}/state/initiate-refund`
**Permission:** `PERM_INITIATE_INVOICE_REFUND`
**Description:** Initiate refund for paid invoice

**Allowed Transitions:**
- PAID → REFUNDED ✅
- PARTIALLY_PAID → REFUNDED ✅

**Request Body:**
```json
{
  "reason": "Product/service not delivered",
  "isFullRefund": true,
  "paymentReference": "REFUND-12345",
  "notes": "Refunded to original payment method"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Refund initiated successfully",
  "data": {
    "id": "abc123",
    "invoiceCode": "INV-000123",
    "status": "REFUNDED"
  }
}
```

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot initiate refund from state Draft. Only PAID or PARTIALLY_PAID invoices can be refunded.",
  "errorCode": "INVALID_STATE_TRANSITION"
}
```

---

## Status Requirements

### Requirements to SEND

An invoice can be sent if:

✅ Status is DRAFT
✅ Has customer linked
✅ Has at least one line item
✅ Has grand totals calculated
✅ Issue date and due date are valid

### Requirements to Mark as VIEWED

✅ Status is SENT
✅ Customer has accessed the invoice

### Requirements to Record Payment

✅ Status is SENT, VIEWED, PARTIALLY_PAID, or OVERDUE
✅ Payment details provided (isFullPayment flag)

### Requirements to Mark as OVERDUE

✅ Status is SENT, VIEWED, or PARTIALLY_PAID
✅ Due date has passed

### Requirements to CANCEL

✅ Status is NOT PAID or REFUNDED
✅ Cancellation reason provided

### Requirements to REFUND

✅ Status is PAID or PARTIALLY_PAID
✅ Refund reason provided

---

## Permissions

### Standard CRUD Permissions
- `PERM_CREATE_INVOICE` - Create new invoices
- `PERM_READ_INVOICE` - View invoice data
- `PERM_UPDATE_INVOICE` - Edit invoice fields
- `PERM_DELETE_INVOICE` - Delete invoices (DRAFT only)

### Workflow Permissions
- `PERM_SEND_INVOICE` - Send invoice to customer
- `PERM_MARK_INVOICE_VIEWED` - Mark invoice as viewed
- `PERM_RECORD_INVOICE_PAYMENT` - Record payments
- `PERM_MARK_INVOICE_OVERDUE` - Mark invoice as overdue
- `PERM_CANCEL_INVOICE` - Cancel invoice
- `PERM_INITIATE_INVOICE_REFUND` - Initiate refund

### Recommended Role Assignments

| Role | Permissions |
|------|-------------|
| **Invoice Creator** | CREATE, READ, UPDATE, SEND |
| **Finance Officer** | CREATE, READ, UPDATE, SEND, MARK_VIEWED, MARK_OVERDUE |
| **Finance Manager** | All except DELETE and REFUND |
| **Finance Director** | All permissions including RECORD_PAYMENT, CANCEL, REFUND |
| **Admin** | All permissions |

---

## Business Rules

### 1. Draft Invoices
- Can be freely edited
- Can be deleted
- Must be sent to customer before payment tracking
- Not visible to customers

### 2. Sent Invoices
- Limited editing (only non-critical fields)
- Cannot be deleted
- Can be marked as viewed by customer
- Automatically becomes overdue if due date passes

### 3. Viewed Invoices
- Limited editing (only non-critical fields)
- Cannot be deleted
- Ready for payment tracking
- Can transition to payment states

### 4. Payment States (PARTIALLY_PAID, PAID)
- Read-only (cannot edit)
- Cannot be deleted
- Can be refunded if needed
- PARTIALLY_PAID can transition to PAID with additional payment

### 5. Overdue Invoices
- Read-only (cannot edit)
- Cannot be deleted
- Can still accept payments
- Can be cancelled if debt is written off

### 6. Cancelled Invoices
- Read-only (cannot edit)
- Cannot be deleted
- Cannot be reactivated
- Maintains audit trail

### 7. Refunded Invoices
- Read-only (cannot edit)
- Cannot be deleted
- Terminal state (no further transitions)
- Maintains full payment and refund history

---

## Edit Restrictions

### Critical Fields (Cannot edit after DRAFT)
- `taxPercentage` - Tax rate
- `discountPercentage` - Discount rate
- `customer` - Linked customer (derived from safari)
- `safari` - Linked safari
- Line items (prices, quantities)

### Non-Critical Fields (Can edit in SENT/VIEWED)
- `description` - Invoice description
- `customerNotes` - Customer-facing notes
- `internalNotes` - Internal notes
- `paymentTerms` - Payment terms text

### Read-Only Fields (Never editable)
- `invoiceCode` - Generated code
- `status` - Use workflow endpoints
- `sentDate` - Set by workflow
- `paidDate` - Set by workflow
- `amountsPaid` - Set by payment recording
- `balances` - Calculated automatically

---

## Integration Points

### Safari Module
Invoices are created FROM safaris and must maintain the link:
```java
// Invoice must be linked to Safari
if (safari == null) {
    throw new BusinessException("Cannot create invoice without safari");
}

// Customer is derived from Safari
Customer customer = safari.getCustomer();
```

### Payment Module (Future)
When recording payments, the system will:
```java
// Update amounts paid
invoice.getAmountsPaid().add(paymentAmount);

// Update status based on total paid
if (totalPaid >= grandTotal) {
    invoice.setStatus(InvoiceStatus.PAID);
    invoice.setPaidDate(LocalDate.now());
} else {
    invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
}
```

### Email/Notification Module
When sending an invoice:
```java
// Generate PDF
InvoiceDocument pdf = pdfGenerator.generate(invoice);

// Send email to customer
emailService.sendInvoiceEmail(
    invoice.getCustomer().getEmail(),
    invoice.getTitle(),
    pdf
);

// Update status
invoice.setStatus(InvoiceStatus.SENT);
invoice.setSentDate(LocalDate.now());
```

---

## Audit Trail

All status transitions are logged via `@AuditLogAnnotation`:
- **Action:** The permission/action performed
- **Entity Type:** Invoice
- **Entity ID:** Obfuscated invoice ID
- **User:** Current authenticated user
- **Timestamp:** When the action occurred
- **Additional Data:** Reason, notes, or metadata

---

## Testing Scenarios

### Scenario 1: Happy Path - Draft to Paid

```bash
# 1. Create invoice from safari
POST /api/invoices/from-safari
{
  "safariId": "safari123",
  "issueDate": "2026-02-01",
  "dueDate": "2026-02-28"
}
# Response: status = DRAFT

# 2. Send to customer
POST /api/invoices/{id}/state/send
# Response: status = SENT, sentDate = 2026-02-06

# 3. Customer views invoice
POST /api/invoices/{id}/state/mark-viewed
# Response: status = VIEWED

# 4. Record full payment
POST /api/invoices/{id}/state/record-payment
{
  "isFullPayment": true,
  "paymentReference": "TXN-12345"
}
# Response: status = PAID, paidDate = 2026-02-06
```

### Scenario 2: Partial Payment

```bash
# Invoice in VIEWED state

# 1. Record partial payment
POST /api/invoices/{id}/state/record-payment
{
  "isFullPayment": false,
  "paymentReference": "TXN-11111"
}
# Response: status = PARTIALLY_PAID

# 2. Record remaining payment
POST /api/invoices/{id}/state/record-payment
{
  "isFullPayment": true,
  "paymentReference": "TXN-22222"
}
# Response: status = PAID
```

### Scenario 3: Overdue Invoice

```bash
# Invoice past due date

# 1. Mark as overdue
POST /api/invoices/{id}/state/mark-overdue
# Response: status = OVERDUE

# 2. Eventually receive payment
POST /api/invoices/{id}/state/record-payment
{
  "isFullPayment": true
}
# Response: status = PAID
```

### Scenario 4: Cancellation

```bash
# Customer cancels before payment

# 1. Cancel invoice
POST /api/invoices/{id}/state/cancel
{
  "reason": "Customer cancelled safari booking",
  "cancellationCategory": "Customer request"
}
# Response: status = CANCELLED
```

### Scenario 5: Refund

```bash
# Service not delivered, need to refund

# 1. Initiate refund
POST /api/invoices/{id}/state/initiate-refund
{
  "reason": "Safari cancelled due to weather",
  "isFullRefund": true,
  "paymentReference": "REFUND-12345"
}
# Response: status = REFUNDED
```

---

## Error Handling

### Common Error Codes
- `INVOICE_NOT_FOUND` (404) - Invoice ID not found
- `INVALID_STATE_TRANSITION` (400) - Cannot perform requested transition
- `PAYMENT_DETAILS_REQUIRED` (400) - Missing payment information
- `REASON_REQUIRED` (400) - Missing cancellation/refund reason
- `NOT_YET_OVERDUE` (400) - Due date has not passed
- `EDIT_BLOCKED` (400) - Cannot edit in current status
- `DELETE_BLOCKED` (400) - Cannot delete in current status
- `DIRECT_STATUS_CHANGE_BLOCKED` (400) - Must use workflow endpoints

---

## Files

### Entity
- [Invoice.java](Entity/Invoice.java) - Main entity with status enum and validation
- [InvoiceStatus.java](Enums/InvoiceStatus.java) - Status enum with transition logic

### Service
- [InvoiceStateTransitionService.java](Services/InvoiceStateTransitionService.java) - All workflow transitions
- [InvoiceCreateService.java](Services/InvoiceServices/InvoiceCreateService.java) - Invoice creation
- [InvoiceUpdateService.java](Services/InvoiceServices/InvoiceUpdateService.java) - Invoice updates
- [InvoiceDeleteService.java](Services/InvoiceServices/InvoiceDeleteService.java) - Invoice deletion

### Controller
- [InvoiceStateTransitionController.java](Controller/InvoiceStateTransitionController.java) - Workflow endpoints
- [InvoiceController.java](Controller/InvoiceController.java) - CRUD endpoints

### Configuration
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Workflow permissions

---

## Future Enhancements

### Planned Features
1. **Automatic Overdue Detection** - Scheduled job to auto-mark overdue invoices
2. **Payment Reminders** - Email reminders before and after due date
3. **Multiple Payment Methods** - Track payment method per transaction
4. **Recurring Invoices** - Support for subscription/recurring billing
5. **Credit Notes** - Formal credit note generation for refunds
6. **Multi-Currency Payment** - Accept payments in different currencies
7. **Payment Plans** - Support installment payment schedules
8. **Bulk Operations** - Send/cancel multiple invoices at once

---

*Last Updated: 2026-02-06*
