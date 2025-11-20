# RBAC Implementation Documentation

**Project:** Kabengo Safaris Spring Boot Application
**Date:** November 18, 2025
**Focus:** Role-Based Access Control (RBAC) System Architecture

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Core Components](#core-components)
4. [Data Model](#data-model)
5. [Authentication Flow](#authentication-flow)
6. [Authorization Mechanisms](#authorization-mechanisms)
7. [Permission Checking](#permission-checking)
8. [Entity Relationships](#entity-relationships)

---

## Executive Summary

This Spring Boot application implements a **sophisticated, multi-layered RBAC system** with three complementary authorization mechanisms:

1. **JWT Authentication** - Stateless token-based authentication
2. **Endpoint-Level Authorization** - Servlet filter with dynamic permission mapping
3. **Method-Level Authorization** - AOP aspect with custom annotations

The system is **fully database-driven**, allowing runtime configuration of roles, permissions, and endpoint access without code changes. It supports Frappe ERPNext-inspired architecture with granular permission controls.

### Key Features

- ✅ Database-driven role and permission management
- ✅ Dynamic endpoint-to-permission mapping via servlet filter
- ✅ Method-level access control via AOP aspects
- ✅ Flexible permission models (name-based, action-based, role-based)
- ✅ System role/action protection (prevents accidental deletion)
- ✅ Pattern-matching for URL endpoints (regex support)
- ✅ Caching for optimal performance
- ✅ Comprehensive logging and audit trails
- ✅ JWT-based stateless authentication

---

## Architecture Overview

### Three-Layer Security Model

```
┌─────────────────────────────────────────────┐
│        Layer 3: Method-Level (AOP)          │
│     @RequirePermission + Aspect             │
└─────────────────────────────────────────────┘
                     ▲
                     │
                     │
┌─────────────────────────────────────────────┐
│     Layer 2: Endpoint-Level (Filter)        │
│  DynamicPermissionFilter + Database         │
└─────────────────────────────────────────────┘
                     ▲
                     │
                     │
┌─────────────────────────────────────────────┐
│  Layer 1: Authentication (JWT Filter)       │
│   JwtAuthenticationFilter + Provider        │
└─────────────────────────────────────────────┘
```

### Filter Chain Execution Order

```
HTTP Request
    ↓
DynamicPermissionFilter (endpoint-level permission check)
    ↓
JwtAuthenticationFilter (JWT extraction & validation)
    ↓
Spring Security Filters
    ↓
PermissionCheckAspect (method-level permission check)
    ↓
Controller Method Execution
```

---

## Core Components

### 1. Role Management

**File:** `Role.java`, `RoleService.java`

```
Role Entity:
├─ id: Long
├─ name: String (unique)
├─ displayName: String
├─ description: String
├─ active: Boolean (soft delete flag)
├─ isSystemRole: Boolean (prevents deletion if true)
├─ permissions: Set<Permission> (many-to-many, EAGER loaded)
├─ createdAt: LocalDateTime
└─ updatedAt: LocalDateTime

Key Methods:
├─ hasPermission(String permissionName): Boolean
├─ hasPermission(String actionCode, String resource): Boolean
├─ getResourcesForAction(String actionCode): Set<String>
├─ addPermission(Permission): void
└─ removePermission(Permission): void
```

**RoleService Responsibilities:**
- Create/update/delete roles (with system role protection)
- Assign/revoke permissions to/from roles
- Initialize system roles (ADMIN, USER)
- Query roles by name, status, permission

### 2. Permission Management

**File:** `Permission.java`, `PermissionService.java`, `PermissionActionType.java`

```
Permission Entity:
├─ id: Long
├─ name: String (unique, e.g., "create_booking")
├─ description: String
├─ category: String (e.g., "bookings", "parks")
├─ actionType: PermissionActionType (FK, EAGER loaded)
├─ resource: String (e.g., "Booking", "Park")
├─ active: Boolean (soft delete flag)
├─ createdAt: LocalDateTime
└─ updatedAt: LocalDateTime

PermissionActionType Entity:
├─ id: Long
├─ code: String (unique, e.g., "CREATE", "READ")
├─ description: String
├─ active: Boolean
├─ isSystem: Boolean (prevents deactivation if true)
├─ createdAt: LocalDateTime
└─ updatedAt: LocalDateTime
```

**System Action Types:**
- CREATE, READ, UPDATE, DELETE (CRUD operations)
- EXECUTE (Workflow actions)
- SUBMIT, AMEND, CANCEL (Document lifecycle)
- EXPORT, PRINT (Data operations)

### 3. User & Authentication

**File:** `User.java`, `CustomUserDetailsService.java`

```
User Entity (implements UserDetails):
├─ id: Long
├─ username: String (unique)
├─ email: String (unique)
├─ password: String (BCrypt hashed)
├─ enabled: Boolean
├─ accountLocked: Boolean
├─ firstName: String
├─ lastName: String
├─ roles: Set<Role> (many-to-many, EAGER loaded)
├─ failedAttempt: Integer
├─ accountLockedTime: LocalDateTime
├─ passwordExpiryDate: LocalDateTime
└─ ... (other user metadata)

Key Methods:
├─ getAuthorities(): Collection<GrantedAuthority>
│  (returns "ROLE_{name}" + "PERM_{name}" for each permission)
├─ hasPermission(String permissionName): Boolean
├─ hasPermission(String actionCode, String resource): Boolean
└─ hasRole(String roleName): Boolean
```

### 4. JWT Authentication

**File:** `JwtTokenProvider.java`, `JwtAuthenticationFilter.java`

```
JWT Token Structure:
{
  "alg": "HS256",
  "typ": "JWT"
}
{
  "subject": "john",
  "issuedAt": 1734596000,
  "expiresAt": 1734607800,  // 3 hours later
  "iss": "kabengo-safaris"
}
Signature: HMAC-SHA256(header.payload, secret)

Token Flow:
1. User POSTs username/password to /api/auth/login
2. JwtTokenProvider.generateToken(Authentication)
3. Returns {token: "...", expiresIn: 10800}
4. Client stores token
5. Client includes in Authorization: Bearer {token} header
6. JwtAuthenticationFilter extracts and validates token
7. CustomUserDetailsService loads User details
8. SecurityContext populated with authenticated user
```

---

## Data Model

### Entity Relationship Diagram

```
┌──────────────┐
│    User      │
│ (1 → M Role) │
└──────────────┘
      │
      │ many-to-many (user_roles)
      │
      ▼
┌──────────────┐
│    Role      │
│(1 → M Perm)  │
└──────────────┘
      │
      │ many-to-many (role_permissions)
      │
      ▼
┌──────────────┐         ┌─────────────────────┐
│ Permission   │◄────────│ PermissionActionType│
│ (N → 1 Act)  │ (FK)    │  (Action Types)     │
└──────────────┘         └─────────────────────┘

┌────────────────────┐
│ EndpointPermission │ (URL → Permission mapping)
│ (Runtime config)   │
└────────────────────┘
```

### Core Tables

**users:** User account information
- PK: id
- UNIQUE: email, username
- FK: None (owns relationships)

**roles:** System roles
- PK: id
- UNIQUE: name
- System roles: isSystemRole=true (cannot delete)

**permissions:** Granular permissions
- PK: id
- UNIQUE: name
- FK: action_type_id (required)

**permission_action_types:** Action type definitions
- PK: id
- UNIQUE: code
- System actions: isSystem=true (cannot deactivate)

**endpoint_permissions:** Runtime endpoint-to-permission mapping
- PK: id
- UNIQUE: (httpMethod, endpoint)
- Supports pattern matching (regex)
- Flexible permission models (name, action+resource, role)

**user_roles:** Many-to-many junction
- PK: (user_id, role_id)
- FK: Both cascade

**role_permissions:** Many-to-many junction
- PK: (role_id, permission_id)
- FK: Both cascade

---

## Authentication Flow

### Login Process

```
HTTP POST /api/auth/login
{
  "username": "john",
  "password": "password123"
}
        ↓
AuthenticationManager.authenticate()
        ↓
CustomUserDetailsService.loadUserByUsername("john")
        ↓
SELECT * FROM users WHERE username = 'john'
        ↓
PasswordEncoder.matches(rawPassword, hashedPassword)
        ↓
If valid: JwtTokenProvider.generateToken(Authentication)
        ↓
HMAC-SHA256 sign(header.payload)
        ↓
HTTP 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 10800
}
```

### Request Authentication

```
HTTP GET /api/parks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        ↓
JwtAuthenticationFilter.doFilterInternal()
        ↓
Extract: Authorization header → Bearer token
        ↓
JwtTokenProvider.validateToken(token)
  - Verify signature (HMAC-SHA256)
  - Check expiration
  - Return: true/false
        ↓
JwtTokenProvider.getUsernameFromToken(token)
        ↓
CustomUserDetailsService.loadUserByUsername("john")
  - Query: User + roles (EAGER)
  - Query: Role permissions (EAGER)
        ↓
Create UsernamePasswordAuthenticationToken
        ↓
SecurityContextHolder.getContext().setAuthentication(auth)
        ↓
Continue filter chain with authenticated User
```

---

## Authorization Mechanisms

### Layer 1: Endpoint-Level (Servlet Filter)

**Component:** `DynamicPermissionFilter`, `EndpointPermissionService`

Enforces authorization before request reaches controller.

```
Request: POST /api/parks
        ↓
DynamicPermissionFilter.doFilterInternal()
        ↓
Check if public path:
  - /api/auth/*
  - /swagger-ui/*
  - /v3/api-docs/*
  - /actuator/*
  If YES → Skip to next filter
        ↓
Get User from SecurityContext
        ↓
EndpointPermissionService.canUserAccessEndpoint(user, "POST", "/api/parks")
        ↓
Lookup endpoint_permissions table:
  1. Try exact match: (POST, /api/parks)
  2. Try pattern match: (POST, /api/parks/.*)
  3. If not found: ALLOW by default
        ↓
Check permission requirement:
  Model A: requiredPermissionName?
    → user.hasPermission("create_park")
  Model B: actionCode + resourceType?
    → user.hasPermission("CREATE", "Park")
  Model C: allowedRoles?
    → user.hasRole("admin") or user.hasRole("manager")
        ↓
Decision: ALLOW → Continue | DENY → 403 Forbidden
```

**Advantage:** Single point of enforcement for all REST endpoints

### Layer 2: Method-Level (AOP Aspect)

**Component:** `PermissionCheckAspect`, `RequirePermission` annotation

Fine-grained method protection.

```
@RestController
public class ParkController {

  @RequirePermission(permission = "create_park")
  @PostMapping("/parks")
  public ResponseEntity<Park> createPark(@RequestBody CreateParkRequest req) {
    // This method is intercepted by PermissionCheckAspect
  }
}
        ↓
PermissionCheckAspect.checkPermission(@Before)
        ↓
Get User from SecurityContext
        ↓
Check roles (if specified):
  @RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
  → user.hasRole("ADMIN") OR user.hasRole("MANAGER")
        ↓
Check permission:
  @RequirePermission(permission = "create_park")
  → user.hasPermission("create_park")
  OR
  @RequirePermission(action = "CREATE", resource = "Park")
  → user.hasPermission("CREATE", "Park")
        ↓
Decision: ALLOW → Execute method | DENY → throw AccessDeniedException
```

**Advantage:** Redundant protection, documentation via annotation, per-method granularity

### Layer 3: Database-Driven Endpoint Mapping

**Component:** `EndpointPermission`, `EndpointPermissionService`, `EndpointPermissionController`

Zero-code permission updates.

```
Admin configures endpoint permission at runtime:
POST /api/admin/endpoint-permissions
{
  "httpMethod": "POST",
  "endpoint": "/api/parks",
  "requiredPermissionName": "create_park",
  "requiresAuth": true,
  "active": true
}
        ↓
Saved to endpoint_permissions table
        ↓
On next request to POST /api/parks:
DynamicPermissionFilter queries table (cached)
        ↓
Enforces permission without restarting application
```

---

## Permission Checking

### Three Permission Models

#### Model 1: Specific Permission Name

```java
// In database:
Permission: name="create_park", actionType="CREATE", resource="Park"
Role: permissions=[create_park, update_park, ...]

// In code:
@RequirePermission(permission = "create_park")
public void createPark(Park park) { ... }

// Check:
user.hasPermission("create_park")
  → iterate roles → role.getPermissions()
  → check if any permission.name == "create_park" and active=true
```

#### Model 2: Action + Resource

```java
// In database:
PermissionActionType: code="CREATE"
Permission: actionType="CREATE", resource="Park"
Role: permissions=[permission1, permission2, ...]

// In code:
@RequirePermission(action = "CREATE", resource = "Park")
public void createPark(Park park) { ... }

// Check:
user.hasPermission("CREATE", "Park")
  → iterate roles → role.getPermissions()
  → check if permission.actionType.code=="CREATE"
    AND permission.resource=="Park" AND active=true
```

#### Model 3: Role-Based

```java
// In code:
@RequirePermission(
    roles = {"ADMIN", "MANAGER"},
    requireAllRoles = false  // OR logic
)
public void deleteBooking(Long id) { ... }

// Check:
user.hasRole("ADMIN") OR user.hasRole("MANAGER")
  → iterate roles → check if role.name matches any in required list
```

### User Permission Methods

```java
// User.java

public boolean hasPermission(String permissionName) {
    return roles.stream()
        .filter(Role::getActive)  // Only active roles
        .anyMatch(role -> role.hasPermission(permissionName));
}

public boolean hasPermission(String actionCode, String resource) {
    return roles.stream()
        .filter(Role::getActive)
        .anyMatch(role -> role.hasPermission(actionCode, resource));
}

public boolean hasRole(String roleName) {
    return roles.stream()
        .filter(Role::getActive)
        .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
}
```

### Role Permission Methods

```java
// Role.java

public boolean hasPermission(String permissionName) {
    return permissions.stream()
        .filter(Permission::getActive)
        .anyMatch(p -> p.getName().equals(permissionName));
}

public boolean hasPermission(String actionCode, String resource) {
    return permissions.stream()
        .filter(Permission::getActive)
        .anyMatch(p ->
            p.getActionType() != null &&
            p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
            p.getResource().equalsIgnoreCase(resource)
        );
}

public Set<String> getResourcesForAction(String actionCode) {
    return permissions.stream()
        .filter(p -> p.getActionType() != null &&
                   p.getActionType().getCode().equalsIgnoreCase(actionCode) &&
                   p.getActive())
        .map(Permission::getResource)
        .collect(Collectors.toSet());
}
```

---

## Entity Relationships

### One-to-Many: Role ↔ Permission

```
User
  │
  ├─ Role "admin"
  │   ├─ Permission "create_park"
  │   ├─ Permission "update_park"
  │   ├─ Permission "delete_park"
  │   └─ Permission "read_park"
  │
  └─ Role "manager"
      ├─ Permission "create_booking"
      ├─ Permission "update_booking"
      └─ Permission "read_booking"
```

### Many-to-Many: Role ↔ Permission

```
sql
SELECT r.name, p.name
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.active = true AND p.active = true
```

### Many-to-Many: User ↔ Role

```
sql
SELECT u.username, r.name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.active = true
```

### OneToMany: PermissionActionType ← Permission

```
PermissionActionType "CREATE"
  ├─ Permission "create_park"
  ├─ Permission "create_booking"
  └─ Permission "create_safari_package"

PermissionActionType "DELETE"
  ├─ Permission "delete_park"
  ├─ Permission "delete_booking"
  └─ Permission "delete_safari_package"
```

### Endpoint Permission Lookup

```
EndpointPermission: /api/parks (POST)
  ├─ requiredPermissionName: "create_park"
  ├─ actionCode: null
  ├─ resourceType: null
  ├─ allowedRoles: null
  └─ Check: user.hasPermission("create_park")

EndpointPermission: /api/bookings/{id} (DELETE)
  ├─ requiredPermissionName: null
  ├─ actionCode: "DELETE"
  ├─ resourceType: "Booking"
  ├─ allowedRoles: null
  └─ Check: user.hasPermission("DELETE", "Booking")

EndpointPermission: /api/admin/* (GET)
  ├─ requiredPermissionName: null
  ├─ actionCode: null
  ├─ resourceType: null
  ├─ allowedRoles: "admin,superadmin"
  └─ Check: user.hasRole("admin") OR user.hasRole("superadmin")
```

---

## Authority Conversion

Spring Security uses "authorities" (also called "permissions" colloquially):

```
User.getAuthorities() returns:
[
  "ROLE_admin",           // From user roles
  "ROLE_manager",
  "PERM_create_park",     // From permissions in roles
  "PERM_update_park",
  "PERM_delete_park",
  "PERM_read_park",
  "PERM_create_booking",
  ...
]

Used in annotations:
@PreAuthorize("hasRole('ADMIN')")
  ↓ Converted internally to:
@PreAuthorize("hasAuthority('ROLE_ADMIN')")

@PreAuthorize("hasAuthority('PERM_create_park')")
  ↓ Exact match against authority list
```

---

## Summary

The RBAC implementation provides **three complementary layers** of authorization:

1. **JWT Authentication** - Ensures user identity
2. **Endpoint Authorization** - Protects REST endpoints dynamically
3. **Method Authorization** - Fine-grained method protection

The system is **database-driven** and **runtime-configurable**, allowing permission changes without redeployment. It follows **enterprise security practices** with proper separation of concerns, comprehensive logging, and performance optimization through caching.
