# Safari Document Controller API Documentation

## Base URL
```
/api/safari-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_SAFARI_DOCUMENT` - For reading safari documents
- `PERM_CREATE_SAFARI_DOCUMENT` - For uploading documents
- `PERM_UPDATE_SAFARI_DOCUMENT` - For updating document metadata
- `PERM_DELETE_SAFARI_DOCUMENT` - For deleting documents

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/safari-documents`

**Permission:** `PERM_READ_SAFARI_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| safariId | String | No | - | Filter by safari (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| isGenerated | Boolean | No | - | Filter by whether document was system-generated |
| title | String | No | - | Filter by title (partial match) |
| version | String | No | - | Filter by version |
| currentlyValid | Boolean | No | - | Filter for currently valid documents only |
| safariName | String | No | - | Filter by safari name (partial match) |
| safariCode | String | No | - | Filter by safari code (partial match) |
| safariIsActive | Boolean | No | - | Filter by safari active status |
| safariState | SafariState | No | - | Filter by safari state |
| quotationDocumentsOnly | Boolean | No | - | Filter for quotation/invoice documents only |
| travelDocumentsOnly | Boolean | No | - | Filter for travel plan documents only |
| voucherDocumentsOnly | Boolean | No | - | Filter for voucher documents only |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `QUOTATION`, `TRAVEL_PLAN`, `FINAL_ITINERARY`, `BOOKING_CONFIRMATION`, `INVOICE`, `PROFORMA_INVOICE`, `RECEIPT`, `VISA_SUPPORT_LETTER`, `FLIGHT_ITINERARY`, `ACCOMMODATION_VOUCHER`, `ACTIVITY_VOUCHER`, `PARK_PERMITS`, `TRANSFER_VOUCHER`, `TRAVEL_INSURANCE`, `EMERGENCY_CONTACTS`, `PACKING_LIST`, `TERMS_CONDITIONS`, `CANCELLATION_POLICY`, `HEALTH_REQUIREMENTS`, `VISA_REQUIREMENTS`, `SAFARI_GUIDELINES`, `CUSTOM`, `OTHER`

**Available SafariState Values:**
- `PLANNING`, `INQUIRY`, `QUOTED`, `BOOKING_PENDING`, `DEPOSIT_RECEIVED`, `CONFIRMED`, `FULLY_PAID`, `PRE_DEPARTURE`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ON_HOLD`, `FOLLOW_UP`

**Response:**
```json
{
  "status": 200,
  "message": "Safari documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "safariId": "obfuscated_safari_id",
        "safariName": "Tanzania Safari & Zanzibar Beach Holiday",
        "safariCode": "SAF-5D4N-00001",
        "title": "Safari Quotation - Johnson Family",
        "documentType": "QUOTATION",
        "documentTypeDisplayName": "Quotation",
        "documentTypeDescription": "Price quotation for the safari itinerary",
        "documentUrl": "http://localhost:4450/api/safari-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/safari-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "quotation_johnson_family.pdf",
        "fileSize": 524288,
        "fileSizeFormatted": "512.0 KB",
        "fileType": "pdf",
        "description": "Complete quotation for 5-day safari",
        "version": "2.0",
        "notes": "Updated pricing for 2026",
        "validFrom": "2026-01-01T00:00:00",
        "validTo": "2026-12-31T23:59:59",
        "isCurrentlyValid": true,
        "isActive": true,
        "isGenerated": true,
        "createdAt": "2026-02-03T10:30:00",
        "updatedAt": "2026-02-03T10:30:00"
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

**Endpoint:** `GET /api/safari-documents/{id}`

**Permission:** `PERM_READ_SAFARI_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Safari document retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "safariId": "obfuscated_safari_id",
    "safariName": "Tanzania Safari & Zanzibar Beach Holiday",
    "safariCode": "SAF-5D4N-00001",
    "title": "Safari Quotation - Johnson Family",
    "documentType": "QUOTATION",
    "documentTypeDisplayName": "Quotation",
    "documentTypeDescription": "Price quotation for the safari itinerary",
    "documentUrl": "http://localhost:4450/api/safari-documents/obfuscated_id/file",
    "fileDocumentUrl": "http://localhost:4450/api/safari-documents/file/hashed_filename.pdf",
    "fileName": "hashed_filename.pdf",
    "originalFileName": "quotation_johnson_family.pdf",
    "fileSize": 524288,
    "fileSizeFormatted": "512.0 KB",
    "fileType": "pdf",
    "description": "Complete quotation for 5-day safari",
    "version": "2.0",
    "isCurrentlyValid": true,
    "isActive": true,
    "isGenerated": true,
    "createdAt": "2026-02-03T10:30:00",
    "updatedAt": "2026-02-03T10:30:00"
  }
}
```

---

### 3. Get Documents by Safari ID

**Endpoint:** `GET /api/safari-documents/safari/{safariId}`

**Permission:** `PERM_READ_SAFARI_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| safariId | String | Obfuscated safari ID |

**Response:**
```json
{
  "status": 200,
  "message": "Safari documents retrieved successfully",
  "data": [
    {
      "id": "obfuscated_id",
      "safariId": "obfuscated_safari_id",
      "title": "Safari Quotation - Johnson Family",
      "documentType": "QUOTATION",
      "isActive": true,
      "isGenerated": true
    }
  ]
}
```

---

### 4. Get Document File by Filename (Public)

**Endpoint:** `GET /api/safari-documents/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename |

**Description:** Serves the actual document file for viewing/downloading. Returns the file with appropriate Content-Type headers.

**Example:**
```
GET http://localhost:4450/api/safari-documents/file/a1b2c3d4e5f6...123_1738591830000.pdf
```

---

### 5. Serve Document File by ID (Public)

**Endpoint:** `GET /api/safari-documents/{id}/file`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Description:** Serves the actual document file using the obfuscated document ID. Returns the file with appropriate Content-Type headers and original filename.

**Example:**
```
GET http://localhost:4450/api/safari-documents/xyz123abc456/file
```

---

### 6. Upload Multiple Documents

**Endpoint:** `POST /api/safari-documents/upload`

**Permission:** `PERM_CREATE_SAFARI_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].safariId | String | Yes | Obfuscated safari ID |
| documents[0].document | File | Yes | Document file |
| documents[0].title | String | Yes | Document title |
| documents[0].documentType | DocumentType | No | Type of document (defaults to OTHER) |
| documents[0].description | String | No | Document description |
| documents[0].version | String | No | Document version |
| documents[0].validFrom | DateTime | No | Validity start date |
| documents[0].validTo | DateTime | No | Validity end date |
| documents[0].notes | String | No | Additional notes |

**Note:** The `isGenerated` field is automatically set to `false` for all manually uploaded documents. System-generated documents (from PDF generation) have `isGenerated=true` and are created via the PDF Generation API.

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/safari-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].safariId=obfuscated_safari_id" \
  -F "documents[0].document=@/path/to/document.pdf" \
  -F "documents[0].title=Safari Quotation - Johnson Family" \
  -F "documents[0].documentType=QUOTATION" \
  -F "documents[0].version=1.0"
```

**Example: Upload Multiple Documents:**
```bash
curl -X POST "http://localhost:4450/api/safari-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].safariId=obfuscated_safari_id" \
  -F "documents[0].document=@/path/to/quotation.pdf" \
  -F "documents[0].title=Safari Quotation" \
  -F "documents[0].documentType=QUOTATION" \
  -F "documents[1].safariId=obfuscated_safari_id" \
  -F "documents[1].document=@/path/to/itinerary.pdf" \
  -F "documents[1].title=Final Safari Itinerary" \
  -F "documents[1].documentType=FINAL_ITINERARY"
```

**Response:**
```json
{
  "status": 201,
  "message": "2 document(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "safariId": "obfuscated_safari_id",
      "title": "Safari Quotation",
      "documentType": "QUOTATION",
      "isGenerated": false,
      "createdAt": "2026-02-03T10:30:00"
    },
    {
      "id": "obfuscated_id_2",
      "safariId": "obfuscated_safari_id",
      "title": "Final Safari Itinerary",
      "documentType": "FINAL_ITINERARY",
      "isGenerated": false,
      "createdAt": "2026-02-03T10:30:00"
    }
  ]
}
```

---

### 7. Update Document Metadata

**Endpoint:** `PUT /api/safari-documents/{id}`

**Permission:** `PERM_UPDATE_SAFARI_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Request Body:**
```json
{
  "title": "Updated Safari Quotation - Johnson Family",
  "documentType": "QUOTATION",
  "description": "Updated description with new pricing",
  "version": "2.1",
  "notes": "Updated notes for 2026 season",
  "validFrom": "2026-01-01T00:00:00",
  "validTo": "2026-12-31T23:59:59",
  "isActive": true
}
```

**Note:** All fields are optional. Only provided fields will be updated.

**Response:**
```json
{
  "status": 200,
  "message": "Safari document updated successfully",
  "data": {
    "id": "obfuscated_id",
    "safariId": "obfuscated_safari_id",
    "title": "Updated Safari Quotation - Johnson Family",
    "documentType": "QUOTATION",
    "version": "2.1",
    "isActive": true,
    "updatedAt": "2026-02-03T11:00:00"
  }
}
```

---

### 8. Delete Single Document

**Endpoint:** `DELETE /api/safari-documents/{id}`

**Permission:** `PERM_DELETE_SAFARI_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Safari document deleted successfully",
  "data": null
}
```

**Note:** This will permanently delete both the database record and the physical file from the storage directory.

---

### 9. Bulk Delete Documents

**Endpoint:** `DELETE /api/safari-documents`

**Permission:** `PERM_DELETE_SAFARI_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

**Example:**
```
DELETE /api/safari-documents?ids=id1,id2,id3
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
| INVALID_SAFARI_ID | The provided safari ID is invalid |
| INVALID_DOCUMENT_ID | The provided document ID is invalid |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |
| DOCUMENT_FETCH_FAILED | Failed to retrieve documents |
| DOCUMENT_UPDATE_FAILED | Failed to update document metadata |
| DOCUMENT_DELETE_FAILED | Failed to delete document |
| BULK_DELETE_FAILED | Failed to delete documents in bulk operation |
| NO_IDS_PROVIDED | No document IDs provided for bulk operation |

---

## Storage

Documents are stored at the configured path:
```
${safari.document.storage.path:./data/safari-documents/}
```

**File Naming Convention:**
- Files are stored with SHA-256 hash + timestamp to prevent naming conflicts
- Format: `{sha256_hash}_{timestamp}.{extension}`
- Original filename is preserved in database for display purposes

---

## Document Type Categories

### Quotation Documents (quotationDocumentsOnly filter)
- `QUOTATION` - Price quotation
- `PROFORMA_INVOICE` - Proforma invoice
- `INVOICE` - Invoice

### Travel Documents (travelDocumentsOnly filter)
- `TRAVEL_PLAN` - Travel plan
- `FINAL_ITINERARY` - Final itinerary
- `FLIGHT_ITINERARY` - Flight schedule

### Voucher Documents (voucherDocumentsOnly filter)
- `ACCOMMODATION_VOUCHER` - Hotel/lodge vouchers
- `ACTIVITY_VOUCHER` - Activity booking vouchers
- `TRANSFER_VOUCHER` - Transfer confirmations
- `PARK_PERMITS` - Park entry permits

### Other Document Types
- `BOOKING_CONFIRMATION` - Booking confirmation
- `RECEIPT` - Payment receipt
- `VISA_SUPPORT_LETTER` - Visa support letter
- `TRAVEL_INSURANCE` - Travel insurance documents
- `EMERGENCY_CONTACTS` - Emergency contact information
- `PACKING_LIST` - Recommended packing list
- `TERMS_CONDITIONS` - Terms and conditions
- `CANCELLATION_POLICY` - Cancellation policy
- `HEALTH_REQUIREMENTS` - Vaccination requirements
- `VISA_REQUIREMENTS` - Visa requirements
- `SAFARI_GUIDELINES` - Safari guidelines
- `CUSTOM` - Custom documents
- `OTHER` - Other document types

---

## System-Generated Documents

Documents can be created in two ways:

### 1. Manual Upload (isGenerated = false)
Documents uploaded via the `/api/safari-documents/upload` endpoint are marked as `isGenerated=false`. These are manually uploaded files like contracts, client-provided documents, scanned receipts, etc.

### 2. PDF Generation (isGenerated = true)
Documents created via the PDF Generation API with `saveToDocuments=true` are automatically marked as `isGenerated=true`. These include:
- Generated quotation PDFs
- Final safari itinerary documents
- Invoice PDFs
- Travel plan documents
- Safari vouchers

**Creating System-Generated Documents via PDF API:**

```bash
# Generate a Safari PDF and save it as a SafariDocument
curl -X GET "http://localhost:4450/api/pdf/safari/obfuscated_safari_id?saveToDocuments=true&documentType=QUOTATION&documentTitle=Safari%20Quotation" \
  -H "Authorization: Bearer your_jwt_token" \
  --output quotation.pdf

# The response headers will include:
# X-Document-Saved: true
# X-Document-Id: obfuscated_document_id
# X-Document-Url: http://localhost:4450/api/safari-documents/obfuscated_document_id/file
```

**Or via POST:**
```bash
curl -X POST "http://localhost:4450/api/pdf/generate" \
  -H "Authorization: Bearer your_jwt_token" \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "FULL_SAFARI",
    "dataId": "obfuscated_safari_id",
    "saveToDocuments": true,
    "safariDocumentType": "QUOTATION",
    "documentTitle": "Safari Quotation - Johnson Family",
    "documentVersion": "1.0"
  }' \
  --output quotation.pdf
```

**Filtering by isGenerated:**

```bash
# Get only system-generated documents
curl -X GET "http://localhost:4450/api/safari-documents?isGenerated=true" \
  -H "Authorization: Bearer your_jwt_token"

# Get only manually uploaded documents
curl -X GET "http://localhost:4450/api/safari-documents?isGenerated=false" \
  -H "Authorization: Bearer your_jwt_token"
```

---

## Safari State Integration

Documents can be filtered by the current state of the associated safari:

**SafariState Values:**
- `PLANNING` - Safari is in planning phase
- `INQUIRY` - Initial customer inquiry
- `QUOTED` - Quotation has been sent
- `BOOKING_PENDING` - Awaiting booking confirmation
- `DEPOSIT_RECEIVED` - Deposit payment received
- `CONFIRMED` - Safari booking confirmed
- `FULLY_PAID` - Full payment received
- `PRE_DEPARTURE` - Preparing for safari departure
- `IN_PROGRESS` - Safari is currently ongoing
- `COMPLETED` - Safari has been completed
- `CANCELLED` - Safari was cancelled
- `ON_HOLD` - Safari is temporarily on hold
- `FOLLOW_UP` - Follow-up required

**Example: Get documents for confirmed safaris only**
```bash
curl -X GET "http://localhost:4450/api/safari-documents?safariState=CONFIRMED" \
  -H "Authorization: Bearer your_jwt_token"
```

---

## Document Validity

Documents can have optional validity periods defined by `validFrom` and `validTo` timestamps:

**Valid Document:**
- `validFrom` is null or in the past
- `validTo` is null or in the future
- `isActive` is true

**Filter for Currently Valid Documents:**
```bash
curl -X GET "http://localhost:4450/api/safari-documents?currentlyValid=true" \
  -H "Authorization: Bearer your_jwt_token"
```

This is useful for:
- Finding active quotations
- Retrieving valid travel insurance documents
- Getting current booking confirmations

---

## File Size Limits

Document uploads are subject to configurable size limits:

- **Maximum File Size:** Configured via application settings
- **Maximum Request Size:** Configured via application settings
- **Allowed File Types:** PDF, DOC, DOCX, XLS, XLSX, PNG, JPG, ZIP, TXT, CSV

To get current limits:
- Check application properties or
- Contact system administrator

---

## Related APIs

| API | Description |
|-----|-------------|
| `/api/pdf/generate` | Generate PDFs with optional save to documents |
| `/api/pdf/safari/{id}` | Generate safari PDF with optional save |
| `/api/safaris` | Manage safaris |
| `/api/safaris/{safariId}/days` | Manage safari days |

---

## Usage Examples

### Complete Workflow: Upload and Manage Safari Documents

```bash
# 1. Upload a quotation document
curl -X POST "http://localhost:4450/api/safari-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].safariId=xyz123" \
  -F "documents[0].document=@quotation.pdf" \
  -F "documents[0].title=Safari Quotation v1.0" \
  -F "documents[0].documentType=QUOTATION" \
  -F "documents[0].version=1.0"

# Response includes document ID: abc456

# 2. Get all documents for a specific safari
curl -X GET "http://localhost:4450/api/safari-documents/safari/xyz123" \
  -H "Authorization: Bearer your_jwt_token"

# 3. Update document metadata
curl -X PUT "http://localhost:4450/api/safari-documents/abc456" \
  -H "Authorization: Bearer your_jwt_token" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.1",
    "notes": "Updated pricing for group discount"
  }'

# 4. Access document file (public URL - no auth required)
curl -X GET "http://localhost:4450/api/safari-documents/abc456/file" \
  --output downloaded_quotation.pdf

# 5. Delete document when no longer needed
curl -X DELETE "http://localhost:4450/api/safari-documents/abc456" \
  -H "Authorization: Bearer your_jwt_token"
```

### Filter Examples

```bash
# Get all quotation documents
curl -X GET "http://localhost:4450/api/safari-documents?quotationDocumentsOnly=true" \
  -H "Authorization: Bearer your_jwt_token"

# Get documents for a specific safari in CONFIRMED state
curl -X GET "http://localhost:4450/api/safari-documents?safariId=xyz123&safariState=CONFIRMED" \
  -H "Authorization: Bearer your_jwt_token"

# Get all currently valid travel documents
curl -X GET "http://localhost:4450/api/safari-documents?travelDocumentsOnly=true&currentlyValid=true" \
  -H "Authorization: Bearer your_jwt_token"

# Get system-generated documents only
curl -X GET "http://localhost:4450/api/safari-documents?isGenerated=true" \
  -H "Authorization: Bearer your_jwt_token"

# Search documents by title
curl -X GET "http://localhost:4450/api/safari-documents?title=Johnson" \
  -H "Authorization: Bearer your_jwt_token"
```

---

## Version
API Version: 1.0

Last Updated: 2026-02-03
