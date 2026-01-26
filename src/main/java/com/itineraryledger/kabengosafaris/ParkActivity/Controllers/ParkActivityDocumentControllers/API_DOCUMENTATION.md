# Park Activity Document Controller API Documentation

## Base URL
```
/api/park-activity-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_PARK_ACTIVITY_DOCUMENT` - For reading park activity documents
- `PERM_CREATE_PARK_ACTIVITY_DOCUMENT` - For uploading documents
- `PERM_UPDATE_PARK_ACTIVITY_DOCUMENT` - For updating document metadata
- `PERM_DELETE_PARK_ACTIVITY_DOCUMENT` - For deleting documents

---

## Important Note

**Park-Activity Relationship Must Exist:** Before uploading documents for a park-activity combination, the park-activity relationship must already exist in the system. This means the activity must be associated with the park first.

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/park-activity-documents`

**Permission:** `PERM_READ_PARK_ACTIVITY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| parkId | String | No | - | Filter by park (obfuscated ID) |
| activityId | String | No | - | Filter by activity (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| title | String | No | - | Filter by title (partial match) |
| version | String | No | - | Filter by version |
| currentlyValid | Boolean | No | - | Filter for currently valid documents only |
| parkName | String | No | - | Filter by park name (partial match) |
| activityName | String | No | - | Filter by activity name (partial match) |
| parkIsActive | Boolean | No | - | Filter by park active status |
| activityIsActive | Boolean | No | - | Filter by activity active status |
| hasTariff | Boolean | No | - | Filter by whether activity has tariff |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `SAFETY_GUIDELINES`, `WAIVER`, `LIABILITY_FORM`, `PARK_RULES`, `EQUIPMENT_CHECKLIST`, `TRAINING_MATERIAL`, `CERTIFICATION`, `PERMIT`, `INSURANCE`, `MEDICAL_FORM`, `EMERGENCY_PROCEDURE`, `BRIEFING`, `ITINERARY`, `MAP`, `MEETING_POINT`, `BROCHURE`, `PRICE_LIST`, `TERMS_CONDITIONS`, `FAQ`, `GUIDE`, `WILDLIFE_CHECKLIST`, `SEASONAL_INFO`, `INCIDENT_REPORT`, `POLICY`, `OTHER`

**Response:**
```json
{
  "status": 200,
  "message": "Park activity documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "parkId": "obfuscated_park_id",
        "parkName": "Serengeti National Park",
        "activityId": "obfuscated_activity_id",
        "activityName": "Game Drive Safari",
        "title": "Serengeti Game Drive Safety Guidelines",
        "documentType": "SAFETY_GUIDELINES",
        "documentTypeDisplayName": "Safety Guidelines",
        "documentTypeDescription": "Park-specific safety instructions for this activity",
        "documentUrl": "http://localhost:4450/api/park-activity-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/park-activity-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "safety_guidelines_serengeti.pdf",
        "fileSize": 524288,
        "fileSizeFormatted": "512.0 KB",
        "fileType": "application/pdf",
        "description": "Complete safety guidelines for game drives in Serengeti",
        "version": "2.0",
        "notes": "Updated for 2024 season",
        "validFrom": "2024-01-01T00:00:00",
        "validTo": "2024-12-31T23:59:59",
        "isCurrentlyValid": true,
        "isActive": true,
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

### 2. Get Document by ID

**Endpoint:** `GET /api/park-activity-documents/{id}`

**Permission:** `PERM_READ_PARK_ACTIVITY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

---

### 3. Get Documents by Park-Activity

**Endpoint:** `GET /api/park-activity-documents/park/{parkId}/activity/{activityId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |
| activityId | String | Obfuscated activity ID |

---

### 4. Get Documents by Park ID

**Endpoint:** `GET /api/park-activity-documents/park/{parkId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |

---

### 5. Get Documents by Activity ID

**Endpoint:** `GET /api/park-activity-documents/activity/{activityId}`

**Permission:** `PERM_READ_PARK_ACTIVITY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| activityId | String | Obfuscated activity ID |

---

### 6. Get Document File by Filename (Public)

**Endpoint:** `GET /api/park-activity-documents/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename |

---

### 7. Serve Document File by ID (Public)

**Endpoint:** `GET /api/park-activity-documents/{id}/file`

**Permission:** None (Public)

---

### 8. Upload Multiple Documents

**Endpoint:** `POST /api/park-activity-documents/upload`

**Permission:** `PERM_CREATE_PARK_ACTIVITY_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].parkId | String | Yes | Obfuscated park ID |
| documents[0].activityId | String | Yes | Obfuscated activity ID |
| documents[0].document | File | Yes | Document file |
| documents[0].title | String | Yes | Document title |
| documents[0].documentType | DocumentType | Yes | Type of document |
| documents[0].description | String | No | Document description |
| documents[0].version | String | No | Document version |
| documents[0].validFrom | DateTime | No | Validity start date |
| documents[0].validTo | DateTime | No | Validity end date |
| documents[0].notes | String | No | Additional notes |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/park-activity-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].parkId=obfuscated_park_id" \
  -F "documents[0].activityId=obfuscated_activity_id" \
  -F "documents[0].document=@/path/to/document.pdf" \
  -F "documents[0].title=Serengeti Game Drive Safety Guidelines" \
  -F "documents[0].documentType=SAFETY_GUIDELINES" \
  -F "documents[0].version=2.0"
```

---

### 9. Update Document Metadata

**Endpoint:** `PUT /api/park-activity-documents/{id}`

**Permission:** `PERM_UPDATE_PARK_ACTIVITY_DOCUMENT`

**Request Body:**
```json
{
  "title": "Updated Title",
  "documentType": "SAFETY_GUIDELINES",
  "description": "Updated description",
  "version": "2.1",
  "notes": "Updated notes",
  "validFrom": "2024-01-01T00:00:00",
  "validTo": "2024-12-31T23:59:59",
  "isActive": true
}
```

---

### 10. Delete Single Document

**Endpoint:** `DELETE /api/park-activity-documents/{id}`

**Permission:** `PERM_DELETE_PARK_ACTIVITY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

---

### 11. Bulk Delete Documents

**Endpoint:** `DELETE /api/park-activity-documents`

**Permission:** `PERM_DELETE_PARK_ACTIVITY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_PARK_ID | The provided park ID is invalid |
| INVALID_ACTIVITY_ID | The provided activity ID is invalid |
| INVALID_ID | The provided park or activity ID is invalid |
| INVALID_DOCUMENT_ID | The provided document ID is invalid |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| PARK_ACTIVITY_NOT_FOUND | The park-activity relationship was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |

---

## Storage

Documents are stored at the configured path:
```
${park-activity.document.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/park-activity-documents/}
```
