# Quote Document API Documentation

## Overview
The Quote Document API provides endpoints for managing documents related to quotes, such as PDF quotes, proposals, itineraries, contracts, invoices, confirmations, and other supporting documents. The API supports multi-file uploads, versioning, validity periods, and public/authenticated file serving.

**Base URL**: `/api/quote-documents`

**Required Permissions**:
- `PERM_CREATE_QUOTE_DOCUMENT` - Upload new documents
- `PERM_READ_QUOTE_DOCUMENT` - View document metadata
- `PERM_UPDATE_QUOTE_DOCUMENT` - Update document metadata
- `PERM_DELETE_QUOTE_DOCUMENT` - Delete documents

**Public Endpoints** (No Authentication Required):
- File serving endpoints (`/file/{fileName}` and `/{id}/file`)

---

## Table of Contents
1. [Data Models](#data-models)
2. [Endpoints](#endpoints)
   - [Upload Documents](#1-upload-documents)
   - [Get All Documents](#2-get-all-documents-with-filters)
   - [Get Document by ID](#3-get-document-by-id)
   - [Get Document File by Filename](#4-get-document-file-by-filename)
   - [Get Document File by ID](#5-get-document-file-by-id)
   - [Update Document Metadata](#6-update-document-metadata)
   - [Bulk Delete Documents](#7-bulk-delete-documents)
3. [Response Format](#response-format)
4. [Error Codes](#error-codes)
5. [File Upload Specifications](#file-upload-specifications)
6. [Best Practices](#best-practices)

---

## Data Models

### DocumentType Enum

Represents the type/purpose of the document.

```json
{
  "allowedValues": [
    "QUOTE_PDF",             // Main quote document in PDF format
    "PROPOSAL",              // Detailed travel proposal
    "ITINERARY",             // Detailed itinerary document
    "TERMS_AND_CONDITIONS",  // Terms and conditions document
    "PAYMENT_SCHEDULE",      // Payment terms and schedule
    "CONTRACT",              // Service contract or agreement
    "INVOICE",               // Invoice document
    "RECEIPT",               // Payment receipt
    "CONFIRMATION",          // Booking confirmation
    "VOUCHER",               // Travel voucher
    "INSURANCE",             // Travel insurance documents
    "VISA_SUPPORT",          // Visa application support letter
    "FLIGHT_DETAILS",        // Flight itinerary and tickets
    "ACCOMMODATION_DETAILS", // Hotel/lodge confirmation
    "ACTIVITY_DETAILS",      // Activity booking confirmations
    "TRANSPORT_DETAILS",     // Vehicle rental or transfer details
    "HEALTH_REQUIREMENTS",   // Vaccination and health information
    "PACKING_LIST",          // Recommended packing list
    "TRAVEL_GUIDE",          // Destination travel guide
    "MAP",                   // Route maps and location details
    "EMERGENCY_CONTACTS",    // Emergency contact information
    "CORRESPONDENCE",        // Email or letter correspondence
    "AMENDMENT",             // Quote amendment or revision
    "CANCELLATION",          // Cancellation document
    "REFUND",                // Refund documentation
    "OTHER"                  // Other document type
  ]
}
```

### QuoteDocument Object (QuoteDocumentDTO)

```json
{
  "id": "string (obfuscated ID)",
  "quoteId": "string (obfuscated quote ID)",
  "quoteCode": "string (e.g., QT-2024-001)",

  "title": "string",
  "documentType": "DocumentType enum",
  "documentTypeDisplayName": "string",
  "documentTypeDescription": "string",

  "documentUrl": "string (URL for file access by ID)",
  "fileDocumentUrl": "string (URL for file access by filename)",
  "fileName": "string (system-generated hashed filename)",
  "originalFileName": "string (original upload filename)",
  "fileSize": "number (bytes)",
  "fileSizeFormatted": "string (e.g., '2.5 MB')",
  "fileType": "string (MIME type, e.g., 'application/pdf')",

  "description": "string (optional)",
  "version": "string (optional, e.g., 'v1.0', 'Draft', 'Final')",
  "notes": "string (optional internal notes)",

  "validFrom": "datetime (ISO 8601, optional)",
  "validTo": "datetime (ISO 8601, optional)",
  "isCurrentlyValid": "boolean (computed based on current time)",

  "isActive": "boolean",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

### Supported File Types

The following file types are supported:
- **Documents**: PDF (.pdf), Word (.doc, .docx), Excel (.xls, .xlsx), Text (.txt), CSV (.csv)
- **Images**: PNG (.png), JPEG (.jpg, .jpeg), GIF (.gif), BMP (.bmp), SVG (.svg), WebP (.webp)
- **Archives**: ZIP (.zip), RAR (.rar), 7Z (.7z)

---

## Endpoints

### 1. Upload Documents

**Endpoint**: `POST /api/quote-documents/upload`

**Permission**: `PERM_CREATE_QUOTE_DOCUMENT`

**Content-Type**: `multipart/form-data`

**Description**: Upload one or more documents with metadata. Each document is uploaded with its associated metadata.

#### Request Body (Form Data)

**Field**: `documents` (array of document objects)

Each document object should contain:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| quoteId | String | Yes | Obfuscated quote ID |
| document | File | Yes | The document file to upload |
| title | String | Yes | Document title/name |
| documentType | DocumentType | Yes | Type of document |
| description | String | No | Optional description |
| version | String | No | Version identifier (e.g., "v1.0", "Draft", "Final") |
| notes | String | No | Internal notes (not visible to clients) |
| validFrom | DateTime | No | Document validity start date (ISO 8601) |
| validTo | DateTime | No | Document validity end date (ISO 8601) |

#### Example Request (Form Data)

```
POST /api/quote-documents/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

documents[0].quoteId: abc123
documents[0].document: [FILE: quote_proposal.pdf]
documents[0].title: Safari Quote Proposal
documents[0].documentType: PROPOSAL
documents[0].description: Detailed 7-day safari proposal
documents[0].version: v1.0
documents[0].validFrom: 2024-01-01T00:00:00
documents[0].validTo: 2024-12-31T23:59:59

documents[1].quoteId: abc123
documents[1].document: [FILE: itinerary.pdf]
documents[1].title: Safari Itinerary
documents[1].documentType: ITINERARY
documents[1].description: Day-by-day itinerary
documents[1].version: Final
```

#### Success Response (201 Created)

```json
{
  "success": true,
  "statusCode": 201,
  "message": "2 document(s) uploaded successfully",
  "data": {
    "uploadedCount": 2,
    "documents": [
      {
        "id": "xY9Kp2Lm",
        "quoteId": "abc123",
        "quoteCode": "QT-2024-001",
        "title": "Safari Quote Proposal",
        "documentType": "PROPOSAL",
        "documentTypeDisplayName": "Proposal",
        "documentTypeDescription": "Detailed travel proposal",
        "documentUrl": "http://localhost:4450/api/quote-documents/xY9Kp2Lm/file",
        "fileDocumentUrl": "http://localhost:4450/api/quote-documents/file/a1b2c3d4e5f6g7h8.pdf",
        "fileName": "a1b2c3d4e5f6g7h8.pdf",
        "originalFileName": "quote_proposal.pdf",
        "fileSize": 2458624,
        "fileSizeFormatted": "2.34 MB",
        "fileType": "application/pdf",
        "description": "Detailed 7-day safari proposal",
        "version": "v1.0",
        "notes": null,
        "validFrom": "2024-01-01T00:00:00",
        "validTo": "2024-12-31T23:59:59",
        "isCurrentlyValid": true,
        "isActive": true,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      },
      {
        "id": "zW8Mn3Pq",
        "quoteId": "abc123",
        "quoteCode": "QT-2024-001",
        "title": "Safari Itinerary",
        "documentType": "ITINERARY",
        ...
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

#### Error Responses

**400 Bad Request** - Invalid quote ID, missing required fields, or file validation errors
```json
{
  "success": false,
  "statusCode": 400,
  "message": "File size exceeds maximum allowed size (10 MB)",
  "errorCode": "FILE_TOO_LARGE",
  "timestamp": "2024-01-15T10:30:00"
}
```

**400 Bad Request** - Unsupported file type
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Unsupported file type. Allowed types: pdf, doc, docx, xls, xlsx, txt, csv, png, jpg, jpeg, gif, bmp, svg, webp, zip, rar, 7z",
  "errorCode": "UNSUPPORTED_FILE_TYPE",
  "timestamp": "2024-01-15T10:30:00"
}
```

**404 Not Found** - Quote not found
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote not found with ID: abc123",
  "errorCode": "QUOTE_NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### 2. Get All Documents (with Filters)

**Endpoint**: `GET /api/quote-documents`

**Permission**: `PERM_READ_QUOTE_DOCUMENT`

**Description**: Retrieves a paginated list of quote documents with optional filtering and sorting.

#### Query Parameters

**Filtering**:
- `quoteId` (string, optional): Filter by quote ID (obfuscated)
- `quoteCode` (string, optional): Filter by quote code (e.g., "QT-2024-001")
- `documentType` (DocumentType, optional): Filter by document type (exact match)
- `title` (string, optional): Filter by title (partial match, case-insensitive)
- `version` (string, optional): Filter by version (exact match)
- `isActive` (boolean, optional): Filter by active status
- `currentlyValid` (boolean, optional): Filter by current validity (based on validFrom/validTo and current date)

**Pagination**:
- `page` (integer, optional, default: 0): Page number (0-indexed)
- `size` (integer, optional, default: 20): Number of items per page

**Sorting**:
- `sortBy` (string, optional, default: "createdAt"): Field to sort by
  - Allowed values: `createdAt`, `updatedAt`, `title`, `version`, `documentType`, `fileSize`, `validFrom`, `validTo`
- `sortDirection` (string, optional, default: "desc"): Sort direction
  - Allowed values: `asc`, `desc`

#### Example Requests

**Get all documents for a quote**:
```
GET /api/quote-documents?quoteId=abc123
```

**Filter by document type**:
```
GET /api/quote-documents?quoteId=abc123&documentType=PROPOSAL
```

**Filter currently valid invoices**:
```
GET /api/quote-documents?documentType=INVOICE&currentlyValid=true&page=0&size=10
```

**Search by title**:
```
GET /api/quote-documents?title=safari&sortBy=createdAt&sortDirection=desc
```

**Complex filter**:
```
GET /api/quote-documents?quoteCode=QT-2024-001&documentType=QUOTE_PDF&isActive=true&version=Final&page=0&size=20
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "xY9Kp2Lm",
        "quoteId": "abc123",
        "quoteCode": "QT-2024-001",
        "title": "Safari Quote Proposal",
        "documentType": "PROPOSAL",
        "documentTypeDisplayName": "Proposal",
        "documentUrl": "http://localhost:4450/api/quote-documents/xY9Kp2Lm/file",
        "fileDocumentUrl": "http://localhost:4450/api/quote-documents/file/a1b2c3d4e5f6g7h8.pdf",
        "fileName": "a1b2c3d4e5f6g7h8.pdf",
        "originalFileName": "quote_proposal.pdf",
        "fileSize": 2458624,
        "fileSizeFormatted": "2.34 MB",
        "fileType": "application/pdf",
        "isCurrentlyValid": true,
        "isActive": true,
        ...
      }
    ],
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2024-01-15T11:00:00"
}
```

---

### 3. Get Document by ID

**Endpoint**: `GET /api/quote-documents/{id}`

**Permission**: `PERM_READ_QUOTE_DOCUMENT`

**Description**: Retrieves a single document's metadata by its obfuscated ID.

#### Path Parameters
- `id` (required): The obfuscated document ID

#### Success Response (200 OK)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote document retrieved successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "quoteId": "abc123",
    "quoteCode": "QT-2024-001",
    "title": "Safari Quote Proposal",
    "documentType": "PROPOSAL",
    "documentTypeDisplayName": "Proposal",
    "documentTypeDescription": "Detailed travel proposal",
    "documentUrl": "http://localhost:4450/api/quote-documents/xY9Kp2Lm/file",
    "fileDocumentUrl": "http://localhost:4450/api/quote-documents/file/a1b2c3d4e5f6g7h8.pdf",
    "fileName": "a1b2c3d4e5f6g7h8.pdf",
    "originalFileName": "quote_proposal.pdf",
    "fileSize": 2458624,
    "fileSizeFormatted": "2.34 MB",
    "fileType": "application/pdf",
    "description": "Detailed 7-day safari proposal",
    "version": "v1.0",
    "notes": "Internal notes here",
    "validFrom": "2024-01-01T00:00:00",
    "validTo": "2024-12-31T23:59:59",
    "isCurrentlyValid": true,
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T11:15:00"
}
```

#### Error Responses

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote document not found with ID: xY9Kp2Lm",
  "errorCode": "DOCUMENT_NOT_FOUND",
  "timestamp": "2024-01-15T11:15:00"
}
```

**400 Bad Request** - Invalid document ID
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid document ID",
  "errorCode": "INVALID_DOCUMENT_ID",
  "timestamp": "2024-01-15T11:15:00"
}
```

---

### 4. Get Document File by Filename

**Endpoint**: `GET /api/quote-documents/file/{fileName}`

**Permission**: **None** (Public access)

**Description**: Serves the actual document file by its system-generated filename. This endpoint is publicly accessible and designed for direct file links. PDFs are displayed inline in the browser, other files are downloaded as attachments.

#### Path Parameters
- `fileName` (required): The system-generated hashed filename (e.g., "a1b2c3d4e5f6g7h8.pdf")

#### Response Headers

```
Content-Type: application/pdf (or appropriate MIME type)
Content-Length: 2458624
Cache-Control: public, max-age=86400
Content-Disposition: inline; filename="a1b2c3d4e5f6g7h8.pdf" (or attachment for non-PDFs)
```

#### Success Response (200 OK)

Returns the raw file bytes with appropriate headers for browser display or download.

#### Error Responses

**404 Not Found** - File doesn't exist or document is inactive
```
HTTP/1.1 404 Not Found
```

---

### 5. Get Document File by ID

**Endpoint**: `GET /api/quote-documents/{id}/file`

**Permission**: **None** (Public access)

**Description**: Serves the actual document file by its obfuscated document ID. This endpoint is publicly accessible. PDFs are displayed inline in the browser, other files are downloaded as attachments.

#### Path Parameters
- `id` (required): The obfuscated document ID

#### Response Headers

```
Content-Type: application/pdf (or appropriate MIME type)
Content-Length: 2458624
Cache-Control: public, max-age=86400
Content-Disposition: inline; filename="a1b2c3d4e5f6g7h8.pdf" (or attachment for non-PDFs)
```

#### Success Response (200 OK)

Returns the raw file bytes with appropriate headers for browser display or download.

#### Error Responses

**404 Not Found** - Document doesn't exist or is inactive
```
HTTP/1.1 404 Not Found
```

**400 Bad Request** - Invalid document ID
```
HTTP/1.1 400 Bad Request
```

---

### 6. Update Document Metadata

**Endpoint**: `PUT /api/quote-documents/{id}`

**Permission**: `PERM_UPDATE_QUOTE_DOCUMENT`

**Description**: Updates document metadata only. To replace the actual file, delete the document and upload a new one.

**Note**: The document file itself cannot be updated through this endpoint. Only metadata fields can be modified.

#### Path Parameters
- `id` (required): The obfuscated document ID

#### Request Body (UpdateQuoteDocumentDTO)

All fields are optional. Only include fields you want to update.

```json
{
  "title": "Updated Safari Proposal",
  "documentType": "PROPOSAL",
  "description": "Updated description",
  "version": "v2.0",
  "notes": "Updated internal notes",
  "validFrom": "2024-02-01T00:00:00",
  "validTo": "2024-12-31T23:59:59",
  "isActive": false
}
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quote document updated successfully",
  "data": {
    "id": "xY9Kp2Lm",
    "quoteId": "abc123",
    "quoteCode": "QT-2024-001",
    "title": "Updated Safari Proposal",
    "version": "v2.0",
    ...
    "updatedAt": "2024-01-15T14:30:00"
  },
  "timestamp": "2024-01-15T14:30:00"
}
```

#### Error Responses

**404 Not Found** - Document doesn't exist
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Quote document not found with ID: xY9Kp2Lm",
  "errorCode": "DOCUMENT_NOT_FOUND",
  "timestamp": "2024-01-15T14:30:00"
}
```

**400 Bad Request** - Invalid document ID or validation errors
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid document ID",
  "errorCode": "INVALID_DOCUMENT_ID",
  "timestamp": "2024-01-15T14:30:00"
}
```

---

### 7. Bulk Delete Documents

**Endpoint**: `DELETE /api/quote-documents`

**Permission**: `PERM_DELETE_QUOTE_DOCUMENT`

**Description**: Permanently deletes one or more documents from both the database and filesystem.

**Warning**: This is a destructive operation. Deleted files cannot be recovered.

#### Query Parameters
- `ids` (required): Comma-separated list of obfuscated document IDs

#### Example Request

```
DELETE /api/quote-documents?ids=xY9Kp2Lm,aB3Cd4Ef,pQ7Rs8Tv
```

Or with JSON body:

```json
["xY9Kp2Lm", "aB3Cd4Ef", "pQ7Rs8Tv"]
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully deleted 3 document(s)",
  "data": {
    "deletedCount": 3,
    "deletedIds": ["xY9Kp2Lm", "aB3Cd4Ef", "pQ7Rs8Tv"],
    "failedIds": []
  },
  "timestamp": "2024-01-15T15:00:00"
}
```

#### Partial Success Response (200 OK)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deleted 2 document(s), 1 failed",
  "data": {
    "deletedCount": 2,
    "deletedIds": ["xY9Kp2Lm", "aB3Cd4Ef"],
    "failedIds": ["invalidId123"]
  },
  "timestamp": "2024-01-15T15:00:00"
}
```

#### Error Responses

**400 Bad Request** - Empty ID list
```json
{
  "success": false,
  "statusCode": 400,
  "message": "No document IDs provided for deletion",
  "errorCode": "EMPTY_DELETE_LIST",
  "timestamp": "2024-01-15T15:00:00"
}
```

---

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-01-15T10:00:00"
}
```

### Error Response
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-01-15T10:00:00"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `DOCUMENT_NOT_FOUND` | 404 | Document with specified ID not found |
| `QUOTE_NOT_FOUND` | 404 | Quote with specified ID not found |
| `INVALID_DOCUMENT_ID` | 400 | The provided document ID is invalid |
| `INVALID_QUOTE_ID` | 400 | The provided quote ID is invalid |
| `FILE_TOO_LARGE` | 400 | File size exceeds maximum allowed size |
| `REQUEST_TOO_LARGE` | 400 | Total request size exceeds maximum allowed size |
| `UNSUPPORTED_FILE_TYPE` | 400 | File type is not supported |
| `FILE_UPLOAD_FAILED` | 500 | Failed to upload or save the file |
| `DOCUMENT_CREATE_FAILED` | 500 | Failed to create document record |
| `DOCUMENT_UPDATE_FAILED` | 500 | Failed to update document |
| `DOCUMENT_DELETE_FAILED` | 500 | Failed to delete document(s) |
| `DOCUMENT_FETCH_FAILED` | 500 | Failed to fetch documents |
| `EMPTY_DELETE_LIST` | 400 | No document IDs provided for deletion |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `PERMISSION_DENIED` | 403 | User lacks required permission |
| `UNAUTHORIZED` | 401 | User is not authenticated |

---

## File Upload Specifications

### Maximum File Sizes
- **Single File**: 10 MB (default, configurable via application settings)
- **Request Size**: 50 MB total (default, configurable via application settings)

### Allowed File Types

| Category | Extensions | MIME Types |
|----------|------------|------------|
| **PDF** | .pdf | application/pdf |
| **Word Documents** | .doc, .docx | application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document |
| **Excel Spreadsheets** | .xls, .xlsx | application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| **Text Files** | .txt, .csv | text/plain, text/csv |
| **Images** | .png, .jpg, .jpeg, .gif, .bmp, .svg, .webp | image/png, image/jpeg, image/gif, image/bmp, image/svg+xml, image/webp |
| **Archives** | .zip, .rar, .7z | application/zip, application/x-rar-compressed, application/x-7z-compressed |

### File Storage
- Files are stored on the server filesystem at: `/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/quote-documents/` (configurable)
- Original filenames are hashed using SHA-256 for security
- System-generated filenames format: `[hash].extension` (e.g., `a1b2c3d4e5f6g7h8.pdf`)
- Files are cached with `max-age=86400` (24 hours)

---

## Authentication & Authorization

### Authenticated Endpoints

All metadata endpoints require:

1. **Authentication**: Valid JWT token in the `Authorization` header
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Authorization**: User must have the appropriate permission:
   - Upload operations: `PERM_CREATE_QUOTE_DOCUMENT`
   - Read metadata: `PERM_READ_QUOTE_DOCUMENT`
   - Update operations: `PERM_UPDATE_QUOTE_DOCUMENT`
   - Delete operations: `PERM_DELETE_QUOTE_DOCUMENT`

### Public Endpoints

File serving endpoints (`/file/{fileName}` and `/{id}/file`) are publicly accessible without authentication, allowing quotes to be shared with clients via direct links.

---

## Best Practices

1. **Use Public Links for Client Sharing**: Share the `/file/{fileName}` or `/{id}/file` endpoints with clients for easy document access without authentication.

2. **Version Your Documents**: Use the `version` field to track document revisions (e.g., "Draft", "v1.0", "Final").

3. **Set Validity Periods**: Use `validFrom` and `validTo` for time-sensitive documents like quotes with expiration dates or seasonal pricing.

4. **Leverage Document Types**: Use appropriate `DocumentType` values for better organization and filtering.

5. **Use Descriptive Titles**: Make document titles clear and searchable for easy retrieval.

6. **Filter Inactive Documents**: For public-facing applications, filter by `isActive=true` to show only active documents.

7. **Cache File URLs**: The file serving endpoints include cache headers (24 hours) - leverage browser caching for better performance.

8. **Validate Before Upload**: Check file size and type on the client side before uploading to prevent unnecessary server requests.

9. **Handle Bulk Operations**: Use bulk delete for efficiency when removing multiple documents.

10. **Use Notes for Internal Communication**: The `notes` field is perfect for internal team communication that shouldn't be visible to clients.

11. **Monitor File Storage**: Regularly monitor disk space usage as documents accumulate over time.

12. **Implement Retention Policies**: Consider implementing document retention policies to archive or delete old documents.

---

## Examples

### cURL Examples

**Upload a single document**:
```bash
curl -X POST http://localhost:4450/api/quote-documents/upload \
  -H "Authorization: Bearer <your-token>" \
  -F "documents[0].quoteId=abc123" \
  -F "documents[0].document=@/path/to/quote_proposal.pdf" \
  -F "documents[0].title=Safari Quote Proposal" \
  -F "documents[0].documentType=PROPOSAL" \
  -F "documents[0].description=Detailed 7-day safari proposal" \
  -F "documents[0].version=v1.0"
```

**Upload multiple documents**:
```bash
curl -X POST http://localhost:4450/api/quote-documents/upload \
  -H "Authorization: Bearer <your-token>" \
  -F "documents[0].quoteId=abc123" \
  -F "documents[0].document=@/path/to/proposal.pdf" \
  -F "documents[0].title=Proposal" \
  -F "documents[0].documentType=PROPOSAL" \
  -F "documents[1].quoteId=abc123" \
  -F "documents[1].document=@/path/to/itinerary.pdf" \
  -F "documents[1].title=Itinerary" \
  -F "documents[1].documentType=ITINERARY"
```

**Get all documents for a quote**:
```bash
curl -X GET "http://localhost:4450/api/quote-documents?quoteId=abc123&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**Get document metadata**:
```bash
curl -X GET http://localhost:4450/api/quote-documents/xY9Kp2Lm \
  -H "Authorization: Bearer <your-token>"
```

**Download document file (public, no auth)**:
```bash
curl -X GET http://localhost:4450/api/quote-documents/xY9Kp2Lm/file \
  --output downloaded_document.pdf
```

**Update document metadata**:
```bash
curl -X PUT http://localhost:4450/api/quote-documents/xY9Kp2Lm \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Safari Proposal",
    "version": "v2.0",
    "isActive": true
  }'
```

**Delete multiple documents**:
```bash
curl -X DELETE "http://localhost:4450/api/quote-documents?ids=xY9Kp2Lm,aB3Cd4Ef,pQ7Rs8Tv" \
  -H "Authorization: Bearer <your-token>"
```

**Filter currently valid invoices**:
```bash
curl -X GET "http://localhost:4450/api/quote-documents?documentType=INVOICE&currentlyValid=true&isActive=true" \
  -H "Authorization: Bearer <your-token>"
```

---

## Use Case Scenarios

### Scenario 1: Creating a Quote Package

When creating a quote for a client, you might upload multiple related documents:

1. Upload main quote PDF (`QUOTE_PDF`)
2. Upload detailed proposal (`PROPOSAL`)
3. Upload itinerary (`ITINERARY`)
4. Upload terms and conditions (`TERMS_AND_CONDITIONS`)
5. Upload payment schedule (`PAYMENT_SCHEDULE`)

All documents linked to the same `quoteId` with appropriate `validFrom` and `validTo` dates.

### Scenario 2: Sharing Documents with Clients

After uploading documents, share the public file URLs with clients:
- Use `documentUrl` or `fileDocumentUrl` from the API response
- No authentication required - clients can access directly via browser
- PDFs open inline for easy viewing
- Other file types download automatically

### Scenario 3: Managing Document Versions

Track document revisions using the version field:
1. Upload initial quote: `version: "Draft"`
2. Update metadata: `version: "v1.0"`
3. Upload revised quote: `version: "v2.0"`
4. Mark old versions as inactive: `isActive: false`

### Scenario 4: Time-Limited Quotes

For quotes with expiration dates:
1. Set `validFrom` to quote creation date
2. Set `validTo` to expiration date (e.g., 30 days later)
3. Use `currentlyValid=true` filter to show only current quotes
4. System automatically computes `isCurrentlyValid` based on current time

---

## Notes

- **ID Obfuscation**: All document and quote IDs are obfuscated for security.
- **File Hashing**: Uploaded files are renamed using SHA-256 hashing to prevent naming conflicts and enhance security.
- **Timestamps**: All timestamps are in ISO 8601 format (UTC).
- **Public Access**: File serving endpoints are intentionally public to allow easy sharing with clients.
- **PDF Inline Viewing**: PDFs are served with `Content-Disposition: inline` for browser viewing; other files use `attachment` for download.
- **Cache Headers**: File serving responses include cache headers for 24-hour browser caching.
- **File Size Formatting**: File sizes are automatically formatted in human-readable format (B, KB, MB, GB).
- **Soft Delete Consideration**: The system uses `isActive` flag which allows for soft deletion by setting it to `false`.

---

## Support

For issues or questions about the Quote Document API, please contact the development team or refer to the main application documentation.

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-01-29 | Initial documentation |
