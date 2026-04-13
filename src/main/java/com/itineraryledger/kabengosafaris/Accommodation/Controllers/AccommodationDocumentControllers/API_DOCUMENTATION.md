# Accommodation Document API Documentation

## Overview

The Accommodation Document API provides endpoints for managing documents associated with accommodations. Documents are stored on the filesystem with SHA-256 hashed filenames, while only the filename is stored in the database. The full URL is constructed dynamically using the configured base URL.

**Base URL:** `/api/accommodation-documents`

---

## Authentication & Authorization

All endpoints require authentication via JWT token (except file serving endpoints):
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET (list/details) | `PERM_READ_ACCOMMODATION_DOCUMENT` |
| GET (file serving) | None (public access) |
| POST (upload) | `PERM_CREATE_ACCOMMODATION_DOCUMENT` |
| PUT (update) | `PERM_UPDATE_ACCOMMODATION_DOCUMENT` |
| DELETE | `PERM_DELETE_ACCOMMODATION_DOCUMENT` |

---

## Document Storage

- **Storage Path:** Configured via `accommodation.document.storage.path` in application.properties
- **URL Construction:** `app.base.url` + `/api/accommodation-documents/{obfuscatedId}/file`
- **Alternative URL:** `app.base.url` + `/api/accommodation-documents/file/{fileName}`
- **Filename Format:** SHA-256 hash + timestamp (e.g., `f2e5a046548d723b59874286c9d528188b50881b215a49341b03998d7f2d00eb_1701304567890.pdf`)
- **Validation:** Uses `FileSettingGetterServices` for file extension and size validation
- **Allowed Extensions:** Configured via `file.upload.allowed.extensions` (default: `pdf,doc,docx,xls,xlsx,csv,txt,zip`)

---

## Endpoints

### 1. Get All Documents (with Filters, Pagination, and Sorting)

Get all documents with optional filters, pagination, and sorting. Always sorted by `createdAt`, descending by default.

**Endpoint:** `GET /api/accommodation-documents`

**Permission:** `PERM_READ_ACCOMMODATION_DOCUMENT`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accommodationId` | String | No | - | Filter by accommodation (obfuscated ID) |
| `accommodationName` | String | No | - | Filter by accommodation name (contains) |
| `accommodationType` | Enum | No | - | Filter by accommodation type (HOTEL, LODGE, etc.) |
| `accommodationCategory` | Enum | No | - | Filter by category (LUXURY, MID_RANGE, BUDGET, etc.) |
| `documentType` | Enum | No | - | Filter by document type (STO_RATE, RACK_RATE, CONTRACT, etc.) |
| `title` | String | No | - | Filter by title (contains) |
| `version` | String | No | - | Filter by version (contains) |
| `isActive` | Boolean | No | - | Filter by active status |
| `currentlyValid` | Boolean | No | - | Filter for currently valid documents (validFrom <= now <= validTo) |
| `page` | Integer | No | 0 | Page number (0-indexed) |
| `size` | Integer | No | 20 | Page size |
| `sortDirection` | String | No | desc | Sort direction (asc/desc) |

#### Example Request

```bash
curl -X GET "http://localhost:4450/api/accommodation-documents?accommodationName=Serengeti&documentType=STO_RATE&isActive=true&page=0&size=10" \
  -H "Authorization: Bearer <jwt_token>"
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "accommodationId": "obfuscated_accommodation_id",
        "accommodationName": "Serengeti Safari Lodge",
        "title": "2025 STO Rate Sheet",
        "documentType": "STO_RATE",
        "documentTypeDisplayName": "STO Rate Document",
        "documentTypeDescription": "Special Tour Operator pricing sheet",
        "documentUrl": "http://localhost:4450/api/accommodation-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf",
        "fileName": "f2e5a046548d723b_1701304567890.pdf",
        "fileSize": 245760,
        "fileSizeFormatted": "240.0 KB",
        "fileType": "application/pdf",
        "version": "2025-Q1",
        "validFrom": "2025-01-01T00:00:00",
        "validTo": "2025-12-31T23:59:59",
        "isCurrentlyValid": true,
        "isActive": true,
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

### 2. Get Document by ID

Get a specific document by its obfuscated ID.

**Endpoint:** `GET /api/accommodation-documents/{id}`

**Permission:** `PERM_READ_ACCOMMODATION_DOCUMENT`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Document retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "accommodationId": "obfuscated_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "title": "2025 STO Rate Sheet",
    "documentType": "STO_RATE",
    "documentTypeDisplayName": "STO Rate Document",
    "documentTypeDescription": "Special Tour Operator pricing sheet",
    "documentUrl": "http://localhost:4450/api/accommodation-documents/obfuscated_id/file",
    "fileDocumentUrl": "http://localhost:4450/api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf",
    "fileName": "f2e5a046548d723b_1701304567890.pdf",
    "fileSize": 245760,
    "fileSizeFormatted": "240.0 KB",
    "fileType": "application/pdf",
    "description": "Special rates for tour operators",
    "version": "2025-Q1",
    "notes": "Includes group discounts",
    "validFrom": "2025-01-01T00:00:00",
    "validTo": "2025-12-31T23:59:59",
    "isCurrentlyValid": true,
    "isActive": true,
    "createdAt": "2025-01-23T10:30:00",
    "updatedAt": "2025-01-23T10:30:00"
  }
}
```

---

### 3. Get Document File by Filename

Serve the actual document file by its stored filename.

**Endpoint:** `GET /api/accommodation-documents/file/{fileName}`

**Permission:** None (public access for document viewing)

**Example:** `GET /api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf`

**Response:** Binary file data with appropriate Content-Type header

**Headers Returned:**
- `Content-Type`: Document MIME type (e.g., `application/pdf`, `application/msword`)
- `Content-Length`: File size in bytes
- `Cache-Control`: `public, max-age=86400` (1 day caching)
- `Content-Disposition`: `inline` for PDFs (view in browser), `attachment` for others (download)

**Usage:**
```html
<!-- View PDF in browser -->
<iframe src="http://localhost:4450/api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf"></iframe>

<!-- Download link -->
<a href="http://localhost:4450/api/accommodation-documents/file/f2e5a046548d723b_1701304567890.xlsx">Download Rate Sheet</a>
```

**Error Responses:**
- 404 Not Found: Document not found or inactive

---

### 4. Get Document File by ID

Serve the actual document file by its obfuscated ID.

**Endpoint:** `GET /api/accommodation-documents/{id}/file`

**Permission:** None (public access for document viewing)

**Response:** Binary file data with appropriate Content-Type header

**Headers Returned:**
- `Content-Type`: Document MIME type (e.g., `application/pdf`, `application/msword`)
- `Content-Length`: File size in bytes
- `Cache-Control`: `public, max-age=86400` (1 day caching)
- `Content-Disposition`: `inline` for PDFs (view in browser), `attachment` for others (download)

**Error Responses:**
- 400 Bad Request: Invalid document ID format
- 404 Not Found: Document not found or inactive

---

### 5. Upload Multiple Documents

Upload multiple documents for accommodations in a single request.

**Endpoint:** `POST /api/accommodation-documents/upload`

**Content-Type:** `multipart/form-data`

**Permission:** `PERM_CREATE_ACCOMMODATION_DOCUMENT`

#### Request Body (UploadAccommodationDocumentsDTO)

The request uses a wrapper DTO with a `documents` array. Each item in the `documents` array should contain:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `document` | File | Yes | The document file to upload |
| `title` | String | Yes | Document title |
| `documentType` | Enum | Yes | Document type (STO_RATE, RACK_RATE, CONTRACT, etc.) |
| `description` | String | No | Document description |
| `version` | String | No | Document version (e.g., "2025-Q1", "v2.0") |
| `notes` | String | No | Internal notes |
| `validFrom` | DateTime | No | When document becomes valid |
| `validTo` | DateTime | No | When document expires |

#### Example Request (cURL)

```bash
curl -X POST "http://localhost:4450/api/accommodation-documents/upload" \
  -H "Authorization: Bearer <jwt_token>" \
  -F "documents[0].accommodationId=abc123" \
  -F "documents[0].document=@/path/to/rate_sheet.pdf" \
  -F "documents[0].title=2025 STO Rate Sheet" \
  -F "documents[0].documentType=STO_RATE" \
  -F "documents[0].version=2025-Q1" \
  -F "documents[0].validFrom=2025-01-01T00:00:00" \
  -F "documents[0].validTo=2025-12-31T23:59:59" \
  -F "documents[1].accommodationId=abc123" \
  -F "documents[1].document=@/path/to/contract.pdf" \
  -F "documents[1].title=Service Agreement 2025" \
  -F "documents[1].documentType=CONTRACT"
```

#### Response

```json
{
  "success": true,
  "status": 201,
  "message": "2 document(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "accommodationId": "abc123",
      "accommodationName": "Serengeti Safari Lodge",
      "title": "2025 STO Rate Sheet",
      "documentType": "STO_RATE",
      "documentUrl": "http://localhost:4450/api/accommodation-documents/obfuscated_id_1/file",
      "fileDocumentUrl": "http://localhost:4450/api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf",
      "fileName": "f2e5a046548d723b_1701304567890.pdf",
      "version": "2025-Q1",
      "validFrom": "2025-01-01T00:00:00",
      "validTo": "2025-12-31T23:59:59",
      "isActive": true
    },
    {
      "id": "obfuscated_id_2",
      "accommodationId": "abc123",
      "accommodationName": "Serengeti Safari Lodge",
      "title": "Service Agreement 2025",
      "documentType": "CONTRACT",
      "documentUrl": "http://localhost:4450/api/accommodation-documents/obfuscated_id_2/file",
      "fileDocumentUrl": "http://localhost:4450/api/accommodation-documents/file/a1b2c3d4e5f6g7h8_1701304567891.pdf",
      "fileName": "a1b2c3d4e5f6g7h8_1701304567891.pdf",
      "isActive": true
    }
  ]
}
```

#### Validation Error Response

```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed: Document 1: Title is required; Document 2 (large.exe): File type 'exe' is blocked for security reasons",
  "errorCode": "VALIDATION_ERROR"
}
```

---

### 6. Update Document Metadata

Update document metadata (not the document file itself). To replace the actual document file, delete the old document and upload a new one.

**Endpoint:** `PUT /api/accommodation-documents/{id}`

**Permission:** `PERM_UPDATE_ACCOMMODATION_DOCUMENT`

#### Request Body

```json
{
  "title": "Updated Title",
  "documentType": "STO_RATE",
  "description": "Updated description",
  "version": "2025-Q2",
  "notes": "Updated internal notes",
  "validFrom": "2025-04-01T00:00:00",
  "validTo": "2025-06-30T23:59:59",
  "isActive": true
}
```

All fields are optional. Only provided fields will be updated.

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Accommodation document updated successfully",
  "data": {
    "id": "obfuscated_id",
    "title": "Updated Title",
    "documentType": "STO_RATE",
    "version": "2025-Q2",
    "validFrom": "2025-04-01T00:00:00",
    "validTo": "2025-06-30T23:59:59",
    "isActive": true
  }
}
```

---

### 7. Bulk Delete Documents

Permanently delete multiple documents by their IDs. Removes from both database and filesystem.

**Endpoint:** `DELETE /api/accommodation-documents?ids={id1}&ids={id2}&ids={id3}`

**Permission:** `PERM_DELETE_ACCOMMODATION_DOCUMENT`

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ids` | String[] | Yes | List of obfuscated document IDs to delete |

#### Example Request

```bash
curl -X DELETE "http://localhost:4450/api/accommodation-documents?ids=obfuscated_id_1&ids=obfuscated_id_2&ids=obfuscated_id_3" \
  -H "Authorization: Bearer <jwt_token>"
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "3 accommodation document(s) deleted successfully",
  "data": null
}
```

---

## Document Types

| Type | Display Name | Description |
|------|--------------|-------------|
| `STO_RATE` | STO Rate Document | Special Tour Operator pricing sheet |
| `RACK_RATE` | Rack Rate Document | Public/published pricing sheet |
| `CONTRACT` | Contract | Service agreement or contract |
| `LICENSE` | License | Business or tourism license |
| `CERTIFICATE` | Certificate | Certification or accreditation |
| `BROCHURE` | Brochure | Marketing brochure or flyer |
| `FLOOR_PLAN` | Floor Plan | Property floor plan or layout |
| `MENU` | Menu | Restaurant or dining menu |
| `POLICY` | Policy | Terms, conditions, or policy document |
| `INSURANCE` | Insurance | Insurance certificate or policy |
| `SAFETY` | Safety Document | Safety procedures or guidelines |
| `TAX_DOCUMENT` | Tax Document | TIN, VRN, or tax registration |
| `INVOICE` | Invoice | Invoice or billing document |
| `RECEIPT` | Receipt | Payment receipt |
| `PHOTO` | Photo | Property or facility photo |
| `VIDEO` | Video | Property or promotional video |
| `MAP` | Map | Location or area map |
| `PRESENTATION` | Presentation | Sales or marketing presentation |
| `OTHER` | Other | Other document type |

---

## Data Types

### AccommodationDocumentDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated document ID |
| `accommodationId` | String | Obfuscated accommodation ID |
| `accommodationName` | String | Name of the accommodation |
| `title` | String | Document title |
| `documentType` | Enum | Document type |
| `documentTypeDisplayName` | String | Human-readable type name |
| `documentTypeDescription` | String | Type description |
| `documentUrl` | String | Full URL to the document using ID (`/api/accommodation-documents/{id}/file`) |
| `fileDocumentUrl` | String | Full URL to the document using filename (`/api/accommodation-documents/file/{fileName}`) |
| `fileName` | String | Stored filename (SHA-256 hash format) |
| `originalFileName` | String | Original uploaded filename |
| `fileSize` | Long | File size in bytes |
| `fileSizeFormatted` | String | Human-readable file size |
| `fileType` | String | MIME type |
| `description` | String | Document description |
| `version` | String | Document version |
| `notes` | String | Internal notes |
| `validFrom` | DateTime | When document becomes valid |
| `validTo` | DateTime | When document expires |
| `isCurrentlyValid` | Boolean | Is document currently valid |
| `isActive` | Boolean | Is the document active |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

### UploadAccommodationDocumentsDTO (Wrapper)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `documents` | Array | Yes | List of CreateAccommodationDocumentDTO |

### CreateAccommodationDocumentDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accommodationId` | String | Yes | Obfuscated accommodation ID |
| `document` | MultipartFile | Yes | The document file |
| `title` | String | Yes | Document title |
| `documentType` | Enum | Yes | Document type |
| `description` | String | No | Document description |
| `version` | String | No | Document version |
| `notes` | String | No | Internal notes |
| `validFrom` | DateTime | No | When document becomes valid |
| `validTo` | DateTime | No | When document expires |

### UpdateAccommodationDocumentDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | No | Document title |
| `documentType` | Enum | No | Document type |
| `description` | String | No | Document description |
| `version` | String | No | Document version |
| `notes` | String | No | Internal notes |
| `validFrom` | DateTime | No | When document becomes valid |
| `validTo` | DateTime | No | When document expires |
| `isActive` | Boolean | No | Is the document active |

---

## Configuration

### application.properties

```properties
# Storage path for accommodation documents on the filesystem
accommodation.document.storage.path=./data/accommodation-documents/

# Base URL (used for constructing full URLs)
app.base.url=http://localhost:4450

# Document URL is constructed as: app.base.url + /api/accommodation-documents/{documentId}/file
# Alternative: app.base.url + /api/accommodation-documents/file/{fileName}

# File upload size limits (Spring Multipart)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Allowed file extensions
file.upload.allowed.extensions=pdf,doc,docx,xls,xlsx,csv,txt,zip

# Blocked file extensions (security)
file.upload.blocked.extensions=exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js
```

### Document Validation

Document uploads are validated using `FileSettingGetterServices`:

- **Max file size:** `spring.servlet.multipart.max-file-size` (default: 10MB)
- **Max request size:** `spring.servlet.multipart.max-request-size` (default: 50MB)
- **Allowed extensions:** `file.upload.allowed.extensions` (default: `pdf,doc,docx,xls,xlsx,csv,txt,zip`)
- **Blocked extensions:** `file.upload.blocked.extensions` (security blocked types)
- **Upload enabled:** Configured via `file.upload.enabled`

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `NO_DOCUMENTS_PROVIDED` | No documents in upload request |
| 400 | `REQUEST_SIZE_EXCEEDED` | Total request size exceeds limit |
| 400 | `VALIDATION_ERROR` | Invalid request data or failed validation |
| 400 | `INVALID_ACCOMMODATION_ID` | Invalid accommodation ID format |
| 400 | `INVALID_DOCUMENT_ID` | Invalid document ID format |
| 400 | `NO_IDS_PROVIDED` | No document IDs provided for delete |
| 400 | `NO_DOCUMENTS_DELETED` | No documents found to delete |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `ACCOMMODATION_NOT_FOUND` | Accommodation not found |
| 404 | `DOCUMENT_NOT_FOUND` | Document not found |
| 500 | `STORAGE_ERROR` | Failed to save document file |
| 500 | `DATABASE_ERROR` | Failed to save document record |
| 500 | `DOCUMENT_UPDATE_FAILED` | Failed to update document |
| 500 | `ACCOMMODATION_DOCUMENT_UPLOAD_FAILED` | Failed to upload documents |
| 500 | `ACCOMMODATION_DOCUMENT_DELETE_FAILED` | Failed to delete documents |

---

## Audit Logging

All create, update, and delete operations are logged:

| Action | Description |
|--------|-------------|
| `CREATE_ACCOMMODATION_DOCUMENTS` | Documents uploaded |
| `UPDATE_ACCOMMODATION_DOCUMENT` | Document metadata updated |
| `DELETE_ACCOMMODATION_DOCUMENT` | Document permanently deleted |

---

## Validity Period

Documents support optional validity periods:

- **validFrom:** When the document becomes valid
- **validTo:** When the document expires
- **isCurrentlyValid:** Computed field indicating if document is valid now

### Filtering by Validity

Use the `currentlyValid=true` query parameter to get only currently valid documents:

```bash
curl -X GET "http://localhost:4450/api/accommodation-documents?accommodationId=abc123&documentType=STO_RATE&currentlyValid=true" \
  -H "Authorization: Bearer <jwt_token>"
```

This filters documents where:
- `isActive = true`
- `validFrom` is null OR `validFrom <= now`
- `validTo` is null OR `validTo >= now`
