# Kabengo Safaris Backup System Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Configuration](#configuration)
5. [Backup Methods](#backup-methods)
6. [Restoration](#restoration)
7. [Monitoring](#monitoring)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)
10. [API Reference](#api-reference)

---

## Overview

The Kabengo Safaris Backup System is a comprehensive solution for protecting your critical business data, including:

- **MySQL Database**: All business data (quotes, invoices, customers, safaris, etc.)
- **File Storage**: Documents, images, templates, and uploaded files
- **Configuration**: Application settings and configurations

The system supports both **automated scheduled backups** and **manual on-demand backups**, with configurable retention policies and compression.

---

## Architecture

### Components

1. **BackupSettings** - Database-driven configuration system
2. **BackupService** - Core backup engine for database and files (internal helper)
3. **BackupCreateService** - Service for triggering and creating backups
4. **BackupGetService** - Service for retrieving backup information with pagination
5. **BackupDeleteService** - Service for deleting and cleanup operations
6. **BackupScheduler** - Automated backup scheduler (Spring @Scheduled)
7. **BackupController** - REST API for backup operations
8. **BackupDownloadController** - Secure backup file download endpoint
9. **Bash Scripts** - Alternative cron job backup scripts

### Database Schema

```
backup_settings
├── id (Primary Key)
├── setting_key (Unique)
├── setting_value
├── data_type (ENUM: STRING, INTEGER, BOOLEAN, LONG, DOUBLE)
├── description
├── active (Boolean)
├── is_system_default (Boolean)
├── category (ENUM: GENERAL, SCHEDULE, DATABASE, FILES, STORAGE, RETENTION, COMPRESSION, NOTIFICATION)
├── requires_restart (Boolean)
├── created_at (Timestamp)
└── updated_at (Timestamp)
```

---

## Features

### ✅ Automated Scheduled Backups
- Configurable cron schedule (default: daily at 2:00 AM)
- Runs automatically in background
- Sends notifications on success/failure

### ✅ Manual On-Demand Backups
- Trigger backups via REST API
- Trigger backups via bash scripts
- Immediate execution

### ✅ Comprehensive Data Coverage
- Complete MySQL database dump (with routines, triggers, events)
- All file storage directories
- Selective backup (choose what to backup)

### ✅ Compression & Storage
- ZIP compression (configurable compression level 0-9)
- Automatic file naming with timestamps
- Configurable storage location

### ✅ Retention Management
- Time-based retention (e.g., keep backups for 7 days)
- Count-based retention (e.g., keep last 30 backups)
- Automatic cleanup of old backups

### ✅ Dynamic Configuration
- All settings stored in database
- Change settings without restart
- Fallback to application.properties

### ✅ Monitoring & Notifications
- Detailed logging
- Email notifications (success/failure)
- Backup history and statistics

---

## Configuration

### Database Configuration (Recommended)

All backup settings are stored in the `backup_settings` table and can be modified via REST API without restarting the application.

**API Endpoint:**
```bash
GET    /api/backup-settings          # List all settings
GET    /api/backup-settings/{key}    # Get specific setting
PUT    /api/backup-settings/{key}    # Update setting
POST   /api/backup-settings/{key}/reset  # Reset to default
```

**Example: Update backup schedule**
```bash
curl -X PUT http://localhost:4450/api/backup-settings/backup.schedule.cron \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "settingValue": "0 0 3 * * ?",
    "active": true
  }'
```

### Application Properties (Fallback)

Settings in `application.properties` serve as fallback defaults:

```properties
# General Settings
backup.enabled=true
backup.type=FULL

# Schedule Settings
backup.schedule.enabled=true
backup.schedule.cron=0 0 2 * * ?

# Database Settings
backup.database.enabled=true
backup.database.host=localhost
backup.database.port=3306
backup.database.name=springboot_itineraryledger_kabengosafaris
backup.database.username=root

# File Settings
backup.files.enabled=true

# NOTE: These paths are STATIC (not in database settings - require restart to change)
backup.files.base.path=./data/

# Storage Settings
# NOTE: This path is STATIC (not in database settings - requires restart to change)
backup.storage.path=/opt/lampp/htdocs/kabengosafaris/backups/

backup.storage.filename.prefix=kabengosafaris_backup

# Retention Settings
backup.retention.days=7
backup.retention.max.count=30
backup.retention.auto.cleanup.enabled=true

# Compression Settings
backup.compression.enabled=true
backup.compression.format=zip
backup.compression.level=5

# Notification Settings
backup.notification.enabled=false
backup.notification.on.success=false
backup.notification.on.failure=true
backup.notification.emails=admin@kabengosafaris.com
```

---

## Backup Methods

### Method 1: Automated Scheduled Backups (Recommended)

The scheduler runs automatically based on the configured cron expression.

**Default Schedule:** Daily at 2:00 AM (`0 0 2 * * ?`)

**How it works:**
1. Application starts
2. `BackupScheduler` registers the `@Scheduled` task
3. Task executes at configured time
4. Backup is performed automatically
5. Notifications sent (if enabled)

**To change schedule:**
```bash
# Via API
curl -X PUT http://localhost:4450/api/backup-settings/backup.schedule.cron \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"settingValue": "0 0 3 * * ?", "active": true}'

# Or update application.properties
backup.schedule.cron=0 0 3 * * ?
```

**Common Cron Expressions:**
```
0 0 2 * * ?    # Daily at 2:00 AM
0 0 */6 * * ?  # Every 6 hours
0 0 * * 0 ?    # Weekly on Sunday at midnight
0 0 1 1 * ?    # Monthly on 1st at 1:00 AM
```

### Method 2: Manual REST API Trigger

Trigger backups on-demand via REST API:

```bash
# Trigger backup
curl -X POST http://localhost:4450/api/backups/trigger \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# List backups with pagination
curl -X GET "http://localhost:4450/api/backups?page=0&size=10&sortBy=createdAt&sortDirection=desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Filter backups by date range
curl -X GET "http://localhost:4450/api/backups?startDate=2026-02-01T00:00:00&endDate=2026-02-09T23:59:59" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get single backup details
curl -X GET http://localhost:4450/api/backups/kabengosafaris_backup_20260209_143000.zip \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get backup statistics
curl -X GET http://localhost:4450/api/backups/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Delete specific backups
curl -X DELETE http://localhost:4450/api/backups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '["kabengosafaris_backup_20260201_020000.zip", "kabengosafaris_backup_20260202_020000.zip"]'

# Cleanup old backups
curl -X POST http://localhost:4450/api/backups/cleanup \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Download backup file
curl -X GET http://localhost:4450/api/backups/download/kabengosafaris_backup_20260209_143000.zip \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output backup.zip
```

**Response Example:**
```json
{
  "code": 200,
  "message": "Backup completed successfully",
  "data": {
    "success": true,
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

### Method 3: Bash Scripts (Cron Job Alternative)

Use standalone bash scripts for independent backup operations:

**Location:** `scripts/backup.sh`

**Basic Usage:**
```bash
# Full backup (database + files)
./scripts/backup.sh

# Database only
./scripts/backup.sh --db-only

# Files only
./scripts/backup.sh --files-only

# Without compression
./scripts/backup.sh --no-compress

# Without cleanup
./scripts/backup.sh --no-cleanup
```

**Set up Cron Job:**
```bash
# Edit crontab
crontab -e

# Add backup job (daily at 2:00 AM)
0 2 * * * /home/ricksy/Documents/SPRING\ BOOT\ PROJECTS/kabengosafaris/scripts/backup.sh >> /var/log/kabengo_backup.log 2>&1
```

**Environment Variables:**
```bash
# Set environment variables
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=springboot_itineraryledger_kabengosafaris
export DB_USER=root
export DB_PASSWORD=your_password

# Or create a wrapper script
#!/bin/bash
export DB_PASSWORD=your_password
/path/to/backup.sh
```

---

## Restoration

### Using Restore Script

**Location:** `scripts/restore.sh`

**Usage:**
```bash
# Full restore (database + files)
./scripts/restore.sh /path/to/backup.zip

# Database only
./scripts/restore.sh /path/to/backup.zip --db-only

# Files only
./scripts/restore.sh /path/to/backup.zip --files-only
```

**Important:** The restore script will:
1. Ask for confirmation before proceeding
2. Create a backup of current files before restoration
3. Overwrite existing database and files

### Manual Restoration

**Restore Database:**
```bash
mysql -h localhost -P 3306 -u root -p springboot_itineraryledger_kabengosafaris < database_backup.sql
```

**Restore Files:**
```bash
# Extract backup
unzip kabengosafaris_backup_20260209_143000.zip

# Copy files
cp -r backup_folder/files/* ./data/
```

---

## Monitoring

### Check Backup Status

**Via API:**
```bash
# List all backups with pagination
curl -X GET "http://localhost:4450/api/backups?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get statistics
curl -X GET http://localhost:4450/api/backups/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**List Response:**
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
      }
    ],
    "currentPage": 0,
    "totalItems": 7,
    "totalPages": 1
  }
}
```

**Statistics Response:**
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

### View Logs

**Application Logs:**
```bash
# View backup logs in application output
tail -f /path/to/application.log | grep BACKUP

# Or in Spring Boot logs
tail -f logs/spring-boot-application.log | grep "BackupService\|BackupScheduler"
```

**Bash Script Logs:**
```bash
# Scripts create timestamped log files
ls -lh /opt/lampp/htdocs/kabengosafaris/backups/backup_*.log

# View latest log
tail -f /opt/lampp/htdocs/kabengosafaris/backups/backup_*.log | tail -1
```

### Email Notifications

Configure email notifications for backup status:

**Update Settings:**
```bash
curl -X PUT http://localhost:4450/api/backup-settings/backup.notification.enabled \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"settingValue": "true", "active": true}'

curl -X PUT http://localhost:4450/api/backup-settings/backup.notification.emails \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"settingValue": "admin@kabengosafaris.com,backup@kabengosafaris.com", "active": true}'
```

---

## Best Practices

### 1. **Regular Backup Schedule**
- Run daily backups at off-peak hours (e.g., 2:00 AM)
- Verify backups are completing successfully
- Monitor backup size and duration

### 2. **Retention Policy**
- Keep at least 7 days of daily backups
- Keep at least 4 weekly backups (monthly strategy)
- Keep at least 3 monthly backups (yearly strategy)

```bash
# Update retention settings
curl -X PUT http://localhost:4450/api/backup-settings/backup.retention.days \
  -d '{"settingValue": "7"}'

curl -X PUT http://localhost:4450/api/backup-settings/backup.retention.max.count \
  -d '{"settingValue": "30"}'
```

### 3. **Off-Site Backups**
- Copy backups to remote server or cloud storage
- Use rsync, scp, or cloud sync tools
- Automate off-site transfer after backup completion

**Example rsync to remote server:**
```bash
# Add to cron after backup
rsync -avz --delete \
  /opt/lampp/htdocs/kabengosafaris/backups/ \
  user@remote-server:/backups/kabengo/
```

### 4. **Test Restoration Regularly**
- Perform test restorations monthly
- Verify data integrity
- Document restoration procedures

### 5. **Monitor Backup Health**
- Check backup logs daily
- Enable failure notifications
- Set up monitoring alerts (if using monitoring tools)

### 6. **Secure Backups**
- Restrict backup directory permissions
  ```bash
  chmod 700 /opt/lampp/htdocs/kabengosafaris/backups/
  chown www-data:www-data /opt/lampp/htdocs/kabengosafaris/backups/
  ```
- Encrypt sensitive backups
  ```bash
  # Encrypt backup
  gpg -c kabengosafaris_backup_20260209_020000.zip

  # Decrypt backup
  gpg kabengosafaris_backup_20260209_020000.zip.gpg
  ```

### 7. **Document Your Strategy**
- Document backup procedures
- Document restoration procedures
- Keep emergency contact information

---

## Troubleshooting

### Problem: Scheduled backups not running

**Solution:**
1. Check if scheduler is enabled:
   ```bash
   curl http://localhost:4450/api/backup-settings/backup.schedule.enabled
   ```

2. Check cron expression is valid:
   ```bash
   curl http://localhost:4450/api/backup-settings/backup.schedule.cron
   ```

3. Check application logs for errors:
   ```bash
   tail -f logs/application.log | grep BackupScheduler
   ```

### Problem: Database backup fails

**Solution:**
1. Check database credentials in settings
2. Verify `mysqldump` is installed:
   ```bash
   which mysqldump
   ```

3. Test database connection:
   ```bash
   mysql -h localhost -P 3306 -u root -p -e "SHOW DATABASES;"
   ```

4. Check database user has necessary privileges:
   ```sql
   GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON springboot_itineraryledger_kabengosafaris.* TO 'root'@'localhost';
   FLUSH PRIVILEGES;
   ```

### Problem: File backup fails

**Solution:**
1. Check file paths exist:
   ```bash
   ls -la ./data/
   ```

2. Check permissions:
   ```bash
   # Application user must have read access
   sudo chown -R www-data:www-data ./data/
   ```

3. Check disk space:
   ```bash
   df -h /opt/lampp/htdocs/kabengosafaris/backups/
   ```

### Problem: Backup directory full

**Solution:**
1. Manual cleanup:
   ```bash
   curl -X POST http://localhost:4450/api/backups/cleanup \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

2. Adjust retention policy:
   ```bash
   # Reduce retention days
   curl -X PUT http://localhost:4450/api/backup-settings/backup.retention.days \
     -d '{"settingValue": "3"}'
   ```

3. Move old backups to archive:
   ```bash
   mkdir /archive/backups
   mv /opt/lampp/htdocs/kabengosafaris/backups/kabengosafaris_backup_2026* /archive/backups/
   ```

### Problem: Restoration fails

**Solution:**
1. Verify backup file integrity:
   ```bash
   # Test ZIP file
   unzip -t kabengosafaris_backup_20260209_020000.zip
   ```

2. Check backup contents:
   ```bash
   unzip -l kabengosafaris_backup_20260209_020000.zip
   ```

3. Ensure sufficient disk space:
   ```bash
   df -h
   ```

---

## API Reference

### Backup Settings API

| Endpoint | Method | Description | Permission |
|----------|--------|-------------|------------|
| `/api/backup-settings` | GET | List all backup settings | PERM_READ_BACKUP_SETTING |
| `/api/backup-settings/category/{category}` | GET | List settings by category | PERM_READ_BACKUP_SETTING |
| `/api/backup-settings/active` | GET | List active settings only | PERM_READ_BACKUP_SETTING |
| `/api/backup-settings/{key}` | GET | Get specific setting | PERM_READ_BACKUP_SETTING |
| `/api/backup-settings/{key}` | PUT | Update setting value | PERM_UPDATE_BACKUP_SETTING |
| `/api/backup-settings/{key}/reset` | POST | Reset to default value | PERM_UPDATE_BACKUP_SETTING |

### Backup Operations API

| Endpoint | Method | Description | Permission |
|----------|--------|-------------|------------|
| `/api/backups/trigger` | POST | Trigger manual backup | PERM_CREATE_BACKUP |
| `/api/backups` | GET | List backups with pagination & filtering | PERM_READ_BACKUP |
| `/api/backups/{filename}` | GET | Get single backup details | PERM_READ_BACKUP |
| `/api/backups/stats` | GET | Get backup statistics | PERM_READ_BACKUP |
| `/api/backups` | DELETE | Delete specific backups | PERM_DELETE_BACKUP |
| `/api/backups/cleanup` | POST | Cleanup old backups (retention policy) | PERM_DELETE_BACKUP |
| `/api/backups/download/{filename}` | GET | Download backup file | BACKUP_READ |

#### Backup List Query Parameters

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `filename` | String | Filter by filename (partial match) | `?filename=20260209` |
| `startDate` | DateTime | Filter by date (after) | `?startDate=2026-02-01T00:00:00` |
| `endDate` | DateTime | Filter by date (before) | `?endDate=2026-02-09T23:59:59` |
| `minSize` | Long | Minimum file size in bytes | `?minSize=10485760` |
| `maxSize` | Long | Maximum file size in bytes | `?maxSize=1073741824` |
| `isCompressed` | Boolean | Filter by compression status | `?isCompressed=true` |
| `page` | Integer | Page number (0-based) | `?page=0` |
| `size` | Integer | Page size | `?size=10` |
| `sortBy` | String | Sort field (name, size, createdAt) | `?sortBy=createdAt` |
| `sortDirection` | String | Sort direction (asc, desc) | `?sortDirection=desc` |

### Setting Categories

- **GENERAL** - General backup settings (enabled, type)
- **SCHEDULE** - Backup scheduling settings (cron, hour, minute)
- **DATABASE** - Database backup settings (host, port, name, includes)
- **FILES** - File system backup settings (paths, includes)
- **STORAGE** - Storage settings (path, filename format)
- **RETENTION** - Retention policy settings (days, max count, auto cleanup)
- **COMPRESSION** - Compression settings (enabled, format, level)
- **NOTIFICATION** - Notification settings (enabled, on success/failure, email)

---

## Permissions

The backup system uses the following permissions:

### Entity Permissions (Standard CRUD)

- `PERM_CREATE_BACKUP` - Create/trigger backups
- `PERM_READ_BACKUP` - View backup history and statistics
- `PERM_UPDATE_BACKUP` - Update backup settings (reserved)
- `PERM_DELETE_BACKUP` - Delete/cleanup old backups

- `PERM_CREATE_BACKUP_SETTING` - Create new backup settings
- `PERM_READ_BACKUP_SETTING` - View backup settings
- `PERM_UPDATE_BACKUP_SETTING` - Modify backup settings
- `PERM_DELETE_BACKUP_SETTING` - Delete custom backup settings

### Custom Permissions

- `TRIGGER_MANUAL_BACKUP` - Trigger manual backup operations
- `VIEW_BACKUP_HISTORY` - View backup history and statistics
- `CLEANUP_OLD_BACKUPS` - Manually cleanup old backups
- `DOWNLOAD_BACKUP` - Download backup files
- `RESTORE_BACKUP` - Restore from backup (high privilege)

---

## Support

For issues or questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review application logs
- Contact: IT Team - Kabengo Safaris

---

**Last Updated:** 2026-02-09
**Version:** 1.0.0
