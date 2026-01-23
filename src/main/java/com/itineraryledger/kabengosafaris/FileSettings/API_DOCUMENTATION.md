# File Settings API Documentation

## Overview

The File Settings API provides endpoints for managing runtime-configurable file upload settings. These settings control file upload behavior across the application without requiring a restart, including general file uploads, email signatures, email templates, and PDF templates.

**Base URL:** `/api/file-settings`

---

## Authentication & Authorization

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET /api/file-settings | `PERM_READ_FILE_SETTING` |
| GET /api/file-settings/category/{category} | `PERM_READ_FILE_SETTING` |
| PUT /api/file-settings/{id} | `PERM_UPDATE_FILE_SETTING` |
| POST /api/file-settings/reset/* | `PERM_UPDATE_FILE_SETTING` |

---

## Available Settings

### UPLOAD Category (General File Upload)

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `file.upload.enabled` | BOOLEAN | `true` | Enable or disable file uploads globally |
| `file.upload.max.file.size` | LONG | `10485760` (10MB) | Maximum file size in bytes |
| `file.upload.allowed.extensions` | STRING | `pdf,doc,docx,xls,xlsx,csv,txt,zip` | Allowed file extensions |
| `file.upload.blocked.extensions` | STRING | `exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js` | Blocked/dangerous extensions |
| `file.upload.sanitize.filename` | BOOLEAN | `true` | Sanitize filenames on upload |
| `file.upload.validate.content.type` | BOOLEAN | `true` | Validate MIME type matches extension |

### EMAIL_SIGNATURE Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `file.email.signature.max.file.size` | LONG | `1048576` (1MB) | Max size for email signatures |

### EMAIL_TEMPLATE Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `file.email.template.max.file.size` | LONG | `2097152` (2MB) | Max size for email templates |

### PDF_TEMPLATE Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `file.pdf.template.max.file.size` | LONG | `5242880` (5MB) | Max size for PDF templates |

---

## Endpoints

### 1. Get All File Settings

Retrieves all file configuration settings.

**Endpoint:** `GET /api/file-settings`

**Permission:** `PERM_READ_FILE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "File Settings retrieved successfully.",
  "data": [
    {
      "id": "obfuscated_id_1",
      "displayName": "General File Upload Settings",
      "settingKey": "file.upload.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable file uploads globally...",
      "active": true,
      "isSystemDefault": true,
      "category": "UPLOAD",
      "requiresRestart": false,
      "createdAt": "2025-01-23T10:30:00",
      "updatedAt": "2025-01-23T10:30:00"
    },
    // ... more settings
  ],
  "timestamp": "2025-01-23T10:35:00"
}
```

---

### 2. Get Settings by Category

Retrieves file settings for a specific category.

**Endpoint:** `GET /api/file-settings/category/{category}`

**Permission:** `PERM_READ_FILE_SETTING`

#### Path Parameters

| Parameter | Type | Required | Valid Values |
|-----------|------|----------|--------------|
| `category` | Enum | Yes | `UPLOAD`, `EMAIL_SIGNATURE`, `EMAIL_TEMPLATE`, `PDF_TEMPLATE` |

#### Example

```
GET /api/file-settings/category/EMAIL_SIGNATURE
```

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Email Signature Upload Settings retrieved successfully.",
  "data": [
    {
      "id": "obfuscated_id",
      "displayName": "Email Signature Upload Settings",
      "settingKey": "file.email.signature.max.file.size",
      "settingValue": "1048576",
      "dataType": "LONG",
      "description": "Maximum file size for email signature uploads...",
      "active": true,
      "category": "EMAIL_SIGNATURE"
    }
  ],
  "timestamp": "2025-01-23T10:35:00"
}
```

---

### 3. Update File Setting

Updates a specific file setting by its obfuscated ID.

**Endpoint:** `PUT /api/file-settings/{id}`

**Permission:** `PERM_UPDATE_FILE_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated ID of the setting |

#### Request Body

```json
{
  "settingValue": "20971520",
  "active": true
}
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "File Setting updated successfully. Setting Value changed.",
  "data": {
    "id": "obfuscated_id",
    "settingKey": "file.upload.max.file.size",
    "settingValue": "20971520",
    "dataType": "LONG",
    "active": true
  },
  "timestamp": "2025-01-23T10:40:00"
}
```

---

### 4. Reset Upload Settings

Resets general file upload settings to defaults.

**Endpoint:** `POST /api/file-settings/reset/upload`

**Permission:** `PERM_UPDATE_FILE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "File Upload Settings reset to default values successfully.",
  "data": null,
  "timestamp": "2025-01-23T10:45:00"
}
```

---

### 5. Reset Email Signature Settings

Resets email signature upload settings to defaults.

**Endpoint:** `POST /api/file-settings/reset/email-signature`

**Permission:** `PERM_UPDATE_FILE_SETTING`

---

### 6. Reset Email Template Settings

Resets email template upload settings to defaults.

**Endpoint:** `POST /api/file-settings/reset/email-template`

**Permission:** `PERM_UPDATE_FILE_SETTING`

---

### 7. Reset PDF Template Settings

Resets PDF template upload settings to defaults.

**Endpoint:** `POST /api/file-settings/reset/pdf-template`

**Permission:** `PERM_UPDATE_FILE_SETTING`

---

### 8. Reset All Settings

Resets all file settings to their default values.

**Endpoint:** `POST /api/file-settings/reset/all`

**Permission:** `PERM_UPDATE_FILE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "All File Settings reset to default values successfully.",
  "data": null,
  "timestamp": "2025-01-23T10:45:00"
}
```

---

## Using Settings in Code

The `FileSettingGetterServices` class provides helper methods to read settings with automatic fallback to `application.properties` values:

```java
@Autowired
private FileSettingGetterServices fileSettings;

// ========================================
// General File Upload Validation
// ========================================

// Check if uploads are enabled
if (!fileSettings.isFileUploadEnabled()) {
    throw new RuntimeException("File uploads are disabled");
}

// Validate file size
if (!fileSettings.isFileSizeAllowed(fileSize)) {
    throw new RuntimeException("File exceeds max size of " +
        fileSettings.getMaxFileSizeInMB() + " MB");
}

// Check if extension is blocked (dangerous)
String ext = fileSettings.getExtension(filename);
if (fileSettings.isExtensionBlocked(ext)) {
    throw new RuntimeException("File type is blocked for security");
}

// Check if extension is allowed
if (!fileSettings.isExtensionAllowed(ext)) {
    throw new RuntimeException("File type not allowed");
}

// Sanitize filename if enabled
String safeFilename = fileSettings.isSanitizeFilenameEnabled()
    ? fileSettings.sanitizeFilename(filename)
    : filename;

// Combined validation (returns error message or null)
String error = fileSettings.validateFileUpload(filename, fileSize);
if (error != null) {
    throw new RuntimeException(error);
}

// ========================================
// Email Signature Validation
// ========================================

String error = fileSettings.validateEmailSignatureUpload(filename, fileSize);
if (error != null) {
    throw new RuntimeException(error);
}

// ========================================
// Email Template Validation
// ========================================

String error = fileSettings.validateEmailTemplateUpload(filename, fileSize);
if (error != null) {
    throw new RuntimeException(error);
}

// ========================================
// PDF Template Validation
// ========================================

String error = fileSettings.validatePdfTemplateUpload(filename, fileSize);
if (error != null) {
    throw new RuntimeException(error);
}
```

### Available Helper Methods

#### General Upload Methods
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isFileUploadEnabled()` | Boolean | Check if uploads enabled |
| `getMaxFileSize()` | Long | Get max size in bytes |
| `getMaxFileSizeInMB()` | double | Get max size in MB |
| `getAllowedExtensions()` | List<String> | Get allowed extensions |
| `getBlockedExtensions()` | List<String> | Get blocked extensions |
| `isSanitizeFilenameEnabled()` | Boolean | Check if sanitization enabled |
| `isValidateContentTypeEnabled()` | Boolean | Check if MIME validation enabled |

#### Validation Methods
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isExtensionAllowed(ext)` | boolean | Check if extension is allowed |
| `isExtensionBlocked(ext)` | boolean | Check if extension is blocked |
| `isFilenameAllowed(filename)` | boolean | Check if filename extension allowed |
| `isFileSizeAllowed(size)` | boolean | Check general size limit |
| `getExtension(filename)` | String | Extract extension from filename |
| `sanitizeFilename(filename)` | String | Remove unsafe characters |
| `validateFileUpload(name, size)` | String | Full validation (null=valid) |

#### Email Signature Methods
| Method | Return Type | Description |
|--------|-------------|-------------|
| `getEmailSignatureMaxFileSize()` | Long | Get max size in bytes |
| `getEmailSignatureMaxFileSizeInMB()` | double | Get max size in MB |
| `isEmailSignatureFileSizeAllowed(size)` | boolean | Check size limit |
| `validateEmailSignatureUpload(name, size)` | String | Full validation |

#### Email Template Methods
| Method | Return Type | Description |
|--------|-------------|-------------|
| `getEmailTemplateMaxFileSize()` | Long | Get max size in bytes |
| `getEmailTemplateMaxFileSizeInMB()` | double | Get max size in MB |
| `isEmailTemplateFileSizeAllowed(size)` | boolean | Check size limit |
| `validateEmailTemplateUpload(name, size)` | String | Full validation |

#### PDF Template Methods
| Method | Return Type | Description |
|--------|-------------|-------------|
| `getPdfTemplateMaxFileSize()` | Long | Get max size in bytes |
| `getPdfTemplateMaxFileSizeInMB()` | double | Get max size in MB |
| `isPdfTemplateFileSizeAllowed(size)` | boolean | Check size limit |
| `validatePdfTemplateUpload(name, size)` | String | Full validation |

---

## cURL Examples

### Get All Settings
```bash
curl -X GET "http://localhost:4450/api/file-settings" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Email Signature Settings Only
```bash
curl -X GET "http://localhost:4450/api/file-settings/category/EMAIL_SIGNATURE" \
  -H "Authorization: Bearer <jwt_token>"
```

### Update Max File Size to 20MB
```bash
curl -X PUT "http://localhost:4450/api/file-settings/<obfuscated_id>" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "20971520",
    "active": true
  }'
```

### Add HEIC to Blocked Extensions
```bash
curl -X PUT "http://localhost:4450/api/file-settings/<obfuscated_id>" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js,heic",
    "active": true
  }'
```

### Reset All Settings
```bash
curl -X POST "http://localhost:4450/api/file-settings/reset/all" \
  -H "Authorization: Bearer <jwt_token>"
```

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data or ID |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `NOT_FOUND` | Setting not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

## Audit Logging

All update and reset operations are automatically logged:

| Action | Description |
|--------|-------------|
| `UPDATE_FILE_SETTING` | Setting value changed |
| `RESET_FILE_UPLOAD_SETTINGS` | Upload settings reset |
| `RESET_EMAIL_SIGNATURE_FILE_SETTINGS` | Email signature settings reset |
| `RESET_EMAIL_TEMPLATE_FILE_SETTINGS` | Email template settings reset |
| `RESET_PDF_TEMPLATE_FILE_SETTINGS` | PDF template settings reset |
| `RESET_ALL_FILE_SETTINGS` | All settings reset |
