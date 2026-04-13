# Customer Document Controller API Documentation

## Base URL
```
/api/customer-documents
```

## Authentication
All endpoints except file serving require authentication via JWT token.

**Required Permissions:**
- `PERM_READ_CUSTOMER_DOCUMENT` - For reading customer documents
- `PERM_CREATE_CUSTOMER_DOCUMENT` - For uploading documents
- `PERM_UPDATE_CUSTOMER_DOCUMENT` - For updating document metadata
- `PERM_DELETE_CUSTOMER_DOCUMENT` - For deleting documents

---

## Endpoints

### 1. Get All Documents (with filters and pagination)

**Endpoint:** `GET /api/customer-documents`

**Permission:** `PERM_READ_CUSTOMER_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| customerId | String | No | - | Filter by customer (obfuscated ID) |
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| title | String | No | - | Filter by title (partial match) |
| documentNumber | String | No | - | Filter by document number (partial match) |
| version | String | No | - | Filter by version (partial match) |
| currentlyValid | Boolean | No | - | Filter only currently valid documents |
| customerName | String | No | - | Filter by customer name (partial match) |
| customerType | CustomerType | No | - | Filter by customer type |
| email | String | No | - | Filter by customer email (partial match) |
| identityDocumentsOnly | Boolean | No | - | Filter only PASSPORT, ID_CARD, DRIVERS_LICENSE |
| travelDocumentsOnly | Boolean | No | - | Filter only PASSPORT, VISA, INSURANCE, VACCINATION |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Available DocumentType Values:**
- `PASSPORT` - Passport scan or copy
- `VISA` - Visa copy or travel authorization
- `ID_CARD` - National ID or identity card
- `DRIVERS_LICENSE` - Driving license
- `INSURANCE` - Travel or medical insurance document
- `VACCINATION` - Vaccination certificate (e.g., Yellow Fever)
- `MEDICAL` - Medical certificates or health documents
- `PRESCRIPTION` - Medical prescriptions
- `CONTRACT` - Signed booking contract or agreement
- `INVOICE` - Invoice or receipt
- `CORPORATE_REG` - Company registration documents
- `TAX_ID` - Tax identification documents
- `POWER_OF_ATTORNEY` - Authorization documents
- `CONSENT` - Consent or waiver forms
- `EMERGENCY_CONTACT` - Emergency contact information
- `FLIGHT_TICKET` - Flight booking or ticket
- `HOTEL_VOUCHER` - Hotel or accommodation voucher
- `ITINERARY` - Travel itinerary
- `REFERENCE` - Reference or recommendation letter
- `PHOTO` - Customer photo or headshot
- `OTHER` - Other document type

**Available CustomerType Values:**
- `INDIVIDUAL` - Individual customer
- `CORPORATE` - Corporate customer
- `TRAVEL_AGENT` - Travel agent

**Response:**
```json
{
  "status": 200,
  "message": "Customer documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id",
        "customerId": "obfuscated_customer_id",
        "customerName": "John Doe",
        "title": "Passport Copy",
        "documentType": "PASSPORT",
        "documentTypeDisplayName": "Passport",
        "documentTypeDescription": "Passport scan or copy",
        "documentUrl": "http://localhost:4450/api/customer-documents/obfuscated_id/file",
        "fileDocumentUrl": "http://localhost:4450/api/customer-documents/file/hashed_filename.pdf",
        "fileName": "hashed_filename.pdf",
        "originalFileName": "john_doe_passport.pdf",
        "fileSize": 1234567,
        "fileSizeFormatted": "1.2 MB",
        "fileType": "application/pdf",
        "description": "Valid passport for international travel",
        "documentNumber": "AB1234567",
        "version": "2024",
        "notes": "Valid until 2030",
        "validFrom": "2020-06-15T00:00:00",
        "validTo": "2030-06-15T23:59:59",
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

**Endpoint:** `GET /api/customer-documents/{id}`

**Permission:** `PERM_READ_CUSTOMER_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Customer document retrieved successfully",
  "data": {
    "id": "obfuscated_id",
    "customerId": "obfuscated_customer_id",
    "customerName": "Jane Smith",
    "title": "Travel Insurance",
    "documentType": "INSURANCE",
    "documentTypeDisplayName": "Insurance",
    "documentTypeDescription": "Travel or medical insurance document",
    "documentUrl": "http://localhost:4450/api/customer-documents/obfuscated_id/file",
    "fileDocumentUrl": "http://localhost:4450/api/customer-documents/file/hashed_filename.pdf",
    "fileName": "hashed_filename.pdf",
    "originalFileName": "travel_insurance_2024.pdf",
    "fileSize": 567890,
    "fileSizeFormatted": "554.6 KB",
    "fileType": "application/pdf",
    "description": "Comprehensive travel insurance coverage",
    "documentNumber": "INS-2024-001234",
    "version": "v1.0",
    "notes": "Covers medical expenses up to $100,000",
    "validFrom": "2024-01-01T00:00:00",
    "validTo": "2024-12-31T23:59:59",
    "isCurrentlyValid": true,
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 3. Get Documents by Customer ID (with filters and pagination)

**Endpoint:** `GET /api/customer-documents/customer/{customerId}`

**Permission:** `PERM_READ_CUSTOMER_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| customerId | String | Obfuscated customer ID (required) |

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| documentType | DocumentType | No | - | Filter by document type |
| isActive | Boolean | No | - | Filter by active status |
| title | String | No | - | Filter by title (partial match) |
| documentNumber | String | No | - | Filter by document number (partial match) |
| version | String | No | - | Filter by version (partial match) |
| currentlyValid | Boolean | No | - | Filter only currently valid documents |
| identityDocumentsOnly | Boolean | No | - | Filter only PASSPORT, ID_CARD, DRIVERS_LICENSE |
| travelDocumentsOnly | Boolean | No | - | Filter only PASSPORT, VISA, INSURANCE, VACCINATION |
| sortBy | String | No | "createdAt" | Field to sort by |
| sortDirection | String | No | "desc" | Sort direction (asc/desc) |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

**Example Requests:**
```
GET /api/customer-documents/customer/abc123xyz

GET /api/customer-documents/customer/abc123xyz?documentType=PASSPORT&isActive=true

GET /api/customer-documents/customer/abc123xyz?identityDocumentsOnly=true&page=0&size=10

GET /api/customer-documents/customer/abc123xyz?currentlyValid=true&sortBy=validTo&sortDirection=asc
```

**Response:**
```json
{
  "status": 200,
  "message": "Customer documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "obfuscated_id_1",
        "customerId": "obfuscated_customer_id",
        "customerName": "John Doe",
        "title": "Passport Copy",
        "documentType": "PASSPORT",
        ...
      },
      {
        "id": "obfuscated_id_2",
        "customerId": "obfuscated_customer_id",
        "customerName": "John Doe",
        "title": "Travel Insurance",
        "documentType": "INSURANCE",
        ...
      }
    ],
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 2,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

---

### 4. Get Document File by Filename (Public)

**Endpoint:** `GET /api/customer-documents/file/{fileName}`

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

**Endpoint:** `GET /api/customer-documents/{id}/file`

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

**Endpoint:** `POST /api/customer-documents/upload`

**Permission:** `PERM_CREATE_CUSTOMER_DOCUMENT`

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documents[0].customerId | String | Yes | Obfuscated customer ID |
| documents[0].document | File | Yes | Document file |
| documents[0].title | String | Yes | Document title |
| documents[0].documentType | DocumentType | No | Document type (defaults to OTHER) |
| documents[0].description | String | No | Document description |
| documents[0].documentNumber | String | No | Document number (passport #, etc.) |
| documents[0].version | String | No | Version string |
| documents[0].validFrom | DateTime | No | Validity start date (ISO 8601) |
| documents[0].validTo | DateTime | No | Validity end date (ISO 8601) |
| documents[0].notes | String | No | Additional notes |

**Example cURL:**
```bash
curl -X POST "http://localhost:4450/api/customer-documents/upload" \
  -H "Authorization: Bearer your_jwt_token" \
  -F "documents[0].customerId=obfuscated_customer_id" \
  -F "documents[0].document=@/path/to/passport.pdf" \
  -F "documents[0].title=Passport Copy" \
  -F "documents[0].documentType=PASSPORT" \
  -F "documents[0].documentNumber=AB1234567" \
  -F "documents[0].validFrom=2020-06-15T00:00:00" \
  -F "documents[0].validTo=2030-06-15T23:59:59" \
  -F "documents[1].customerId=obfuscated_customer_id" \
  -F "documents[1].document=@/path/to/insurance.pdf" \
  -F "documents[1].title=Travel Insurance" \
  -F "documents[1].documentType=INSURANCE"
```

**Success Response:**
```json
{
  "status": 201,
  "message": "2 document(s) uploaded successfully",
  "data": [
    {
      "id": "obfuscated_id_1",
      "customerId": "obfuscated_customer_id",
      "customerName": "John Doe",
      "title": "Passport Copy",
      "documentType": "PASSPORT",
      ...
    },
    {
      "id": "obfuscated_id_2",
      "customerId": "obfuscated_customer_id",
      "customerName": "John Doe",
      "title": "Travel Insurance",
      "documentType": "INSURANCE",
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

**Endpoint:** `PUT /api/customer-documents/{id}`

**Permission:** `PERM_UPDATE_CUSTOMER_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Request Body:**
```json
{
  "title": "Updated Passport Copy",
  "documentType": "PASSPORT",
  "description": "Updated description",
  "documentNumber": "AB9876543",
  "version": "2024",
  "notes": "Renewed passport",
  "validFrom": "2024-01-01T00:00:00",
  "validTo": "2034-01-01T23:59:59",
  "isActive": true
}
```

All fields are optional. Only provided fields will be updated.

**Response:**
```json
{
  "status": 200,
  "message": "Customer document updated successfully",
  "data": {
    "id": "obfuscated_id",
    "customerId": "obfuscated_customer_id",
    "title": "Updated Passport Copy",
    "documentNumber": "AB9876543",
    ...
  }
}
```

---

### 8. Delete Single Document

**Endpoint:** `DELETE /api/customer-documents/{id}`

**Permission:** `PERM_DELETE_CUSTOMER_DOCUMENT`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated document ID |

**Response:**
```json
{
  "status": 200,
  "message": "Customer document deleted successfully",
  "data": null
}
```

---

### 9. Bulk Delete Documents

**Endpoint:** `DELETE /api/customer-documents`

**Permission:** `PERM_DELETE_CUSTOMER_DOCUMENT`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ids | List<String> | Yes | List of obfuscated document IDs to delete |

**Example:**
```
DELETE /api/customer-documents?ids=id1,id2,id3
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
| INVALID_CUSTOMER_ID | The provided customer ID is invalid or cannot be decoded |
| INVALID_DOCUMENT_ID | The provided document ID is invalid or cannot be decoded |
| DOCUMENT_NOT_FOUND | The requested document was not found |
| CUSTOMER_NOT_FOUND | The referenced customer was not found |
| NO_DOCUMENTS_PROVIDED | No documents were provided in the upload request |
| VALIDATION_ERROR | One or more validation errors occurred |
| REQUEST_SIZE_EXCEEDED | Total request size exceeds the maximum allowed |
| STORAGE_ERROR | Failed to save document to filesystem |
| DATABASE_ERROR | Failed to save document record to database |
| DOCUMENT_FETCH_FAILED | Failed to fetch customer documents |
| DOCUMENT_UPDATE_FAILED | Failed to update customer document |
| DOCUMENT_DELETE_FAILED | Failed to delete customer document |
| BULK_DELETE_FAILED | Failed to perform bulk delete operation |

---

## File Validation

Documents are validated against the following criteria (configurable via FileSettings):

- **Allowed Extensions:** pdf, doc, docx, xls, xlsx, csv, txt, zip, png, jpg, jpeg, gif, mp4, ppt, pptx, etc. (as configured)
- **Maximum File Size:** Configurable (default varies)
- **Maximum Request Size:** Configurable for bulk uploads

---

## Document Categories

### Identity Documents
Documents that verify customer identity:
- `PASSPORT`
- `ID_CARD`
- `DRIVERS_LICENSE`

Use filter: `identityDocumentsOnly=true`

### Travel Documents
Documents required for travel:
- `PASSPORT`
- `VISA`
- `INSURANCE`
- `VACCINATION`

Use filter: `travelDocumentsOnly=true`

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
${customer.document.storage.path:./data/customer-documents/}
```

Filenames are generated using SHA-256 hash + timestamp to ensure uniqueness and prevent conflicts.

---

## Special Notes

### Document Numbers
The `documentNumber` field is particularly useful for:
- Passport numbers
- Visa numbers
- Insurance policy numbers
- Contract reference numbers
- Tax ID numbers
- Any official document identifier

### Privacy Considerations
Customer documents may contain sensitive personal information. Ensure:
- Proper access controls are in place
- Documents are stored securely
- File access is logged for audit purposes
- Inactive documents are not publicly accessible
