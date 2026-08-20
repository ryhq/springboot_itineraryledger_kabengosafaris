package com.itineraryledger.kabengosafaris.Initializers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSetting;
import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsRepository;

/**
 * Initializer for Backup Settings.
 * Runs at application startup and initializes default backup settings in the database.
 *
 * This ensures that the database has the required backup settings even if they're
 * not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupSettingsInitializer implements ApplicationRunner, Ordered {

    private final BackupSettingsRepository backupSettingsRepository;

    /**
     * Injected values from application.properties
     * These serve as fallback/default values if not present in the database
     */

    // General Settings
    @Value("${backup.enabled:true}")
    private Boolean backupEnabled;

    @Value("${backup.type:FULL}")
    private String backupType;

    // Schedule Settings
    @Value("${backup.schedule.enabled:true}")
    private Boolean scheduleEnabled;

    // Database Backup Settings
    @Value("${backup.database.enabled:true}")
    private Boolean databaseBackupEnabled;

    @Value("${backup.database.host:${MYSQL_HOST:localhost}}")
    private String databaseHost;

    @Value("${backup.database.port:3306}")
    private Integer databasePort;

    @Value("${backup.database.name:}")
    private String databaseName;

    @Value("${backup.database.username:root}")
    private String databaseUsername;

    @Value("${backup.database.include.routines:true}")
    private Boolean includeRoutines;

    @Value("${backup.database.include.triggers:true}")
    private Boolean includeTriggers;

    @Value("${backup.database.include.events:true}")
    private Boolean includeEvents;

    // File System Backup Settings
    @Value("${backup.files.enabled:true}")
    private Boolean filesBackupEnabled;

    @Value("${backup.files.include.email.signatures:true}")
    private Boolean includeEmailSignatures;

    @Value("${backup.files.include.email.templates:true}")
    private Boolean includeEmailTemplates;

    @Value("${backup.files.include.pdf.templates:true}")
    private Boolean includePdfTemplates;

    @Value("${backup.files.include.accommodation.images:true}")
    private Boolean includeAccommodationImages;

    @Value("${backup.files.include.accommodation.documents:true}")
    private Boolean includeAccommodationDocuments;

    @Value("${backup.files.include.park.images:true}")
    private Boolean includeParkImages;

    @Value("${backup.files.include.park.documents:true}")
    private Boolean includeParkDocuments;

    @Value("${backup.files.include.activity.images:true}")
    private Boolean includeActivityImages;

    @Value("${backup.files.include.activity.documents:true}")
    private Boolean includeActivityDocuments;

    @Value("${backup.files.include.itinerary.documents:true}")
    private Boolean includeItineraryDocuments;

    @Value("${backup.files.include.quote.documents:true}")
    private Boolean includeQuoteDocuments;

    @Value("${backup.files.include.safari.documents:true}")
    private Boolean includeSafariDocuments;

    // Storage Settings
    /* a filename is handed to people: it names the company, not this repo */
    @Value("${backup.storage.filename.prefix:backup}")
    private String filenamePrefix;

    @Value("${backup.storage.filename.date.format:yyyyMMdd_HHmmss}")
    private String filenameDateFormat;

    // Retention Settings
    @Value("${backup.retention.days:7}")
    private Integer retentionDays;

    @Value("${backup.retention.max.count:30}")
    private Integer retentionMaxCount;

    @Value("${backup.retention.auto.cleanup.enabled:true}")
    private Boolean autoCleanupEnabled;

    // Compression Settings
    @Value("${backup.compression.enabled:true}")
    private Boolean compressionEnabled;

    @Value("${backup.compression.format:zip}")
    private String compressionFormat;

    @Value("${backup.compression.level:5}")
    private Integer compressionLevel;

    // Notification Settings
    @Value("${backup.notification.enabled:false}")
    private Boolean notificationEnabled;

    @Value("${backup.notification.on.success:false}")
    private Boolean notifyOnSuccess;

    @Value("${backup.notification.on.failure:true}")
    private Boolean notifyOnFailure;

    @Value("${backup.notification.emails:}")
    private String notificationEmails;

    /**
     * Set order - runs after FileSettingsInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 6;
    }

    /**
     * Run initialization at application startup
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializeBackupSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during Backup Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║              BACKUP SETTINGS INITIALIZER - START                   ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✓ BACKUP SETTINGS INITIALIZER - COMPLETED                  ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✗ BACKUP SETTINGS INITIALIZER - FAILED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize or update backup settings in the database
     */
    private void initializeBackupSettings() {
        // General Settings
        createOrUpdateSetting(
                "backup.enabled",
                String.valueOf(backupEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable the backup system globally",
                BackupSetting.Category.GENERAL,
                false
        );

        createOrUpdateSetting(
                "backup.type",
                backupType,
                SettingDataType.STRING,
                "Type of backup: FULL (complete backup), INCREMENTAL (only changes), DIFFERENTIAL (changes since last full)",
                BackupSetting.Category.GENERAL,
                false
        );

        // Schedule Settings
        createOrUpdateSetting(
                "backup.schedule.enabled",
                String.valueOf(scheduleEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable automatic scheduled backups",
                BackupSetting.Category.SCHEDULE,
                false
        );

        // Database Backup Settings
        createOrUpdateSetting(
                "backup.database.enabled",
                String.valueOf(databaseBackupEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable database backups",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.host",
                databaseHost,
                SettingDataType.STRING,
                "Database host address",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.port",
                String.valueOf(databasePort),
                SettingDataType.INTEGER,
                "Database port number",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.name",
                databaseName,
                SettingDataType.STRING,
                "Database name to backup",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.username",
                databaseUsername,
                SettingDataType.STRING,
                "Database username for backup operations",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.include.routines",
                String.valueOf(includeRoutines),
                SettingDataType.BOOLEAN,
                "Include stored procedures and functions in database backup",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.include.triggers",
                String.valueOf(includeTriggers),
                SettingDataType.BOOLEAN,
                "Include triggers in database backup",
                BackupSetting.Category.DATABASE,
                false
        );

        createOrUpdateSetting(
                "backup.database.include.events",
                String.valueOf(includeEvents),
                SettingDataType.BOOLEAN,
                "Include scheduled events in database backup",
                BackupSetting.Category.DATABASE,
                false
        );

        // File System Backup Settings
        createOrUpdateSetting(
                "backup.files.enabled",
                String.valueOf(filesBackupEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable file system backups",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.email.signatures",
                String.valueOf(includeEmailSignatures),
                SettingDataType.BOOLEAN,
                "Include email signatures in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.email.templates",
                String.valueOf(includeEmailTemplates),
                SettingDataType.BOOLEAN,
                "Include email templates in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.pdf.templates",
                String.valueOf(includePdfTemplates),
                SettingDataType.BOOLEAN,
                "Include PDF templates in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.accommodation.images",
                String.valueOf(includeAccommodationImages),
                SettingDataType.BOOLEAN,
                "Include accommodation images in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.accommodation.documents",
                String.valueOf(includeAccommodationDocuments),
                SettingDataType.BOOLEAN,
                "Include accommodation documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.park.images",
                String.valueOf(includeParkImages),
                SettingDataType.BOOLEAN,
                "Include park images in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.park.documents",
                String.valueOf(includeParkDocuments),
                SettingDataType.BOOLEAN,
                "Include park documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.activity.images",
                String.valueOf(includeActivityImages),
                SettingDataType.BOOLEAN,
                "Include activity images in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.activity.documents",
                String.valueOf(includeActivityDocuments),
                SettingDataType.BOOLEAN,
                "Include activity documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.itinerary.documents",
                String.valueOf(includeItineraryDocuments),
                SettingDataType.BOOLEAN,
                "Include itinerary documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.quote.documents",
                String.valueOf(includeQuoteDocuments),
                SettingDataType.BOOLEAN,
                "Include quote documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        createOrUpdateSetting(
                "backup.files.include.safari.documents",
                String.valueOf(includeSafariDocuments),
                SettingDataType.BOOLEAN,
                "Include safari documents in file backup",
                BackupSetting.Category.FILES,
                false
        );

        // Storage Settings
        createOrUpdateSetting(
                "backup.storage.filename.prefix",
                filenamePrefix,
                SettingDataType.STRING,
                "Prefix for backup filenames",
                BackupSetting.Category.STORAGE,
                false
        );

        createOrUpdateSetting(
                "backup.storage.filename.date.format",
                filenameDateFormat,
                SettingDataType.STRING,
                "Date format for backup filenames (Java DateTimeFormatter pattern)",
                BackupSetting.Category.STORAGE,
                false
        );

        // Retention Settings
        createOrUpdateSetting(
                "backup.retention.days",
                String.valueOf(retentionDays),
                SettingDataType.INTEGER,
                "Number of days to retain backups (older backups are deleted)",
                BackupSetting.Category.RETENTION,
                false
        );

        createOrUpdateSetting(
                "backup.retention.max.count",
                String.valueOf(retentionMaxCount),
                SettingDataType.INTEGER,
                "Maximum number of backups to retain (oldest are deleted when exceeded)",
                BackupSetting.Category.RETENTION,
                false
        );

        createOrUpdateSetting(
                "backup.retention.auto.cleanup.enabled",
                String.valueOf(autoCleanupEnabled),
                SettingDataType.BOOLEAN,
                "Enable automatic cleanup of old backups based on retention policy",
                BackupSetting.Category.RETENTION,
                false
        );

        // Compression Settings
        createOrUpdateSetting(
                "backup.compression.enabled",
                String.valueOf(compressionEnabled),
                SettingDataType.BOOLEAN,
                "Enable compression for backups to save storage space",
                BackupSetting.Category.COMPRESSION,
                false
        );

        createOrUpdateSetting(
                "backup.compression.format",
                compressionFormat,
                SettingDataType.STRING,
                "Compression format: zip, gzip, tar, tar.gz",
                BackupSetting.Category.COMPRESSION,
                false
        );

        createOrUpdateSetting(
                "backup.compression.level",
                String.valueOf(compressionLevel),
                SettingDataType.INTEGER,
                "Compression level (0-9): 0=no compression, 9=maximum compression",
                BackupSetting.Category.COMPRESSION,
                false
        );

        // Notification Settings
        createOrUpdateSetting(
                "backup.notification.enabled",
                String.valueOf(notificationEnabled),
                SettingDataType.BOOLEAN,
                "Enable email notifications for backup operations",
                BackupSetting.Category.NOTIFICATION,
                false
        );

        createOrUpdateSetting(
                "backup.notification.on.success",
                String.valueOf(notifyOnSuccess),
                SettingDataType.BOOLEAN,
                "Send notification when backup completes successfully",
                BackupSetting.Category.NOTIFICATION,
                false
        );

        createOrUpdateSetting(
                "backup.notification.on.failure",
                String.valueOf(notifyOnFailure),
                SettingDataType.BOOLEAN,
                "Send notification when backup fails",
                BackupSetting.Category.NOTIFICATION,
                false
        );

        createOrUpdateSetting(
                "backup.notification.emails",
                notificationEmails,
                SettingDataType.STRING,
                "Email addresses to send backup notifications (comma-separated: email1@example.com,email2@example.com)",
                BackupSetting.Category.NOTIFICATION,
                false
        );

        log.info("All backup settings have been initialized");
    }

    /**
     * Create or update a backup setting
     * If the setting already exists (by key), it will not be overwritten
     * This preserves any user modifications to settings
     *
     * @param settingKey the setting key
     * @param settingValue the setting value
     * @param dataType the data type
     * @param description the description
     * @param category the category
     * @param requiresRestart whether changing this setting requires restart
     */
    private void createOrUpdateSetting(String settingKey, String settingValue,
                                        SettingDataType dataType,
                                        String description, BackupSetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (backupSettingsRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ Setting already exists, skipping: {}", settingKey);
                return;
            }

            BackupSetting setting = BackupSetting.builder()
                    .settingKey(settingKey)
                    .settingValue(settingValue)
                    .dataType(dataType)
                    .description(description)
                    .category(category)
                    .active(true)
                    .isSystemDefault(true)
                    .requiresRestart(requiresRestart)
                    .build();

            backupSettingsRepository.save(setting);
            log.info("✓ Backup setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("✗ Failed to initialize backup setting {}: {}", settingKey, e.getMessage());
        }
    }
}
