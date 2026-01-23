# Translation API Documentation

Complete API reference for the Translation module, including settings management, service health, cache operations, and translation testing.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Base URLs](#2-base-urls)
3. [Authentication & Permissions](#3-authentication--permissions)
4. [Translation Settings API](#4-translation-settings-api)
   - [Get All Settings](#41-get-all-settings)
   - [Update Setting](#42-update-setting)
   - [Reset Connection Settings](#43-reset-connection-settings)
   - [Reset Language Settings](#44-reset-language-settings)
   - [Reset Caching Settings](#45-reset-caching-settings)
   - [Reset Limits Settings](#46-reset-limits-settings)
   - [Reset All Settings](#47-reset-all-settings)
5. [Translation Service API](#5-translation-service-api)
   - [Get Available Languages](#51-get-available-languages)
   - [Check Service Health](#52-check-service-health)
   - [Get Cache Statistics](#53-get-cache-statistics)
   - [Clear Cache](#54-clear-cache)
   - [Test Translation](#55-test-translation)
   - [Detect Language](#56-detect-language)
6. [Data Models](#6-data-models)
7. [Setting Keys Reference](#7-setting-keys-reference)
8. [Error Handling](#8-error-handling)
9. [Usage Examples](#9-usage-examples)

---

## 1. Overview

The Translation module provides two controllers:

| Controller | Base Path | Purpose |
|------------|-----------|---------|
| `TranslationSettingController` | `/api/translation-settings` | Manage translation configuration settings |
| `TranslationController` | `/api/translation` | Translation service operations, health checks, cache management |

---

## 2. Base URLs

```
Settings API:  /api/translation-settings
Service API:   /api/translation
```

---

## 3. Authentication & Permissions

All endpoints require JWT authentication. Required permissions:

| Endpoint | Permission Required |
|----------|---------------------|
| `GET /api/translation-settings` | `PERM_READ_TRANSLATION_SETTING` |
| `PUT /api/translation-settings/{id}` | `PERM_UPDATE_TRANSLATION_SETTING` |
| `POST /api/translation-settings/reset/*` | `PERM_UPDATE_TRANSLATION_SETTING` |
| `GET /api/translation/languages` | `PERM_GENERATE_PDF` |
| `GET /api/translation/health` | `PERM_TEST_TRANSLATION_SERVICE` |
| `GET /api/translation/cache/stats` | `PERM_READ_TRANSLATION_CACHE_STATS` |
| `DELETE /api/translation/cache` | `PERM_CLEAR_TRANSLATION_CACHE` |
| `POST /api/translation/test` | `PERM_TEST_TRANSLATION_SERVICE` |
| `POST /api/translation/detect` | `PERM_TEST_TRANSLATION_SERVICE` |

---

## 4. Translation Settings API

### 4.1 Get All Settings

Retrieve all translation configuration settings.

**Endpoint:** `GET /api/translation-settings`

**Permission:** `PERM_READ_TRANSLATION_SETTING`

**Request:**
```http
GET /api/translation-settings HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation settings retrieved successfully",
  "data": [
    {
      "id": "abc123",
      "displayName": "LibreTranslate Enabled",
      "settingKey": "libretranslate.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable LibreTranslate integration",
      "active": true,
      "isSystemDefault": true,
      "category": "CONNECTION",
      "requiresRestart": false,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "id": "def456",
      "displayName": "LibreTranslate Base URL",
      "settingKey": "libretranslate.base.url",
      "settingValue": "http://localhost:5000",
      "dataType": "STRING",
      "description": "Base URL of the LibreTranslate service",
      "active": true,
      "isSystemDefault": true,
      "category": "CONNECTION",
      "requiresRestart": false,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

### 4.2 Update Setting

Update a specific translation setting by ID.

**Endpoint:** `PUT /api/translation-settings/{id}`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Obfuscated setting ID |

**Request Body:**
```json
{
  "settingValue": "true",
  "active": true
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | No | New value for the setting |
| `active` | Boolean | No | Whether the setting is active (default: true) |

**Request:**
```http
PUT /api/translation-settings/abc123 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "settingValue": "false"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation setting updated successfully",
  "data": {
    "id": "abc123",
    "displayName": "LibreTranslate Enabled",
    "settingKey": "libretranslate.enabled",
    "settingValue": "false",
    "dataType": "BOOLEAN",
    "description": "Enable or disable LibreTranslate integration",
    "active": true,
    "isSystemDefault": true,
    "category": "CONNECTION",
    "requiresRestart": false,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-16T14:22:00"
  }
}
```

---

### 4.3 Reset Connection Settings

Reset LibreTranslate connection settings to their default values.

**Endpoint:** `POST /api/translation-settings/reset/connection`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Affected Settings:**
- `libretranslate.enabled`
- `libretranslate.base.url`
- `libretranslate.timeout.seconds`
- `libretranslate.api.key`

**Request:**
```http
POST /api/translation-settings/reset/connection HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Connection settings reset to defaults",
  "data": {
    "resetCount": 4,
    "category": "CONNECTION"
  }
}
```

---

### 4.4 Reset Language Settings

Reset language settings to their default values.

**Endpoint:** `POST /api/translation-settings/reset/languages`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Affected Settings:**
- `default.source.language`
- `default.target.language`
- `supported.languages`

**Request:**
```http
POST /api/translation-settings/reset/languages HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Language settings reset to defaults",
  "data": {
    "resetCount": 3,
    "category": "LANGUAGES"
  }
}
```

---

### 4.5 Reset Caching Settings

Reset translation caching settings to their default values.

**Endpoint:** `POST /api/translation-settings/reset/caching`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Affected Settings:**
- `cache.enabled`
- `cache.ttl.hours`

**Request:**
```http
POST /api/translation-settings/reset/caching HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Caching settings reset to defaults",
  "data": {
    "resetCount": 2,
    "category": "CACHING"
  }
}
```

---

### 4.6 Reset Limits Settings

Reset translation limits settings to their default values.

**Endpoint:** `POST /api/translation-settings/reset/limits`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Affected Settings:**
- `max.characters`
- `chunk.size`

**Request:**
```http
POST /api/translation-settings/reset/limits HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Limits settings reset to defaults",
  "data": {
    "resetCount": 2,
    "category": "LIMITS"
  }
}
```

---

### 4.7 Reset All Settings

Reset ALL translation settings to their default values.

**Endpoint:** `POST /api/translation-settings/reset/all`

**Permission:** `PERM_UPDATE_TRANSLATION_SETTING`

**Request:**
```http
POST /api/translation-settings/reset/all HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "All translation settings reset to defaults",
  "data": {
    "resetCount": 11,
    "categories": ["CONNECTION", "LANGUAGES", "CACHING", "LIMITS"]
  }
}
```

---

## 5. Translation Service API

### 5.1 Get Available Languages

Get list of available translation languages from LibreTranslate.

**Endpoint:** `GET /api/translation/languages`

**Permission:** `PERM_GENERATE_PDF`

**Request:**
```http
GET /api/translation/languages HTTP/1.1
Authorization: Bearer <token>
```

**Response (LibreTranslate Available):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Available languages retrieved",
  "data": {
    "languages": [
      {"code": "en", "name": "English", "targets": ["de", "es", "fr", "it"]},
      {"code": "fr", "name": "French", "targets": ["de", "en", "es", "it"]},
      {"code": "de", "name": "German", "targets": ["en", "es", "fr", "it"]},
      {"code": "es", "name": "Spanish", "targets": ["de", "en", "fr", "it"]},
      {"code": "it", "name": "Italian", "targets": ["de", "en", "es", "fr"]}
    ],
    "supportedLanguages": ["en", "fr", "de", "es", "it"],
    "defaultSourceLanguage": "en",
    "defaultTargetLanguage": "fr"
  }
}
```

**Response (LibreTranslate Unavailable):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Configured languages retrieved (LibreTranslate unavailable)",
  "data": {
    "languages": [],
    "supportedLanguages": ["en", "fr", "de", "es", "it"],
    "defaultSourceLanguage": "en",
    "defaultTargetLanguage": "fr",
    "warning": "LibreTranslate unavailable, showing configured languages only"
  }
}
```

---

### 5.2 Check Service Health

Check the health status of the LibreTranslate service.

**Endpoint:** `GET /api/translation/health`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Request:**
```http
GET /api/translation/health HTTP/1.1
Authorization: Bearer <token>
```

**Response (Healthy):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation service is healthy",
  "data": {
    "enabled": true,
    "available": true,
    "status": "HEALTHY",
    "message": "LibreTranslate is running and responding",
    "languageCount": 5,
    "languages": ["en", "fr", "de", "es", "it"],
    "baseUrl": "http://localhost:5000",
    "timeoutSeconds": 30,
    "cacheEnabled": true
  }
}
```

**Response (Disabled):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation service is disabled",
  "data": {
    "enabled": false,
    "status": "DISABLED",
    "message": "LibreTranslate is disabled in settings"
  }
}
```

**Response (Unhealthy):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation service is not available",
  "data": {
    "enabled": true,
    "available": false,
    "status": "UNHEALTHY",
    "message": "LibreTranslate is not responding",
    "baseUrl": "http://localhost:5000",
    "timeoutSeconds": 30,
    "cacheEnabled": true
  }
}
```

---

### 5.3 Get Cache Statistics

Get translation cache statistics and metrics.

**Endpoint:** `GET /api/translation/cache/stats`

**Permission:** `PERM_READ_TRANSLATION_CACHE_STATS`

**Request:**
```http
GET /api/translation/cache/stats HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Cache statistics retrieved",
  "data": {
    "validEntries": 150,
    "totalEntries": 180,
    "totalHits": 1250,
    "charactersSaved": 2500000,
    "cacheEnabled": true,
    "cacheTtlHours": 168,
    "estimatedHitRate": "87.41%"
  }
}
```

**Response (Empty Cache):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Cache statistics retrieved",
  "data": {
    "validEntries": 0,
    "totalEntries": 0,
    "totalHits": 0,
    "charactersSaved": 0,
    "cacheEnabled": true,
    "cacheTtlHours": 168,
    "estimatedHitRate": "N/A"
  }
}
```

---

### 5.4 Clear Cache

Clear translation cache entries.

**Endpoint:** `DELETE /api/translation/cache`

**Permission:** `PERM_CLEAR_TRANSLATION_CACHE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `sourceLanguage` | String | No | - | Source language to filter by |
| `targetLanguage` | String | No | - | Target language to filter by |
| `expiredOnly` | Boolean | No | `false` | Only clear expired entries |

**Clear All Entries:**
```http
DELETE /api/translation/cache HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation cache cleared",
  "data": {
    "action": "CLEARED_ALL",
    "message": "All cache entries cleared",
    "deletedEntries": 180
  }
}
```

**Clear by Language Pair:**
```http
DELETE /api/translation/cache?sourceLanguage=en&targetLanguage=fr HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation cache cleared",
  "data": {
    "action": "CLEARED_LANGUAGE_PAIR",
    "sourceLanguage": "en",
    "targetLanguage": "fr",
    "message": "Cache cleared for en -> fr translations",
    "deletedEntries": 45
  }
}
```

**Clear Expired Only:**
```http
DELETE /api/translation/cache?expiredOnly=true HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation cache cleared",
  "data": {
    "action": "CLEARED_EXPIRED",
    "message": "Expired cache entries cleared",
    "deletedEntries": 30
  }
}
```

---

### 5.5 Test Translation

Test the translation service with sample text.

**Endpoint:** `POST /api/translation/test`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `text` | String | No | `"Welcome to Tanzania Safari!"` | Text to translate |
| `sourceLanguage` | String | No | `"en"` | Source language code |
| `targetLanguage` | String | No | `"fr"` | Target language code |

**Request:**
```http
POST /api/translation/test?text=Hello%20World&sourceLanguage=en&targetLanguage=de HTTP/1.1
Authorization: Bearer <token>
```

**Response (Success):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation test successful",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "de",
    "success": true,
    "translatedText": "Hallo Welt",
    "durationMs": 125
  }
}
```

**Response (Service Disabled):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation test failed - service disabled",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "de",
    "success": false,
    "error": "LibreTranslate is disabled"
  }
}
```

**Response (Service Unavailable):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation test failed - service unavailable",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "de",
    "success": false,
    "error": "LibreTranslate service is not reachable"
  }
}
```

**Response (Unsupported Language):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Translation test failed - unsupported target language",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "zh",
    "success": false,
    "error": "Target language 'zh' is not supported"
  }
}
```

---

### 5.6 Detect Language

Detect the language of given text.

**Endpoint:** `POST /api/translation/detect`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `text` | String | Yes | Text to detect language for |

**Request:**
```http
POST /api/translation/detect?text=Bonjour%20le%20monde HTTP/1.1
Authorization: Bearer <token>
```

**Response (Success):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Language detected",
  "data": {
    "text": "Bonjour le monde",
    "detected": true,
    "language": "fr"
  }
}
```

**Response (Service Disabled):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Language detection failed - service disabled",
  "data": {
    "text": "Bonjour le monde",
    "detected": false,
    "error": "LibreTranslate is disabled"
  }
}
```

---

## 6. Data Models

### TranslationSettingDTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated setting ID |
| `displayName` | String | Human-readable setting name |
| `settingKey` | String | Unique key for the setting |
| `settingValue` | String | Current value (stored as string) |
| `dataType` | Enum | Data type: `STRING`, `INTEGER`, `BOOLEAN`, `LONG`, `DOUBLE` |
| `description` | String | Description of the setting |
| `active` | Boolean | Whether setting is active |
| `isSystemDefault` | Boolean | Whether this is a system default (cannot be deleted) |
| `category` | Enum | Category: `CONNECTION`, `LANGUAGES`, `CACHING`, `LIMITS` |
| `requiresRestart` | Boolean | Whether changing requires app restart |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |

### UpdateTranslationSettingDTO

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | No | New value for the setting |
| `active` | Boolean | No | Whether setting should be active |

### Setting Categories

| Category | Description |
|----------|-------------|
| `CONNECTION` | LibreTranslate connection settings (URL, timeout, API key) |
| `LANGUAGES` | Language configuration (source, target, supported) |
| `CACHING` | Translation caching settings (enabled, TTL) |
| `LIMITS` | Translation limits (max characters, chunk size) |

---

## 7. Setting Keys Reference

### CONNECTION Settings

| Setting Key | Data Type | Default | Description |
|-------------|-----------|---------|-------------|
| `libretranslate.enabled` | BOOLEAN | `false` | Enable/disable LibreTranslate integration |
| `libretranslate.base.url` | STRING | `http://localhost:5000` | LibreTranslate server URL |
| `libretranslate.timeout.seconds` | INTEGER | `30` | Request timeout in seconds |
| `libretranslate.api.key` | STRING | `` | API key (if required by server) |

### LANGUAGES Settings

| Setting Key | Data Type | Default | Description |
|-------------|-----------|---------|-------------|
| `default.source.language` | STRING | `en` | Default source language |
| `default.target.language` | STRING | `fr` | Default target language |
| `supported.languages` | STRING | `en,fr,de,es,it,pt,sw` | Comma-separated list of supported languages |

### CACHING Settings

| Setting Key | Data Type | Default | Description |
|-------------|-----------|---------|-------------|
| `cache.enabled` | BOOLEAN | `true` | Enable/disable translation caching |
| `cache.ttl.hours` | INTEGER | `168` | Cache time-to-live in hours (168 = 1 week) |

### LIMITS Settings

| Setting Key | Data Type | Default | Description |
|-------------|-----------|---------|-------------|
| `max.characters` | INTEGER | `10000` | Maximum characters per translation request |
| `chunk.size` | INTEGER | `5000` | Chunk size for splitting large texts |

---

## 8. Error Handling

### Standard Error Response

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "data": null
}
```

### Common Errors

| Status Code | Error | Description |
|-------------|-------|-------------|
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Setting not found |
| 500 | Internal Server Error | Server-side error |

### Translation-Specific Errors

| Error Type | Description |
|------------|-------------|
| `SERVICE_UNAVAILABLE` | LibreTranslate is not reachable |
| `SERVICE_DISABLED` | LibreTranslate is disabled in settings |
| `UNSUPPORTED_LANGUAGE` | Language not in supported list |
| `TEXT_TOO_LONG` | Text exceeds maximum character limit |

---

## 9. Usage Examples

### Enable LibreTranslate

```bash
# 1. Get all settings to find the ID
curl -X GET "http://localhost:8080/api/translation-settings" \
  -H "Authorization: Bearer <token>"

# 2. Update the libretranslate.enabled setting
curl -X PUT "http://localhost:8080/api/translation-settings/<id>" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "true"}'
```

### Check Service Status

```bash
# Check health
curl -X GET "http://localhost:8080/api/translation/health" \
  -H "Authorization: Bearer <token>"
```

### Test Translation

```bash
# Test with default text
curl -X POST "http://localhost:8080/api/translation/test" \
  -H "Authorization: Bearer <token>"

# Test with custom text
curl -X POST "http://localhost:8080/api/translation/test?text=Welcome%20to%20Safari&targetLanguage=de" \
  -H "Authorization: Bearer <token>"
```

### Monitor Cache

```bash
# Get cache stats
curl -X GET "http://localhost:8080/api/translation/cache/stats" \
  -H "Authorization: Bearer <token>"

# Clear expired entries
curl -X DELETE "http://localhost:8080/api/translation/cache?expiredOnly=true" \
  -H "Authorization: Bearer <token>"
```

### Generate Translated PDF

```bash
# Generate French itinerary PDF
curl -X GET "http://localhost:8080/api/pdf/itinerary/abc123?language=fr" \
  -H "Authorization: Bearer <token>" \
  --output itinerary_fr.pdf
```

---

## API Endpoints Summary

### Translation Settings (`/api/translation-settings`)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/` | `READ_TRANSLATION_SETTING` | Get all settings |
| `PUT` | `/{id}` | `UPDATE_TRANSLATION_SETTING` | Update a setting |
| `POST` | `/reset/connection` | `UPDATE_TRANSLATION_SETTING` | Reset connection settings |
| `POST` | `/reset/languages` | `UPDATE_TRANSLATION_SETTING` | Reset language settings |
| `POST` | `/reset/caching` | `UPDATE_TRANSLATION_SETTING` | Reset caching settings |
| `POST` | `/reset/limits` | `UPDATE_TRANSLATION_SETTING` | Reset limits settings |
| `POST` | `/reset/all` | `UPDATE_TRANSLATION_SETTING` | Reset all settings |

### Translation Service (`/api/translation`)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/languages` | `GENERATE_PDF` | Get available languages |
| `GET` | `/health` | `TEST_TRANSLATION_SERVICE` | Check service health |
| `GET` | `/cache/stats` | `READ_TRANSLATION_CACHE_STATS` | Get cache statistics |
| `DELETE` | `/cache` | `CLEAR_TRANSLATION_CACHE` | Clear cache entries |
| `POST` | `/test` | `TEST_TRANSLATION_SERVICE` | Test translation |
| `POST` | `/detect` | `TEST_TRANSLATION_SERVICE` | Detect language |
