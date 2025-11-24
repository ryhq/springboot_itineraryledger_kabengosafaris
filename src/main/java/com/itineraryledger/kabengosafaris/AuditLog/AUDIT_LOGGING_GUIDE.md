# Audit Logging Guide

## Overview

The audit logging system captures every action performed in the application, storing comprehensive logs in the database. This enables full traceability of user actions, entity changes, and system operations.

### Key Features

- **Automatic Action Tracking**: Uses AOP annotations to automatically log method calls without modifying business logic
- **Comprehensive Logging**: Captures user identity, IP address, browser info, request/response data, and timestamps
- **Asynchronous Processing**: Non-blocking audit logging using async thread pool to prevent performance degradation
- **Dynamic Configuration**: Runtime-configurable settings via REST API without requiring application restart
- **Policy-Based Filtering**: Automatically excludes sensitive data (passwords, tokens) and applies retention policies
- **Automatic Cleanup**: Scheduled tasks automatically delete logs older than retention period
- **Sensitive Data Protection**: Multiple layers of protection against logging sensitive information
- **Audit Trail Versioning**: Track complete change history for any entity with before/after values
- **Compliance Ready**: Supports audit log retention policies, access control, and change tracking for compliance

## Architecture

### Components

**Core Audit Logging (Automatic tracking):**
1. **AuditLog Entity** - JPA entity representing an audit log record
2. **AuditLogRepository** - Data access layer for audit logs
3. **AuditLogService** - Business logic for managing and querying audit logs, applies policies
4. **AuditLogAnnotation** - Custom annotation to mark methods for logging
5. **AuditLoggingAspect** - AOP aspect that intercepts annotated methods and captures context
6. **AuditLogMaintenanceScheduler** - Scheduled tasks for automatic log cleanup/retention

**Dynamic Settings Management (Runtime configuration):**

7. **AuditLogSetting Entity** - Database-driven configuration storage
8. **AuditLogSettingRepository** - Repository for settings CRUD
9. **AuditLogSettingServices** - Business logic for updating and resetting settings
10. **AuditLogSettingGetterServices** - Retrieves and parses runtime settings with fallbacks
11. **AuditLogSettingController** - REST endpoints for managing settings
12. **AuditLogSettingInitializer** - Initializes default settings on application startup

### How It Works

```
User performs action
    ↓
Method marked with @AuditLogAnnotation is called
    ↓
AuditLoggingAspect intercepts the method (AOP)
    ↓
Captures: user, action, entity type, IP address, timestamp
    ↓
Executes the actual method
    ↓
Serializes request/response data
    ↓
Records success/failure status
    ↓
Saves audit log asynchronously (non-blocking)
    ↓
Method returns to caller
```

## Usage

### 1. Add @AuditLogAnnotation to Service Methods

To enable audit logging for a method, add the `@AuditLogAnnotation` annotation:

```java
@Service
public class RoleService {

    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_ROLE",
        entityType = "Role",
        entityIdParamName = "",  // Optional: parameter name containing entity ID
        description = "Create a new role"
    )
    public Role createRole(String name, String displayName, String description) {
        // Your implementation
        return role;
    }

    @Transactional
    @AuditLogAnnotation(
        action = "UPDATE_ROLE",
        entityType = "Role",
        entityIdParamName = "roleId",  // Captures this as entityId
        description = "Update an existing role"
    )
    public Role updateRole(Long roleId, String displayName) {
        // Your implementation
        return role;
    }
}
```

### Annotation Parameters

| Parameter | Required | Example | Description |
|-----------|----------|---------|-------------|
| `action` | Yes | `CREATE_ROLE` | Unique action identifier |
| `entityType` | Yes | `Role` | Type of entity being acted upon |
| `entityIdParamName` | No | `roleId` | Method parameter name containing the entity ID |
| `description` | No | `Create a new role` | Human-readable description of the action |

### 2. What Gets Logged

Each audit log entry captures:

```json
{
  "id": 1,
  "userId": 123,                          // ID of user performing action
  "username": "admin@example.com",        // Username
  "action": "CREATE_ROLE",                // Action performed
  "entityType": "Role",                   // Entity type
  "entityId": 456,                        // ID of affected entity
  "description": "Create a new role",     // Action description
  "oldValues": {...},                     // Request parameters (excludes passwords)
  "newValues": {...},                     // Response data
  "ipAddress": "192.168.1.100",          // Client IP address
  "userAgent": "Mozilla/5.0...",         // Browser/client info
  "createdAt": "2025-01-15T10:30:00",   // Timestamp
  "status": "SUCCESS",                    // SUCCESS or FAILURE
  "errorMessage": null                    // Error details if failed
}
```

### 3. Sensitive Field Protection

The following fields are automatically excluded from logs:

- Fields containing: `password`, `token`, `secret`, `apikey`, `creditcard`
- This is case-insensitive

Example:
```java
@AuditLogAnnotation(action = "CREATE_USER", entityType = "User")
public User createUser(String email, String password) {  // password is excluded
    return userService.create(email, password);
}
```

## REST API Endpoints

### Get All Audit Logs (Paginated)
```http
GET /api/audit-logs?page=0&size=20
Authorization: Bearer <token>
```

Response:
```json
{
  "content": [...],
  "pageable": {...},
  "totalElements": 1000,
  "totalPages": 50
}
```

### Get Audit Logs for Specific User
```http
GET /api/audit-logs/user/{userId}?page=0&size=20
Authorization: Bearer <token>
```

### Get Audit Logs for Specific Action
```http
GET /api/audit-logs/action/{action}?page=0&size=20
Authorization: Bearer <token>
```

Example:
```http
GET /api/audit-logs/action/CREATE_ROLE?page=0&size=20
```

### Get Audit Logs for Specific Entity Type
```http
GET /api/audit-logs/entity-type/{entityType}?page=0&size=20
Authorization: Bearer <token>
```

### Get Complete Audit Trail for Specific Entity
```http
GET /api/audit-logs/entity/{entityType}/{entityId}
Authorization: Bearer <token>
```

Example: Get all changes to Role #456
```http
GET /api/audit-logs/entity/Role/456
```

### Get Audit Logs by Date Range
```http
GET /api/audit-logs/date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59&page=0&size=20
Authorization: Bearer <token>
```

### Get Audit Logs for User by Date Range
```http
GET /api/audit-logs/user/{userId}/date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
Authorization: Bearer <token>
```

### Get Audit Logs for Action by Date Range
```http
GET /api/audit-logs/action/{action}/date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59&page=0&size=20
Authorization: Bearer <token>
```

### Delete Old Audit Logs (Retention Policy)
```http
DELETE /api/audit-logs/cleanup?retentionDays=90
Authorization: Bearer <token>
```

## Configuration

### Application Properties (Fallback Defaults)

```properties
# Enable async processing for audit logs (non-blocking)
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100

# Audit log retention in days (logs older than this are candidates for cleanup)
audit.log.retention.days=90

# Enable/disable audit logging globally
audit.log.enabled=true

# Capture IP address in audit logs
audit.log.capture.ip.address=true

# Capture user agent in audit logs
audit.log.capture.user.agent=true

# Capture old values (request parameters)
audit.log.capture.old.values=true

# Capture new values (response data)
audit.log.capture.new.values=true

# Comma-separated list of field names to exclude from old/new values
audit.log.excluded.fields=password,token,secret,apikey,creditcard

# Maximum length for old/new value strings (exceeding this will be truncated)
audit.log.max.value.length=2048
```

### Dynamic Settings (Database-Driven)

Audit logging configuration is now managed dynamically through the database via `AuditLogSetting` entity. This allows configuration changes without restarting the application.

**Available Settings:**

| Setting Key | Data Type | Default | Description |
|------------|-----------|---------|-------------|
| `audit.log.enabled` | BOOLEAN | true | Enable/disable audit logging globally |
| `audit.log.retention.days` | INTEGER | 90 | Number of days to retain audit logs |
| `audit.log.capture.ip.address` | BOOLEAN | true | Capture client IP address |
| `audit.log.capture.user.agent` | BOOLEAN | true | Capture browser/client user agent |
| `audit.log.capture.old.values` | BOOLEAN | true | Capture old values (request parameters) |
| `audit.log.capture.new.values` | BOOLEAN | true | Capture new values (response data) |
| `audit.log.excluded.fields` | STRING | password,token,secret,apikey,creditcard | Comma-separated field names to exclude |
| `audit.log.max.value.length` | INTEGER | 2048 | Maximum length for field values before truncation |

Settings are categorized as:
- **GENERAL**: Enable/disable audit logging and retention policies
- **CAPTURE**: Configure what data to capture (IP, user agent, old/new values)
- **VALUES**: Configure value filtering and truncation

### Thread Pool Configuration

The async audit logging uses a thread pool:
- **Core Pool Size**: 2 threads always available
- **Max Pool Size**: 5 threads when needed
- **Queue Capacity**: 100 pending tasks

This ensures audit logging never blocks the main request thread.

## Database Schema

The `audit_logs` table is automatically created by JPA with the following structure:

```sql
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT,
    description TEXT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(255),
    user_agent TEXT,
    created_at DATETIME NOT NULL,
    status VARCHAR(50),
    error_message TEXT,

    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_entity_type_id (entity_type, entity_id)
);
```

The `audit_log_settings` table stores dynamic configuration:

```sql
CREATE TABLE audit_log_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

## System Components

### Core Classes

| Class | Purpose |
|-------|---------|
| [AuditLog.java](AuditLog.java) | JPA entity for audit log records with indexed fields |
| [AuditLogAnnotation.java](AuditLogAnnotation.java) | Custom annotation to mark methods for automatic logging |
| [AuditLoggingAspect.java](AuditLoggingAspect.java) | AOP aspect intercepting annotated methods and capturing context |
| [AuditLogService.java](AuditLogService.java) | Service managing log storage, policy application, and cleanup |
| [AuditLogRepository.java](AuditLogRepository.java) | JPA repository for database queries |
| [AuditLogMaintenanceScheduler.java](AuditLogMaintenanceScheduler.java) | Scheduled tasks for automatic log cleanup |

### Settings Management Classes

| Class | Purpose |
|-------|---------|
| [AuditLogSetting.java](AuditLogSettings/AuditLogSetting.java) | Entity for dynamic audit log configuration |
| [AuditLogSettingServices.java](AuditLogSettings/AuditLogSettingServices.java) | Service for managing settings and resets |
| [AuditLogSettingGetterServices.java](AuditLogSettings/AuditLogSettingGetterServices.java) | Service for retrieving and parsing setting values |
| [AuditLogSettingRepository.java](AuditLogSettings/AuditLogSettingRepository.java) | Repository for settings CRUD operations |
| [AuditLogSettingController.java](AuditLogSettings/AuditLogSettingController.java) | REST endpoints for managing settings |
| [AuditLogSettingInitializer.java](AuditLogSettings/AuditLogSettingInitializer.java) | Initializes default settings on application startup |

## Audit Log Settings Management

### Managing Settings via REST API

Audit log settings can be updated through REST endpoints:

#### Get All Audit Log Settings
```http
GET /api/audit-log-settings
Authorization: Bearer <token>
```

Response:
```json
{
  "status": 200,
  "message": "Audit Log Settings retrieved successfully.",
  "data": [
    {
      "id": "encoded_id",
      "settingKey": "audit.log.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Global audit logging enabled/disabled",
      "active": true,
      "isSystemDefault": true,
      "category": "GENERAL",
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ]
}
```

#### Update Audit Log Setting
```http
PUT /api/audit-log-settings/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "settingValue": "180",
  "description": "Retention period updated to 180 days",
  "active": true
}
```

Response:
```json
{
  "status": 200,
  "message": "Audit Log Setting updated successfully. Updated fields: settingValue, description",
  "data": { /* updated setting */ }
}
```

### Resetting Settings to Defaults

#### Reset General Settings
```http
POST /api/audit-log-settings/reset/general
Authorization: Bearer <token>
```

#### Reset Capture Settings
```http
POST /api/audit-log-settings/reset/capture
Authorization: Bearer <token>
```

#### Reset Value Settings
```http
POST /api/audit-log-settings/reset/values
Authorization: Bearer <token>
```

#### Reset All Settings
```http
POST /api/audit-log-settings/reset/all
Authorization: Bearer <token>
```

All setting changes are automatically logged in the audit log for compliance and tracking purposes.

### Policy Application

Settings are applied to each audit log entry at save time through `AuditLogService.applyAuditPolicies()`:

1. **Capture Policies**: Fields like IP address and user agent are excluded based on settings
2. **Value Exclusion**: Sensitive fields are removed from old/new values JSON
3. **Length Truncation**: Values exceeding `max.value.length` are truncated with "[TRUNCATED]" marker

Example: Applying policies with these settings:
- `audit.log.capture.ip.address=false` → IP address field set to null
- `audit.log.excluded.fields=password,ssn` → Those fields removed from JSON
- `audit.log.max.value.length=1000` → Values > 1000 chars truncated

## Automatic Maintenance

### Audit Log Cleanup Scheduler

The `AuditLogMaintenanceScheduler` automatically manages audit log retention through scheduled tasks:

**Default Schedules:**
- **Daily Cleanup**: Every day at 2:00 AM - Deletes logs older than configured retention days
- **Weekly Cleanup**: Every Sunday at 3:00 AM - Secondary thorough cleanup
- **Fixed-Rate Cleanup** (optional): Every 6 hours - For high-volume scenarios

The scheduler respects the `audit.log.retention.days` setting and logs all cleanup operations.

Example log output:
```
INFO: Starting daily audit log cleanup task...
INFO: Audit log cleanup completed successfully. Deleted 1,234 logs older than 90 days
```

## Settings Flow and Lifecycle

### Configuration Hierarchy

Settings are resolved in this order (highest to lowest priority):

1. **Database Settings** (`AuditLogSetting` table) - Most specific, runtime-configurable
2. **Application Properties** (application.properties) - Fallback defaults
3. **Hardcoded Defaults** - Final fallback in code

### Settings Retrieval Flow

```
User Action
    ↓
@AuditLogAnnotation triggers
    ↓
AuditLoggingAspect captures request/response
    ↓
AuditLogService.logAction() called
    ↓
applyAuditPolicies() invoked
    ↓
AuditLogSettingGetterServices queries database settings
    ↓
Falls back to application.properties if not in database
    ↓
Policies applied to audit log entry
    ↓
Policies applied entries saved to database
```

### Example: Policy Application Flow

Request to update a user with these settings:
```
audit.log.capture.ip.address=false
audit.log.excluded.fields=password,ssn
audit.log.max.value.length=500
```

When the request is logged:

1. **Before Policy Application:**
   ```json
   {
     "ipAddress": "192.168.1.100",
     "oldValues": {"email": "user@example.com", "password": "secret123", "ssn": "123-45-6789"},
     "newValues": "{\"email\": \"user@example.com\", \"password\": \"secret456\", \"ssn\": \"123-45-6789\", \"largeData\": \"...very long string...\"}"
   }
   ```

2. **After Policy Application:**
   ```json
   {
     "ipAddress": null,                    // Excluded by capture policy
     "oldValues": "{\"email\": \"user@example.com\"}",  // password and ssn removed
     "newValues": "{\"email\": \"user@example.com\", \"largeData\": \"...trun... [TRUNCATED]\"}"  // Truncated
   }
   ```

## Examples

### Example 1: Track Role Creation

```java
// RoleService.java
@Transactional
@AuditLogAnnotation(
    action = "CREATE_ROLE",
    entityType = "Role",
    description = "Create a new role"
)
public Role createRole(String name, String displayName, String description) {
    Role role = Role.builder()
        .name(name)
        .displayName(displayName)
        .description(description)
        .build();
    return roleRepository.save(role);
}
```

When called:
```java
roleService.createRole("manager", "Manager", "Manager role");
```

Audit Log Entry:
```json
{
  "username": "admin@example.com",
  "action": "CREATE_ROLE",
  "entityType": "Role",
  "description": "Create a new role",
  "oldValues": {"name": "manager", "displayName": "Manager"},
  "newValues": {"id": 1, "name": "manager", "displayName": "Manager", "active": true},
  "status": "SUCCESS",
  "ipAddress": "192.168.1.100",
  "createdAt": "2025-01-15T10:30:00"
}
```

### Example 2: Track Permission Assignment

```java
// RoleService.java
@Transactional
@AuditLogAnnotation(
    action = "ADD_PERMISSION_TO_ROLE",
    entityType = "Role",
    entityIdParamName = "roleId",
    description = "Add permission to role"
)
public void addPermissionToRole(Long roleId, Long permissionId) {
    Role role = roleRepository.findById(roleId).orElseThrow();
    Permission permission = permissionRepository.findById(permissionId).orElseThrow();
    role.addPermission(permission);
    roleRepository.save(role);
}
```

### Example 3: Query Audit Trail for Specific Entity

```java
// Controller
@GetMapping("/roles/{roleId}/audit-trail")
public ResponseEntity<List<AuditLog>> getRoleAuditTrail(@PathVariable Long roleId) {
    List<AuditLog> trail = auditLogService.getEntityAuditTrail("Role", roleId);
    return ResponseEntity.ok(trail);
}
```

Response shows all changes to Role #123:
```json
[
  {
    "id": 100,
    "username": "admin@example.com",
    "action": "CREATE_ROLE",
    "entityId": 123,
    "createdAt": "2025-01-10T14:00:00"
  },
  {
    "id": 101,
    "username": "admin@example.com",
    "action": "ADD_PERMISSION_TO_ROLE",
    "entityId": 123,
    "createdAt": "2025-01-10T14:05:00"
  },
  {
    "id": 102,
    "username": "manager@example.com",
    "action": "REACTIVATE_ROLE",
    "entityId": 123,
    "createdAt": "2025-01-11T09:30:00"
  }
]
```

## Permissions Required

The audit log endpoints require the following permission:

```java
// AUDIT_LOG resource with READ action
@RequirePermission(action = "READ", resource = "AUDIT_LOG")
public ResponseEntity<Page<AuditLog>> getAllAuditLogs(Pageable pageable) {
    // ...
}
```

Ensure users have `READ_AUDIT_LOG` permission or equivalent role.

## Advanced Usage

### Manually Log Actions

To manually log an action without a method annotation:

```java
@Autowired
private AuditLogService auditLogService;

public void someMethod() {
    AuditLog log = AuditLog.builder()
        .userId(getCurrentUserId())
        .username(getCurrentUsername())
        .action("CUSTOM_ACTION")
        .entityType("CustomEntity")
        .entityId(123L)
        .description("Custom action description")
        .status("SUCCESS")
        .build();

    auditLogService.logActionSync(log);  // or logAction() for async
}
```

### Query Audit Logs Programmatically

```java
@Autowired
private AuditLogService auditLogService;

// Get all changes to a specific entity
List<AuditLog> auditTrail = auditLogService.getEntityAuditTrail("User", userId);

// Get user's actions for a date range
List<AuditLog> userActions = auditLogService.getUserAuditLogsByDateRange(
    userId,
    LocalDateTime.of(2025, 1, 1, 0, 0),
    LocalDateTime.of(2025, 1, 31, 23, 59)
);

// Get all failed actions
Page<AuditLog> failedActions = auditLogService.getActionAuditLogs(
    "DELETE_ROLE",
    PageRequest.of(0, 20)
);
```

## Cleanup and Maintenance

### Automatic Retention

Logs older than 90 days (configurable) can be deleted using the cleanup endpoint:

```bash
curl -X DELETE http://localhost:4450/api/audit-logs/cleanup?retentionDays=90 \
  -H "Authorization: Bearer <token>"
```

### Performance Tips

1. **Index Optimization**: The audit logs table has indexes on:
   - `user_id` - for user action queries
   - `action` - for action type queries
   - `created_at` - for date range queries
   - `entity_type, entity_id` - for entity change tracking

2. **Archive Old Logs**: Periodically backup and delete logs older than retention period

3. **Monitor Database Size**: Audit logs can grow quickly; monitor disk usage

## Initialization and Startup

### Application Startup Process

When the application starts, the `AuditLogSettingInitializer` automatically:

1. **Creates default settings** if the `audit_log_settings` table is empty
2. **Initializes all audit log configuration keys** with default values from application.properties
3. **Marks system defaults** so administrators know which settings are core to the system
4. **Logs initialization actions** to the audit log

### Default Settings Initialized

| Setting Key | Initial Value | Category |
|-------------|---------------|----------|
| `audit.log.enabled` | true | GENERAL |
| `audit.log.retention.days` | 90 | GENERAL |
| `audit.log.capture.ip.address` | true | CAPTURE |
| `audit.log.capture.user.agent` | true | CAPTURE |
| `audit.log.capture.old.values` | true | CAPTURE |
| `audit.log.capture.new.values` | true | CAPTURE |
| `audit.log.excluded.fields` | password,token,secret,apikey,creditcard | VALUES |
| `audit.log.max.value.length` | 2048 | VALUES |

### Customizing Defaults

To change default values on startup:

1. **Edit application.properties:**
   ```properties
   audit.log.retention.days=180
   audit.log.excluded.fields=password,token,secret,apikey,creditcard,ssn
   ```

2. **Delete database records** (or let initializer override them):
   ```sql
   DELETE FROM audit_log_settings WHERE is_system_default = true;
   ```

3. **Restart application** - New defaults will be initialized

## Troubleshooting

### Audit Logs Not Being Created

1. Verify `@EnableAspectJAutoProxy` is set in main application class
2. Verify `@EnableAsync` is set in main application class
3. Check that method has `@AuditLogAnnotation`
4. Verify user is authenticated (non-anonymous)
5. Check logs for AOP aspect errors

### Performance Issues

1. Increase async thread pool: `spring.task.execution.pool.max-size`
2. Reduce log detail (filter sensitive data)
3. Archive old logs periodically
4. Add database indexes for frequently queried fields

### Missing Entity IDs

1. Verify `entityIdParamName` matches actual method parameter name
2. Ensure parameter is of type Long or convertible to Long
3. Use different approach for complex entity lookups

### Settings Not Taking Effect

1. Verify setting exists in `audit_log_settings` table
2. Check `active` flag is true for the setting
3. Verify setting value format matches data type (e.g., "true" for BOOLEAN, "90" for INTEGER)
4. Check logs for `AuditLogSettingGetterServices` errors
5. Confirm `audit.log.enabled=true` in either database or application.properties

### Settings Changes Not Applied

1. Changes are applied at log-save time, not retroactively
2. Restart application if application.properties were changed
3. Settings database takes precedence over application.properties
4. Check if setting is marked as inactive

### Excluded Fields Not Removing Data

1. Verify field names in `audit.log.excluded.fields` match JSON keys (case-insensitive)
2. Check field names are comma-separated with no extra spaces
3. Field removal uses regex pattern matching on JSON
4. Complex nested objects may require more specific field names

### Performance After Settings Changes

1. Reducing captured data improves performance
2. Lowering `max.value.length` reduces database storage
3. Disabling capture policies saves processing time
4. Monitor database size after major policy changes

## Best Practices

1. **Use Consistent Action Names**: Follow naming convention like `CREATE_ROLE`, `UPDATE_USER`
2. **Add Descriptions**: Help audit trail readers understand what happened
3. **Capture Entity IDs**: Always specify `entityIdParamName` for CRUD operations
4. **Regular Cleanup**: Run retention cleanup monthly
5. **Archive Logs**: Archive old logs before deletion for compliance
6. **Monitor Size**: Set up alerts if audit log table grows unexpectedly
7. **Restrict Access**: Only admins/compliance should access audit logs
8. **Test Changes**: Verify audit logging works when adding new @AuditLogAnnotation

## Security Considerations

1. **Sensitive Data**: Passwords and tokens are automatically filtered
2. **Access Control**: Use @RequirePermission to restrict audit log access
3. **Database Access**: Restrict direct database access to audit logs table
4. **Log Retention**: Balance compliance requirements with storage costs
5. **Encryption**: Consider encrypting sensitive fields in database
6. **Audit Trail Tampering**: Implement database triggers to detect tampering
