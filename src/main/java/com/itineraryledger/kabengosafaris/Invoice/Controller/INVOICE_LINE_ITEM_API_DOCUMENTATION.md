# Invoice Line Item API Documentation

## Overview

The Invoice Line Item API provides endpoints for managing line items within invoices. Invoice line items represent individual components of an invoice such as accommodations, park fees, activities, transport, guides, meals, equipment, insurance, visa fees, and other services. Each item can have multiple prices in different currencies with automatic total price calculation.

**Base Path:** `/api/invoices/{invoiceId}/line-items`

**Controller:** `InvoiceLineItemController.java`

---

## Table of Contents

1. [Authentication & Permissions](#authentication--permissions)
2. [Data Models](#data-models)
3. [Endpoints](#endpoints)
   - [Create Invoice Line Item](#1-create-invoice-line-item)
   - [Update Invoice Line Item](#2-update-invoice-line-item)
   - [Delete Invoice Line Items (Bulk)](#3-delete-invoice-line-items-bulk)
   - [Get Invoice Line Items (List with Filters)](#4-get-invoice-line-items-list-with-filters)
   - [Get Invoice Line Item by ID](#5-get-invoice-line-item-by-id)
   - [Reorder Invoice Line Items](#6-reorder-invoice-line-items)
4. [Error Handling](#error-handling)
5. [Examples](#examples)

---

## Authentication & Permissions

All endpoints require authentication via JWT token passed in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Permission | Endpoints |
|------------|-----------|
| `PERM_CREATE_INVOICE_LINE_ITEM` | Create Invoice Line Item |
| `PERM_READ_INVOICE_LINE_ITEM` | Get Invoice Line Items, Get Invoice Line Item by ID |
| `PERM_UPDATE_INVOICE_LINE_ITEM` | Update Invoice Line Item, Reorder Invoice Line Items |
| `PERM_DELETE_INVOICE_LINE_ITEM` | Delete Invoice Line Items |

---

## Data Models

### InvoiceItemType Enum

```
ACCOMMODATION      - Hotel, lodge, camp, or other accommodation
PARK_FEE          - National park entrance fees and conservation fees
ACTIVITY          - Safari activities like game drives, balloon rides, etc.
TRANSPORT         - Vehicle rental, transfers, flights
GUIDE             - Tour guide services
MEALS             - Breakfast, lunch, dinner
EQUIPMENT         - Camping gear, binoculars, etc.
INSURANCE         - Travel insurance, medical insurance
VISA              - Visa processing fees
OTHER             - Miscellaneous items
```

### PriceInput Object

Used when creating or updating invoice line items. The system automatically computes `totalPrice = quantity × unitPrice`.

```json
{
  "currency": "USD",        // Required: 3-letter ISO currency code
  "quantity": 2,            // Required: Min 1
  "unitPrice": 150.00,      // Required: Min 0.0
  "breakdown": "Per person per night"  // Optional: Max 500 chars
}
```

**Note:** `totalPrice` is NOT provided by the user - it is computed automatically before saving.

### InvoiceLineItemDTO (Response)

```json
{
  "id": "obfuscated_id",
  "invoiceId": "obfuscated_invoice_id",
  "invoiceCode": "INV-2024-001",
  "itemType": "ACCOMMODATION",
  "itemTypeDisplayName": "Accommodation",
  "itemName": "Serengeti Safari Lodge",
  "description": "Luxury tented camp with full board",
  "displayOrder": 1,
  "prices": [
    {
      "currency": "USD",
      "quantity": 2,
      "unitPrice": 150.00,
      "totalPrice": 300.00,
      "breakdown": "Per person per night"
    }
  ],
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

## Endpoints

### 1. Create Invoice Line Item

Create a new item within an invoice.

**Endpoint:** `POST /api/invoices/{invoiceId}/line-items`

**Permission:** `PERM_CREATE_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |

#### Request Body

```json
{
  "itemType": "ACCOMMODATION",                    // Required
  "itemName": "Serengeti Safari Lodge",           // Required
  "description": "Luxury tented camp",            // Optional
  "prices": [                                     // Required: At least 1 price
    {
      "currency": "USD",
      "quantity": 2,
      "unitPrice": 150.00,
      "breakdown": "Per person per night"
    }
  ],
  "isActive": true                                // Optional (default: true)
}
```

**Important Notes:**
- `invoiceId` in request body is automatically set from the path parameter
- `displayOrder` is **automatically assigned** by the system:
  - First item for an invoice: `displayOrder = 1`
  - Subsequent items: `displayOrder = max(existing) + 1`
- **Users cannot specify displayOrder during creation** - it must be set through the `/reorder` endpoint
- To change display order after creation, use the dedicated `/reorder` endpoint

#### Response

**Success (201 Created):**

```json
{
  "status": 201,
  "message": "Invoice line item created successfully",
  "data": {
    "id": "xyz123",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "ACCOMMODATION",
    "itemTypeDisplayName": "Accommodation",
    "itemName": "Serengeti Safari Lodge",
    "description": "Luxury tented camp",
    "displayOrder": 1,
    "prices": [
      {
        "currency": "USD",
        "quantity": 2,
        "unitPrice": 150.00,
        "totalPrice": 300.00,
        "breakdown": "Per person per night"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid invoice ID or validation errors
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Invoice not found
- `500 Internal Server Error` - Server error

---

### 2. Update Invoice Line Item

Update an existing invoice line item.

**Endpoint:** `PUT /api/invoices/{invoiceId}/line-items/{itemId}`

**Permission:** `PERM_UPDATE_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |
| itemId | String | Yes | Obfuscated invoice line item ID |

#### Request Body

All fields are optional. Only provided fields will be updated.

```json
{
  "itemType": "ACCOMMODATION",
  "itemName": "Updated Lodge Name",
  "description": "Updated description",
  "prices": [
    {
      "currency": "USD",
      "quantity": 3,
      "unitPrice": 175.00,
      "breakdown": "Updated breakdown"
    }
  ],
  "isActive": false
}
```

**Note:**
- `displayOrder` **cannot be updated** through this endpoint - it can **only** be changed using the `/reorder` endpoint
- When updating prices, provide complete price list (replaces existing prices)
- `totalPrice` is computed automatically from `quantity × unitPrice`

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Invoice line item updated successfully",
  "data": {
    "id": "xyz123",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "ACCOMMODATION",
    "itemTypeDisplayName": "Accommodation",
    "itemName": "Updated Lodge Name",
    "description": "Updated description",
    "displayOrder": 1,
    "prices": [
      {
        "currency": "USD",
        "quantity": 3,
        "unitPrice": 175.00,
        "totalPrice": 525.00,
        "breakdown": "Updated breakdown"
      }
    ],
    "isActive": false,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid item ID or validation errors
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Invoice line item not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Invoice Line Items (Bulk)

Delete one or more invoice line items in a single request.

**Endpoint:** `DELETE /api/invoices/{invoiceId}/line-items`

**Permission:** `PERM_DELETE_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |

#### Request Body

Array of obfuscated invoice line item IDs:

```json
["item_id_1", "item_id_2", "item_id_3"]
```

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "3 invoice line items deleted successfully",
  "data": {
    "deletedCount": 3,
    "deletedIds": ["item_id_1", "item_id_2", "item_id_3"]
  }
}
```

**Partial Success (200 OK with warnings):**

```json
{
  "status": 200,
  "message": "2 invoice line items deleted, 1 not found",
  "data": {
    "deletedCount": 2,
    "deletedIds": ["item_id_1", "item_id_2"],
    "notFoundIds": ["item_id_3"]
  }
}
```

**Error Responses:**

- `400 Bad Request` - Empty ID list or invalid IDs
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `500 Internal Server Error` - Server error

---

### 4. Get Invoice Line Items (List with Filters)

Retrieve invoice line items with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/invoices/{invoiceId}/line-items`

**Permission:** `PERM_READ_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| itemName | String | No | - | Filter by item name (case-insensitive, partial match) |
| description | String | No | - | Filter by description (case-insensitive, partial match) |
| isActive | Boolean | No | - | Filter by active status |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 10 | Page size |
| sortDirection | String | No | asc | Sort direction: `asc` or `desc` (always sorts by displayOrder) |

**Note:** Items are always sorted by `displayOrder` field. The `sortDirection` parameter controls ascending or descending order.

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Invoice line items retrieved successfully",
  "data": {
    "items": [
      {
        "id": "xyz123",
        "invoiceId": "abc456",
        "invoiceCode": "INV-2024-001",
        "itemType": "ACCOMMODATION",
        "itemTypeDisplayName": "Accommodation",
        "itemName": "Serengeti Safari Lodge",
        "description": "Luxury tented camp",
        "displayOrder": 0,
        "prices": [
          {
            "currency": "USD",
            "quantity": 2,
            "unitPrice": 150.00,
            "totalPrice": 300.00,
            "breakdown": "Per person per night"
          }
        ],
        "isActive": true,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
    "pageSize": 10,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid invoice ID or query parameters
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `500 Internal Server Error` - Server error

#### Example Requests

```bash
# Get all items for an invoice
GET /api/invoices/abc456/line-items

# Get only active items
GET /api/invoices/abc456/line-items?isActive=true

# Get items with pagination
GET /api/invoices/abc456/line-items?page=1&size=20

# Search by item name
GET /api/invoices/abc456/line-items?itemName=serengeti

# Get items sorted descending
GET /api/invoices/abc456/line-items?sortDirection=desc
```

---

### 5. Get Invoice Line Item by ID

Retrieve a specific invoice line item by its ID.

**Endpoint:** `GET /api/invoices/{invoiceId}/line-items/{itemId}`

**Permission:** `PERM_READ_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |
| itemId | String | Yes | Obfuscated invoice line item ID |

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Invoice line item retrieved successfully",
  "data": {
    "id": "xyz123",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "ACCOMMODATION",
    "itemTypeDisplayName": "Accommodation",
    "itemName": "Serengeti Safari Lodge",
    "description": "Luxury tented camp",
    "displayOrder": 1,
    "prices": [
      {
        "currency": "USD",
        "quantity": 2,
        "unitPrice": 150.00,
        "totalPrice": 300.00,
        "breakdown": "Per person per night"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid item ID
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Invoice line item not found
- `500 Internal Server Error` - Server error

---

### 6. Reorder Invoice Line Items

Update the display order of invoice line items. This is the recommended way to reorder multiple items at once.

**Endpoint:** `POST /api/invoices/{invoiceId}/line-items/reorder`

**Permission:** `PERM_UPDATE_INVOICE_LINE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| invoiceId | String | Yes | Obfuscated invoice ID |

#### Request Body

```json
{
  "itemOrders": [
    {
      "itemId": "item_id_3",
      "displayOrder": 0
    },
    {
      "itemId": "item_id_1",
      "displayOrder": 1
    },
    {
      "itemId": "item_id_2",
      "displayOrder": 2
    }
  ]
}
```

**Important Notes:**

1. Each object in `itemOrders` must include both `itemId` and `displayOrder`
2. All items for the invoice should be included in the reorder request
3. Display orders must be unique within the invoice
4. Uses a two-pass approach to avoid unique constraint violations
5. The operation is atomic - either all items are reordered or none

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Invoice line items reordered successfully",
  "data": {
    "reorderedCount": 3,
    "items": [
      {
        "id": "item_id_3",
        "displayOrder": 0,
        "itemName": "First Item"
      },
      {
        "id": "item_id_1",
        "displayOrder": 1,
        "itemName": "Second Item"
      },
      {
        "id": "item_id_2",
        "displayOrder": 2,
        "itemName": "Third Item"
      }
    ]
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid invoice ID, missing items, or validation errors
  - "Item order list cannot be empty"
  - "Item ID is required"
  - "Display order is required"
  - "Item does not belong to this invoice"
  - "Duplicate display orders found"
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Invoice or items not found
- `409 Conflict` - Display order conflict
- `500 Internal Server Error` - Server error

---

## Error Handling

### Standard Error Response Format

```json
{
  "status": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| INVALID_INVOICE_ID | 400 | The provided invoice ID is invalid or cannot be decoded |
| INVALID_INVOICE_LINE_ITEM_ID | 400 | The provided invoice line item ID is invalid or cannot be decoded |
| INVOICE_NOT_FOUND | 404 | Invoice with the specified ID does not exist |
| INVOICE_LINE_ITEM_NOT_FOUND | 404 | Invoice line item with the specified ID does not exist |
| INVOICE_LINE_ITEM_CREATE_FAILED | 500 | Failed to create invoice line item |
| INVOICE_LINE_ITEM_UPDATE_FAILED | 500 | Failed to update invoice line item |
| INVOICE_LINE_ITEM_DELETE_FAILED | 500 | Failed to delete invoice line item(s) |
| INVOICE_LINE_ITEM_REORDER_FAILED | 500 | Failed to reorder invoice line items |
| VALIDATION_ERROR | 400 | Request validation failed |

---

## Examples

### Example 1: Create Accommodation Item

**Request:**

```bash
POST /api/invoices/abc456/line-items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemType": "ACCOMMODATION",
  "itemName": "Serengeti Serena Safari Lodge",
  "description": "Full board accommodation in a luxury lodge",
  "prices": [
    {
      "currency": "USD",
      "quantity": 4,
      "unitPrice": 250.00,
      "breakdown": "Per person per night, full board"
    }
  ],
  "isActive": true
}
```

**Response:**

```json
{
  "status": 201,
  "message": "Invoice line item created successfully",
  "data": {
    "id": "xyz789",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "ACCOMMODATION",
    "itemTypeDisplayName": "Accommodation",
    "itemName": "Serengeti Serena Safari Lodge",
    "description": "Full board accommodation in a luxury lodge",
    "displayOrder": 1,
    "prices": [
      {
        "currency": "USD",
        "quantity": 4,
        "unitPrice": 250.00,
        "totalPrice": 1000.00,
        "breakdown": "Per person per night, full board"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Notes:**
- The system automatically calculated `totalPrice = 4 × 250.00 = 1000.00`
- The system automatically assigned `displayOrder = 1` (first item for this invoice)

---

### Example 2: Create Multi-Currency Park Fee Item

**Request:**

```bash
POST /api/invoices/abc456/line-items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemType": "PARK_FEE",
  "itemName": "Serengeti National Park Entry Fee",
  "description": "Per person per day",
  "prices": [
    {
      "currency": "USD",
      "quantity": 4,
      "unitPrice": 70.00,
      "breakdown": "Non-resident adult"
    },
    {
      "currency": "TZS",
      "quantity": 2,
      "unitPrice": 10000.00,
      "breakdown": "Resident adult"
    }
  ],
  "isActive": true
}
```

**Response:**

```json
{
  "status": 201,
  "message": "Invoice line item created successfully",
  "data": {
    "id": "xyz890",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "PARK_FEE",
    "itemTypeDisplayName": "Park Fee",
    "itemName": "Serengeti National Park Entry Fee",
    "description": "Per person per day",
    "displayOrder": 2,
    "prices": [
      {
        "currency": "USD",
        "quantity": 4,
        "unitPrice": 70.00,
        "totalPrice": 280.00,
        "breakdown": "Non-resident adult"
      },
      {
        "currency": "TZS",
        "quantity": 2,
        "unitPrice": 10000.00,
        "totalPrice": 20000.00,
        "breakdown": "Resident adult"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  }
}
```

**Note:** The system automatically assigned `displayOrder = 2` (second item for this invoice, after the accommodation item created in Example 1).

---

### Example 3: Update Item Prices

**Request:**

```bash
PUT /api/invoices/abc456/line-items/xyz789
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "prices": [
    {
      "currency": "USD",
      "quantity": 4,
      "unitPrice": 275.00,
      "breakdown": "Per person per night, full board (updated rate)"
    }
  ]
}
```

**Response:**

```json
{
  "status": 200,
  "message": "Invoice line item updated successfully",
  "data": {
    "id": "xyz789",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "ACCOMMODATION",
    "itemTypeDisplayName": "Accommodation",
    "itemName": "Serengeti Serena Safari Lodge",
    "description": "Full board accommodation in a luxury lodge",
    "displayOrder": 1,
    "prices": [
      {
        "currency": "USD",
        "quantity": 4,
        "unitPrice": 275.00,
        "totalPrice": 1100.00,
        "breakdown": "Per person per night, full board (updated rate)"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T14:20:00"
  }
}
```

**Note:** Total price automatically recalculated: `4 × 275.00 = 1100.00`

---

### Example 4: Filter and Paginate Items

**Request:**

```bash
GET /api/invoices/abc456/line-items?itemName=serengeti&isActive=true&page=0&size=5&sortDirection=asc
Authorization: Bearer eyJhbGc...
```

**Response:**

```json
{
  "status": 200,
  "message": "Invoice line items retrieved successfully",
  "data": {
    "items": [
      {
        "id": "xyz789",
        "itemType": "ACCOMMODATION",
        "itemTypeDisplayName": "Accommodation",
        "itemName": "Serengeti Serena Safari Lodge",
        "displayOrder": 1,
        "prices": [{"currency": "USD", "totalPrice": 1100.00}],
        "isActive": true
      },
      {
        "id": "xyz890",
        "itemType": "PARK_FEE",
        "itemTypeDisplayName": "Park Fee",
        "itemName": "Serengeti National Park Entry Fee",
        "displayOrder": 2,
        "prices": [
          {"currency": "USD", "totalPrice": 280.00},
          {"currency": "TZS", "totalPrice": 20000.00}
        ],
        "isActive": true
      }
    ],
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 2,
    "pageSize": 5,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

---

### Example 5: Reorder Items

**Request:**

```bash
POST /api/invoices/abc456/line-items/reorder
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemOrders": [
    {"itemId": "item_3", "displayOrder": 0},
    {"itemId": "item_1", "displayOrder": 1},
    {"itemId": "item_2", "displayOrder": 2}
  ]
}
```

**Response:**

```json
{
  "status": 200,
  "message": "Invoice line items reordered successfully",
  "data": {
    "reorderedCount": 3,
    "items": [
      {"id": "item_3", "displayOrder": 0, "itemName": "Park Entry Fee"},
      {"id": "item_1", "displayOrder": 1, "itemName": "Accommodation"},
      {"id": "item_2", "displayOrder": 2, "itemName": "Activity Fee"}
    ]
  }
}
```

---

### Example 6: Bulk Delete Items

**Request:**

```bash
DELETE /api/invoices/abc456/line-items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

["item_1", "item_2", "item_3"]
```

**Response:**

```json
{
  "status": 200,
  "message": "3 invoice line items deleted successfully",
  "data": {
    "deletedCount": 3,
    "deletedIds": ["item_1", "item_2", "item_3"]
  }
}
```

---

### Example 7: Create Invoice Item with Multiple Item Types

**Request:**

```bash
POST /api/invoices/abc456/line-items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemType": "INSURANCE",
  "itemName": "Safari Travel Insurance",
  "description": "Comprehensive travel and medical insurance coverage",
  "prices": [
    {
      "currency": "USD",
      "quantity": 4,
      "unitPrice": 45.00,
      "breakdown": "Per person for 7-day safari"
    }
  ],
  "isActive": true
}
```

**Response:**

```json
{
  "status": 201,
  "message": "Invoice line item created successfully",
  "data": {
    "id": "xyz991",
    "invoiceId": "abc456",
    "invoiceCode": "INV-2024-001",
    "itemType": "INSURANCE",
    "itemTypeDisplayName": "Insurance",
    "itemName": "Safari Travel Insurance",
    "description": "Comprehensive travel and medical insurance coverage",
    "displayOrder": 3,
    "prices": [
      {
        "currency": "USD",
        "quantity": 4,
        "unitPrice": 45.00,
        "totalPrice": 180.00,
        "breakdown": "Per person for 7-day safari"
      }
    ],
    "isActive": true,
    "createdAt": "2024-01-15T10:40:00",
    "updatedAt": "2024-01-15T10:40:00"
  }
}
```

**Note:** The `displayOrder` was automatically assigned as 3 by the system (third item for this invoice).

---

## Notes

1. **Automatic Price Calculation:** When creating or updating items, users only provide `currency`, `quantity`, `unitPrice`, and optional `breakdown`. The system automatically computes `totalPrice = quantity × unitPrice`.

2. **Display Order Management:**
   - When creating a new item, `displayOrder` is **automatically assigned** by the system and **cannot be specified by users**
   - **First item** for an invoice gets `displayOrder = 1`, **subsequent items** get `displayOrder = max(existing displayOrders) + 1`
   - Display order can only be changed using the dedicated `/reorder` endpoint
   - For reordering multiple items efficiently, always use the `/reorder` endpoint

3. **Multi-Currency Support:** Each invoice line item can have multiple prices in different currencies, useful for mixed payment scenarios or international clients.

4. **Nested Resource:** Invoice line items are always accessed through their parent invoice (`/api/invoices/{invoiceId}/line-items`), ensuring proper data isolation.

5. **Sorting:** All list endpoints sort items by `displayOrder` field. Users can control the sort direction (ascending or descending).

6. **Item Types:** Invoice items support 10 different types covering common safari billing scenarios:
   - **ACCOMMODATION** - Hotels, lodges, camps
   - **PARK_FEE** - National park fees
   - **ACTIVITY** - Game drives, balloon rides, etc.
   - **TRANSPORT** - Vehicle rental, transfers
   - **GUIDE** - Tour guide services
   - **MEALS** - Food services
   - **EQUIPMENT** - Camping gear, etc.
   - **INSURANCE** - Travel insurance
   - **VISA** - Visa processing
   - **OTHER** - Miscellaneous items

7. **Validation:** All input is validated before processing. Check the response for detailed validation error messages.

8. **Relationship to Invoices:** Line items are part of invoice totals calculation. Changes to line items may trigger invoice total recalculation depending on the implementation.

---

## Best Practices

1. **Create Items Before Sending Invoice:** Add all line items before marking the invoice as SENT to avoid confusion with clients.

2. **Use Descriptive Names:** Use clear, descriptive item names that clients will easily understand on the invoice.

3. **Provide Breakdown Details:** Use the `breakdown` field to explain pricing (e.g., "Per person per night", "4 adults × 3 days").

4. **Group Related Items:** Use `displayOrder` to group related items together (e.g., all accommodation items, then all park fees).

5. **Multi-Currency Pricing:** When dealing with international clients, provide prices in multiple currencies where appropriate.

6. **Keep Items Active:** Only set `isActive = false` for items that should be hidden but preserved for audit purposes.

7. **Bulk Operations:** Use bulk delete for efficiency when removing multiple items at once.

8. **Reorder Endpoint:** When reordering multiple items, use the dedicated `/reorder` endpoint instead of updating each item individually for better performance and atomicity.

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-02-04 | Initial documentation |

