# Pax Nation Category API Documentation

Base URL: `/api/pax-nation-categories`

## Overview

The Pax Nation Category API manages passenger nationality categories used for pricing and itinerary management. Categories define nationality-based pricing tiers (e.g., Resident, Expatriate, East African, Non-Resident) with priority factors for group rate calculations.

---

## Authentication

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

---

## Permissions

| Permission | Description |
|------------|-------------|
| `PERM_CREATE_PAX_NATION_CATEGORY` | Create new pax nation categories |
| `PERM_READ_PAX_NATION_CATEGORY` | View pax nation categories |
| `PERM_UPDATE_PAX_NATION_CATEGORY` | Update existing pax nation categories |
| `PERM_DELETE_PAX_NATION_CATEGORY` | Delete pax nation categories |

---

## Category Types

| Type | Display Name | Description |
|------|--------------|-------------|
| `RESIDENT` | Resident | Tanzanian citizens and permanent residents |
| `EXPATRIATE` | Expatriate | Foreign nationals residing in Tanzania with work/residence permits |
| `EAST_AFRICAN` | East African | Citizens of East African Community member states |
| `NON_RESIDENT` | Non-Resident | International visitors from outside East Africa |
| `CUSTOM` | Custom | Custom nationality category |

---

## Priority Factor

The priority factor is used when `ChargingBasis` is `PER_VEHICLE` or `PER_GROUP`:
- Higher priority factor = higher rate tier
- The passenger with the highest priority factor determines the rate for the entire group
- Example: Vehicle with Residents (priority 1) and Non-Residents (priority 4) uses Non-Resident rates

**Standard Priority Assignments:**
| Category | Priority Factor | Rate Tier |
|----------|----------------|-----------|
| Resident | 1 | Lowest |
| Expatriate | 2 | Low-Medium |
| East African | 3 | Medium-High |
| Non-Resident | 4 | Highest |

---

## Endpoints

### 1. Create Pax Nation Category

Creates a new pax nation category.

**Request**
```
POST /api/pax-nation-categories
```

**Permission:** `PERM_CREATE_PAX_NATION_CATEGORY`

**Request Body**
```json
{
  "name": "Diplomatic",
  "categoryType": "CUSTOM",
  "description": "Diplomatic staff and their families",
  "priorityFactor": 2,
  "isActive": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Category name (max 100 characters) |
| `categoryType` | Enum | No | RESIDENT, EXPATRIATE, EAST_AFRICAN, NON_RESIDENT, or CUSTOM |
| `description` | String | No | Category description |
| `priorityFactor` | Integer | No | Priority for rate selection (>= 1, auto-assigned if not provided) |
| `isActive` | Boolean | No | Active status (default: true) |

**Validation Rules**
- Name must be unique (case-insensitive)
- Priority factor must be >= 1
- Priority factor must be unique across all categories

**Success Response (201 Created)**
```json
{
  "status": 201,
  "message": "Pax nation category created successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Diplomatic",
    "categoryType": "CUSTOM",
    "categoryTypeDisplayName": "Custom",
    "categoryTypeDescription": "Custom nationality category",
    "description": "Diplomatic staff and their families",
    "priorityFactor": 2,
    "priorityDisplay": "Priority 2",
    "isActive": true,
    "isSystem": false,
    "createdAt": "2026-01-11T10:30:00",
    "updatedAt": "2026-01-11T10:30:00"
  }
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `INVALID_NAME` | Category name cannot be empty |
| 400 | `NAME_TOO_LONG` | Category name cannot exceed 100 characters |
| 400 | `DUPLICATE_CATEGORY_NAME` | Pax nation category name already exists |
| 400 | `INVALID_PRIORITY_FACTOR` | Priority factor must be at least 1 |
| 400 | `DUPLICATE_PRIORITY_FACTOR` | Priority factor is already in use |
| 500 | `PAX_NATION_CATEGORY_CREATE_FAILED` | Server error during creation |

---

### 2. Get Pax Nation Category by ID

Retrieves a single pax nation category by its ID.

**Request**
```
GET /api/pax-nation-categories/{id}
```

**Permission:** `PERM_READ_PAX_NATION_CATEGORY`

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated category ID |

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax nation category retrieved successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Resident",
    "categoryType": "RESIDENT",
    "categoryTypeDisplayName": "Resident",
    "categoryTypeDescription": "Tanzanian citizens and permanent residents",
    "description": "Tanzanian citizens and permanent residents...",
    "priorityFactor": 1,
    "priorityDisplay": "Priority 1",
    "isActive": true,
    "isSystem": true,
    "createdAt": "2026-01-11T10:30:00",
    "updatedAt": "2026-01-11T10:30:00"
  }
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `INVALID_PAX_NATION_CATEGORY_ID` | Invalid category ID format |
| 404 | `PAX_NATION_CATEGORY_NOT_FOUND` | Category not found |
| 500 | `PAX_NATION_CATEGORY_FETCH_FAILED` | Server error during fetch |

---

### 3. Get All Pax Nation Categories

Retrieves all pax nation categories with pagination, sorting, and filtering.

**Request**
```
GET /api/pax-nation-categories
```

**Permission:** `PERM_READ_PAX_NATION_CATEGORY`

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | String | - | Filter by name (partial match, case-insensitive) |
| `categoryType` | Enum | - | Filter by category type |
| `isActive` | Boolean | - | Filter by active status |
| `isSystem` | Boolean | - | Filter by system status |
| `priorityFactor` | Integer | - | Filter by exact priority factor |
| `keyword` | String | - | Search across name and description |
| `page` | Integer | 0 | Page number (0-indexed) |
| `size` | Integer | 10 | Page size |
| `sortDirection` | String | asc | Sort direction (asc/desc) by priority factor |

**Example Request**
```
GET /api/pax-nation-categories?categoryType=RESIDENT&isActive=true&page=0&size=10
```

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax nation categories retrieved successfully",
  "data": {
    "paxNationCategories": [
      {
        "id": "abc123xyz",
        "name": "Resident",
        "categoryType": "RESIDENT",
        "categoryTypeDisplayName": "Resident",
        "categoryTypeDescription": "Tanzanian citizens and permanent residents",
        "description": "Tanzanian citizens and permanent residents...",
        "priorityFactor": 1,
        "priorityDisplay": "Priority 1",
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
      },
      {
        "id": "def456uvw",
        "name": "Expatriate",
        "categoryType": "EXPATRIATE",
        "categoryTypeDisplayName": "Expatriate",
        "categoryTypeDescription": "Foreign nationals residing in Tanzania with work/residence permits",
        "description": "Foreign nationals residing in Tanzania...",
        "priorityFactor": 2,
        "priorityDisplay": "Priority 2",
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 4,
    "totalPages": 1
  }
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 500 | `PAX_NATION_CATEGORIES_FETCH_FAILED` | Server error during fetch |

---

### 4. Update Pax Nation Category

Updates an existing pax nation category.

**Request**
```
PUT /api/pax-nation-categories/{id}
```

**Permission:** `PERM_UPDATE_PAX_NATION_CATEGORY`

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated category ID |

**Request Body**
```json
{
  "name": "Updated Resident",
  "categoryType": "RESIDENT",
  "description": "Updated description",
  "priorityFactor": 1,
  "isActive": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | No | Category name (max 100 characters) |
| `categoryType` | Enum | No | RESIDENT, EXPATRIATE, EAST_AFRICAN, NON_RESIDENT, or CUSTOM |
| `description` | String | No | Category description |
| `priorityFactor` | Integer | No | Priority for rate selection (>= 1) |
| `isActive` | Boolean | No | Active status |

**Note:** At least one field must be provided for update.

**Validation Rules**
- Same validation rules as create
- Name must be unique (case-insensitive, excluding current category)
- Priority factor must be unique (excluding current category)

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax nation category updated successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Updated Resident",
    "categoryType": "RESIDENT",
    "categoryTypeDisplayName": "Resident",
    "categoryTypeDescription": "Tanzanian citizens and permanent residents",
    "description": "Updated description",
    "priorityFactor": 1,
    "priorityDisplay": "Priority 1",
    "isActive": true,
    "isSystem": true,
    "createdAt": "2026-01-11T10:30:00",
    "updatedAt": "2026-01-11T11:00:00"
  }
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `NO_FIELDS_TO_UPDATE` | At least one field must be provided |
| 400 | `INVALID_NAME` | Category name cannot be empty |
| 400 | `NAME_TOO_LONG` | Category name cannot exceed 100 characters |
| 400 | `DUPLICATE_CATEGORY_NAME` | Pax nation category name already exists |
| 400 | `INVALID_PAX_NATION_CATEGORY_ID` | Invalid category ID format |
| 400 | `INVALID_PRIORITY_FACTOR` | Priority factor must be at least 1 |
| 400 | `DUPLICATE_PRIORITY_FACTOR` | Priority factor is already in use |
| 404 | `PAX_NATION_CATEGORY_NOT_FOUND` | Category not found |
| 500 | `PAX_NATION_CATEGORY_UPDATE_FAILED` | Server error during update |

---

### 5. Delete Pax Nation Categories

Deletes one or more pax nation categories by their IDs.

**Request**
```
DELETE /api/pax-nation-categories
```

**Permission:** `PERM_DELETE_PAX_NATION_CATEGORY`

**Request Body**
```json
["abc123xyz", "def456uvw"]
```

**Important:** System categories (created by initializer) cannot be deleted. If any ID in the list is a system category, the entire operation will be rejected.

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "2 pax nation category(ies) deleted successfully",
  "data": null
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `NO_IDS_PROVIDED` | No pax nation category IDs provided |
| 400 | `INVALID_PAX_NATION_CATEGORY_IDS` | Invalid category ID(s) in the list |
| 400 | `CANNOT_DELETE_SYSTEM_PAX_NATION_CATEGORIES` | Cannot delete system categories |
| 400 | `NO_PAX_NATION_CATEGORIES_DELETED` | No categories were deleted (not found) |
| 500 | `PAX_NATION_CATEGORY_DELETE_FAILED` | Server error during deletion |

---

## Response DTO Structure

### PaxNationCategoryDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated category ID |
| `name` | String | Category name |
| `categoryType` | Enum | Category type (RESIDENT, EXPATRIATE, EAST_AFRICAN, NON_RESIDENT, CUSTOM) |
| `categoryTypeDisplayName` | String | Human-readable category type name |
| `categoryTypeDescription` | String | Description of the category type |
| `description` | String | Category description |
| `priorityFactor` | Integer | Priority for rate selection (higher = higher priority) |
| `priorityDisplay` | String | Formatted priority display (e.g., "Priority 1") |
| `isActive` | Boolean | Whether the category is active |
| `isSystem` | Boolean | Whether this is a system-created category |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

---

## Usage Examples

### Create Custom Category

```bash
curl -X POST /api/pax-nation-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "NGO Worker",
    "categoryType": "CUSTOM",
    "description": "Staff of registered NGOs operating in Tanzania",
    "priorityFactor": 5,
    "isActive": true
  }'
```

### Get Categories by Type

```bash
curl -X GET "/api/pax-nation-categories?categoryType=RESIDENT&isActive=true" \
  -H "Authorization: Bearer <token>"
```

### Get Active Categories Sorted by Priority

```bash
curl -X GET "/api/pax-nation-categories?isActive=true&sortDirection=asc" \
  -H "Authorization: Bearer <token>"
```

### Delete Multiple Categories

```bash
curl -X DELETE /api/pax-nation-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '["abc123xyz", "def456uvw"]'
```
