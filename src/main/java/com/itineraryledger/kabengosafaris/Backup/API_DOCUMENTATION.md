# Backup API Documentation

## Overview

The Backup API provides comprehensive endpoints for managing, viewing, and downloading backup files created by the Kabenga Safaris backup system. The API supports pagination, filtering, secure downloads, and bulk operations.

**Base URL:** `/api/backups`

**Controllers:**
- `BackupController.java` - Main backup operations (create, list, delete, stats)
- `BackupDownloadController.java` - Secure file downloads

**Services:**
- `BackupCreateService` - Backup creation and triggering
- `BackupGetService` - Backup retrieval with pagination and filtering
- `BackupDeleteService` - Backup deletion and cleanup operations

---

## Authentication & Authorization

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| POST /api/backups/trigger | `PERM_CREATE_BACKUP` |
| GET /api/backups | `PERM_READ_BACKUP` |
| GET /api/backups/{filename} | `PERM_READ_BACKUP` |
| GET /api/backups/stats | `PERM_READ_BACKUP` |
| DELETE /api/backups | `PERM_DELETE_BACKUP` |
| POST /api/backups/cleanup | `PERM_DELETE_BACKUP` |
| GET /api/backups/download/{filename} | `BACKUP_READ` |

---

## Backup Operations Endpoints

### 1. Trigger Manual Backup

Creates a new backup of the database and files.

**Endpoint:** `POST /api/backups/trigger`

**Permission:** `PERM_CREATE_BACKUP`

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "Backup completed successfully",
  "data": {
    "success": true,
    "message": "Backup completed successfully",
    "startTime": "2026-02-09T14:30:00",
    "endTime": "2026-02-09T14:35:23",
    "durationSeconds": 323,
    "backupPath": "/opt/lampp/htdocs/kabengosafaris/backups/kabengosafaris_backup_20260209_143000.zip",
    "backupName": "kabengosafaris_backup_20260209_143000.zip",
    "backupSize": 314572800,
    "backupSizeFormatted": "300.00 MB",
    "databaseBackupSuccess": true,
    "filesBackupSuccess": true,
    "compressed": true,
    "downloadUrl": "http://localhost:8080/api/backups/download/kabengosafaris_backup_20260209_143000.zip"
  }
}
```

#### Response - Failure (500)

```json
{
  "code": 500,
  "message": "Backup failed: Database connection error",
  "error": "BACKUP_FAILED",
  "data": {
    "success": false,
    "error": "Could not connect to database",
    "startTime": "2026-02-09T14:30:00",
    "endTime": "2026-02-09T14:30:15"
  }
}
```

#### Example

```bash
curl -X POST http://localhost:4450/api/backups/trigger \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 2. List Backups with Pagination

Retrieves a paginated list of backups with optional filtering and sorting.

**Endpoint:** `GET /api/backups`

**Permission:** `PERM_READ_BACKUP`

#### Query Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `filename` | String | No | Filter by filename (partial match) | `?filename=20260209` |
| `startDate` | DateTime | No | Filter by creation date (after) | `?startDate=2026-02-01T00:00:00` |
| `endDate` | DateTime | No | Filter by creation date (before) | `?endDate=2026-02-09T23:59:59` |
| `minSize` | Long | No | Minimum file size in bytes | `?minSize=10485760` |
| `maxSize` | Long | No | Maximum file size in bytes | `?maxSize=1073741824` |
| `isCompressed` | Boolean | No | Filter by compression status | `?isCompressed=true` |
| `page` | Integer | No | Page number (0-based, default: 0) | `?page=0` |
| `size` | Integer | No | Page size (default: 10) | `?size=20` |
| `sortBy` | String | No | Sort field (name, size, createdAt) | `?sortBy=createdAt` |
| `sortDirection` | String | No | Sort direction (asc, desc) | `?sortDirection=desc` |

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "Backups retrieved successfully",
  "data": {
    "backups": [
      {
        "name": "kabengosafaris_backup_20260209_020000.zip",
        "size": 314572800,
        "sizeFormatted": "300.00 MB",
        "createdAt": "2026-02-09T02:00:00",
        "isDirectory": false,
        "fileExtension": "zip",
        "downloadUrl": "http://localhost:8080/api/backups/download/kabengosafaris_backup_20260209_020000.zip"
      },
      {
        "name": "kabengosafaris_backup_20260208_020000.zip",
        "size": 312428800,
        "sizeFormatted": "298.00 MB",
        "createdAt": "2026-02-08T02:00:00",
        "isDirectory": false,
        "fileExtension": "zip",
        "downloadUrl": "http://localhost:8080/api/backups/download/kabengosafaris_backup_20260208_020000.zip"
      }
    ],
    "currentPage": 0,
    "totalItems": 7,
    "totalPages": 1
  }
}
```

#### Examples

**Get first page (10 items):**
```bash
curl -X GET "http://localhost:4450/api/backups?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Filter by date range:**
```bash
curl -X GET "http://localhost:4450/api/backups?startDate=2026-02-01T00:00:00&endDate=2026-02-09T23:59:59" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Filter by size (100MB to 500MB):**
```bash
curl -X GET "http://localhost:4450/api/backups?minSize=104857600&maxSize=524288000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Search by filename:**
```bash
curl -X GET "http://localhost:4450/api/backups?filename=20260209" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Sort by size (largest first):**
```bash
curl -X GET "http://localhost:4450/api/backups?sortBy=size&sortDirection=desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 3. Get Single Backup Details

Retrieves detailed information about a specific backup file.

**Endpoint:** `GET /api/backups/{filename}`

**Permission:** `PERM_READ_BACKUP`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `filename` | String | Yes | The backup filename |

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "Backup retrieved successfully",
  "data": {
    "name": "kabengosafaris_backup_20260209_143000.zip",
    "path": "/opt/lampp/htdocs/kabengosafaris/backups/kabengosafaris_backup_20260209_143000.zip",
    "size": 314572800,
    "sizeFormatted": "300.00 MB",
    "createdAt": "2026-02-09T14:30:00",
    "isDirectory": false,
    "isCompressed": true,
    "compressionFormat": "ZIP",
    "containsDatabase": true,
    "containsFiles": true,
    "fileExtension": "zip",
    "downloadUrl": "http://localhost:8080/api/backups/download/kabengosafaris_backup_20260209_143000.zip"
  }
}
```

#### Response - Not Found (404)

```json
{
  "code": 404,
  "message": "Backup not found",
  "error": "BACKUP_NOT_FOUND"
}
```

#### Example

```bash
curl -X GET "http://localhost:4450/api/backups/kabengosafaris_backup_20260209_143000.zip" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 4. Get Backup Statistics

Retrieves comprehensive backup statistics including total size, count, and averages.

**Endpoint:** `GET /api/backups/stats`

**Permission:** `PERM_READ_BACKUP`

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "Backup statistics retrieved successfully",
  "data": {
    "totalBackups": 7,
    "totalSize": 2147483648,
    "totalSizeFormatted": "2.00 GB",
    "compressedBackups": 7,
    "uncompressedBackups": 0,
    "oldestBackup": "2026-02-02T02:00:00",
    "newestBackup": "2026-02-09T02:00:00",
    "averageBackupSize": 306783232,
    "averageBackupSizeFormatted": "292.57 MB"
  }
}
```

#### Example

```bash
curl -X GET http://localhost:4450/api/backups/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 5. Delete Backups

Deletes specific backup files by filename. Supports bulk deletion.

**Endpoint:** `DELETE /api/backups`

**Permission:** `PERM_DELETE_BACKUP`

#### Request Body

Array of backup filenames to delete:

```json
[
  "kabengosafaris_backup_20260201_020000.zip",
  "kabengosafaris_backup_20260202_020000.zip"
]
```

#### Security Validations

- Prevents directory traversal (rejects filenames with `..`, `/`, `\`)
- Validates filename prefix matches configured backup prefix
- Ensures file is within backup directory
- Verifies file exists before deletion

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "2 backups deleted successfully",
  "data": null
}
```

#### Response - Partial Success (200 OK)

```json
{
  "code": 200,
  "message": "1 backup deleted successfully. 1 backup skipped",
  "data": [
    "Backup not found: kabengosafaris_backup_20260299_020000.zip"
  ]
}
```

#### Response - All Failed (400 Bad Request)

```json
{
  "code": 400,
  "message": "2 backups skipped: Backup not found: file1.zip; Invalid filename: file2.zip",
  "error": "NO_BACKUPS_DELETED"
}
```

#### Example

```bash
curl -X DELETE http://localhost:4450/api/backups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '[
    "kabengosafaris_backup_20260201_020000.zip",
    "kabengosafaris_backup_20260202_020000.zip"
  ]'
```

---

### 6. Cleanup Old Backups

Manually triggers cleanup of old backups based on retention policy configured in settings.

**Endpoint:** `POST /api/backups/cleanup`

**Permission:** `PERM_DELETE_BACKUP`

**How it works:**
1. Retrieves retention settings from database:
   - `backup.retention.days` - Delete backups older than X days
   - `backup.retention.max.count` - Keep only the latest X backups
2. Identifies backups that exceed retention policy
3. Deletes old backups automatically
4. Returns count of deleted backups

#### Response - Success (200 OK)

```json
{
  "code": 200,
  "message": "3 old backup(s) deleted",
  "data": null
}
```

#### Response - No Cleanup Needed (200 OK)

```json
{
  "code": 200,
  "message": "No old backups to clean up",
  "data": null
}
```

#### Example

```bash
curl -X POST http://localhost:4450/api/backups/cleanup \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Download Endpoint

### 7. Download Backup File

Downloads a backup file by filename. Provides secure, authenticated file download with multiple security layers.

**Endpoint:** `GET /api/backups/download/{filename}`

**Permission:** `BACKUP_READ`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `filename` | String | Yes | The backup filename to download (e.g., `kabengosafaris_backup_20260209_143000.zip`) |

#### Filename Requirements

Valid backup filenames must:
- Start with the configured backup prefix (default: `kabengosafaris_backup`)
- End with `.zip` OR match the pattern `{prefix}_{YYYYMMDD}_{HHMMSS}`
- NOT contain directory traversal characters (`..`, `/`, `\`)
- NOT contain null bytes or special characters

#### Supported File Types

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.zip` | `application/zip` | Compressed backup archive |
| `.tar.gz` | `application/gzip` | Tar gzip compressed archive |
| `.sql` | `application/sql` | SQL database dump |
| Other | `application/octet-stream` | Generic binary file |

#### Security Features

The download endpoint implements multiple security layers:

1. **Spring Security Authorization**
   - Requires `BACKUP_READ` permission
   - JWT token validation

2. **Filename Validation**
   - Prevents directory traversal (blocks `..`, `/`, `\`)
   - Validates filename prefix matches configured backup prefix
   - Ensures file extension is `.zip` or matches backup timestamp pattern
   - Blocks null bytes and other malicious characters

3. **Path Normalization**
   - Normalizes file paths to resolve any symbolic links or relative paths
   - Ensures requested file is within the backup directory
   - Returns 400 Bad Request for any path traversal attempts

4. **File System Checks**
   - Verifies file exists before serving
   - Ensures requested path is a file (not a directory)
   - Returns 404 Not Found for missing files

#### Response - Success (200 OK)

**Headers:**
```
Content-Type: application/zip
Content-Disposition: attachment; filename="kabengosafaris_backup_20260209_143000.zip"
Content-Length: 314572800
```

**Body:** Binary file stream

#### Response - Bad Request (400)

Returned when:
- Filename is invalid or contains directory traversal characters
- Filename doesn't match expected backup pattern
- Requested path is outside backup directory

```http
HTTP/1.1 400 Bad Request
Content-Length: 0
```

**Example scenarios:**
```bash
# Directory traversal attempt
GET /api/backups/download/../../etc/passwd
# Response: 400 Bad Request

# Invalid filename pattern
GET /api/backups/download/malicious_file.zip
# Response: 400 Bad Request

# Contains path separator
GET /api/backups/download/subdir/backup.zip
# Response: 400 Bad Request
```

#### Response - Not Found (404)

Returned when:
- Backup file doesn't exist
- Requested path is a directory instead of a file

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

#### Response - Unauthorized (401)

Returned when:
- JWT token is missing or invalid
- Token has expired

```json
{
  "code": 401,
  "message": "Unauthorized",
  "error": "Invalid or expired token"
}
```

#### Response - Forbidden (403)

Returned when:
- User doesn't have `BACKUP_READ` permission
- User is authenticated but not authorized

```json
{
  "code": 403,
  "message": "Forbidden",
  "error": "Insufficient permissions"
}
```

#### Examples

**Example 1: Download latest backup with curl**

```bash
curl -X GET "http://localhost:4450/api/backups/download/kabengosafaris_backup_20260209_143000.zip" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output backup.zip
```

**Example 2: Download specific backup using wget**

```bash
wget --header="Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:4450/api/backups/download/kabengosafaris_backup_20260208_020000.zip" \
  -O backup_20260208.zip
```

**Example 3: JavaScript/Fetch API**

```javascript
fetch('http://localhost:4450/api/backups/download/kabengosafaris_backup_20260209_143000.zip', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  }
})
.then(response => response.blob())
.then(blob => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'backup.zip';
  a.click();
});
```

---

## Common Use Cases

### Use Case 1: Daily Backup Monitoring

Check if today's backup completed successfully:

```bash
# Get latest backups
curl -X GET "http://localhost:4450/api/backups?page=0&size=1&sortBy=createdAt&sortDirection=desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Check statistics
curl -X GET "http://localhost:4450/api/backups/stats" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Use Case 2: Download Backups for Off-Site Storage

```bash
#!/bin/bash
# Script to download all backups to remote server

TOKEN="your_jwt_token"
BACKUP_SERVER="http://localhost:4450"
DESTINATION="/remote-backup-storage/"

# Get list of backups
BACKUPS=$(curl -s -X GET "${BACKUP_SERVER}/api/backups" \
  -H "Authorization: Bearer ${TOKEN}" \
  | jq -r '.data.backups[].name')

# Download each backup
for backup in $BACKUPS; do
  echo "Downloading ${backup}..."
  curl -X GET "${BACKUP_SERVER}/api/backups/download/${backup}" \
    -H "Authorization: Bearer ${TOKEN}" \
    --output "${DESTINATION}${backup}"
done
```

---

### Use Case 3: Cleanup Old Backups Before System Maintenance

```bash
# Check current backup count
curl -X GET http://localhost:4450/api/backups/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Cleanup old backups
curl -X POST http://localhost:4450/api/backups/cleanup \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Verify cleanup
curl -X GET http://localhost:4450/api/backups/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Use Case 4: Find and Delete Specific Old Backups

```bash
# Find backups from January 2026
curl -X GET "http://localhost:4450/api/backups?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Delete specific backups
curl -X DELETE http://localhost:4450/api/backups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '[
    "kabengosafaris_backup_20260115_020000.zip",
    "kabengosafaris_backup_20260122_020000.zip"
  ]'
```

---

## Error Response Summary

| HTTP Code | Description | Common Causes |
|-----------|-------------|---------------|
| 200 | Success | Operation completed successfully |
| 400 | Bad Request | Invalid filename, directory traversal attempt, invalid request body |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Missing required permission |
| 404 | Not Found | Backup file doesn't exist, path is a directory |
| 500 | Internal Server Error | Backup creation failed, file system error, unexpected exception |

---

## Best Practices

### 1. Secure Token Management
- Never hardcode JWT tokens in scripts
- Use environment variables or secure credential stores
- Rotate tokens regularly
- Use HTTPS in production to protect tokens in transit

```bash
# Good: Use environment variable
TOKEN="${BACKUP_API_TOKEN}"

# Bad: Hardcoded token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 2. Verify Downloaded Files
Always verify backup integrity after download:

```bash
# Download backup
curl -X GET "${BACKUP_SERVER}/api/backups/download/${FILENAME}" \
  -H "Authorization: Bearer ${TOKEN}" \
  --output "${FILENAME}"

# Verify file size
FILE_SIZE=$(stat -f%z "${FILENAME}")
if [ $FILE_SIZE -gt 0 ]; then
  echo "Download successful: ${FILE_SIZE} bytes"
else
  echo "Download failed: file is empty"
  exit 1
fi

# Verify ZIP integrity
unzip -t "${FILENAME}"
```

### 3. Implement Retry Logic
Network issues can interrupt operations. Implement retry logic:

```bash
#!/bin/bash
MAX_RETRIES=3
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  curl -X GET "${BACKUP_SERVER}/api/backups/download/${FILENAME}" \
    -H "Authorization: Bearer ${TOKEN}" \
    --output "${FILENAME}" \
    --fail

  if [ $? -eq 0 ]; then
    echo "Download successful"
    exit 0
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Download failed. Retry $RETRY_COUNT of $MAX_RETRIES..."
    sleep 5
  fi
done
```

### 4. Use Pagination for Large Backup Lists
When retrieving backup lists, use pagination to avoid overwhelming responses:

```bash
# Good: Paginated request
curl "http://localhost:4450/api/backups?page=0&size=20"

# Avoid: Requesting all backups at once without pagination
```

### 5. Filter and Sort Intelligently
Use filters to narrow down results before processing:

```bash
# Find large backups (>500MB) from last week
curl "http://localhost:4450/api/backups?minSize=524288000&startDate=2026-02-02T00:00:00&sortBy=size&sortDirection=desc"
```

---

## Related Documentation

- **BACKUP_SYSTEM_GUIDE.md** - Comprehensive backup system guide
- **Backup Settings API** - Documentation for backup configuration endpoints
- **Email Events** - Backup notification email templates and variables

---

## Support

For issues or questions about the Backup API:

- **Check Application Logs:** `/var/log/kabenga_safaris/application.log`
- **Check Backup Directory:** `/opt/lampp/htdocs/kabengosafaris/backups/`
- **Verify Permissions:** Ensure user has appropriate permissions
- **Contact:** IT Team - Kabenga Safaris

---

**Last Updated:** 2026-02-09
**API Version:** 1.0.0
**Spring Boot Version:** 3.x
**Controllers:** `BackupController.java`, `BackupDownloadController.java`
