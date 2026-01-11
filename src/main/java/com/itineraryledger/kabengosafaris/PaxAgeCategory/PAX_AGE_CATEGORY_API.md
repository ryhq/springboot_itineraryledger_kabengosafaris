# Pax Age Category API Documentation

Base URL: `/api/pax-age-categories`

## Overview

The Pax Age Category API manages passenger age categories used for pricing and itinerary management. Categories define age ranges (e.g., Child 0-5, Youth 6-14, Adult 15+) with validation to ensure no overlaps or gaps in coverage.

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
| `PERM_CREATE_PAX_AGE_CATEGORY` | Create new pax age categories |
| `PERM_READ_PAX_AGE_CATEGORY` | View pax age categories |
| `PERM_UPDATE_PAX_AGE_CATEGORY` | Update existing pax age categories |
| `PERM_DELETE_PAX_AGE_CATEGORY` | Delete pax age categories |

---

## Category Types

| Type | Display Name | Description |
|------|--------------|-------------|
| `CHILD` | Child | Typically 0-5 years old |
| `YOUTH` | Youth | Typically 6-14 years old |
| `ADULT` | Adult | Typically 15 years and above |
| `CUSTOM` | Custom | Custom age category |

---

## Endpoints

### 1. Create Pax Age Category

Creates a new pax age category.

**Request**
```
POST /api/pax-age-categories
```

**Permission:** `PERM_CREATE_PAX_AGE_CATEGORY`

**Request Body**
```json
{
  "name": "Child",
  "categoryType": "CHILD",
  "minAge": 0,
  "maxAge": 5,
  "description": "Children aged 0-5 years",
  "isActive": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Category name (max 100 characters) |
| `categoryType` | Enum | No | CHILD, YOUTH, ADULT, or CUSTOM |
| `minAge` | Integer | Yes | Minimum age (>= 0) |
| `maxAge` | Integer | Yes | Maximum age (>= minAge) |
| `description` | String | No | Category description |
| `isActive` | Boolean | No | Active status (default: true) |

**Validation Rules**
- First category must start at age 0
- No overlapping age ranges with existing categories
- No gaps between age ranges (categories must be contiguous)
- minAge cannot be greater than maxAge
- Name must be unique (case-insensitive)

**Success Response (201 Created)**
```json
{
  "status": 201,
  "message": "Pax age category created successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Child",
    "categoryType": "CHILD",
    "categoryTypeDisplayName": "Child",
    "categoryTypeDescription": "Typically 0-5 years old",
    "minAge": 0,
    "maxAge": 5,
    "ageRangeDisplay": "0-5 years",
    "description": "Children aged 0-5 years",
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
| 400 | `MISSING_MIN_AGE` | Minimum age is required |
| 400 | `MISSING_MAX_AGE` | Maximum age is required |
| 400 | `INVALID_MIN_AGE` | Minimum age cannot be negative |
| 400 | `INVALID_MAX_AGE` | Maximum age cannot be negative |
| 400 | `INVALID_AGE_RANGE` | Minimum age cannot be greater than maximum age |
| 400 | `DUPLICATE_CATEGORY_NAME` | Pax age category name already exists |
| 400 | `AGE_GAP_FROM_ZERO` | First age category must start at age 0 |
| 400 | `AGE_RANGE_OVERLAP` | Age range overlaps with existing category |
| 400 | `AGE_GAP_DETECTED` | Age range would create a gap in coverage |
| 500 | `PAX_AGE_CATEGORY_CREATE_FAILED` | Server error during creation |

---

### 2. Get Pax Age Category by ID

Retrieves a single pax age category by its ID.

**Request**
```
GET /api/pax-age-categories/{id}
```

**Permission:** `PERM_READ_PAX_AGE_CATEGORY`

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated category ID |

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax age category retrieved successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Child",
    "categoryType": "CHILD",
    "categoryTypeDisplayName": "Child",
    "categoryTypeDescription": "Typically 0-5 years old",
    "minAge": 0,
    "maxAge": 5,
    "ageRangeDisplay": "0-5 years",
    "description": "Children aged 0-5 years",
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
| 400 | `INVALID_PAX_AGE_CATEGORY_ID` | Invalid category ID format |
| 404 | `PAX_AGE_CATEGORY_NOT_FOUND` | Category not found |
| 500 | `PAX_AGE_CATEGORY_FETCH_FAILED` | Server error during fetch |

---

### 3. Get All Pax Age Categories

Retrieves all pax age categories with pagination, sorting, and filtering.

**Request**
```
GET /api/pax-age-categories
```

**Permission:** `PERM_READ_PAX_AGE_CATEGORY`

**Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | String | - | Filter by name (partial match, case-insensitive) |
| `categoryType` | Enum | - | Filter by category type (CHILD, YOUTH, ADULT, CUSTOM) |
| `isActive` | Boolean | - | Filter by active status |
| `isSystem` | Boolean | - | Filter by system status |
| `minAge` | Integer | - | Filter by exact minimum age |
| `maxAge` | Integer | - | Filter by exact maximum age |
| `age` | Integer | - | Filter categories that include this specific age |
| `keyword` | String | - | Search across name and description |
| `page` | Integer | 0 | Page number (0-indexed) |
| `size` | Integer | 10 | Page size |
| `sortDirection` | String | asc | Sort direction (asc/desc) by minAge |

**Example Request**
```
GET /api/pax-age-categories?categoryType=CHILD&isActive=true&page=0&size=10
```

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax age categories retrieved successfully",
  "data": {
    "paxAgeCategories": [
      {
        "id": "abc123xyz",
        "name": "Child",
        "categoryType": "CHILD",
        "categoryTypeDisplayName": "Child",
        "categoryTypeDescription": "Typically 0-5 years old",
        "minAge": 0,
        "maxAge": 5,
        "ageRangeDisplay": "0-5 years",
        "description": "Children aged 0-5 years",
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
      },
      {
        "id": "def456uvw",
        "name": "Youth",
        "categoryType": "YOUTH",
        "categoryTypeDisplayName": "Youth",
        "categoryTypeDescription": "Typically 6-14 years old",
        "minAge": 6,
        "maxAge": 16,
        "ageRangeDisplay": "6-14 years",
        "description": "Youthagers aged 6-14 years",
        "isActive": true,
        "isSystem": true,
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 3,
    "totalPages": 1
  }
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 500 | `PAX_AGE_CATEGORIES_FETCH_FAILED` | Server error during fetch |

---

### 4. Update Pax Age Category

Updates an existing pax age category.

**Request**
```
PUT /api/pax-age-categories/{id}
```

**Permission:** `PERM_UPDATE_PAX_AGE_CATEGORY`

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated category ID |

**Request Body**
```json
{
  "name": "Updated Child",
  "categoryType": "CHILD",
  "minAge": 0,
  "maxAge": 4,
  "description": "Updated description",
  "isActive": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | No | Category name (max 100 characters) |
| `categoryType` | Enum | No | CHILD, YOUTH, ADULT, or CUSTOM |
| `minAge` | Integer | No | Minimum age (>= 0) |
| `maxAge` | Integer | No | Maximum age (>= minAge) |
| `description` | String | No | Category description |
| `isActive` | Boolean | No | Active status |

**Note:** At least one field must be provided for update.

**Validation Rules**
- Same age range validation rules as create
- Name must be unique (case-insensitive, excluding current category)
- Updated age range must not create overlaps or gaps

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "Pax age category updated successfully",
  "data": {
    "id": "abc123xyz",
    "name": "Updated Child",
    "categoryType": "CHILD",
    "categoryTypeDisplayName": "Child",
    "categoryTypeDescription": "Typically 0-5 years old",
    "minAge": 0,
    "maxAge": 4,
    "ageRangeDisplay": "0-4 years",
    "description": "Updated description",
    "isActive": true,
    "isSystem": false,
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
| 400 | `INVALID_MIN_AGE` | Minimum age cannot be negative |
| 400 | `INVALID_MAX_AGE` | Maximum age cannot be negative |
| 400 | `INVALID_AGE_RANGE` | Minimum age cannot be greater than maximum age |
| 400 | `DUPLICATE_CATEGORY_NAME` | Pax age category name already exists |
| 400 | `INVALID_PAX_AGE_CATEGORY_ID` | Invalid category ID format |
| 400 | `AGE_GAP_FROM_ZERO` | Category must start at age 0 |
| 400 | `AGE_RANGE_OVERLAP` | Age range overlaps with existing category |
| 400 | `AGE_GAP_DETECTED` | Age range would create a gap in coverage |
| 404 | `PAX_AGE_CATEGORY_NOT_FOUND` | Category not found |
| 500 | `PAX_AGE_CATEGORY_UPDATE_FAILED` | Server error during update |

---

### 5. Delete Pax Age Categories

Deletes one or more pax age categories by their IDs.

**Request**
```
DELETE /api/pax-age-categories
```

**Permission:** `PERM_DELETE_PAX_AGE_CATEGORY`

**Request Body**
```json
["abc123xyz", "def456uvw"]
```

**Important:** System categories (created by initializer) cannot be deleted. If any ID in the list is a system category, the entire operation will be rejected.

**Success Response (200 OK)**
```json
{
  "status": 200,
  "message": "2 pax age category(ies) deleted successfully",
  "data": null
}
```

**Error Responses**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `NO_IDS_PROVIDED` | No pax age category IDs provided |
| 400 | `CANNOT_DELETE_SYSTEM_PAX_AGE_CATEGORIES` | Cannot delete system categories |
| 400 | `NO_PAX_AGE_CATEGORIES_DELETED` | No categories were deleted (not found) |
| 500 | `PAX_AGE_CATEGORY_DELETE_FAILED` | Server error during deletion |

---

## Response DTO Structure

### PaxAgeCategoryDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated category ID |
| `name` | String | Category name |
| `categoryType` | Enum | Category type (CHILD, YOUTH, ADULT, CUSTOM) |
| `categoryTypeDisplayName` | String | Human-readable category type name |
| `categoryTypeDescription` | String | Description of the category type |
| `minAge` | Integer | Minimum age for this category |
| `maxAge` | Integer | Maximum age for this category |
| `ageRangeDisplay` | String | Formatted age range (e.g., "0-5 years") |
| `description` | String | Category description |
| `isActive` | Boolean | Whether the category is active |
| `isSystem` | Boolean | Whether this is a system-created category |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

---

## Usage Examples

### Create Standard Age Categories

```bash
# Create Child category (0-5)
curl -X POST /api/pax-age-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Child",
    "categoryType": "CHILD",
    "minAge": 0,
    "maxAge": 5,
    "description": "Children aged 0-5 years"
  }'

# Create Youth category (6-14)
curl -X POST /api/pax-age-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Youth",
    "categoryType": "YOUTH",
    "minAge": 6,
    "maxAge": 16,
    "description": "Youthagers aged 6-14 years"
  }'

# Create Adult category (17+)
curl -X POST /api/pax-age-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Adult",
    "categoryType": "ADULT",
    "minAge": 15,
    "maxAge": 150,
    "description": "Adults aged 15 and above"
  }'
```

### Find Category for a Specific Age

```bash
# Find category that includes age 10
curl -X GET "/api/pax-age-categories?age=10" \
  -H "Authorization: Bearer <token>"
```

### Get Active Categories Only

```bash
curl -X GET "/api/pax-age-categories?isActive=true&sortDirection=asc" \
  -H "Authorization: Bearer <token>"
```

### Delete Multiple Categories

```bash
curl -X DELETE /api/pax-age-categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '["abc123xyz", "def456uvw"]'
```
