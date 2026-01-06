# Role Management API Documentation

## Overview

The Role Management API provides endpoints for managing roles in the system. Roles are collections of permissions that define what users with that role can do. This API follows RESTful principles and uses obfuscated IDs for security.

**Base URL:** `/api/roles`

**Authentication:** Required for all endpoints

---

## Table of Contents

1. [Create Role](#1-create-role)
2. [Get All Roles (with Pagination & Filtering)](#2-get-all-roles-with-pagination--filtering)
3. [Get Single Role](#3-get-single-role)
4. [Update Role](#4-update-role)
5. [Delete Single Role](#5-delete-single-role)
6. [Delete Multiple Roles (Batch)](#6-delete-multiple-roles-batch)
7. [Get Entities for Role](#7-get-entities-for-role)
8. [Get Entity Permissions for Role](#8-get-entity-permissions-for-role)
9. [Update Role Permissions for Entity](#9-update-role-permissions-for-entity)
10. [Reset Role Permissions to System Defaults](#10-reset-role-permissions-to-system-defaults)
11. [Error Codes](#error-codes)
12. [Data Models](#data-models)

---

## 1. Create Role

Create a new role in the system.

### Endpoint

```
POST /api/roles
```

### Request Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

```json
{
  "name": "booking_manager",           // Optional: Auto-generated from displayName if not provided
  "displayName": "Booking Manager",    // Required
  "description": "Manages all booking operations and customer interactions",  // Optional
  "active": true                       // Required
}
```

### Request Body Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | No | Unique role identifier (lowercase with underscores). Auto-generated from displayName if not provided. |
| `displayName` | String | Yes | Human-readable role name |
| `description` | String | No | Detailed description of what this role is for |
| `active` | Boolean | Yes | Whether this role is active/enabled |

### Success Response

**Status Code:** `201 Created`

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Role created successfully",
  "data": {
    "id": "encoded_role_id",
    "name": "booking_manager",
    "displayName": "Booking Manager",
    "description": "Manages all booking operations and customer interactions",
    "active": true,
    "isSystemRole": false,
    "createdAt": "2025-12-25T10:30:00",
    "updatedAt": "2025-12-25T10:30:00"
  }
}
```

### Error Responses

#### Duplicate Role Name

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Role name already exists",
  "errorCode": "DUPLICATE_ROLE_NAME"
}
```

#### Validation Error

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Display name is required",
  "errorCode": "VALIDATION_ERROR"
}
```

### Example cURL Request

```bash
curl -X POST https://api.example.com/api/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "displayName": "Booking Manager",
    "description": "Manages all booking operations",
    "active": true
  }'
```

---

## 2. Get All Roles (with Pagination & Filtering)

Retrieve a paginated list of roles with optional filtering and sorting.

### Endpoint

```
GET /api/roles
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 0 | Page number (0-based) |
| `size` | Integer | No | 10 | Number of items per page |
| `name` | String | No | - | Filter by role name (case-insensitive partial match) |
| `displayName` | String | No | - | Filter by display name (case-insensitive partial match) |
| `active` | Boolean | No | - | Filter by active status (true/false) |
| `isSystemRole` | Boolean | No | - | Filter by system role status (true/false) |
| `sortDir` | String | No | desc | Sort direction: "asc" or "desc" (sorts by createdAt) |

**Note:** The `description` field is included in the response data but is not available as a filter parameter due to database type constraints (CLOB field).

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved roles.",
  "data": {
    "roles": [
      {
        "id": "encoded_role_id_1",
        "name": "admin",
        "displayName": "Administrator",
        "description": "Full system access - create, read, update, delete all resources",
        "active": true,
        "isSystemRole": true,
        "createdAt": "2025-12-01T10:00:00",
        "updatedAt": "2025-12-01T10:00:00"
      },
      {
        "id": "encoded_role_id_2",
        "name": "booking_manager",
        "displayName": "Booking Manager",
        "description": "Manages all booking operations",
        "active": true,
        "isSystemRole": false,
        "createdAt": "2025-12-25T10:30:00",
        "updatedAt": "2025-12-25T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2
  }
}
```

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

#### Basic Request (Default Pagination)

```bash
curl -X GET "https://api.example.com/api/roles" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter Active Roles Only

```bash
curl -X GET "https://api.example.com/api/roles?active=true&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Filter System Roles

```bash
curl -X GET "https://api.example.com/api/roles?isSystemRole=true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Search by Name with Sorting

```bash
curl -X GET "https://api.example.com/api/roles?name=manager&sortDir=asc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Combined Filters

```bash
curl -X GET "https://api.example.com/api/roles?active=true&isSystemRole=false&displayName=booking&page=0&size=10&sortDir=desc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 3. Get Single Role

Retrieve a specific role by its obfuscated ID.

### Endpoint

```
GET /api/roles/{id}
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated role ID |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Successfully retrieved role.",
  "data": {
    "id": "encoded_role_id",
    "name": "booking_manager",
    "displayName": "Booking Manager",
    "description": "Manages all booking operations",
    "active": true,
    "isSystemRole": false,
    "createdAt": "2025-12-25T10:30:00",
    "updatedAt": "2025-12-25T10:30:00"
  }
}
```

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "ROLE_NOT_FOUND"
}
```

### Example cURL Request

```bash
curl -X GET "https://api.example.com/api/roles/encoded_role_id" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 4. Update Role

Update an existing role with partial updates. Only provided fields will be updated.

### Endpoint

```
PUT /api/roles/{id}
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated role ID |

### Request Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

All fields are **optional**. Only include fields you want to update.

```json
{
  "name": "senior_booking_manager",          // Optional
  "displayName": "Senior Booking Manager",   // Optional
  "description": "Updated description",       // Optional
  "active": true                             // Optional
}
```

### Request Body Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | No | Updated role name (must be unique) |
| `displayName` | String | No | Updated display name |
| `description` | String | No | Updated description |
| `active` | Boolean | No | Updated active status |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Role updated successfully",
  "data": {
    "id": "encoded_role_id",
    "name": "senior_booking_manager",
    "displayName": "Senior Booking Manager",
    "description": "Updated description",
    "active": true,
    "isSystemRole": false,
    "createdAt": "2025-12-25T10:30:00",
    "updatedAt": "2025-12-25T11:45:00"
  }
}
```

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "ROLE_NOT_FOUND"
}
```

#### System Role Protected

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot modify system roles",
  "errorCode": "SYSTEM_ROLE_PROTECTED"
}
```

#### Duplicate Role Name

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Role name already exists",
  "errorCode": "DUPLICATE_ROLE_NAME"
}
```

### Example cURL Request

```bash
curl -X PUT "https://api.example.com/api/roles/encoded_role_id" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "displayName": "Senior Booking Manager",
    "description": "Updated description"
  }'
```

### Important Notes

- **System roles cannot be updated**: Attempting to update a system role (like "admin" or "user") will result in a `SYSTEM_ROLE_PROTECTED` error.
- **Partial updates**: You only need to include the fields you want to change. Other fields will remain unchanged.
- **Name uniqueness**: If updating the name, it must be unique across all roles.

---

## 5. Delete Single Role

Delete a specific role by its obfuscated ID.

### Endpoint

```
DELETE /api/roles/{id}
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Obfuscated role ID |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "1 role(s) deleted successfully",
  "data": null
}
```

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

The role is silently skipped if not found, returning success with 0 deleted count.

#### System Role Protection

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete system roles. System roles cannot be deleted.",
  "errorCode": "CANNOT_DELETE_SYSTEM_ROLES"
}
```

### Example cURL Request

```bash
curl -X DELETE "https://api.example.com/api/roles/encoded_role_id" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Important Notes

- **System roles cannot be deleted**: Attempting to delete a system role will result in an error.
- **Atomic operation**: The role is validated before deletion.

---

## 6. Delete Multiple Roles (Batch)

Delete multiple roles in a single request.

### Endpoint

```
DELETE /api/roles
```

### Request Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

```json
[
  "encoded_role_id_1",
  "encoded_role_id_2",
  "encoded_role_id_3"
]
```

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "3 role(s) deleted successfully",
  "data": null
}
```

### Error Responses

#### System Role Protection

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete system roles. System roles cannot be deleted.",
  "errorCode": "CANNOT_DELETE_SYSTEM_ROLES"
}
```

### Example cURL Request

```bash
curl -X DELETE "https://api.example.com/api/roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '[
    "encoded_role_id_1",
    "encoded_role_id_2",
    "encoded_role_id_3"
  ]'
```

### Important Notes

- **Atomic validation**: If **ANY** role in the list is a system role, **NO** roles will be deleted.
- **Partial success**: If some roles are not found, they are skipped, and the operation continues for valid roles.
- **System role protection**: The entire batch operation fails if it contains any system role.

---

## 7. Get Entities for Role

Get all entities with permission summary for a specific role. This endpoint returns a list of all entities (USER, ROLE, EMAIL_ACCOUNT, etc.) along with statistics about how many permissions are assigned to the role for each entity.

### Endpoint

```
GET /api/roles/{roleId}/entities
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `roleId` | String | Yes | Obfuscated role ID |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Entities retrieved successfully",
  "data": [
    {
      "entity": "AUDIT_LOG_SETTING",
      "entityDisplayName": "Audit Log Setting",
      "totalPermissions": 4,
      "assignedPermissions": 2,
      "unassignedPermissions": 2,
      "assignmentPercentage": 50.0
    },
    {
      "entity": "EMAIL_ACCOUNT",
      "entityDisplayName": "Email Account",
      "totalPermissions": 4,
      "assignedPermissions": 4,
      "unassignedPermissions": 0,
      "assignmentPercentage": 100.0
    },
    {
      "entity": "EMAIL_ACCOUNT_SIGNATURE",
      "entityDisplayName": "Email Account Signature",
      "totalPermissions": 4,
      "assignedPermissions": 0,
      "unassignedPermissions": 4,
      "assignmentPercentage": 0.0
    },
    {
      "entity": "USER",
      "entityDisplayName": "User",
      "totalPermissions": 4,
      "assignedPermissions": 2,
      "unassignedPermissions": 2,
      "assignmentPercentage": 50.0
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `entity` | String | Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT") |
| `entityDisplayName` | String | Human-readable entity name |
| `totalPermissions` | Integer | Total number of permissions available for this entity |
| `assignedPermissions` | Integer | Number of permissions assigned to the role |
| `unassignedPermissions` | Integer | Number of permissions not assigned to the role |
| `assignmentPercentage` | Double | Percentage of permissions assigned (0-100) |

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Invalid Role ID

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid role ID format",
  "errorCode": "INVALID_INPUT"
}
```

### Example cURL Request

```bash
curl -X GET "https://api.example.com/api/roles/encoded_role_id/entities" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Use Cases

1. **Permission Management UI**: Display a list of entities to choose from when managing role permissions
2. **Dashboard**: Show permission assignment statistics for a role
3. **Audit**: Track which entities a role has access to

---

## 8. Get Entity Permissions for Role

Get detailed permissions for a specific entity showing which permissions are assigned to the role and which are not. This endpoint is used after selecting an entity from the entities list.

### Endpoint

```
GET /api/roles/{roleId}/entities/{entity}/permissions
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `roleId` | String | Yes | Obfuscated role ID |
| `entity` | String | Yes | Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT") |

### Success Response

**Status Code:** `200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Entity permissions retrieved successfully",
  "data": {
    "entity": "USER",
    "entityDisplayName": "User",
    "permissions": [
      {
        "id": "perm_123_encoded",
        "name": "CREATE_USER",
        "description": "Allows creating new users in the system",
        "action": "CREATE",
        "actionDisplayName": "Create",
        "entity": "USER",
        "assigned": true,
        "active": true,
        "createdAt": "2025-01-15T10:00:00",
        "updatedAt": "2025-01-15T10:00:00"
      },
      {
        "id": "perm_456_encoded",
        "name": "READ_USER",
        "description": "Allows viewing user details and listings",
        "action": "READ",
        "actionDisplayName": "Read",
        "entity": "USER",
        "assigned": true,
        "active": true,
        "createdAt": "2025-01-15T10:00:00",
        "updatedAt": "2025-01-15T10:00:00"
      },
      {
        "id": "perm_789_encoded",
        "name": "UPDATE_USER",
        "description": "Allows updating user information",
        "action": "UPDATE",
        "actionDisplayName": "Update",
        "entity": "USER",
        "assigned": false,
        "active": true,
        "createdAt": "2025-01-15T10:00:00",
        "updatedAt": "2025-01-15T10:00:00"
      },
      {
        "id": "perm_012_encoded",
        "name": "DELETE_USER",
        "description": "Allows deleting users from the system",
        "action": "DELETE",
        "actionDisplayName": "Delete",
        "entity": "USER",
        "assigned": false,
        "active": true,
        "createdAt": "2025-01-15T10:00:00",
        "updatedAt": "2025-01-15T10:00:00"
      }
    ],
    "totalPermissions": 4,
    "assignedPermissions": 2,
    "unassignedPermissions": 2
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `entity` | String | Entity name |
| `entityDisplayName` | String | Human-readable entity name |
| `permissions` | Array | List of all permissions for this entity |
| `permissions[].id` | String | Obfuscated permission ID |
| `permissions[].name` | String | Permission name (e.g., "CREATE_USER") |
| `permissions[].description` | String | Human-readable description |
| `permissions[].action` | String | Permission action (CREATE, READ, UPDATE, DELETE, etc.) |
| `permissions[].actionDisplayName` | String | Human-readable action name |
| `permissions[].assigned` | Boolean | **true** if role has this permission, **false** otherwise |
| `permissions[].active` | Boolean | Whether the permission is active in the system |
| `totalPermissions` | Integer | Total number of permissions for this entity |
| `assignedPermissions` | Integer | Number of permissions assigned to the role |
| `unassignedPermissions` | Integer | Number of permissions not assigned |

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Entity Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "No permissions found for entity: INVALID_ENTITY",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

### Example cURL Request

```bash
curl -X GET "https://api.example.com/api/roles/encoded_role_id/entities/USER/permissions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Use Cases

1. **Permission Toggle UI**: Display checkboxes for each permission with current assignment status
2. **Permission Audit**: View what permissions a role has for a specific entity
3. **Permission Comparison**: Compare permissions across different roles

---

## 9. Update Role Permissions for Entity

Update (upsert) permissions for a role on a specific entity. This endpoint replaces all permissions for the given entity - permissions in the request will be assigned, and permissions not in the request will be removed.

**Important:** This is a **"replace"** operation, not an "add" operation.

### Endpoint

```
POST /api/roles/{roleId}/entities/{entity}/permissions
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `roleId` | String | Yes | Obfuscated role ID |
| `entity` | String | Yes | Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT") |

### Request Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

```json
{
  "permissionIds": [
    "perm_123_encoded",
    "perm_456_encoded"
  ]
}
```

### Request Body Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `permissionIds` | Array[String] | Yes | List of obfuscated permission IDs to assign to the role. Empty array removes all permissions. |

### Behavior

Given this request:
```json
{
  "permissionIds": ["perm_CREATE_USER", "perm_READ_USER"]
}
```

The system will:
- ✅ **Assign** CREATE_USER permission to the role
- ✅ **Assign** READ_USER permission to the role
- ❌ **Remove** UPDATE_USER permission from the role (if it was assigned)
- ❌ **Remove** DELETE_USER permission from the role (if it was assigned)

To remove all permissions for an entity:
```json
{
  "permissionIds": []
}
```

### Success Response

**Status Code:** `200 OK`

Returns the same structure as [Get Entity Permissions](#8-get-entity-permissions-for-role) with updated `assigned` status:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Entity permissions retrieved successfully",
  "data": {
    "entity": "USER",
    "entityDisplayName": "User",
    "permissions": [
      {
        "id": "perm_123_encoded",
        "name": "CREATE_USER",
        "assigned": true,
        ...
      },
      {
        "id": "perm_456_encoded",
        "name": "READ_USER",
        "assigned": true,
        ...
      },
      {
        "id": "perm_789_encoded",
        "name": "UPDATE_USER",
        "assigned": false,
        ...
      },
      {
        "id": "perm_012_encoded",
        "name": "DELETE_USER",
        "assigned": false,
        ...
      }
    ],
    "totalPermissions": 4,
    "assignedPermissions": 2,
    "unassignedPermissions": 2
  }
}
```

### Error Responses

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Entity Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "No permissions found for entity: INVALID_ENTITY",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

#### Invalid Permission ID

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid permission ID format",
  "errorCode": "INVALID_INPUT"
}
```

#### Permission Not For Entity

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "One or more permission IDs do not belong to entity: USER",
  "errorCode": "INVALID_INPUT"
}
```

### Example cURL Request

#### Assign Specific Permissions

```bash
curl -X POST "https://api.example.com/api/roles/encoded_role_id/entities/USER/permissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "permissionIds": [
      "perm_123_encoded",
      "perm_456_encoded"
    ]
  }'
```

#### Remove All Permissions for Entity

```bash
curl -X POST "https://api.example.com/api/roles/encoded_role_id/entities/USER/permissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "permissionIds": []
  }'
```

### Use Cases

1. **Permission Management UI**: Update permissions when user toggles checkboxes
2. **Batch Permission Update**: Update all permissions for an entity in one call
3. **Permission Templates**: Apply predefined permission sets to roles
4. **Role Cloning**: Copy permissions from one role to another

### Important Notes

- **Replace Operation**: This is NOT an additive operation. The endpoint replaces all permissions for the entity.
- **Transaction Support**: The operation is transactional - either all changes succeed or none do.
- **System Roles Allowed**: Unlike role deletion/update, permissions can be modified for system roles.
- **Validation**: All permission IDs must belong to the specified entity.
- **Audit Logging**: All permission changes are logged for audit purposes.

---

## 10. Reset Role Permissions to System Defaults

Reset permissions for a specific role and entity back to system defaults. This endpoint is only available for system roles (SUPERADMIN, ADMIN, USER, GUEST) and restores the default permission configuration for the specified entity.

### Endpoint

```
POST /api/roles/{roleId}/entities/{entity}/permissions/reset
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `roleId` | String | Yes | Obfuscated role ID (must be a system role) |
| `entity` | String | Yes | Entity name (e.g., "USER", "ROLE", "EMAIL_ACCOUNT") |

### Request Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

**None** - This endpoint requires no request body.

### Default Permission Configuration

The endpoint resets permissions based on the role type:

| Role | Permissions Assigned |
|------|---------------------|
| **SUPERADMIN** | CREATE, READ, UPDATE, DELETE |
| **ADMIN** | CREATE, READ, UPDATE |
| **USER** | CREATE, READ |
| **GUEST** | READ |

### Success Response

**Status Code:** `200 OK`

Returns the same structure as [Get Entity Permissions](#8-get-entity-permissions-for-role) with permissions reset to defaults:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Entity permissions retrieved successfully",
  "data": {
    "entity": "USER",
    "entityDisplayName": "User",
    "permissions": [
      {
        "id": "perm_123_encoded",
        "name": "CREATE_USER",
        "description": "Allows creating new users",
        "action": "CREATE",
        "actionDisplayName": "Create",
        "entity": "USER",
        "assigned": true,
        "active": true,
        "createdAt": "2025-12-25T10:00:00",
        "updatedAt": "2025-12-25T10:00:00"
      },
      {
        "id": "perm_456_encoded",
        "name": "READ_USER",
        "description": "Allows viewing user details",
        "action": "READ",
        "actionDisplayName": "Read",
        "entity": "USER",
        "assigned": true,
        "active": true,
        "createdAt": "2025-12-25T10:00:00",
        "updatedAt": "2025-12-25T10:00:00"
      },
      {
        "id": "perm_789_encoded",
        "name": "UPDATE_USER",
        "description": "Allows updating user information",
        "action": "UPDATE",
        "actionDisplayName": "Update",
        "entity": "USER",
        "assigned": false,
        "active": true,
        "createdAt": "2025-12-25T10:00:00",
        "updatedAt": "2025-12-25T10:00:00"
      },
      {
        "id": "perm_012_encoded",
        "name": "DELETE_USER",
        "description": "Allows deleting users",
        "action": "DELETE",
        "actionDisplayName": "Delete",
        "entity": "USER",
        "assigned": false,
        "active": true,
        "createdAt": "2025-12-25T10:00:00",
        "updatedAt": "2025-12-25T10:00:00"
      }
    ],
    "totalPermissions": 4,
    "assignedPermissions": 2,
    "unassignedPermissions": 2
  }
}
```

**Note:** For a USER role, only CREATE and READ are assigned. The `assignedPermissions` count reflects the default permissions for that role type.

### Error Responses

#### Invalid Role ID Format

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid role ID format",
  "errorCode": "INVALID_INPUT"
}
```

#### Role Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Role not found",
  "errorCode": "ROLE_NOT_FOUND"
}
```

#### Not a System Role

**Status Code:** `400 Bad Request`

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Permission reset is only allowed for system roles",
  "errorCode": "OPERATION_NOT_ALLOWED"
}
```

#### Entity Not Found

**Status Code:** `404 Not Found`

```json
{
  "success": false,
  "statusCode": 404,
  "message": "No permissions found for entity: INVALID_ENTITY",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

### Example cURL Request

#### Reset USER permissions for ADMIN role

```bash
curl -X POST "https://api.example.com/api/roles/encoded_admin_id/entities/USER/permissions/reset" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

This will reset the ADMIN role's USER permissions to: CREATE_USER, READ_USER, UPDATE_USER (no DELETE_USER).

#### Reset EMAIL_ACCOUNT permissions for GUEST role

```bash
curl -X POST "https://api.example.com/api/roles/encoded_guest_id/entities/EMAIL_ACCOUNT/permissions/reset" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

This will reset the GUEST role's EMAIL_ACCOUNT permissions to: READ_EMAIL_ACCOUNT only.

### Use Cases

1. **Restore Default Configuration**: After manual permission changes, restore a role's permissions to system defaults
2. **Fix Permission Issues**: Quickly fix misconfigured permissions for system roles
3. **Testing**: Reset permissions to known state for testing purposes
4. **Onboarding**: Ensure system roles have correct baseline permissions after system updates

### Important Notes

- **System Roles Only**: This endpoint only works for built-in system roles (SUPERADMIN, ADMIN, USER, GUEST)
- **Custom Roles**: Custom roles cannot be reset using this endpoint
- **Entity-Specific**: Reset is applied only to the specified entity, other entities remain unchanged
- **Transaction Support**: The operation is transactional - either all changes succeed or none do
- **No Request Body**: Unlike the update endpoint, this endpoint requires no request body
- **Audit Logging**: All permission resets are logged for audit purposes

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `DUPLICATE_ROLE_NAME` | 400 | Role name already exists in the system |
| `VALIDATION_ERROR` | 400 | Request validation failed (missing required fields) |
| `SYSTEM_ROLE_PROTECTED` | 400 | Attempted to modify or delete a system role |
| `CANNOT_DELETE_SYSTEM_ROLES` | 400 | Batch delete request contains system roles |
| `ROLE_NOT_FOUND` | 404 | Role with specified ID not found |
| `ROLE_CREATE_FAILED` | 500 | Internal error during role creation |
| `ROLE_UPDATE_FAILED` | 500 | Internal error during role update |
| `ROLES_DELETE_FAILED` | 500 | Internal error during role deletion |
| `GET_ROLE_FAILED` | 500 | Internal error during role retrieval |

---

## Data Models

### RoleDTO

Response model for role data.

```json
{
  "id": "string",              // Obfuscated role ID
  "name": "string",            // Unique role identifier (lowercase_with_underscores)
  "displayName": "string",     // Human-readable role name
  "description": "string",     // Detailed description (nullable)
  "active": "boolean",         // Whether role is active
  "isSystemRole": "boolean",   // Whether role is a system role (cannot be deleted/modified)
  "createdAt": "datetime",     // Timestamp when role was created
  "updatedAt": "datetime"      // Timestamp when role was last updated
}
```

### CreateRoleDTO

Request model for creating a role.

```json
{
  "name": "string",            // Optional: Auto-generated if not provided
  "displayName": "string",     // Required
  "description": "string",     // Optional
  "active": "boolean"          // Required
}
```

### UpdateRoleDTO

Request model for updating a role. All fields are optional.

```json
{
  "name": "string",            // Optional
  "displayName": "string",     // Optional
  "description": "string",     // Optional
  "active": "boolean"          // Optional
}
```

### EntitySummaryDTO

Response model for entity permission summary.

```json
{
  "entity": "string",                    // Entity name (e.g., "USER", "ROLE")
  "entityDisplayName": "string",         // Human-readable entity name
  "totalPermissions": "integer",         // Total permissions for this entity
  "assignedPermissions": "integer",      // Permissions assigned to role
  "unassignedPermissions": "integer",    // Permissions not assigned to role
  "assignmentPercentage": "double"       // Percentage assigned (0-100)
}
```

### EntityPermissionsDTO

Response model for detailed entity permissions.

```json
{
  "entity": "string",                    // Entity name
  "entityDisplayName": "string",         // Human-readable entity name
  "permissions": "array",                // Array of RolePermissionItemDTO
  "totalPermissions": "integer",         // Total permissions for this entity
  "assignedPermissions": "integer",      // Permissions assigned to role
  "unassignedPermissions": "integer"     // Permissions not assigned to role
}
```

### RolePermissionItemDTO

Permission item with assignment status.

```json
{
  "id": "string",                        // Obfuscated permission ID
  "name": "string",                      // Permission name (e.g., "CREATE_USER")
  "description": "string",               // Human-readable description
  "action": "string",                    // Permission action (CREATE, READ, UPDATE, DELETE)
  "actionDisplayName": "string",         // Human-readable action name
  "entity": "string",                    // Entity name
  "assigned": "boolean",                 // true if role has this permission
  "active": "boolean",                   // Whether permission is active
  "createdAt": "datetime",               // Timestamp when created
  "updatedAt": "datetime"                // Timestamp when updated
}
```

### UpdateRolePermissionsDTO

Request model for updating role permissions.

```json
{
  "permissionIds": "array[string]"       // Required: List of permission IDs to assign
}
```

---

## System Roles

The system includes predefined roles that cannot be modified or deleted:

| Name | Display Name | Description |
|------|--------------|-------------|
| `admin` | Administrator | Full system access - create, read, update, delete all resources |
| `user` | User | Basic user access - read own data and create new items |

**Note:** System roles have `isSystemRole: true` and are protected from modification and deletion.

---

## Best Practices

### 1. Role Naming Conventions

- Use lowercase letters, numbers, and underscores only
- Keep names concise and descriptive
- Examples: `booking_manager`, `finance_officer`, `safari_guide`

### 2. Pagination

- Use appropriate page sizes (10-50 items recommended)
- Always handle pagination metadata (totalPages, totalItems)
- Cache results when appropriate

### 3. Filtering

- Combine multiple filters for precise queries
- Use case-insensitive partial matching for text searches (available for `name` and `displayName`)
- Filter by `active: true` to get only active roles in production
- Note: The `description` field cannot be used for filtering due to database constraints (CLOB type)

### 4. Error Handling

- Always check the `success` field in responses
- Handle specific error codes appropriately
- Implement retry logic for 500-level errors

### 5. Security

- Never expose system role IDs to untrusted clients
- Validate permissions before allowing role modifications
- Use obfuscated IDs in all client-facing interfaces

---

## Changelog

### Version 1.2.0 (2025-12-28)

- Added permission reset endpoint for system roles
- New endpoint: `POST /api/roles/{roleId}/entities/{entity}/permissions/reset` - Reset role permissions to system defaults
- Reset endpoint only works for system roles (SUPERADMIN, ADMIN, USER, GUEST)
- Automatic restoration of default permissions based on role type

### Version 1.1.0 (2025-12-28)

- Added permission management endpoints for roles
- New endpoint: `GET /api/roles/{roleId}/entities` - Get all entities with permission summary
- New endpoint: `GET /api/roles/{roleId}/entities/{entity}/permissions` - Get detailed permissions for entity
- New endpoint: `POST /api/roles/{roleId}/entities/{entity}/permissions` - Update role permissions for entity
- Added new DTOs: EntitySummaryDTO, EntityPermissionsDTO, RolePermissionItemDTO, UpdateRolePermissionsDTO
- Permission management supports system roles (unlike role deletion/update)
- Transactional permission updates with validation

### Version 1.0.1 (2025-12-25)

- Removed `description` filter parameter due to CLOB field limitations
- Description field remains in response data but cannot be used for filtering
- All other filtering capabilities remain unchanged

### Version 1.0.0 (2025-12-25)

- Initial release of Role Management API
- CRUD operations for roles
- Pagination and filtering support (name, displayName, active, isSystemRole)
- System role protection
- Batch delete operations

---

## Support

For issues or questions, please contact the API support team or file an issue in the project repository.
