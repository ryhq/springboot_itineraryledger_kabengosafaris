package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSetting.Category;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializer for Audit Logging Settings.
 * Runs at application startup and initializes default audit settings in the database.
 *
 * This ensures that the database has the required setting entries even if they're
 * not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogSettingInitializer implements ApplicationRunner, Ordered {
    
    private final AuditLogSettingRepository auditLogSettingRepository;

    /**
     * Injected values from application.properties
     * These serve as fallback/default values if not present in the database
     */

    @Value("${audit.log.retention.days:90}")
    private Integer auditLogRetentionDays;

    @Value("${audit.log.enabled:true}")
    private Boolean auditLogEnabled;
    
    @Value("${audit.log.capture.ip.address:true}")
    private Boolean captureIpAddress;

    @Value("${audit.log.capture.user.agent:true}")
    private Boolean captureUserAgent;

    @Value("${audit.log.capture.old.values:true}")
    private Boolean captureOldValues;

    @Value("${audit.log.capture.new.values:true}")
    private Boolean captureNewValues;

    @Value("${audit.log.excluded.fields:password,token,secret,apikey,creditcard}")
    private String excludedFields;

    @Value("${audit.log.max.value.length:2048}")
    private Integer maxValueLength;

    /**
     * Run initialization at application startup with highest priority
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("\n\n\n");
        log.info("=================================================");
        log.info("Starting Audit Logging Settings initialization...");
        log.info("=================================================");
        System.out.println("\n");
        try {
            log.info("Initializing Audit Logging Settings from database...");
            initializeAuditLogSettings();
            log.info("Audit Logging Settings initialization completed successfully");
            System.out.println("\n");
            log.info("================================================");
            log.info("✓ Audit Logging Settings initialization Complete");
            log.info("================================================");
            System.out.println("\n");
        } catch (Exception e) {
            log.error("Error initializing Audit Logging Settings", e);
        }
    }

    /**
     * Set order to HIGHEST_PRECEDENCE so this initializer runs first
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void initializeAuditLogSettings() {
        // Audit Logging General Settings
        // 1. Audit Logging Enabled
        createOrUpdateConfig(
                "audit.log.enabled",
                String.valueOf(auditLogEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable audit logging globally",
                true,
                Category.GENERAL
        );

        // 2. Audit Log Retention Days
        createOrUpdateConfig(
                "audit.log.retention.days",
                String.valueOf(auditLogRetentionDays),
                SettingDataType.INTEGER,
                "Audit log retention in days - logs older than this will be cleaned up",
                true,
                Category.GENERAL
        );

        // Audit Logging Capture Settings
        // 3. Capture IP Address
        createOrUpdateConfig(
                "audit.log.capture.ip.address",
                String.valueOf(captureIpAddress),
                SettingDataType.BOOLEAN,
                "Whether to capture IP address in audit logs",
                true,
                Category.CAPTURE
        );

        // 4. Capture User-Agent
        createOrUpdateConfig(
                "audit.log.capture.user.agent",
                String.valueOf(captureUserAgent),
                SettingDataType.BOOLEAN,
                "Whether to capture User-Agent in audit logs",
                true,
                Category.CAPTURE
        );

        // 5. Capture Old Values
        createOrUpdateConfig(
                "audit.log.capture.old.values",
                String.valueOf(captureOldValues),
                SettingDataType.BOOLEAN,
                "Whether to capture old values before update",
                true,
                Category.CAPTURE
        );

        // 6. Capture New Values
        createOrUpdateConfig(
                "audit.log.capture.new.values",
                String.valueOf(captureNewValues),
                SettingDataType.BOOLEAN,
                "Whether to capture new values after update",
                true,
                Category.CAPTURE
        );

        // Audit Logging Value Settings
        // 7. Excluded Fields
        createOrUpdateConfig(
                "audit.log.excluded.fields",
                excludedFields,
                SettingDataType.STRING,
                "Comma-separated list of fields to exclude from audit logs (case-insensitive)",
                true,
                Category.VALUES
        );

        // 8. Max Value Length
        createOrUpdateConfig(
                "audit.log.max.value.length",
                String.valueOf(maxValueLength),
                SettingDataType.INTEGER,
                "Maximum length of captured old/new values (to prevent huge JSON strings)",
                true,
                Category.VALUES
        );
    }

    /**
     * Create or update a setting entry
     * If the setting already exists (by key), it will not be overwritten
     * This preserves any user modifications to settings
     *
     * @param settingKey the setting key
     * @param settingValue the setting value
     * @param settingDataType the data type
     * @param description the description
     * @param isSystemDefault whether this is a system default setting
     * @param category the category of the setting
     */
    private void createOrUpdateConfig(
            String settingKey,
            String settingValue,
            SettingDataType settingDataType,
            String description,
            Boolean isSystemDefault,
            Category category
    ) {
            try {
                if (auditLogSettingRepository.existsBySettingKey(settingKey)) {
                    log.debug("Audit Setting already exists, skipping: {}", settingKey);
                    return;
                }

                AuditLogSetting setting = AuditLogSetting.builder()
                        .settingKey(settingKey)
                        .settingValue(settingValue)
                        .dataType(settingDataType)
                        .description(description)
                        .active(true)
                        .isSystemDefault(isSystemDefault)
                        .category(category)
                        .build();
                auditLogSettingRepository.save(setting);
                log.info("Audit Setting initialized: {} = {}", settingKey, settingValue);

            } catch (Exception e) {
                log.warn("Failed to initialize setting {}: {}", settingKey, e.getMessage());
            }
        }


    
}
