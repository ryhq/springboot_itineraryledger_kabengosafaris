# Hero Management API Documentation

## Overview

The Hero Management API provides endpoints for managing website hero sections. Hero sections are prominent visual elements displayed on different pages of the website with images, titles, subtitles, call-to-action buttons, and overlay settings.

**Base URL:** `/api/heroes`

**Controller:** `HeroController.java`

**Tag:** Hero Management

---

## Authentication & Authorization

All endpoints (except public endpoints) require JWT authentication via Bearer token in the Authorization header.

### Required Permissions

| Permission | Description |
|------------|-------------|
| `PERM_CREATE_HERO` | Required to create new hero sections |
| `PERM_READ_HERO` | Required to read hero data (list and get by ID) |
| `PERM_UPDATE_HERO` | Required to update or reorder hero sections |
| `PERM_DELETE_HERO` | Required to delete hero sections |

### Public Endpoints

- `GET /api/heroes/page/{page}` - No authentication required (for front-end website display)

---

## Endpoints

### 1. Create Hero

Create a new hero section for a website page.

**Endpoint:** `POST /api/heroes`

**Permission:** `PERM_CREATE_HERO`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "Explore Tanzania's Wildlife",
  "subtitle": "Unforgettable Safari Adventures",
  "description": "Discover the beauty of East Africa with our expertly curated safari packages",
  "page": "HOME",
  "ctaText": "Book Your Safari",
  "ctaLink": "/safaris",
  "displayOrder": 1,
  "isActive": true,
  "overlayColor": "#000000",
  "overlayOpacity": 0.4,
  "textAlignment": "center",
  "cssClasses": "hero-fade-in hero-large"
}
```

**CreateHeroDTO Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | Yes | Hero section title |
| subtitle | String | No | Hero section subtitle |
| description | String | No | Detailed description |
| page | HeroPage | Yes | Page where hero appears (HOME, ABOUT, SAFARIS, ACCOMMODATIONS, PARKS, ACTIVITIES, CONTACT, BLOG) |
| ctaText | String | No | Call-to-action button text |
| ctaLink | String | No | Call-to-action button URL |
| displayOrder | Integer | No | Order of appearance (auto-assigned if null) |
| isActive | Boolean | No | Active status (default: true) |
| overlayColor | String | No | Overlay color (hex format) |
| overlayOpacity | Double | No | Overlay opacity (0.0 to 1.0) |
| textAlignment | String | No | Text alignment (left, center, right) |
| cssClasses | String | No | Custom CSS classes |

**Response (201 Created):**
```json
{
  "status": 201,
  "message": "Hero created successfully",
  "data": {
    "id": "xY8kL3mP",
    "title": "Explore Tanzania's Wildlife",
    "subtitle": "Unforgettable Safari Adventures",
    "description": "Discover the beauty of East Africa with our expertly curated safari packages",
    "page": "HOME",
    "pageDisplayName": "Home",
    "ctaText": "Book Your Safari",
    "ctaLink": "/safaris",
    "displayOrder": 1,
    "isActive": true,
    "overlayColor": "#000000",
    "overlayOpacity": 0.4,
    "textAlignment": "center",
    "cssClasses": "hero-fade-in hero-large",
    "primaryImageUrl": null,
    "imageCount": 0,
    "createdById": "aB3dE5fG",
    "createdByName": "John Doe",
    "updatedById": "aB3dE5fG",
    "updatedByName": "John Doe",
    "createdAt": "2026-02-09T10:30:00",
    "updatedAt": "2026-02-09T10:30:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Validation error (missing required fields, invalid data)
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_CREATE_HERO permission
- `500 Internal Server Error` - Server error

---

### 2. Update Hero

Update an existing hero section. Only provided fields will be updated (partial update supported).

**Endpoint:** `PUT /api/heroes/{idObfuscated}`

**Permission:** `PERM_UPDATE_HERO`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated hero ID |

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body (all fields optional):**
```json
{
  "title": "Discover Tanzania's National Parks",
  "subtitle": "Updated Subtitle",
  "overlayOpacity": 0.5,
  "isActive": true
}
```

**UpdateHeroDTO Fields (all optional):**

| Field | Type | Description |
|-------|------|-------------|
| title | String | Hero section title |
| subtitle | String | Hero section subtitle |
| description | String | Detailed description |
| page | HeroPage | Page where hero appears |
| ctaText | String | Call-to-action button text |
| ctaLink | String | Call-to-action button URL |
| displayOrder | Integer | Order of appearance |
| isActive | Boolean | Active status |
| overlayColor | String | Overlay color (hex format) |
| overlayOpacity | Double | Overlay opacity (0.0 to 1.0) |
| textAlignment | String | Text alignment |
| cssClasses | String | Custom CSS classes |

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero updated successfully",
  "data": {
    "id": "xY8kL3mP",
    "title": "Discover Tanzania's National Parks",
    "subtitle": "Updated Subtitle",
    "description": "Discover the beauty of East Africa with our expertly curated safari packages",
    "page": "HOME",
    "pageDisplayName": "Home",
    "ctaText": "Book Your Safari",
    "ctaLink": "/safaris",
    "displayOrder": 1,
    "isActive": true,
    "overlayColor": "#000000",
    "overlayOpacity": 0.5,
    "textAlignment": "center",
    "cssClasses": "hero-fade-in hero-large",
    "primaryImageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
    "imageCount": 3,
    "createdById": "aB3dE5fG",
    "createdByName": "John Doe",
    "updatedById": "zX9wY8vU",
    "updatedByName": "Jane Smith",
    "createdAt": "2026-02-09T10:30:00",
    "updatedAt": "2026-02-09T14:25:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid ID or validation error
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_UPDATE_HERO permission
- `404 Not Found` - Hero not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Heroes (Bulk)

Delete one or more hero sections by their IDs. This operation also deletes associated images from database and filesystem.

**Endpoint:** `DELETE /api/heroes`

**Permission:** `PERM_DELETE_HERO`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
["xY8kL3mP", "aB3dE5fG", "zX9wY8vU"]
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "3 hero(es) deleted successfully",
  "data": null
}
```

**Partial Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "2 hero(es) deleted successfully. 1 hero(es) not found: [invalidId123]",
  "data": null
}
```

**Error Responses:**

- `400 Bad Request` - No IDs provided or all IDs invalid
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_DELETE_HERO permission
- `500 Internal Server Error` - Server error

---

### 4. Get Hero by ID

Retrieve a single hero section by its obfuscated ID.

**Endpoint:** `GET /api/heroes/{idObfuscated}`

**Permission:** `PERM_READ_HERO`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated hero ID |

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero retrieved successfully",
  "data": {
    "id": "xY8kL3mP",
    "title": "Explore Tanzania's Wildlife",
    "subtitle": "Unforgettable Safari Adventures",
    "description": "Discover the beauty of East Africa with our expertly curated safari packages",
    "page": "HOME",
    "pageDisplayName": "Home",
    "ctaText": "Book Your Safari",
    "ctaLink": "/safaris",
    "displayOrder": 1,
    "isActive": true,
    "overlayColor": "#000000",
    "overlayOpacity": 0.4,
    "textAlignment": "center",
    "cssClasses": "hero-fade-in hero-large",
    "primaryImageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
    "imageCount": 3,
    "createdById": "aB3dE5fG",
    "createdByName": "John Doe",
    "updatedById": "aB3dE5fG",
    "updatedByName": "John Doe",
    "createdAt": "2026-02-09T10:30:00",
    "updatedAt": "2026-02-09T10:30:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid ID format
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_READ_HERO permission
- `404 Not Found` - Hero not found
- `500 Internal Server Error` - Server error

---

### 5. Get Heroes by Page (Public)

Retrieve all active hero sections for a specific page. This endpoint is public (no authentication required) for front-end website display.

**Endpoint:** `GET /api/heroes/page/{page}`

**Permission:** None (public access)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| page | HeroPage | Page name (HOME, ABOUT, SAFARIS, ACCOMMODATIONS, PARKS, ACTIVITIES, CONTACT, BLOG) |

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Heroes retrieved successfully",
  "data": [
    {
      "id": "xY8kL3mP",
      "title": "Explore Tanzania's Wildlife",
      "subtitle": "Unforgettable Safari Adventures",
      "description": "Discover the beauty of East Africa with our expertly curated safari packages",
      "page": "HOME",
      "pageDisplayName": "Home",
      "ctaText": "Book Your Safari",
      "ctaLink": "/safaris",
      "displayOrder": 1,
      "isActive": true,
      "overlayColor": "#000000",
      "overlayOpacity": 0.4,
      "textAlignment": "center",
      "cssClasses": "hero-fade-in hero-large",
      "primaryImageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
      "imageCount": 3,
      "createdById": "aB3dE5fG",
      "createdByName": "John Doe",
      "updatedById": "aB3dE5fG",
      "updatedByName": "John Doe",
      "createdAt": "2026-02-09T10:30:00",
      "updatedAt": "2026-02-09T10:30:00"
    },
    {
      "id": "aB3dE5fG",
      "title": "Experience Serengeti",
      "subtitle": "The Great Migration Awaits",
      "description": "Witness one of nature's most spectacular events",
      "page": "HOME",
      "pageDisplayName": "Home",
      "ctaText": "Learn More",
      "ctaLink": "/parks/serengeti",
      "displayOrder": 2,
      "isActive": true,
      "overlayColor": "#1a1a1a",
      "overlayOpacity": 0.3,
      "textAlignment": "left",
      "cssClasses": "hero-slide-in",
      "primaryImageUrl": "http://localhost:4450/api/hero-images/zX9wY8vU/file",
      "imageCount": 5,
      "createdById": "aB3dE5fG",
      "createdByName": "John Doe",
      "updatedById": "aB3dE5fG",
      "updatedByName": "John Doe",
      "createdAt": "2026-02-09T11:00:00",
      "updatedAt": "2026-02-09T11:00:00"
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Invalid page parameter
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/heroes/page/HOME"
```

---

### 6. List All Heroes (with Filtering & Pagination)

Retrieve all heroes with advanced filtering, pagination, and sorting capabilities.

**Endpoint:** `GET /api/heroes`

**Permission:** `PERM_READ_HERO`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| title | String | null | Filter by title (partial match) |
| page | HeroPage | null | Filter by page (HOME, ABOUT, etc.) |
| isActive | Boolean | null | Filter by active status |
| textAlignment | String | null | Filter by text alignment (left, center, right) |
| createdById | String | null | Filter by creator (obfuscated user ID) |
| updatedById | String | null | Filter by updater (obfuscated user ID) |
| pageNumber | Integer | 0 | Page number (0-indexed) |
| pageSize | Integer | 20 | Number of items per page |
| sortBy | String | displayOrder | Field to sort by (title, displayOrder, page, createdAt, updatedAt) |
| sortDirection | String | asc | Sort direction (asc or desc) |

**Example Request:**
```
GET /api/heroes?page=HOME&isActive=true&pageNumber=0&pageSize=10&sortBy=displayOrder&sortDirection=asc
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Heroes retrieved successfully",
  "data": {
    "content": [
      {
        "id": "xY8kL3mP",
        "title": "Explore Tanzania's Wildlife",
        "subtitle": "Unforgettable Safari Adventures",
        "description": "Discover the beauty of East Africa",
        "page": "HOME",
        "pageDisplayName": "Home",
        "ctaText": "Book Your Safari",
        "ctaLink": "/safaris",
        "displayOrder": 1,
        "isActive": true,
        "overlayColor": "#000000",
        "overlayOpacity": 0.4,
        "textAlignment": "center",
        "cssClasses": "hero-fade-in",
        "primaryImageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
        "imageCount": 3,
        "createdById": "aB3dE5fG",
        "createdByName": "John Doe",
        "updatedById": "aB3dE5fG",
        "updatedByName": "John Doe",
        "createdAt": "2026-02-09T10:30:00",
        "updatedAt": "2026-02-09T10:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "numberOfElements": 1,
    "first": true,
    "empty": false
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid query parameters
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_READ_HERO permission
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/heroes?page=HOME&isActive=true&pageNumber=0&pageSize=10&sortBy=displayOrder&sortDirection=asc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 7. Reorder Heroes (Drag & Drop)

Reorder heroes within a page. This endpoint supports drag-and-drop functionality by updating the displayOrder of multiple heroes atomically.

**Endpoint:** `POST /api/heroes/reorder`

**Permission:** `PERM_UPDATE_HERO`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "page": "HOME",
  "heroOrder": [
    {
      "heroId": "aB3dE5fG",
      "expectedDisplayOrder": 1
    },
    {
      "heroId": "xY8kL3mP",
      "expectedDisplayOrder": 2
    },
    {
      "heroId": "zX9wY8vU",
      "expectedDisplayOrder": 3
    }
  ]
}
```

**ReorderHeroDTO Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| page | HeroPage | Yes | Page where heroes are being reordered |
| heroOrder | Array | Yes | Array of hero order items |
| heroOrder[].heroId | String | Yes | Obfuscated hero ID |
| heroOrder[].expectedDisplayOrder | Integer | No | Expected current display order (for validation) |

**Validation Rules:**

1. **Count Match:** Number of heroes in request must match number of heroes on the page
2. **No Duplicates:** No duplicate hero IDs in the list
3. **All Belong to Page:** All heroes must belong to the specified page
4. **No Missing Heroes:** All heroes on the page must be included in the reorder list
5. **Expected Order Validation:** If expectedDisplayOrder is provided, it must match current order

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Heroes reordered successfully",
  "data": [
    {
      "id": "aB3dE5fG",
      "title": "Experience Serengeti",
      "displayOrder": 1,
      "page": "HOME",
      "pageDisplayName": "Home",
      "isActive": true,
      "primaryImageUrl": "http://localhost:4450/api/hero-images/zX9wY8vU/file",
      "imageCount": 5
    },
    {
      "id": "xY8kL3mP",
      "title": "Explore Tanzania's Wildlife",
      "displayOrder": 2,
      "page": "HOME",
      "pageDisplayName": "Home",
      "isActive": true,
      "primaryImageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
      "imageCount": 3
    },
    {
      "id": "zX9wY8vU",
      "title": "Visit Ngorongoro Crater",
      "displayOrder": 3,
      "page": "HOME",
      "pageDisplayName": "Home",
      "isActive": true,
      "primaryImageUrl": "http://localhost:4450/api/hero-images/qW2eR4tY/file",
      "imageCount": 2
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Validation error:
  - Hero count mismatch
  - Duplicate hero IDs
  - Invalid hero IDs
  - Heroes not belonging to page
  - Missing heroes from page
  - Expected order mismatch
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_UPDATE_HERO permission
- `500 Internal Server Error` - Server error

**Error Example:**
```json
{
  "status": 400,
  "message": "Hero count mismatch: expected 5 heroes but received 3",
  "errorCode": "HERO_COUNT_MISMATCH"
}
```

**Usage Example:**
```bash
curl -X POST "http://localhost:4450/api/heroes/reorder" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "page": "HOME",
    "heroOrder": [
      {"heroId": "aB3dE5fG", "expectedDisplayOrder": 1},
      {"heroId": "xY8kL3mP", "expectedDisplayOrder": 2}
    ]
  }'
```

---

## Data Models

### HeroDTO

Response DTO for hero data.

```typescript
{
  id: string;                    // Obfuscated ID
  title: string;                 // Hero title
  subtitle?: string;             // Hero subtitle
  description?: string;          // Detailed description
  page: HeroPage;                // Page enum
  pageDisplayName: string;       // Human-readable page name
  ctaText?: string;              // Call-to-action button text
  ctaLink?: string;              // Call-to-action button URL
  displayOrder: number;          // Display order
  isActive: boolean;             // Active status
  overlayColor?: string;         // Overlay color (hex)
  overlayOpacity?: number;       // Overlay opacity (0.0-1.0)
  textAlignment?: string;        // Text alignment
  cssClasses?: string;           // Custom CSS classes
  primaryImageUrl?: string;      // URL of primary image
  imageCount: number;            // Number of images
  createdById: string;           // Creator obfuscated ID
  createdByName: string;         // Creator display name
  updatedById: string;           // Updater obfuscated ID
  updatedByName: string;         // Updater display name
  createdAt: string;             // ISO 8601 datetime
  updatedAt: string;             // ISO 8601 datetime
}
```

### HeroPage Enum

Valid page values:

- `HOME` - Home page
- `ABOUT` - About page
- `SAFARIS` - Safaris page
- `ACCOMMODATIONS` - Accommodations page
- `PARKS` - Parks page
- `ACTIVITIES` - Activities page
- `CONTACT` - Contact page
- `BLOG` - Blog page

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| HERO_NOT_FOUND | 404 | Hero with specified ID not found |
| HERO_COUNT_MISMATCH | 400 | Reorder count doesn't match page hero count |
| DUPLICATE_HERO_IDS | 400 | Duplicate hero IDs in reorder request |
| INVALID_HERO_ID | 400 | One or more hero IDs are invalid |
| HERO_NOT_ON_PAGE | 400 | Hero doesn't belong to specified page |
| MISSING_HEROES | 400 | Some heroes from page are missing in reorder |
| EXPECTED_ORDER_MISMATCH | 400 | Expected display order doesn't match current |
| VALIDATION_ERROR | 400 | Request validation failed |
| UNAUTHORIZED | 401 | Missing or invalid authentication token |
| FORBIDDEN | 403 | User lacks required permission |
| INTERNAL_ERROR | 500 | Unexpected server error |

---

## Usage Examples

### Example 1: Create and Display Heroes on Home Page

```bash
# 1. Create first hero
curl -X POST "http://localhost:4450/api/heroes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Explore Tanzania",
    "subtitle": "Safari Adventures Await",
    "page": "HOME",
    "ctaText": "View Safaris",
    "ctaLink": "/safaris",
    "isActive": true,
    "overlayOpacity": 0.4,
    "textAlignment": "center"
  }'

# 2. Create second hero
curl -X POST "http://localhost:4450/api/heroes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Visit Serengeti",
    "subtitle": "Witness The Great Migration",
    "page": "HOME",
    "ctaText": "Learn More",
    "ctaLink": "/parks/serengeti",
    "isActive": true
  }'

# 3. Get all heroes for home page (public - no auth needed)
curl -X GET "http://localhost:4450/api/heroes/page/HOME"
```

### Example 2: Update and Reorder Heroes

```bash
# 1. Update hero title
curl -X PUT "http://localhost:4450/api/heroes/xY8kL3mP" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Discover Tanzania Wildlife"
  }'

# 2. Reorder heroes (swap order)
curl -X POST "http://localhost:4450/api/heroes/reorder" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "page": "HOME",
    "heroOrder": [
      {"heroId": "aB3dE5fG"},
      {"heroId": "xY8kL3mP"}
    ]
  }'
```

### Example 3: Search and Filter Heroes

```bash
# 1. Get all active heroes on HOME page
curl -X GET "http://localhost:4450/api/heroes?page=HOME&isActive=true" \
  -H "Authorization: Bearer $TOKEN"

# 2. Search heroes by title
curl -X GET "http://localhost:4450/api/heroes?title=Tanzania" \
  -H "Authorization: Bearer $TOKEN"

# 3. Get heroes with pagination and sorting
curl -X GET "http://localhost:4450/api/heroes?pageNumber=0&pageSize=5&sortBy=createdAt&sortDirection=desc" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Best Practices

1. **Display Order:** Always specify displayOrder when creating heroes to control order explicitly
2. **Reordering:** Use the reorder endpoint instead of updating individual heroes for better consistency
3. **Public Access:** Use `/api/heroes/page/{page}` endpoint for front-end website (no auth required)
4. **Filtering:** Use the list endpoint with filters for admin interfaces
5. **Validation:** Always provide expectedDisplayOrder when reordering to prevent race conditions
6. **Active Status:** Set isActive=false to hide heroes without deleting them
7. **Image URLs:** Use primaryImageUrl from response to display hero background images

---

## Changelog

**Version 1.0.0** (2026-02-09)
- Initial API documentation
- 7 endpoints: Create, Update, Delete, Get by ID, Get by Page, List, Reorder
- Support for multi-image hero sections
- Drag & drop reordering with validation
- Public access for page-specific hero retrieval
