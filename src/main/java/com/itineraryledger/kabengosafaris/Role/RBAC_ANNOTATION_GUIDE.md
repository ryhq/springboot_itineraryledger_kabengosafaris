# RBAC Annotation Usage Guide

## Overview

This application uses Spring Security's method-level security annotations to enforce Role-Based Access Control (RBAC). The system is already configured with `@EnableMethodSecurity(prePostEnabled = true)` in [SecurityConfigurations.java](../Configurations/SecurityConfigurations.java:31).

## Key Concepts

### 1. Permission Naming Convention
Permissions follow the `{ACTION}_{ENTITY}` format:
- **Format**: `CREATE_USER`, `READ_ROLE`, `UPDATE_EMAIL_ACCOUNT`, `DELETE_BOOKING`
- **Actions**: CREATE, READ, UPDATE, DELETE, EXECUTE, SUBMIT, AMEND, CANCEL, EXPORT, PRINT
- **Entities**: USER, ROLE, EMAIL_ACCOUNT, EMAIL_EVENT, SECURITY_SETTING, etc.

### 2. Authority Prefixes
The `User.getAuthorities()` method returns authorities with specific prefixes:
- **Permissions**: `PERM_CREATE_USER`, `PERM_READ_ROLE`, `PERM_UPDATE_EMAIL_ACCOUNT`
- **Roles**: `ROLE_SUPERADMIN`, `ROLE_ADMIN`, `ROLE_USER`, `ROLE_GUEST`

### 3. System Roles and Their Permissions

| Role | CREATE | READ | UPDATE | DELETE |
|------|--------|------|--------|--------|
| **SUPERADMIN** | ✅ All | ✅ All | ✅ All | ✅ All |
| **ADMIN** | ✅ All | ✅ All | ✅ All | ❌ None |
| **USER** | ✅ All | ✅ All | ❌ None | ❌ None |
| **GUEST** | ❌ None | ✅ All | ❌ None | ❌ None |

## Using @PreAuthorize Annotations

### Basic Permission Checks

```java
@PreAuthorize("hasAuthority('PERM_CREATE_USER')")
public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {
    // Only users with CREATE_USER permission can execute this
}

@PreAuthorize("hasAuthority('PERM_READ_ROLE')")
public ResponseEntity<?> getAllRoles() {
    // Only users with READ_ROLE permission can execute this
}

@PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_ACCOUNT')")
public ResponseEntity<?> updateEmailAccount(@PathVariable String id) {
    // Only users with UPDATE_EMAIL_ACCOUNT permission can execute this
}

@PreAuthorize("hasAuthority('PERM_DELETE_BOOKING')")
public ResponseEntity<?> deleteBooking(@PathVariable String id) {
    // Only users with DELETE_BOOKING permission can execute this
}
```

### Role-Based Checks

```java
@PreAuthorize("hasRole('SUPERADMIN')")
public ResponseEntity<?> dangerousOperation() {
    // Only SUPERADMIN role can execute this
}

@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public ResponseEntity<?> adminOperation() {
    // Either SUPERADMIN or ADMIN can execute this
}
```

### Combining Multiple Conditions

```java
// User must have BOTH permissions
@PreAuthorize("hasAuthority('PERM_CREATE_USER') and hasAuthority('PERM_UPDATE_USER')")
public ResponseEntity<?> createAndUpdateUser() {
    // Requires both CREATE and UPDATE permissions
}

// User must have AT LEAST ONE permission
@PreAuthorize("hasAuthority('PERM_READ_USER') or hasAuthority('PERM_READ_ROLE')")
public ResponseEntity<?> readUserOrRole() {
    // Can have either READ_USER or READ_ROLE permission
}

// Complex: Role OR specific permission
@PreAuthorize("hasRole('SUPERADMIN') or hasAuthority('PERM_DELETE_USER')")
public ResponseEntity<?> deleteUser() {
    // Either SUPERADMIN role OR DELETE_USER permission
}
```

### Parameter-Based Security

```java
@PreAuthorize("hasAuthority('PERM_UPDATE_USER') and #userId == authentication.principal.id")
public ResponseEntity<?> updateOwnProfile(@PathVariable Long userId) {
    // User must have UPDATE_USER permission AND be updating their own profile
}

@PreAuthorize("hasRole('SUPERADMIN') or (#user.id == authentication.principal.id)")
public ResponseEntity<?> updateUser(@RequestBody User user) {
    // Either SUPERADMIN OR the user is updating themselves
}
```

## Real-World Examples

### Example 1: RoleController (Current Implementation)

```java
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ROLE')")
    public ResponseEntity<ApiResponse<?>> createRole(@RequestBody CreateRoleDTO dto) {
        // Only users with CREATE_ROLE permission (SUPERADMIN, ADMIN, USER)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ROLE')")
    public ResponseEntity<?> getAllRoles() {
        // Any authenticated user (all roles have READ permissions)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<ApiResponse<?>> updateRole(@PathVariable String id) {
        // Only users with UPDATE_ROLE permission (SUPERADMIN, ADMIN)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ROLE')")
    public ResponseEntity<ApiResponse<?>> deleteRole(@PathVariable String id) {
        // Only users with DELETE_ROLE permission (SUPERADMIN only)
    }
}
```

### Example 2: UserController

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_USER')")
    public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {
        // SUPERADMIN, ADMIN, USER can create users
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_USER')")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        // All authenticated users can read
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER') or #id == authentication.principal.username")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserDTO dto) {
        // Either have UPDATE_USER permission OR updating own profile
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_USER')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        // Only SUPERADMIN can delete users
    }

    // Self-service endpoint - no permission check needed
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        // Any authenticated user can view their own profile
        // No @PreAuthorize annotation needed
    }
}
```

### Example 3: Service Layer Security

You can also apply security at the service layer:

```java
@Service
public class EmailAccountService {

    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_ACCOUNT')")
    public EmailAccount createEmailAccount(EmailAccountDTO dto) {
        // Service-level security check
    }

    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_ACCOUNT')")
    public void deleteEmailAccount(Long id) {
        // Only SUPERADMIN can delete email accounts
    }
}
```

## Common Patterns

### Pattern 1: CRUD Operations

```java
// CREATE - Usually SUPERADMIN, ADMIN, USER
@PreAuthorize("hasAuthority('PERM_CREATE_ENTITY')")

// READ - Usually all authenticated users
@PreAuthorize("hasAuthority('PERM_READ_ENTITY')")

// UPDATE - Usually SUPERADMIN, ADMIN
@PreAuthorize("hasAuthority('PERM_UPDATE_ENTITY')")

// DELETE - Usually SUPERADMIN only
@PreAuthorize("hasAuthority('PERM_DELETE_ENTITY')")
```

### Pattern 2: Self-Service with Fallback

```java
// User can update their own data OR have UPDATE permission
@PreAuthorize("hasAuthority('PERM_UPDATE_USER') or #userId == authentication.principal.id")
public void updateUser(Long userId, UserDTO dto) {
    // Allows both admin updates and self-updates
}
```

### Pattern 3: Admin-Only Operations

```java
// Only SUPERADMIN or ADMIN roles
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public void adminOnlyOperation() {
    // High-privilege operation
}
```

## Error Handling

When a user lacks the required permission, Spring Security throws an `AccessDeniedException`:

```json
{
  "timestamp": "2025-12-26T10:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/roles"
}
```

You can customize this by adding an `@ExceptionHandler`:

```java
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiResponse.error(
                "You do not have permission to perform this action",
                "ACCESS_DENIED",
                HttpStatus.FORBIDDEN.value()
            )
        );
    }
}
```

## Testing

### Test with Different Roles

```java
@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = {"PERM_CREATE_ROLE"})
    void testCreateRole_withPermission_success() throws Exception {
        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Test Role\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"PERM_READ_ROLE"}) // No CREATE permission
    void testCreateRole_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Test Role\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SUPERADMIN", "PERM_DELETE_ROLE"})
    void testDeleteRole_asSuperAdmin_success() throws Exception {
        mockMvc.perform(delete("/api/roles/encoded_id"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GUEST", "PERM_READ_ROLE"})
    void testDeleteRole_asGuest_forbidden() throws Exception {
        mockMvc.perform(delete("/api/roles/encoded_id"))
                .andExpect(status().isForbidden());
    }
}
```

## Best Practices

1. **Use Permission Checks for Resource Operations**
   - Prefer `hasAuthority('PERM_CREATE_USER')` over `hasRole('ADMIN')`
   - Permissions are more granular and flexible

2. **Use Role Checks for High-Level Operations**
   - Use `hasRole('SUPERADMIN')` for extremely sensitive operations
   - Example: system configuration, global settings

3. **Apply Security at Controller Level**
   - Makes security explicit and visible in API documentation
   - Easier to audit and review

4. **Consider Service Layer Security for Complex Logic**
   - When business logic requires security checks
   - When multiple controllers use the same service

5. **Document Required Permissions**
   - Add comments or Javadoc about required permissions
   - Helps frontend developers know what to expect

6. **Test Security Thoroughly**
   - Test both authorized and unauthorized access
   - Test edge cases and permission combinations

## Adding New Permissions

When you add a new entity to the system:

1. **Add permissions in PermissionInitializer.java**
   ```java
   createPermissionIfNotExists("CREATE_BOOKING", ...);
   createPermissionIfNotExists("READ_BOOKING", ...);
   createPermissionIfNotExists("UPDATE_BOOKING", ...);
   createPermissionIfNotExists("DELETE_BOOKING", ...);
   ```

2. **System roles automatically receive permissions**
   - RoleInitializer will update roles on next startup
   - SUPERADMIN gets all 4 permissions
   - ADMIN gets CREATE, READ, UPDATE
   - USER gets CREATE, READ
   - GUEST gets READ

3. **Add @PreAuthorize to your controller**
   ```java
   @PostMapping("/api/bookings")
   @PreAuthorize("hasAuthority('PERM_CREATE_BOOKING')")
   public ResponseEntity<?> createBooking() { ... }
   ```

4. **Restart the application**
   - Permissions are created
   - Roles are updated
   - Ready to use!

## Configuration Reference

The RBAC system is enabled in [SecurityConfigurations.java](../Configurations/SecurityConfigurations.java):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ← This enables @PreAuthorize
public class SecurityConfigurations {
    // Security configuration...
}
```

The authority mapping is done in [User.java](../User/User.java:129-150):

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();

    // Add roles with ROLE_ prefix
    roles.stream()
        .filter(Role::getActive)
        .forEach(role ->
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()))
        );

    // Add permissions with PERM_ prefix
    roles.stream()
        .filter(Role::getActive)
        .flatMap(role -> role.getPermissions().stream())
        .filter(Permission::getActive)
        .forEach(permission ->
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getName()))
        );

    return authorities;
}
```

## Related Documentation

- [Permission System Guide](../Permission/PERMISSION_SYSTEM_GUIDE.md)
- [Dynamic RBAC Usage Guide](../Role/DYNAMIC_RBAC_USAGE_GUIDE.md)
- [Role API Documentation](../Role/ROLE_API_DOCUMENTATION.md)
- [Spring Security Method Security Documentation](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
