# Invoice API Documentation

## Overview
The Invoice API provides endpoints for managing safari tour invoices. Invoices are generated for confirmed safaris and include detailed line items, payment tracking, tax calculations, and status management. The system supports multi-currency pricing, automatic totals calculation, and comprehensive payment status tracking.

**Base URL**: `/api/invoices`

**Required Permissions**:
- `PERM_CREATE_INVOICE` - Create new invoices
- `PERM_READ_INVOICE` - View invoices
- `PERM_UPDATE_INVOICE` - Update existing invoices
- `PERM_DELETE_INVOICE` - Delete invoices

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Create Invoice](#1-create-invoice)
   - [Generate Invoice from Safari](#2-generate-invoice-from-safari)
   - [Update Invoice](#3-update-invoice)
   - [Delete Invoices](#4-delete-invoices)
   - [Get Invoice by ID](#5-get-invoice-by-id)
   - [Get Invoice by Code](#6-get-invoice-by-code)
   - [Get Full Invoice](#7-get-full-invoice-with-all-nested-data)
   - [Get All Invoices](#8-get-all-invoices-with-filters)
   - [Recalculate Totals](#9-recalculate-invoice-totals)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)
5. [Authentication & Authorization](#authentication--authorization)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

---

## Data Models

### InvoiceStatus Enum
```json
{
  "allowedValues": [
    "DRAFT",
    "PENDING_APPROVAL",
    "APPROVED",
    "SENT",
    "OVERDUE",
    "PARTIALLY_PAID",
    "PAID",
    "CANCELLED",
    "VOID"
  ]
}
```

**Status Descriptions**:
- `DRAFT` - Invoice is being prepared and not yet finalized
- `PENDING_APPROVAL` - Invoice awaiting approval from designated approver
- `APPROVED` - Invoice has been approved and ready to be sent
- `SENT` - Invoice has been sent to customer
- `OVERDUE` - Invoice payment is past the due date
- `PARTIALLY_PAID` - Invoice has received partial payment
- `PAID` - Invoice has been fully paid
- `CANCELLED` - Invoice was cancelled before completion
- `VOID` - Invoice was voided (kept for record but invalidated)

### PaymentStatus Enum
```json
{
  "allowedValues": [
    "UNPAID",
    "PARTIALLY_PAID",
    "PAID",
    "OVERPAID",
    "REFUNDED",
    "CANCELLED"
  ]
}
```

**Payment Status Descriptions**:
- `UNPAID` - No payment received yet
- `PARTIALLY_PAID` - Some payment received, balance remaining
- `PAID` - Invoice fully paid
- `OVERPAID` - Payment received exceeds invoice total
- `REFUNDED` - Payment was refunded to customer
- `CANCELLED` - Payment cancelled

### Status Groups
Invoices can be filtered by status groups for easier management:

| Group | Included Statuses | Description |
|-------|-------------------|-------------|
| `draft` | DRAFT | Invoices being prepared |
| `pending` | PENDING_APPROVAL | Awaiting approval |
| `active` | APPROVED, SENT, OVERDUE | Active invoices in circulation |
| `unpaid` | SENT, OVERDUE (where paymentStatus is UNPAID or PARTIALLY_PAID) | Outstanding invoices |
| `completed` | PAID, CANCELLED, VOID | Closed invoices |

### Price Object (Multi-Currency)
```json
{
  "USD": 5000.00,
  "TZS": 11750000.00,
  "EUR": 4500.00
}
```

### Invoice Object (InvoiceDTO)
```json
{
  "id": "string (obfuscated ID for security)",
  "invoiceCode": "string (system-generated, e.g., INV-2024-0001)",
  "title": "string",
  "description": "string",
  "customerId": "string (obfuscated)",
  "customerName": "string",
  "customerEmail": "string",
  "safariId": "string (obfuscated, nullable)",
  "safariCode": "string",
  "safariName": "string",
  "subtotals": "Price object (multi-currency)",
  "taxes": "Price object (multi-currency)",
  "discounts": "Price object (multi-currency)",
  "grandTotals": "Price object (multi-currency)",
  "amountPaid": "Price object (multi-currency)",
  "amountDue": "Price object (multi-currency)",
  "taxPercentage": "decimal",
  "discountPercentage": "decimal",
  "discountReason": "string",
  "status": "InvoiceStatus enum",
  "paymentStatus": "PaymentStatus enum",
  "issueDate": "date",
  "dueDate": "date",
  "sentDate": "date (system-set when status changes to SENT)",
  "paidDate": "date (system-set when fully paid)",
  "isOverdue": "boolean (computed field)",
  "daysPastDue": "integer (computed field, negative if not due yet)",
  "paymentTerms": "string (e.g., 'Net 30', 'Due on receipt')",
  "invoiceNotes": "string (visible to customer)",
  "internalNotes": "string (staff-only)",
  "customerNotes": "string (from customer about this invoice)",
  "isActive": "boolean",
  "itemCount": "long (number of invoice line items)",
  "documentCount": "long (number of attached documents)",
  "createdById": "string (obfuscated)",
  "createdByName": "string",
  "createdAt": "datetime (ISO 8601)",
  "updatedById": "string (obfuscated)",
  "updatedByName": "string",
  "updatedAt": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Invoice

**Endpoint**: `POST /api/invoices`

**Permission**: `PERM_CREATE_INVOICE`

**Description**: Creates a new invoice for a customer. The invoice code is automatically generated. The invoice starts in DRAFT status by default.

#### Request Body (CreateInvoiceDTO)
```json
{
  "title": "Safari Tour Invoice - Northern Circuit Package",
  "description": "Invoice for 7-day Northern Circuit safari tour",
  "customerId": "aB3Cd4Ef",
  "safariId": "xY9Kp2Lm",
  "taxPercentage": 18.00,
  "discountPercentage": 5.00,
  "discountReason": "Returning customer discount",
  "issueDate": "2024-06-01",
  "dueDate": "2024-06-15",
  "paymentTerms": "Net 30",
  "invoiceNotes": "Payment can be made via bank transfer or credit card",
  "internalNotes": "VIP client - priority processing",
  "isActive": true
}
```

**Required Fields**:
- `title` (string, not blank)
- `customerId` (string, obfuscated ID)
- `issueDate` (date)
- `dueDate` (date)

**Optional Fields**: All other fields are optional.

**System-Managed Fields** (auto-generated, not in request):
- `invoiceCode` - Auto-generated (e.g., INV-2024-0001)
- `status` - Starts at DRAFT
- `paymentStatus` - Starts at UNPAID
- `sentDate` - Set when status changes to SENT
- `paidDate` - Set when paymentStatus changes to PAID
- `subtotals`, `taxes`, `discounts`, `grandTotals`, `amountPaid`, `amountDue` - Calculated from invoice items
- `isOverdue`, `daysPastDue` - Computed based on dates

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice created successfully",
  "data": {
    "id": "mN4Op5Qr",
    "invoiceCode": "INV-2024-0001",
    "title": "Safari Tour Invoice - Northern Circuit Package",
    "description": "Invoice for 7-day Northern Circuit safari tour",
    "customerId": "aB3Cd4Ef",
    "customerName": "John Smith",
    "customerEmail": "john.smith@example.com",
    "safariId": "xY9Kp2Lm",
    "safariCode": "SAF-7D6N-01001",
    "safariName": "7-Day Serengeti Adventure",
    "subtotals": {
      "USD": 5000.00
    },
    "taxes": {
      "USD": 900.00
    },
    "discounts": {
      "USD": 250.00
    },
    "grandTotals": {
      "USD": 5650.00
    },
    "amountPaid": {
      "USD": 0.00
    },
    "amountDue": {
      "USD": 5650.00
    },
    "taxPercentage": 18.00,
    "discountPercentage": 5.00,
    "discountReason": "Returning customer discount",
    "status": "DRAFT",
    "paymentStatus": "UNPAID",
    "issueDate": "2024-06-01",
    "dueDate": "2024-06-15",
    "sentDate": null,
    "paidDate": null,
    "isOverdue": false,
    "daysPastDue": 0,
    "paymentTerms": "Net 30",
    "invoiceNotes": "Payment can be made via bank transfer or credit card",
    "internalNotes": "VIP client - priority processing",
    "customerNotes": null,
    "isActive": true,
    "itemCount": 0,
    "documentCount": 0,
    "createdById": "tU9Vw0Xy",
    "createdByName": "Admin User",
    "createdAt": "2024-06-01T10:30:00",
    "updatedById": null,
    "updatedByName": null,
    "updatedAt": "2024-06-01T10:30:00"
  },
  "timestamp": "2024-06-01T10:30:00"
}
```

#### Error Responses

**400 Bad Request** - Validation errors
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid customer ID",
  "errorCode": "INVALID_CUSTOMER_ID",
  "timestamp": "2024-06-01T10:30:00"
}
```

**400 Bad Request** - Date validation error
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Due date must be after issue date",
  "errorCode": "INVALID_DUE_DATE",
  "timestamp": "2024-06-01T10:30:00"
}
```

**404 Not Found** - Customer not found
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Customer not found",
  "errorCode": "CUSTOMER_NOT_FOUND",
  "timestamp": "2024-06-01T10:30:00"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to create invoice",
  "errorCode": "INVOICE_CREATE_FAILED",
  "timestamp": "2024-06-01T10:30:00"
}
```

---

### 2. Generate Invoice from Safari

**Endpoint**: `POST /api/invoices/from-safari`

**Permission**: `PERM_CREATE_INVOICE`

**Description**: Automatically generates an invoice from a confirmed safari. This endpoint analyzes the safari data and creates invoice line items for accommodations, activities, park fees, and other services.

#### Request Body (CreateInvoiceFromSafariDTO)
```json
{
  "safariId": "xY9Kp2Lm",
  "title": "Safari Tour Invoice - Northern Circuit",
  "description": "Invoice for confirmed safari tour",
  "taxPercentage": 18.00,
  "discountPercentage": 0.00,
  "issueDate": "2024-06-01",
  "dueDate": "2024-06-15",
  "paymentTerms": "Net 30",
  "invoiceNotes": "Thank you for your booking",
  "internalNotes": "Generated from safari SAF-7D6N-01001",
  "includeAccommodations": true,
  "includeActivities": true,
  "includeParkFees": true,
  "includeTransport": true
}
```

**Required Fields**:
- `safariId` (string, obfuscated ID)
- `issueDate` (date)
- `dueDate` (date)

**Optional Fields**: All other fields are optional. If `title` is not provided, it will be auto-generated from the safari name.

**Include Flags** (all default to true if not specified):
- `includeAccommodations` - Include accommodation costs
- `includeActivities` - Include activity costs
- `includeParkFees` - Include park entry fees
- `includeTransport` - Include transportation costs

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice generated from safari successfully",
  "data": {
    "id": "pQ7Rs8Tv",
    "invoiceCode": "INV-2024-0002",
    "title": "Safari Tour Invoice - Northern Circuit",
    "customerId": "aB3Cd4Ef",
    "customerName": "John Smith",
    "safariId": "xY9Kp2Lm",
    "safariCode": "SAF-7D6N-01001",
    "safariName": "7-Day Serengeti Adventure",
    "grandTotals": {
      "USD": 5900.00
    },
    "itemCount": 12,
    "status": "DRAFT",
    "paymentStatus": "UNPAID",
    ...
  },
  "timestamp": "2024-06-01T11:00:00"
}
```

#### Error Responses

**404 Not Found** - Safari not found
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Safari not found",
  "errorCode": "SAFARI_NOT_FOUND",
  "timestamp": "2024-06-01T11:00:00"
}
```

**400 Bad Request** - Safari not confirmed
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Safari must be confirmed before generating invoice",
  "errorCode": "SAFARI_NOT_CONFIRMED",
  "timestamp": "2024-06-01T11:00:00"
}
```

---

### 3. Update Invoice

**Endpoint**: `PUT /api/invoices/{idObfuscated}`

**Permission**: `PERM_UPDATE_INVOICE`

**Description**: Updates an existing invoice's metadata. Only provided fields will be updated (partial update). Line items are updated through separate endpoints.

#### Path Parameters
- `idObfuscated` (required): The obfuscated invoice ID

#### Request Body (UpdateInvoiceDTO)
```json
{
  "title": "Updated Invoice Title",
  "description": "Updated description",
  "status": "APPROVED",
  "paymentStatus": "PARTIALLY_PAID",
  "taxPercentage": 18.00,
  "discountPercentage": 10.00,
  "discountReason": "Updated discount reason",
  "issueDate": "2024-06-01",
  "dueDate": "2024-06-30",
  "paymentTerms": "Net 30",
  "invoiceNotes": "Updated payment instructions",
  "internalNotes": "Updated internal notes",
  "customerNotes": "Customer requested extended payment terms",
  "isActive": true
}
```

**Updatable Fields**: All fields in the request body are optional. Only include fields you want to update.

**Non-Updatable Fields** (system-managed):
- `invoiceCode` - System-generated, never changes
- `sentDate` - System-set when status changes to SENT
- `paidDate` - System-set when paymentStatus changes to PAID
- `customerId` - Cannot change invoice's customer
- `safariId` - Cannot change invoice's safari
- `subtotals`, `taxes`, `discounts`, `grandTotals`, `amountPaid`, `amountDue` - Recalculated from items
- `isOverdue`, `daysPastDue` - Computed fields

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice updated successfully",
  "data": {
    "id": "mN4Op5Qr",
    "invoiceCode": "INV-2024-0001",
    "title": "Updated Invoice Title",
    "status": "APPROVED",
    "discountPercentage": 10.00,
    ...
    "updatedById": "tU9Vw0Xy",
    "updatedByName": "Admin User",
    "updatedAt": "2024-06-01T15:45:00"
  },
  "timestamp": "2024-06-01T15:45:00"
}
```

#### Error Responses

**400 Bad Request** - Invalid ID
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid invoice ID",
  "errorCode": "INVALID_INVOICE_ID",
  "timestamp": "2024-06-01T15:45:00"
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Invoice not found",
  "errorCode": "INVOICE_NOT_FOUND",
  "timestamp": "2024-06-01T15:45:00"
}
```

---

### 4. Delete Invoices

**Endpoint**: `DELETE /api/invoices`

**Permission**: `PERM_DELETE_INVOICE`

**Description**: Deletes one or more invoices by their obfuscated IDs. This also deletes associated invoice items and documents.

#### Request Body
```json
["mN4Op5Qr", "xY9Kp2Lm", "aB3Cd4Ef"]
```

**Note**: Send an array of obfuscated invoice IDs to delete.

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 invoice(s) deleted successfully",
  "data": {
    "deletedIds": ["mN4Op5Qr", "xY9Kp2Lm", "aB3Cd4Ef"],
    "failedIds": []
  },
  "timestamp": "2024-06-01T16:00:00"
}
```

#### Partial Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "2 invoice(s) deleted successfully, 1 failed",
  "data": {
    "deletedIds": ["mN4Op5Qr", "xY9Kp2Lm"],
    "failedIds": ["aB3Cd4Ef"]
  },
  "timestamp": "2024-06-01T16:00:00"
}
```

---

### 5. Get Invoice by ID

**Endpoint**: `GET /api/invoices/{idObfuscated}`

**Permission**: `PERM_READ_INVOICE`

**Description**: Retrieves a single invoice by its obfuscated ID. Returns the invoice object with summary data (not including line items).

#### Path Parameters
- `idObfuscated` (required): The obfuscated invoice ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice retrieved successfully",
  "data": {
    "id": "mN4Op5Qr",
    "invoiceCode": "INV-2024-0001",
    "title": "Safari Tour Invoice - Northern Circuit Package",
    "customerId": "aB3Cd4Ef",
    "customerName": "John Smith",
    "safariId": "xY9Kp2Lm",
    "safariCode": "SAF-7D6N-01001",
    "grandTotals": {
      "USD": 5650.00
    },
    "amountDue": {
      "USD": 5650.00
    },
    "status": "SENT",
    "paymentStatus": "UNPAID",
    "issueDate": "2024-06-01",
    "dueDate": "2024-06-15",
    "isOverdue": false,
    "daysPastDue": -5,
    "itemCount": 8,
    ...
  },
  "timestamp": "2024-06-01T16:30:00"
}
```

#### Error Responses

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Invoice not found",
  "errorCode": "INVOICE_NOT_FOUND",
  "timestamp": "2024-06-01T16:30:00"
}
```

---

### 6. Get Invoice by Code

**Endpoint**: `GET /api/invoices/code/{invoiceCode}`

**Permission**: `PERM_READ_INVOICE`

**Description**: Retrieves a single invoice by its system-generated invoice code. Useful for customer-facing references.

#### Path Parameters
- `invoiceCode` (required): The invoice code (e.g., "INV-2024-0001")

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice retrieved successfully",
  "data": {
    "id": "mN4Op5Qr",
    "invoiceCode": "INV-2024-0001",
    ...
  },
  "timestamp": "2024-06-01T17:00:00"
}
```

---

### 7. Get Full Invoice (with all nested data)

**Endpoint**: `GET /api/invoices/{idObfuscated}/full`

**Permission**: `PERM_READ_INVOICE`

**Description**: Retrieves complete invoice data including all line items, payments, and documents. Use this endpoint when you need the complete invoice structure for PDF generation or detailed display.

#### Path Parameters
- `idObfuscated` (required): The obfuscated invoice ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Full invoice retrieved successfully",
  "data": {
    "id": "mN4Op5Qr",
    "invoiceCode": "INV-2024-0001",
    "title": "Safari Tour Invoice",
    "customer": {
      "id": "aB3Cd4Ef",
      "code": "CUS-000101",
      "displayName": "John Smith",
      "primaryEmail": "john.smith@example.com",
      "primaryPhone": "+1-555-0100",
      "fullAddress": "123 Main St, New York, NY 10001, USA"
    },
    "safari": {
      "id": "xY9Kp2Lm",
      "code": "SAF-7D6N-01001",
      "name": "7-Day Serengeti Adventure",
      "startDate": "2024-07-01",
      "endDate": "2024-07-07"
    },
    "items": [
      {
        "id": "zA1Bc2De",
        "itemType": "ACCOMMODATION",
        "description": "Serengeti Safari Lodge - Luxury Tent",
        "quantity": 3,
        "unitPrice": {
          "USD": 250.00
        },
        "totalPrice": {
          "USD": 750.00
        },
        "currency": "USD",
        "taxable": true,
        "notes": "Full board included"
      },
      {
        "id": "dE3Fg4Hi",
        "itemType": "PARK_FEE",
        "description": "Serengeti National Park Entry Fee",
        "quantity": 2,
        "unitPrice": {
          "USD": 60.00
        },
        "totalPrice": {
          "USD": 120.00
        },
        "currency": "USD",
        "taxable": false
      }
    ],
    "subtotals": {
      "USD": 5000.00
    },
    "taxes": {
      "USD": 900.00
    },
    "grandTotals": {
      "USD": 5900.00
    },
    "amountPaid": {
      "USD": 0.00
    },
    "amountDue": {
      "USD": 5900.00
    },
    "status": "SENT",
    "paymentStatus": "UNPAID",
    "issueDate": "2024-06-01",
    "dueDate": "2024-06-15",
    "isOverdue": false,
    "itemCount": 8,
    ...
  },
  "timestamp": "2024-06-01T17:30:00"
}
```

---

### 8. Get All Invoices (with Filters)

**Endpoint**: `GET /api/invoices`

**Permission**: `PERM_READ_INVOICE`

**Description**: Retrieves a paginated list of invoices with optional filtering and sorting. Results are always sorted by `createdAt` field.

#### Query Parameters

**Filtering**:
- `invoiceCode` (string, optional): Filter by invoice code (partial match, case-insensitive)
- `title` (string, optional): Filter by title (partial match, case-insensitive)
- `status` (InvoiceStatus, optional): Filter by exact status
- `paymentStatus` (PaymentStatus, optional): Filter by payment status
- `customerId` (string, optional): Filter by customer ID (obfuscated, exact match)
- `safariId` (string, optional): Filter by safari ID (obfuscated, exact match)
- `createdById` (string, optional): Filter by created by user ID (obfuscated, exact match)
- `updatedById` (string, optional): Filter by updated by user ID (obfuscated, exact match)
- `isActive` (boolean, optional): Filter by active status (true/false)
- `issueDateAfter` (date, optional): Filter invoices issued after date (YYYY-MM-DD)
- `issueDateBefore` (date, optional): Filter invoices issued before date (YYYY-MM-DD)
- `dueDateAfter` (date, optional): Filter invoices due after date (YYYY-MM-DD)
- `dueDateBefore` (date, optional): Filter invoices due before date (YYYY-MM-DD)
- `sentAfter` (date, optional): Filter invoices sent after date (YYYY-MM-DD)
- `sentBefore` (date, optional): Filter invoices sent before date (YYYY-MM-DD)
- `isOverdue` (boolean, optional): Filter overdue invoices (true/false)
- `statusGroup` (string, optional): Filter by status group
  - `draft` - DRAFT status
  - `pending` - PENDING_APPROVAL status
  - `active` - APPROVED, SENT, OVERDUE statuses
  - `unpaid` - Invoices with UNPAID or PARTIALLY_PAID payment status
  - `completed` - PAID, CANCELLED, VOID statuses

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 10): Number of items per page

**Sorting**:
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`
  - **Note**: All invoices are sorted by `createdAt` field

#### Example Requests

**Basic request**:
```
GET /api/invoices
```

**Filter by payment status**:
```
GET /api/invoices?paymentStatus=UNPAID
```

**Filter overdue invoices**:
```
GET /api/invoices?isOverdue=true&isActive=true
```

**Filter by customer with date range**:
```
GET /api/invoices?customerId=aB3Cd4Ef&issueDateAfter=2024-01-01&issueDateBefore=2024-12-31
```

**Filter by status group**:
```
GET /api/invoices?statusGroup=unpaid&page=0&size=20
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoices retrieved successfully",
  "data": {
    "invoices": [
      {
        "id": "mN4Op5Qr",
        "invoiceCode": "INV-2024-0001",
        "title": "Safari Tour Invoice",
        "customerName": "John Smith",
        "grandTotals": {
          "USD": 5900.00
        },
        "amountDue": {
          "USD": 5900.00
        },
        "status": "SENT",
        "paymentStatus": "UNPAID",
        "dueDate": "2024-06-15",
        "isOverdue": false,
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 45,
    "totalPages": 3
  },
  "timestamp": "2024-06-01T18:00:00"
}
```

---

### 9. Recalculate Invoice Totals

**Endpoint**: `POST /api/invoices/{idObfuscated}/recalculate-totals`

**Permission**: `PERM_UPDATE_INVOICE`

**Description**: Triggers recalculation of invoice totals based on current line items, tax percentage, and discount percentage. Use this after adding, updating, or removing invoice items.

#### Path Parameters
- `idObfuscated` (required): The obfuscated invoice ID

#### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Invoice totals recalculation triggered successfully",
  "data": null,
  "timestamp": "2024-06-01T19:00:00"
}
```

#### Error Responses

**400 Bad Request**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Failed to recalculate invoice totals",
  "errorCode": "RECALCULATION_FAILED",
  "timestamp": "2024-06-01T19:00:00"
}
```

---

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-06-01T10:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-06-01T10:00:00"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `INVOICE_NOT_FOUND` | 404 | Invoice with specified ID or code not found |
| `INVALID_INVOICE_ID` | 400 | The provided obfuscated ID is invalid or malformed |
| `INVALID_CUSTOMER_ID` | 400 | The provided customer ID is invalid |
| `INVALID_SAFARI_ID` | 400 | The provided safari ID is invalid |
| `CUSTOMER_NOT_FOUND` | 404 | Specified customer does not exist |
| `SAFARI_NOT_FOUND` | 404 | Specified safari does not exist |
| `SAFARI_NOT_CONFIRMED` | 400 | Safari must be confirmed before generating invoice |
| `INVALID_DUE_DATE` | 400 | Due date must be after issue date |
| `INVALID_ISSUE_DATE` | 400 | Issue date is invalid |
| `INVOICE_CREATE_FAILED` | 500 | Failed to create invoice (internal error) |
| `INVOICE_UPDATE_FAILED` | 500 | Failed to update invoice (internal error) |
| `INVOICE_FETCH_FAILED` | 500 | Failed to fetch invoice (internal error) |
| `INVOICES_FETCH_FAILED` | 500 | Failed to fetch invoices list (internal error) |
| `DELETE_FAILED` | 400 | Failed to delete all requested invoices |
| `NO_IDS_PROVIDED` | 400 | Delete request sent with empty ID list |
| `RECALCULATION_FAILED` | 400 | Failed to recalculate invoice totals |
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields, invalid format) |
| `PERMISSION_DENIED` | 403 | User lacks required permission for the operation |
| `UNAUTHORIZED` | 401 | User is not authenticated |

---

## Authentication & Authorization

All endpoints require:

1. **Authentication**: Valid JWT token in the `Authorization` header
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Authorization**: User must have the appropriate permission:
   - Create operations: `PERM_CREATE_INVOICE`
   - Read operations: `PERM_READ_INVOICE`
   - Update operations: `PERM_UPDATE_INVOICE`
   - Delete operations: `PERM_DELETE_INVOICE`

---

## Best Practices

1. **Use Invoice Codes for Customer Communication**: Use the `invoiceCode` (e.g., INV-2024-0001) in customer-facing communications for easy reference.

2. **Use IDs for Management**: Use obfuscated IDs for administrative operations (update, delete) in internal systems.

3. **Status Workflow**: Follow the proper status workflow:
   - DRAFT → PENDING_APPROVAL → APPROVED → SENT → PAID
   - Use OVERDUE for invoices past their due date
   - Use CANCELLED or VOID for invoices that won't be paid

4. **Generate from Safari**: Use the `/from-safari` endpoint to automatically generate invoices with all safari line items, reducing manual data entry.

5. **Recalculate Totals**: Always call the `/recalculate-totals` endpoint after modifying invoice items to ensure totals are accurate.

6. **Multi-Currency Support**: The system supports multiple currencies in price objects. Always check which currencies are present in the response.

7. **Payment Tracking**: Keep `paymentStatus` updated as payments are received. The system distinguishes between invoice status and payment status.

8. **Overdue Monitoring**: Use the `isOverdue` filter to monitor invoices needing follow-up.

9. **Date Validation**: Ensure `issueDate` and `dueDate` are logical when creating invoices.

10. **Handle Partial Updates**: When updating, only send fields that need to be changed.

11. **System-Managed Fields**: Don't attempt to update system-managed fields (invoiceCode, sentDate, paidDate, computed totals).

12. **Pagination**: Always use pagination for list endpoints to improve performance.

13. **Full vs Summary**: Use `/full` endpoint only when you need complete data with items. Use the regular GET endpoint for list views.

14. **Soft Deletes**: Consider using `isActive=false` instead of deletion to maintain audit history.

---

## Examples

### cURL Examples

**Create an invoice**:
```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Safari Tour Invoice - Northern Circuit",
    "description": "Invoice for 7-day safari tour",
    "customerId": "aB3Cd4Ef",
    "safariId": "xY9Kp2Lm",
    "taxPercentage": 18.00,
    "issueDate": "2024-06-01",
    "dueDate": "2024-06-15",
    "paymentTerms": "Net 30",
    "isActive": true
  }'
```

**Generate invoice from safari**:
```bash
curl -X POST http://localhost:8080/api/invoices/from-safari \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "safariId": "xY9Kp2Lm",
    "issueDate": "2024-06-01",
    "dueDate": "2024-06-15",
    "taxPercentage": 18.00,
    "includeAccommodations": true,
    "includeActivities": true,
    "includeParkFees": true
  }'
```

**Get all unpaid invoices**:
```bash
curl -X GET "http://localhost:8080/api/invoices?statusGroup=unpaid&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Get overdue invoices**:
```bash
curl -X GET "http://localhost:8080/api/invoices?isOverdue=true&isActive=true" \
  -H "Authorization: Bearer <your-token>"
```

**Get full invoice with items**:
```bash
curl -X GET http://localhost:8080/api/invoices/mN4Op5Qr/full \
  -H "Authorization: Bearer <your-token>"
```

**Update invoice status**:
```bash
curl -X PUT http://localhost:8080/api/invoices/mN4Op5Qr \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SENT",
    "paymentStatus": "UNPAID"
  }'
```

**Recalculate invoice totals**:
```bash
curl -X POST http://localhost:8080/api/invoices/mN4Op5Qr/recalculate-totals \
  -H "Authorization: Bearer <your-token>"
```

**Delete invoices**:
```bash
curl -X DELETE http://localhost:8080/api/invoices \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '["mN4Op5Qr", "xY9Kp2Lm"]'
```

---

## Notes

- **ID Obfuscation**: All invoice IDs, customer IDs, safari IDs, and user IDs are obfuscated for security. Never expose internal database IDs.

- **Invoice Code Generation**: Invoice codes are automatically generated in the format INV-YYYY-NNNN (e.g., INV-2024-0001) and are unique system-wide.

- **Timestamps**: All timestamps are in ISO 8601 format (UTC).

- **Multi-Currency Pricing**: Price objects support multiple currencies. Each currency has its own total.

- **Payment vs Invoice Status**: The system tracks both invoice status (workflow) and payment status (financial) separately.

- **Automatic Date Setting**: The `sentDate` is automatically set when status changes to SENT. The `paidDate` is automatically set when paymentStatus changes to PAID.

- **Overdue Calculation**: `isOverdue` and `daysPastDue` are computed based on current date vs due date and payment status.

- **Invoice Items**: Use dedicated Invoice Item APIs to add, update, or remove line items. Always recalculate totals after item changes.

- **Internal vs Customer Notes**: Use `internalNotes` for staff-only information and `invoiceNotes` for information visible to customers.

- **Safari Integration**: Invoices can be linked to safaris, enabling automatic generation of line items from safari bookings.

- **Sorting**: All list queries are sorted by `createdAt` in descending order by default (newest first).

---

## Support

For issues or questions about the Invoice API, please contact the development team or refer to the main application documentation.
