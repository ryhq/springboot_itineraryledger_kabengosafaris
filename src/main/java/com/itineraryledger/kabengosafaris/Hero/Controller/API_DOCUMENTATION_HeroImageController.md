# Hero Image Management API Documentation

## Overview

The Hero Image Management API provides comprehensive endpoints for managing images within hero sections. Each hero can have multiple images for carousel/slideshow display. Images are stored with SHA-256 hashing for security and support public access for front-end display.

**Base URL:** `/api/hero-images`

**Controller:** `HeroImageController.java`

**Tag:** Hero Image Management

---

## Authentication & Authorization

Most endpoints require JWT authentication, except public image serving endpoints.

### Required Permissions

| Permission | Description |
|------------|-------------|
| `PERM_CREATE_HERO_IMAGE` | Required to upload images |
| `PERM_READ_HERO_IMAGE` | Required to read/list images |
| `PERM_UPDATE_HERO_IMAGE` | Required to update or reorder images |
| `PERM_DELETE_HERO_IMAGE` | Required to delete images |

### Public Endpoints (No Authentication Required)

- `GET /api/hero-images/file/{fileName}` - Serve image by filename
- `GET /api/hero-images/{id}/file` - Serve image by ID

These endpoints are public to allow front-end website display without authentication.

---

## Endpoints

### 1. List All Images (with Filtering & Pagination)

Retrieve all hero images with advanced filtering, pagination, and sorting capabilities.

**Endpoint:** `GET /api/hero-images`

**Permission:** `PERM_READ_HERO_IMAGE`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| heroId | String | null | Filter by hero ID (obfuscated) |
| heroTitle | String | null | Filter by hero title (partial match) |
| heroPage | HeroPage | null | Filter by hero page (HOME, ABOUT, etc.) |
| isPrimary | Boolean | null | Filter by primary status |
| isActive | Boolean | null | Filter by active status |
| displayOrder | Integer | null | Filter by display order |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 20 | Number of items per page |
| sortDirection | String | desc | Sort direction (asc or desc) |

**Example Request:**
```
GET /api/hero-images?heroPage=HOME&isActive=true&page=0&size=10&sortDirection=desc
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero images retrieved successfully",
  "data": {
    "images": [
      {
        "id": "aB3dE5fG",
        "heroId": "xY8kL3mP",
        "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
        "originalFileName": "serengeti-sunset.jpg",
        "altText": "Beautiful sunset over Serengeti plains",
        "caption": "Golden Hour in Serengeti",
        "description": "A stunning sunset captured during the dry season",
        "isPrimary": true,
        "isActive": true,
        "displayOrder": 1,
        "fileSize": 2457600,
        "fileSizeFormatted": "2.34 MB",
        "mimeType": "image/jpeg",
        "width": 1920,
        "height": 1080,
        "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
        "fileImageUrl": "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
        "createdAt": "2026-02-09T10:45:00",
        "updatedAt": "2026-02-09T10:45:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 25,
    "totalPages": 3,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid query parameters
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_READ_HERO_IMAGE permission
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/hero-images?heroPage=HOME&isActive=true&page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 2. Get Image by ID

Retrieve a single hero image by its obfuscated ID (metadata only, not the actual image file).

**Endpoint:** `GET /api/hero-images/{id}`

**Permission:** `PERM_READ_HERO_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero image retrieved successfully",
  "data": {
    "id": "aB3dE5fG",
    "heroId": "xY8kL3mP",
    "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
    "originalFileName": "serengeti-sunset.jpg",
    "altText": "Beautiful sunset over Serengeti plains",
    "caption": "Golden Hour in Serengeti",
    "description": "A stunning sunset captured during the dry season",
    "isPrimary": true,
    "isActive": true,
    "displayOrder": 1,
    "fileSize": 2457600,
    "fileSizeFormatted": "2.34 MB",
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
    "fileImageUrl": "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
    "createdAt": "2026-02-09T10:45:00",
    "updatedAt": "2026-02-09T10:45:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid image ID format
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_READ_HERO_IMAGE permission
- `404 Not Found` - Image not found
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/hero-images/aB3dE5fG" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3. Get Images for Hero

Retrieve all images associated with a hero section, ordered by displayOrder.

**Endpoint:** `GET /api/hero-images/hero/{heroId}`

**Permission:** `PERM_READ_HERO_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| heroId | String | Obfuscated hero ID |

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero images retrieved successfully",
  "data": [
    {
      "id": "aB3dE5fG",
      "heroId": "xY8kL3mP",
      "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
      "originalFileName": "serengeti-sunset.jpg",
      "altText": "Beautiful sunset over Serengeti plains",
      "caption": "Golden Hour in Serengeti",
      "description": "A stunning sunset captured during the dry season",
      "isPrimary": true,
      "isActive": true,
      "displayOrder": 1,
      "fileSize": 2457600,
      "fileSizeFormatted": "2.34 MB",
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1080,
      "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
      "fileImageUrl": "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
      "createdAt": "2026-02-09T10:45:00",
      "updatedAt": "2026-02-09T10:45:00"
    },
    {
      "id": "qW2eR4tY",
      "heroId": "xY8kL3mP",
      "fileName": "3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c.jpg",
      "originalFileName": "wildlife-elephants.jpg",
      "altText": "Elephant herd crossing the plains",
      "caption": "Majestic Giants",
      "description": "A family of elephants during migration",
      "isPrimary": false,
      "isActive": true,
      "displayOrder": 2,
      "fileSize": 3145728,
      "fileSizeFormatted": "3.00 MB",
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1280,
      "imageUrl": "http://localhost:4450/api/hero-images/qW2eR4tY/file",
      "fileImageUrl": "http://localhost:4450/api/hero-images/file/3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c.jpg",
      "createdAt": "2026-02-09T10:46:00",
      "updatedAt": "2026-02-09T10:46:00"
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Invalid hero ID format
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_READ_HERO_IMAGE permission
- `404 Not Found` - Hero not found
- `500 Internal Server Error` - Server error

---

### 4. Get Image File by Filename (Public)

Serve the actual image file by its hashed filename. No authentication required.

**Endpoint:** `GET /api/hero-images/file/{fileName}`

**Permission:** None (public access)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | SHA-256 hashed filename with extension |

**Response Headers:**
```
Content-Type: image/jpeg (or appropriate MIME type)
Content-Length: 2457600
Cache-Control: public, max-age=86400
```

**Response (200 OK):** Binary image data

**Error Responses:**

- `404 Not Found` - Image file not found on filesystem or inactive
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg" \
  --output serengeti-sunset.jpg
```

**HTML Image Tag Example:**
```html
<img src="http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg"
     alt="Beautiful sunset over Serengeti plains">
```

**Note:** Inactive images (isActive=false) will return 404.

---

### 5. Get Image File by ID (Public)

Serve the actual image file by its obfuscated ID. No authentication required. This endpoint looks up the image in the database first, then serves the file.

**Endpoint:** `GET /api/hero-images/{id}/file`

**Permission:** None (public access)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Response Headers:**
```
Content-Type: image/jpeg (or appropriate MIME type)
Content-Length: 2457600
Cache-Control: public, max-age=86400
```

**Response (200 OK):** Binary image data

**Error Responses:**

- `400 Bad Request` - Invalid image ID format
- `404 Not Found` - Image not found in database, file not found on filesystem, or inactive
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X GET "http://localhost:4450/api/hero-images/aB3dE5fG/file" \
  --output hero-image.jpg
```

**HTML Image Tag Example:**
```html
<img src="http://localhost:4450/api/hero-images/aB3dE5fG/file"
     alt="Hero image">
```

**Note:** Inactive images (isActive=false) will return 404.

---

### 6. Upload Hero Images (Bulk)

Upload one or more images for a hero section. Supports multipart/form-data for file uploads.

**Endpoint:** `POST /api/hero-images/upload`

**Permission:** `PERM_CREATE_HERO_IMAGE`

**Content-Type:** `multipart/form-data`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data
```

**Request Body (UploadHeroImagesDTO):**

The request uses `@ModelAttribute` binding with an `UploadHeroImagesDTO` containing:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| images | CreateHeroImageDTO[] | Yes | Array of image upload objects |
| images[].heroId | String | Yes | Obfuscated hero ID |
| images[].image | File | Yes | Image file (JPEG, PNG, GIF, WebP, SVG) |
| images[].altText | String | No | Alt text (accessibility) |
| images[].caption | String | No | Caption |
| images[].description | String | No | Description |

**Validation Rules:**

- **Supported Formats:** JPEG, JPG, PNG, GIF, WebP, SVG
- **Max File Size:** 10 MB per image (configurable)
- **Max Request Size:** 50 MB total (configurable)
- **Required:** At least one image in the images array

**Request Example (cURL):**
```bash
curl -X POST "http://localhost:4450/api/hero-images/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "images[0].heroId=xY8kL3mP" \
  -F "images[0].image=@serengeti-sunset.jpg" \
  -F "images[0].altText=Beautiful sunset over Serengeti plains" \
  -F "images[0].caption=Golden Hour in Serengeti" \
  -F "images[1].heroId=xY8kL3mP" \
  -F "images[1].image=@wildlife-elephants.jpg" \
  -F "images[1].altText=Elephant herd crossing the plains" \
  -F "images[1].caption=Majestic Giants"
```

**Response (201 Created):**
```json
{
  "status": 201,
  "message": "2 image(s) uploaded successfully",
  "data": [
    {
      "id": "aB3dE5fG",
      "heroId": "xY8kL3mP",
      "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
      "originalFileName": "serengeti-sunset.jpg",
      "altText": "Beautiful sunset over Serengeti plains",
      "caption": "Golden Hour in Serengeti",
      "description": null,
      "isPrimary": true,
      "isActive": true,
      "displayOrder": 1,
      "fileSize": 2457600,
      "fileSizeFormatted": "2.34 MB",
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1080,
      "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
      "fileImageUrl": "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
      "createdAt": "2026-02-09T10:45:00",
      "updatedAt": "2026-02-09T10:45:00"
    },
    {
      "id": "qW2eR4tY",
      "heroId": "xY8kL3mP",
      "fileName": "3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c.jpg",
      "originalFileName": "wildlife-elephants.jpg",
      "altText": "Elephant herd crossing the plains",
      "caption": "Majestic Giants",
      "description": null,
      "isPrimary": false,
      "isActive": true,
      "displayOrder": 2,
      "fileSize": 3145728,
      "fileSizeFormatted": "3.00 MB",
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1280,
      "imageUrl": "http://localhost:4450/api/hero-images/qW2eR4tY/file",
      "fileImageUrl": "http://localhost:4450/api/hero-images/file/3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c.jpg",
      "createdAt": "2026-02-09T10:46:00",
      "updatedAt": "2026-02-09T10:46:00"
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Validation errors:
  - No images provided
  - Invalid hero ID
  - Hero not found
  - File too large
  - Unsupported file format
  - Total request size exceeded
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_CREATE_HERO_IMAGE permission
- `500 Internal Server Error` - Server error or storage error

**Error Example:**
```json
{
  "status": 400,
  "message": "Validation failed: Image 1 (large-image.jpg): File size exceeds maximum allowed size of 10 MB",
  "errorCode": "VALIDATION_ERROR"
}
```

**Features:**

- **Automatic Display Order:** Images are assigned sequential display orders
- **SHA-256 Hashing:** Filenames are hashed for security and deduplication
- **Primary Image:** First image uploaded is automatically set as primary
- **Rollback on Error:** If database save fails, uploaded files are automatically deleted
- **Audit Logging:** All uploads are logged with user information

---

### 7. Update Image Metadata

Update metadata for an existing hero image (altText, caption, description, isPrimary, isActive). The actual image file cannot be updated - delete and re-upload if needed.

**Endpoint:** `PUT /api/hero-images/{id}`

**Permission:** `PERM_UPDATE_HERO_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body (UpdateHeroImageDTO - all fields optional):**
```json
{
  "altText": "Updated alt text for accessibility",
  "caption": "Updated Caption",
  "description": "Updated detailed description",
  "isPrimary": true,
  "isActive": true
}
```

**UpdateHeroImageDTO Fields:**

| Field | Type | Description |
|-------|------|-------------|
| altText | String | Alt text for accessibility |
| caption | String | Image caption |
| description | String | Detailed description |
| isPrimary | Boolean | Set as primary/default image |
| isActive | Boolean | Active status |

**Notes:**
- **displayOrder** is managed via the reorder endpoint
- **Image File** cannot be replaced - delete and upload new image instead
- **isPrimary=true** will automatically unset primary flag on other images for the same hero

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Hero image updated successfully",
  "data": {
    "id": "aB3dE5fG",
    "heroId": "xY8kL3mP",
    "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
    "originalFileName": "serengeti-sunset.jpg",
    "altText": "Updated alt text for accessibility",
    "caption": "Updated Caption",
    "description": "Updated detailed description",
    "isPrimary": true,
    "isActive": true,
    "displayOrder": 1,
    "fileSize": 2457600,
    "fileSizeFormatted": "2.34 MB",
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file",
    "fileImageUrl": "http://localhost:4450/api/hero-images/file/8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
    "createdAt": "2026-02-09T10:45:00",
    "updatedAt": "2026-02-09T15:30:00"
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid ID or validation error
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_UPDATE_HERO_IMAGE permission
- `404 Not Found` - Image not found
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X PUT "http://localhost:4450/api/hero-images/aB3dE5fG" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "altText": "Updated description for screen readers",
    "isPrimary": true
  }'
```

---

### 8. Reorder Hero Images (Drag & Drop)

Reorder images within a hero section. Supports drag-and-drop functionality by updating displayOrder of multiple images atomically.

**Endpoint:** `POST /api/hero-images/reorder`

**Permission:** `PERM_UPDATE_HERO_IMAGE`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "heroId": "xY8kL3mP",
  "imageOrder": [
    {
      "imageId": "qW2eR4tY",
      "expectedDisplayOrder": 1
    },
    {
      "imageId": "aB3dE5fG",
      "expectedDisplayOrder": 2
    },
    {
      "imageId": "zX9wY8vU",
      "expectedDisplayOrder": 3
    }
  ]
}
```

**ReorderHeroImagesDTO Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| heroId | String | Yes | Obfuscated hero ID |
| imageOrder | Array | Yes | Array of image order items |
| imageOrder[].imageId | String | Yes | Obfuscated image ID |
| imageOrder[].expectedDisplayOrder | Integer | No | Expected current display order (for validation) |

**Validation Rules:**

1. **Count Match:** Number of images in request must match hero's image count
2. **No Duplicates:** No duplicate image IDs in the list
3. **All Belong to Hero:** All images must belong to the specified hero
4. **No Missing Images:** All images of the hero must be included in the reorder list
5. **Expected Order Validation:** If expectedDisplayOrder is provided, it must match current order

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Images reordered successfully",
  "data": [
    {
      "id": "qW2eR4tY",
      "heroId": "xY8kL3mP",
      "fileName": "3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c.jpg",
      "originalFileName": "wildlife-elephants.jpg",
      "altText": "Elephant herd crossing the plains",
      "caption": "Majestic Giants",
      "displayOrder": 1,
      "isPrimary": true,
      "isActive": true,
      "imageUrl": "http://localhost:4450/api/hero-images/qW2eR4tY/file"
    },
    {
      "id": "aB3dE5fG",
      "heroId": "xY8kL3mP",
      "fileName": "8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg",
      "originalFileName": "serengeti-sunset.jpg",
      "altText": "Beautiful sunset over Serengeti plains",
      "caption": "Golden Hour in Serengeti",
      "displayOrder": 2,
      "isPrimary": false,
      "isActive": true,
      "imageUrl": "http://localhost:4450/api/hero-images/aB3dE5fG/file"
    },
    {
      "id": "zX9wY8vU",
      "heroId": "xY8kL3mP",
      "fileName": "1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b.jpg",
      "originalFileName": "zebras-migration.jpg",
      "altText": "Zebra herd during migration",
      "caption": "Stripes on the Move",
      "displayOrder": 3,
      "isPrimary": false,
      "isActive": true,
      "imageUrl": "http://localhost:4450/api/hero-images/zX9wY8vU/file"
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Validation error:
  - Invalid hero ID
  - Hero not found
  - Image count mismatch
  - Duplicate image IDs
  - Invalid image IDs
  - Images not belonging to hero
  - Missing images from hero
  - Expected order mismatch
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_UPDATE_HERO_IMAGE permission
- `500 Internal Server Error` - Server error

**Error Example:**
```json
{
  "status": 400,
  "message": "Image count mismatch: hero has 5 images but received 3",
  "errorCode": "IMAGE_COUNT_MISMATCH"
}
```

**Usage Example:**
```bash
curl -X POST "http://localhost:4450/api/hero-images/reorder" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "heroId": "xY8kL3mP",
    "imageOrder": [
      {"imageId": "qW2eR4tY", "expectedDisplayOrder": 1},
      {"imageId": "aB3dE5fG", "expectedDisplayOrder": 2}
    ]
  }'
```

**Features:**

- **Atomic Operation:** All display orders updated in single transaction
- **Primary Image Update:** First image in order becomes primary
- **Temporary Ordering:** Uses negative orders during update to prevent conflicts
- **Race Condition Prevention:** Expected order validation prevents concurrent modification issues

---

### 9. Delete Hero Images (Bulk)

Delete one or more images permanently from both database and filesystem.

**Endpoint:** `DELETE /api/hero-images`

**Permission:** `PERM_DELETE_HERO_IMAGE`

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | String[] | Yes | List of obfuscated image IDs to delete |

**Request Example:**
```
DELETE /api/hero-images?ids=aB3dE5fG&ids=qW2eR4tY&ids=zX9wY8vU
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "3 hero image(s) deleted successfully",
  "data": null
}
```

**Partial Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "2 hero image(s) deleted successfully. 1 image(s) not found: [invalidId123]",
  "data": null
}
```

**Error Responses:**

- `400 Bad Request` - No IDs provided or all IDs invalid
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - User lacks PERM_DELETE_HERO_IMAGE permission
- `500 Internal Server Error` - Server error

**Usage Example:**
```bash
curl -X DELETE "http://localhost:4450/api/hero-images?ids=aB3dE5fG&ids=qW2eR4tY" \
  -H "Authorization: Bearer $TOKEN"
```

**Features:**

- **Cascade Delete:** Removes image record from database and file from filesystem
- **Audit Logging:** Each deletion is logged with user information
- **Partial Success:** If some IDs are invalid, valid images are still deleted
- **File Cleanup:** Automatically deletes image files from storage directory

**Note:** Deleted images cannot be recovered. Ensure proper backups exist before deletion.

---

## Data Models

### HeroImageDTO

Response DTO for hero image data.

```typescript
{
  id: string;                    // Obfuscated image ID
  heroId: string;                // Obfuscated hero ID
  fileName: string;              // SHA-256 hashed filename
  originalFileName: string;      // Original uploaded filename
  altText?: string;              // Alt text for accessibility
  caption?: string;              // Image caption
  description?: string;          // Detailed description
  isPrimary: boolean;            // Is primary/default image
  isActive: boolean;             // Active status
  displayOrder: number;          // Display order
  fileSize: number;              // File size in bytes
  fileSizeFormatted: string;     // Human-readable file size (e.g., "2.34 MB")
  mimeType: string;              // MIME type (e.g., "image/jpeg")
  width?: number;                // Image width in pixels
  height?: number;               // Image height in pixels
  imageUrl: string;              // Full URL to access image by ID
  fileImageUrl: string;          // Full URL to access image by filename
  createdAt: string;             // ISO 8601 datetime
  updatedAt: string;             // ISO 8601 datetime
}
```

### CreateHeroImageDTO

Request DTO for uploading images.

```typescript
{
  heroId: string;                // Obfuscated hero ID (required)
  image: File;                   // Image file (required)
  altText?: string;              // Alt text (optional)
  caption?: string;              // Caption (optional)
  description?: string;          // Description (optional)
}
```

### UploadHeroImagesDTO

Wrapper DTO for uploading multiple images.

```typescript
{
  images: CreateHeroImageDTO[];  // Array of image upload objects (required)
}
```

### UpdateHeroImageDTO

Request DTO for updating image metadata.

```typescript
{
  altText?: string;              // Alt text (optional)
  caption?: string;              // Caption (optional)
  description?: string;          // Description (optional)
  isPrimary?: boolean;           // Primary status (optional)
  isActive?: boolean;            // Active status (optional)
}
```

### ReorderHeroImagesDTO

Request DTO for reordering images.

```typescript
{
  heroId: string;                // Obfuscated hero ID (required)
  imageOrder: [                  // Array of image order items (required)
    {
      imageId: string;           // Obfuscated image ID (required)
      expectedDisplayOrder?: number; // Expected current order (optional)
    }
  ]
}
```

---

## File Storage

### Storage Configuration

Images are stored in the filesystem with the following configuration:

**Default Storage Path:**
```
./data/hero-images/
```

**Configuration Property:**
```properties
hero.image.storage.path=./data/hero-images/
```

### Filename Hashing

All uploaded images are renamed using SHA-256 hashing:

**Process:**
1. Calculate SHA-256 hash of file contents
2. Append original file extension
3. Save file with hashed name

**Example:**
- Original: `serengeti-sunset.jpg`
- Hashed: `8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e.jpg`

**Benefits:**
- **Deduplication:** Identical images have same hash
- **Security:** Prevents filename-based attacks
- **Collision Resistance:** SHA-256 provides strong uniqueness guarantee

### Supported Formats

| Format | Extension | MIME Type | Max Size |
|--------|-----------|-----------|----------|
| JPEG | .jpg, .jpeg | image/jpeg | 10 MB |
| PNG | .png | image/png | 10 MB |
| GIF | .gif | image/gif | 10 MB |
| WebP | .webp | image/webp | 10 MB |
| SVG | .svg | image/svg+xml | 10 MB |

### Size Limits

- **Per Image:** 10 MB (configurable)
- **Per Request:** 50 MB total (configurable)

**Configuration Properties:**
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| HERO_IMAGE_NOT_FOUND | 404 | Image with specified ID not found |
| HERO_NOT_FOUND | 404 | Hero with specified ID not found |
| IMAGE_NOT_FOUND | 404 | Image not found |
| NO_IMAGES_PROVIDED | 400 | No image files in upload request |
| NO_IDS_PROVIDED | 400 | No image IDs in delete request |
| VALIDATION_ERROR | 400 | Request validation failed |
| INVALID_IMAGE_ID | 400 | Invalid or malformed image ID |
| INVALID_HERO_ID | 400 | Invalid or malformed hero ID |
| FILE_TOO_LARGE | 400 | Image file exceeds size limit |
| UNSUPPORTED_FORMAT | 400 | Image format not supported |
| REQUEST_SIZE_EXCEEDED | 400 | Total request size exceeds limit |
| IMAGE_COUNT_MISMATCH | 400 | Reorder count doesn't match hero image count |
| DUPLICATE_IMAGE_IDS | 400 | Duplicate image IDs in reorder request |
| IMAGE_NOT_IN_HERO | 400 | Image doesn't belong to specified hero |
| MISSING_IMAGES | 400 | Some images from hero are missing in reorder |
| EXPECTED_ORDER_MISMATCH | 400 | Expected display order doesn't match current |
| STORAGE_ERROR | 500 | Failed to save image to filesystem |
| DATABASE_ERROR | 500 | Failed to save image record to database |
| IMAGE_UPDATE_FAILED | 500 | Failed to update image metadata |
| UNAUTHORIZED | 401 | Missing or invalid authentication token |
| FORBIDDEN | 403 | User lacks required permission |
| INTERNAL_ERROR | 500 | Unexpected server error |

---

## Usage Examples

### Example 1: Complete Image Management Workflow

```bash
# 1. Upload images to hero
curl -X POST "http://localhost:4450/api/hero-images/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "images[0].heroId=xY8kL3mP" \
  -F "images[0].image=@image1.jpg" \
  -F "images[0].altText=Serengeti landscape" \
  -F "images[1].heroId=xY8kL3mP" \
  -F "images[1].image=@image2.jpg" \
  -F "images[1].altText=Wildlife close-up"

# 2. Get all images for hero
curl -X GET "http://localhost:4450/api/hero-images/hero/xY8kL3mP" \
  -H "Authorization: Bearer $TOKEN"

# 3. Update image metadata
curl -X PUT "http://localhost:4450/api/hero-images/aB3dE5fG" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "altText": "Updated description for better accessibility",
    "isPrimary": true
  }'

# 4. Display image in HTML (public access - no auth needed)
# Use imageUrl from response:
# <img src="http://localhost:4450/api/hero-images/aB3dE5fG/file" alt="Serengeti landscape">
```

### Example 2: Search and Filter Images

```bash
# 1. List all images for HOME page heroes
curl -X GET "http://localhost:4450/api/hero-images?heroPage=HOME&isActive=true" \
  -H "Authorization: Bearer $TOKEN"

# 2. Search by hero title
curl -X GET "http://localhost:4450/api/hero-images?heroTitle=Serengeti" \
  -H "Authorization: Bearer $TOKEN"

# 3. Get all primary images
curl -X GET "http://localhost:4450/api/hero-images?isPrimary=true&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Example 3: Reorder Images (Drag & Drop)

```bash
# User drags image 3 to position 1 in UI

# Send reorder request
curl -X POST "http://localhost:4450/api/hero-images/reorder" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "heroId": "xY8kL3mP",
    "imageOrder": [
      {"imageId": "zX9wY8vU", "expectedDisplayOrder": 3},
      {"imageId": "aB3dE5fG", "expectedDisplayOrder": 1},
      {"imageId": "qW2eR4tY", "expectedDisplayOrder": 2}
    ]
  }'
```

### Example 4: Delete Unused Images

```bash
# 1. Get images for hero
curl -X GET "http://localhost:4450/api/hero-images/hero/xY8kL3mP" \
  -H "Authorization: Bearer $TOKEN"

# 2. Delete specific images
curl -X DELETE "http://localhost:4450/api/hero-images?ids=oldImageId1&ids=oldImageId2" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Best Practices

1. **Alt Text:** Always provide alt text for accessibility (screen readers)
2. **Image Optimization:** Compress images before upload to reduce file size and improve load times
3. **Primary Image:** First image is automatically set as primary - upload best image first
4. **Metadata Updates:** Use PUT endpoint to update metadata instead of deleting and re-uploading
5. **Reordering:** Use reorder endpoint instead of deleting and re-uploading for better performance
6. **Public URLs:** Use `imageUrl` from API response for front-end display
7. **Caching:** Images are cached for 24 hours (max-age=86400) - use cache busting if needed
8. **Responsive Images:** Consider uploading multiple resolutions for responsive design
9. **File Naming:** Use descriptive original filenames for better organization
10. **Batch Operations:** Upload multiple images in single request for better performance
11. **Validation:** Always provide expectedDisplayOrder when reordering to prevent race conditions
12. **Filtering:** Use list endpoint with filters for admin interfaces to find specific images
13. **Pagination:** Use pagination for large image collections to improve performance

---

## Changelog

**Version 2.0.0** (2026-02-10)
- Added GET /api/hero-images endpoint for listing all images with filters and pagination
- Added GET /api/hero-images/{id} endpoint for getting single image by ID
- Added PUT /api/hero-images/{id} endpoint for updating image metadata
- Updated upload endpoint to use @ModelAttribute UploadHeroImagesDTO
- Updated delete endpoint to use @RequestParam("ids") instead of @RequestBody
- Enhanced documentation with new filtering capabilities
- Added comprehensive examples for all new endpoints

**Version 1.0.0** (2026-02-09)
- Initial API documentation
- 6 endpoints: Get by hero, Upload, Reorder, Delete, Serve by filename, Serve by ID
- Support for multi-image hero carousels
- SHA-256 filename hashing for security
- Public image serving endpoints
- Drag & drop reordering with validation
- Bulk upload and delete operations
- Automatic primary image management
