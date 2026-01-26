# Park Image Controller API Documentation

## Base URL
```
/api/park-images
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_PARK_IMAGE` - For reading park images
- `PERM_CREATE_PARK_IMAGE` - For uploading images
- `PERM_UPDATE_PARK_IMAGE` - For updating image metadata and reordering
- `PERM_DELETE_PARK_IMAGE` - For deleting images

---

## Endpoints

### 1. Get All Images (with filters and pagination)

**Endpoint:** `GET /api/park-images`

**Permission:** `PERM_READ_PARK_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| parkId | String | No | - | Filter by park (obfuscated ID) |
| parkName | String | No | - | Filter by park name (partial match) |
| parkType | ParkType | No | - | Filter by park type |
| region | String | No | - | Filter by region (partial match) |
| imageType | ImageType | No | - | Filter by image type |
| isPrimary | Boolean | No | - | Filter by primary flag |
| isActive | Boolean | No | - | Filter by active status |
| displayOrder | Integer | No | - | Filter by specific display order |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |

**Available ParkType Values:**
- `NATIONAL_PARK`
- `GAME_RESERVE`
- `CONSERVATION_AREA`
- `WILDLIFE_RESERVE`
- `MARINE_PARK`
- `FOREST_RESERVE`
- `BIRD_SANCTUARY`
- `PRIVATE_RESERVE`
- `OTHER`

**Available ImageType Values:**
- `LANDSCAPE`, `WILDLIFE`, `BIRD`, `ENTRANCE`, `VIEWPOINT`, `TRAIL`, `CAMPSITE`, `ACCOMMODATION`, `VISITOR_CENTER`, `GATE`, `FACILITY`, `AERIAL`, `PANORAMIC`, `SUNSET`, `SUNRISE`, `WATERFALL`, `RIVER`, `LAKE`, `HISTORICAL`, `MAP`, `OTHER`

**Response:**
```json
{
  "status": 200,
  "message": "Park images retrieved successfully",
  "data": {
    "images": [
      {
        "id": "obfuscated_id",
        "parkId": "obfuscated_park_id",
        "parkName": "Serengeti National Park",
        "imageType": "LANDSCAPE",
        "imageTypeDisplayName": "Landscape",
        "imageTypeDescription": "Scenic landscape photography",
        "imageUrl": "http://localhost:4450/api/park-images/obfuscated_id/file",
        "fileImageUrl": "http://localhost:4450/api/park-images/file/hashed_filename.jpg",
        "fileName": "hashed_filename.jpg",
        "originalFileName": "serengeti_sunrise.jpg",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "mimeType": "image/jpeg",
        "altText": "Sunrise over Serengeti plains",
        "caption": "Beautiful sunrise",
        "description": "Captured at dawn",
        "isPrimary": true,
        "isActive": true,
        "displayOrder": 1,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 100,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. Get Image by ID

**Endpoint:** `GET /api/park-images/{id}`

**Permission:** `PERM_READ_PARK_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Response:**
```json
{
  "status": 200,
  "message": "Park image retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "parkId": "obfuscated_park_id",
    "parkName": "Serengeti National Park",
    "imageType": "WILDLIFE",
    "imageTypeDisplayName": "Wildlife",
    "imageTypeDescription": "Wildlife and animal photography",
    "imageUrl": "http://localhost:4450/api/park-images/obfuscated_id/file",
    "fileImageUrl": "http://localhost:4450/api/park-images/file/hashed_filename.jpg",
    "fileName": "hashed_filename.jpg",
    "originalFileName": "lion_pride.jpg",
    "fileSize": 2345678,
    "fileSizeFormatted": "2.2 MB",
    "mimeType": "image/jpeg",
    "altText": "Lion pride at Serengeti",
    "caption": "A pride of lions resting",
    "description": "Photographed during afternoon game drive",
    "isPrimary": false,
    "isActive": true,
    "displayOrder": 2,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 3. Get Images by Park ID

**Endpoint:** `GET /api/park-images/park/{parkId}`

**Permission:** `PERM_READ_PARK_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |

**Response:**
```json
{
  "status": 200,
  "message": "Park images retrieved successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "imageType": "LANDSCAPE",
      "displayOrder": 1,
      ...
    },
    {
      "id": "obfuscated_id_2",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "imageType": "WILDLIFE",
      "displayOrder": 2,
      ...
    }
  ]
}
```

---

### 4. Get Image File by Filename (Public)

**Endpoint:** `GET /api/park-images/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename (e.g., `abc123_1701304567890.jpg`) |

**Response:**
- Returns the actual image binary with appropriate `Content-Type` header
- Returns `404 Not Found` if image doesn't exist or is inactive

**Headers:**
```
Content-Type: image/jpeg (or appropriate MIME type)
Content-Length: 1234567
Cache-Control: public, max-age=86400
```

---

### 5. Serve Image File by ID (Public)

**Endpoint:** `GET /api/park-images/{id}/file`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Response:**
- Returns the actual image binary with appropriate `Content-Type` header
- Returns `400 Bad Request` for invalid ID
- Returns `404 Not Found` if image doesn't exist or is inactive

---

### 6. Upload Multiple Images

**Endpoint:** `POST /api/park-images/upload`

**Permission:** `PERM_CREATE_PARK_IMAGE`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| images[0].parkId | String | Yes | Obfuscated park ID |
| images[0].image | File | Yes | Image file |
| images[0].imageType | ImageType | No | Image type (defaults to OTHER) |
| images[0].altText | String | No | Alt text for accessibility |
| images[0].caption | String | No | Image caption |
| images[0].description | String | No | Detailed description |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/park-images/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "images[0].parkId=obfuscated_park_id" \
  -F "images[0].image=@/path/to/image1.jpg" \
  -F "images[0].imageType=LANDSCAPE" \
  -F "images[0].altText=Serengeti landscape" \
  -F "images[1].parkId=obfuscated_park_id" \
  -F "images[1].image=@/path/to/image2.jpg" \
  -F "images[1].imageType=WILDLIFE"
```

**Success Response:**
```json
{
  "status": 201,
  "message": "2 image(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "imageType": "LANDSCAPE",
      "displayOrder": 1,
      ...
    },
    {
      "id": "obfuscated_id_2",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "imageType": "WILDLIFE",
      "displayOrder": 2,
      ...
    }
  ]
}
```

**Error Response (Validation):**
```json
{
  "status": 400,
  "message": "Validation failed: Image 1: Park ID is required; Image 2 (invalid.exe): File type not allowed",
  "errorCode": "VALIDATION_ERROR"
}
```

---

### 7. Update Image Metadata

**Endpoint:** `PUT /api/park-images/{id}`

**Permission:** `PERM_UPDATE_PARK_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

**Request Body:**
```json
{
  "imageType": "WILDLIFE",
  "altText": "Updated alt text",
  "caption": "Updated caption",
  "description": "Updated description",
  "isPrimary": true,
  "isActive": true
}
```

All fields are optional. Only provided fields will be updated.

**Note:** Setting `isPrimary: true` will automatically unset any existing primary image for the same park.

**Response:**
```json
{
  "status": 200,
  "message": "Park image updated successfully",
  "data": {
    "id": "obfuscated_id",
    "parkId": "obfuscated_park_id",
    "imageType": "WILDLIFE",
    "isPrimary": true,
    ...
  }
}
```

---

### 8. Reorder Images

**Endpoint:** `POST /api/park-images/reorder`

**Permission:** `PERM_UPDATE_PARK_IMAGE`

**Request Body:**
```json
{
  "parkId": "obfuscated_park_id",
  "imageOrder": [
    "obfuscated_image_id_3",
    "obfuscated_image_id_1",
    "obfuscated_image_id_2"
  ]
}
```

The `imageOrder` array must contain ALL image IDs for the park. The position in the array determines the new `displayOrder` (1-indexed).

**Response:**
```json
{
  "status": 200,
  "message": "Images reordered successfully",
  "data": [
    {
      "id": "obfuscated_image_id_3",
      "displayOrder": 1,
      ...
    },
    {
      "id": "obfuscated_image_id_1",
      "displayOrder": 2,
      ...
    },
    {
      "id": "obfuscated_image_id_2",
      "displayOrder": 3,
      ...
    }
  ]
}
```

---

### 9. Bulk Delete Images

**Endpoint:** `DELETE /api/park-images`

**Permission:** `PERM_DELETE_PARK_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated image IDs to delete |

**Example:**
```
DELETE /api/park-images?ids=id1,id2,id3
```

**Response:**
```json
{
  "status": 200,
  "message": "3 image(s) deleted successfully",
  "data": {
    "deletedIds": ["id1", "id2", "id3"],
    "failedIds": []
  }
}
```

**Partial Success Response:**
```json
{
  "status": 200,
  "message": "2 image(s) deleted successfully, 1 failed",
  "data": {
    "deletedIds": ["id1", "id2"],
    "failedIds": ["id3"]
  }
}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_PARK_ID | The provided park ID is invalid or cannot be decoded |
| INVALID_IMAGE_ID | The provided image ID is invalid or cannot be decoded |
| IMAGE_NOT_FOUND | The requested image was not found |
| PARK_NOT_FOUND | The referenced park was not found |
| NO_IMAGES_PROVIDED | No images were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save image to filesystem |
| DATABASE_ERROR | Failed to save image record to database |
| IMAGE_FETCH_FAILED | Failed to fetch park images |
| IMAGE_UPDATE_FAILED | Failed to update park image |
| IMAGE_DELETE_FAILED | Failed to delete park image |
| BULK_DELETE_FAILED | Failed to perform bulk delete operation |

---

## File Validation

Images are validated against the following criteria (configurable via FileSettings):

- **Allowed Extensions:** jpg, jpeg, png, gif, webp, etc. (as configured)
- **Maximum File Size:** Configurable (default varies)
- **Maximum Request Size:** Configurable for bulk uploads

---

## Storage

Images are stored at the configured path:
```
${park.image.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/park-images/}
```

Filenames are generated using SHA-256 hash + timestamp to ensure uniqueness and prevent conflicts.
