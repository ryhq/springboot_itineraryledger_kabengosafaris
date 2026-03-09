# Audit Log API Documentation

## Overview
The Audit Log API provides endpoints for retrieving and analyzing system audit logs. Audit logs automatically capture user actions, system events, and data changes for security monitoring, compliance, and troubleshooting purposes.

**Key Features:**
- **Automatic Logging**: System automatically logs user actions based on configured policies
- **Comprehensive Filtering**: Filter by user, action, entity type, status, and more
- **Pagination & Sorting**: Efficient querying of large audit log datasets
- **Privacy Controls**: IP address and user agent capture configurable via settings
- **Field Exclusion**: Sensitive fields can be excluded from old/new values
- **Retention Policies**: Automatic cleanup of old logs based on configured retention period
- **Obfuscated IDs**: All IDs are obfuscated for security
- **Read-Only API**: Audit logs are created automatically; API provides read-only access

**Audit Log Policies:**
- Controlled via Audit Log Settings (see Audit Log Settings API)
- Global enable/disable switch
- IP address capture (on/off)
- User agent capture (on/off)
- Old values capture (on/off)
- New values capture (on/off)
- Field exclusion (comma-separated list)
- Maximum value length
- Retention period (days)

---

## Base URL

```
/api/audit-logs
```

---

## Data Transfer Object (DTO)

### AuditLogDTO (Response)
Returned when retrieving audit logs.

```json
{
  "id": "string (obfuscated audit log ID)",
  "userId": "string (obfuscated user ID)",
  "username": "string (username of user who performed action)",
  "action": "string (action performed, e.g., CREATE, UPDATE, DELETE, LOGIN)",
  "entityType": "string (type of entity affected, e.g., USER, ROLE, PERMISSION)",
  "entityId": "string (obfuscated ID of affected entity)",
  "description": "string (human-readable description of action)",
  "oldValues": "string (JSON of previous values before change)",
  "newValues": "string (JSON of new values after change)",
  "ipAddress": "string (IP address of user, if captured)",
  "userAgent": "string (browser/client user agent, if captured)",
  "createdAt": "datetime (ISO 8601 - when action occurred)",
  "status": "string (SUCCESS, FAILURE, ERROR, etc.)",
  "errorMessage": "string (error details if status is FAILURE/ERROR)"
}
```

**Example:**
```json
{
  "id": "encoded_log_abc123",
  "userId": "encoded_user_xyz789",
  "username": "john.doe",
  "action": "UPDATE_USER",
  "entityType": "USER",
  "entityId": "encoded_user_def456",
  "description": "Updated user profile: email changed",
  "oldValues": "{\"email\":\"old@example.com\",\"phoneNumber\":\"+1234567890\"}",
  "newValues": "{\"email\":\"new@example.com\",\"phoneNumber\":\"+1234567890\"}",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
  "createdAt": "2025-12-30T14:30:00",
  "status": "SUCCESS",
  "errorMessage": null
}
```

**Field Notes:**
- **Null Fields**: Fields with null values are excluded from JSON response (`@JsonInclude(NON_NULL)`)
- **ID Obfuscation**: All IDs (id, userId, entityId) are obfuscated for security
- **Old/New Values**: May be null based on capture policies or action type
- **IP Address**: May be null if capture disabled in settings
- **User Agent**: May be null if capture disabled in settings
- **Error Message**: Only present when status is FAILURE or ERROR

---

## Endpoints

### 1. Get All Audit Logs
**GET** `/api/audit-logs`

Retrieves audit logs with advanced filtering, pagination, and sorting capabilities.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 0 | Page number (0-based) |
| `size` | Integer | No | 10 | Number of items per page |
| `userId` | String | No | - | Filter by user ID (obfuscated) |
| `username` | String | No | - | Filter by username (partial match, case-insensitive) |
| `action` | String | No | - | Filter by action (partial match, case-insensitive) |
| `entityType` | String | No | - | Filter by entity type (partial match, case-insensitive) |
| `entityId` | String | No | - | Filter by entity ID (obfuscated) |
| `description` | String | No | - | Filter by description (partial match, case-insensitive) |
| `ipAddress` | String | No | - | Filter by IP address (exact match) |
| `userAgent` | String | No | - | Filter by user agent (partial match, case-insensitive) |
| `status` | String | No | - | Filter by status (partial match, case-insensitive) |
| `errorMessage` | String | No | - | Filter by error message (partial match, case-insensitive) |
| `sortDirection` | String | No | desc | Sort direction: "asc" or "desc" |

**Request Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved audit logs.",
  "data": {
    "auditLogs": [
      {
        "id": "encoded_log_abc123",
        "userId": "encoded_user_xyz789",
        "username": "john.doe",
        "action": "UPDATE_USER",
        "entityType": "USER",
        "entityId": "encoded_user_def456",
        "description": "Updated user profile: email changed",
        "oldValues": "{\"email\":\"old@example.com\"}",
        "newValues": "{\"email\":\"new@example.com\"}",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2025-12-30T14:30:00",
        "status": "SUCCESS"
      },
      {
        "id": "encoded_log_def456",
        "userId": "encoded_user_xyz789",
        "username": "john.doe",
        "action": "CREATE_ROLE",
        "entityType": "ROLE",
        "entityId": "encoded_role_ghi789",
        "description": "Created new role: Manager",
        "newValues": "{\"name\":\"MANAGER\",\"displayName\":\"Manager\"}",
        "ipAddress": "192.168.1.100",
        "createdAt": "2025-12-30T14:25:00",
        "status": "SUCCESS"
      }
    ],
    "currentPage": 0,
    "totalItems": 1523,
    "totalPages": 153
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `auditLogs` | Array | List of audit log objects |
| `auditLogs[].id` | String | Obfuscated audit log ID |
| `auditLogs[].userId` | String | Obfuscated user ID |
| `auditLogs[].username` | String | Username of user who performed action |
| `auditLogs[].action` | String | Action performed |
| `auditLogs[].entityType` | String | Type of entity affected |
| `auditLogs[].entityId` | String | Obfuscated entity ID |
| `auditLogs[].description` | String | Human-readable description |
| `auditLogs[].oldValues` | String | JSON of previous values |
| `auditLogs[].newValues` | String | JSON of new values |
| `auditLogs[].ipAddress` | String | IP address (if captured) |
| `auditLogs[].userAgent` | String | User agent (if captured) |
| `auditLogs[].createdAt` | DateTime | Timestamp when action occurred |
| `auditLogs[].status` | String | Action status |
| `auditLogs[].errorMessage` | String | Error details (if applicable) |
| `currentPage` | Integer | Current page number (0-based) |
| `totalItems` | Long | Total number of audit logs matching filters |
| `totalPages` | Integer | Total number of pages |

**Error Responses:**

#### Invalid User ID Format
**Status Code:** `400 Bad Request`
```json
"Invalid user ID format"
```

#### Invalid Entity ID Format
**Status Code:** `400 Bad Request`
```json
"Invalid entity ID format"
```

#### Invalid Page Number
**Status Code:** `400 Bad Request`
```json
"Page number cannot be negative"
```

#### Invalid Page Size
**Status Code:** `400 Bad Request`
```json
"Page size must be greater than 0"
```

**Example Requests:**

#### Get all audit logs (default pagination)
```bash
curl -X GET "https://api.example.com/api/audit-logs" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by user
```bash
curl -X GET "https://api.example.com/api/audit-logs?userId=encoded_user_id&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by username and action
```bash
curl -X GET "https://api.example.com/api/audit-logs?username=john&action=UPDATE&sortDirection=asc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by entity type "USER" and status "SUCCESS"
```bash
curl -X GET "https://api.example.com/api/audit-logs?entityType=USER&status=SUCCESS&page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by IP address
```bash
curl -X GET "https://api.example.com/api/audit-logs?ipAddress=192.168.1.100" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Search by description containing "email"
```bash
curl -X GET "https://api.example.com/api/audit-logs?description=email&size=100" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by specific entity
```bash
curl -X GET "https://api.example.com/api/audit-logs?entityType=ROLE&entityId=encoded_role_id" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by error messages
```bash
curl -X GET "https://api.example.com/api/audit-logs?status=FAILURE&errorMessage=validation" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Use Cases:**

1. **Security Monitoring**: Track all actions by a specific user
2. **Compliance Auditing**: Generate reports of all changes to sensitive entities
3. **Troubleshooting**: Find error logs for a specific entity or action type
4. **Access Tracking**: Monitor login attempts from specific IP addresses
5. **Change History**: View all modifications to a specific record
6. **User Activity**: Analyze user behavior patterns
7. **Incident Investigation**: Search for suspicious activities or failed operations

**Required Permission:**
- `PERM_READ_AUDIT_LOG`

---

### 2. Get Single Audit Log
**GET** `/api/audit-logs/{id}`

Retrieves detailed information about a specific audit log entry by its obfuscated ID.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated audit log ID |

**Request Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved audit log.",
  "data": {
    "id": "encoded_log_abc123",
    "userId": "encoded_user_xyz789",
    "username": "john.doe",
    "action": "UPDATE_USER",
    "entityType": "USER",
    "entityId": "encoded_user_def456",
    "description": "Updated user profile: email and phone number changed",
    "oldValues": "{\"email\":\"old@example.com\",\"phoneNumber\":\"+1234567890\",\"firstName\":\"John\",\"lastName\":\"Doe\"}",
    "newValues": "{\"email\":\"new@example.com\",\"phoneNumber\":\"+0987654321\",\"firstName\":\"John\",\"lastName\":\"Doe\"}",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "createdAt": "2025-12-30T14:30:00",
    "status": "SUCCESS",
    "errorMessage": null
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated audit log ID |
| `userId` | String | Obfuscated user ID |
| `username` | String | Username of user who performed action |
| `action` | String | Action performed |
| `entityType` | String | Type of entity affected |
| `entityId` | String | Obfuscated entity ID |
| `description` | String | Human-readable description |
| `oldValues` | String | JSON of previous values |
| `newValues` | String | JSON of new values |
| `ipAddress` | String | IP address (if captured) |
| `userAgent` | String | User agent (if captured) |
| `createdAt` | DateTime | Timestamp when action occurred |
| `status` | String | Action status |
| `errorMessage` | String | Error details (if applicable) |

**Error Responses:**

#### Audit Log Not Found
**Status Code:** `404 Not Found`
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Audit log not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Internal Server Error
**Status Code:** `500 Internal Server Error`
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to get audit log",
  "errorCode": "GET_AUDIT_LOG_FAILED"
}
```

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/audit-logs/encoded_log_abc123" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Use Cases:**

1. **Detailed Investigation**: View complete details of a specific event
2. **Change Analysis**: Compare old and new values to understand what changed
3. **Error Investigation**: Examine error messages for failed operations
4. **Audit Trail**: Verify specific action details for compliance
5. **Forensic Analysis**: Deep dive into suspicious activities

**Required Permission:**
- `PERM_READ_AUDIT_LOG`

---

## Common Action Types

Audit logs capture various types of actions across the system:

### User Actions
- `CREATE_USER` - New user account created
- `READ_USER` - User record accessed/viewed
- `UPDATE_USER` - User record modified
- `DELETE_USER` - User record deleted
- `LOGIN_SUCCESS` - Successful login
- `LOGIN_FAILURE` - Failed login attempt
- `LOGOUT` - User logout
- `PASSWORD_CHANGE` - Password changed
- `ACCOUNT_ACTIVATION` - Account activated
- `ACCOUNT_DEACTIVATION` - Account deactivated

### Role Actions
- `CREATE_ROLE` - New role created
- `READ_ROLE` - Role accessed/viewed
- `UPDATE_ROLE` - Role modified
- `DELETE_ROLE` - Role deleted
- `ASSIGN_ROLE` - Role assigned to user
- `REVOKE_ROLE` - Role revoked from user
- `UPDATE_ROLE_PERMISSIONS` - Role permissions modified

### Permission Actions
- `READ_PERMISSION` - Permission accessed/viewed
- `UPDATE_PERMISSION` - Permission modified
- `TOGGLE_PERMISSION` - Permission active status toggled

### Email Actions
- `CREATE_EMAIL_ACCOUNT` - Email account created
- `UPDATE_EMAIL_ACCOUNT` - Email account modified
- `DELETE_EMAIL_ACCOUNT` - Email account deleted
- `SEND_EMAIL` - Email sent
- `EMAIL_SEND_FAILURE` - Email sending failed

### Settings Actions
- `UPDATE_SECURITY_SETTINGS` - Security settings modified
- `UPDATE_AUDIT_LOG_SETTINGS` - Audit log settings modified

---

## Common Entity Types

Entities that are tracked in audit logs:

- `USER` - User accounts
- `ROLE` - User roles
- `PERMISSION` - System permissions
- `EMAIL_ACCOUNT` - Email account configurations
- `EMAIL_ACCOUNT_SIGNATURE` - Email signatures
- `EMAIL_EVENT` - Email event definitions
- `EMAIL_TEMPLATE` - Email templates
- `SECURITY_SETTING` - Security configuration
- `AUDIT_LOG_SETTING` - Audit log configuration
- `AUDIT_LOG` - Audit log entries

---

## Status Values

Audit logs track the outcome of actions:

- `SUCCESS` - Action completed successfully
- `FAILURE` - Action failed (business logic validation)
- `ERROR` - Action failed (system/technical error)
- `PARTIAL_SUCCESS` - Action partially completed
- `PENDING` - Action initiated but not completed
- `CANCELLED` - Action cancelled by user or system

---

## Filtering Strategies

### By User Activity
Track all actions by a specific user:
```
GET /api/audit-logs?userId=encoded_user_id
GET /api/audit-logs?username=john.doe
```

### By Action Type
Find all instances of a specific action:
```
GET /api/audit-logs?action=UPDATE_USER
GET /api/audit-logs?action=DELETE
```

### By Entity
Track changes to a specific entity:
```
GET /api/audit-logs?entityType=USER&entityId=encoded_user_id
GET /api/audit-logs?entityType=ROLE
```

### By Status
Find failed operations:
```
GET /api/audit-logs?status=FAILURE
GET /api/audit-logs?status=ERROR&errorMessage=validation
```

### By IP Address
Track actions from specific locations:
```
GET /api/audit-logs?ipAddress=192.168.1.100
```

### By Time Period (using pagination)
Get recent logs:
```
GET /api/audit-logs?page=0&size=100&sortDirection=desc
```

### Combined Filters
Complex queries combining multiple filters:
```
GET /api/audit-logs?username=john&action=UPDATE&entityType=USER&status=SUCCESS
```

---

## Error Codes Reference

| Code | Status | Description |
|------|--------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Audit log with specified ID not found |
| `GET_AUDIT_LOG_FAILED` | 500 | Internal error occurred while retrieving audit log |
| `VALIDATION_ERROR` | 400 | Request validation failed (invalid pagination or ID format) |

---

## Important Notes

### Read-Only Access
1. **No Create/Update/Delete**: Audit logs are created automatically by the system
2. **API is Read-Only**: These endpoints provide read-only access for querying and analysis
3. **Automatic Logging**: All logged-in user actions are automatically captured based on policies
4. **System Events**: Some logs are created by system processes (e.g., scheduled tasks)

### Privacy & Compliance
1. **Configurable Capture**: IP addresses and user agents can be disabled via settings
2. **Field Exclusion**: Sensitive fields (e.g., passwords) can be excluded from old/new values
3. **Data Retention**: Logs are automatically deleted after configured retention period
4. **Obfuscated IDs**: All IDs are obfuscated to prevent enumeration attacks
5. **Access Control**: Requires `PERM_READ_AUDIT_LOG` permission

### Performance Considerations
1. **Large Datasets**: Use pagination for better performance with large result sets
2. **Filter Early**: Apply filters to reduce dataset size before pagination
3. **Indexed Fields**: userId, action, entityType, and createdAt are indexed for fast queries
4. **Composite Indexes**: entityType + entityId indexed together for entity tracking

### Data Capture Policies
1. **Global Enable/Disable**: Audit logging can be turned off completely
2. **IP Address Capture**: Can be disabled for privacy (set to null)
3. **User Agent Capture**: Can be disabled for privacy (set to null)
4. **Old Values Capture**: Can be disabled to save storage
5. **New Values Capture**: Can be disabled to save storage
6. **Field Exclusion**: Specific fields can be excluded from old/new values JSON
7. **Value Truncation**: Old/new values truncated if exceed max length setting

### Best Practices
1. **Regular Monitoring**: Review audit logs regularly for security monitoring
2. **Alert on Failures**: Set up alerts for failed operations or suspicious patterns
3. **Retention Policy**: Configure appropriate retention period for compliance needs
4. **Filter Effectively**: Use specific filters to narrow down results efficiently
5. **Export for Analysis**: Use pagination to export large datasets for analysis
6. **Protect Access**: Limit `PERM_READ_AUDIT_LOG` permission to authorized users only
7. **Archive Old Logs**: Export old logs before automatic deletion if needed

---

## Common Use Cases

### 1. Security Monitoring
**Scenario**: Monitor failed login attempts from a specific IP
```bash
GET /api/audit-logs?action=LOGIN_FAILURE&ipAddress=192.168.1.100&size=50
```

### 2. Compliance Auditing
**Scenario**: Generate report of all user modifications in last 30 days
```bash
GET /api/audit-logs?action=UPDATE_USER&page=0&size=1000&sortDirection=desc
```

### 3. Change History Tracking
**Scenario**: View all changes to a specific user account
```bash
GET /api/audit-logs?entityType=USER&entityId=encoded_user_id&sortDirection=asc
```

### 4. User Activity Report
**Scenario**: Track all actions performed by a specific user
```bash
GET /api/audit-logs?userId=encoded_user_id&page=0&size=100
```

### 5. Error Investigation
**Scenario**: Find all failed operations with specific error
```bash
GET /api/audit-logs?status=ERROR&errorMessage=database&size=50
```

### 6. Permission Changes Audit
**Scenario**: Track all permission-related changes
```bash
GET /api/audit-logs?entityType=PERMISSION&action=UPDATE&sortDirection=desc
```

### 7. Role Assignment Tracking
**Scenario**: Monitor role assignments and revocations
```bash
GET /api/audit-logs?action=ASSIGN_ROLE&page=0&size=100
GET /api/audit-logs?action=REVOKE_ROLE&page=0&size=100
```

### 8. Email Activity Monitoring
**Scenario**: Track email sending and failures
```bash
GET /api/audit-logs?entityType=EMAIL&status=FAILURE&size=50
```

---

## Integration Examples

### Example 1: Audit Dashboard
```javascript
// Fetch recent audit logs for dashboard
async function fetchRecentAuditLogs() {
  const response = await fetch('/api/audit-logs?page=0&size=10&sortDirection=desc', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data.data.auditLogs;
}
```

### Example 2: User Activity Timeline
```javascript
// Get all actions by specific user
async function getUserActivityTimeline(userId) {
  const response = await fetch(`/api/audit-logs?userId=${userId}&size=100&sortDirection=asc`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data.data.auditLogs;
}
```

### Example 3: Security Alert System
```javascript
// Check for failed login attempts
async function checkFailedLogins(ipAddress) {
  const response = await fetch(`/api/audit-logs?action=LOGIN_FAILURE&ipAddress=${ipAddress}&size=10`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  const failedAttempts = data.data.totalItems;

  if (failedAttempts > 5) {
    alert(`Warning: ${failedAttempts} failed login attempts from ${ipAddress}`);
  }
}
```

### Example 4: Change History Component
```javascript
// Get detailed change history for an entity
async function getEntityChangeHistory(entityType, entityId) {
  const response = await fetch(
    `/api/audit-logs?entityType=${entityType}&entityId=${entityId}&sortDirection=desc`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  const data = await response.json();

  // Display old vs new values
  return data.data.auditLogs.map(log => ({
    timestamp: log.createdAt,
    user: log.username,
    action: log.action,
    changes: compareValues(log.oldValues, log.newValues)
  }));
}

function compareValues(oldValues, newValues) {
  const old = JSON.parse(oldValues || '{}');
  const newVal = JSON.parse(newValues || '{}');

  const changes = {};
  Object.keys(newVal).forEach(key => {
    if (old[key] !== newVal[key]) {
      changes[key] = {
        old: old[key],
        new: newVal[key]
      };
    }
  });
  return changes;
}
```

---

## Related APIs

- **Audit Log Settings API**: Configure audit logging policies and retention
- **User API**: Manage users whose actions are logged
- **Role API**: Manage roles and permissions for audit log access
- **Security Settings API**: Configure security policies that affect logging

---

**End of Documentation**
