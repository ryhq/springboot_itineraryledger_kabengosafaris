# Testimony Management API Documentation

## Overview

The Testimony Management API provides comprehensive endpoints for managing customer testimonials and reviews in the Kabengo Safaris tourism platform. Testimonies can be linked to specific customers and safaris, support multiple review sources, and include approval/moderation workflows.

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format.

---

## Table of Contents

1. [Testimony API](#testimony-api)
   - [Create Testimony](#1-create-testimony)
   - [Get Testimony by ID](#2-get-testimony-by-id)
   - [List Testimonies](#3-list-testimonies)
   - [Get Public Testimonies](#4-get-public-testimonies)
   - [Get Featured Testimonies](#5-get-featured-testimonies)
   - [Update Testimony](#6-update-testimony)
   - [Approve/Unapprove Testimony](#7-approveunapprove-testimony)
   - [Respond to Testimony](#8-respond-to-testimony)
   - [Delete Testimonies](#9-delete-testimonies)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)

---

## Testimony API

Base URL: `/api/testimonies`

### 1. Create Testimony

Creates a new testimony/review.

**Endpoint:** `POST /api/testimonies`

**Permission Required:** `PERM_CREATE_TESTIMONY`

**Request Body:**
```json
{
  "authorName": "John Smith",
  "authorTitle": "Travel Blogger",
  "authorCountry": "United States",
  "message": "Absolutely incredible safari experience!",
  "rating": 5,
  "source": "GOOGLE",
  "reviewDate": "2026-02-15",
  "isVerifiedBooking": true,
  "isApproved": false,
  "isFeatured": false,
  "isActive": true,
  "displayOrder": 0,
  "sentimentTags": "excellent,wildlife,guides",
  "customerId": "abc123xyz",
  "safariId": "def456ghi"
}
```

**Required Fields:**

| Field | Type | Validation |
|-------|------|------------|
| `authorName` | string | Non-blank |
| `message` | string | Non-blank |
| `rating` | integer | 1-5 |
| `source` | enum | See [Testimony Sources](#testimony-sources) |

**Optional Fields:**

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `authorTitle` | string | null | Author's title or role |
| `authorCountry` | string | null | Author's country |
| `reviewDate` | date | null | Original review date (YYYY-MM-DD format) |
| `isVerifiedBooking` | boolean | false | Whether linked to a verified booking |
| `isApproved` | boolean | false | Whether approved for public display |
| `isFeatured` | boolean | false | Whether featured on homepage |
| `isActive` | boolean | true | Whether active |
| `displayOrder` | integer | 0 | Display ordering position |
| `sentimentTags` | string | null | Comma-separated sentiment tags |
| `customerId` | string | null | Obfuscated customer ID (link to customer) |
| `safariId` | string | null | Obfuscated safari ID (link to safari) |

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Testimony created successfully",
  "data": {
    "id": "xyz789abc",
    "authorName": "John Smith",
    "authorTitle": "Travel Blogger",
    "authorCountry": "United States",
    "message": "Absolutely incredible safari experience!",
    "rating": 5,
    "adminResponse": null,
    "adminResponseDate": null,
    "reviewDate": "2026-02-15",
    "source": "GOOGLE",
    "sourceDisplayName": "Google",
    "isVerifiedBooking": true,
    "isApproved": false,
    "isFeatured": false,
    "isActive": true,
    "displayOrder": 0,
    "sentimentTags": "excellent,wildlife,guides",
    "customerId": "abc123xyz",
    "customerName": "John Smith",
    "safariId": "def456ghi",
    "safariName": "7-Day Serengeti Adventure",
    "primaryImageUrl": null,
    "imageCount": 0,
    "createdById": "usr123abc",
    "createdByName": "Admin User",
    "updatedById": null,
    "updatedByName": null,
    "createdAt": "2026-03-10T10:30:00",
    "updatedAt": "2026-03-10T10:30:00"
  },
  "timestamp": "2026-03-10T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Validation failed
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Author name is required",
    "errorCode": "VALIDATION_ERROR",
    "timestamp": "2026-03-10T10:30:00"
  }
  ```

**Notes:**
- `customerId` and `safariId` are optional associations; the customer and safari must exist if provided
- `source` determines the `sourceDisplayName` automatically
- New testimonies default to unapproved — use the approve endpoint or set `isApproved: true` at creation

---

### 2. Get Testimony by ID

Retrieves a single testimony by its obfuscated ID, including circular navigation.

**Endpoint:** `GET /api/testimonies/{idObfuscated}`

**Permission Required:** `PERM_READ_TESTIMONY`

**Path Parameters:**
- `idObfuscated` (string, required): Obfuscated testimony ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony retrieved successfully",
  "data": {
    "testimony": {
      "id": "xyz789abc",
      "authorName": "John Smith",
      "authorTitle": "Travel Blogger",
      "authorCountry": "United States",
      "message": "Absolutely incredible safari experience!",
      "rating": 5,
      "adminResponse": "Thank you for your kind words!",
      "adminResponseDate": "2026-03-11T09:00:00",
      "reviewDate": "2026-02-15",
      "source": "GOOGLE",
      "sourceDisplayName": "Google",
      "isVerifiedBooking": true,
      "isApproved": true,
      "isFeatured": true,
      "isActive": true,
      "displayOrder": 1,
      "sentimentTags": "excellent,wildlife,guides",
      "customerId": "abc123xyz",
      "customerName": "John Smith",
      "safariId": "def456ghi",
      "safariName": "7-Day Serengeti Adventure",
      "primaryImageUrl": "http://localhost:4450/api/testimony-images/img123/file",
      "imageCount": 3,
      "createdById": "usr123abc",
      "createdByName": "Admin User",
      "updatedById": "usr123abc",
      "updatedByName": "Admin User",
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-11T09:00:00"
    },
    "nextId": "abc456def",
    "previousId": "ghi789jkl"
  },
  "timestamp": "2026-03-10T12:00:00"
}
```

**Navigation:**
- `nextId` / `previousId` provide circular navigation — wraps from last to first and vice versa
- Both are obfuscated IDs

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Testimony not found

---

### 3. List Testimonies

Retrieves all testimonies with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/testimonies`

**Permission Required:** `PERM_READ_TESTIMONY`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `pageNumber` | integer | No | 0 | Page number (0-indexed) |
| `pageSize` | integer | No | 20 | Page size |
| `sortBy` | string | No | createdAt | Sort field (see valid fields below) |
| `sortDirection` | string | No | desc | Sort direction (`asc` or `desc`) |
| `authorName` | string | No | - | Filter by author name (partial match, case-insensitive) |
| `source` | enum | No | - | Filter by source (WEBSITE, GOOGLE, TRIPADVISOR, FACEBOOK, EMAIL, OTHER) |
| `rating` | integer | No | - | Filter by exact rating (1-5) |
| `minRating` | integer | No | - | Filter by minimum rating |
| `maxRating` | integer | No | - | Filter by maximum rating |
| `isApproved` | boolean | No | - | Filter by approval status |
| `isFeatured` | boolean | No | - | Filter by featured status |
| `isVerifiedBooking` | boolean | No | - | Filter by verified booking status |
| `isActive` | boolean | No | - | Filter by active status |
| `sentimentTag` | string | No | - | Filter by sentiment tag |
| `customerId` | string | No | - | Filter by customer ID (obfuscated) |
| `safariId` | string | No | - | Filter by safari ID (obfuscated) |
| `keyword` | string | No | - | Search across text fields |

**Valid Sort Fields:**
`authorName`, `rating`, `source`, `reviewDate`, `isApproved`, `isFeatured`, `isVerifiedBooking`, `isActive`, `displayOrder`, `createdAt`, `updatedAt`

**Example Requests:**

1. Get all testimonies (default pagination):
   ```
   GET /api/testimonies
   ```

2. Get approved, featured testimonies sorted by rating:
   ```
   GET /api/testimonies?isApproved=true&isFeatured=true&sortBy=rating&sortDirection=desc
   ```

3. Get 5-star Google reviews:
   ```
   GET /api/testimonies?rating=5&source=GOOGLE
   ```

4. Get testimonies with rating between 3 and 5:
   ```
   GET /api/testimonies?minRating=3&maxRating=5
   ```

5. Search by keyword:
   ```
   GET /api/testimonies?keyword=serengeti
   ```

6. Filter by customer:
   ```
   GET /api/testimonies?customerId=abc123xyz
   ```

7. Complex filter with pagination:
   ```
   GET /api/testimonies?source=TRIPADVISOR&minRating=4&isApproved=true&isActive=true&pageSize=10&sortBy=rating&sortDirection=desc
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimonies retrieved successfully",
  "data": {
    "testimonies": [
      {
        "id": "xyz789abc",
        "authorName": "John Smith",
        "authorTitle": "Travel Blogger",
        "authorCountry": "United States",
        "message": "Absolutely incredible safari experience!",
        "rating": 5,
        "source": "GOOGLE",
        "sourceDisplayName": "Google",
        "isApproved": true,
        "isFeatured": true,
        "isVerifiedBooking": true,
        "isActive": true,
        "displayOrder": 1,
        "reviewDate": "2026-02-15",
        "customerName": "John Smith",
        "safariName": "7-Day Serengeti Adventure",
        "primaryImageUrl": "http://localhost:4450/api/testimony-images/img123/file",
        "imageCount": 3,
        "createdAt": "2026-03-10T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 2,
    "pageSize": 20,
    "validSortFields": ["authorName", "rating", "source", "reviewDate", "isApproved", "isFeatured", "isVerifiedBooking", "isActive", "displayOrder", "createdAt", "updatedAt"],
    "currentSortBy": "createdAt",
    "currentSortDirection": "desc"
  },
  "timestamp": "2026-03-10T12:00:00"
}
```

**Notes:**
- List endpoint returns `TestimonyListItemDTO` (lighter than full `TestimonyDTO`) — excludes `adminResponse`, `adminResponseDate`, `sentimentTags`, audit fields
- All filters are optional and can be combined
- Invalid sort field returns 400 error with list of valid fields

---

### 4. Get Public Testimonies

Retrieves approved and active testimonies for public website display.

**Endpoint:** `GET /api/testimonies/public`

**Permission Required:** None (public endpoint)

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Public testimonies retrieved successfully",
  "data": [
    {
      "id": "xyz789abc",
      "authorName": "John Smith",
      "authorTitle": "Travel Blogger",
      "authorCountry": "United States",
      "message": "Absolutely incredible safari experience!",
      "rating": 5,
      "adminResponse": "Thank you!",
      "adminResponseDate": "2026-03-11T09:00:00",
      "reviewDate": "2026-02-15",
      "source": "GOOGLE",
      "sourceDisplayName": "Google",
      "isVerifiedBooking": true,
      "isApproved": true,
      "isFeatured": false,
      "isActive": true,
      "displayOrder": 1,
      "sentimentTags": "excellent,wildlife",
      "primaryImageUrl": "http://localhost:4450/api/testimony-images/img123/file",
      "imageCount": 2,
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-10T10:30:00"
    }
  ],
  "timestamp": "2026-03-10T12:00:00"
}
```

**Notes:**
- Returns only testimonies where `isApproved = true` AND `isActive = true`
- Ordered by `displayOrder` ascending
- No authentication required

---

### 5. Get Featured Testimonies

Retrieves featured, approved, and active testimonies for homepage display.

**Endpoint:** `GET /api/testimonies/featured`

**Permission Required:** None (public endpoint)

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Featured testimonies retrieved successfully",
  "data": [
    {
      "id": "xyz789abc",
      "authorName": "Maria Garcia",
      "authorTitle": "Travel Enthusiast",
      "authorCountry": "Spain",
      "message": "A dream come true!",
      "rating": 5,
      "source": "TRIPADVISOR",
      "sourceDisplayName": "TripAdvisor",
      "isVerifiedBooking": true,
      "isApproved": true,
      "isFeatured": true,
      "isActive": true,
      "displayOrder": 1,
      "primaryImageUrl": "http://localhost:4450/api/testimony-images/img456/file",
      "imageCount": 1,
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-10T10:30:00"
    }
  ],
  "timestamp": "2026-03-10T12:00:00"
}
```

**Notes:**
- Returns only testimonies where `isFeatured = true` AND `isApproved = true` AND `isActive = true`
- Ordered by `displayOrder` ascending
- No authentication required

---

### 6. Update Testimony

Updates an existing testimony. All fields are optional (partial update).

**Endpoint:** `PUT /api/testimonies/{idObfuscated}`

**Permission Required:** `PERM_UPDATE_TESTIMONY`

**Path Parameters:**
- `idObfuscated` (string, required): Obfuscated testimony ID

**Request Body:**
```json
{
  "authorName": "John Smith Updated",
  "authorTitle": "Senior Travel Blogger",
  "authorCountry": "United Kingdom",
  "message": "Updated review after second visit!",
  "rating": 5,
  "source": "WEBSITE",
  "reviewDate": "2026-03-01",
  "isVerifiedBooking": true,
  "isApproved": true,
  "isFeatured": true,
  "isActive": true,
  "displayOrder": 1,
  "sentimentTags": "repeat-visit,excellent",
  "customerId": "abc123xyz",
  "safariId": "def456ghi"
}
```

**All Fields Are Optional:**

| Field | Type | Validation |
|-------|------|------------|
| `authorName` | string | - |
| `authorTitle` | string | - |
| `authorCountry` | string | - |
| `message` | string | - |
| `rating` | integer | 1-5 |
| `source` | enum | See [Testimony Sources](#testimony-sources) |
| `reviewDate` | date | YYYY-MM-DD format |
| `isVerifiedBooking` | boolean | - |
| `isApproved` | boolean | - |
| `isFeatured` | boolean | - |
| `isActive` | boolean | - |
| `displayOrder` | integer | - |
| `sentimentTags` | string | - |
| `customerId` | string | Obfuscated ID |
| `safariId` | string | Obfuscated ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony updated successfully",
  "data": { ... },
  "timestamp": "2026-03-10T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format or validation error
- **404 Not Found** - Testimony not found

**Notes:**
- Only provided fields will be updated; omitted fields remain unchanged
- To approve via update, set `isApproved: true` (or use the dedicated approve endpoint)

---

### 7. Approve/Unapprove Testimony

Quick endpoint to toggle the approval status of a testimony.

**Endpoint:** `PUT /api/testimonies/{idObfuscated}/approve`

**Permission Required:** `PERM_UPDATE_TESTIMONY`

**Path Parameters:**
- `idObfuscated` (string, required): Obfuscated testimony ID

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `approved` | boolean | No | true | Set to `false` to unapprove |

**Example Requests:**

1. Approve a testimony:
   ```
   PUT /api/testimonies/xyz789abc/approve
   ```

2. Unapprove a testimony:
   ```
   PUT /api/testimonies/xyz789abc/approve?approved=false
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony approved successfully",
  "data": { ... },
  "timestamp": "2026-03-10T14:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Testimony not found

---

### 8. Respond to Testimony

Add an admin response to a testimony (visible to public when testimony is displayed).

**Endpoint:** `PUT /api/testimonies/{idObfuscated}/respond`

**Permission Required:** `PERM_UPDATE_TESTIMONY`

**Path Parameters:**
- `idObfuscated` (string, required): Obfuscated testimony ID

**Request Body:**
```json
{
  "adminResponse": "Thank you for your wonderful review! We are thrilled you enjoyed your safari experience with us."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Admin response added successfully",
  "data": {
    "id": "xyz789abc",
    "adminResponse": "Thank you for your wonderful review!...",
    "adminResponseDate": "2026-03-10T15:00:00",
    ...
  },
  "timestamp": "2026-03-10T15:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid ID format
- **404 Not Found** - Testimony not found

**Notes:**
- Sets `adminResponse` text and `adminResponseDate` timestamp
- To clear an admin response, send an empty string or null

---

### 9. Delete Testimonies

Bulk deletes multiple testimonies by their IDs.

**Endpoint:** `DELETE /api/testimonies`

**Permission Required:** `PERM_DELETE_TESTIMONY`

**Request Body:**
```json
["xyz789abc", "def456ghi", "jkl123mno"]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 testimonies deleted successfully",
  "data": null,
  "timestamp": "2026-03-10T16:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Empty ID list or invalid IDs
- **500 Internal Server Error** - Deletion failed

**Notes:**
- Permanently deletes testimonies and their associated images from database and filesystem
- Request body is a JSON array of obfuscated testimony IDs

---

## Data Models

### Testimony Sources

| Value | Display Name | Description |
|-------|--------------|-------------|
| `WEBSITE` | Website | Review from company website |
| `GOOGLE` | Google | Google Maps/Business review |
| `TRIPADVISOR` | TripAdvisor | TripAdvisor review |
| `FACEBOOK` | Facebook | Facebook review |
| `EMAIL` | Email | Review received via email |
| `OTHER` | Other | Other review source |

### TestimonyDTO (Full Response)

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated testimony ID |
| `authorName` | string | Author's name |
| `authorTitle` | string | Author's title or role |
| `authorCountry` | string | Author's country |
| `message` | string | Testimony content |
| `rating` | integer | Rating (1-5) |
| `adminResponse` | string | Admin's response to the testimony |
| `adminResponseDate` | datetime | When admin responded |
| `reviewDate` | date | Original review date |
| `source` | enum | Source enum value |
| `sourceDisplayName` | string | Human-readable source name |
| `isVerifiedBooking` | boolean | Whether linked to a verified booking |
| `isApproved` | boolean | Whether approved for public display |
| `isFeatured` | boolean | Whether featured on homepage |
| `isActive` | boolean | Whether active |
| `displayOrder` | integer | Display ordering position |
| `sentimentTags` | string | Comma-separated sentiment tags |
| `customerId` | string | Obfuscated customer ID (if linked) |
| `customerName` | string | Customer name (if linked) |
| `safariId` | string | Obfuscated safari ID (if linked) |
| `safariName` | string | Safari name (if linked) |
| `primaryImageUrl` | string | URL of the primary image |
| `imageCount` | long | Total number of images |
| `createdById` | string | Obfuscated ID of the creator |
| `createdByName` | string | Name of the creator |
| `updatedById` | string | Obfuscated ID of last updater |
| `updatedByName` | string | Name of last updater |
| `createdAt` | datetime | Creation timestamp |
| `updatedAt` | datetime | Last update timestamp |

### TestimonyListItemDTO (List Response)

A lighter DTO used in the paginated list endpoint. Excludes `adminResponse`, `adminResponseDate`, `sentimentTags`, and audit fields (`createdById`, `createdByName`, `updatedById`, `updatedByName`, `updatedAt`).

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated testimony ID |
| `authorName` | string | Author's name |
| `authorTitle` | string | Author's title or role |
| `authorCountry` | string | Author's country |
| `message` | string | Testimony content |
| `rating` | integer | Rating (1-5) |
| `source` | enum | Source enum value |
| `sourceDisplayName` | string | Human-readable source name |
| `isApproved` | boolean | Approval status |
| `isFeatured` | boolean | Featured status |
| `isVerifiedBooking` | boolean | Verified booking status |
| `isActive` | boolean | Active status |
| `displayOrder` | integer | Display ordering |
| `reviewDate` | date | Original review date |
| `customerName` | string | Customer name (if linked) |
| `safariName` | string | Safari name (if linked) |
| `primaryImageUrl` | string | URL of the primary image |
| `imageCount` | long | Total number of images |
| `createdAt` | datetime | Creation timestamp |

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `VALIDATION_ERROR` | Request body failed validation |
| `INVALID_TESTIMONY_ID` | The provided testimony ID is invalid or malformed |
| `TESTIMONY_NOT_FOUND` | Testimony with the specified ID does not exist |
| `TESTIMONY_CREATE_FAILED` | Failed to create testimony due to server error |
| `TESTIMONY_UPDATE_FAILED` | Failed to update testimony due to server error |
| `TESTIMONY_FETCH_FAILED` | Failed to fetch testimony due to server error |
| `TESTIMONY_DELETE_FAILED` | Failed to delete testimony due to server error |
| `INVALID_SORT_FIELD` | Invalid sort field provided (includes valid fields in message) |
| `INVALID_CUSTOMER_ID` | The provided customer ID is invalid |
| `CUSTOMER_NOT_FOUND` | Customer with the specified ID does not exist |
| `INVALID_SAFARI_ID` | The provided safari ID is invalid |
| `SAFARI_NOT_FOUND` | Safari with the specified ID does not exist |

---

## Examples

### Example 1: Create and Approve a Testimony

**Step 1 — Create:**
```http
POST /api/testimonies
Authorization: Bearer <token>
Content-Type: application/json

{
  "authorName": "Emma Wilson",
  "authorTitle": "Wildlife Photographer",
  "authorCountry": "Australia",
  "message": "The most breathtaking wildlife experience of my life. Our guide knew exactly where to find the Big Five, and the accommodations were world-class.",
  "rating": 5,
  "source": "TRIPADVISOR",
  "reviewDate": "2026-02-28",
  "isVerifiedBooking": true,
  "sentimentTags": "wildlife,photography,guides,accommodations"
}
```

**Step 2 — Approve:**
```http
PUT /api/testimonies/xyz789abc/approve
Authorization: Bearer <token>
```

**Step 3 — Add Admin Response:**
```http
PUT /api/testimonies/xyz789abc/respond
Authorization: Bearer <token>
Content-Type: application/json

{
  "adminResponse": "Thank you, Emma! We're so glad our guides could help you capture amazing moments. Your photos were stunning!"
}
```

### Example 2: Feature a Testimony on Homepage

```http
PUT /api/testimonies/xyz789abc
Authorization: Bearer <token>
Content-Type: application/json

{
  "isFeatured": true,
  "displayOrder": 1
}
```

### Example 3: Moderate Testimonies

Get all pending (unapproved) testimonies:
```http
GET /api/testimonies?isApproved=false&isActive=true&sortBy=createdAt&sortDirection=desc
Authorization: Bearer <token>
```

### Example 4: Get Reviews by Source

Get all TripAdvisor reviews with 4+ stars:
```http
GET /api/testimonies?source=TRIPADVISOR&minRating=4&isApproved=true&sortBy=rating&sortDirection=desc
Authorization: Bearer <token>
```

### Example 5: Public Website Integration

Fetch testimonies for the homepage (no auth needed):
```http
GET /api/testimonies/featured
```

Fetch all approved testimonies for the testimonials page (no auth needed):
```http
GET /api/testimonies/public
```

---

## Best Practices

### 1. Moderation Workflow

- New testimonies should default to `isApproved: false`
- Review pending testimonies via `GET /api/testimonies?isApproved=false`
- Approve individually via `PUT /api/testimonies/{id}/approve`
- Add admin responses for engagement

### 2. Display Ordering

- Use `displayOrder` to control the order testimonies appear on the website
- Lower numbers appear first (ascending order)
- Featured testimonies should have explicit display orders

### 3. Filtering

- Combine multiple filters for precise results
- Use `keyword` for broad text search across all text fields
- Use `minRating`/`maxRating` for rating ranges instead of exact `rating`

### 4. Public vs Admin Endpoints

- `/public` and `/featured` endpoints require no authentication
- All other endpoints require JWT token with appropriate permissions
- List endpoint returns lighter DTOs for performance

---

## Changelog

### Version 1.0.0
- Initial API documentation
- Complete CRUD operations for testimonies
- Public and featured endpoints
- Approval and admin response workflows
- Specification-based filtering with 14 filter parameters

---

## Support

For technical support or questions about the Testimony API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
