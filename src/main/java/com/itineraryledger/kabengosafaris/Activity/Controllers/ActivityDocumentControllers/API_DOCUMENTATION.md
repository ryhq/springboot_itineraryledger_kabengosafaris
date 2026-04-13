# Activity Document Controller API Documentation

## Base URL
```
/api/activity-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_ACTIVITY_DOCUMENT` - For reading activity documents
- `PERM_CREATE_ACTIVITY_DOCUMENT` - For uploading documents
- `PERM_UPDATE_ACTIVITY_DOCUMENT` - For updating document metadata
- `PERM_DELETE_ACTIVITY_DOCUMENT` - For deleting documents

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/activity-documents`

**Permission:** `PERM_READ_ACTIVITY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| activityId | String | No | - | Filter by activity (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| title | String | No | - | Filter by title (partial match) |
| version | String | No | - | Filter by version (partial match) |
| currentlyValid | Boolean | No | - | Filter only currently valid documents |
| activityName | String | No | - | Filter by activity name (partial match) |
| activityIsActive | Boolean | No | - | Filter by activity active status |
| hasTariff | Boolean | No | - | Filter by whether activity has tariff |
| safetyDocumentsOnly | Boolean | No | - | Filter only safety-related documents |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `SAFETY_GUIDELINES`, `WAIVER`, `LIABILITY_FORM`, `EQUIPMENT_CHECKLIST`, `TRAINING_MATERIAL`, `CERTIFICATION`, `PERMIT`, `INSURANCE`, `MEDICAL_FORM`, `EMERGENCY_PROCEDURE`, `BRIEFING`, `ITINERARY`, `MAP`, `BROCHURE`, `PRICE_LIST`, `TERMS_CONDITIONS`, `FAQ`, `GUIDE`, `MAINTENANCE`, `INSPECTION`, `INCIDENT_REPORT`, `POLICY`, `OTHER`

**Response:**
```json
{
  "status": 200,
  "message": "Activity documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "activityId": "obfuscated_activity_id",
        "activityName": "Bungee Jumping",
        "title": "Safety Guidelines 2024",
        "documentType": "SAFETY_GUIDELINES",
        "documentTypeDisplayName": "Safety Guidelines",
        "documentTypeDescription": "Safety instructions and guidelines",
        "documentUrl": "http://localhost:4450/api/activity-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/activity-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "bungee_safety_guide.pdf",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "fileType": "pdf",
        "description": "Complete safety guidelines for bungee jumping",
        "version": "2024.1",
        "notes": "Updated with new equipment specifications",
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
    "totalElements": 100,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. Get Document by ID

**Endpoint:** `GET /api/activity-documents/{id}`

**Permission:** `PERM_READ_ACTIVITY_DOCUMENT`

---

### 3. Get Documents by Activity ID

**Endpoint:** `GET /api/activity-documents/activity/{activityId}`

**Permission:** `PERM_READ_ACTIVITY_DOCUMENT`

---

### 4. Get Document File by Filename (Public)

**Endpoint:** `GET /api/activity-documents/file/{fileName}`

**Permission:** None (Public)

---

### 5. Serve Document File by ID (Public)

**Endpoint:** `GET /api/activity-documents/{id}/file`

**Permission:** None (Public)

---

### 6. Upload Multiple Documents

**Endpoint:** `POST /api/activity-documents/upload`

**Permission:** `PERM_CREATE_ACTIVITY_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].activityId | String | Yes | Obfuscated activity ID |
| documents[0].document | File | Yes | Document file |
| documents[0].title | String | Yes | Document title |
| documents[0].documentType | DocumentType | No | Document type (defaults to OTHER) |
| documents[0].description | String | No | Document description |
| documents[0].version | String | No | Version string |
| documents[0].validFrom | DateTime | No | Validity start date (ISO 8601) |
| documents[0].validTo | DateTime | No | Validity end date (ISO 8601) |
| documents[0].notes | String | No | Additional notes |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/activity-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].activityId=obfuscated_activity_id" \
  -F "documents[0].document=@/path/to/safety_guide.pdf" \
  -F "documents[0].title=Safety Guidelines 2024" \
  -F "documents[0].documentType=SAFETY_GUIDELINES" \
  -F "documents[0].version=2024.1"
```

---

### 7. Update Document Metadata

**Endpoint:** `PUT /api/activity-documents/{id}`

**Permission:** `PERM_UPDATE_ACTIVITY_DOCUMENT`

**Request Body:**
```json
{
  "title": "Updated Safety Guidelines",
  "documentType": "SAFETY_GUIDELINES",
  "description": "Updated description",
  "version": "2024.2",
  "notes": "Updated notes",
  "validFrom": "2024-01-01T00:00:00",
  "validTo": "2024-12-31T23:59:59",
  "isActive": true
}
```

---

### 8. Delete Single Document

**Endpoint:** `DELETE /api/activity-documents/{id}`

**Permission:** `PERM_DELETE_ACTIVITY_DOCUMENT`

---

### 9. Bulk Delete Documents

**Endpoint:** `DELETE /api/activity-documents`

**Permission:** `PERM_DELETE_ACTIVITY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_ACTIVITY_ID | The provided activity ID is invalid |
| INVALID_DOCUMENT_ID | The provided document ID is invalid |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| ACTIVITY_NOT_FOUND | The referenced activity was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |

---

## Safety Document Filter

The `safetyDocumentsOnly=true` parameter filters for these document types:
- `SAFETY_GUIDELINES`
- `WAIVER`
- `LIABILITY_FORM`
- `EMERGENCY_PROCEDURE`

---

## Storage

Documents are stored at the configured path:
```
${activity.document.storage.path:./data/activity-documents/}
```
