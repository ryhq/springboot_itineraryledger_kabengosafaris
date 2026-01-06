# Permission API Documentation

This document describes the REST API endpoints for managing permissions in the system.

## Table of Contents

1. [Get All Permissions (with Pagination & Filtering)](#1-get-all-permissions-with-pagination--filtering)
2. [Get Single Permission](#2-get-single-permission)
3. [Toggle Permission Active Status](#3-toggle-permission-active-status)
4. [Error Codes](#error-codes)
5. [Data Models](#data-models)

---

## 1. Get All Permissions (with Pagination & Filtering)

Retrieve all permissions with optional filtering, pagination, and sorting capabilities.

### Endpoint

```
GET /api/permissions
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 0 | Page number (0-based) |
| `size` | Integer | No | 10 | Number of items per page |
| `name` | String | No | - | Filter by permission name (partial match, case-insensitive) |
| `entity` | String | No | - | Filter by entity name (partial match, case-insensitive) |
| `action` | Enum | No | - | Filter by permission action (CREATE, READ, UPDATE, DELETE, etc.) |
| `active` | Boolean | No | - | Filter by active status (true/false) |
| `sortDir` | String | No | desc | Sort direction: "asc" or "desc" |

### Request Headers

```
Authorization: Bearer {token}
```

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved permissions.",
  "data": {
    "permissions": [
      {
        "id": "encoded_permission_id_1",
        "name": "CREATE_USER",
        "description": "Allows creating new User records",
        "action": "CREATE",
        "actionDisplayName": "Create",
        "entity": "USER",
        "active": true,
        "createdAt": "2025-12-29T10:00:00",
        "updatedAt": "2025-12-29T10:00:00"
      },
      {
        "id": "encoded_permission_id_2",
        "name": "READ_USER",
        "description": "Allows viewing and reading User records",
        "action": "READ",
        "actionDisplayName": "Read",
        "entity": "USER",
        "active": true,
        "createdAt": "2025-12-29T10:00:00",
        "updatedAt": "2025-12-29T10:00:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 36,
    "totalPages": 4
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `permissions` | Array | List of permission objects |
| `permissions[].id` | String | Obfuscated permission ID |
| `permissions[].name` | String | Permission name (e.g., "CREATE_USER") |
| `permissions[].description` | String | Human-readable description |
| `permissions[].action` | String | Permission action enum value |
| `permissions[].actionDisplayName` | String | Human-readable action name |
| `permissions[].entity` | String | Entity name (e.g., "USER", "ROLE") |
| `permissions[].active` | Boolean | Whether the permission is active |
| `permissions[].createdAt` | DateTime | Timestamp when created |
| `permissions[].updatedAt` | DateTime | Timestamp when last updated |
| `currentPage` | Integer | Current page number (0-based) |
| `totalItems` | Long | Total number of permissions matching filters |
| `totalPages` | Integer | Total number of pages |

### Error Responses

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

### Example Requests

#### Get all permissions (default pagination)

```bash
curl -X GET "https://api.example.com/api/permissions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by entity "USER" and active status

```bash
curl -X GET "https://api.example.com/api/permissions?entity=USER&active=true&page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter by action "CREATE"

```bash
curl -X GET "https://api.example.com/api/permissions?action=CREATE&sortDir=asc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Search by permission name containing "EMAIL"

```bash
curl -X GET "https://api.example.com/api/permissions?name=EMAIL&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Use Cases

1. **Permission Management UI**: Display all permissions with filtering and pagination
2. **Role Management**: Show available permissions when assigning to roles
3. **Audit**: Review all permissions in the system
4. **Search**: Find specific permissions by name or entity

### Required Permission

- `PERM_READ_PERMISSION`

---

## 2. Get Single Permission

Retrieve detailed information about a specific permission by its ID.

### Endpoint

```
GET /api/permissions/{id}
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated permission ID |

### Request Headers

```
Authorization: Bearer {token}
```

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved permission.",
  "data": {
    "id": "encoded_permission_id",
    "name": "UPDATE_ROLE",
    "description": "Allows editing and updating Role records",
    "action": "UPDATE",
    "actionDisplayName": "Update",
    "entity": "ROLE",
    "active": true,
    "createdAt": "2025-12-29T10:00:00",
    "updatedAt": "2025-12-29T10:00:00"
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated permission ID |
| `name` | String | Permission name |
| `description` | String | Human-readable description |
| `action` | String | Permission action enum value |
| `actionDisplayName` | String | Human-readable action name |
| `entity` | String | Entity name |
| `active` | Boolean | Whether the permission is active |
| `createdAt` | DateTime | Timestamp when created |
| `updatedAt` | DateTime | Timestamp when last updated |

### Error Responses

#### Permission Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Permission not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Internal Server Error

**Status Code:** `500 Internal Server Error`

```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to get permission",
  "errorCode": "GET_PERMISSION_FAILED"
}
```

### Example Request

```bash
curl -X GET "https://api.example.com/api/permissions/encoded_permission_id" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Use Cases

1. **Permission Details**: View full details of a specific permission
2. **Audit**: Check permission configuration
3. **Verification**: Confirm permission status before operations

### Required Permission

- `PERM_READ_PERMISSION`

---

## 3. Toggle Permission Active Status

Toggle a permission between active (true) and inactive (false) status. When a permission is inactive, users with that permission assigned through roles will not be able to access protected endpoints that require it.

### Endpoint

```
PATCH /api/permissions/{id}/toggle-active
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated permission ID |

### Request Headers

```
Authorization: Bearer {token}
```

### Request Body

**None** - This endpoint requires no request body.

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully toggled permission active status to false",
  "data": {
    "id": "encoded_permission_id",
    "name": "DELETE_EMAIL_ACCOUNT",
    "description": "Allows deleting Email Account records",
    "action": "DELETE",
    "actionDisplayName": "Delete",
    "entity": "EMAIL_ACCOUNT",
    "active": false,
    "createdAt": "2025-12-29T10:00:00",
    "updatedAt": "2025-12-29T14:30:00"
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated permission ID |
| `name` | String | Permission name |
| `description` | String | Human-readable description |
| `action` | String | Permission action enum value |
| `actionDisplayName` | String | Human-readable action name |
| `entity` | String | Entity name |
| `active` | Boolean | **New active status** after toggle |
| `createdAt` | DateTime | Timestamp when created |
| `updatedAt` | DateTime | Timestamp when last updated (reflects toggle time) |

### Behavior

- If permission is currently **active (true)**, it will be set to **inactive (false)**
- If permission is currently **inactive (false)**, it will be set to **active (true)**
- The response includes the permission with its **new status**
- The `updatedAt` timestamp is automatically updated

### Impact of Toggling

**When a permission is set to inactive (false):**
- Users with roles containing this permission will **lose access** to endpoints protected by it
- The permission remains in the database and assigned to roles
- This provides a global disable switch without removing permission assignments
- Example: Setting `DELETE_USER` to inactive prevents all users from deleting users, regardless of their roles

**When a permission is reactivated (true):**
- Users with roles containing this permission will **regain access** to protected endpoints
- All existing role assignments remain intact

### Error Responses

#### Permission Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Permission not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Internal Server Error

**Status Code:** `500 Internal Server Error`

```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to toggle permission active status",
  "errorCode": "TOGGLE_PERMISSION_FAILED"
}
```

### Example Requests

#### Deactivate a permission

```bash
curl -X PATCH "https://api.example.com/api/permissions/encoded_permission_id/toggle-active" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Result**: If permission was active, it becomes inactive. If it was inactive, it becomes active.

### Use Cases

1. **Emergency Disable**: Quickly disable a permission system-wide without modifying roles
2. **Maintenance Mode**: Temporarily disable certain operations (e.g., DELETE permissions during maintenance)
3. **Feature Flags**: Enable/disable access to features without code changes
4. **Security**: Immediately revoke a permission across all users if a vulnerability is found
5. **Testing**: Toggle permissions on/off during testing without affecting role configurations

### Important Notes

- **Global Impact**: Toggling affects **all users** who have this permission through any role
- **No Role Modification**: This does **not** remove the permission from roles, only changes its active status
- **Immediate Effect**: Users will lose/gain access immediately (on next authentication check)
- **Transactional**: The operation is atomic - either succeeds completely or fails with no changes
- **Audit Trail**: The `updatedAt` timestamp is automatically updated for audit purposes

### Required Permission

- `PERM_UPDATE_PERMISSION`

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Permission with the specified ID was not found |
| `GET_PERMISSION_FAILED` | 500 | Internal error occurred while retrieving permission |
| `TOGGLE_PERMISSION_FAILED` | 500 | Internal error occurred while toggling permission status |
| `VALIDATION_ERROR` | 400 | Request validation failed (invalid pagination parameters) |

---

## Data Models

### PermissionDTO

Response model for permission data transfer.

```json
{
  "id": "string",                      // Obfuscated permission ID
  "name": "string",                    // Permission name (e.g., "CREATE_USER")
  "description": "string",             // Human-readable description
  "action": "string",                  // Permission action enum (CREATE, READ, UPDATE, DELETE, etc.)
  "actionDisplayName": "string",       // Human-readable action name (e.g., "Create", "Read")
  "entity": "string",                  // Entity name (e.g., "USER", "ROLE")
  "active": "boolean",                 // Whether permission is active
  "createdAt": "datetime",             // Timestamp when created
  "updatedAt": "datetime"              // Timestamp when last updated
}
```

### PermissionAction Enum

Available permission actions:

| Action | Code | Display Name | Description |
|--------|------|--------------|-------------|
| `CREATE` | create | Create | Create new records |
| `READ` | read | Read | View and read records |
| `UPDATE` | update | Update | Edit and update records |
| `DELETE` | delete | Delete | Delete records |
| `EXECUTE` | execute | Execute | Execute actions and workflows |
| `SUBMIT` | submit | Submit | Submit documents for approval |
| `AMEND` | amend | Amend | Amend submitted documents |
| `CANCEL` | cancel | Cancel | Cancel submitted documents |
| `EXPORT` | export | Export | Export data to external formats |
| `PRINT` | print | Print | Print documents |

---

## Entities with Permissions

The following entities have CRUD permissions in the system:

- `USER` - User management
- `ROLE` - Role management
- `PERMISSION` - Permission management
- `EMAIL_ACCOUNT` - Email account management
- `EMAIL_ACCOUNT_SIGNATURE` - Email signature management
- `EMAIL_EVENT` - Email event management
- `EMAIL_TEMPLATE` - Email template management
- `SECURITY_SETTING` - Security settings management
- `AUDIT_LOG_SETTING` - Audit log settings management

Each entity has four standard permissions:
- `CREATE_{ENTITY}` - Create new records
- `READ_{ENTITY}` - View and read records
- `UPDATE_{ENTITY}` - Edit and update records
- `DELETE_{ENTITY}` - Delete records

Example: For USER entity, the permissions are:
- `CREATE_USER`
- `READ_USER`
- `UPDATE_USER`
- `DELETE_USER`

---

## Security Notes

- All endpoints require authentication (Bearer token)
- Read operations require `PERM_READ_PERMISSION`
- Toggle operations require `PERM_UPDATE_PERMISSION`
- Permissions cannot be created or deleted via API (managed by system initialization)
- Permission IDs are obfuscated for security
- Never expose internal permission IDs to untrusted clients

---

## Best Practices

1. **Filtering**: Use filters to reduce payload size and improve performance
2. **Pagination**: Always use pagination for large datasets
3. **Caching**: Consider caching permission lists as they change infrequently
4. **Active Status**: Use toggle instead of delete to preserve permission history
5. **Search**: Use partial name/entity matching for user-friendly search
6. **Sorting**: Sort by creation date (default) or implement custom sorting as needed

---

## Changelog

### Version 1.0.0 (2025-12-29)

- Initial release of Permission API
- Added GET /api/permissions - List permissions with filtering and pagination
- Added GET /api/permissions/{id} - Get single permission
- Added PATCH /api/permissions/{id}/toggle-active - Toggle permission active status
- Support for filtering by name, entity, action, and active status
- Pagination and sorting capabilities

---

## Support

For issues or questions, please contact the API support team or file an issue in the project repository.
