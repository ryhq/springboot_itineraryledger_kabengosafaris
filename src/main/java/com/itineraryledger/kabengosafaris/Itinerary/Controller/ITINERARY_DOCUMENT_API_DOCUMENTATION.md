# Itinerary Document Controller API Documentation

## Base URL
```
/api/itinerary-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_ITINERARY_DOCUMENT` - For reading itinerary documents
- `PERM_CREATE_ITINERARY_DOCUMENT` - For uploading documents
- `PERM_UPDATE_ITINERARY_DOCUMENT` - For updating document metadata
- `PERM_DELETE_ITINERARY_DOCUMENT` - For deleting documents

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/itinerary-documents`

**Permission:** `PERM_READ_ITINERARY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| itineraryId | String | No | - | Filter by itinerary (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| isGenerated | Boolean | No | - | Filter by whether document was system-generated |
| title | String | No | - | Filter by title (partial match) |
| version | String | No | - | Filter by version |
| currentlyValid | Boolean | No | - | Filter for currently valid documents only |
| itineraryName | String | No | - | Filter by itinerary name (partial match) |
| itineraryCode | String | No | - | Filter by itinerary code (partial match) |
| itineraryIsActive | Boolean | No | - | Filter by itinerary active status |
| itineraryStatus | ItineraryStatus | No | - | Filter by itinerary status |
| tripType | TripType | No | - | Filter by trip type |
| budgetCategory | BudgetCategory | No | - | Filter by budget category |
| quotationDocumentsOnly | Boolean | No | - | Filter for quotation/invoice documents only |
| travelDocumentsOnly | Boolean | No | - | Filter for travel plan documents only |
| voucherDocumentsOnly | Boolean | No | - | Filter for voucher documents only |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `QUOTATION`, `TRAVEL_PLAN`, `FINAL_ITINERARY`, `BOOKING_CONFIRMATION`, `INVOICE`, `PROFORMA_INVOICE`, `RECEIPT`, `VISA_SUPPORT_LETTER`, `FLIGHT_ITINERARY`, `ACCOMMODATION_VOUCHER`, `ACTIVITY_VOUCHER`, `PARK_PERMITS`, `TRANSFER_VOUCHER`, `TRAVEL_INSURANCE`, `EMERGENCY_CONTACTS`, `PACKING_LIST`, `TERMS_CONDITIONS`, `CANCELLATION_POLICY`, `HEALTH_REQUIREMENTS`, `VISA_REQUIREMENTS`, `SAFARI_GUIDELINES`, `CUSTOM`, `OTHER`

**Available ItineraryStatus Values:**
- `DRAFT`, `COMPLETE`, `PUBLISHED`, `ARCHIVED`

**Available TripType Values:**
- Refer to TripType enum in the codebase

**Available BudgetCategory Values:**
- Refer to BudgetCategory enum in the codebase

**Response:**
```json
{
  "status": 200,
  "message": "Itinerary documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "itineraryId": "obfuscated_itinerary_id",
        "itineraryName": "7-Day Serengeti & Ngorongoro Safari",
        "itineraryCode": "ITI-7D6N-001",
        "title": "Safari Quotation - Johnson Family",
        "documentType": "QUOTATION",
        "documentTypeDisplayName": "Quotation",
        "documentTypeDescription": "Price quotation for the safari itinerary",
        "documentUrl": "http://localhost:4450/api/itinerary-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/itinerary-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "quotation_johnson_family.pdf",
        "fileSize": 524288,
        "fileSizeFormatted": "512.0 KB",
        "fileType": "application/pdf",
        "description": "Complete quotation for 7-day safari",
        "version": "2.0",
        "notes": "Updated pricing for 2024",
        "validFrom": "2024-01-01T00:00:00",
        "validTo": "2024-12-31T23:59:59",
        "isCurrentlyValid": true,
        "isActive": true,
        "isGenerated": true,
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

**Endpoint:** `GET /api/itinerary-documents/{id}`

**Permission:** `PERM_READ_ITINERARY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

---

### 3. Get Documents by Itinerary ID

**Endpoint:** `GET /api/itinerary-documents/itinerary/{itineraryId}`

**Permission:** `PERM_READ_ITINERARY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| itineraryId | String | Obfuscated itinerary ID |

---

### 4. Get Document File by Filename (Public)

**Endpoint:** `GET /api/itinerary-documents/file/{fileName}`

**Permission:** None (Public)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| fileName | String | Stored filename |

---

### 5. Serve Document File by ID (Public)

**Endpoint:** `GET /api/itinerary-documents/{id}/file`

**Permission:** None (Public)

---

### 6. Upload Multiple Documents

**Endpoint:** `POST /api/itinerary-documents/upload`

**Permission:** `PERM_CREATE_ITINERARY_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].itineraryId | String | Yes | Obfuscated itinerary ID |
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
curl -X POST "http://localhost:4450/api/itinerary-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].itineraryId=obfuscated_itinerary_id" \
  -F "documents[0].document=@/path/to/document.pdf" \
  -F "documents[0].title=Safari Quotation - Johnson Family" \
  -F "documents[0].documentType=QUOTATION" \
  -F "documents[0].version=1.0"
```

---

### 7. Update Document Metadata

**Endpoint:** `PUT /api/itinerary-documents/{id}`

**Permission:** `PERM_UPDATE_ITINERARY_DOCUMENT`

**Request Body:**
```json
{
  "title": "Updated Title",
  "documentType": "QUOTATION",
  "description": "Updated description",
  "version": "2.1",
  "notes": "Updated notes",
  "validFrom": "2024-01-01T00:00:00",
  "validTo": "2024-12-31T23:59:59",
  "isActive": true
}
```

---

### 8. Delete Single Document

**Endpoint:** `DELETE /api/itinerary-documents/{id}`

**Permission:** `PERM_DELETE_ITINERARY_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

---

### 9. Bulk Delete Documents

**Endpoint:** `DELETE /api/itinerary-documents`

**Permission:** `PERM_DELETE_ITINERARY_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_ITINERARY_ID | The provided itinerary ID is invalid |
| INVALID_DOCUMENT_ID | The provided document ID is invalid |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |

---

## Storage

Documents are stored at the configured path:
```
${itinerary.document.storage.path:./data/itinerary-documents/}
```

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

---

## System-Generated Documents

Documents can be created in two ways:

### 1. Manual Upload (isGenerated = false)
Documents uploaded via the `/api/itinerary-documents/upload` endpoint are marked as `isGenerated=false`. These are manually uploaded files like contracts, client-provided documents, etc.

### 2. PDF Generation (isGenerated = true)
Documents created via the PDF Generation API with `saveToDocuments=true` are automatically marked as `isGenerated=true`. These include:
- Generated quotation PDFs
- Final itinerary documents
- Invoice PDFs
- Travel plan documents

**Creating System-Generated Documents via PDF API:**

```bash
# Generate a PDF and save it as an ItineraryDocument
curl -X GET "http://localhost:4450/api/pdf/itinerary/obfuscated_itinerary_id?saveToDocuments=true&documentType=QUOTATION&documentTitle=Safari%20Quotation" \
  -H "Authorization: Bearer your_jwt_token" \
  --output quotation.pdf

# The response headers will include:
# X-Document-Saved: true
# X-Document-Id: obfuscated_document_id
# X-Document-Url: http://localhost:4450/api/itinerary-documents/obfuscated_document_id/file
```

**Or via POST:**
```bash
curl -X POST "http://localhost:4450/api/pdf/generate" \
  -H "Authorization: Bearer your_jwt_token" \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "FULL_ITINERARY",
    "dataId": "obfuscated_itinerary_id",
    "saveToDocuments": true,
    "itineraryDocumentType": "QUOTATION",
    "documentTitle": "Safari Quotation - Johnson Family",
    "documentVersion": "1.0"
  }' \
  --output quotation.pdf
```

**Filtering by isGenerated:**

```bash
# Get only system-generated documents
curl -X GET "http://localhost:4450/api/itinerary-documents?isGenerated=true" \
  -H "Authorization: Bearer your_jwt_token"

# Get only manually uploaded documents
curl -X GET "http://localhost:4450/api/itinerary-documents?isGenerated=false" \
  -H "Authorization: Bearer your_jwt_token"
```

---

## Related APIs

| API | Description |
|-----|-------------|
| `/api/pdf/generate` | Generate PDFs with optional save to documents |
| `/api/pdf/itinerary/{id}` | Generate itinerary PDF with optional save |
| `/api/itineraries` | Manage itineraries |

---

## Version
API Version: 1.1

Last Updated: 2026-01-30
