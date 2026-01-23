# Accommodation Image API Documentation

## Overview

The Accommodation Image API provides endpoints for managing images associated with accommodations. Images are stored on the filesystem with generated filenames, while only the filename is stored in the database. The full URL is constructed dynamically using the configured base URL.

**Base URL:** `/api/accommodation-images`

---

## Authentication & Authorization

All endpoints require authentication via JWT token (except file serving endpoints):
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET (list/details) | `PERM_READ_ACCOMMODATION_IMAGE` |
| GET (file serving) | None (public access) |
| POST (upload) | `PERM_CREATE_ACCOMMODATION_IMAGE` |
| PUT (update) | `PERM_UPDATE_ACCOMMODATION_IMAGE` |
| DELETE | `PERM_DELETE_ACCOMMODATION_IMAGE` |

---

## Image Storage

- **Storage Path:** Configured via `accommodation.image.storage.path` in application.properties
- **URL Construction:** `app.base.url` + `/api/accommodation-images/{obfuscatedId}/file`
- **Alternative URL:** `app.base.url` + `/api/accommodation-images/file/{fileName}`
- **Filename Format:** UUID-based (e.g., `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6.jpg`)
- **Validation:** Uses `ImageSettingGetterServices` for file size and format validation

---

## Endpoints

### 1. Get All Images (with Filters, Pagination, and Sorting)

Get all images with optional filters, pagination, and sorting. Always sorted by `createdAt`, descending by default.

**Endpoint:** `GET /api/accommodation-images`

**Permission:** `PERM_READ_ACCOMMODATION_IMAGE`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accommodationId` | String | No | - | Filter by accommodation (obfuscated ID) |
| `accommodationName` | String | No | - | Filter by accommodation name (contains) |
| `accommodationType` | Enum | No | - | Filter by accommodation type (HOTEL, LODGE, etc.) |
| `accommodationCategory` | Enum | No | - | Filter by category (LUXURY, MID_RANGE, BUDGET, etc.) |
| `region` | String | No | - | Filter by accommodation region (contains) |
| `district` | String | No | - | Filter by accommodation district (contains) |
| `imageType` | Enum | No | - | Filter by image type (EXTERIOR, ROOM, etc.) |
| `isPrimary` | Boolean | No | - | Filter by primary status |
| `isActive` | Boolean | No | - | Filter by active status |
| `page` | Integer | No | 0 | Page number (0-indexed) |
| `size` | Integer | No | 20 | Page size |
| `sortDirection` | String | No | desc | Sort direction (asc/desc) |

#### Example Request

```bash
curl -X GET "http://localhost:4450/api/accommodation-images?accommodationName=Serengeti&imageType=EXTERIOR&isActive=true&page=0&size=10" \
  -H "Authorization: Bearer <jwt_token>"
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Accommodation images retrieved successfully.",
  "data": {
    "content": [
      {
        "id": "obfuscated_id",
        "accommodationId": "obfuscated_accommodation_id",
        "accommodationName": "Serengeti Safari Lodge",
        "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id/file",
        "fileName": "a1b2c3d4e5f6.jpg",
        "imageType": "EXTERIOR",
        "isPrimary": true,
        "isActive": true,
        "displayOrder": 1,
        "createdAt": "2025-01-23T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. Get Image by ID

Get a specific image by its obfuscated ID.

**Endpoint:** `GET /api/accommodation-images/{id}`

**Permission:** `PERM_READ_ACCOMMODATION_IMAGE`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Image retrieved successfully.",
  "data": {
    "id": "obfuscated_id",
    "accommodationId": "obfuscated_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id/file",
    "fileName": "a1b2c3d4e5f6.jpg",
    "originalFileName": "front_view.jpg",
    "imageType": "EXTERIOR",
    "imageTypeDisplayName": "Exterior",
    "imageTypeDescription": "Exterior/facade of the accommodation",
    "altText": "Front view of the lodge",
    "caption": "Main Entrance",
    "isPrimary": true,
    "isActive": true,
    "displayOrder": 1,
    "fileSize": 245760,
    "fileSizeFormatted": "240.0 KB",
    "mimeType": "image/jpeg",
    "createdAt": "2025-01-23T10:30:00",
    "updatedAt": "2025-01-23T10:30:00"
  }
}
```

---

### 3. Get Image File by Filename

Serve the actual image file by its stored filename.

**Endpoint:** `GET /api/accommodation-images/file/{fileName}`

**Permission:** None (public access for image display)

**Example:** `GET /api/accommodation-images/file/a1b2c3d4e5f6.jpg`

**Response:** Binary image data with appropriate Content-Type header

**Headers Returned:**
- `Content-Type`: Image MIME type (e.g., `image/jpeg`, `image/png`)
- `Content-Length`: File size in bytes
- `Cache-Control`: `public, max-age=86400` (1 day caching)

**Usage:**
```html
<img src="http://localhost:4450/api/accommodation-images/file/a1b2c3d4e5f6.jpg" alt="Accommodation Image" />
```

**Error Responses:**
- 404 Not Found: Image not found or inactive

---

### 4. Get Image File by ID

Serve the actual image file by its obfuscated ID.

**Endpoint:** `GET /api/accommodation-images/{id}/file`

**Permission:** None (public access for image display)

**Response:** Binary image data with appropriate Content-Type header

**Headers Returned:**
- `Content-Type`: Image MIME type (e.g., `image/jpeg`, `image/png`)
- `Content-Length`: File size in bytes
- `Cache-Control`: `public, max-age=86400` (1 day caching)

**Usage:**
```html
<img src="http://localhost:4450/api/accommodation-images/obfuscated_id/file" alt="Accommodation Image" />
```

**Error Responses:**
- 400 Bad Request: Invalid image ID format
- 404 Not Found: Image not found or inactive

---

### 5. Upload Multiple Images

Upload multiple images for an accommodation in a single request.

**Endpoint:** `POST /api/accommodation-images/accommodation/{accommodationId}/upload`

**Content-Type:** `multipart/form-data`

**Permission:** `PERM_CREATE_ACCOMMODATION_IMAGE`

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `files` | File[] | Yes | List of image files to upload |
| `imageType` | Enum | No | Common image type for all uploads (defaults to `OTHER`) |

#### Example Request (cURL)

```bash
curl -X POST "http://localhost:4450/api/accommodation-images/accommodation/{accommodationId}/upload" \
  -H "Authorization: Bearer <jwt_token>" \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg" \
  -F "files=@/path/to/image3.jpg" \
  -F "imageType=EXTERIOR"
```

#### Response

```json
{
  "success": true,
  "status": 201,
  "message": "3 image(s) uploaded successfully.",
  "data": [
    {
      "id": "obfuscated_id_1",
      "accommodationId": "obfuscated_accommodation_id",
      "accommodationName": "Serengeti Safari Lodge",
      "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id_1/file",
      "fileName": "a1b2c3d4e5f6.jpg",
      "originalFileName": "image1.jpg",
      "imageType": "EXTERIOR",
      "isPrimary": false,
      "isActive": true,
      "displayOrder": 1
    },
    {
      "id": "obfuscated_id_2",
      "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id_2/file",
      "fileName": "g7h8i9j0k1l2.jpg",
      "originalFileName": "image2.jpg",
      "imageType": "EXTERIOR",
      "isPrimary": false,
      "displayOrder": 2
    }
  ]
}
```

#### Validation Error Response

```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed for one or more files.",
  "errorCode": "VALIDATION_ERROR",
  "data": [
    "File 1 (invalid.exe): File extension not allowed",
    "File 3 (large.jpg): File size exceeds maximum allowed (5 MB)"
  ]
}
```

---

### 6. Update Image Metadata

Update image metadata (not the image file itself).

**Endpoint:** `PUT /api/accommodation-images/{id}`

**Permission:** `PERM_UPDATE_ACCOMMODATION_IMAGE`

#### Request Body

```json
{
  "imageType": "ROOM",
  "altText": "Updated alt text",
  "caption": "Updated caption",
  "description": "Updated description",
  "isPrimary": true,
  "isActive": true,
  "displayOrder": 5
}
```

All fields are optional. Only provided fields will be updated.

**Note:** Setting `isPrimary` to `true` will automatically unset the primary flag on other images for the same accommodation.

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Accommodation image updated successfully.",
  "data": {
    "id": "obfuscated_id",
    "imageType": "ROOM",
    "isPrimary": true,
    "displayOrder": 5
  }
}
```

---

### 7. Bulk Delete Images

Permanently delete multiple images by their IDs. Removes from both database and filesystem.

**Endpoint:** `DELETE /api/accommodation-images?ids={id1}&ids={id2}&ids={id3}`

**Permission:** `PERM_DELETE_ACCOMMODATION_IMAGE`

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ids` | String[] | Yes | List of obfuscated image IDs to delete |

#### Example Request

```bash
curl -X DELETE "http://localhost:4450/api/accommodation-images?ids=obfuscated_id_1&ids=obfuscated_id_2&ids=obfuscated_id_3" \
  -H "Authorization: Bearer <jwt_token>"
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "3 image(s) deleted successfully.",
  "data": {
    "deletedCount": 3,
    "requestedCount": 3
  }
}
```

#### Partial Success Response

```json
{
  "success": true,
  "status": 200,
  "message": "2 image(s) deleted, 1 failed.",
  "data": {
    "deletedCount": 2,
    "requestedCount": 3,
    "failedIds": ["obfuscated_id_3"]
  }
}
```

---

## Image Types

| Type | Display Name | Description |
|------|--------------|-------------|
| `EXTERIOR` | Exterior | Exterior/facade of the accommodation |
| `INTERIOR` | Interior | Interior common areas |
| `ROOM` | Room | Guest rooms |
| `BATHROOM` | Bathroom | Bathrooms |
| `DINING` | Dining | Restaurant and dining areas |
| `POOL` | Pool | Swimming pool |
| `SPA` | Spa | Spa and wellness facilities |
| `GYM` | Gym | Fitness center |
| `CONFERENCE` | Conference | Conference and meeting rooms |
| `GARDEN` | Garden | Gardens and outdoor spaces |
| `VIEW` | View | Views from the property |
| `AMENITY` | Amenity | Amenities and facilities |
| `ACTIVITY` | Activity | Activities available |
| `NEARBY` | Nearby Attraction | Nearby attractions |
| `FOOD` | Food | Food and beverages |
| `STAFF` | Staff | Staff and service |
| `LOGO` | Logo | Accommodation logo |
| `OTHER` | Other | Other images |

---

## Data Types

### AccommodationImageDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated image ID |
| `accommodationId` | String | Obfuscated accommodation ID |
| `accommodationName` | String | Name of the accommodation |
| `imageUrl` | String | Full URL to the image |
| `fileName` | String | Stored filename |
| `originalFileName` | String | Original uploaded filename |
| `imageType` | Enum | Image type |
| `imageTypeDisplayName` | String | Human-readable type name |
| `imageTypeDescription` | String | Type description |
| `altText` | String | Alt text for accessibility |
| `caption` | String | Image caption |
| `description` | String | Detailed description |
| `isPrimary` | Boolean | Is this the primary image |
| `isActive` | Boolean | Is the image active |
| `displayOrder` | Integer | Display order for sorting |
| `fileSize` | Long | File size in bytes |
| `fileSizeFormatted` | String | Human-readable file size |
| `mimeType` | String | MIME type |
| `width` | Integer | Image width in pixels |
| `height` | Integer | Image height in pixels |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

---

## Configuration

### application.properties

```properties
# Storage path for accommodation images on the filesystem
accommodation.image.storage.path=/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/accommodation-images/

# Base URL (used for constructing full URLs)
app.base.url=http://localhost:4450

# Image URL is constructed as: app.base.url + /api/accommodation-images/{imageId}/file
# Alternative: app.base.url + /api/accommodation-images/file/{fileName}
```

### Image Validation

Image uploads are validated using `ImageSettingGetterServices`:

- **Max file size:** Configured via `image.upload.max.file.size`
- **Allowed formats:** Configured via `image.upload.allowed.formats`
- **Upload enabled:** Configured via `image.upload.enabled`

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data or failed validation |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `NOT_FOUND` | Image or accommodation not found |
| 500 | `STORAGE_ERROR` | Failed to save image file |
| 500 | `DATABASE_ERROR` | Failed to save image record |
| 500 | `DELETE_ERROR` | Failed to delete images |

---

## Audit Logging

All create, update, and delete operations are logged:

| Action | Description |
|--------|-------------|
| `CREATE_ACCOMMODATION_IMAGES` | Images uploaded |
| `UPDATE_ACCOMMODATION_IMAGE` | Image metadata updated |
| `BULK_DELETE_ACCOMMODATION_IMAGES` | Images permanently deleted |
