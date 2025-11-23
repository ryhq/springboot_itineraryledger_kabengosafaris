# Kabengosafaris Security Module - Complete Guide

## Overview

The Security module provides a comprehensive, database-driven security framework for the Kabengosafaris application. It includes JWT authentication, ID obfuscation, permission management, and dynamic endpoint security with configurable settings stored in the database.

**Key Features:**
- JWT token-based authentication with configurable expiration
- Database-driven permission and role management
- Dynamic endpoint permission enforcement via filters
- ID obfuscation to hide internal sequences
- Strong password generation
- Configurable security settings without application restart
- Method-level access control via annotations
- Account lockout with enable/disable control
- Login rate limiting with configurable token bucket algorithm
- Password policy support with configurable constraints

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Request                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────┐
        │  JwtAuthenticationFilter         │
        │  - Extract JWT from header       │
        │  - Validate token                │
        │  - Load user details             │
        │  - Set security context          │
        └──────────┬───────────────────────┘
                   │
                   ▼
        ┌──────────────────────────────────┐
        │  DynamicPermissionFilter         │
        │  - Check endpoint permissions    │
        │  - Validate user authorization   │
        │  - Enforce database-driven rules │
        └──────────┬───────────────────────┘
                   │
                   ▼
        ┌─────────────────────────────────────┐
        │  Controller/Method                  │
        │  (@RequirePermission optional)      │
        │  - PermissionCheckAspect intercepts │
        │  - Additional permission checks     │
        └──────────┬──────────────────────────┘
                   │
                   ▼
        ┌──────────────────────────────────┐
        │  Response                        │
        └──────────────────────────────────┘
```

---

## Components

### 1. JWT Token Provider (`JwtTokenProvider`)

Manages JWT token generation, validation, and extraction.

**Key Features:**
- Generate tokens from authentication
- Validate token signatures and expiration
- Extract username from tokens
- Database-driven expiration time configuration
- Fallback to `application.properties` if database unavailable


**Configuration Keys:**
- `jwt.expiration.time.minutes` - Token expiration in minutes (default: 180)
- `jwt.refresh.expiration.time.minutes` - Refresh token expiration (default: 10080) 

---

### 2. JWT Authentication Filter (`JwtAuthenticationFilter`)

Intercepts HTTP requests and authenticates users via JWT tokens.

**How It Works:**
1. Extracts JWT from `Authorization: Bearer {token}` header
2. Validates token signature and expiration
3. Loads user details from `UserRepository`
4. Sets Spring Security context with authenticated user

**Automatic Processing:**
- Applied to all endpoints except those in `PUBLIC_PATHS`
- No manual configuration needed - works automatically with JWT in header

---

### 3. Custom User Details Service (`CustomUserDetailsService`)

Loads user details from the database for authentication.

**Key Features:**
- Implements Spring's `UserDetailsService` interface
- Loads `User` entity from database
- Throws `UsernameNotFoundException` if user doesn't exist

**User Entity Requirements:**
Your `User` class must implement `UserDetails` and provide:
- `getUsername()` - unique identifier
- `getPassword()` - encrypted password
- `getAuthorities()` - list of granted authorities (roles/permissions)
- `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isEnabled()`

---

### 4. Dynamic Permission Filter (`DynamicPermissionFilter`)

Enforces database-driven endpoint permissions without annotations.

**Key Features:**
- Checks endpoint permissions from `endpoint_permissions` database table
- Supports exact path and regex pattern matching
- Allows public endpoints to bypass checks
- Returns 403 Forbidden with detailed error messages
- Logs all access decisions for audit trails

**Public Paths (Always Allowed):**
```
/api/auth/*
/swagger-ui/*
/v3/api-docs/*
/actuator/*
/health
```

**How Permission Lookup Works:**
1. Request comes in (HTTP method + path)
2. Filter checks `endpoint_permissions` table
3. Tries exact match first, then regex patterns
4. If endpoint found: validates user has required permission/role
5. If endpoint not found: allows by default (configurable)
6. Returns 403 if access denied

**Database Table Structure:**
```sql
-- Required table in your database
CREATE TABLE endpoint_permissions (
    id BIGINT PRIMARY KEY,
    http_method VARCHAR(10),          -- GET, POST, PUT, DELETE, etc.
    path_pattern VARCHAR(255),         -- /api/safaris or regex pattern
    is_regex BOOLEAN,
    permission_type VARCHAR(50),       -- PERMISSION_NAME or ACTION_RESOURCE
    permission_name VARCHAR(255),      -- e.g., "view_safaris"
    action_code VARCHAR(50),           -- e.g., "read", "create"
    resource_type VARCHAR(100),        -- e.g., "Safari"
    requires_authentication BOOLEAN,
    created_at TIMESTAMP
);
```

**No Code Required:**
Simply configure permissions in the database and they're enforced automatically!

---

### 5. Permission Check Aspect (`PermissionCheckAspect`)

AOP aspect that enforces method-level permissions using `@RequirePermission` annotation.

**Features:**
- Runs before method execution
- Supports role-based and permission-based access control
- Database-driven action code validation
- Detailed logging for audit trails

**Usage Examples:**

**Example 1: Role-Based Access**
```java
@RequirePermission(roles = {"ADMIN"})
public void deleteUser(Long userId) {
    // Only users with ADMIN role can execute this
}

@RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
public void approveBooking(Long bookingId) {
    // Users with ADMIN OR MANAGER role can execute this
}
```

**Example 2: Permission Name**
```java
@RequirePermission(permission = "create_booking")
public Booking createBooking(Booking booking) {
    // Only users with "create_booking" permission
}
```

**Example 3: Action + Resource**
```java
@RequirePermission(action = "delete", resource = "Safari")
public void deleteSafari(Long safariId) {
    // User must have "delete" action on "Safari" resource
    // Action codes come from permission_action_types table
}
```

**Annotation Parameters:**
- `permission` (String) - specific permission name (takes precedence)
- `action` (String) - action code from database (default: "read")
- `resource` (String) - resource/document type
- `roles` (String[]) - role names to check
- `requireAllRoles` (boolean) - AND (all) vs OR (any) logic for roles
- `description` (String) - documentation of why permission required

**User Methods Called:**
The aspect expects your `User` entity to have:
```java
// Check specific permission
boolean hasPermission(String permissionName);

// Check action + resource permission
boolean hasPermission(String actionCode, String resourceType);

// Check role
boolean hasRole(String roleName);
```

---

### 6. Security Settings Management

#### SecuritySettings Entity (`SecuritySettings`)

Database entity storing all configurable security settings.

**Features:**
- Dynamic configuration without restart
- Type conversion (String, Integer, Boolean, Long, Double)
- Category organization (JWT, OBFUSCATION, PASSWORD, etc.)
- System default protection (cannot delete)
- Active/inactive status (soft delete)
- Restart requirement flag

**Database Table:**
```sql
CREATE TABLE security_settings (
    id BIGINT PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(50),                    -- STRING, INTEGER, BOOLEAN, LONG, DOUBLE
    description TEXT,
    active BOOLEAN DEFAULT true,
    is_system_default BOOLEAN DEFAULT false,
    category VARCHAR(50),                     -- JWT, OBFUSCATION, PASSWORD, etc.
    requires_restart BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### SecuritySettingsService (`SecuritySettingsService`)

Service for managing security settings.

**Key Methods:**

```java
// Get setting values with type conversion
String getSettingValue(String settingKey);
Integer getSettingValueAsInteger(String settingKey);
Long getSettingValueAsLong(String settingKey);
Boolean getSettingValueAsBoolean(String settingKey);
Double getSettingValueAsDouble(String settingKey);

// Get full setting object
SecuritySettings getSetting(String settingKey);

// Create/update settings
SecuritySettings createSetting(String key, String value, DataType, description, category);
SecuritySettings updateSettingValue(String settingKey, String newValue);
SecuritySettings saveSetting(SecuritySettings setting);

// Manage settings lifecycle
void deactivateSetting(String settingKey);              // soft delete
void reactivateSetting(String settingKey);
void deleteSetting(String settingKey);                  // hard delete (not for system defaults)
void resetToDefault(String settingKey);

// Query settings
List<SecuritySettings> getAllActiveSettings();
List<SecuritySettings> getActiveSettingsByCategory(String category);
List<SecuritySettings> getAllSystemDefaults();
List<SecuritySettings> getSettingsThatRequireRestart();
boolean settingExists(String settingKey);

// Convenience methods for common settings
long getJwtExpirationTimeMillis();
int getIdObfuscationLength();
int getIdObfuscationSaltLength();
boolean isIdObfuscationEnabled();
int getAccountLockoutMaxAttempts();
int getAccountLockoutDurationMinutes();
boolean isAccountLockoutEnabled();
```


#### SecuritySettingsController (`SecuritySettingsController`)

REST API for managing security settings (admin only).

**Endpoints:**

```
GET    /api/security-settings                      - Get all active settings
GET    /api/security-settings/{settingKey}         - Get specific setting
GET    /api/security-settings/category/{category}  - Get settings by category
GET    /api/security-settings/defaults/all         - Get all system defaults
GET    /api/security-settings/{settingKey}/value   - Get setting value only
GET    /api/security-settings/info/requires-restart - Get settings needing restart
GET    /api/security-settings/{settingKey}/exists  - Check if setting exists
GET    /api/security-settings/health/status        - Health check endpoint

POST   /api/security-settings                      - Create new setting
POST   /api/security-settings/{settingKey}/reactivate - Reactivate setting
POST   /api/security-settings/{settingKey}/reset   - Reset to system default

PUT    /api/security-settings/{settingKey}        - Update setting value
PUT    /api/security-settings/{settingKey}/full   - Update full setting

DELETE /api/security-settings/{settingKey}        - Deactivate setting (soft delete)
```

#### SecuritySettingsInitializer (`SecuritySettingsInitializer`)

Application startup component that initializes default security settings.

**Features:**
- Runs at application startup with highest priority
- Creates default security settings from `application.properties`
- Preserves existing database settings (doesn't overwrite)
- Initializes JWT, Obfuscation, Password, and Account Lockout settings

**What It Does:**
1. Loads default values from `application.properties`
2. Checks if settings exist in database
3. Creates missing settings as system defaults
4. System defaults cannot be deleted

**No Action Required:**
The initializer runs automatically and ensures your database has all required settings.

#### SecuritySettingsProperties (`SecuritySettingsProperties`)

Spring Configuration Properties class for type-safe property binding.

**Usage:**

```java
@Component
public class MyComponent {
    @Autowired
    private SecuritySettingsProperties securityProps;

    public void checkSecurityConfig() {
        long jwtExpiration = securityProps.getJwt().getExpirationTimeMinutes();
        int obfuscationLength = securityProps.getObfuscation().getObfuscatedIdLength();
        boolean passwordRequiresUppercase = securityProps.getPassword().isRequireUppercase();
        int maxLockoutAttempts = securityProps.getAccountLockout().getMaxFailedAttempts();
    }
}
```

**Property Structure:**
```
security:
  jwt:
    expirationTimeMinutes: 180
    refreshExpirationTimeMinutes: 10080
    secretKey: "your-secret-key"
    algorithm: "HS256"
    issuer: "kabengosafaris"
  obfuscation:
    obfuscatedIdLength: 70
    saltLength: 21
    enabled: true
    algorithm: "hashids"
  password:
    minLength: 8
    maxLength: 128
    requireUppercase: true
    requireLowercase: true
    requireNumbers: true
    requireSpecialCharacters: true
    expirationDays: 90
    historyCount: 5
  accountLockout:
    maxFailedAttempts: 5
    lockoutDurationMinutes: 30
    enabled: true
    counterResetHours: 24
```

---

### 7. ID Obfuscator (`IdObfuscator`)

Encodes numeric IDs into hash strings to hide internal ID sequences from API responses.

**Features:**
- Uses Hashids library for reversible encoding
- Configurable hash length and salt via database settings
- Protects database structure from enumeration attacks
- Fallback to `application.properties` if database unavailable


**Configuration Keys:**
- `idObfuscator.obfuscated.length` - Length of hash string (default: 70)
- `idObfuscator.salt.length` - Salt length for obfuscation (default: 21)

**Methods:**

```java
// Encode numeric ID to hash
String encodeId(Long id);

// Decode hash back to numeric ID
Long decodeId(String hash);
```

**Example:**
```java
Long id = 12345L;
String hash = idObfuscator.encodeId(id);        // "krx5LpoJvOr7XzNQ2Pqw..."
Long decodedId = idObfuscator.decodeId(hash);   // 12345L
```

---

### 8. Strong Password Generator (`StrongPasswordGenerator`)

Utility for generating cryptographically secure passwords.

**Features:**
- Includes uppercase, lowercase, numbers, and special characters
- Minimum 8 characters (configurable)
- Shuffled for unpredictability
- Uses `SecureRandom` for security

**Method:**
```java
// Generate password with specified length (minimum 8)
static String generateStrongPassword(int length);
```

**Character Sets:**
- Uppercase: A-Z
- Lowercase: a-z
- Digits: 0-9
- Special: !@#$%^&*()-_+=<>?

---

### 9. Account Lockout Policy (`AccountMaintenanceScheduledService`, `LoginService`)

Protects against brute-force attacks by locking accounts after multiple failed login attempts.

**Features:**
- Configurable maximum failed attempts before lockout
- Automatic unlock after configurable duration
- Failed attempt counter reset after inactivity period
- **Enable/Disable Control**: Policy can be toggled on/off via database setting
- Background scheduled task for automatic maintenance

**Configuration Keys:**
- `accountLockout.enabled` - Enable/disable the account lockout policy (default: true)
- `accountLockout.maxFailedAttempts` - Max failed attempts before lock (default: 5)
- `accountLockout.lockoutDurationMinutes` - How long account stays locked (default: 30)
- `accountLockout.counterResetHours` - Hours of inactivity before counter resets (default: 24, 0=never)

**How It Works:**
1. User fails authentication
2. If lockout is **enabled**: failed attempt counter increments
3. When counter reaches maxFailedAttempts: account is locked
4. User cannot login until lockout duration expires
5. Scheduled task (every 5 minutes) automatically unlocks expired locks
6. Counter resets after counterResetHours of inactivity (scheduled task)

**When Lockout is Disabled:**
- Failed attempts are NOT tracked
- Account locking is bypassed
- Users can attempt unlimited logins (rate limiting still applies if enabled)
- Useful for development/testing environments

**Code References:**
- Login check: [LoginService.java:80-88](LoginService.java#L80-L88)
- Failed attempt handler: [LoginService.java:127-137](LoginService.java#L127-L137)
- Auto-unlock task: [AccountMaintenanceScheduledService.java:42-69](AccountMaintenanceScheduledService.java#L42-L69)
- Counter reset task: [AccountMaintenanceScheduledService.java:80-110](AccountMaintenanceScheduledService.java#L80-L110)

---

### 10. Login Rate Limiting (`LoginRateLimitingService`)

Implements token bucket algorithm to prevent brute-force attacks by limiting login attempt frequency.

**Features:**
- Per-user/email rate limiting
- Token bucket algorithm for flexible rate control
- Configurable burst capacity and refill rate
- **Enable/Disable Control**: Can be toggled on/off via database setting
- Graceful degradation on errors (fail-open)

**Configuration Keys:**
- `loginAttempts.enabled` - Enable/disable rate limiting (default: true)
- `loginAttempts.maxCapacity` - Initial tokens available for login attempts (default: 5)
- `loginAttempts.refillRate` - Tokens to add during refill (default: 5)
- `loginAttempts.refillDurationMinutes` - Refill interval in minutes (default: 1)

**Token Bucket Algorithm Explained:**
```
Initial State: 5 tokens
├─ Attempt 1: Consume 1 token (4 remaining)
├─ Attempt 2: Consume 1 token (3 remaining)
├─ Attempt 3: Consume 1 token (2 remaining)
├─ Attempt 4: Consume 1 token (1 remaining)
├─ Attempt 5: Consume 1 token (0 remaining)
├─ Attempt 6: DENIED - No tokens available
└─ After 1 minute (refill): 5 tokens restored (capped at maxCapacity)
```

**When Rate Limiting is Disabled:**
- All login attempts are allowed (no token consumption)
- No rate limit exceptions thrown
- Useful for testing/development or high-trust environments
- Account lockout policy still applies if enabled

**Code References:**
- Rate limit check: [LoginService.java:50-61](LoginService.java#L50-L61)
- Token bucket implementation: [LoginRateLimitingService.java:42-89](LoginRateLimitingService.java#L42-L89)

---

## Complete Configuration Guide

### application.properties

```properties
# JWT Configuration
security.jwt.expiration.time.minutes=180
security.jwt.refresh.expiration.time.minutes=10080

# ID Obfuscation Configuration
security.idObfuscator.obfuscated.length=70
security.idObfuscator.salt.length=21

# Password Policy Configuration
security.password.min.length=8
security.password.max.length=128
security.password.require.uppercase=true
security.password.require.lowercase=true
security.password.require.numbers=true
security.password.require.special.characters=true
security.password.expiration.days=90

# Account Lockout Configuration
security.account-lockout.max-failed-attempts=5
security.account-lockout.lockout-duration-minutes=30
security.account-lockout.counter-reset-hours=24
security.account-lockout.enabled=true

# Login Rate Limit Configuration
security.login-rate-limit.max-capacity=5
security.login-rate-limit.refill-rate=5
security.login-rate-limit.refill-duration-minutes=1
security.login-rate-limit.enabled=true
```

---

## Implementation Workflow

### Step 1: Setup Security in Application

```java
@SpringBootApplication
@EnableAspectJAutoProxy  // Enable @RequirePermission AOP
public class KabengosafarisApplication {
    public static void main(String[] args) {
        SpringApplication.run(KabengosafarisApplication.class, args);
    }
}
```

### Step 2: Configure Spring Security (example)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DynamicPermissionFilter dynamicPermissionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(dynamicPermissionFilter, JwtAuthenticationFilter.class)
            .httpBasic().disable();

        return http.build();
    }
}
```

### Step 3: Implement User Entity

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @ManyToMany
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    private Set<Permission> permissions = new HashSet<>();

    // Implement UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
    }

    // Add helper methods for permission checking
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
            .anyMatch(p -> p.getName().equals(permissionName));
    }

    public boolean hasPermission(String actionCode, String resourceType) {
        return permissions.stream()
            .anyMatch(p -> p.getActionCode().equals(actionCode)
                && p.getResourceType().equals(resourceType));
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(r -> r.getName().equals(roleName));
    }

    // ... other UserDetails implementation
}
```

### Step 4: Use Security Features in Controllers

```java
@RestController
@RequestMapping("/api/safaris")
@RequiredArgsConstructor
public class SafariController {
    private final SafariService safariService;
    private final IdObfuscator idObfuscator;

    @GetMapping
    @RequirePermission(action = "read", resource = "Safari")
    public ResponseEntity<List<SafariDTO>> getAllSafaris() {
        return ResponseEntity.ok(safariService.getAllSafaris());
    }

    @PostMapping
    @RequirePermission(action = "create", resource = "Safari")
    public ResponseEntity<SafariDTO> createSafari(@RequestBody SafariRequest request) {
        SafariDTO safari = safariService.create(request);
        // Obfuscate ID in response
        safari.setId(idObfuscator.encodeId(Long.parseLong(safari.getId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(safari);
    }

    @DeleteMapping("/{safariId}")
    @RequirePermission(roles = {"ADMIN"})
    public ResponseEntity<Void> deleteSafari(@PathVariable String safariId) {
        // Decode obfuscated ID
        Long id = idObfuscator.decodeId(safariId);
        safariService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Security Best Practices

### 1. Always Obfuscate IDs in API Responses

```java
// Before sending response, encode IDs
SafariDTO dto = new SafariDTO();
dto.setId(idObfuscator.encodeId(safari.getId()));

// When receiving requests, decode IDs
Long safariId = idObfuscator.decodeId(request.getId());
```

### 2. Use Database-Driven Permissions

Instead of hardcoding roles:
```java
// DON'T: Hardcoded roles
@RequirePermission(roles = {"ADMIN", "MANAGER"})

// DO: Manage in database via endpoint_permissions table
// No annotation needed - filter enforces database rules
```

### 3. Protect Sensitive Settings

System default settings cannot be deleted. For critical settings:
- Mark `isSystemDefault = true` in database
- Set `requiresRestart = true` for settings affecting security behavior
- Monitor changes via logs

### 4. Rotate JWT Secrets

Store JWT secrets in secure vaults, not in code. Update via:
```java
settingsService.updateSettingValue("jwt.secret.key", newSecret);
tokenProvider.refreshExpirationTime();  // Reload config
```

### 5. Audit Trail

All security decisions are logged:
- Permission denials (WARN level)
- Setting changes (INFO level)
- Token generation/validation (DEBUG level)

Monitor logs for suspicious patterns:
```
WARN: Access denied: User X does not have permission Y
WARN: Invalid action code: Z
```

### 6. Password Security

Use `StrongPasswordGenerator` for temporary/admin passwords:
```java
String tempPassword = StrongPasswordGenerator.generateStrongPassword(16);
// Send via secure channel, force change on first login
```

### 7. Rate Limiting (Future Enhancement)

Consider implementing rate limiting on authentication endpoints:
- Login attempts (use accountLockout settings)
- Token refresh endpoints
- Settings management endpoints

---

## Common Use Cases

### Use Case 1: Role-Based Access Control

```java
@RequirePermission(roles = {"ADMIN"})
public void deleteUser(Long userId) {
    // Only admins can delete users
}

@RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
public void approveBooking(Long bookingId) {
    // Admin OR Manager can approve
}
```

### Use Case 2: Fine-Grained Permission Control

```java
@RequirePermission(action = "create", resource = "Booking")
public Booking createBooking(BookingRequest request) {
    // Must have "create" permission on "Booking" resource
}

@RequirePermission(permission = "export_reports")
public void exportMonthlyReport() {
    // Specific permission check
}
```

### Use Case 3: API with Obfuscated IDs

```java
@GetMapping("/{safariId}")
public ResponseEntity<SafariDTO> getSafari(@PathVariable String safariId) {
    Long realId = idObfuscator.decodeId(safariId);
    Safari safari = safariService.getById(realId);

    SafariDTO dto = safariToDto(safari);
    dto.setId(idObfuscator.encodeId(realId));  // Re-encode for response

    return ResponseEntity.ok(dto);
}
```

### Use Case 4: Temporary Admin Password Generation

```java
@PostMapping("/users")
@RequirePermission(roles = {"ADMIN"})
public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
    String tempPassword = StrongPasswordGenerator.generateStrongPassword(12);

    User user = new User();
    user.setUsername(request.getEmail());
    user.setPassword(passwordEncoder.encode(tempPassword));
    user.setMustChangePassword(true);

    userRepository.save(user);
    emailService.sendTemporaryPassword(request.getEmail(), tempPassword);

    return ResponseEntity.status(HttpStatus.CREATED).body(userToDto(user));
}
```

### Use Case 5: Dynamic Permission Configuration

Update permissions at runtime without restarting:

```java
// Database endpoint_permissions table
INSERT INTO endpoint_permissions
(http_method, path_pattern, permission_type, permission_name)
VALUES ('DELETE', '/api/safaris/*', 'PERMISSION_NAME', 'delete_safari');

// No code change needed - filter applies immediately!
// GET /api/safaris/my-hash will check delete_safari permission
```

---

## Troubleshooting

### Issue: JWT Token Validation Failing

**Causes:**
- Expired token (check `jwt.expiration.time.minutes`)
- Invalid signature (secret key changed)
- Malformed token format

**Solution:**
```java
// Check JWT settings health
GET /api/security-settings/health/status

// Verify token expiration time
GET /api/security-settings/jwt.expiration.time.minutes
```

### Issue: Permission Denied Even with Correct Role

**Causes:**
- User doesn't have the role assigned
- Role/permission mismatch
- Endpoint permission not configured

**Solution:**
```java
// Check endpoint permissions
GET /api/security-settings/health/status

// Enable debug logging
logging.level.com.itineraryledger.kabengosafaris.Security=DEBUG
```

### Issue: ID Obfuscator Not Working

**Causes:**
- Obfuscation disabled in settings
- Salt configuration issue

**Solution:**
```java
// Verify obfuscation is enabled
boolean enabled = settingsService.isIdObfuscationEnabled();

// Check hash length
int length = settingsService.getIdObfuscationLength();
```

### Issue: Settings Not Persisting After Update

**Causes:**
- Setting marked as `requiresRestart = true`
- Component caching old values

**Solution:**
```java
// For JWT expiration changes:
tokenProvider.refreshExpirationTime();

// For ID obfuscation changes:
// Restart application (or implement refresh method)
```

---

## Database Schema Reference

### security_settings table
```sql
CREATE TABLE security_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT true,
    is_system_default BOOLEAN DEFAULT false,
    category VARCHAR(50),
    requires_restart BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### endpoint_permissions table (for DynamicPermissionFilter)
```sql
CREATE TABLE endpoint_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    http_method VARCHAR(10) NOT NULL,
    path_pattern VARCHAR(255) NOT NULL,
    is_regex BOOLEAN DEFAULT false,
    permission_type VARCHAR(50),
    permission_name VARCHAR(255),
    action_code VARCHAR(50),
    resource_type VARCHAR(100),
    requires_authentication BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Summary Table

| Component | Purpose | Configuration | Annotation |
|-----------|---------|---------------|-----------|
| JwtTokenProvider | Token generation/validation | jwt.expiration.time.minutes | N/A |
| JwtAuthenticationFilter | User authentication via JWT | (automatic) | N/A |
| CustomUserDetailsService | Load user details | (automatic) | N/A |
| DynamicPermissionFilter | Endpoint security | endpoint_permissions table | N/A |
| PermissionCheckAspect | Method-level security | Database action codes | @RequirePermission |
| IdObfuscator | Hide internal IDs | idObfuscator.* settings | N/A |
| StrongPasswordGenerator | Secure password generation | (none) | N/A |
| SecuritySettings | Config persistence | security_settings table | N/A |
| SecuritySettingsService | CRUD for settings | (automatic) | N/A |
| SecuritySettingsController | API for settings | /api/security-settings | N/A |

---

## Conclusion

The Kabengosafaris Security module provides enterprise-grade security with maximum flexibility. All components work together to create a secure, configurable, and maintainable authentication and authorization system. The database-driven approach allows runtime configuration changes without code deployment, making it ideal for dynamic security requirements.
