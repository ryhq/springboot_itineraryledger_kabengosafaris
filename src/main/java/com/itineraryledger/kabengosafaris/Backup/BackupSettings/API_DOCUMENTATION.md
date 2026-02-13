# Backup Settings API Documentation

## Overview

The Backup Settings API provides endpoints for managing runtime-configurable backup settings. These settings control backup behavior across the application without requiring a restart, including database backups, file system backups, scheduling, compression, retention policies, and notifications.

**Base URL:** `/api/backup-settings`

---

## Authentication & Authorization

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET /api/backup-settings | `PERM_READ_BACKUP_SETTING` |
| GET /api/backup-settings/category/{category} | `PERM_READ_BACKUP_SETTING` |
| GET /api/backup-settings/active | `PERM_READ_BACKUP_SETTING` |
| GET /api/backup-settings/{settingKey} | `PERM_READ_BACKUP_SETTING` |
| PUT /api/backup-settings/{settingKey} | `PERM_UPDATE_BACKUP_SETTING` |
| POST /api/backup-settings/{settingKey}/reset | `PERM_UPDATE_BACKUP_SETTING` |

---

## Available Settings

### GENERAL Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.enabled` | BOOLEAN | `true` | Enable or disable the backup system globally |
| `backup.type` | STRING | `FULL` | Type of backup: FULL (complete backup), INCREMENTAL (only changes), DIFFERENTIAL (changes since last full) |

### SCHEDULE Category

**Note:** `backup.schedule.cron` is a static configuration in `application.properties` only (not dynamically configurable). The cron expression is read at application startup by the `@Scheduled` annotation and cannot be changed without restarting the application.

**To change the backup schedule:**
1. Edit `application.properties`: `backup.schedule.cron=0 0 3 * * ?`
2. Restart the application
3. Use `backup.schedule.enabled` setting to enable/disable scheduling without restart

**Common Cron Expressions:**
- `0 0 2 * * ?` - Daily at 2:00 AM (default)
- `0 0 */6 * * ?` - Every 6 hours
- `0 0 * * 0 ?` - Weekly on Sunday at midnight
- `0 0 1 1 * ?` - Monthly on 1st at 1:00 AM

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.schedule.enabled` | BOOLEAN | `true` | Enable or disable automatic scheduled backups |

### DATABASE Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.database.enabled` | BOOLEAN | `true` | Enable or disable database backups |
| `backup.database.host` | STRING | `localhost` | Database host address |
| `backup.database.port` | INTEGER | `3306` | Database port number |
| `backup.database.name` | STRING | `springboot_itineraryledger_kabengosafaris` | Database name to backup |
| `backup.database.username` | STRING | `root` | Database username for backup operations |
| `backup.database.include.routines` | BOOLEAN | `true` | Include stored procedures and functions in database backup |
| `backup.database.include.triggers` | BOOLEAN | `true` | Include triggers in database backup |
| `backup.database.include.events` | BOOLEAN | `true` | Include scheduled events in database backup |

### FILES Category

**Note:** `backup.files.base.path` is a static configuration in `application.properties` only (not dynamically configurable).

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.files.enabled` | BOOLEAN | `true` | Enable or disable file system backups |
| `backup.files.include.email.signatures` | BOOLEAN | `true` | Include email signatures in file backup |
| `backup.files.include.email.templates` | BOOLEAN | `true` | Include email templates in file backup |
| `backup.files.include.pdf.templates` | BOOLEAN | `true` | Include PDF templates in file backup |
| `backup.files.include.accommodation.images` | BOOLEAN | `true` | Include accommodation images in file backup |
| `backup.files.include.accommodation.documents` | BOOLEAN | `true` | Include accommodation documents in file backup |
| `backup.files.include.park.images` | BOOLEAN | `true` | Include park images in file backup |
| `backup.files.include.park.documents` | BOOLEAN | `true` | Include park documents in file backup |
| `backup.files.include.activity.images` | BOOLEAN | `true` | Include activity images in file backup |
| `backup.files.include.activity.documents` | BOOLEAN | `true` | Include activity documents in file backup |
| `backup.files.include.itinerary.documents` | BOOLEAN | `true` | Include itinerary documents in file backup |
| `backup.files.include.quote.documents` | BOOLEAN | `true` | Include quote documents in file backup |
| `backup.files.include.safari.documents` | BOOLEAN | `true` | Include safari documents in file backup |

### STORAGE Category

**Note:** `backup.storage.path` is a static configuration in `application.properties` only (not dynamically configurable).

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.storage.filename.prefix` | STRING | `kabengosafaris_backup` | Prefix for backup filenames |
| `backup.storage.filename.date.format` | STRING | `yyyyMMdd_HHmmss` | Date format for backup filenames (Java DateTimeFormatter pattern) |

**Example Filename:** `kabengosafaris_backup_20260209_143000.zip`

### RETENTION Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.retention.days` | INTEGER | `7` | Number of days to retain backups (older backups are deleted) |
| `backup.retention.max.count` | INTEGER | `30` | Maximum number of backups to retain (oldest are deleted when exceeded) |
| `backup.retention.auto.cleanup.enabled` | BOOLEAN | `true` | Enable automatic cleanup of old backups based on retention policy |

### COMPRESSION Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.compression.enabled` | BOOLEAN | `true` | Enable compression for backups to save storage space |
| `backup.compression.format` | STRING | `zip` | Compression format: zip, gzip, tar, tar.gz |
| `backup.compression.level` | INTEGER | `5` | Compression level (0-9): 0=no compression, 9=maximum compression |

### NOTIFICATION Category

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `backup.notification.enabled` | BOOLEAN | `false` | Enable email notifications for backup operations |
| `backup.notification.on.success` | BOOLEAN | `false` | Send notification when backup completes successfully |
| `backup.notification.on.failure` | BOOLEAN | `true` | Send notification when backup fails |
| `backup.notification.emails` | STRING | `admin@kabengosafaris.com` | Email addresses to send backup notifications (comma-separated: email1@example.com,email2@example.com) |

---

## Endpoints

### 1. Get All Backup Settings

Retrieves all backup configuration settings.

**Endpoint:** `GET /api/backup-settings`

**Permission:** `PERM_READ_BACKUP_SETTING`

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Backup settings retrieved successfully",
  "data": [
    {
      "id": 1,
      "settingKey": "backup.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable the backup system globally",
      "active": true,
      "isSystemDefault": true,
      "category": "GENERAL",
      "requiresRestart": false,
      "createdAt": "2026-02-09T10:00:00",
      "updatedAt": "2026-02-09T10:00:00"
    },
    {
      "id": 2,
      "settingKey": "backup.schedule.cron",
      "settingValue": "0 0 2 * * ?",
      "dataType": "STRING",
      "description": "Cron expression for backup schedule",
      "active": true,
      "isSystemDefault": true,
      "category": "SCHEDULE",
      "requiresRestart": false,
      "createdAt": "2026-02-09T10:00:00",
      "updatedAt": "2026-02-09T10:00:00"
    }
    // ... more settings
  ],
  "timestamp": "2026-02-09T10:35:00"
}
```

#### Response - Error

```json
{
  "success": false,
  "status": 500,
  "message": "Failed to retrieve backup settings: Database connection error",
  "timestamp": "2026-02-09T10:35:00"
}
```

---

### 2. Get Settings by Category

Retrieves backup settings for a specific category.

**Endpoint:** `GET /api/backup-settings/category/{category}`

**Permission:** `PERM_READ_BACKUP_SETTING`

#### Path Parameters

| Parameter | Type | Required | Valid Values |
|-----------|------|----------|--------------|
| `category` | Enum | Yes | `GENERAL`, `SCHEDULE`, `DATABASE`, `FILES`, `STORAGE`, `RETENTION`, `COMPRESSION`, `NOTIFICATION` |

#### Example

```
GET /api/backup-settings/category/DATABASE
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Database Backup Settings retrieved successfully",
  "data": [
    {
      "id": 5,
      "settingKey": "backup.database.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable database backups",
      "active": true,
      "isSystemDefault": true,
      "category": "DATABASE",
      "requiresRestart": false
    },
    {
      "id": 6,
      "settingKey": "backup.database.host",
      "settingValue": "localhost",
      "dataType": "STRING",
      "description": "Database host address",
      "active": true,
      "category": "DATABASE"
    }
    // ... more database settings
  ],
  "timestamp": "2026-02-09T10:35:00"
}
```

---

### 3. Get Active Settings Only

Retrieves only active backup settings (where active=true).

**Endpoint:** `GET /api/backup-settings/active`

**Permission:** `PERM_READ_BACKUP_SETTING`

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Active backup settings retrieved successfully",
  "data": [
    // Array of active settings only
  ],
  "timestamp": "2026-02-09T10:35:00"
}
```

---

### 4. Get Setting by Key

Retrieves a specific backup setting by its setting key.

**Endpoint:** `GET /api/backup-settings/{settingKey}`

**Permission:** `PERM_READ_BACKUP_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key (e.g., `backup.enabled`) |

#### Example

```
GET /api/backup-settings/backup.retention.days
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Backup setting retrieved successfully",
  "data": {
    "id": 15,
    "settingKey": "backup.retention.days",
    "settingValue": "7",
    "dataType": "INTEGER",
    "description": "Number of days to retain backups (older backups are deleted)",
    "active": true,
    "isSystemDefault": true,
    "category": "RETENTION",
    "requiresRestart": false,
    "createdAt": "2026-02-09T10:00:00",
    "updatedAt": "2026-02-09T10:00:00"
  },
  "timestamp": "2026-02-09T10:35:00"
}
```

#### Response - Not Found

```json
{
  "success": false,
  "status": 404,
  "message": "Backup setting not found with key: backup.invalid.key",
  "timestamp": "2026-02-09T10:35:00"
}
```

---

### 5. Update Backup Setting

Updates a specific backup setting value by its setting key.

**Endpoint:** `PUT /api/backup-settings/{settingKey}`

**Permission:** `PERM_UPDATE_BACKUP_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key to update |

#### Request Body

```json
{
  "settingValue": "14",
  "active": true
}
```

#### Field Validation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | Yes | New value for the setting (validated against dataType) |
| `active` | Boolean | No | Whether the setting is active |

#### Example - Update Retention Days

```
PUT /api/backup-settings/backup.retention.days
Content-Type: application/json

{
  "settingValue": "14",
  "active": true
}
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Backup setting updated successfully. Setting Value changed.",
  "data": {
    "id": 15,
    "settingKey": "backup.retention.days",
    "settingValue": "14",
    "dataType": "INTEGER",
    "description": "Number of days to retain backups (older backups are deleted)",
    "active": true,
    "isSystemDefault": true,
    "category": "RETENTION",
    "requiresRestart": false,
    "updatedAt": "2026-02-09T11:20:00"
  },
  "timestamp": "2026-02-09T11:20:00"
}
```

#### Response - Validation Error

```json
{
  "success": false,
  "status": 400,
  "message": "Invalid value for INTEGER type: abc",
  "timestamp": "2026-02-09T11:20:00"
}
```

---

### 6. Reset Setting to Default

Resets a specific backup setting to its default value from `application.properties`.

**Endpoint:** `POST /api/backup-settings/{settingKey}/reset`

**Permission:** `PERM_UPDATE_BACKUP_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key to reset |

#### Example

```
POST /api/backup-settings/backup.retention.days/reset
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Backup setting reset to default value successfully",
  "data": {
    "id": 15,
    "settingKey": "backup.retention.days",
    "settingValue": "7",
    "dataType": "INTEGER",
    "description": "Number of days to retain backups (older backups are deleted)",
    "active": true,
    "isSystemDefault": true,
    "category": "RETENTION",
    "requiresRestart": false,
    "updatedAt": "2026-02-09T11:25:00"
  },
  "timestamp": "2026-02-09T11:25:00"
}
```

---

### 7. Health Check

Health check endpoint to verify the Backup Settings API is operational.

**Endpoint:** `GET /api/backup-settings/health`

**Permission:** None (public endpoint)

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Backup Settings API is healthy",
  "data": null,
  "timestamp": "2026-02-09T10:35:00"
}
```

---

## Using Settings in Code

The `BackupSettingsGetterServices` class provides typed getter methods to read settings with automatic fallback to `application.properties` values:

```java
@Autowired
private BackupSettingsGetterServices backupSettings;

// ========================================
// General Settings
// ========================================

// Check if backups are enabled globally
if (!backupSettings.isBackupEnabled()) {
    log.warn("Backup system is disabled");
    return;
}

// Get backup type
String backupType = backupSettings.getBackupType(); // FULL, INCREMENTAL, DIFFERENTIAL

// ========================================
// Schedule Settings
// ========================================

// Check if scheduled backups are enabled
if (backupSettings.isScheduleEnabled()) {
    // Cron schedule is configured in application.properties
    // and loaded by @Scheduled annotation at startup
}

// ========================================
// Database Settings
// ========================================

// Check if database backups are enabled
if (backupSettings.isDatabaseBackupEnabled()) {
    String host = backupSettings.getDatabaseHost();
    Integer port = backupSettings.getDatabasePort();
    String dbName = backupSettings.getDatabaseName();
    String username = backupSettings.getDatabaseUsername();

    // Check what to include in database backup
    Boolean includeRoutines = backupSettings.includeRoutines();
    Boolean includeTriggers = backupSettings.includeTriggers();
    Boolean includeEvents = backupSettings.includeEvents();
}

// ========================================
// File Settings
// ========================================

// Check if file backups are enabled
if (backupSettings.isFilesBackupEnabled()) {
    // Check which file types to include
    if (backupSettings.includeEmailSignatures()) {
        // Backup email signatures
    }
    if (backupSettings.includeEmailTemplates()) {
        // Backup email templates
    }
    if (backupSettings.includePdfTemplates()) {
        // Backup PDF templates
    }
    if (backupSettings.includeAccommodationImages()) {
        // Backup accommodation images
    }
    if (backupSettings.includeAccommodationDocuments()) {
        // Backup accommodation documents
    }
    // ... more file inclusion checks
}

// ========================================
// Storage Settings
// ========================================

String filenamePrefix = backupSettings.getFilenamePrefix();
String dateFormat = backupSettings.getFilenameDateFormat();

// Generate backup filename
String timestamp = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern(dateFormat));
String filename = filenamePrefix + "_" + timestamp + ".zip";

// ========================================
// Retention Settings
// ========================================

Integer retentionDays = backupSettings.getRetentionDays();
Integer maxBackupCount = backupSettings.getRetentionMaxCount();
Boolean autoCleanup = backupSettings.isAutoCleanupEnabled();

if (autoCleanup) {
    // Delete backups older than retentionDays
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
    // ... cleanup logic
}

// ========================================
// Compression Settings
// ========================================

if (backupSettings.isCompressionEnabled()) {
    String format = backupSettings.getCompressionFormat(); // zip, gzip, tar, tar.gz
    Integer level = backupSettings.getCompressionLevel(); // 0-9

    // Apply compression with specified format and level
}

// ========================================
// Notification Settings
// ========================================

if (backupSettings.isNotificationEnabled()) {
    List<String> emails = backupSettings.getNotificationEmails();

    if (backupSuccessful && backupSettings.notifyOnSuccess()) {
        for (String email : emails) {
            sendNotification(email, "Backup successful");
        }
    }

    if (!backupSuccessful && backupSettings.notifyOnFailure()) {
        sendNotification(email, "Backup failed");
    }
}
```

### Available Helper Methods

#### General Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isBackupEnabled()` | Boolean | Check if backup system is enabled |
| `getBackupType()` | String | Get backup type (FULL/INCREMENTAL/DIFFERENTIAL) |

#### Schedule Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isScheduleEnabled()` | Boolean | Check if scheduled backups are enabled |

#### Database Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isDatabaseBackupEnabled()` | Boolean | Check if database backups are enabled |
| `getDatabaseHost()` | String | Get database host |
| `getDatabasePort()` | Integer | Get database port |
| `getDatabaseName()` | String | Get database name |
| `getDatabaseUsername()` | String | Get database username |
| `includeRoutines()` | Boolean | Include stored procedures/functions |
| `includeTriggers()` | Boolean | Include triggers |
| `includeEvents()` | Boolean | Include scheduled events |

#### File Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isFilesBackupEnabled()` | Boolean | Check if file backups are enabled |
| `includeEmailSignatures()` | Boolean | Include email signatures |
| `includeEmailTemplates()` | Boolean | Include email templates |
| `includePdfTemplates()` | Boolean | Include PDF templates |
| `includeAccommodationImages()` | Boolean | Include accommodation images |
| `includeAccommodationDocuments()` | Boolean | Include accommodation documents |
| `includeParkImages()` | Boolean | Include park images |
| `includeParkDocuments()` | Boolean | Include park documents |
| `includeActivityImages()` | Boolean | Include activity images |
| `includeActivityDocuments()` | Boolean | Include activity documents |
| `includeItineraryDocuments()` | Boolean | Include itinerary documents |
| `includeQuoteDocuments()` | Boolean | Include quote documents |
| `includeSafariDocuments()` | Boolean | Include safari documents |

#### Storage Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `getFilenamePrefix()` | String | Get backup filename prefix |
| `getFilenameDateFormat()` | String | Get filename date format pattern |

#### Retention Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `getRetentionDays()` | Integer | Get retention period in days |
| `getRetentionMaxCount()` | Integer | Get maximum backup count |
| `isAutoCleanupEnabled()` | Boolean | Check if auto cleanup is enabled |

#### Compression Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isCompressionEnabled()` | Boolean | Check if compression is enabled |
| `getCompressionFormat()` | String | Get compression format |
| `getCompressionLevel()` | Integer | Get compression level (0-9) |

#### Notification Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isNotificationEnabled()` | Boolean | Check if notifications are enabled |
| `notifyOnSuccess()` | Boolean | Check if notify on success |
| `notifyOnFailure()` | Boolean | Check if notify on failure |
| `getNotificationEmails()` | List&lt;String&gt; | Get list of notification email addresses |

---

## cURL Examples

### Get All Backup Settings
```bash
curl -X GET "http://localhost:4450/api/backup-settings" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Database Settings Only
```bash
curl -X GET "http://localhost:4450/api/backup-settings/category/DATABASE" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Schedule Settings Only
```bash
curl -X GET "http://localhost:4450/api/backup-settings/category/SCHEDULE" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Active Settings Only
```bash
curl -X GET "http://localhost:4450/api/backup-settings/active" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Specific Setting by Key
```bash
curl -X GET "http://localhost:4450/api/backup-settings/backup.retention.days" \
  -H "Authorization: Bearer <jwt_token>"
```

### Update Retention Days to 14 Days
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.retention.days" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "14",
    "active": true
  }'
```

### Change Backup Schedule to Every 6 Hours
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.schedule.cron" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "0 0 */6 * * ?",
    "active": true
  }'
```

### Disable Email Signature Backups
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.files.include.email.signatures" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "false",
    "active": true
  }'
```

### Enable Backup Notifications
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.notification.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "true",
    "active": true
  }'
```

### Set Notification Emails
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.notification.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "backup-admin@kabengosafaris.com,dev@kabengosafaris.com,ops@kabengosafaris.com",
    "active": true
  }'
```

### Increase Compression Level to Maximum
```bash
curl -X PUT "http://localhost:4450/api/backup-settings/backup.compression.level" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "9",
    "active": true
  }'
```

### Reset Setting to Default Value
```bash
curl -X POST "http://localhost:4450/api/backup-settings/backup.retention.days/reset" \
  -H "Authorization: Bearer <jwt_token>"
```

### Health Check
```bash
curl -X GET "http://localhost:4450/api/backup-settings/health"
```

---

## Common Use Cases

### 1. Configure Weekly Backups on Sundays at Midnight
```bash
# Update cron expression
curl -X PUT "http://localhost:4450/api/backup-settings/backup.schedule.cron" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "0 0 0 * * 0",
    "active": true
  }'
```

### 2. Extend Backup Retention to 30 Days
```bash
# Update retention days
curl -X PUT "http://localhost:4450/api/backup-settings/backup.retention.days" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "30",
    "active": true
  }'
```

### 3. Disable File Backups (Database Only)
```bash
# Disable file backups
curl -X PUT "http://localhost:4450/api/backup-settings/backup.files.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "false",
    "active": true
  }'
```

### 4. Enable Email Notifications for Failed Backups Only
```bash
# Enable notifications
curl -X PUT "http://localhost:4450/api/backup-settings/backup.notification.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "true", "active": true}'

# Disable success notifications
curl -X PUT "http://localhost:4450/api/backup-settings/backup.notification.on.success" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'

# Enable failure notifications
curl -X PUT "http://localhost:4450/api/backup-settings/backup.notification.on.failure" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "true", "active": true}'
```

### 5. Backup Only Critical Files (Exclude Images)
```bash
# Disable image backups
curl -X PUT "http://localhost:4450/api/backup-settings/backup.files.include.accommodation.images" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'

curl -X PUT "http://localhost:4450/api/backup-settings/backup.files.include.park.images" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'

curl -X PUT "http://localhost:4450/api/backup-settings/backup.files.include.activity.images" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'
```

---

## Error Codes

| HTTP Status | Error Type | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data or setting value doesn't match data type |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions for this operation |
| 404 | `NOT_FOUND` | Setting not found with the specified key |
| 500 | `INTERNAL_ERROR` | Server error or database connection issue |

### Example Error Responses

#### Invalid Data Type
```json
{
  "success": false,
  "status": 400,
  "message": "Invalid value for INTEGER type: abc",
  "timestamp": "2026-02-09T11:20:00"
}
```

#### Setting Not Found
```json
{
  "success": false,
  "status": 404,
  "message": "Backup setting not found with key: backup.invalid.key",
  "timestamp": "2026-02-09T11:20:00"
}
```

#### Insufficient Permissions
```json
{
  "success": false,
  "status": 403,
  "message": "Access Denied: Insufficient permissions",
  "timestamp": "2026-02-09T11:20:00"
}
```

---

## Audit Logging

All update and reset operations are automatically logged to the audit log system:

| Action | Description |
|--------|-------------|
| `UPDATE_BACKUP_SETTING` | Backup setting value or status changed |
| `RESET_BACKUP_SETTING` | Backup setting reset to default value |

Audit log entries include:
- User who made the change
- Timestamp of the change
- Old value and new value
- Setting key that was modified
- Client IP address

---

## Data Type Validation

When updating settings, the `settingValue` must be compatible with the setting's `dataType`:

| Data Type | Valid Examples | Invalid Examples |
|-----------|----------------|------------------|
| BOOLEAN | `"true"`, `"false"` | `"yes"`, `"1"`, `"enabled"` |
| INTEGER | `"7"`, `"30"`, `"100"` | `"7.5"`, `"abc"`, `"7 days"` |
| LONG | `"1048576"`, `"10485760"` | `"1MB"`, `"10.5"` |
| STRING | `"localhost"`, `"0 0 2 * * ?"` | Any string is valid |
| DOUBLE | `"5.5"`, `"10.0"` | `"abc"`, `"5.5MB"` |

---

## Best Practices

### 1. Testing Configuration Changes
Always test backup configuration changes in a non-production environment first:
```bash
# Update setting
curl -X PUT "http://localhost:4450/api/backup-settings/backup.retention.days" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "14", "active": true}'

# Trigger manual backup to test
curl -X POST "http://localhost:4450/api/backups/trigger" \
  -H "Authorization: Bearer <jwt_token>"
```

### 2. Monitoring Settings Changes
Regularly review audit logs to track configuration changes and identify unauthorized modifications.

### 3. Backup Verification
After changing file inclusion settings, verify the backup contains expected files:
```bash
# Trigger backup
curl -X POST "http://localhost:4450/api/backups/trigger" \
  -H "Authorization: Bearer <jwt_token>"

# Check backup stats
curl -X GET "http://localhost:4450/api/backups/stats" \
  -H "Authorization: Bearer <jwt_token>"
```

### 4. Schedule Optimization
Consider system load when setting backup schedules:
- Run backups during off-peak hours (typically 2-4 AM)
- Avoid overlapping with other maintenance tasks
- Test schedule changes with manual backups first

### 5. Storage Management
Monitor backup storage consumption:
```bash
# Get backup statistics
curl -X GET "http://localhost:4450/api/backups/stats" \
  -H "Authorization: Bearer <jwt_token>"

# Adjust retention if storage is constrained
curl -X PUT "http://localhost:4450/api/backup-settings/backup.retention.days" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "3", "active": true}'
```

### 6. Notification Configuration
Configure notifications appropriately:
- Enable failure notifications for production systems
- Consider disabling success notifications to reduce email noise
- Use a dedicated monitoring email address

### 7. Compression Settings
Balance compression level with system resources:
- Level 5 (default): Good balance of speed and compression
- Level 9: Maximum compression, slower backups
- Level 1-3: Faster backups, larger files

---

## Troubleshooting

### Problem: Setting update doesn't take effect

**Solution:** Check if the setting requires a restart
```bash
# Get setting details
curl -X GET "http://localhost:4450/api/backup-settings/<settingKey>" \
  -H "Authorization: Bearer <jwt_token>"

# If requiresRestart is true, restart the application
```

### Problem: Invalid cron expression error

**Solution:** Validate cron expression format
- Use standard 6-field cron format: `second minute hour day month weekday`
- Example: `0 0 2 * * ?` (daily at 2:00 AM)
- Test at: https://crontab.guru/ (note: convert to 6-field format)

### Problem: Cannot update setting (403 Forbidden)

**Solution:** Verify user has `PERM_UPDATE_BACKUP_SETTING` permission
```bash
# Check user's current permissions via API
# Contact administrator to grant necessary permissions
```

### Problem: Database backup settings not applying

**Solution:** Verify database credentials and connectivity
```bash
# Get database settings
curl -X GET "http://localhost:4450/api/backup-settings/category/DATABASE" \
  -H "Authorization: Bearer <jwt_token>"

# Test manual backup
curl -X POST "http://localhost:4450/api/backups/trigger" \
  -H "Authorization: Bearer <jwt_token>"
```

---

## Related APIs

- **[Backup Operations API](../API_DOCUMENTATION.md)** - Trigger backups, view backup history, cleanup operations
- **[File Settings API](../../FileSettings/API_DOCUMENTATION.md)** - Configure file upload settings
- **[Security Settings API](../../Security/API_DOCUMENTATION.md)** - Manage security configurations

---

## Support

For issues or questions:
- Check the [Backup System Guide](../BACKUP_SYSTEM_GUIDE.md) for detailed implementation information
- Review audit logs for configuration change history
- Contact system administrator for permission issues
- Consult application logs for backup execution errors
