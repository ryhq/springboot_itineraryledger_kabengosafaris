# Park Activity Image Controller API Documentation

## Base URL
```
/api/park-activity-images
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_PARK_ACTIVITY_IMAGE` - For reading park activity images
- `PERM_CREATE_PARK_ACTIVITY_IMAGE` - For uploading images
- `PERM_UPDATE_PARK_ACTIVITY_IMAGE` - For updating image metadata and reordering
- `PERM_DELETE_PARK_ACTIVITY_IMAGE` - For deleting images

---

## Important Note

**Park-Activity Relationship Must Exist:** Before uploading images for a park-activity combination, the park-activity relationship must already exist in the system. This means the activity must be associated with the park first.

---

## Endpoints

### 1. Get All Images (with filters and pagination)

**Endpoint:** `GET /api/park-activity-images`

**Permission:** `PERM_READ_PARK_ACTIVITY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| parkId | String | No | - | Filter by park (obfuscated ID) |
| activityId | String | No | - | Filter by activity (obfuscated ID) |
| parkName | String | No | - | Filter by park name (partial match) |
| activityName | String | No | - | Filter by activity name (partial match) |
| parkIsActive | Boolean | No | - | Filter by park active status |
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
- `ACTION`, `EQUIPMENT`, `LOCATION`, `SAFETY`, `GROUP`, `GUIDE`, `WILDLIFE`, `SCENIC`, `AERIAL`, `UNDERWATER`, `NIGHT`, `SUNRISE`, `SUNSET`, `PROMOTIONAL`, `THUMBNAIL`, `BANNER`, `GALLERY`, `BEFORE_AFTER`, `INSTRUCTIONAL`, `MEETING_POINT`, `FACILITY`, `OTHER`

**Response:**
```json
{
  "status": 200,
  "message": "Park activity images retrieved successfully",
  "data": {
    "images": [
      {
        "id": "obfuscated_id",
        "parkId": "obfuscated_park_id",
        "parkName": "Serengeti National Park",
        "activityId": "obfuscated_activity_id",
        "activityName": "Game Drive Safari",
        "imageType": "ACTION",
        "imageTypeDisplayName": "Action Shot",
        "imageTypeDescription": "Action photography of activity at this park",
        "imageUrl": "http://localhost:4450/api/park-activity-images/obfuscated_id/file",
        "fileImageUrl": "http://localhost:4450/api/park-activity-images/file/hashed_filename.jpg",
        "fileName": "hashed_filename.jpg",
        "originalFileName": "safari_action.jpg",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "mimeType": "image/jpeg",
        "altText": "Tourists on game drive in Serengeti",
        "caption": "Morning game drive adventure",
        "description": "Captured during sunrise game drive",
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

**Endpoint:** `GET /api/park-activity-images/{id}`

**Permission:** `PERM_READ_PARK_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated image ID |

---

### 3. Get Images by Park-Activity

**Endpoint:** `GET /api/park-activity-images/park/{parkId}/activity/{activityId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |
| activityId | String | Obfuscated activity ID |

---

### 4. Get Images by Park ID

**Endpoint:** `GET /api/park-activity-images/park/{parkId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |

---

### 5. Get Images by Activity ID

**Endpoint:** `GET /api/park-activity-images/activity/{activityId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_IMAGE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| activityId | String | Obfuscated activity ID |

---

### 6. Get Image File by Filename (Public)

**Endpoint:** `GET /api/park-activity-images/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename |

---

### 7. Serve Image File by ID (Public)

**Endpoint:** `GET /api/park-activity-images/{id}/file`

**Permission:** None (Public)

---

### 8. Upload Multiple Images

**Endpoint:** `POST /api/park-activity-images/upload`

**Permission:** `PERM_CREATE_PARK_ACTIVITY_IMAGE`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| images[0].parkId | String | Yes | Obfuscated park ID |
| images[0].activityId | String | Yes | Obfuscated activity ID |
| images[0].image | File | Yes | Image file |
| images[0].imageType | ImageType | No | Image type (defaults to OTHER) |
| images[0].altText | String | No | Alt text for accessibility |
| images[0].caption | String | No | Image caption |
| images[0].description | String | No | Detailed description |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/park-activity-images/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "images[0].parkId=obfuscated_park_id" \
  -F "images[0].activityId=obfuscated_activity_id" \
  -F "images[0].image=@/path/to/image1.jpg" \
  -F "images[0].imageType=ACTION"
```

---

### 9. Update Image Metadata

**Endpoint:** `PUT /api/park-activity-images/{id}`

**Permission:** `PERM_UPDATE_PARK_ACTIVITY_IMAGE`

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

### 10. Reorder Images

**Endpoint:** `POST /api/park-activity-images/reorder`

**Permission:** `PERM_UPDATE_PARK_ACTIVITY_IMAGE`

**Request Body:**
```json
{
  "parkId": "obfuscated_park_id",
  "activityId": "obfuscated_activity_id",
  "imageOrder": [
    "obfuscated_image_id_3",
    "obfuscated_image_id_1",
    "obfuscated_image_id_2"
  ]
}
```

---

### 11. Bulk Delete Images

**Endpoint:** `DELETE /api/park-activity-images`

**Permission:** `PERM_DELETE_PARK_ACTIVITY_IMAGE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated image IDs to delete |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_PARK_ID | The provided park ID is invalid |
| INVALID_ACTIVITY_ID | The provided activity ID is invalid |
| INVALID_ID | The provided park or activity ID is invalid |
| INVALID_IMAGE_ID | The provided image ID is invalid |
| IMAGE_NOT_FOUND | The requested image was not found |
| PARK_ACTIVITY_NOT_FOUND | The park-activity relationship was not found |
| NO_IMAGES_PROVIDED | No images were provided in the upload request |
| NO_IMAGES_FOUND | No images found for this park-activity |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save image to filesystem |
| DATABASE_ERROR | Failed to save image record to database |
| INVALID_IMAGE_ORDER | Image ID does not belong to this park-activity |
| INCOMPLETE_IMAGE_ORDER | Image order list must contain all images |

---

## Storage

Images are stored at the configured path:
```
${park-activity.image.storage.path:./data/park-activity-images/}
```
