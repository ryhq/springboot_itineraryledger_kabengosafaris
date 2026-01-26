# Activity Image Controller API Documentation

## Base URL
```
/api/activity-images
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_ACTIVITY_IMAGE` - For reading activity images
- `PERM_CREATE_ACTIVITY_IMAGE` - For uploading images
- `PERM_UPDATE_ACTIVITY_IMAGE` - For updating image metadata and reordering
- `PERM_DELETE_ACTIVITY_IMAGE` - For deleting images

---

## Endpoints

### 1. Get All Images (with filters and pagination)

**Endpoint:** `GET /api/activity-images`

**Permission:** `PERM_READ_ACTIVITY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| activityId | String | No | - | Filter by activity (obfuscated ID) |
| activityName | String | No | - | Filter by activity name (partial match) |
| activityIsActive | Boolean | No | - | Filter by activity active status |
| hasTariff | Boolean | No | - | Filter by whether activity has tariff |
| imageType | ImageType | No | - | Filter by image type |
| isPrimary | Boolean | No | - | Filter by primary flag |
| isActive | Boolean | No | - | Filter by active status |
| displayOrder | Integer | No | - | Filter by specific display order |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |

**Available ImageType Values:**
- `ACTION`, `EQUIPMENT`, `LOCATION`, `SAFETY`, `GROUP`, `GUIDE`, `WILDLIFE`, `SCENIC`, `AERIAL`, `UNDERWATER`, `NIGHT`, `SUNRISE`, `SUNSET`, `PROMOTIONAL`, `THUMBNAIL`, `BANNER`, `GALLERY`, `BEFORE_AFTER`, `INSTRUCTIONAL`, `CERTIFICATE`, `OTHER`

**Response:**
```json
{
  "status": 200,
  "message": "Activity images retrieved successfully",
  "data": {
    "images": [
      {
        "id": "obfuscated_id",
        "activityId": "obfuscated_activity_id",
        "activityName": "Game Drive Safari",
        "imageType": "ACTION",
        "imageTypeDisplayName": "Action Shot",
        "imageTypeDescription": "Action and activity photography",
        "imageUrl": "http://localhost:4450/api/activity-images/obfuscated_id/file",
        "fileImageUrl": "http://localhost:4450/api/activity-images/file/hashed_filename.jpg",
        "fileName": "hashed_filename.jpg",
        "originalFileName": "safari_action.jpg",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "mimeType": "image/jpeg",
        "altText": "Tourists on game drive",
        "caption": "Exciting safari experience",
        "description": "Captured during morning game drive",
        "isPrimary": true,
        "isActive": true,
        "displayOrder": 1,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalPages": 5,
    "totalItems": 100,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. Get Image by ID

**Endpoint:** `GET /api/activity-images/{id}`

**Permission:** `PERM_READ_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

---

### 3. Get Images by Activity ID

**Endpoint:** `GET /api/activity-images/activity/{activityId}`

**Permission:** `PERM_READ_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| activityId | String | Obfuscated activity ID |

---

### 4. Get Image File by Filename (Public)

**Endpoint:** `GET /api/activity-images/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename |

---

### 5. Serve Image File by ID (Public)

**Endpoint:** `GET /api/activity-images/{id}/file`

**Permission:** None (Public)

---

### 6. Upload Multiple Images

**Endpoint:** `POST /api/activity-images/upload`

**Permission:** `PERM_CREATE_ACTIVITY_IMAGE`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| images[0].activityId | String | Yes | Obfuscated activity ID |
| images[0].image | File | Yes | Image file |
| images[0].imageType | ImageType | No | Image type (defaults to OTHER) |
| images[0].altText | String | No | Alt text for accessibility |
| images[0].caption | String | No | Image caption |
| images[0].description | String | No | Detailed description |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/activity-images/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "images[0].activityId=obfuscated_activity_id" \
  -F "images[0].image=@/path/to/image1.jpg" \
  -F "images[0].imageType=ACTION"
```

---

### 7. Update Image Metadata

**Endpoint:** `PUT /api/activity-images/{id}`

**Permission:** `PERM_UPDATE_ACTIVITY_IMAGE`

**Request Body:**
```json
{
  "imageType": "ACTION",
  "altText": "Updated alt text",
  "caption": "Updated caption",
  "description": "Updated description",
  "isPrimary": true,
  "isActive": true
}
```

---

### 8. Reorder Images

**Endpoint:** `POST /api/activity-images/reorder`

**Permission:** `PERM_UPDATE_ACTIVITY_IMAGE`

**Request Body:**
```json
{
  "activityId": "obfuscated_activity_id",
  "imageOrder": [
    "obfuscated_image_id_3",
    "obfuscated_image_id_1",
    "obfuscated_image_id_2"
  ]
}
```

---

### 9. Bulk Delete Images

**Endpoint:** `DELETE /api/activity-images`

**Permission:** `PERM_DELETE_ACTIVITY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated image IDs to delete |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_ACTIVITY_ID | The provided activity ID is invalid |
| INVALID_IMAGE_ID | The provided image ID is invalid |
| IMAGE_NOT_FOUND | The requested image was not found |
| ACTIVITY_NOT_FOUND | The referenced activity was not found |
| NO_IMAGES_PROVIDED | No images were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save image to filesystem |
| DATABASE_ERROR | Failed to save image record to database |

---

## Storage

Images are stored at the configured path:
```
${activity.image.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/activity-images/}
```
