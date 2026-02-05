# Quote Item API Documentation

## Overview

The Quote Item API provides endpoints for managing line items within quotes. Quote items represent individual components of a quote such as accommodations, park fees, activities, transport, guides, and meals. Each item can have multiple prices in different currencies with automatic total price calculation.

**Base Path:** `/api/quotes/{quoteId}/items`

**Controller:** `QuoteItemController.java`

---

## Table of Contents

1. [Authentication & Permissions](#authentication--permissions)
2. [Data Models](#data-models)
3. [Endpoints](#endpoints)
   - [Create Quote Item](#1-create-quote-item)
   - [Update Quote Item](#2-update-quote-item)
   - [Delete Quote Items (Bulk)](#3-delete-quote-items-bulk)
   - [Get Quote Items (List with Filters)](#4-get-quote-items-list-with-filters)
   - [Get Quote Item by ID](#5-get-quote-item-by-id)
   - [Reorder Quote Items](#6-reorder-quote-items)
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
| `PERM_CREATE_QUOTE_ITEM` | Create Quote Item |
| `PERM_READ_QUOTE_ITEM` | Get Quote Items, Get Quote Item by ID |
| `PERM_UPDATE_QUOTE_ITEM` | Update Quote Item, Reorder Quote Items |
| `PERM_DELETE_QUOTE_ITEM` | Delete Quote Items |

---

## Data Models

### QuoteItemType Enum

```
ACCOMMODATION
PARK_FEE_CONSERVATION
PARK_FEE_ENTRY
ACTIVITY
TRANSPORT_VEHICLE_RENTAL
TRANSPORT_FUEL
TRANSPORT_DRIVER
GUIDE
MEALS
OTHER
```

### PriceInput Object

Used when creating or updating quote items. The system automatically computes `totalPrice = quantity × unitPrice`.

```json
{
  "currency": "USD",        // Required: 3-letter ISO currency code
  "quantity": 2,            // Required: Min 1
  "unitPrice": 150.00,      // Required: Min 0.0
  "breakdown": "Per person per night"  // Optional: Max 500 chars
}
```

**Note:** `totalPrice` is NOT provided by the user - it is computed automatically before saving.

### QuoteItemDTO (Response)

```json
{
  "id": "obfuscated_id",
  "quoteId": "obfuscated_quote_id",
  "quoteCode": "QT-2024-001",
  "itemType": "ACCOMMODATION",
  "itemName": "Serengeti Safari Lodge",
  "description": "Luxury tented camp with full board",
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
```

---

## Endpoints

### 1. Create Quote Item

Create a new item within a quote.

**Endpoint:** `POST /api/quotes/{quoteId}/items`

**Permission:** `PERM_CREATE_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |

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
- `quoteId` in request body is automatically set from the path parameter
- `displayOrder` is **automatically assigned** by the system (cannot be specified by user):
  - First item for a quote: `displayOrder = 0`
  - Subsequent items: `displayOrder = max(existing) + 1`
  - To change display order, use the dedicated `/reorder` endpoint

#### Response

**Success (201 Created):**

```json
{
  "status": 201,
  "message": "Quote item created successfully",
  "data": {
    "id": "xyz123",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "ACCOMMODATION",
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
}
```

**Error Responses:**

- `400 Bad Request` - Invalid quote ID or validation errors
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Quote not found
- `500 Internal Server Error` - Server error

---

### 2. Update Quote Item

Update an existing quote item. **Note:** Display order cannot be updated through this endpoint - use the reorder endpoint instead.

**Endpoint:** `PUT /api/quotes/{quoteId}/items/{itemId}`

**Permission:** `PERM_UPDATE_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |
| itemId | String | Yes | Obfuscated quote item ID |

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
- `displayOrder` field is NOT available in update - use `/reorder` endpoint
- When updating prices, provide complete price list (replaces existing prices)
- `totalPrice` is computed automatically from `quantity × unitPrice`

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Quote item updated successfully",
  "data": {
    "id": "xyz123",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "ACCOMMODATION",
    "itemName": "Updated Lodge Name",
    "description": "Updated description",
    "displayOrder": 0,
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
- `404 Not Found` - Quote item not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Quote Items (Bulk)

Delete one or more quote items in a single request.

**Endpoint:** `DELETE /api/quotes/{quoteId}/items`

**Permission:** `PERM_DELETE_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |

#### Request Body

Array of obfuscated quote item IDs:

```json
["item_id_1", "item_id_2", "item_id_3"]
```

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "3 quote items deleted successfully",
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
  "message": "2 quote items deleted, 1 not found",
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

### 4. Get Quote Items (List with Filters)

Retrieve quote items with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/quotes/{quoteId}/items`

**Permission:** `PERM_READ_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| itemType | QuoteItemType | No | - | Filter by specific item type |
| itemName | String | No | - | Filter by item name (case-insensitive, partial match) |
| description | String | No | - | Filter by description (case-insensitive, partial match) |
| isActive | Boolean | No | - | Filter by active status |
| itemTypeGroup | String | No | - | Filter by item type group: `accommodation`, `parkfee`, `activity`, `transport`, `guide`, `meal` |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 10 | Page size |
| sortDirection | String | No | asc | Sort direction: `asc` or `desc` (always sorts by displayOrder) |

**Note:** Items are always sorted by `displayOrder` field. The `sortDirection` parameter controls ascending or descending order.

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Quote items retrieved successfully",
  "data": {
    "items": [
      {
        "id": "xyz123",
        "quoteId": "abc456",
        "quoteCode": "QT-2024-001",
        "itemType": "ACCOMMODATION",
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

- `400 Bad Request` - Invalid quote ID or query parameters
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `500 Internal Server Error` - Server error

#### Example Requests

```bash
# Get all items for a quote
GET /api/quotes/abc456/items

# Get only accommodation items
GET /api/quotes/abc456/items?itemTypeGroup=accommodation

# Get active park fees, page 2
GET /api/quotes/abc456/items?itemTypeGroup=parkfee&isActive=true&page=1&size=20

# Search by item name
GET /api/quotes/abc456/items?itemName=serengeti

# Get items sorted descending
GET /api/quotes/abc456/items?sortDirection=desc
```

---

### 5. Get Quote Item by ID

Retrieve a specific quote item by its ID.

**Endpoint:** `GET /api/quotes/{quoteId}/items/{itemId}`

**Permission:** `PERM_READ_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |
| itemId | String | Yes | Obfuscated quote item ID |

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Quote item retrieved successfully",
  "data": {
    "id": "xyz123",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "ACCOMMODATION",
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
}
```

**Error Responses:**

- `400 Bad Request` - Invalid item ID
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Quote item not found
- `500 Internal Server Error` - Server error

---

### 6. Reorder Quote Items

Update the display order of quote items. This is the ONLY way to change item display order.

**Endpoint:** `POST /api/quotes/{quoteId}/items/reorder`

**Permission:** `PERM_UPDATE_QUOTE_ITEM`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |

#### Request Body

```json
{
  "itemOrder": [
    {
      "itemId": "item_id_3",
      "expectedDisplayOrder": 0    // Optional: for validation
    },
    {
      "itemId": "item_id_1",
      "expectedDisplayOrder": 1
    },
    {
      "itemId": "item_id_2",
      "expectedDisplayOrder": 2
    }
  ]
}
```

**Important Notes:**

1. The array defines the new order - first item gets displayOrder=0, second gets 1, etc.
2. `expectedDisplayOrder` is optional and used for optimistic locking validation
3. All items for the quote must be included in the reorder request
4. Uses a two-pass approach to avoid unique constraint violations
5. The operation is atomic - either all items are reordered or none

#### Response

**Success (200 OK):**

```json
{
  "status": 200,
  "message": "Quote items reordered successfully",
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

- `400 Bad Request` - Invalid quote ID, missing items, or validation errors
  - "All quote items must be included in reorder"
  - "Item does not belong to this quote"
  - "Display order mismatch (expected X, got Y)"
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Missing required permission
- `404 Not Found` - Quote or items not found
- `409 Conflict` - Display order conflict (optimistic locking failed)
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
| INVALID_QUOTE_ID | 400 | The provided quote ID is invalid or cannot be decoded |
| INVALID_QUOTE_ITEM_ID | 400 | The provided quote item ID is invalid or cannot be decoded |
| QUOTE_NOT_FOUND | 404 | Quote with the specified ID does not exist |
| QUOTE_ITEM_NOT_FOUND | 404 | Quote item with the specified ID does not exist |
| QUOTE_ITEM_CREATE_FAILED | 500 | Failed to create quote item |
| QUOTE_ITEM_UPDATE_FAILED | 500 | Failed to update quote item |
| QUOTE_ITEM_DELETE_FAILED | 500 | Failed to delete quote item(s) |
| QUOTE_ITEM_REORDER_FAILED | 500 | Failed to reorder quote items |
| VALIDATION_ERROR | 400 | Request validation failed |

---

## Examples

### Example 1: Create Accommodation Item

**Request:**

```bash
POST /api/quotes/abc456/items
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
  "message": "Quote item created successfully",
  "data": {
    "id": "xyz789",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "ACCOMMODATION",
    "itemName": "Serengeti Serena Safari Lodge",
    "description": "Full board accommodation in a luxury lodge",
    "displayOrder": 0,
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
- The system automatically assigned `displayOrder = 0` (first item for this quote)

---

### Example 2: Create Multi-Currency Park Fee Item

**Request:**

```bash
POST /api/quotes/abc456/items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemType": "PARK_FEE_ENTRY",
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
  "message": "Quote item created successfully",
  "data": {
    "id": "xyz890",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "PARK_FEE_ENTRY",
    "itemName": "Serengeti National Park Entry Fee",
    "description": "Per person per day",
    "displayOrder": 1,
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

**Note:** The system automatically assigned `displayOrder = 1` (second item for this quote, after the accommodation item created in Example 1).

---

### Example 3: Update Item Prices

**Request:**

```bash
PUT /api/quotes/abc456/items/xyz789
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
  "message": "Quote item updated successfully",
  "data": {
    "id": "xyz789",
    "quoteId": "abc456",
    "quoteCode": "QT-2024-001",
    "itemType": "ACCOMMODATION",
    "itemName": "Serengeti Serena Safari Lodge",
    "description": "Full board accommodation in a luxury lodge",
    "displayOrder": 0,
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
GET /api/quotes/abc456/items?itemTypeGroup=accommodation&isActive=true&page=0&size=5&sortDirection=asc
Authorization: Bearer eyJhbGc...
```

**Response:**

```json
{
  "status": 200,
  "message": "Quote items retrieved successfully",
  "data": {
    "items": [
      {
        "id": "xyz789",
        "itemType": "ACCOMMODATION",
        "itemName": "Serengeti Serena Safari Lodge",
        "displayOrder": 0,
        "prices": [{"currency": "USD", "totalPrice": 1100.00}],
        "isActive": true
      }
    ],
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
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
POST /api/quotes/abc456/items/reorder
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "itemOrder": [
    {"itemId": "item_3"},
    {"itemId": "item_1"},
    {"itemId": "item_2"}
  ]
}
```

**Response:**

```json
{
  "status": 200,
  "message": "Quote items reordered successfully",
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
DELETE /api/quotes/abc456/items
Authorization: Bearer eyJhbGc...
Content-Type: application/json

["item_1", "item_2", "item_3"]
```

**Response:**

```json
{
  "status": 200,
  "message": "3 quote items deleted successfully",
  "data": {
    "deletedCount": 3,
    "deletedIds": ["item_1", "item_2", "item_3"]
  }
}
```

---

## Notes

1. **Automatic Price Calculation:** When creating or updating items, users only provide `currency`, `quantity`, `unitPrice`, and optional `breakdown`. The system automatically computes `totalPrice = quantity × unitPrice`.

2. **Automatic Display Order Assignment:** When creating a new item, the `displayOrder` is automatically assigned by the system:
   - **First item** for a quote: `displayOrder = 0`
   - **Subsequent items**: `displayOrder = max(existing displayOrders) + 1`
   - Users **cannot** specify displayOrder during creation or update
   - To change display order, use the dedicated `/reorder` endpoint

3. **Multi-Currency Support:** Each quote item can have multiple prices in different currencies, useful for mixed tourist groups (e.g., residents and non-residents).

4. **Nested Resource:** Quote items are always accessed through their parent quote (`/api/quotes/{quoteId}/items`), ensuring proper data isolation.

5. **Sorting:** All list endpoints sort items by `displayOrder` field. Users can only control the sort direction (ascending or descending).

6. **Filtering Groups:** The `itemTypeGroup` parameter allows filtering by logical groups:
   - `accommodation` → ACCOMMODATION items
   - `parkfee` → PARK_FEE_* items
   - `activity` → ACTIVITY items
   - `transport` → TRANSPORT_* items
   - `guide` → GUIDE items
   - `meal` → MEALS items

7. **Validation:** All input is validated before processing. Check the response for detailed validation error messages.

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-01-29 | Initial documentation |

