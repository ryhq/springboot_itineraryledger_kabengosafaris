# PDF Generation API Documentation

## Overview
The PDF Generation API provides endpoints for generating PDF documents from various data sources. It supports generating full PDF files for download as well as previewing the rendered HTML before final generation.

---

## Base URL
```
/api/pdf
```

---

## Data Transfer Objects (DTOs)

### GeneratePdfRequestDTO (Request Body)
Used for POST endpoints to specify what PDF to generate.

```json
{
  "documentType": "string (required - document type name, e.g., 'FULL_ITINERARY')",
  "dataId": "string (required - obfuscated ID of the data source)",
  "templateId": "string (optional - specific template ID to use)",
  "fileName": "string (optional - custom filename for the generated PDF)",
  "language": "string (optional - target language code for translation)"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `documentType` | String | Yes | The PDF document type name (e.g., "FULL_ITINERARY") |
| `dataId` | String | Yes | The obfuscated ID of the data source (e.g., itinerary ID) |
| `templateId` | String | No | Specific template ID to use. If not provided, uses the default template for the document type |
| `fileName` | String | No | Custom file name for the generated PDF |
| `language` | String | No | Target language code for translation (e.g., "fr", "de", "es"). If not provided or "en", PDF is generated in English |

**Supported Languages:**

| Code | Language |
|------|----------|
| `en` | English (default) |
| `fr` | French |
| `de` | German |
| `es` | Spanish |
| `it` | Italian |

*Note: Additional languages may be configured via Translation Settings. Check `/api/translation/languages` for available languages.*

**Example Request Body:**
```json
{
  "documentType": "FULL_ITINERARY",
  "dataId": "encoded_itinerary_xyz",
  "templateId": "encoded_template_abc"
}
```

**Example Request Body with Translation:**
```json
{
  "documentType": "FULL_ITINERARY",
  "dataId": "encoded_itinerary_xyz",
  "language": "fr"
}
```

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

**Note:** For successful PDF generation, the response is a binary PDF file with appropriate headers, not the standard JSON wrapper.

---

## Endpoints

### 1. Generate PDF
**POST** `/api/pdf/generate`

Generates a PDF document using the specified document type, data source, and optionally a specific template. Returns the PDF file as a binary download.

**Permission Required:** `PERM_GENERATE_PDF`

**Request Body:** `GeneratePdfRequestDTO`

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/pdf/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "FULL_ITINERARY",
    "dataId": "encoded_itinerary_xyz"
  }'
```

**Success Response (200):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="<generated_filename>.pdf"`
- Body: Binary PDF content

**Error Response (400 - Validation Error):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Document type is required",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Invalid Template):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid template ID",
  "errorCode": "INVALID_TEMPLATE_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Data Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Document Type Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Unknown document type: INVALID_TYPE",
  "errorCode": "UNKNOWN_DOCUMENT_TYPE",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (500 - Generation Failed):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to generate PDF",
  "errorCode": "PDF_GENERATION_FAILED",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 2. Preview PDF (HTML)
**POST** `/api/pdf/preview`

Generates a preview of the PDF by returning the rendered HTML content. Useful for template development and debugging.

**Permission Required:** `PERM_GENERATE_PDF`

**Request Body:** `GeneratePdfRequestDTO`

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/pdf/preview \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "FULL_ITINERARY",
    "dataId": "encoded_itinerary_xyz",
    "templateId": "encoded_template_abc"
  }'
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Preview generated successfully",
  "data": {
    "html": "<!DOCTYPE html><html>...(rendered HTML content)...</html>",
    "documentType": "FULL_ITINERARY",
    "templateId": "encoded_template_abc",
    "templateName": "Default Itinerary Template"
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Invalid Template):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid template ID",
  "errorCode": "INVALID_TEMPLATE_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Data Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (500 - Preview Failed):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to generate preview",
  "errorCode": "PREVIEW_GENERATION_FAILED",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 3. Generate Itinerary PDF (Convenience)
**GET** `/api/pdf/itinerary/{itineraryId}`

Convenience endpoint for generating a Full Itinerary PDF directly by itinerary ID. Returns the PDF file as a binary download. Supports optional translation to other languages.

**Permission Required:** `PERM_GENERATE_PDF`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `itineraryId` | String | Yes | Obfuscated itinerary ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `templateId` | String | No | Specific template ID to use. If not provided, uses the default template |
| `language` | String | No | Target language code (e.g., "fr", "de", "es"). If not provided or "en", PDF is in English |

**Example Request:**
```bash
# Using default template (English)
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz" \
  -H "Authorization: Bearer <token>" \
  --output itinerary.pdf

# Using specific template
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz?templateId=encoded_template_abc" \
  -H "Authorization: Bearer <token>" \
  --output itinerary.pdf

# Generate French translation
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz?language=fr" \
  -H "Authorization: Bearer <token>" \
  --output itinerary_fr.pdf

# German translation with specific template
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz?templateId=encoded_template_abc&language=de" \
  -H "Authorization: Bearer <token>" \
  --output itinerary_de.pdf
```

**Success Response (200):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="<itinerary_name>.pdf"`
- Body: Binary PDF content

**Error Response (400 - Invalid ID):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid itinerary ID",
  "errorCode": "INVALID_ITINERARY_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (500 - Generation Failed):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to generate itinerary PDF",
  "errorCode": "PDF_GENERATION_FAILED",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 4. Preview Itinerary PDF (Convenience)
**GET** `/api/pdf/itinerary/{itineraryId}/preview`

Convenience endpoint for previewing a Full Itinerary PDF as rendered HTML. Useful for template development.

**Permission Required:** `PERM_GENERATE_PDF`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `itineraryId` | String | Yes | Obfuscated itinerary ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `templateId` | String | No | Specific template ID to use. If not provided, uses the default template |

**Example Request:**
```bash
# Using default template
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz/preview" \
  -H "Authorization: Bearer <token>"

# Using specific template
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz/preview?templateId=encoded_template_abc" \
  -H "Authorization: Bearer <token>"
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Preview generated successfully",
  "data": {
    "html": "<!DOCTYPE html><html>...(rendered HTML content)...</html>",
    "documentType": "FULL_ITINERARY",
    "templateId": "encoded_template_abc",
    "templateName": "Default Itinerary Template"
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Invalid ID):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid itinerary ID",
  "errorCode": "INVALID_ITINERARY_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

## Error Codes

| Error Code | Status | Description |
|-----------|--------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields) |
| `INVALID_ITINERARY_ID` | 400 | The provided itinerary ID could not be decoded |
| `INVALID_TEMPLATE_ID` | 400 | The provided template ID could not be decoded |
| `ITINERARY_NOT_FOUND` | 404 | Itinerary with the specified ID not found |
| `TEMPLATE_NOT_FOUND` | 404 | Template with the specified ID not found |
| `UNKNOWN_DOCUMENT_TYPE` | 404 | The specified document type does not exist |
| `DOCUMENT_TYPE_DISABLED` | 400 | The document type is disabled |
| `TEMPLATE_DISABLED` | 400 | The specified template is disabled |
| `NO_DEFAULT_TEMPLATE` | 404 | No default template found for the document type |
| `PDF_GENERATION_FAILED` | 500 | PDF generation failed due to a server error |
| `PREVIEW_GENERATION_FAILED` | 500 | Preview generation failed due to a server error |
| `TEMPLATE_FILE_NOT_FOUND` | 500 | Template HTML file could not be found |
| `TEMPLATE_RENDER_FAILED` | 500 | Thymeleaf template rendering failed |

---

## Supported Document Types

| Document Type | Description | Data Source |
|---------------|-------------|-------------|
| `FULL_ITINERARY` | Complete safari itinerary with days, parks, activities, accommodations, and passengers | FullItineraryDTO |

*More document types may be added in the future (e.g., SAFARI_QUOTE, BOOKING_CONFIRMATION, INVOICE)*

---

## Example Workflows

### Workflow 1: Generate and Download Itinerary PDF

```bash
# Step 1: Generate PDF using the convenience endpoint
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz" \
  -H "Authorization: Bearer <token>" \
  --output "safari_itinerary.pdf"
```

### Workflow 2: Preview Before Generating

```bash
# Step 1: Preview the HTML output
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz/preview" \
  -H "Authorization: Bearer <token>"

# Step 2: If satisfied, generate the PDF
curl -X GET "http://localhost:8080/api/pdf/itinerary/encoded_itinerary_xyz" \
  -H "Authorization: Bearer <token>" \
  --output "safari_itinerary.pdf"
```

### Workflow 3: Use a Specific Template

```bash
# Step 1: List available templates (via PDF Template API)
curl -X GET "http://localhost:8080/api/pdf-templates?documentId=encoded_doc_id" \
  -H "Authorization: Bearer <token>"

# Step 2: Generate PDF with specific template
curl -X POST http://localhost:8080/api/pdf/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "FULL_ITINERARY",
    "dataId": "encoded_itinerary_xyz",
    "templateId": "encoded_template_abc"
  }' \
  --output "safari_itinerary_custom.pdf"
```

### Workflow 4: Programmatic PDF Generation

```javascript
// JavaScript/Fetch example
const response = await fetch('/api/pdf/generate', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    documentType: 'FULL_ITINERARY',
    dataId: 'encoded_itinerary_xyz'
  })
});

if (response.ok) {
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'itinerary.pdf';
  a.click();
}
```

---

## Important Notes

1. **Template Selection**: If `templateId` is not provided, the system uses the default template for the document type. Ensure at least one template is marked as default.

2. **PDF Response Headers**: Successful PDF generation returns:
   - `Content-Type: application/pdf`
   - `Content-Disposition: attachment; filename="<filename>.pdf"`

3. **Preview vs Generate**: Use preview endpoints during template development to see rendered HTML. This is faster and allows inspection of the output before final PDF generation.

4. **Document Type Status**: If a document type is disabled, PDF generation will fail. Check the document type status via the PDF Document API.

5. **Template Status**: Disabled templates cannot be used for PDF generation.

6. **File Naming**: The generated PDF filename is based on the data source (e.g., itinerary name) unless a custom `fileName` is provided in the request.

7. **Authentication**: All endpoints require authentication and the `PERM_GENERATE_PDF` permission.

---

## Permissions Reference

| Permission | Description |
|------------|-------------|
| `PERM_GENERATE_PDF` | Required for all PDF generation and preview endpoints |

---

## Translation Feature

The PDF Generation API supports multi-language PDF generation using LibreTranslate integration.

### How It Works

1. When a `language` parameter is provided (and not "en"), the system:
   - Renders the PDF template with data in English
   - Sends the HTML content to LibreTranslate for translation
   - Generates the PDF from the translated HTML

2. **Fallback Behavior**: If translation fails (service unavailable, unsupported language, etc.), the PDF is generated in English without error.

3. **Caching**: Translations are cached to improve performance for repeated content.

### Prerequisites

- LibreTranslate must be running (Docker: `sudo docker-compose up -d`)
- Translation must be enabled in settings (`libretranslate.enabled = true`)
- The target language must be in the supported languages list

### Check Available Languages

```bash
curl -X GET "http://localhost:8080/api/translation/languages" \
  -H "Authorization: Bearer <token>"
```

### Check Translation Service Health

```bash
curl -X GET "http://localhost:8080/api/translation/health" \
  -H "Authorization: Bearer <token>"
```

### Translation Settings

Configure translation via the Translation Settings API:

| Setting | Description |
|---------|-------------|
| `libretranslate.enabled` | Enable/disable translation |
| `libretranslate.base.url` | LibreTranslate server URL |
| `supported.languages` | Comma-separated list of supported language codes |

See `/api/translation-settings` for full configuration options.

---

## Related APIs

| API | Description |
|-----|-------------|
| `/api/pdf-documents` | Manage PDF document types and view variable schemas |
| `/api/pdf-templates` | Manage PDF templates (create, update, delete) |
| `/api/itineraries/{id}/full` | Retrieve full itinerary data (used as PDF data source) |
| `/api/translation/languages` | Get available translation languages |
| `/api/translation/health` | Check translation service health |
| `/api/translation-settings` | Manage translation configuration |

---

## Version
API Version: 1.1

Last Updated: 2026-01-22
