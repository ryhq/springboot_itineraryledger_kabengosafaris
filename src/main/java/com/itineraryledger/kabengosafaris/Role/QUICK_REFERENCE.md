# RBAC Quick Reference

A one-page quick reference for the RBAC system.

---

## System Architecture

```
HTTP Request
    ↓
┌─────────────────────────────────┐
│ DynamicPermissionFilter         │  Layer 2: Endpoint-level
│ (Checks URL permissions)        │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ JwtAuthenticationFilter         │  Layer 1: Authentication
│ (Extracts & validates JWT)      │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ @RequirePermission Aspect       │  Layer 3: Method-level
│ (Checks method permissions)     │
└─────────────────────────────────┘
    ↓
Controller Method
```

---

## Entity Model

```
User (1) ──many-to-many──> Role (1) ──many-to-many──> Permission
                                                             │
                                                            FK
                                                             ↓
                                                    PermissionActionType
                                                    (CREATE, READ, etc.)

EndpointPermission: Maps URL → Permission
```

---

## Core Concepts

| Concept | Purpose | Example |
|---------|---------|---------|
| **User** | Individual system user | john, jane |
| **Role** | Logical group of permissions | admin, manager, operator |
| **Permission** | Specific access right | create_booking, read_park |
| **Action Type** | What operation | CREATE, READ, UPDATE, DELETE |
| **Resource** | What entity | Booking, Park, User |
| **Endpoint Permission** | URL access mapping | POST /api/bookings → create_booking |

---

## Permission Models

### 1. Permission Name
```
@RequirePermission(permission = "create_booking")
// Check: user.hasPermission("create_booking")
```

### 2. Action + Resource
```
@RequirePermission(action = "CREATE", resource = "Booking")
// Check: user.hasPermission("CREATE", "Booking")
```

### 3. Role-Based
```
@RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
// Check: user.hasRole("ADMIN") OR user.hasRole("MANAGER")
```

---

## Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| **User** | User/ | User entity with permission methods |
| **Role** | Role/ | Role entity with permission checks |
| **Permission** | Permission/ | Permission entity |
| **PermissionActionType** | Permission/ | Action type definitions |
| **EndpointPermission** | Permission/ | URL-to-permission mapping |
| **RoleService** | Role/ | Role CRUD operations |
| **PermissionService** | Permission/ | Permission CRUD operations |
| **EndpointPermissionService** | Permission/ | Endpoint permission logic |
| **DynamicPermissionFilter** | Security/ | Servlet filter for endpoints |
| **JwtTokenProvider** | Security/ | JWT generation & validation |
| **PermissionCheckAspect** | Security/ | AOP enforcement |

---

## Common REST Endpoints

### Role Management
```bash
POST   /api/admin/roles                          # Create role
GET    /api/admin/roles                          # List roles
GET    /api/admin/roles/{id}                     # Get role
PUT    /api/admin/roles/{id}                     # Update role
PATCH  /api/admin/roles/{id}/active?active=true # Activate
DELETE /api/admin/roles/{id}                     # Delete role

POST   /api/admin/roles/{roleId}/permissions/{permId}      # Add permission
DELETE /api/admin/roles/{roleId}/permissions/{permId}      # Remove permission
GET    /api/admin/roles/{roleId}/permissions               # List permissions
```

### Permission Management
```bash
POST   /api/admin/permissions                    # Create permission
GET    /api/admin/permissions                    # List permissions
GET    /api/admin/permissions/{id}               # Get permission
PUT    /api/admin/permissions/{id}               # Update permission
PATCH  /api/admin/permissions/{id}/active?active=true # Activate
DELETE /api/admin/permissions/{id}               # Delete permission
```

### Action Types
```bash
POST   /api/admin/action-types                   # Create action
GET    /api/admin/action-types                   # List actions
PATCH  /api/admin/action-types/{id}/active?active=true # Activate
```

### Endpoint Permissions
```bash
POST   /api/admin/endpoint-permissions                    # Register endpoint
GET    /api/admin/endpoint-permissions                    # List endpoints
GET    /api/admin/endpoint-permissions/by-permission?perm=X  # By permission
PATCH  /api/admin/endpoint-permissions/{id}/active       # Enable/disable
POST   /api/admin/endpoint-permissions/cache/clear       # Clear cache
```

### User Management
```bash
GET    /api/admin/users/{userId}/permissions    # Get user permissions
GET    /api/admin/users/{userId}/roles          # Get user roles
POST   /api/admin/users/{userId}/roles/{roleId} # Assign role
```

---

## Code Examples

### Check Permission (Code)
```java
User user = (User) SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();

if (user.hasPermission("create_booking")) {
    // Allowed
} else {
    throw new AccessDeniedException("Cannot create booking");
}
```

### Check Permission (Annotation)
```java
@RequirePermission(permission = "create_booking")
@PostMapping("/bookings")
public ResponseEntity<Booking> create(@RequestBody CreateBookingRequest req) {
    // Only executed if user has permission
}
```

### Create Role via API
```bash
curl -X POST http://localhost:8080/api/admin/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "booking_manager",
    "displayName": "Booking Manager",
    "description": "Manages bookings"
  }'
```

### Register Endpoint Permission
```bash
curl -X POST http://localhost:8080/api/admin/endpoint-permissions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "httpMethod": "POST",
    "endpoint": "/api/bookings",
    "requiredPermissionName": "create_booking",
    "requiresAuth": true,
    "active": true
  }'
```

---

## Permission Check Flow

```
User makes request
    ↓
DynamicPermissionFilter
├─ Is path public? (auth/, swagger) → ALLOW
├─ Get user from SecurityContext
├─ Lookup endpoint_permissions table
│  ├─ Exact match found?
│  ├─ Pattern match found?
│  └─ Not found → ALLOW by default (⚠️ risky)
└─ Check permission:
   ├─ permission name → user.hasPermission(name)
   ├─ action+resource → user.hasPermission(action, resource)
   └─ roles → user.hasRole(roleName)
    ↓
Result: ALLOW (200) or DENY (403)
```

---

## Database Queries

### Get user's permissions
```sql
SELECT DISTINCT p.name FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
JOIN roles r ON rp.role_id = r.id
JOIN user_roles ur ON r.id = ur.role_id
JOIN users u ON ur.user_id = u.id
WHERE u.username = 'john' AND r.active = true AND p.active = true;
```

### Get users with a permission
```sql
SELECT DISTINCT u.username FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE p.name = 'create_booking' AND r.active = true AND p.active = true;
```

### Get endpoints requiring a permission
```sql
SELECT http_method, endpoint FROM endpoint_permissions
WHERE required_permission_name = 'create_booking' AND active = true;
```

### Check role has permission
```sql
SELECT COUNT(*) FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.id
WHERE rp.role_id = {roleId} AND p.name = 'permission_name' AND p.active = true;
```

---

## Troubleshooting Checklist

**User can't access protected endpoint:**
- [ ] Is JWT token valid? (check expiration: 3 hours)
- [ ] Is user role active? (check `roles.active = true`)
- [ ] Does role have permission? (check `role_permissions` table)
- [ ] Is permission active? (check `permissions.active = true`)
- [ ] Is action type active? (check `permission_action_types.active = true`)
- [ ] Is endpoint permission registered? (check `endpoint_permissions` table)
- [ ] Is endpoint permission active? (check `endpoint_permissions.active = true`)

**Endpoint permission not taking effect:**
- [ ] Endpoint registered in database?
- [ ] Endpoint active in database?
- [ ] Cache stale? → Call `POST /api/admin/endpoint-permissions/cache/clear`
- [ ] URL pattern correct? (e.g., `/api/bookings/.*` vs `/api/bookings/`)

**System role can't be deleted:**
- [ ] System roles have `is_system_role = true` (ADMIN, USER)
- [ ] Soft delete instead: `PATCH /api/admin/roles/{id}/active?active=false`

---

## System Permissions (Auto-Created)

| Code | Description | Used For |
|------|-------------|----------|
| CREATE | Create new records | create_* permissions |
| READ | View records | read_* permissions |
| UPDATE | Modify records | update_* permissions |
| DELETE | Remove records | delete_* permissions |
| SUBMIT | Submit document | submit_* permissions |
| APPROVE | Approve request | approve_* permissions |
| CANCEL | Cancel operation | cancel_* permissions |
| EXPORT | Export data | export_* permissions |
| PRINT | Print report | print_* permissions |
| EXECUTE | Execute action | execute_* permissions |

---

## System Roles (Auto-Created)

| Name | isSystemRole | Protected |
|------|--------------|-----------|
| ADMIN | true | Cannot delete |
| USER | true | Cannot delete |

---

## Configuration

**JWT Settings:**
```properties
jwt.expiration=10800000         # 3 hours in milliseconds
jwt.secret=${JWT_SECRET}        # Load from environment
```

**RBAC Behavior:**
```properties
rbac.default-endpoint-behavior=allow  # allow or deny (currently allow)
```

---

## File Locations

```
src/main/java/com/itineraryledger/kabengosafaris/

Role/
  ├── Role.java
  ├── RoleService.java
  ├── RoleRepository.java
  └── RBAC_IMPLEMENTATION.md          ← Full documentation

Permission/
  ├── Permission.java
  ├── PermissionService.java
  ├── PermissionActionType.java
  ├── PermissionActionService.java
  ├── EndpointPermission.java
  ├── EndpointPermissionService.java
  └── EndpointPermissionController.java

Security/
  ├── RequirePermission.java
  ├── PermissionCheckAspect.java
  ├── DynamicPermissionFilter.java
  ├── JwtTokenProvider.java
  ├── JwtAuthenticationFilter.java
  └── CustomUserDetailsService.java

Configurations/
  ├── SecurityConfigurations.java
  └── DataInitializationService.java

User/
  ├── User.java
  └── UserRepository.java
```

---

## Performance Notes

- **Caching:** Action types and endpoint permissions are cached
- **EAGER Loading:** Roles and permissions loaded immediately for permission checks
- **Pattern Matching:** Regex patterns compiled and cached on first use
- **JWT:** Validated using HMAC-SHA256 (fast)
- **N+1 Prevention:** EAGER loading prevents DB queries in loops

---

## Security Considerations

**Current:**
- ✅ JWT-based authentication
- ✅ BCRYPT password hashing
- ✅ Multi-layered authorization
- ✅ System role protection

**Needs Improvement:**
- ⚠️ JWT secret externalization
- ⚠️ Token revocation (blacklist)
- ⚠️ Default-allow behavior (should default-deny)
- ⚠️ No refresh token mechanism
- ⚠️ No row-level security

See `IMPROVEMENTS_ROADMAP.md` for detailed enhancement proposals.

---

## Quick Wins (Easiest Improvements)

1. **Externalize JWT Secret** (1 hour)
   - Move secret from code to environment variable

2. **Implement Token Refresh** (2 hours)
   - Add `/api/auth/refresh` endpoint

3. **Default Deny Mode** (1 hour)
   - Add configuration for unknown endpoints

4. **Audit Logging** (3 hours)
   - Log permission denials to database

5. **Permission Testing Helper** (2 hours)
   - Create test utility for mocking users

See `IMPROVEMENTS_ROADMAP.md` for full roadmap with priorities.

---

**Last Updated:** 2025-11-18
**For detailed info:** See RBAC_IMPLEMENTATION.md, DYNAMIC_RBAC_USAGE_GUIDE.md, IMPROVEMENTS_ROADMAP.md
