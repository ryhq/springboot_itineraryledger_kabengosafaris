# PDF Document API Documentation

## Overview
The PDF Document API provides read-only access to built-in PDF document types and their schemas. PDF document types define the structure and variables available for PDF templates (e.g., "FULL_ITINERARY"). These document types are system-defined and cannot be created or deleted via API, but can be enabled/disabled.

---

## Base URL
```
/api/pdf-documents
```

---

## Data Transfer Objects (DTOs)

### PdfDocumentDTO (Response)
Returned in API responses. Contains all document type information.

```json
{
  "id": "string (obfuscated ID)",
  "name": "string (system name, e.g., 'FULL_ITINERARY')",
  "displayName": "string (human-readable name)",
  "description": "string (document type description)",
  "dataSourceClass": "string (fully qualified DTO class name)",
  "rootVariableName": "string (Thymeleaf root variable, e.g., 'itinerary')",
  "enabled": "boolean (whether document type is active)",
  "variablesJson": "string (JSON schema of available template variables)",
  "templateCount": "integer (number of templates using this document type)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

**Example Response:**
```json
{
  "id": "encoded_id_xyz",
  "name": "FULL_ITINERARY",
  "displayName": "Full Safari Itinerary",
  "description": "Complete itinerary document with all days, parks, activities, accommodations, and passenger configurations.",
  "dataSourceClass": "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO",
  "rootVariableName": "itinerary",
  "enabled": true,
  "variablesJson": "[{\"path\":\"id\",\"type\":\"String\",\"description\":\"Obfuscated itinerary ID\"}...]",
  "templateCount": 3,
  "createdAt": "2025-01-15T08:00:00",
  "updatedAt": "2025-01-18T14:30:00"
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

---

## Endpoints

### 1. Get All PDF Document Types
**GET** `/api/pdf-documents`

Retrieves all available PDF document types.

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Example Request:**
```
GET /api/pdf-documents
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "PDF documents retrieved successfully",
  "data": [
    {
      "id": "encoded_id_1",
      "name": "FULL_ITINERARY",
      "displayName": "Full Safari Itinerary",
      "description": "Complete itinerary document with all days, parks, activities, accommodations, and passenger configurations.",
      "dataSourceClass": "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO",
      "rootVariableName": "itinerary",
      "enabled": true,
      "variablesJson": "[...]",
      "templateCount": 3,
      "createdAt": "2025-01-15T08:00:00",
      "updatedAt": "2025-01-18T14:30:00"
    }
  ],
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (500):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to retrieve PDF documents",
  "errorCode": "PDF_DOCUMENTS_FETCH_FAILED",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 2. Get PDF Document Type by ID
**GET** `/api/pdf-documents/{id}`

Retrieves a single PDF document type by its obfuscated ID.

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated PDF document ID (string)
```

**Example Request:**
```
GET /api/pdf-documents/encoded_id_123
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "PDF document retrieved successfully",
  "data": {
    "id": "encoded_id_123",
    "name": "FULL_ITINERARY",
    "displayName": "Full Safari Itinerary",
    "description": "Complete itinerary document with all days, parks, activities, accommodations, and passenger configurations.",
    "dataSourceClass": "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO",
    "rootVariableName": "itinerary",
    "enabled": true,
    "variablesJson": "[...]",
    "templateCount": 3,
    "createdAt": "2025-01-15T08:00:00",
    "updatedAt": "2025-01-18T14:30:00"
  },
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Invalid ID):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid document ID",
  "errorCode": "INVALID_DOCUMENT_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "PDF document not found",
  "errorCode": "DOCUMENT_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 3. Get Document Variable Schema
**GET** `/api/pdf-documents/{id}/schema`

Retrieves the variable schema (JSON) for a PDF document type. This schema defines all available variables that can be used in Thymeleaf templates for this document type.

**Permission Required:** `PERM_READ_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated PDF document ID (string)
```

**Example Request:**
```
GET /api/pdf-documents/encoded_id_123/schema
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Schema retrieved successfully",
  "data": [
    {
      "path": "id",
      "type": "String",
      "description": "Obfuscated itinerary ID",
      "isRequired": true
    },
    {
      "path": "name",
      "type": "String",
      "description": "Itinerary name (e.g., '7-Day Serengeti Safari')",
      "isRequired": true
    },
    {
      "path": "days",
      "type": "List<DayDTO>",
      "description": "List of days. Access with: th:each='day : ${itinerary.days}'",
      "isRequired": false,
      "children": [
        {"path": "dayNumber", "type": "Integer", "description": "Sequential day number"},
        {"path": "title", "type": "String", "description": "Day title"},
        {"path": "description", "type": "String", "description": "Day description"}
      ]
    }
  ],
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (400 - Invalid ID):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid document ID",
  "errorCode": "INVALID_DOCUMENT_ID",
  "timestamp": "2025-01-19T10:30:45"
}
```

**Error Response (404 - Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "PDF document not found",
  "errorCode": "DOCUMENT_NOT_FOUND",
  "timestamp": "2025-01-19T10:30:45"
}
```

---

### 4. Toggle Enabled Status
**PATCH** `/api/pdf-documents/{id}/toggle-enabled`

Toggles the enabled status of a PDF document type. When disabled, PDF generation for this document type will be blocked.

**Permission Required:** `PERM_UPDATE_PDF_TEMPLATE`

**Path Parameters:**
```
id = obfuscated PDF document ID (string)
```

**Example Request:**
```
PATCH /api/pdf-documents/encoded_id_123/toggle-enabled
```

**Success Response (200 - Enabled):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "PDF document enabled successfully",
  "data": {
    "id": "encoded_id_123",
    "name": "FULL_ITINERARY",
    "displayName": "Full Safari Itinerary",
    "enabled": true,
    "templateCount": 3,
    "updatedAt": "2025-01-19T10:35:00"
  },
  "timestamp": "2025-01-19T10:35:00"
}
```

**Success Response (200 - Disabled):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "PDF document disabled successfully",
  "data": {
    "id": "encoded_id_123",
    "name": "FULL_ITINERARY",
    "displayName": "Full Safari Itinerary",
    "enabled": false,
    "templateCount": 3,
    "updatedAt": "2025-01-19T10:35:00"
  },
  "timestamp": "2025-01-19T10:35:00"
}
```

**Error Response (400 - Invalid ID):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid document ID",
  "errorCode": "INVALID_DOCUMENT_ID",
  "timestamp": "2025-01-19T10:35:00"
}
```

**Error Response (404 - Not Found):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "PDF document not found",
  "errorCode": "DOCUMENT_NOT_FOUND",
  "timestamp": "2025-01-19T10:35:00"
}
```

**Error Response (500):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to toggle PDF document status",
  "errorCode": "PDF_DOCUMENT_TOGGLE_FAILED",
  "timestamp": "2025-01-19T10:35:00"
}
```

---

## Error Codes

| Error Code | Status | Description |
|-----------|--------|-------------|
| `INVALID_DOCUMENT_ID` | 400 | The provided document ID could not be decoded |
| `DOCUMENT_NOT_FOUND` | 404 | PDF document type not found |
| `PDF_DOCUMENTS_FETCH_FAILED` | 500 | Failed to retrieve PDF documents |
| `PDF_DOCUMENT_FETCH_FAILED` | 500 | Failed to retrieve a single PDF document |
| `SCHEMA_FETCH_FAILED` | 500 | Failed to retrieve document schema |
| `PDF_DOCUMENT_TOGGLE_FAILED` | 500 | Failed to toggle document enabled status |

---

## Variable Schema Reference

The variable schema defines all available variables for Thymeleaf templates. Each variable includes:

| Field | Description |
|-------|-------------|
| `path` | The variable path in the DTO (e.g., "name", "days[].title") |
| `type` | The Java type (String, Integer, List, Boolean, etc.) |
| `description` | Human-readable description of the variable |
| `isRequired` | Whether the variable is always present |
| `children` | Nested fields for complex types (List, Object) |

### Example Thymeleaf Usage

```html
<!-- Access root variable -->
<h1 th:text="${itinerary.name}">Itinerary Name</h1>

<!-- Iterate over days -->
<div th:each="day : ${itinerary.days}">
    <h2 th:text="${day.title}">Day Title</h2>
    <p th:text="${day.description}">Day Description</p>
</div>

<!-- Conditional display -->
<span th:if="${itinerary.totalNights > 0}"
      th:text="${itinerary.totalNights} + ' nights'">
</span>
```

---

## Important Notes

1. **Built-in Document Types**: PDF document types are system-defined and cannot be created or deleted via API. Only the `enabled` status can be toggled.

2. **Enabled Status Impact**: When a document type is disabled (`enabled=false`), any attempt to generate a PDF using that document type will fail with error `DOCUMENT_TYPE_DISABLED`.

3. **Template Count**: The `templateCount` field shows how many PDF templates are associated with each document type.

4. **Root Variable Name**: The `rootVariableName` field (e.g., "itinerary") is the variable name used to access data in Thymeleaf templates.

5. **Data Source Class**: The `dataSourceClass` field documents the DTO class that provides data for this document type.

6. **Obfuscated IDs**: All document IDs are obfuscated for security. Use the returned `id` field for subsequent requests.

---

## Example Workflow

### 1. List All Document Types
```bash
curl -X GET http://localhost:8080/api/pdf-documents \
  -H "Authorization: Bearer <token>"
```

### 2. Get Document Schema for Template Development
```bash
curl -X GET http://localhost:8080/api/pdf-documents/encoded_id_123/schema \
  -H "Authorization: Bearer <token>"
```

### 3. Disable a Document Type
```bash
curl -X PATCH http://localhost:8080/api/pdf-documents/encoded_id_123/toggle-enabled \
  -H "Authorization: Bearer <token>"
```

### 4. Re-enable the Document Type
```bash
curl -X PATCH http://localhost:8080/api/pdf-documents/encoded_id_123/toggle-enabled \
  -H "Authorization: Bearer <token>"
```

---

## Permissions Reference

| Permission | Description |
|------------|-------------|
| `PERM_READ_PDF_TEMPLATE` | Required to view PDF document types and schemas |
| `PERM_UPDATE_PDF_TEMPLATE` | Required to toggle enabled status |

---

## Version
API Version: 1.0

Last Updated: 2025-01-19
