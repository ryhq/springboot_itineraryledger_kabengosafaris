# Translation API Documentation

Base URL: `/api/translation`

## Overview

The Translation API provides endpoints for managing the LibreTranslate integration, including:
- Service health monitoring
- Language discovery
- Translation testing
- Cache management and statistics

---

## Endpoints

### 1. Get Available Languages

Retrieves the list of available languages from LibreTranslate and configured supported languages.

**Endpoint:** `GET /api/translation/languages`

**Permission:** `PERM_GENERATE_PDF`

**Response:**
```json
{
  "status": 200,
  "message": "Available languages retrieved",
  "data": {
    "languages": [
      { "code": "en", "name": "English" },
      { "code": "fr", "name": "French" },
      { "code": "de", "name": "German" }
    ],
    "supportedLanguages": ["en", "fr", "de", "es", "it"],
    "defaultSourceLanguage": "en",
    "defaultTargetLanguage": "fr"
  }
}
```

**Fallback Response (LibreTranslate unavailable):**
```json
{
  "status": 200,
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

### 2. Check Service Health

Returns detailed health status of the LibreTranslate service.

**Endpoint:** `GET /api/translation/health`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Response (Healthy):**
```json
{
  "status": 200,
  "message": "Translation service is healthy",
  "data": {
    "enabled": true,
    "available": true,
    "status": "HEALTHY",
    "message": "LibreTranslate is running and responding",
    "languageCount": 32,
    "languages": ["en", "fr", "de", "es", "it", "..."],
    "baseUrl": "http://localhost:5000",
    "timeoutSeconds": 30,
    "cacheEnabled": true
  }
}
```

**Response (Disabled):**
```json
{
  "status": 200,
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
  "status": 200,
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

### 3. Get Cache Statistics

Returns metrics about translation cache usage and performance.

**Endpoint:** `GET /api/translation/cache/stats`

**Permission:** `PERM_READ_TRANSLATION_CACHE_STATS`

**Response:**
```json
{
  "status": 200,
  "message": "Cache statistics retrieved",
  "data": {
    "validEntries": 150,
    "totalHits": 1250,
    "charactersSaved": 2500000,
    "cacheEnabled": true,
    "cacheTtlHours": 168,
    "totalEntries": 175,
    "estimatedHitRate": "87.72%"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| validEntries | Integer | Number of non-expired cache entries |
| totalHits | Long | Total number of cache hits across all entries |
| charactersSaved | Long | Total characters saved by using cache (characters * hits) |
| cacheEnabled | Boolean | Whether caching is enabled |
| cacheTtlHours | Integer | Cache time-to-live in hours |
| totalEntries | Long | Total cache entries (including expired) |
| estimatedHitRate | String | Estimated cache hit rate percentage |

---

### 4. Clear Translation Cache

Clears translation cache entries based on criteria.

**Endpoint:** `DELETE /api/translation/cache`

**Permission:** `PERM_CLEAR_TRANSLATION_CACHE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| sourceLanguage | String | No | - | Filter by source language code |
| targetLanguage | String | No | - | Filter by target language code |
| expiredOnly | Boolean | No | false | Only clear expired entries |

**Examples:**

1. Clear all cache:
   ```
   DELETE /api/translation/cache
   ```

2. Clear expired entries only:
   ```
   DELETE /api/translation/cache?expiredOnly=true
   ```

3. Clear specific language pair:
   ```
   DELETE /api/translation/cache?sourceLanguage=en&targetLanguage=fr
   ```

**Response:**
```json
{
  "status": 200,
  "message": "Translation cache cleared",
  "data": {
    "action": "CLEARED_ALL",
    "message": "All cache entries cleared",
    "deletedEntries": 175
  }
}
```

**Response (Language Pair):**
```json
{
  "status": 200,
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

---

### 5. Test Translation

Tests the translation service with sample text.

**Endpoint:** `POST /api/translation/test`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| text | String | No | "Welcome to Tanzania Safari!" | Text to translate |
| sourceLanguage | String | No | "en" | Source language code |
| targetLanguage | String | No | "fr" | Target language code |

**Example:**
```
POST /api/translation/test?text=Hello%20World&sourceLanguage=en&targetLanguage=de
```

**Response (Success):**
```json
{
  "status": 200,
  "message": "Translation test successful",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "de",
    "success": true,
    "translatedText": "Hallo Welt",
    "durationMs": 245
  }
}
```

**Response (Failure):**
```json
{
  "status": 200,
  "message": "Translation test failed",
  "data": {
    "originalText": "Hello World",
    "sourceLanguage": "en",
    "targetLanguage": "xx",
    "success": false,
    "error": "Target language 'xx' is not supported"
  }
}
```

---

### 6. Detect Language

Detects the language of the provided text.

**Endpoint:** `POST /api/translation/detect`

**Permission:** `PERM_TEST_TRANSLATION_SERVICE`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| text | String | Yes | Text to analyze (max 500 chars used for detection) |

**Example:**
```
POST /api/translation/detect?text=Bonjour%20le%20monde
```

**Response (Success):**
```json
{
  "status": 200,
  "message": "Language detected",
  "data": {
    "text": "Bonjour le monde",
    "detected": true,
    "language": "fr"
  }
}
```

**Response (Failure):**
```json
{
  "status": 200,
  "message": "Language detection failed - service disabled",
  "data": {
    "text": "Bonjour le monde",
    "detected": false,
    "error": "LibreTranslate is disabled"
  }
}
```

---

### 7. Get Cache Entries (Paginated)

Retrieves translation cache entries with filtering, pagination, and sorting.

**Endpoint:** `GET /api/translation/cache/entries`

**Permission:** `PERM_READ_TRANSLATION_CACHE`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | Integer | No | 0 | Page number (0-based) |
| size | Integer | No | 10 | Page size |
| name | String | No | - | Filter by name (partial match, case-insensitive) |
| sourceLanguage | String | No | - | Filter by source language (exact match) |
| targetLanguage | String | No | - | Filter by target language (exact match) |
| contentHash | String | No | - | Filter by content hash (exact match) |
| originalContent | String | No | - | Filter by original content (partial match, case-insensitive) |
| translatedContent | String | No | - | Filter by translated content (partial match, case-insensitive) |
| minHitCount | Long | No | - | Filter by minimum hit count |
| maxHitCount | Long | No | - | Filter by maximum hit count |
| minCharCount | Integer | No | - | Filter by minimum character count |
| maxCharCount | Integer | No | - | Filter by maximum character count |
| createdAfter | DateTime | No | - | Filter by created after date (ISO format: yyyy-MM-ddTHH:mm:ss) |
| createdBefore | DateTime | No | - | Filter by created before date (ISO format: yyyy-MM-ddTHH:mm:ss) |
| expired | Boolean | No | - | Filter by expired status (true = expired, false = valid) |
| accessed | Boolean | No | - | Filter by accessed status (true = has been accessed, false = never accessed) |
| sortDirection | String | No | "desc" | Sort direction: "asc" or "desc" (always sorted by createdAt) |

**Examples:**

1. Get all cache entries (first page):
   ```
   GET /api/translation/cache/entries
   ```

2. Get English to French translations:
   ```
   GET /api/translation/cache/entries?sourceLanguage=en&targetLanguage=fr
   ```

3. Get most accessed entries:
   ```
   GET /api/translation/cache/entries?minHitCount=10&sortDirection=desc
   ```

4. Get expired entries:
   ```
   GET /api/translation/cache/entries?expired=true
   ```

5. Get entries created in a date range:
   ```
   GET /api/translation/cache/entries?createdAfter=2024-01-01T00:00:00&createdBefore=2024-12-31T23:59:59
   ```

6. Search by name:
   ```
   GET /api/translation/cache/entries?name=TRN_CACHE_0001
   ```

**Response:**
```json
{
  "status": 200,
  "message": "Successfully retrieved translation cache entries.",
  "data": {
    "cacheEntries": [
      {
        "id": "abc123def456",
        "name": "TRN_CACHE_00011224",
        "contentHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "sourceLanguage": "en",
        "targetLanguage": "fr",
        "originalContent": "Welcome to Tanzania Safari! Explore the...",
        "translatedContent": "Bienvenue au Safari en Tanzanie! Explorez...",
        "originalContentTruncated": true,
        "translatedContentTruncated": true,
        "characterCount": 15000,
        "hitCount": 25,
        "createdAt": "2024-01-15T10:30:00",
        "expiresAt": "2024-01-22T10:30:00",
        "lastAccessedAt": "2024-01-20T15:45:00",
        "isExpired": false
      }
    ],
    "currentPage": 0,
    "totalItems": 150,
    "totalPages": 15
  }
}
```

| Response Field | Type | Description |
|----------------|------|-------------|
| id | String | Obfuscated cache entry ID |
| name | String | Auto-generated unique name (format: TRN_CACHE_{####}{MM}{YY}) |
| contentHash | String | SHA-256 hash of original content + languages |
| sourceLanguage | String | Source language code |
| targetLanguage | String | Target language code |
| originalContent | String | Original content (truncated to 500 chars in list view) |
| translatedContent | String | Translated content (truncated to 500 chars in list view) |
| originalContentTruncated | Boolean | Whether original content was truncated |
| translatedContentTruncated | Boolean | Whether translated content was truncated |
| characterCount | Integer | Character count of original content |
| hitCount | Long | Number of times this cache entry was used |
| createdAt | DateTime | When the cache entry was created |
| expiresAt | DateTime | When the cache entry expires |
| lastAccessedAt | DateTime | When the cache was last accessed (null if never) |
| isExpired | Boolean | Whether the cache entry is expired |

---

### 8. Get Single Cache Entry

Retrieves a single translation cache entry by ID with full content (not truncated).

**Endpoint:** `GET /api/translation/cache/entries/{id}`

**Permission:** `PERM_READ_TRANSLATION_CACHE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | String | Obfuscated cache entry ID |

**Example:**
```
GET /api/translation/cache/entries/abc123def456
```

**Response:**
```json
{
  "status": 200,
  "message": "Successfully retrieved translation cache entry.",
  "data": {
    "id": "abc123def456",
    "name": "TRN_CACHE_00011224",
    "contentHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "sourceLanguage": "en",
    "targetLanguage": "fr",
    "originalContent": "Full original content here without truncation...",
    "translatedContent": "Full translated content here without truncation...",
    "originalContentTruncated": false,
    "translatedContentTruncated": false,
    "characterCount": 15000,
    "hitCount": 25,
    "createdAt": "2024-01-15T10:30:00",
    "expiresAt": "2024-01-22T10:30:00",
    "lastAccessedAt": "2024-01-20T15:45:00",
    "isExpired": false
  }
}
```

**Error Response (Not Found):**
```json
{
  "status": 404,
  "message": "Translation cache entry not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

---

## Permission Summary

| Endpoint | Permission Required |
|----------|---------------------|
| GET /languages | PERM_GENERATE_PDF |
| GET /health | PERM_TEST_TRANSLATION_SERVICE |
| GET /cache/stats | PERM_READ_TRANSLATION_CACHE_STATS |
| DELETE /cache | PERM_CLEAR_TRANSLATION_CACHE |
| POST /test | PERM_TEST_TRANSLATION_SERVICE |
| POST /detect | PERM_TEST_TRANSLATION_SERVICE |
| GET /cache/entries | PERM_READ_TRANSLATION_CACHE |
| GET /cache/entries/{id} | PERM_READ_TRANSLATION_CACHE |

---

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_PAGE | Page number cannot be negative |
| INVALID_SIZE | Page size must be greater than 0 |
| RESOURCE_NOT_FOUND | Cache entry not found |
| GET_CACHE_ENTRY_FAILED | Failed to retrieve cache entry |

---

## Cache Entry Name Format

Each translation cache entry is assigned an auto-generated unique name following this format:

**Format:** `TRN_CACHE_{######}{MM}{YY}`

| Part | Description |
|------|-------------|
| TRN_CACHE_ | Fixed prefix |
| ###### | Unique identifier (6 digits, zero-padded) |
| MM | Month (2 digits, zero-padded) |
| YY | Last 2 digits of year |

**Examples:**
- `TRN_CACHE_1000010126` - Cache entry in January 2026
- `TRN_CACHE_1000420125` - Cache entry in January 2025
- `TRN_CACHE_1012340325` - Cache entry in March 2025
