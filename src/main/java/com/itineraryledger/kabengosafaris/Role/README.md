# Role-Based Access Control (RBAC) Documentation

**Project:** Kabengo Safaris - Spring Boot Application
**Date:** November 18, 2025
**Documentation Version:** 1.0

---

## Quick Navigation

This directory contains comprehensive documentation for the RBAC system:

### 📖 Main Documentation Files

1. **[RBAC_IMPLEMENTATION.md](./RBAC_IMPLEMENTATION.md)** ← Start here
   - Complete system architecture overview
   - Core components and their interactions
   - Data model and entity relationships
   - Authentication and authorization flows
   - Permission checking mechanisms
   - Entity relationships visualization

2. **[DYNAMIC_RBAC_USAGE_GUIDE.md](./DYNAMIC_RBAC_USAGE_GUIDE.md)** ← For using the system
   - How to create roles dynamically
   - Managing permissions at runtime
   - Assigning permissions to roles
   - Configuring endpoint permissions
   - Method-level permission control
   - Advanced scenarios and use cases
   - Best practices
   - Troubleshooting guide

3. **[IMPROVEMENTS_ROADMAP.md](./IMPROVEMENTS_ROADMAP.md)** ← For future enhancements
   - Security enhancements needed
   - New authorization features
   - Performance optimizations
   - Data model improvements
   - Developer experience improvements
   - Observability and audit enhancements
   - Implementation roadmap with priorities

---

## System Overview

### What is RBAC?

Role-Based Access Control (RBAC) is a method of restricting system access based on a user's role within an organization. In this system:

```
User → has many → Roles → have many → Permissions → use → Actions on Resources
```

### Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    KABENGO SAFARIS RBAC SYSTEM                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 3: Method-Level (AOP Aspect)                            │
│  ├─ @RequirePermission annotation                              │
│  ├─ PermissionCheckAspect intercepts methods                   │
│  └─ Additional redundant protection                            │
│                                                                 │
│  Layer 2: Endpoint-Level (Servlet Filter)                      │
│  ├─ DynamicPermissionFilter enforces HTTP endpoints            │
│  ├─ EndpointPermissionService with pattern matching            │
│  ├─ Database-driven configuration (no code restart needed)     │
│  └─ Supports name, action, role-based permissions             │
│                                                                 │
│  Layer 1: Authentication (JWT Filter)                          │
│  ├─ JwtAuthenticationFilter extracts tokens                    │
│  ├─ JwtTokenProvider validates signatures                      │
│  ├─ CustomUserDetailsService loads user details               │
│  └─ SecurityContext stores authenticated user                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Key Features

✅ **Multi-layered Security**
- JWT authentication at entry point
- Endpoint-level permission checks
- Method-level fine-grained control

✅ **Database-Driven**
- Zero-code endpoint permission configuration
- Runtime role and permission management
- No need to restart application for changes

✅ **Flexible Permission Models**
- Specific permission names: "create_booking"
- Action-resource based: CREATE action on Booking resource
- Role-based: user must have specific roles

✅ **Performance Optimized**
- Caching of action types and endpoint permissions
- Pattern regex compilation cached
- EAGER loading of relationships for permission checks

✅ **System Protection**
- System roles (ADMIN, USER) cannot be deleted
- System action types cannot be deactivated
- Prevents accidental data loss

✅ **Comprehensive Logging**
- Permission checks logged throughout
- Audit trails for debugging
- SQL query logging in debug mode

---

## Quick Start

### 1. Understanding the Data Model

```sql
-- Users have multiple roles
SELECT u.username, r.name FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id;

-- Roles have multiple permissions
SELECT r.name, p.name FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id;

-- Permissions have action types
SELECT p.name, pat.code, p.resource
FROM permissions p
LEFT JOIN permission_action_types pat ON p.action_type_id = pat.id;

-- Endpoints mapped to permissions
SELECT ep.http_method, ep.endpoint, ep.required_permission_name
FROM endpoint_permissions ep;
```

### 2. Creating a New Role with Permissions

```bash
# 1. Create permission (if not exists)
curl -X POST http://localhost:8080/api/admin/permissions \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_booking",
    "description": "Create safari bookings",
    "category": "bookings",
    "actionCode": "CREATE",
    "resource": "Booking"
  }'

# 2. Create role
curl -X POST http://localhost:8080/api/admin/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "booking_operator",
    "displayName": "Booking Operator",
    "description": "Handles customer booking requests"
  }'

# 3. Assign permission to role
curl -X POST http://localhost:8080/api/admin/roles/5/permissions/12 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 4. Register endpoint permission
curl -X POST http://localhost:8080/api/admin/endpoint-permissions \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "endpoint": "/api/bookings",
    "requiredPermissionName": "create_booking",
    "requiresAuth": true,
    "active": true
  }'
```

### 3. Using @RequirePermission in Code

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @RequirePermission(permission = "create_booking")
    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody CreateBookingRequest req) {
        // Only users with "create_booking" permission can execute this
        return bookingService.create(req);
    }

    @RequirePermission(action = "DELETE", resource = "Booking")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // Only users with DELETE action on Booking resource
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePermission(
        roles = {"ADMIN", "MANAGER"},
        requireAllRoles = false
    )
    @GetMapping("/reports")
    public ResponseEntity<List<BookingReport>> getReports() {
        // Only ADMIN or MANAGER role can access
        return ResponseEntity.ok(bookingService.getReports());
    }
}
```

### 4. Programmatic Permission Checks

```java
@Service
public class BookingService {

    public void approveBooking(Long bookingId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        // Check permission
        if (!user.hasPermission("approve_booking")) {
            throw new AccessDeniedException("Cannot approve bookings");
        }

        // Check role
        if (!user.hasRole("manager")) {
            throw new AccessDeniedException("Only managers can approve");
        }

        // Check action-resource
        if (!user.hasPermission("APPROVE", "Booking")) {
            throw new AccessDeniedException("Missing APPROVE permission");
        }

        // Proceed...
    }
}
```

---

## Core Concepts

### Roles
- Logical grouping of permissions
- Examples: ADMIN, MANAGER, OPERATOR, USER
- Can be created, modified, deleted (except system roles)
- Assigned to users via many-to-many relationship

### Permissions
- Specific access right to perform action
- Examples: "create_booking", "read_park", "delete_user"
- Composed of: Action Type + Resource
- Assigned to roles (permissions can't be assigned directly to users)

### Action Types
- Defines what operations are possible
- System actions: CREATE, READ, UPDATE, DELETE, SUBMIT, APPROVE, CANCEL, EXPORT, PRINT
- Can create custom action types at runtime
- Used with permissions for action-resource model

### Users
- Have multiple roles
- Inherit permissions from all assigned roles
- Permissions checked at login and cached
- Can use programmatic checks with hasPermission() methods

### Endpoint Permissions
- Maps HTTP endpoints to required permissions
- Database-driven (can be configured without code restart)
- Supports exact match and regex pattern matching
- Flexible: permission name, action-resource, or role-based

---

## Common Tasks

### View All Users and Their Roles

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users?page=0&size=20
```

### Get User's Permissions

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users/{userId}/permissions
```

### List All Active Roles

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/roles
```

### List All Permissions for a Role

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/roles/{roleId}/permissions
```

### Register Endpoint Permission

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "endpoint": "/api/resource",
    "requiredPermissionName": "permission_name",
    "requiresAuth": true,
    "active": true
  }' \
  http://localhost:8080/api/admin/endpoint-permissions
```

### Check If Endpoint is Protected

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/endpoint-permissions \
  | grep "endpoint_name"
```

### Deactivate a Role (Soft Delete)

```bash
curl -X PATCH -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/roles/{roleId}/active?active=false
```

### Reactivate a Role

```bash
curl -X PATCH -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/roles/{roleId}/active?active=true
```

---

## Database Schema

### Key Tables

**users**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    account_locked BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**roles**
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    active BOOLEAN DEFAULT true,
    is_system_role BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**permissions**
```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(255),
    action_type_id BIGINT,
    resource VARCHAR(255),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (action_type_id) REFERENCES permission_action_types(id)
);
```

**permission_action_types**
```sql
CREATE TABLE permission_action_types (
    id BIGINT PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT true,
    is_system BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**endpoint_permissions**
```sql
CREATE TABLE endpoint_permissions (
    id BIGINT PRIMARY KEY,
    http_method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    required_permission_name VARCHAR(255),
    action_code VARCHAR(255),
    resource_type VARCHAR(255),
    allowed_roles VARCHAR(500),
    requires_auth BOOLEAN DEFAULT true,
    requires_pattern_matching BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (http_method, endpoint)
);
```

**user_roles** (Junction)
```sql
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

**role_permissions** (Junction)
```sql
CREATE TABLE role_permissions (
    role_id BIGINT,
    permission_id BIGINT,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
```

---

## Security Considerations

### Current Strengths
✅ JWT-based stateless authentication
✅ BCRYPT password hashing
✅ Multi-layered authorization (endpoint + method)
✅ System role protection
✅ Comprehensive logging

### Areas for Improvement
⚠️ JWT secret key regenerated on startup (should be externalized)
⚠️ No token revocation mechanism (implement blacklist)
⚠️ Unknown endpoints default to ALLOW (should default to DENY)
⚠️ No refresh token mechanism (users must re-auth after 3 hours)
⚠️ No row-level security (all "read_booking" users see all bookings)

See [IMPROVEMENTS_ROADMAP.md](./IMPROVEMENTS_ROADMAP.md) for detailed enhancement proposals.

---

## Performance Tips

### Reduce N+1 Queries
The system uses EAGER loading for roles and permissions to avoid N+1 issues during permission checks. This is a trade-off for convenience vs. memory. For large role permission sets, consider:

```java
@ManyToMany(fetch = FetchType.LAZY)  // Load only when accessed
private Set<Role> roles;

// Implement caching in User class
public Set<String> getAllPermissions() {
    if (cachedPermissions != null && cacheNotExpired()) {
        return cachedPermissions;
    }
    // Load from database
    cachedPermissions = computePermissions();
    return cachedPermissions;
}
```

### Cache Endpoint Permissions
The system caches endpoint permission lookups. After making changes, clear cache:

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/endpoint-permissions/cache/clear
```

### Use Pattern Matching Wisely
Regex patterns are compiled once and cached. Too many patterns can impact lookup performance.

```
Good: /api/admin/.*  (one pattern for all admin endpoints)
Bad:  /api/users/.*, /api/roles/.*, /api/permissions/.*  (many patterns)
```

---

## Troubleshooting Guide

### User Can't Access Protected Endpoint

**Check List:**
1. Is JWT token valid? → Verify with `/api/auth/validate-token`
2. Is user role active? → Query database: `SELECT * FROM user_roles WHERE user_id = X`
3. Does role have permission? → Query database: `SELECT * FROM role_permissions WHERE role_id = Y`
4. Is permission active? → Query database: `SELECT * FROM permissions WHERE name = 'Z' AND active = true`
5. Is endpoint permission registered? → Query: `SELECT * FROM endpoint_permissions WHERE endpoint = '/api/path'`

### Permission Check Always Fails

**Common Causes:**
- Inactive role → Use `PATCH /api/admin/roles/{id}/active?active=true`
- Inactive permission → Use `PATCH /api/admin/permissions/{id}/active?active=true`
- ActionType is NULL → Check foreign key: `WHERE action_type_id IS NULL`

### Endpoint Permission Not Taking Effect

**Check:**
1. Is endpoint registered in database?
2. Is endpoint permission ACTIVE?
3. Is cache stale? → Clear: `POST /api/admin/endpoint-permissions/cache/clear`
4. Is URL pattern correct? (e.g., `/api/bookings/.*` vs `/api/bookings/`)

See [DYNAMIC_RBAC_USAGE_GUIDE.md#troubleshooting](./DYNAMIC_RBAC_USAGE_GUIDE.md#troubleshooting) for more detailed troubleshooting steps.

---

## File Organization

```
src/main/java/com/itineraryledger/kabengosafaris/

├── Role/                                    # ← Documentation here
│   ├── Role.java                           # Entity with permission checks
│   ├── RoleService.java                    # Role CRUD operations
│   ├── RoleRepository.java                 # Role data access
│   ├── RBAC_IMPLEMENTATION.md              # Architecture documentation
│   ├── DYNAMIC_RBAC_USAGE_GUIDE.md         # Usage guide
│   ├── IMPROVEMENTS_ROADMAP.md             # Enhancement proposals
│   └── README.md                           # This file
│
├── Permission/                              # Permission & Action Types
│   ├── Permission.java                      # Permission entity
│   ├── PermissionAction.java                # Enum reference (legacy)
│   ├── PermissionActionType.java            # Action types entity
│   ├── PermissionService.java               # Permission CRUD
│   ├── PermissionRepository.java            # Permission queries
│   ├── PermissionActionService.java         # Action type CRUD (cached)
│   ├── PermissionActionTypeRepository.java  # Action type queries
│   ├── EndpointPermission.java              # URL-to-permission mapping
│   ├── EndpointPermissionService.java       # Endpoint permission logic
│   ├── EndpointPermissionRepository.java    # Endpoint permission queries
│   └── EndpointPermissionController.java    # Endpoint permission REST API
│
├── Security/                                # Authentication & Authorization
│   ├── RequirePermission.java               # Custom annotation
│   ├── PermissionCheckAspect.java           # AOP enforcement
│   ├── DynamicPermissionFilter.java         # Servlet filter
│   ├── CustomUserDetailsService.java        # UserDetailsService impl
│   ├── JwtTokenProvider.java                # JWT generation/validation
│   └── JwtAuthenticationFilter.java         # JWT extraction
│
├── Configurations/                          # Spring Configuration
│   ├── SecurityConfigurations.java          # Filter chain setup
│   └── DataInitializationService.java       # Startup initialization
│
└── User/                                    # User Entity
    ├── User.java                            # User with permission checks
    └── UserRepository.java                  # User queries
```

---

## Related Documentation

- **Spring Security Guide:** https://spring.io/projects/spring-security
- **JWT RFC 7519:** https://tools.ietf.org/html/rfc7519
- **OWASP Authorization:** https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- **Kabengo Safaris Project:** See parent project README.md

---

## Support & Questions

For questions about RBAC implementation:
1. Check [RBAC_IMPLEMENTATION.md](./RBAC_IMPLEMENTATION.md) for architecture details
2. Check [DYNAMIC_RBAC_USAGE_GUIDE.md](./DYNAMIC_RBAC_USAGE_GUIDE.md) for usage examples
3. Check [IMPROVEMENTS_ROADMAP.md](./IMPROVEMENTS_ROADMAP.md) for future features
4. Check database schema and run SQL queries to debug
5. Enable DEBUG logging: `logging.level.com.itineraryledger=DEBUG`

---

## Document Maintenance

| Document | Last Updated | Scope |
|----------|--------------|-------|
| README.md | 2025-11-18 | Quick reference, navigation |
| RBAC_IMPLEMENTATION.md | 2025-11-18 | Complete architecture, data model, flows |
| DYNAMIC_RBAC_USAGE_GUIDE.md | 2025-11-18 | How to use system, examples, best practices |
| IMPROVEMENTS_ROADMAP.md | 2025-11-18 | Future enhancements, security gaps, roadmap |

**Version:** 1.0 (2025-11-18)
**Status:** Complete & Production Ready (with noted improvements)
**Audience:** Developers, Architects, DevOps, System Administrators
