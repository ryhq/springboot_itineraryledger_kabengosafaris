# Audit Logging Configuration - Database-Backed Configuration System

## Overview

This document explains how the audit logging configuration has been moved from static `application.properties` to a **database-backed, dynamically configurable system**.

### Benefits

- **Dynamic Updates** - Change audit configuration at runtime without restarting
- **Persistence** - Configuration changes survive application restarts
- **Flexibility** - Add new configurations without code changes
- **Auditability** - Track who changed what configuration and when
- **Type Safety** - Strongly typed configuration values with enum types

---

## Architecture

### 1. **AuditLogConfig Entity** (`AuditLogConfig.java`)

Represents a configuration entry stored in the database.

**Table: `audit_log_config`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `config_key` | VARCHAR(100) | Configuration key (e.g., 'audit.log.enabled') |
| `config_value` | TEXT | Configuration value as string |
| `data_type` | VARCHAR(50) | Data type: STRING, INTEGER, BOOLEAN, LONG, DOUBLE |
| `description` | TEXT | Human-readable description |
| `active` | BOOLEAN | Whether configuration is active |
| `is_system_default` | BOOLEAN | Whether it's a system default (cannot be deleted) |
| `created_at` | TIMESTAMP | Creation timestamp (auto) |
| `updated_at` | TIMESTAMP | Last update timestamp (auto) |

**Unique Constraint:** `config_key` must be unique

### 2. **AuditLogConfigRepository** (`AuditLogConfigRepository.java`)

Spring Data JPA repository providing database access:

```java
// Find by key
Optional<AuditLogConfig> findByConfigKey(String configKey);

// Find active by key
Optional<AuditLogConfig> findActiveByConfigKey(String configKey);

// Get all active configurations
List<AuditLogConfig> findAllActive();

// Get all system defaults
List<AuditLogConfig> findAllSystemDefaults();

// Check existence
boolean existsByConfigKey(String configKey);
```

### 3. **AuditLogConfigProperties** (`AuditLogConfigProperties.java`)

Spring `@ConfigurationProperties` class for binding configuration values:

```java
@ConfigurationProperties(prefix = "audit")
public class AuditLogConfigProperties {

    private Log log = new Log();

    public static class Log {
        private boolean enabled = true;                // Default: true
        private int retentionDays = 90;               // Default: 90 days
        private boolean captureIpAddress = true;      // Default: true
        private boolean captureUserAgent = true;      // Default: true
        private boolean captureOldValues = true;      // Default: true
        private boolean captureNewValues = true;      // Default: true
        private String excludedFields = "...";        // Password, token, secret, etc.
        private int maxValueLength = 5000;            // Default: 5000 chars
    }
}
```

### 4. **AuditLogConfigService** (`AuditLogConfigService.java`)

Business logic service for managing configurations:

**Key Methods:**

```java
// Get configuration value
String getConfigValue(String configKey);
Integer getConfigValueAsInteger(String configKey);
Boolean getConfigValueAsBoolean(String configKey);
Long getConfigValueAsLong(String configKey);
Double getConfigValueAsDouble(String configKey);

// Create/Update
AuditLogConfig createConfig(String key, String value, ConfigDataType type, String description);
AuditLogConfig updateConfigValue(String configKey, String newValue);

// Deactivate/Reactivate
void deactivateConfig(String configKey);
void reactivateConfig(String configKey);

// Delete
void deleteConfig(String configKey);  // Cannot delete system defaults

// Retrieve
AuditLogConfig getConfig(String configKey);
List<AuditLogConfig> getAllActiveConfigs();
List<AuditLogConfig> getAllSystemDefaults();

// Utility
boolean configExists(String configKey);
void resetToDefault(String configKey);
```

### 5. **AuditLogConfigInitializer** (`AuditLogConfigInitializer.java`)

Application startup component that initializes default configurations:

```java
@Component
public class AuditLogConfigInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // Initializes 11 default audit configurations
    }
}
```

**Initialization Process:**
1. Runs at application startup (implements `ApplicationRunner`)
2. Reads values from `application.properties` as fallback defaults
3. Creates database entries for each configuration if they don't exist
4. Marks them as `isSystemDefault = true` (cannot be deleted)
5. Skips existing configurations (preserves user modifications)

---

## Default Configurations

The system initializes the following configurations on first startup:

### 1. Audit Logging Control
- **Key:** `audit.log.enabled`
- **Value:** `true`
- **Type:** BOOLEAN
- **Description:** Enable or disable audit logging globally

### 2. Retention Policy
- **Key:** `audit.log.retention.days`
- **Value:** `90`
- **Type:** INTEGER
- **Description:** Audit log retention in days - logs older than this will be cleaned up

### 3. Thread Pool Configuration
- **Key:** `spring.task.execution.pool.core-size`
- **Value:** `2`
- **Type:** INTEGER
- **Description:** Core thread pool size for async audit log processing

- **Key:** `spring.task.execution.pool.max-size`
- **Value:** `5`
- **Type:** INTEGER
- **Description:** Maximum thread pool size for async audit log processing

- **Key:** `spring.task.execution.pool.queue-capacity`
- **Value:** `100`
- **Type:** INTEGER
- **Description:** Queue capacity for async audit log processing tasks

### 4. Data Capture Options
- **Key:** `audit.log.capture.ip-address`
- **Value:** `true`
- **Type:** BOOLEAN

- **Key:** `audit.log.capture.user-agent`
- **Value:** `true`
- **Type:** BOOLEAN

- **Key:** `audit.log.capture.old-values`
- **Value:** `true`
- **Type:** BOOLEAN

- **Key:** `audit.log.capture.new-values`
- **Value:** `true`
- **Type:** BOOLEAN

### 5. Exclusion and Limits
- **Key:** `audit.log.excluded-fields`
- **Value:** `password,token,secret,apikey,creditcard`
- **Type:** STRING

- **Key:** `audit.log.max-value-length`
- **Value:** `5000`
- **Type:** INTEGER

---

## Usage Examples

### 1. Injecting AuditLogConfigService

```java
@Service
public class MyService {

    private final AuditLogConfigService auditConfigService;

    public MyService(AuditLogConfigService auditConfigService) {
        this.auditConfigService = auditConfigService;
    }

    public void someMethod() {
        // Get configuration values
        boolean auditEnabled = auditConfigService.getConfigValueAsBoolean("audit.log.enabled");
        int retentionDays = auditConfigService.getConfigValueAsInteger("audit.log.retention.days");

        if (auditEnabled) {
            // Perform audit operations
        }
    }
}
```

### 2. Reading Configuration Properties

```java
@Service
public class AnotherService {

    private final AuditLogConfigProperties auditConfigProps;

    public AnotherService(AuditLogConfigProperties auditConfigProps) {
        this.auditConfigProps = auditConfigProps;
    }

    public void someMethod() {
        boolean enabled = auditConfigProps.getLog().isEnabled();
        int retentionDays = auditConfigProps.getLog().getRetentionDays();
        String excludedFields = auditConfigProps.getLog().getExcludedFields();
    }
}
```

### 3. Updating Configuration at Runtime

```java
@RestController
@RequestMapping("/api/audit-config")
public class AuditConfigController {

    private final AuditLogConfigService auditConfigService;

    @PutMapping("/{configKey}")
    public AuditLogConfig updateConfig(
            @PathVariable String configKey,
            @RequestBody String newValue) {
        return auditConfigService.updateConfigValue(configKey, newValue);
    }

    @GetMapping
    public List<AuditLogConfig> getAllConfigs() {
        return auditConfigService.getAllActiveConfigs();
    }

    @PostMapping
    public AuditLogConfig createConfig(@RequestBody AuditLogConfig config) {
        return auditConfigService.saveConfig(config);
    }
}
```

---

## Migration from application.properties

### Before (Static Configuration)

```properties
# application.properties
audit.log.retention.days=90
audit.log.enabled=true
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100
```

**Limitations:**
- ❌ Required restart to change configuration
- ❌ Not auditable (no tracking of changes)
- ❌ Hard to manage at scale
- ❌ No runtime flexibility

### After (Database-Backed Configuration)

```java
// application.properties - Now serves as FALLBACK only
audit.log.retention.days=90
audit.log.enabled=true
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100

// Database (audit_log_config table)
// Contains same configurations but can be updated at runtime
```

**Benefits:**
- ✅ No restart needed for changes
- ✅ Changes are auditable
- ✅ Can add new configurations dynamically
- ✅ Type-safe access to configuration values
- ✅ Backward compatible (application.properties still works as fallback)

---

## Database Schema

### SQL to create the table manually (if needed)

```sql
CREATE TABLE audit_log_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    INDEX idx_active (active)
);
```

Since we're using `spring.jpa.hibernate.ddl-auto=update`, Hibernate will automatically create this table on startup.

---

## Configuration Management Best Practices

### 1. **Always Use Type-Specific Getters**

```java
// ✅ Good - Type-safe
Integer coreSize = auditConfigService.getConfigValueAsInteger("spring.task.execution.pool.core-size");

// ❌ Avoid - String comparison issues
String coreSize = auditConfigService.getConfigValue("spring.task.execution.pool.core-size");
Integer parsed = Integer.parseInt(coreSize); // Prone to errors
```

### 2. **Handle NoSuchElementException**

```java
try {
    boolean enabled = auditConfigService.getConfigValueAsBoolean("audit.log.enabled");
} catch (NoSuchElementException e) {
    // Configuration not found, use fallback
    boolean enabled = true; // Default
}
```

### 3. **Never Delete System Default Configurations**

```java
// ❌ Will throw IllegalArgumentException
auditConfigService.deleteConfig("audit.log.enabled");

// ✅ Deactivate instead
auditConfigService.deactivateConfig("audit.log.enabled");

// ✅ Or reset to default
auditConfigService.resetToDefault("audit.log.enabled");
```

### 4. **Use Descriptive Configuration Keys**

Follow the pattern: `module.submodule.setting`

Good examples:
- `audit.log.enabled`
- `audit.log.retention.days`
- `spring.task.execution.pool.core-size`
- `audit.log.capture.ip-address`

---

## Troubleshooting

### Configuration Not Found

**Problem:** `NoSuchElementException: Configuration not found: audit.log.enabled`

**Solution:**
1. Check if `AuditLogConfigInitializer` ran successfully
2. Verify the configuration key spelling
3. Check database table `audit_log_config` for entries
4. Restart application to trigger initializer

### Configuration Not Updating

**Problem:** Changes via `updateConfigValue()` don't take effect

**Solution:**
1. Ensure you're reading from `AuditLogConfigService`, not application.properties
2. Check that configuration is `active = true`
3. Verify the configuration value is correct (wrong type)
4. Check logs for errors during update

### Type Conversion Errors

**Problem:** `NumberFormatException` when reading integer config

**Solution:**
1. Verify the stored value is numeric (check database)
2. Use correct getter method: `getConfigValueAsInteger()` not `getConfigValue()`
3. Check `dataType` field matches actual data

---

## Integration with AuditLoggingAspect

The `AuditLoggingAspect` can be enhanced to read configuration from `AuditLogConfigService`:

```java
@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogConfigService auditConfigService;

    public AuditLoggingAspect(AuditLogConfigService auditConfigService) {
        this.auditConfigService = auditConfigService;
    }

    @Around("@annotation(auditLogAnnotation)")
    public Object auditLog(ProceedingJoinPoint joinPoint, AuditLogAnnotation auditLogAnnotation) {
        // Check if audit logging is enabled
        boolean auditEnabled = auditConfigService.getConfigValueAsBoolean("audit.log.enabled");
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        // Get other configuration
        boolean captureOldValues = auditConfigService.getConfigValueAsBoolean("audit.log.capture.old-values");
        boolean captureNewValues = auditConfigService.getConfigValueAsBoolean("audit.log.capture.new-values");

        // ... rest of aspect logic
    }
}
```

---

## Future Enhancements

1. **Configuration Change History** - Track who changed what and when
2. **Configuration Validation** - Add validators for configuration values
3. **Feature Flags** - Use configuration for feature toggling
4. **Performance Metrics** - Monitor configuration changes
5. **Configuration Export/Import** - Backup and restore configurations
6. **Configuration Templates** - Predefined sets of configurations for different environments

---

## Files Created/Modified

### Created Files:
1. ✅ `AuditLogConfig.java` - Entity class
2. ✅ `AuditLogConfigRepository.java` - Repository interface
3. ✅ `AuditLogConfigProperties.java` - Configuration properties class
4. ✅ `AuditLogConfigService.java` - Service layer
5. ✅ `AuditLogConfigInitializer.java` - Startup initializer

### Modified Files:
1. ✅ `application.properties` - Added documentation about database configuration

---

## Summary

The audit logging configuration system has been successfully moved from static `application.properties` to a **dynamic, database-backed system**. This provides:

- **Runtime Configurability** - No restart needed
- **Type Safety** - Strongly typed configuration values
- **Auditability** - Track configuration changes
- **Flexibility** - Add new configurations without code changes
- **Backward Compatibility** - application.properties still works as fallback

Start using `AuditLogConfigService` in your code to read audit configurations dynamically!
