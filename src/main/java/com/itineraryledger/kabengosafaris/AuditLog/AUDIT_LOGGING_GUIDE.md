# Audit Logging Guide

## Overview

The audit logging system captures every action performed in the application, storing comprehensive logs in the database. This enables full traceability of user actions, entity changes, and system operations.

## Architecture

### Components

1. **AuditLog Entity** - JPA entity representing an audit log record
2. **AuditLogRepository** - Data access layer for audit logs
3. **AuditLogService** - Business logic for managing and querying audit logs
4. **AuditLogAnnotation** - Custom annotation to mark methods for logging
5. **AuditLoggingAspect** - AOP aspect that intercepts annotated methods
6. **AuditLogController** - REST endpoints for retrieving audit logs

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

### Application Properties

```properties
# Enable async processing for audit logs (non-blocking)
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100

# Audit log retention in days (logs older than this are candidates for cleanup)
audit.log.retention.days=90

# Enable/disable audit logging globally
audit.log.enabled=true
```

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
