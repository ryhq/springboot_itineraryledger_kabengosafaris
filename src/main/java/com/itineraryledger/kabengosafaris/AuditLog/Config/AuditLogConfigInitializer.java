package com.itineraryledger.kabengosafaris.AuditLog.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializer for Audit Logging Configuration.
 * Runs at application startup and initializes default audit configurations in the database.
 *
 * This ensures that the database has the required configuration entries even if they're
 * not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogConfigInitializer implements ApplicationRunner {

    private final AuditLogConfigRepository auditLogConfigRepository;

    /**
     * Injected values from application.properties
     * These serve as fallback/default values if not present in the database
     */
    @Value("${spring.task.execution.pool.core-size:2}")
    private Integer taskExecutionCoreSize;

    @Value("${spring.task.execution.pool.max-size:5}")
    private Integer taskExecutionMaxSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private Integer taskExecutionQueueCapacity;

    @Value("${audit.log.retention.days:90}")
    private Integer auditLogRetentionDays;

    @Value("${audit.log.enabled:true}")
    private Boolean auditLogEnabled;

    /**
     * Run initialization at application startup
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            log.info("Initializing Audit Logging Configuration from database...");
            initializeAuditLogConfigs();
            log.info("Audit Logging Configuration initialization completed successfully");
        } catch (Exception e) {
            log.error("Error initializing Audit Logging Configuration", e);
        }
    }

    /**
     * Initialize or update audit log configurations in the database
     * If configuration doesn't exist, it will be created with system default flag
     * If configuration exists, it will be skipped (to preserve user modifications)
     */
    private void initializeAuditLogConfigs() {
        // 1. Audit Logging Enabled
        createOrUpdateConfig(
                "audit.log.enabled",
                String.valueOf(auditLogEnabled),
                AuditLogConfig.ConfigDataType.BOOLEAN,
                "Enable or disable audit logging globally",
                true
        );

        // 2. Audit Log Retention Days
        createOrUpdateConfig(
                "audit.log.retention.days",
                String.valueOf(auditLogRetentionDays),
                AuditLogConfig.ConfigDataType.INTEGER,
                "Audit log retention in days - logs older than this will be cleaned up",
                true
        );

        // 3. Task Execution Core Size
        createOrUpdateConfig(
                "spring.task.execution.pool.core-size",
                String.valueOf(taskExecutionCoreSize),
                AuditLogConfig.ConfigDataType.INTEGER,
                "Core thread pool size for async audit log processing",
                true
        );

        // 4. Task Execution Max Size
        createOrUpdateConfig(
                "spring.task.execution.pool.max-size",
                String.valueOf(taskExecutionMaxSize),
                AuditLogConfig.ConfigDataType.INTEGER,
                "Maximum thread pool size for async audit log processing",
                true
        );

        // 5. Task Execution Queue Capacity
        createOrUpdateConfig(
                "spring.task.execution.pool.queue-capacity",
                String.valueOf(taskExecutionQueueCapacity),
                AuditLogConfig.ConfigDataType.INTEGER,
                "Queue capacity for async audit log processing tasks",
                true
        );

        // 6. Capture IP Address
        createOrUpdateConfig(
                "audit.log.capture.ip-address",
                "true",
                AuditLogConfig.ConfigDataType.BOOLEAN,
                "Whether to capture client IP address in audit logs",
                true
        );

        // 7. Capture User Agent
        createOrUpdateConfig(
                "audit.log.capture.user-agent",
                "true",
                AuditLogConfig.ConfigDataType.BOOLEAN,
                "Whether to capture User-Agent header in audit logs",
                true
        );

        // 8. Capture Old Values
        createOrUpdateConfig(
                "audit.log.capture.old-values",
                "true",
                AuditLogConfig.ConfigDataType.BOOLEAN,
                "Whether to capture old entity values before updates",
                true
        );

        // 9. Capture New Values
        createOrUpdateConfig(
                "audit.log.capture.new-values",
                "true",
                AuditLogConfig.ConfigDataType.BOOLEAN,
                "Whether to capture new entity values after updates",
                true
        );

        // 10. Excluded Fields
        createOrUpdateConfig(
                "audit.log.excluded-fields",
                "password,token,secret,apikey,creditcard",
                AuditLogConfig.ConfigDataType.STRING,
                "Comma-separated list of fields to exclude from audit logs (case-insensitive)",
                true
        );

        // 11. Max Value Length
        createOrUpdateConfig(
                "audit.log.max-value-length",
                "5000",
                AuditLogConfig.ConfigDataType.INTEGER,
                "Maximum length of captured old/new values to prevent huge JSON strings",
                true
        );

        log.info("All audit log configurations have been initialized");
    }

    /**
     * Create or update a configuration entry
     * If the configuration already exists (by key), it will not be overwritten
     * This preserves any user modifications to configurations
     *
     * @param configKey the configuration key
     * @param configValue the configuration value
     * @param dataType the data type
     * @param description the description
     * @param isSystemDefault whether this is a system default configuration
     */
    private void createOrUpdateConfig(
        String configKey, 
        String configValue,
        AuditLogConfig.ConfigDataType dataType,
        String description, 
        Boolean isSystemDefault
) {
        try {
            if (auditLogConfigRepository.existsByConfigKey(configKey)) {
                log.debug("Configuration already exists, skipping: {}", configKey);
                return;
            }

            AuditLogConfig config = AuditLogConfig.builder()
                    .configKey(configKey)
                    .configValue(configValue)
                    .dataType(dataType)
                    .description(description)
                    .active(true)
                    .isSystemDefault(isSystemDefault)
                    .build();

            auditLogConfigRepository.save(config);
            log.info("Configuration initialized: {} = {}", configKey, configValue);

        } catch (Exception e) {
            log.warn("Failed to initialize configuration {}: {}", configKey, e.getMessage());
        }
    }
}
