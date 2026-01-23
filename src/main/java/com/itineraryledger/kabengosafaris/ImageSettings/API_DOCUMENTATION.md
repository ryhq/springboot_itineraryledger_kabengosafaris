# Image Settings API Documentation

## Overview

The Image Settings API provides endpoints for managing runtime-configurable image upload settings. These settings control image upload behavior across the application without requiring a restart.

**Base URL:** `/api/image-settings`

---

## Authentication & Authorization

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET /api/image-settings | `PERM_READ_IMAGE_SETTING` |
| PUT /api/image-settings/{id} | `PERM_UPDATE_IMAGE_SETTING` |
| POST /api/image-settings/reset/upload | `PERM_UPDATE_IMAGE_SETTING` |
| POST /api/image-settings/reset/all | `PERM_UPDATE_IMAGE_SETTING` |

---

## Available Settings

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `image.upload.enabled` | BOOLEAN | `true` | Enable or disable image uploads globally |
| `image.upload.max.file.size` | LONG | `5242880` (5MB) | Maximum file size in bytes |
| `image.upload.allowed.formats` | STRING | `jpg,jpeg,png,webp,gif` | Comma-separated allowed extensions |

---

## Endpoints

### 1. Get All Image Settings

Retrieves all image configuration settings.

**Endpoint:** `GET /api/image-settings`

**Permission:** `PERM_READ_IMAGE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Image Settings retrieved successfully.",
  "data": [
    {
      "id": "obfuscated_id_1",
      "displayName": "Image Upload Settings",
      "settingKey": "image.upload.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable image uploads. When disabled, image upload requests will be rejected.",
      "active": true,
      "isSystemDefault": true,
      "category": "UPLOAD",
      "requiresRestart": false,
      "createdAt": "2025-01-23T10:30:00",
      "updatedAt": "2025-01-23T10:30:00"
    },
    {
      "id": "obfuscated_id_2",
      "displayName": "Image Upload Settings",
      "settingKey": "image.upload.max.file.size",
      "settingValue": "5242880",
      "dataType": "LONG",
      "description": "Maximum file size for image uploads in bytes. Default is 5MB (5242880 bytes).",
      "active": true,
      "isSystemDefault": true,
      "category": "UPLOAD",
      "requiresRestart": false,
      "createdAt": "2025-01-23T10:30:00",
      "updatedAt": "2025-01-23T10:30:00"
    },
    {
      "id": "obfuscated_id_3",
      "displayName": "Image Upload Settings",
      "settingKey": "image.upload.allowed.formats",
      "settingValue": "jpg,jpeg,png,webp,gif",
      "dataType": "STRING",
      "description": "Comma-separated list of allowed image formats (e.g., 'jpg,jpeg,png,webp,gif'). Only images with these extensions can be uploaded.",
      "active": true,
      "isSystemDefault": true,
      "category": "UPLOAD",
      "requiresRestart": false,
      "createdAt": "2025-01-23T10:30:00",
      "updatedAt": "2025-01-23T10:30:00"
    }
  ],
  "timestamp": "2025-01-23T10:35:00"
}
```

---

### 2. Update Image Setting

Updates a specific image setting by its obfuscated ID.

**Endpoint:** `PUT /api/image-settings/{id}`

**Permission:** `PERM_UPDATE_IMAGE_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated ID of the setting |

#### Request Body

```json
{
  "settingValue": "10485760",
  "active": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | Yes | New value for the setting |
| `active` | Boolean | Yes | Whether the setting is active |

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Image Setting updated successfully. Setting Value changed.",
  "data": {
    "id": "obfuscated_id_2",
    "displayName": "Image Upload Settings",
    "settingKey": "image.upload.max.file.size",
    "settingValue": "10485760",
    "dataType": "LONG",
    "description": "Maximum file size for image uploads in bytes. Default is 5MB (5242880 bytes).",
    "active": true,
    "isSystemDefault": true,
    "category": "UPLOAD",
    "requiresRestart": false,
    "createdAt": "2025-01-23T10:30:00",
    "updatedAt": "2025-01-23T10:40:00"
  },
  "timestamp": "2025-01-23T10:40:00"
}
```

#### Response - No Changes

```json
{
  "success": true,
  "status": 200,
  "message": "No changes detected. Image Setting remains unchanged.",
  "data": null,
  "timestamp": "2025-01-23T10:40:00"
}
```

#### Response - Invalid ID

```json
{
  "success": false,
  "status": 400,
  "message": "Invalid Image Setting ID provided.",
  "error": "VALIDATION_ERROR",
  "timestamp": "2025-01-23T10:40:00"
}
```

#### Response - Not Found

```json
{
  "success": false,
  "status": 404,
  "message": "Image Setting not found.",
  "error": "NOT_FOUND",
  "timestamp": "2025-01-23T10:40:00"
}
```

---

### 3. Reset Upload Settings

Resets all upload-related settings to their default values from `application.properties`.

**Endpoint:** `POST /api/image-settings/reset/upload`

**Permission:** `PERM_UPDATE_IMAGE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Image Upload Settings reset to default values successfully.",
  "data": null,
  "timestamp": "2025-01-23T10:45:00"
}
```

---

### 4. Reset All Settings

Resets all image settings to their default values from `application.properties`.

**Endpoint:** `POST /api/image-settings/reset/all`

**Permission:** `PERM_UPDATE_IMAGE_SETTING`

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "All Image Settings reset to default values successfully.",
  "data": null,
  "timestamp": "2025-01-23T10:45:00"
}
```

---

## Data Types

### ImageSettingDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated unique identifier |
| `displayName` | String | Human-readable category name |
| `settingKey` | String | Setting key (e.g., `image.upload.enabled`) |
| `settingValue` | String | Current value (stored as string) |
| `dataType` | Enum | Data type: `STRING`, `INTEGER`, `BOOLEAN`, `LONG`, `DOUBLE` |
| `description` | String | Description of the setting |
| `active` | Boolean | Whether the setting is active |
| `isSystemDefault` | Boolean | Whether this is a system default (cannot be deleted) |
| `category` | Enum | Category: `UPLOAD` |
| `requiresRestart` | Boolean | Whether changing requires application restart |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

### UpdateImageSettingDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | Yes | New value for the setting |
| `active` | Boolean | Yes | Whether the setting should be active |

---

## Usage Examples

### cURL Examples

#### Get All Settings
```bash
curl -X GET "http://localhost:4450/api/image-settings" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json"
```

#### Update Max File Size to 10MB
```bash
curl -X PUT "http://localhost:4450/api/image-settings/<obfuscated_id>" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "10485760",
    "active": true
  }'
```

#### Disable Image Uploads
```bash
curl -X PUT "http://localhost:4450/api/image-settings/<obfuscated_id>" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "false",
    "active": true
  }'
```

#### Add WebP Format Support
```bash
curl -X PUT "http://localhost:4450/api/image-settings/<obfuscated_id>" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "jpg,jpeg,png,webp,gif,bmp,tiff",
    "active": true
  }'
```

#### Reset All Settings to Defaults
```bash
curl -X POST "http://localhost:4450/api/image-settings/reset/all" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json"
```

---

## Using Settings in Code

The `ImageSettingGetterServices` class provides helper methods to read settings with automatic fallback to `application.properties` values:

```java
@Autowired
private ImageSettingGetterServices imageSettings;

// Check if uploads are enabled
if (!imageSettings.isImageUploadEnabled()) {
    throw new RuntimeException("Image uploads are disabled");
}

// Validate file size
long fileSize = uploadedFile.getSize();
if (!imageSettings.isFileSizeAllowed(fileSize)) {
    throw new RuntimeException("File exceeds maximum size of " +
        imageSettings.getMaxFileSizeInMB() + " MB");
}

// Validate file format
String filename = uploadedFile.getOriginalFilename();
if (!imageSettings.isFilenameAllowed(filename)) {
    throw new RuntimeException("File format not allowed. Allowed: " +
        imageSettings.getAllowedFormatsString());
}

// Or use the combined validation method
String validationError = imageSettings.validateImageUpload(filename, fileSize);
if (validationError != null) {
    throw new RuntimeException(validationError);
}
```

### Available Helper Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `isImageUploadEnabled()` | Boolean | Check if image uploads are enabled |
| `getMaxFileSize()` | Long | Get max file size in bytes |
| `getMaxFileSizeInMB()` | double | Get max file size in megabytes |
| `getAllowedFormatsString()` | String | Get allowed formats as comma-separated string |
| `getAllowedFormats()` | List<String> | Get allowed formats as list |
| `isFormatAllowed(format)` | boolean | Check if a format extension is allowed |
| `isFilenameAllowed(filename)` | boolean | Check if a filename has allowed extension |
| `isFileSizeAllowed(size)` | boolean | Check if file size is within limit |
| `validateImageUpload(filename, size)` | String | Full validation, returns error message or null |

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

All update and reset operations are automatically logged in the audit log with:
- User ID and username
- Action performed
- Old and new values
- Timestamp

Actions logged:
- `UPDATE_IMAGE_SETTING` - When a setting is updated
- `RESET_IMAGE_UPLOAD_SETTINGS` - When upload settings are reset
- `RESET_ALL_IMAGE_SETTINGS` - When all settings are reset
