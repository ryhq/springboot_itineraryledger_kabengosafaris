# PDF Template API Documentation

## Overview
The PDF Template API provides CRUD operations for managing PDF templates within document types. Templates contain the Thymeleaf HTML content used to generate PDFs with specific page settings (paper size, orientation, margins).

---

## Base URL
```
/api/pdf-templates
```

---

## Data Transfer Objects (DTOs)

### 1. CreatePdfTemplateDTO (Request)
Used when creating a new template.

```json
{
  "name": "string (required, 2-200 chars, unique per document type)",
  "description": "string (optional, max 500 chars)",
  "content": "string (required, Thymeleaf HTML content)",
  "paperSize": "enum (optional, default: A4)",
  "orientation": "enum (optional, default: PORTRAIT)",
  "marginTop": "integer (optional, default: 20mm)",
  "marginBottom": "integer (optional, default: 20mm)",
  "marginLeft": "integer (optional, default: 15mm)",
  "marginRight": "integer (optional, default: 15mm)",
  "isDefault": "boolean (optional, default: false)",
  "enabled": "boolean (optional, default: true)",
  "version": "string (optional, max 50 chars)"
}
```

**Example:**
```json
{
  "name": "Luxury Safari Template",
  "description": "Premium template with gold accents",
  "content": "<!DOCTYPE html><html>...</html>",
  "paperSize": "A4",
  "orientation": "PORTRAIT",
  "marginTop": 25,
  "marginBottom": 25,
  "marginLeft": 20,
  "marginRight": 20,
  "isDefault": false,
  "enabled": true,
  "version": "1.0"
}
```

---

### 2. UpdatePdfTemplateDTO (Request)
Used when updating a template. All fields are optional.

```json
{
  "name": "string (optional, 2-200 chars)",
  "description": "string (optional, max 500 chars)",
  "content": "string (optional, Thymeleaf HTML content)",
  "paperSize": "enum (optional)",
  "orientation": "enum (optional)",
  "marginTop": "integer (optional)",
  "marginBottom": "integer (optional)",
  "marginLeft": "integer (optional)",
  "marginRight": "integer (optional)",
  "isDefault": "boolean (optional)",
  "enabled": "boolean (optional)",
  "version": "string (optional, max 50 chars)"
}
```

**Example - Update name and set as default:**
```json
{
  "name": "Updated Template Name",
  "isDefault": true
}
```

---

### 3. PdfTemplateDTO (Response)
Returned in API responses.

```json
{
  "id": "string (obfuscated ID)",
  "pdfDocumentId": "string (obfuscated document type ID)",
  "pdfDocumentName": "string (e.g., 'FULL_ITINERARY')",
  "pdfDocumentDisplayName": "string (e.g., 'Full Safari Itinerary')",
  "name": "string",
  "description": "string",
  "fileName": "string (storage file name)",
  "paperSize": "enum (A4, A3, A5, LETTER, LEGAL, TABLOID)",
  "paperSizeDisplayName": "string",
  "orientation": "enum (PORTRAIT, LANDSCAPE)",
  "orientationDisplayName": "string",
  "marginTop": "integer (mm)",
  "marginBottom": "integer (mm)",
  "marginLeft": "integer (mm)",
  "marginRight": "integer (mm)",
  "isDefault": "boolean",
  "isSystemDefault": "boolean",
  "enabled": "boolean",
  "fileSize": "long (bytes)",
  "fileSizeFormatted": "string (e.g., '15.2 KB')",
  "version": "string",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
  "content": "string (only when explicitly requested)"
}
```

---

## Enums

### PaperSize
| Value | Display Name | Dimensions (mm) |
|-------|--------------|-----------------|
| `A4` | A4 | 210 x 297 |
| `A3` | A3 | 297 x 420 |
| `A5` | A5 | 148 x 210 |
| `LETTER` | Letter | 216 x 279 |
| `LEGAL` | Legal | 216 x 356 |
| `TABLOID` | Tabloid | 279 x 432 |

### Orientation
| Value | Display Name |
|-------|--------------|
| `PORTRAIT` | Portrait |
| `LANDSCAPE` | Landscape |

---

## API Response Wrapper

All responses follow a standard format:

```json
{
  "success": "boolean",
  "statusCode": "integer (HTTP status)",
  "message": "string",
  "data": "object or array (optional)",
  "errorCode": "string (only on errors)",
  "timestamp": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Template
**POST** `/api/pdf-templates/document/{documentId}`

Creates a new PDF template for a specific document type.

**Permission Required:** `PERM_CREATE_PDF_TEMPLATE`

**Path Parameters:**
```
documentId = obfuscated PDF document type ID (string)
```

**Request Body:** `CreatePdfTemplateDTO`

**Example Request:**
```bash
POST /api/pdf-templates/document/encoded_doc_123
Content-Type: application/json

{
  "name": "Premium Safari Template",
  "description": "High-end template with premium styling",
  "content": "<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\">...</html>",
  "paperSize": "A4",
  "orientation": "PORTRAIT",
  "marginTop": 20,
  "marginBottom": 20,
  "marginLeft": 15,
  "marginRight": 15,
  "enabled": true
}
```

**Success Response (201):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Template created successfully",
  "data": {
    "id": "encoded_template_456",
    "pdfDocumentId": "encoded_doc_123",
    "pdfDocumentName": "FULL_ITINERARY",
    "name": "Premium Safari Template",
    "enabled": true,
    "isDefault": false,
    "isSystemDefault": false
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Duplicate Name):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Template with this name already exists for this document type",
  "errorCode": "DUPLICATE_TEMPLATE_NAME",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 2. Get All Templates (with filtering)
**GET** `/api/pdf-templates`

Retrieves all templates with optional filtering and pagination. Use `documentId` parameter to filter by document type.

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Query Parameters:**
```
documentId = string (optional, filter by document type)
enabled = boolean (optional)
isDefault = boolean (optional)
isSystemDefault = boolean (optional)
name = string (optional, partial match)
page = integer (default: 0)
size = integer (default: 10)
sortDirection = asc|desc (default: desc)
```

**Example Requests:**
```
# Get all templates for a document type
GET /api/pdf-templates?documentId=encoded_doc_123

# Get only enabled templates
GET /api/pdf-templates?enabled=true&page=0&size=20

# Get default templates
GET /api/pdf-templates?isDefault=true

# Search by name
GET /api/pdf-templates?name=premium&sortDirection=asc
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Templates retrieved successfully",
  "data": {
    "content": [
      {
        "id": "encoded_template_456",
        "pdfDocumentId": "encoded_doc_123",
        "pdfDocumentName": "FULL_ITINERARY",
        "pdfDocumentDisplayName": "Full Safari Itinerary",
        "name": "Premium Safari Template",
        "description": "High-end template",
        "paperSize": "A4",
        "paperSizeDisplayName": "A4",
        "orientation": "PORTRAIT",
        "orientationDisplayName": "Portrait",
        "isDefault": false,
        "isSystemDefault": false,
        "enabled": true,
        "fileSizeFormatted": "12.5 KB"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "currentPage": 0,
    "size": 10
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 3. Get Template by ID
**GET** `/api/pdf-templates/{id}`

Retrieves a single template by ID (without content).

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated template ID (string)
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Template retrieved successfully",
  "data": {
    "id": "encoded_template_456",
    "pdfDocumentId": "encoded_doc_123",
    "pdfDocumentName": "FULL_ITINERARY",
    "name": "Premium Safari Template",
    "paperSize": "A4",
    "orientation": "PORTRAIT",
    "marginTop": 20,
    "marginBottom": 20,
    "marginLeft": 15,
    "marginRight": 15,
    "isDefault": false,
    "isSystemDefault": false,
    "enabled": true
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Template not found",
  "errorCode": "TEMPLATE_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 4. Get Template Content
**GET** `/api/pdf-templates/{id}/content`

Retrieves a template including its HTML content.

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated template ID (string)
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Template content retrieved successfully",
  "data": {
    "id": "encoded_template_456",
    "name": "Premium Safari Template",
    "content": "<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\">...</html>",
    "fileSize": 12800,
    "fileSizeFormatted": "12.5 KB"
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 5. Update Template
**PUT** `/api/pdf-templates/{id}`

Updates an existing template. Only provided fields will be updated. Use `isDefault: true` to set this template as the default for its document type.

**Permission Required:** `PERM_UPDATE_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated template ID (string)
```

**Request Body:** `UpdatePdfTemplateDTO`

**Example - Update settings and set as default:**
```json
{
  "name": "Updated Template Name",
  "paperSize": "LETTER",
  "orientation": "LANDSCAPE",
  "isDefault": true
}
```

**Example - Update content only:**
```json
{
  "content": "<!DOCTYPE html><html>...updated content...</html>"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Template updated successfully",
  "data": {
    "id": "encoded_template_456",
    "name": "Updated Template Name",
    "paperSize": "LETTER",
    "orientation": "LANDSCAPE",
    "isDefault": true,
    "updatedAt": "2025-01-19T10:35:00"
  },
  "timestamp": "2025-01-19T10:35:00"
}
```

**Note:** When setting `isDefault: true`, all other templates for the same document type will have their `isDefault` flag set to `false`.

---

### 6. Restore System Default Template
**POST** `/api/pdf-templates/{id}/restore`

Restores a system default template to its original content. Only works for templates where `isSystemDefault: true`.

**Permission Required:** `PERM_UPDATE_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated template ID (string)
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Template restored to original",
  "data": {
    "id": "encoded_template_456",
    "name": "Default Full Itinerary",
    "isSystemDefault": true,
    "fileSize": 15200,
    "fileSizeFormatted": "14.8 KB"
  },
  "timestamp": "2025-01-19T10:35:00"
}
```

**Error Response (400 - Not System Default):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Only system default templates can be restored",
  "errorCode": "NOT_SYSTEM_DEFAULT",
  "timestamp": "2025-01-19T10:35:00"
}
```

---

### 7. Delete Template
**DELETE** `/api/pdf-templates/{id}`

Deletes a single template.

**Permission Required:** `PERM_DELETE_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated template ID (string)
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Template deleted successfully",
  "data": {
    "deletedCount": 1
  },
  "timestamp": "2025-01-19T10:40:00"
}
```

---

### 8. Delete Multiple Templates (Bulk)
**DELETE** `/api/pdf-templates`

Deletes multiple templates in a single operation.

**Permission Required:** `PERM_DELETE_PDF_TEMPLATE`

**Request Body:**
```json
["encoded_id_1", "encoded_id_2", "encoded_id_3"]
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Templates deleted successfully",
  "data": {
    "deletedCount": 3
  },
  "timestamp": "2025-01-19T10:40:00"
}
```

---

### 9. Generate PDF
**POST** `/api/pdf-templates/{templateId}/generate`

Generates a PDF using the specified template and data. Returns binary PDF file. Supports optional translation to other languages.

**Permission Required:** `PERM_GENERATE_PDF`

**Path Parameters:**
```
templateId = obfuscated template ID (string)
```

**Query Parameters:**
```
documentType = string (required, e.g., "FULL_ITINERARY")
dataId = string (required, obfuscated ID of the data source, e.g., itinerary ID)
language = string (optional, target language code for translation, e.g., "fr", "de", "es")
```

**Example Request:**
```bash
# Generate PDF in English (default)
POST /api/pdf-templates/encoded_template_456/generate?documentType=FULL_ITINERARY&dataId=encoded_itinerary_789

# Generate PDF translated to French
POST /api/pdf-templates/encoded_template_456/generate?documentType=FULL_ITINERARY&dataId=encoded_itinerary_789&language=fr

# Generate PDF translated to German
POST /api/pdf-templates/encoded_template_456/generate?documentType=FULL_ITINERARY&dataId=encoded_itinerary_789&language=de
```

**Success Response:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="itinerary_xxx.pdf"`
- Body: Binary PDF data

**Note:** If translation fails (service unavailable, unsupported language), PDF will be generated in English as fallback.

**Error Response (400 - Document Type Disabled):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "PDF document type is disabled",
  "errorCode": "DOCUMENT_TYPE_DISABLED",
  "timestamp": "2025-01-19T10:45:00"
}
```

---

### 10. Preview PDF (HTML)
**POST** `/api/pdf-templates/{templateId}/preview`

Renders the template with data and returns HTML for preview (without generating PDF).

**Permission Required:** `PERM_GENERATE_PDF`

**Path Parameters:**
```
templateId = obfuscated template ID (string)
```

**Query Parameters:**
```
documentType = string (required)
dataId = string (required)
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Preview generated successfully",
  "data": {
    "html": "<!DOCTYPE html><html>...rendered HTML with data...</html>"
  },
  "timestamp": "2025-01-19T10:45:00"
}
```

---

## Error Codes

| Error Code | Status | Description |
|-----------|--------|-------------|
| `INVALID_TEMPLATE_ID` | 400 | Template ID could not be decoded |
| `INVALID_DOCUMENT_ID` | 400 | Document ID could not be decoded |
| `TEMPLATE_NOT_FOUND` | 404 | Template not found |
| `DOCUMENT_NOT_FOUND` | 404 | Document type not found |
| `DUPLICATE_TEMPLATE_NAME` | 400 | Template name already exists for this document type |
| `TEMPLATE_DISABLED` | 400 | Template is disabled |
| `NOT_SYSTEM_DEFAULT` | 400 | Only system default templates can be restored |
| `DOCUMENT_TYPE_DISABLED` | 400 | PDF document type is disabled |
| `CONTENT_UPDATE_FAILED` | 500 | Failed to update template file content |
| `TEMPLATE_CREATE_FAILED` | 500 | Failed to create template |
| `TEMPLATE_UPDATE_FAILED` | 500 | Failed to update template |
| `TEMPLATE_DELETE_FAILED` | 500 | Failed to delete template |
| `TEMPLATES_FETCH_FAILED` | 500 | Failed to retrieve templates |
| `PDF_GENERATION_FAILED` | 500 | Failed to generate PDF |

---

## Permissions Reference

| Permission | Description |
|------------|-------------|
| `PERM_CREATE_PDF_TEMPLATE` | Create new PDF templates |
| `PERM_READ_PDF_TEMPLATE` | View PDF templates |
| `PERM_UPDATE_PDF_TEMPLATE` | Update and restore PDF templates |
| `PERM_DELETE_PDF_TEMPLATE` | Delete PDF templates |
| `PERM_GENERATE_PDF` | Generate and preview PDFs |

---

## Example Workflows

### Create and Use a Custom Template

```bash
# 1. Create a new template
curl -X POST "http://localhost:8080/api/pdf-templates/document/doc_123" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Luxury Safari Template",
    "content": "<!DOCTYPE html>...",
    "paperSize": "A4",
    "enabled": true
  }'

# 2. Set as default
curl -X PUT "http://localhost:8080/api/pdf-templates/template_456" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"isDefault": true}'

# 3. Generate PDF (English)
curl -X POST "http://localhost:8080/api/pdf-templates/template_456/generate?documentType=FULL_ITINERARY&dataId=itinerary_789" \
  -H "Authorization: Bearer <token>" \
  --output itinerary.pdf

# 4. Generate PDF (French translation)
curl -X POST "http://localhost:8080/api/pdf-templates/template_456/generate?documentType=FULL_ITINERARY&dataId=itinerary_789&language=fr" \
  -H "Authorization: Bearer <token>" \
  --output itinerary_fr.pdf
```

### Filter Templates by Document Type

```bash
# Get all templates for FULL_ITINERARY document type
curl -X GET "http://localhost:8080/api/pdf-templates?documentId=doc_123&enabled=true" \
  -H "Authorization: Bearer <token>"
```

---

## Translation Support

PDF generation supports multi-language output via LibreTranslate integration.

### Supported Languages

| Code | Language |
|------|----------|
| `en` | English (default) |
| `fr` | French |
| `de` | German |
| `es` | Spanish |
| `it` | Italian |

### Related Translation APIs

| Endpoint | Description |
|----------|-------------|
| `GET /api/translation/languages` | Get available translation languages |
| `GET /api/translation/health` | Check translation service health |
| `GET /api/translation-settings` | View/update translation settings |

---

## Version
API Version: 1.1

Last Updated: 2026-01-22
