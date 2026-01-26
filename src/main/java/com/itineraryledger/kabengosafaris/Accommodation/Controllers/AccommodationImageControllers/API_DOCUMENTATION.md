# Accommodation Image API Documentation

## Overview

The Accommodation Image API provides endpoints for managing images associated with accommodations. Images are stored on the filesystem with SHA-256 hashed filenames, while only the filename is stored in the database. The full URL is constructed dynamically using the configured base URL.

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
| POST (reorder) | `PERM_UPDATE_ACCOMMODATION_IMAGE` |
| DELETE | `PERM_DELETE_ACCOMMODATION_IMAGE` |

---

## Image Storage

- **Storage Path:** Configured via `accommodation.image.storage.path` in application.properties
- **URL Construction:** `app.base.url` + `/api/accommodation-images/{obfuscatedId}/file`
- **Alternative URL:** `app.base.url` + `/api/accommodation-images/file/{fileName}`
- **Filename Format:** SHA-256 hash + timestamp (e.g., `f2e5a046548d723b59874286c9d528188b50881b215a49341b03998d7f2d00eb_1701304567890.jpg`)
- **Validation:** Uses `FileSettingGetterServices` for file size and `ImageSettingGetterServices` for format validation

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
| `imageType` | Enum | No | - | Filter by image type (EXTERIOR, ROOM, etc.) |
| `isPrimary` | Boolean | No | - | Filter by primary status |
| `isActive` | Boolean | No | - | Filter by active status |
| `displayOrder` | Integer | No | - | Filter by display order (exact match) |
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
        "fileName": "f2e5a046548d723b_1701304567890.jpg",
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
    "fileName": "f2e5a046548d723b_1701304567890.jpg",
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

**Example:** `GET /api/accommodation-images/file/f2e5a046548d723b_1701304567890.jpg`

**Response:** Binary image data with appropriate Content-Type header

**Headers Returned:**
- `Content-Type`: Image MIME type (e.g., `image/jpeg`, `image/png`)
- `Content-Length`: File size in bytes
- `Cache-Control`: `public, max-age=86400` (1 day caching)

**Usage:**
```html
<img src="http://localhost:4450/api/accommodation-images/file/f2e5a046548d723b_1701304567890.jpg" alt="Accommodation Image" />
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

Upload multiple images for accommodations in a single request.

**Endpoint:** `POST /api/accommodation-images/upload`

**Content-Type:** `multipart/form-data`

**Permission:** `PERM_CREATE_ACCOMMODATION_IMAGE`

#### Request Body (UploadAccommodationImagesDTO)

The request uses a wrapper DTO with an `images` array. Each item in the `images` array should contain:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `image` | File | Yes | The image file to upload |
| `imageType` | Enum | No | Image type (defaults to `OTHER`) |
| `altText` | String | No | Alt text for accessibility |
| `caption` | String | No | Image caption |
| `description` | String | No | Detailed description |

#### Example Request (cURL)

```bash
curl -X POST "http://localhost:4450/api/accommodation-images/upload" \
  -H "Authorization: Bearer <jwt_token>" \
  -F "images[0].accommodationId=abc123" \
  -F "images[0].image=@/path/to/image1.jpg" \
  -F "images[0].imageType=EXTERIOR" \
  -F "images[0].altText=Front view" \
  -F "images[1].accommodationId=abc123" \
  -F "images[1].image=@/path/to/image2.jpg" \
  -F "images[1].imageType=ROOM"
```

#### Response

```json
{
  "success": true,
  "status": 201,
  "message": "2 image(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "accommodationId": "abc123",
      "accommodationName": "Serengeti Safari Lodge",
      "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id_1/file",
      "fileName": "f2e5a046548d723b_1701304567890.jpg",
      "originalFileName": "image1.jpg",
      "imageType": "EXTERIOR",
      "altText": "Front view",
      "isPrimary": false,
      "isActive": true,
      "displayOrder": 1
    },
    {
      "id": "obfuscated_id_2",
      "accommodationId": "abc123",
      "accommodationName": "Serengeti Safari Lodge",
      "imageUrl": "http://localhost:4450/api/accommodation-images/obfuscated_id_2/file",
      "fileName": "a1b2c3d4e5f6g7h8_1701304567891.jpg",
      "originalFileName": "image2.jpg",
      "imageType": "ROOM",
      "isPrimary": false,
      "isActive": true,
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
  "message": "Validation failed: Image 1: Accommodation ID is required; Image 2 (large.jpg): File size (15.0 MB) exceeds maximum allowed size (10MB)",
  "errorCode": "VALIDATION_ERROR"
}
```

---

### 6. Update Image Metadata

Update image metadata (not the image file itself). To replace the actual image file, delete the old image and upload a new one.

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
  "isActive": true
}
```

All fields are optional. Only provided fields will be updated.

**Note:**
- Setting `isPrimary` to `true` will automatically unset the primary flag on other images for the same accommodation.
- `displayOrder` is managed via the reorder endpoint and cannot be updated directly.

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Accommodation image updated successfully",
  "data": {
    "id": "obfuscated_id",
    "imageType": "ROOM",
    "isPrimary": true,
    "isActive": true
  }
}
```

---

### 7. Reorder Images

Reorder images within an accommodation using drag-and-drop.

**Endpoint:** `POST /api/accommodation-images/reorder`

**Permission:** `PERM_UPDATE_ACCOMMODATION_IMAGE`

#### Request Body

```json
{
  "accommodationId": "obfuscated_accommodation_id",
  "imageOrder": [
    { "imageId": "obfuscated_image_id_3", "expectedDisplayOrder": 1 },
    { "imageId": "obfuscated_image_id_1", "expectedDisplayOrder": 2 },
    { "imageId": "obfuscated_image_id_2", "expectedDisplayOrder": 3 }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `imageOrder` | Array | Yes | List of image order items |
| `imageOrder[].imageId` | String | Yes | Obfuscated image ID |
| `imageOrder[].expectedDisplayOrder` | Integer | No | Expected position (for validation) |

**Important Notes:**
- The `imageOrder` list must contain ALL image IDs for the accommodation
- The position in the list determines the new `displayOrder` (1-indexed)
- First item becomes `displayOrder` 1, second becomes 2, etc.
- `expectedDisplayOrder` is optional and used for validation only

#### Example Request

```bash
curl -X POST "http://localhost:4450/api/accommodation-images/reorder" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "accommodationId": "abc123",
    "imageOrder": [
      { "imageId": "img3" },
      { "imageId": "img1" },
      { "imageId": "img2" }
    ]
  }'
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Images reordered successfully",
  "data": [
    {
      "id": "img3",
      "displayOrder": 1
    },
    {
      "id": "img1",
      "displayOrder": 2
    },
    {
      "id": "img2",
      "displayOrder": 3
    }
  ]
}
```

#### Error Responses

```json
{
  "success": false,
  "status": 400,
  "message": "Image order list must contain exactly 5 images. Received: 3",
  "errorCode": "IMAGE_COUNT_MISMATCH"
}
```

```json
{
  "success": false,
  "status": 400,
  "message": "Missing image ID(s) in reorder list: img4, img5",
  "errorCode": "MISSING_IMAGE_IDS"
}
```

---

### 8. Bulk Delete Images

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
  "message": "3 accommodation image(s) deleted successfully",
  "data": null
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
| `imageUrl` | String | Full URL to the image using ID (`/api/accommodation-images/{id}/file`) |
| `fileImageUrl` | String | Full URL to the image using filename (`/api/accommodation-images/file/{fileName}`) |
| `fileName` | String | Stored filename (SHA-256 hash format) |
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

### UploadAccommodationImagesDTO (Wrapper)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `images` | Array | Yes | List of CreateAccommodationImageDTO |

### CreateAccommodationImageDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `image` | MultipartFile | Yes | The image file |
| `imageType` | Enum | No | Image type (defaults to OTHER) |
| `altText` | String | No | Alt text for accessibility |
| `caption` | String | No | Image caption |
| `description` | String | No | Detailed description |

### UpdateAccommodationImageDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `imageType` | Enum | No | Image type |
| `altText` | String | No | Alt text for accessibility |
| `caption` | String | No | Image caption |
| `description` | String | No | Detailed description |
| `isPrimary` | Boolean | No | Is this the primary image |
| `isActive` | Boolean | No | Is the image active |

### ReorderAccommodationImagesDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `imageOrder` | Array | Yes | List of ImageOrderItem |

### ImageOrderItem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `imageId` | String | Yes | Obfuscated image ID |
| `expectedDisplayOrder` | Integer | No | Expected position (for validation) |

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

# File upload size limits (Spring Multipart)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

### Image Validation

Image uploads are validated using:

- **Max file size:** `spring.servlet.multipart.max-file-size` (default: 10MB)
- **Max request size:** `spring.servlet.multipart.max-request-size` (default: 50MB)
- **Allowed formats:** Configured via `ImageSettingGetterServices`
- **Upload enabled:** Configured via `image.upload.enabled`

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `NO_IMAGES_PROVIDED` | No images in upload request |
| 400 | `REQUEST_SIZE_EXCEEDED` | Total request size exceeds limit |
| 400 | `VALIDATION_ERROR` | Invalid request data or failed validation |
| 400 | `INVALID_ACCOMMODATION_ID` | Invalid accommodation ID format |
| 400 | `INVALID_IMAGE_ID` | Invalid image ID format |
| 400 | `INVALID_IMAGE_ID_FORMAT` | Invalid image ID format in reorder |
| 400 | `DUPLICATE_IMAGE_IDS` | Duplicate image IDs in reorder list |
| 400 | `IMAGE_COUNT_MISMATCH` | Reorder list count doesn't match |
| 400 | `IMAGE_ACCOMMODATION_MISMATCH` | Image doesn't belong to accommodation |
| 400 | `MISSING_IMAGE_IDS` | Missing image IDs in reorder list |
| 400 | `EXPECTED_ORDER_MISMATCH` | Expected display order doesn't match position |
| 400 | `NO_IDS_PROVIDED` | No image IDs provided for delete |
| 400 | `NO_IMAGES_TO_REORDER` | Accommodation has no images |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `ACCOMMODATION_NOT_FOUND` | Accommodation not found |
| 404 | `IMAGE_NOT_FOUND` | Image not found |
| 500 | `STORAGE_ERROR` | Failed to save image file |
| 500 | `DATABASE_ERROR` | Failed to save image record |
| 500 | `IMAGE_UPDATE_FAILED` | Failed to update image |
| 500 | `ACCOMMODATION_IMAGES_REORDER_FAILED` | Failed to reorder images |
| 500 | `ACCOMMODATION_IMAGE_DELETE_FAILED` | Failed to delete images |

---

## Audit Logging

All create, update, reorder, and delete operations are logged:

| Action | Description |
|--------|-------------|
| `CREATE_ACCOMMODATION_IMAGES` | Images uploaded |
| `UPDATE_ACCOMMODATION_IMAGE` | Image metadata updated |
| `REORDER_ACCOMMODATION_IMAGES` | Images reordered |
| `DELETE_ACCOMMODATION_IMAGE` | Image permanently deleted |
