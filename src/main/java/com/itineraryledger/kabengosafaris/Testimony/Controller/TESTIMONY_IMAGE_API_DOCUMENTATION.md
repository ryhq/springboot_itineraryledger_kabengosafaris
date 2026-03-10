# Testimony Image Management API Documentation

## Overview

The Testimony Image Management API provides comprehensive endpoints for managing images associated with testimonies in the Kabengo Safaris tourism platform. Images are stored on the filesystem with SHA-256 hashed filenames, and metadata is persisted in the database.

All endpoints use permission-based access control and return responses wrapped in the standard `ApiResponse` format. Image file serving endpoints are publicly accessible without authentication.

---

## Table of Contents

1. [Testimony Image API](#testimony-image-api)
   - [List Images](#1-list-images)
   - [Get Image by ID](#2-get-image-by-id)
   - [Get Images for a Testimony](#3-get-images-for-a-testimony)
   - [Serve Image File by Filename](#4-serve-image-file-by-filename)
   - [Serve Image File by ID](#5-serve-image-file-by-id)
   - [Upload Images](#6-upload-images)
   - [Update Image Metadata](#7-update-image-metadata)
   - [Bulk Delete Images](#8-bulk-delete-images)

2. [Data Models](#data-models)
3. [Error Codes](#error-codes)
4. [Examples](#examples)

---

## Testimony Image API

Base URL: `/api/testimony-images`

### 1. List Images

Retrieves all testimony images with optional filtering, pagination, and sorting.

**Endpoint:** `GET /api/testimony-images`

**Permission Required:** `PERM_READ_TESTIMONY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `testimonyId` | string | No | - | Filter by testimony ID (obfuscated) |
| `isPrimary` | boolean | No | - | Filter by primary status |
| `isActive` | boolean | No | - | Filter by active status |
| `keyword` | string | No | - | Search across text fields (altText, caption, description) |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 20 | Page size |
| `sortBy` | string | No | createdAt | Sort field (see valid fields below) |
| `sortDirection` | string | No | desc | Sort direction (`asc` or `desc`) |

**Valid Sort Fields:**
`isPrimary`, `isActive`, `displayOrder`, `fileSize`, `createdAt`, `updatedAt`

**Example Requests:**

1. All images (default pagination):
   ```
   GET /api/testimony-images
   ```

2. Filter by testimony:
   ```
   GET /api/testimony-images?testimonyId=abc123xyz
   ```

3. Primary images only:
   ```
   GET /api/testimony-images?isPrimary=true
   ```

4. Active images sorted by file size:
   ```
   GET /api/testimony-images?isActive=true&sortBy=fileSize&sortDirection=desc
   ```

5. Search by keyword:
   ```
   GET /api/testimony-images?keyword=safari+sunset
   ```

6. Combined filters with pagination:
   ```
   GET /api/testimony-images?testimonyId=abc123xyz&isActive=true&page=0&size=10&sortBy=displayOrder&sortDirection=asc
   ```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony images retrieved successfully",
  "data": {
    "images": [
      {
        "id": "img123abc",
        "testimonyId": "xyz789abc",
        "imageUrl": "http://localhost:4450/api/testimony-images/img123abc/file",
        "fileImageUrl": "http://localhost:4450/api/testimony-images/file/a1b2c3d4e5.jpg",
        "fileName": "a1b2c3d4e5.jpg",
        "originalFileName": "safari-sunset.jpg",
        "altText": "Sunset over the Serengeti",
        "caption": "Beautiful sunset during evening game drive",
        "description": "A stunning photo taken during our evening safari",
        "isPrimary": true,
        "isActive": true,
        "displayOrder": 1,
        "fileSize": 2457600,
        "fileSizeFormatted": "2.3 MB",
        "mimeType": "image/jpeg",
        "width": 1920,
        "height": 1080,
        "createdAt": "2026-03-10T10:30:00",
        "updatedAt": "2026-03-10T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 1,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false,
    "validSortFields": ["isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"],
    "currentSortBy": "createdAt",
    "currentSortDirection": "desc"
  },
  "timestamp": "2026-03-10T12:00:00"
}
```

**Notes:**
- Invalid sort field returns 400 error with list of valid fields
- Null fields are excluded from JSON response (`@JsonInclude(NON_NULL)`)

---

### 2. Get Image by ID

Retrieves a single testimony image by its obfuscated ID, including circular navigation.

**Endpoint:** `GET /api/testimony-images/{id}`

**Permission Required:** `PERM_READ_TESTIMONY_IMAGE`

**Path Parameters:**
- `id` (string, required): Obfuscated image ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony image retrieved successfully",
  "data": {
    "image": {
      "id": "img123abc",
      "testimonyId": "xyz789abc",
      "imageUrl": "http://localhost:4450/api/testimony-images/img123abc/file",
      "fileImageUrl": "http://localhost:4450/api/testimony-images/file/a1b2c3d4e5.jpg",
      "fileName": "a1b2c3d4e5.jpg",
      "originalFileName": "safari-sunset.jpg",
      "altText": "Sunset over the Serengeti",
      "caption": "Beautiful sunset during evening game drive",
      "description": "A stunning photo taken during our evening safari",
      "isPrimary": true,
      "isActive": true,
      "displayOrder": 1,
      "fileSize": 2457600,
      "fileSizeFormatted": "2.3 MB",
      "mimeType": "image/jpeg",
      "width": 1920,
      "height": 1080,
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-10T10:30:00"
    },
    "nextId": "img456def",
    "previousId": "img789ghi"
  },
  "timestamp": "2026-03-10T12:00:00"
}
```

**Navigation:**
- `nextId` / `previousId` provide circular navigation — wraps from last to first and vice versa
- Both are obfuscated IDs

**Error Responses:**

- **400 Bad Request** - Invalid image ID format
- **404 Not Found** - Image not found

---

### 3. Get Images for a Testimony

Retrieves all active images for a specific testimony.

**Endpoint:** `GET /api/testimony-images/testimony/{testimonyId}`

**Permission Required:** `PERM_READ_TESTIMONY_IMAGE`

**Path Parameters:**
- `testimonyId` (string, required): Obfuscated testimony ID

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony images retrieved successfully",
  "data": [
    {
      "id": "img123abc",
      "testimonyId": "xyz789abc",
      "imageUrl": "http://localhost:4450/api/testimony-images/img123abc/file",
      "fileImageUrl": "http://localhost:4450/api/testimony-images/file/a1b2c3d4e5.jpg",
      "fileName": "a1b2c3d4e5.jpg",
      "originalFileName": "safari-sunset.jpg",
      "altText": "Sunset over the Serengeti",
      "isPrimary": true,
      "isActive": true,
      "displayOrder": 1,
      "fileSize": 2457600,
      "fileSizeFormatted": "2.3 MB",
      "mimeType": "image/jpeg",
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-10T10:30:00"
    }
  ],
  "timestamp": "2026-03-10T12:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid testimony ID format

**Notes:**
- Returns only active images (`isActive = true`)

---

### 4. Serve Image File by Filename

Serves the actual image binary file by its stored filename.

**Endpoint:** `GET /api/testimony-images/file/{fileName}`

**Permission Required:** None (public endpoint)

**Path Parameters:**
- `fileName` (string, required): The stored filename (SHA-256 hashed, e.g., `a1b2c3d4e5.jpg`)

**Success Response (200 OK):**
- **Content-Type:** Image MIME type (e.g., `image/jpeg`, `image/png`)
- **Content-Length:** File size in bytes
- **Cache-Control:** `public, max-age=86400` (24 hours)
- **Body:** Raw image binary

**Error Responses:**

- **404 Not Found** - Image not found or inactive

**Notes:**
- No authentication required — designed for public image display
- Only serves images where `isActive = true`
- Cached for 24 hours via Cache-Control header

---

### 5. Serve Image File by ID

Serves the actual image binary file by its obfuscated ID.

**Endpoint:** `GET /api/testimony-images/{id}/file`

**Permission Required:** None (public endpoint)

**Path Parameters:**
- `id` (string, required): Obfuscated image ID

**Success Response (200 OK):**
- **Content-Type:** Image MIME type (e.g., `image/jpeg`, `image/png`)
- **Content-Length:** File size in bytes
- **Cache-Control:** `public, max-age=86400` (24 hours)
- **Body:** Raw image binary

**Error Responses:**

- **400 Bad Request** - Invalid image ID format
- **404 Not Found** - Image not found or inactive

**Notes:**
- No authentication required — designed for public image display
- Only serves images where `isActive = true`
- This is the URL returned in `imageUrl` field of the DTO

---

### 6. Upload Images

Upload one or more images for testimonies via multipart form data.

**Endpoint:** `POST /api/testimony-images/upload`

**Permission Required:** `PERM_CREATE_TESTIMONY_IMAGE`

**Content-Type:** `multipart/form-data`

**Request Body (Multipart Form):**

For each image in the `images` array:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `images[N].testimonyId` | string | Yes | Obfuscated testimony ID |
| `images[N].image` | file | Yes | Image file (JPEG, PNG, etc.) |
| `images[N].altText` | string | No | Alt text for accessibility |
| `images[N].caption` | string | No | Image caption |
| `images[N].description` | string | No | Detailed description |

**Example: Upload Single Image**
```bash
curl -X POST http://localhost:4450/api/testimony-images/upload \
  -H "Authorization: Bearer <token>" \
  -F "images[0].testimonyId=xyz789abc" \
  -F "images[0].image=@/path/to/safari-photo.jpg" \
  -F "images[0].altText=Safari sunset photo" \
  -F "images[0].caption=Beautiful sunset over the Serengeti"
```

**Example: Upload Multiple Images**
```bash
curl -X POST http://localhost:4450/api/testimony-images/upload \
  -H "Authorization: Bearer <token>" \
  -F "images[0].testimonyId=xyz789abc" \
  -F "images[0].image=@/path/to/photo1.jpg" \
  -F "images[0].altText=Wildlife encounter" \
  -F "images[1].testimonyId=xyz789abc" \
  -F "images[1].image=@/path/to/photo2.jpg" \
  -F "images[1].altText=Camp at sunset"
```

**Example: Upload for Different Testimonies**
```bash
curl -X POST http://localhost:4450/api/testimony-images/upload \
  -H "Authorization: Bearer <token>" \
  -F "images[0].testimonyId=xyz789abc" \
  -F "images[0].image=@/path/to/photo1.jpg" \
  -F "images[1].testimonyId=def456ghi" \
  -F "images[1].image=@/path/to/photo2.jpg"
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "2 image(s) uploaded successfully",
  "data": [
    {
      "id": "img123abc",
      "testimonyId": "xyz789abc",
      "imageUrl": "http://localhost:4450/api/testimony-images/img123abc/file",
      "fileImageUrl": "http://localhost:4450/api/testimony-images/file/a1b2c3d4e5.jpg",
      "fileName": "a1b2c3d4e5.jpg",
      "originalFileName": "safari-photo.jpg",
      "altText": "Safari sunset photo",
      "caption": "Beautiful sunset over the Serengeti",
      "isPrimary": false,
      "isActive": true,
      "fileSize": 2457600,
      "fileSizeFormatted": "2.3 MB",
      "mimeType": "image/jpeg",
      "createdAt": "2026-03-10T10:30:00",
      "updatedAt": "2026-03-10T10:30:00"
    }
  ],
  "timestamp": "2026-03-10T10:30:00"
}
```

**Error Responses:**

- **400 Bad Request** - No images provided
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "No images provided",
    "errorCode": "NO_IMAGES_PROVIDED"
  }
  ```

- **400 Bad Request** - Validation errors
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Validation failed: Image 1: Testimony ID is required; Image 2: Image file is required",
    "errorCode": "VALIDATION_ERROR"
  }
  ```

- **400 Bad Request** - Total request size exceeded
  ```json
  {
    "success": false,
    "statusCode": 400,
    "message": "Total request size exceeds maximum allowed",
    "errorCode": "REQUEST_SIZE_EXCEEDED"
  }
  ```

- **500 Internal Server Error** - Storage failure
  ```json
  {
    "success": false,
    "statusCode": 500,
    "message": "Failed to save image file: photo.jpg",
    "errorCode": "STORAGE_ERROR"
  }
  ```

**Notes:**
- Images are stored with SHA-256 hashed filenames for deduplication
- All images in a batch are validated before any are saved
- If a storage error occurs mid-batch, previously saved files are rolled back
- New images default to `isPrimary: false` and `isActive: true`
- File size and MIME type are automatically detected

---

### 7. Update Image Metadata

Updates metadata fields of an existing testimony image. Does not replace the actual image file.

**Endpoint:** `PUT /api/testimony-images/{id}`

**Permission Required:** `PERM_UPDATE_TESTIMONY_IMAGE`

**Path Parameters:**
- `id` (string, required): Obfuscated image ID

**Request Body:**
```json
{
  "altText": "Updated alt text",
  "caption": "Updated caption",
  "description": "Updated description",
  "isPrimary": true,
  "isActive": true,
  "displayOrder": 2
}
```

**All Fields Are Optional:**

| Field | Type | Description |
|-------|------|-------------|
| `altText` | string | Alt text for accessibility |
| `caption` | string | Image caption |
| `description` | string | Detailed description |
| `isPrimary` | boolean | Whether this is the primary image |
| `isActive` | boolean | Whether the image is active |
| `displayOrder` | integer | Display ordering position |

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Testimony image updated successfully",
  "data": {
    "id": "img123abc",
    "testimonyId": "xyz789abc",
    "altText": "Updated alt text",
    "caption": "Updated caption",
    "description": "Updated description",
    "isPrimary": true,
    "isActive": true,
    "displayOrder": 2,
    ...
  },
  "timestamp": "2026-03-10T14:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - Invalid image ID format
- **404 Not Found** - Image not found

**Notes:**
- Only provided fields are updated; omitted fields remain unchanged
- To replace the actual image file, delete the old image and upload a new one
- Setting `isPrimary: true` may unset the previous primary image for the same testimony

---

### 8. Bulk Delete Images

Permanently deletes multiple testimony images from both the database and filesystem.

**Endpoint:** `DELETE /api/testimony-images`

**Permission Required:** `PERM_DELETE_TESTIMONY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ids` | string[] | Yes | Comma-separated list of obfuscated image IDs |

**Example Requests:**

Delete multiple images:
```
DELETE /api/testimony-images?ids=img123abc,img456def,img789ghi
```

Delete single image:
```
DELETE /api/testimony-images?ids=img123abc
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 images deleted successfully",
  "data": null,
  "timestamp": "2026-03-10T16:00:00"
}
```

**Error Responses:**

- **400 Bad Request** - No IDs provided or invalid IDs
- **500 Internal Server Error** - Deletion failed

**Notes:**
- Permanently removes image records from the database AND image files from the filesystem
- This action is irreversible

---

## Data Models

### TestimonyImageDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Obfuscated image ID |
| `testimonyId` | string | Obfuscated testimony ID |
| `imageUrl` | string | Full URL to image by ID (`/api/testimony-images/{id}/file`) |
| `fileImageUrl` | string | Full URL to image by filename (`/api/testimony-images/file/{fileName}`) |
| `fileName` | string | Stored filename (SHA-256 hashed) |
| `originalFileName` | string | Original uploaded filename |
| `altText` | string | Alt text for accessibility |
| `caption` | string | Image caption |
| `description` | string | Detailed description |
| `isPrimary` | boolean | Whether this is the primary image |
| `isActive` | boolean | Whether the image is active |
| `displayOrder` | integer | Display ordering position |
| `fileSize` | long | File size in bytes |
| `fileSizeFormatted` | string | Human-readable file size (e.g., "2.3 MB", "450 KB") |
| `mimeType` | string | MIME type (e.g., `image/jpeg`, `image/png`) |
| `width` | integer | Image width in pixels |
| `height` | integer | Image height in pixels |
| `createdAt` | datetime | Creation timestamp |
| `updatedAt` | datetime | Last update timestamp |

**Note:** Null fields are excluded from JSON responses (`@JsonInclude(NON_NULL)`).

### Image URLs

Each image has two accessible URLs:
1. **By ID:** `http://localhost:4450/api/testimony-images/{id}/file` — uses obfuscated ID
2. **By Filename:** `http://localhost:4450/api/testimony-images/file/{fileName}` — uses stored filename

Both endpoints are public (no authentication required) and return the raw image binary.

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| `VALIDATION_ERROR` | Request body failed validation |
| `NO_IMAGES_PROVIDED` | Upload request contained no images |
| `REQUEST_SIZE_EXCEEDED` | Total upload size exceeds maximum allowed |
| `STORAGE_ERROR` | Failed to save image file to filesystem |
| `DATABASE_ERROR` | Failed to create image records in database |
| `INVALID_IMAGE_ID` | The provided image ID is invalid or malformed |
| `IMAGE_NOT_FOUND` | Image with the specified ID does not exist |
| `INVALID_TESTIMONY_ID` | The provided testimony ID is invalid |
| `TESTIMONY_NOT_FOUND` | Testimony with the specified ID does not exist |
| `INVALID_SORT_FIELD` | Invalid sort field provided (includes valid fields in message) |
| `TESTIMONY_IMAGE_UPLOAD_FAILED` | Failed to upload testimony images |
| `TESTIMONY_IMAGE_FETCH_FAILED` | Failed to fetch testimony images |

---

## Examples

### Example 1: Upload Photos for a Testimony

```bash
curl -X POST http://localhost:4450/api/testimony-images/upload \
  -H "Authorization: Bearer <token>" \
  -F "images[0].testimonyId=xyz789abc" \
  -F "images[0].image=@/home/user/photos/serengeti-sunset.jpg" \
  -F "images[0].altText=Serengeti sunset during evening game drive" \
  -F "images[0].caption=The golden hour over the Serengeti plains" \
  -F "images[0].description=Captured during our final evening game drive" \
  -F "images[1].testimonyId=xyz789abc" \
  -F "images[1].image=@/home/user/photos/lion-close-up.jpg" \
  -F "images[1].altText=Close-up of a male lion" \
  -F "images[1].caption=King of the Savanna"
```

### Example 2: Set Primary Image

After uploading multiple images, set one as the primary:

```http
PUT /api/testimony-images/img123abc
Authorization: Bearer <token>
Content-Type: application/json

{
  "isPrimary": true
}
```

### Example 3: Deactivate an Image

Hide an image without deleting it:

```http
PUT /api/testimony-images/img456def
Authorization: Bearer <token>
Content-Type: application/json

{
  "isActive": false
}
```

### Example 4: View All Images for a Testimony

```http
GET /api/testimony-images/testimony/xyz789abc
Authorization: Bearer <token>
```

### Example 5: Display Image on Website

Use the public URL directly in HTML (no auth needed):

```html
<img src="http://localhost:4450/api/testimony-images/img123abc/file"
     alt="Serengeti sunset" />
```

Or use the filename-based URL:

```html
<img src="http://localhost:4450/api/testimony-images/file/a1b2c3d4e5.jpg"
     alt="Serengeti sunset" />
```

### Example 6: Clean Up Old Images

Find inactive images and delete them:

```http
GET /api/testimony-images?isActive=false
Authorization: Bearer <token>
```

Then delete:
```http
DELETE /api/testimony-images?ids=img111,img222,img333
Authorization: Bearer <token>
```

---

## Best Practices

### 1. Image Management

- **Primary Image:** Each testimony should have one primary image for use as a thumbnail
- **Alt Text:** Always provide alt text for accessibility compliance
- **Deactivate vs Delete:** Use `isActive: false` to hide images temporarily; delete only when permanent removal is needed

### 2. Uploads

- Validate images on the client side before uploading (file type, size)
- Upload related images in a single batch request for atomicity
- The server validates file format and size — check error responses for specifics

### 3. Public Display

- Use `imageUrl` (by ID) or `fileImageUrl` (by filename) for public-facing image URLs
- Both public endpoints cache images for 24 hours
- Only active images are served publicly

### 4. Performance

- Use the paginated list endpoint with filters rather than fetching all images
- Use the testimony-specific endpoint (`/testimony/{id}`) when you only need images for one testimony

---

## Changelog

### Version 1.0.0
- Initial API documentation
- Complete CRUD operations for testimony images
- Multipart upload with batch validation and rollback
- Public image serving with caching
- Specification-based filtering and pagination

---

## Support

For technical support or questions about the Testimony Image API, please contact:
- **Email:** support@kabengosafaris.com
- **Documentation:** https://docs.kabengosafaris.com
- **Issue Tracker:** https://github.com/kabengosafaris/issues
