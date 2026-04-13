# Park Document Controller API Documentation

## Base URL
```
/api/park-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_PARK_DOCUMENT` - For reading park documents
- `PERM_CREATE_PARK_DOCUMENT` - For uploading documents
- `PERM_UPDATE_PARK_DOCUMENT` - For updating document metadata
- `PERM_DELETE_PARK_DOCUMENT` - For deleting documents

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/park-documents`

**Permission:** `PERM_READ_PARK_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| parkId | String | No | - | Filter by park (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| title | String | No | - | Filter by title (partial match) |
| version | String | No | - | Filter by version (partial match) |
| currentlyValid | Boolean | No | - | Filter only currently valid documents |
| parkName | String | No | - | Filter by park name (partial match) |
| parkType | ParkType | No | - | Filter by park type |
| region | String | No | - | Filter by region (partial match) |
| tariffDocumentsOnly | Boolean | No | - | Filter only TARIFF and FEE_SCHEDULE types |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `TARIFF` - Tariff Document
- `FEE_SCHEDULE` - Fee Schedule
- `REGULATION` - Park Rules and Regulations
- `PERMIT` - Permits and Authorization
- `MAP` - Park Maps and Trail Guides
- `BROCHURE` - Marketing Brochure
- `GUIDE` - Visitor Guide
- `CONSERVATION` - Conservation Report
- `RESEARCH` - Research Papers
- `SAFETY` - Safety Guidelines
- `EMERGENCY` - Emergency Procedures
- `WILDLIFE_LIST` - Wildlife Checklist
- `BIRD_LIST` - Bird Checklist
- `PLANT_LIST` - Flora List
- `CALENDAR` - Events Calendar
- `NEWSLETTER` - Park Newsletter
- `ANNUAL_REPORT` - Annual Report
- `MANAGEMENT_PLAN` - Management Plan
- `POLICY` - Park Policy
- `AGREEMENT` - Partnership Agreement
- `PRESENTATION` - Information Presentation
- `HISTORICAL` - Historical Records
- `OTHER` - Other Document Type

**Response:**
```json
{
  "status": 200,
  "message": "Park documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "parkId": "obfuscated_park_id",
        "parkName": "Serengeti National Park",
        "title": "2024 Tariff Schedule",
        "documentType": "TARIFF",
        "documentTypeDisplayName": "Tariff Document",
        "documentTypeDescription": "Park entry fees and tariff schedule",
        "documentUrl": "http://localhost:4450/api/park-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/park-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "serengeti_tariff_2024.pdf",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "fileType": "application/pdf",
        "description": "Official tariff schedule for 2024",
        "version": "2024.1",
        "notes": "Effective from January 1, 2024",
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

**Endpoint:** `GET /api/park-documents/{id}`

**Permission:** `PERM_READ_PARK_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Park document retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "parkId": "obfuscated_park_id",
    "parkName": "Serengeti National Park",
    "title": "Park Regulations 2024",
    "documentType": "REGULATION",
    "documentTypeDisplayName": "Regulation",
    "documentTypeDescription": "Park rules and regulations",
    "documentUrl": "http://localhost:4450/api/park-documents/obfuscated_id/file",
    "fileDocumentUrl": "http://localhost:4450/api/park-documents/file/hashed_filename.pdf",
    "fileName": "hashed_filename.pdf",
    "originalFileName": "serengeti_regulations.pdf",
    "fileSize": 567890,
    "fileSizeFormatted": "554.6 KB",
    "fileType": "application/pdf",
    "description": "Complete park regulations",
    "version": "v3.0",
    "notes": "Updated to include new camping rules",
    "validFrom": "2024-01-01T00:00:00",
    "validTo": null,
    "isCurrentlyValid": true,
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 3. Get Documents by Park ID

**Endpoint:** `GET /api/park-documents/park/{parkId}`

**Permission:** `PERM_READ_PARK_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| parkId | String | Obfuscated park ID |

**Response:**
```json
{
  "status": 200,
  "message": "Park documents retrieved successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "title": "2024 Tariff Schedule",
      "documentType": "TARIFF",
      ...
    },
    {
      "id": "obfuscated_id_2",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "title": "Park Regulations",
      "documentType": "REGULATION",
      ...
    }
  ]
}
```

---

### 4. Get Document File by Filename (Public)

**Endpoint:** `GET /api/park-documents/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename (e.g., `abc123_1701304567890.pdf`) |

**Response:**
- Returns the actual document binary with appropriate `Content-Type` header
- Returns `404 Not Found` if document doesn't exist or is inactive

**Headers:**
```
Content-Type: application/pdf (or appropriate MIME type)
Content-Length: 1234567
Content-Disposition: inline; filename="original_filename.pdf"
Cache-Control: public, max-age=86400
```

---

### 5. Serve Document File by ID (Public)

**Endpoint:** `GET /api/park-documents/{id}/file`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
- Returns the actual document binary with appropriate `Content-Type` header
- Returns `400 Bad Request` for invalid ID
- Returns `404 Not Found` if document doesn't exist or is inactive

---

### 6. Upload Multiple Documents

**Endpoint:** `POST /api/park-documents/upload`

**Permission:** `PERM_CREATE_PARK_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].parkId | String | Yes | Obfuscated park ID |
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
curl -X POST "http://localhost:4450/api/park-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].parkId=obfuscated_park_id" \
  -F "documents[0].document=@/path/to/tariff.pdf" \
  -F "documents[0].title=2024 Tariff Schedule" \
  -F "documents[0].documentType=TARIFF" \
  -F "documents[0].version=2024.1" \
  -F "documents[0].validFrom=2024-01-01T00:00:00" \
  -F "documents[0].validTo=2024-12-31T23:59:59" \
  -F "documents[1].parkId=obfuscated_park_id" \
  -F "documents[1].document=@/path/to/regulations.pdf" \
  -F "documents[1].title=Park Regulations" \
  -F "documents[1].documentType=REGULATION"
```

**Success Response:**
```json
{
  "status": 201,
  "message": "2 document(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "title": "2024 Tariff Schedule",
      "documentType": "TARIFF",
      ...
    },
    {
      "id": "obfuscated_id_2",
      "parkId": "obfuscated_park_id",
      "parkName": "Serengeti National Park",
      "title": "Park Regulations",
      "documentType": "REGULATION",
      ...
    }
  ]
}
```

**Error Response (Validation):**
```json
{
  "status": 400,
  "message": "Validation failed: Document 1: Title is required; Document 2 (malware.exe): File type not allowed",
  "errorCode": "VALIDATION_ERROR"
}
```

---

### 7. Update Document Metadata

**Endpoint:** `PUT /api/park-documents/{id}`

**Permission:** `PERM_UPDATE_PARK_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Request Body:**
```json
{
  "title": "Updated Tariff Schedule 2024",
  "documentType": "TARIFF",
  "description": "Updated description",
  "version": "2024.2",
  "notes": "Updated notes",
  "validFrom": "2024-01-01T00:00:00",
  "validTo": "2024-12-31T23:59:59",
  "isActive": true
}
```

All fields are optional. Only provided fields will be updated.

**Response:**
```json
{
  "status": 200,
  "message": "Park document updated successfully",
  "data": {
    "id": "obfuscated_id",
    "parkId": "obfuscated_park_id",
    "title": "Updated Tariff Schedule 2024",
    "version": "2024.2",
    ...
  }
}
```

---

### 8. Delete Single Document

**Endpoint:** `DELETE /api/park-documents/{id}`

**Permission:** `PERM_DELETE_PARK_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Park document deleted successfully",
  "data": null
}
```

---

### 9. Bulk Delete Documents

**Endpoint:** `DELETE /api/park-documents`

**Permission:** `PERM_DELETE_PARK_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

**Example:**
```
DELETE /api/park-documents?ids=id1,id2,id3
```

**Response:**
```json
{
  "status": 200,
  "message": "3 document(s) deleted successfully",
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
  "message": "2 document(s) deleted successfully, 1 failed",
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
| INVALID_DOCUMENT_ID | The provided document ID is invalid or cannot be decoded |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| PARK_NOT_FOUND | The referenced park was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |
| DOCUMENT_FETCH_FAILED | Failed to fetch park documents |
| DOCUMENT_UPDATE_FAILED | Failed to update park document |
| DOCUMENT_DELETE_FAILED | Failed to delete park document |
| BULK_DELETE_FAILED | Failed to perform bulk delete operation |

---

## File Validation

Documents are validated against the following criteria (configurable via FileSettings):

- **Allowed Extensions:** pdf, doc, docx, xls, xlsx, csv, txt, zip, png, jpg, jpeg, gif, mp4, ppt, pptx, etc. (as configured)
- **Maximum File Size:** Configurable (default varies)
- **Maximum Request Size:** Configurable for bulk uploads

---

## Validity Period

Documents support validity periods:
- `validFrom`: Start date of validity (nullable)
- `validTo`: End date of validity (nullable)
- `isCurrentlyValid`: Computed field indicating if document is currently valid

A document is considered "currently valid" if:
1. It is active (`isActive = true`)
2. Current time is >= `validFrom` (or `validFrom` is null)
3. Current time is <= `validTo` (or `validTo` is null)

---

## Storage

Documents are stored at the configured path:
```
${park.document.storage.path:./data/park-documents/}
```

Filenames are generated using SHA-256 hash + timestamp to ensure uniqueness and prevent conflicts.
