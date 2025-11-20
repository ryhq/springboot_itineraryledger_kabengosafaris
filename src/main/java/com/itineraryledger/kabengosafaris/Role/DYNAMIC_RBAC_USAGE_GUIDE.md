# Dynamic RBAC Usage Guide

**Purpose:** Learn how to leverage the RBAC system for dynamic permission management without code changes
**Audience:** Backend developers, system administrators, DevOps engineers
**Last Updated:** November 18, 2025

---

## Table of Contents

1. [Introduction](#introduction)
2. [Creating Roles Dynamically](#creating-roles-dynamically)
3. [Managing Permissions](#managing-permissions)
4. [Assigning Permissions to Roles](#assigning-permissions-to-roles)
5. [Configuring Endpoint Permissions](#configuring-endpoint-permissions)
6. [Method-Level Permission Control](#method-level-permission-control)
7. [Runtime Permission Checks](#runtime-permission-checks)
8. [Advanced Scenarios](#advanced-scenarios)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

---

## Introduction

The Kabengo Safaris RBAC system is designed to be **completely database-driven**. After the application starts, you can:

- ✅ Create and modify roles (except system roles)
- ✅ Create and assign permissions
- ✅ Map endpoints to permissions at runtime
- ✅ Enable/disable permissions and roles without restarting
- ✅ Modify action types for custom workflows

The only code-based configuration is via the `@RequirePermission` annotation on methods (though even this can be bypassed by using endpoint-level permissions).

---

## Creating Roles Dynamically

### Via REST API

Assuming you implement a `RoleController`:

```http
POST /api/admin/roles
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "name": "booking_manager",
  "displayName": "Booking Manager",
  "description": "Manages safari bookings and customer interactions"
}
```

**Response:**
```json
{
  "id": 5,
  "name": "booking_manager",
  "displayName": "Booking Manager",
  "description": "Manages safari bookings and customer interactions",
  "active": true,
  "isSystemRole": false,
  "createdAt": "2025-11-18T10:30:00Z",
  "updatedAt": "2025-11-18T10:30:00Z"
}
```

### Via Java Service

```java
// Inject RoleService
@Autowired
private RoleService roleService;

public void createBookingManagerRole() {
    roleService.createRole(
        "booking_manager",
        "Booking Manager",
        "Manages safari bookings and customer interactions"
    );
}
```

### Via Database SQL

```sql
INSERT INTO roles (name, display_name, description, active, is_system_role, created_at, updated_at)
VALUES (
    'booking_manager',
    'Booking Manager',
    'Manages safari bookings and customer interactions',
    true,
    false,
    NOW(),
    NOW()
);
```

### Properties of a Role

| Property | Type | Mutable | Notes |
|----------|------|---------|-------|
| name | String | ✓ | Unique identifier |
| displayName | String | ✓ | User-friendly name |
| description | String | ✓ | What this role does |
| active | Boolean | ✓ | Soft delete flag |
| isSystemRole | Boolean | ✗ | Cannot delete if true (ADMIN, USER) |
| permissions | Set<Permission> | ✓ | Associated permissions |

### Deactivating a Role

Instead of deleting, mark as inactive:

```http
PATCH /api/admin/roles/{id}/active?active=false
Authorization: Bearer {adminToken}
```

This prevents cascading deletions and allows easy reactivation:

```http
PATCH /api/admin/roles/{id}/active?active=true
Authorization: Bearer {adminToken}
```

---

## Managing Permissions

### System vs Custom Permissions

**System Permissions** (auto-created at startup):
- Created with system action types (CREATE, READ, UPDATE, DELETE, etc.)
- Cannot be deleted (soft deactivation only)
- Tied to system resources (User, Role, Permission)

**Custom Permissions** (user-defined):
- Created at runtime for business domains
- Can be deleted or deactivated
- Linked to custom action types or resources

### Creating a Custom Action Type

Action types define what operations are possible (CREATE, READ, UPDATE, DELETE, APPROVE, etc.)

```http
POST /api/admin/action-types
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "code": "APPROVE",
  "description": "Approves a document or request"
}
```

**Response:**
```json
{
  "id": 11,
  "code": "APPROVE",
  "description": "Approves a document or request",
  "active": true,
  "isSystem": false,
  "createdAt": "2025-11-18T11:00:00Z"
}
```

### Creating a Permission

Permission = Action Type + Resource Type

```http
POST /api/admin/permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "name": "approve_booking",
  "description": "Approve safari bookings from customer queue",
  "category": "bookings",
  "actionCode": "APPROVE",
  "resource": "Booking"
}
```

This creates a permission combining:
- **Action:** APPROVE (what to do)
- **Resource:** Booking (what to do it to)
- **Name:** approve_booking (human-readable identifier)

### Permission Hierarchies

Design permissions following a pattern:

```
Pattern: {action}_{resource}

Examples:
- create_booking, read_booking, update_booking, delete_booking
- approve_safari_package, reject_safari_package
- export_reports, print_reports
- audit_user_access
```

### Bulk Permission Creation

For a new resource, create permissions for all actions:

```java
// BookingPermissionInitializer.java
@Service
public class BookingPermissionInitializer {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionActionService actionService;

    public void initializeBookingPermissions() {
        String[] actions = {"CREATE", "READ", "UPDATE", "DELETE", "APPROVE", "CANCEL"};

        for (String action : actions) {
            String permissionName = action.toLowerCase() + "_booking";

            if (!permissionService.permissionExists(permissionName)) {
                permissionService.createPermission(
                    permissionName,
                    "Allows " + action.toLowerCase() + " operations on bookings",
                    "bookings",
                    action,
                    "Booking"
                );
            }
        }
    }
}
```

Trigger at startup or via API endpoint:

```java
@PostMapping("/api/admin/permissions/initialize-resource")
public ResponseEntity<String> initializeResourcePermissions(@RequestParam String resource) {
    // Implementation
    return ResponseEntity.ok("Permissions created for " + resource);
}
```

---

## Assigning Permissions to Roles

### Method 1: Via REST API

```http
POST /api/admin/roles/{roleId}/permissions/{permissionId}
Authorization: Bearer {adminToken}
```

Example:
```http
POST /api/admin/roles/5/permissions/12
Authorization: Bearer {adminToken}
```

This adds permission "approve_booking" (id=12) to role "booking_manager" (id=5).

### Method 2: Via Java Service

```java
@Autowired
private RoleService roleService;

public void addPermissionToBookingManager() {
    roleService.addPermissionToRole(5, 12);  // (roleId, permissionId)
}
```

### Method 3: Bulk Assignment

Create a permission template:

```json
{
  "roleName": "booking_manager",
  "permissions": [
    "create_booking",
    "read_booking",
    "update_booking",
    "approve_booking",
    "cancel_booking"
  ]
}
```

Implement endpoint:

```java
@PostMapping("/api/admin/roles/permissions/bulk-assign")
public ResponseEntity<Role> bulkAssignPermissions(@RequestBody RolePermissionTemplate template) {
    Role role = roleService.getRoleByName(template.getRoleName());

    for (String permName : template.getPermissions()) {
        Permission perm = permissionService.getPermissionByName(permName);
        roleService.addPermissionToRole(role.getId(), perm.getId());
    }

    return ResponseEntity.ok(role);
}
```

### Removing a Permission from a Role

```http
DELETE /api/admin/roles/{roleId}/permissions/{permissionId}
Authorization: Bearer {adminToken}
```

Example:
```http
DELETE /api/admin/roles/5/permissions/12
Authorization: Bearer {adminToken}
```

This removes "approve_booking" from "booking_manager" role.

### Viewing Role Permissions

```http
GET /api/admin/roles/{roleId}/permissions
Authorization: Bearer {adminToken}
```

Response:
```json
[
  {
    "id": 10,
    "name": "create_booking",
    "description": "Create new safari bookings",
    "category": "bookings",
    "actionType": {
      "id": 1,
      "code": "CREATE",
      "description": "Create operation"
    },
    "resource": "Booking",
    "active": true
  },
  {
    "id": 11,
    "name": "approve_booking",
    "description": "Approve pending bookings",
    "category": "bookings",
    "actionType": {
      "id": 11,
      "code": "APPROVE",
      "description": "Approve operation"
    },
    "resource": "Booking",
    "active": true
  }
]
```

---

## Configuring Endpoint Permissions

### Scenario 1: Protect an Existing Endpoint

Your application has:

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody BookingRequest req) {
        // Create booking
    }
}
```

At runtime, without restarting, register the endpoint permission:

```http
POST /api/admin/endpoint-permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "httpMethod": "POST",
  "endpoint": "/api/bookings",
  "requiredPermissionName": "create_booking",
  "requiresAuth": true,
  "requiresPatternMatching": false,
  "active": true,
  "notes": "Only users with create_booking permission can create bookings"
}
```

From now on, any POST to `/api/bookings` requires the "create_booking" permission.

### Scenario 2: Use Action-Based Permission

Instead of permission name, use action + resource:

```http
POST /api/admin/endpoint-permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "httpMethod": "DELETE",
  "endpoint": "/api/bookings/{id}",
  "actionCode": "DELETE",
  "resourceType": "Booking",
  "requiresAuth": true,
  "active": true
}
```

This requires user to have permission: action="DELETE" AND resource="Booking"

### Scenario 3: Endpoint with Pattern Matching

Protect all park endpoints:

```http
POST /api/admin/endpoint-permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "httpMethod": "GET",
  "endpoint": "/api/parks/.*",
  "requiredPermissionName": "read_park",
  "requiresAuth": true,
  "requiresPatternMatching": true,
  "active": true,
  "notes": "Pattern matches /api/parks/, /api/parks/123, /api/parks/search, etc."
}
```

Pattern notation:
- `/api/parks/.*` → Regex pattern
- `/api/admin/*` → Wildcard (converted to `/api/admin/.*`)
- `/api/bookings/{id}` → Exact match (no pattern)

### Scenario 4: Role-Based Access

Instead of checking permissions, check roles:

```http
POST /api/admin/endpoint-permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "httpMethod": "GET",
  "endpoint": "/api/admin/reports",
  "allowedRoles": "admin,superadmin",
  "requiresAuth": true,
  "active": true,
  "notes": "Only admin or superadmin roles can access reports"
}
```

### Scenario 5: Public Endpoints

Don't require authentication:

```http
POST /api/admin/endpoint-permissions
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "httpMethod": "GET",
  "endpoint": "/api/parks/featured",
  "requiresAuth": false,
  "active": true,
  "notes": "Public endpoint - anyone can view featured parks"
}
```

### Viewing Configured Endpoints

```http
GET /api/admin/endpoint-permissions
Authorization: Bearer {adminToken}
```

Response:
```json
[
  {
    "id": 1,
    "httpMethod": "POST",
    "endpoint": "/api/bookings",
    "requiredPermissionName": "create_booking",
    "actionCode": null,
    "resourceType": null,
    "allowedRoles": null,
    "requiresAuth": true,
    "requiresPatternMatching": false,
    "active": true,
    "createdAt": "2025-11-18T10:00:00Z"
  },
  {
    "id": 2,
    "httpMethod": "GET",
    "endpoint": "/api/parks/.*",
    "requiredPermissionName": "read_park",
    "actionCode": null,
    "resourceType": null,
    "allowedRoles": null,
    "requiresAuth": true,
    "requiresPatternMatching": true,
    "active": true,
    "createdAt": "2025-11-18T11:00:00Z"
  }
]
```

### Finding Endpoints Using a Permission

```http
GET /api/admin/endpoint-permissions/by-permission?permission=create_booking
Authorization: Bearer {adminToken}
```

Returns all endpoints requiring "create_booking".

### Disabling an Endpoint Permission

Instead of deleting (which can cause FK issues), deactivate:

```http
PATCH /api/admin/endpoint-permissions/{id}/active?active=false
Authorization: Bearer {adminToken}
```

The endpoint is now accessible to anyone (unless another filter protects it).

---

## Method-Level Permission Control

### Using @RequirePermission Annotation

For additional protection beyond endpoint level:

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @RequirePermission(permission = "create_booking")
    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody BookingRequest req) {
        // Additional check: endpoint-level AND method-level
        // User must have create_booking permission
    }

    @RequirePermission(action = "DELETE", resource = "Booking")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // User must have DELETE action on Booking resource
    }

    @RequirePermission(
        roles = {"ADMIN", "BOOKING_MANAGER"},
        requireAllRoles = false  // OR logic
    )
    @GetMapping("/reports")
    public ResponseEntity<List<BookingReport>> getReports() {
        // User must have ADMIN or BOOKING_MANAGER role
    }
}
```

### Dynamic Method Protection

For truly dynamic method protection (no @RequirePermission needed):

Use endpoint-level permissions only:

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    // No @RequirePermission annotation
    // Protected entirely by DynamicPermissionFilter
    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody BookingRequest req) {
        // Endpoint permission checked in filter
    }
}
```

Then configure endpoint permissions to control access.

---

## Runtime Permission Checks

### Checking Permissions Programmatically

```java
@Service
public class BookingService {

    @Autowired
    private SecurityContextHolder securityContextHolder;

    public void createBooking(BookingRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        // Check specific permission
        if (!user.hasPermission("create_booking")) {
            throw new AccessDeniedException("User cannot create bookings");
        }

        // Check action-resource permission
        if (!user.hasPermission("CREATE", "Booking")) {
            throw new AccessDeniedException("User cannot CREATE Booking");
        }

        // Check role
        if (!user.hasRole("booking_manager")) {
            throw new AccessDeniedException("Only booking managers can create bookings");
        }

        // If all checks pass, proceed
        bookingRepository.save(new Booking(req));
    }
}
```

### Checking Permissions in Business Logic

```java
@Service
public class BookingApprovalService {

    public void approveBooking(Long bookingId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        // Only ADMIN or users with approve_booking permission can approve
        if (!user.hasRole("ADMIN") && !user.hasPermission("approve_booking")) {
            throw new AccessDeniedException("Cannot approve this booking");
        }

        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovedBy(user);
        booking.setApprovedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }
}
```

### Getting User's Permissions

```java
@Service
public class UserPermissionService {

    public Set<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        Set<String> permissions = new HashSet<>();

        // From all roles
        for (Role role : user.getRoles()) {
            if (role.getActive()) {
                role.getPermissions().stream()
                    .filter(Permission::getActive)
                    .forEach(p -> permissions.add(p.getName()));
            }
        }

        return permissions;
    }

    public Set<String> getUserRoles(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        return user.getRoles().stream()
            .filter(Role::getActive)
            .map(Role::getName)
            .collect(Collectors.toSet());
    }
}
```

---

## Advanced Scenarios

### Scenario 1: Feature Flags via Permissions

Use permissions as feature flags:

```java
@Service
public class BookingService {

    public void exportBooking(Long bookingId, String format) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        // Feature: export bookings (can be enabled/disabled via permission)
        if (!user.hasPermission("export_booking")) {
            throw new AccessDeniedException("Exporting bookings is not available");
        }

        // Proceed with export
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        // ... export logic
    }
}
```

Admin can enable/disable this feature for specific roles without code change:
1. Create permission "export_booking" (if not exists)
2. Add to roles that should have the feature
3. Feature becomes available immediately

### Scenario 2: Conditional Role Assignment

Assign roles based on criteria:

```java
@Service
public class UserRoleService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void promoteToManager(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Role managerRole = roleRepository.findByName("booking_manager").orElseThrow();

        // Add role only if not already present
        if (!user.hasRole("booking_manager")) {
            user.getRoles().add(managerRole);
            userRepository.save(user);

            // Log promotion
            auditService.log("User promoted to manager", user);
        }
    }
}
```

### Scenario 3: Temporary Permission Grant

For time-limited access:

```java
@Service
public class TemporaryPermissionService {

    @Autowired
    private PermissionGrantRepository grantRepository;

    public void grantTemporaryPermission(
        User user,
        Permission permission,
        LocalDateTime expiresAt
    ) {
        PermissionGrant grant = new PermissionGrant();
        grant.setUser(user);
        grant.setPermission(permission);
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);
    }

    public boolean hasTemporaryPermission(User user, String permissionName) {
        return grantRepository
            .findByUserAndPermissionName(user, permissionName)
            .stream()
            .anyMatch(grant -> grant.getExpiresAt().isAfter(LocalDateTime.now()));
    }
}
```

Then check both regular and temporary permissions:

```java
public boolean hasPermissionIncludingTemporary(User user, String permissionName) {
    return user.hasPermission(permissionName) ||
           temporaryPermissionService.hasTemporaryPermission(user, permissionName);
}
```

### Scenario 4: Audit Trail of Permission Changes

Log all permission modifications:

```java
@Service
public class PermissionAuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logRolePermissionChange(
        Role role,
        Permission permission,
        String action,  // "ADDED" or "REMOVED"
        User changedBy
    ) {
        AuditLog log = new AuditLog();
        log.setEntity("RolePermission");
        log.setEntityId(role.getId());
        log.setAction(action);
        log.setDetails("Permission " + permission.getName() + " " + action.toLowerCase());
        log.setChangedBy(changedBy);
        log.setChangedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
```

Use as interceptor:

```java
@Service
public class RoleService {

    @Autowired
    private PermissionAuditService auditService;

    @Autowired
    private SecurityContextHolder securityContextHolder;

    public void addPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        Permission permission = permissionRepository.findById(permissionId).orElseThrow();

        role.getPermissions().add(permission);
        roleRepository.save(role);

        // Audit log
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        auditService.logRolePermissionChange(role, permission, "ADDED", user);
    }
}
```

---

## Best Practices

### 1. Naming Conventions

Follow consistent naming for permissions:

```
{action}_{resource}

Good:
- create_booking
- read_park
- update_safari_package
- delete_itinerary
- approve_payment
- export_reports

Bad:
- booking_create (inconsistent order)
- make_booking (vague verb)
- BOOKING_PERMISSION (too generic)
- bp (abbreviation, unclear)
```

### 2. Use Action Types for CRUD Operations

Create standard action types:

```
CRUD:
- CREATE (Insert new record)
- READ (View record)
- UPDATE (Modify record)
- DELETE (Remove record)

Workflow:
- SUBMIT (Submit for review)
- APPROVE (Approve request)
- REJECT (Reject request)
- CANCEL (Cancel operation)

Special:
- EXPORT (Export data)
- PRINT (Print report)
- EXECUTE (Run script/workflow)
```

Then create permissions combining actions and resources:

```
Permission: create_booking = CREATE + Booking resource
Permission: approve_payment = APPROVE + Payment resource
```

### 3. Role Naming

Use descriptive, domain-specific role names:

```
Good:
- booking_manager (manages bookings)
- park_administrator (administers parks)
- content_editor (edits website content)
- finance_auditor (audits financial records)

Bad:
- admin (too generic)
- user (every role is a "user")
- superuser (implies hierarchy not expressed in system)
- temp (no clarity on scope)
```

### 4. Endpoint Permission Strategy

For public APIs:

```
1. Public endpoints (no auth required):
   - /api/parks/featured (public listing)
   - /api/itineraries/{id} (public view)

2. Authenticated but unrestricted:
   - /api/profile (any logged-in user)
   - /api/bookings/my (user's own bookings)

3. Permission-based:
   - /api/bookings (requires create_booking)
   - /api/payments/approve (requires approve_payment)

4. Admin-only:
   - /api/admin/* (requires admin role)
```

### 5. Caching Awareness

The system caches:

```
Cached:
- PermissionActionService.getAllActiveActions()
- EndpointPermission lookups (by httpMethod + endpoint)
- Pattern compilations (regex patterns)

Not Cached:
- Individual action lookups (ensures freshness)
- Permission checks themselves (computed on-demand)
- Role membership (loaded once per login)
```

When making changes:

```java
// After creating new action type
permissionActionService.clearCache();

// After registering new endpoint permission
endpointPermissionService.clearAllCaches();
```

### 6. Permission Hierarchies

For common workflows:

```
Booking workflow:
- create_booking (required first)
- update_booking (refine before approval)
- approve_booking (management step)
- cancel_booking (final step)

Grant in this order to roles:
- operator: create, update
- manager: create, update, approve
- admin: create, update, approve, cancel
```

### 7. Documenting Permissions

Create internal documentation:

```markdown
## Booking Module Permissions

### create_booking
- **Action:** CREATE
- **Resource:** Booking
- **Roles:** operator, manager, admin
- **Description:** Create new safari bookings from customer requests
- **Usage:** POST /api/bookings
- **Since:** v1.0
- **Notes:** Operators can create; managers must approve

### approve_booking
- **Action:** APPROVE
- **Resource:** Booking
- **Roles:** manager, admin
- **Description:** Approve pending bookings for processing
- **Usage:** POST /api/bookings/{id}/approve
- **Since:** v1.1
- **Notes:** Requires manager+ authority
```

---

## Troubleshooting

### Problem: Permission Check Always Fails

**Symptoms:**
```
User has role "booking_manager"
"booking_manager" has permission "create_booking"
But user.hasPermission("create_booking") returns false
```

**Causes & Solutions:**

1. **Role not active:**
```java
// Check
role.getActive() == false

// Solution
PATCH /api/admin/roles/{roleId}/active?active=true
```

2. **Permission not active:**
```java
// Check
permission.getActive() == false

// Solution
PATCH /api/admin/permissions/{permId}/active?active=true
```

3. **Action type doesn't exist:**
```java
// Check
permission.getActionType() == null

// Solution
Ensure permission.actionType_id is not null in database
DELETE FROM permissions WHERE action_type_id IS NULL;
```

4. **EAGER loading issue:**
```java
// Debug: Print what's loaded
User user = userRepository.findById(userId).orElseThrow();
System.out.println("Roles: " + user.getRoles());  // Should not be null
System.out.println("Role permissions: " + user.getRoles().stream()
    .flatMap(r -> r.getPermissions().stream())
    .toList());  // Should contain your permission
```

### Problem: Endpoint Permission Configuration Not Taking Effect

**Symptoms:**
```
Registered endpoint permission for POST /api/bookings
But requests still succeed without the required permission
```

**Causes & Solutions:**

1. **Endpoint permission is inactive:**
```java
// Check database
SELECT * FROM endpoint_permissions
WHERE http_method='POST' AND endpoint='/api/bookings';

// active column should be true
UPDATE endpoint_permissions SET active=true WHERE id={id};
```

2. **Endpoint not in database (defaults to ALLOW):**
```java
// Current behavior: unknown endpoints are ALLOWED
// Check if your endpoint was registered:
GET /api/admin/endpoint-permissions

// Register if missing:
POST /api/admin/endpoint-permissions
{
  "httpMethod": "POST",
  "endpoint": "/api/bookings",
  "requiredPermissionName": "create_booking",
  "requiresAuth": true,
  "active": true
}
```

3. **Pattern matching issue:**
```
Registered: /api/bookings/.*
Requesting: /api/bookings (no trailing slash)

These don't match! Pattern /api/bookings/.* requires something after /
Use: /api/bookings(.*)? to make trailing path optional
```

4. **Case sensitivity:**
```java
// HTTP method is case-sensitive in matching
Registered: POST (correct)
Registered: post (wrong - database has uppercase)

// Resource comparison is case-insensitive:
resource="Park" matches resource="park" ✓
```

5. **Cache not cleared:**
```java
// After making changes via SQL or API, call:
POST /api/admin/endpoint-permissions/cache/clear

// Or restart application
```

### Problem: JWT Token Validation Fails

**Symptoms:**
```
401 Unauthorized: Invalid token
```

**Causes & Solutions:**

1. **Token expired:**
```
Tokens expire after 3 hours
Solution: Re-login to get new token
```

2. **Token malformed:**
```
Token not in format: Bearer {token}
Check: Authorization header includes "Bearer " prefix
```

3. **Secret key changed:**
```
Application restarted → new secret key generated
Old tokens become invalid
Solution: Re-login
```

4. **Token from wrong issuer:**
```
Token claims may include issuer validation
Ensure issuer matches in JwtTokenProvider
```

### Problem: Permission Check Performance Degradation

**Symptoms:**
```
Response times increase after adding many endpoint permissions
Pattern matching on every request is slow
```

**Causes & Solutions:**

1. **Too many regex patterns:**
```java
// Avoid registering too many patterns like:
/api/users/.*
/api/roles/.*
/api/permissions/.*
// etc.

// Instead use:
/api/admin/.*  // Combine into one pattern
```

2. **Pattern compilation cache full:**
```java
// Clear pattern cache to re-optimize:
POST /api/admin/endpoint-permissions/cache/clear
```

3. **EAGER loading too aggressive:**
```java
// If loading 1000s of permissions per user:
// Consider switching to LAZY loading for roles/permissions
// Then explicitly load as needed

// In User entity:
@ManyToMany(fetch = FetchType.LAZY)  // Changed from EAGER
private Set<Role> roles;
```

### Problem: Cannot Delete System Role/Permission

**Symptoms:**
```
DELETE /api/admin/roles/1  // System ADMIN role
403 Forbidden: Cannot delete system role
```

**Cause:**
System roles have `isSystemRole=true` and are protected.

**Solutions:**

1. **Deactivate instead of delete:**
```
PATCH /api/admin/roles/{id}/active?active=false
```

2. **To truly remove, update database directly:**
```sql
UPDATE roles SET is_system_role=false WHERE id={id};
DELETE FROM roles WHERE id={id};  // Now allowed
```

**Warning:** Only do this if you're sure about the implications!

---

## Summary

The Dynamic RBAC system allows you to:

1. **Create and manage roles** at runtime
2. **Define and assign permissions** without code changes
3. **Configure endpoint access** dynamically via database
4. **Combine multiple authorization models** (name, action-resource, role)
5. **Enable/disable features** via permissions
6. **Audit permission changes** for compliance

Mastering these capabilities makes your application highly flexible and maintainable in production environments.
