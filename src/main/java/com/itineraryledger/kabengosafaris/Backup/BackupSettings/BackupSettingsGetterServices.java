package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for getting backup settings with fallback to application.properties
 * Provides typed getter methods for all backup configuration settings
 */
@Service
public class BackupSettingsGetterServices {

    // Fallback values from application.properties

    // General Settings
    @Value("${backup.enabled:true}")
    private Boolean defaultBackupEnabled;

    @Value("${backup.type:FULL}")
    private String defaultBackupType;

    // Schedule Settings
    @Value("${backup.schedule.enabled:true}")
    private Boolean defaultScheduleEnabled;

    // Database Settings
    @Value("${backup.database.enabled:true}")
    private Boolean defaultDatabaseEnabled;

    @Value("${backup.database.host:${MYSQL_HOST:localhost}}")
    private String defaultDatabaseHost;

    @Value("${backup.database.port:3306}")
    private Integer defaultDatabasePort;

    @Value("${backup.database.name:springboot_itineraryledger_kabengosafaris}")
    private String defaultDatabaseName;

    @Value("${backup.database.username:root}")
    private String defaultDatabaseUsername;

    @Value("${backup.database.include.routines:true}")
    private Boolean defaultIncludeRoutines;

    @Value("${backup.database.include.triggers:true}")
    private Boolean defaultIncludeTriggers;

    @Value("${backup.database.include.events:false}")
    private Boolean defaultIncludeEvents;

    // File Settings
    @Value("${backup.files.enabled:true}")
    private Boolean defaultFilesEnabled;

    // Storage Settings
    @Value("${backup.storage.filename.prefix:kabengosafaris_backup}")
    private String defaultFilenamePrefix;

    @Value("${backup.storage.filename.date.format:yyyyMMdd_HHmmss}")
    private String defaultFilenameDateFormat;

    // Retention Settings
    @Value("${backup.retention.days:7}")
    private Integer defaultRetentionDays;

    @Value("${backup.retention.max.count:30}")
    private Integer defaultRetentionMaxCount;

    @Value("${backup.retention.auto.cleanup.enabled:true}")
    private Boolean defaultAutoCleanupEnabled;

    // Compression Settings
    @Value("${backup.compression.enabled:true}")
    private Boolean defaultCompressionEnabled;

    @Value("${backup.compression.format:zip}")
    private String defaultCompressionFormat;

    @Value("${backup.compression.level:5}")
    private Integer defaultCompressionLevel;

    // Notification Settings
    @Value("${backup.notification.enabled:false}")
    private Boolean defaultNotificationEnabled;

    @Value("${backup.notification.on.success:false}")
    private Boolean defaultNotifyOnSuccess;

    @Value("${backup.notification.on.failure:true}")
    private Boolean defaultNotifyOnFailure;

    @Value("${backup.notification.emails:admin@kabengosafaris.com}")
    private String defaultNotificationEmails;

    @Autowired
    private BackupSettingsRepository backupSettingsRepository;

    /**
     * Helper method to get setting value with fallback
     */
    private String getSettingValue(String key, String defaultValue) {
        BackupSetting setting = backupSettingsRepository.findBySettingKey(key).orElse(null);
        if (setting == null || !setting.getActive()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }

    private Boolean getBooleanSetting(String key, Boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getSettingValue(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer getIntegerSetting(String key, Integer defaultValue) {
        try {
            return Integer.parseInt(getSettingValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ==================== GENERAL SETTINGS ====================

    public Boolean isBackupEnabled() {
        return getBooleanSetting("backup.enabled", defaultBackupEnabled);
    }

    public String getBackupType() {
        return getSettingValue("backup.type", defaultBackupType);
    }

    // ==================== SCHEDULE SETTINGS ====================

    public Boolean isScheduleEnabled() {
        return getBooleanSetting("backup.schedule.enabled", defaultScheduleEnabled);
    }

    // ==================== DATABASE SETTINGS ====================

    public Boolean isDatabaseBackupEnabled() {
        return getBooleanSetting("backup.database.enabled", defaultDatabaseEnabled);
    }

    public String getDatabaseHost() {
        return getSettingValue("backup.database.host", defaultDatabaseHost);
    }

    public Integer getDatabasePort() {
        return getIntegerSetting("backup.database.port", defaultDatabasePort);
    }

    public String getDatabaseName() {
        return getSettingValue("backup.database.name", defaultDatabaseName);
    }

    public String getDatabaseUsername() {
        return getSettingValue("backup.database.username", defaultDatabaseUsername);
    }

    public Boolean includeRoutines() {
        return getBooleanSetting("backup.database.include.routines", defaultIncludeRoutines);
    }

    public Boolean includeTriggers() {
        return getBooleanSetting("backup.database.include.triggers", defaultIncludeTriggers);
    }

    public Boolean includeEvents() {
        return getBooleanSetting("backup.database.include.events", defaultIncludeEvents);
    }

    // ==================== FILE SETTINGS ====================

    public Boolean isFilesBackupEnabled() {
        return getBooleanSetting("backup.files.enabled", defaultFilesEnabled);
    }

    public Boolean includeEmailSignatures() {
        return getBooleanSetting("backup.files.include.email.signatures", true);
    }

    public Boolean includeEmailTemplates() {
        return getBooleanSetting("backup.files.include.email.templates", true);
    }

    public Boolean includePdfTemplates() {
        return getBooleanSetting("backup.files.include.pdf.templates", true);
    }

    public Boolean includeAccommodationImages() {
        return getBooleanSetting("backup.files.include.accommodation.images", true);
    }

    public Boolean includeAccommodationDocuments() {
        return getBooleanSetting("backup.files.include.accommodation.documents", true);
    }

    public Boolean includeParkImages() {
        return getBooleanSetting("backup.files.include.park.images", true);
    }

    public Boolean includeParkDocuments() {
        return getBooleanSetting("backup.files.include.park.documents", true);
    }

    public Boolean includeActivityImages() {
        return getBooleanSetting("backup.files.include.activity.images", true);
    }

    public Boolean includeActivityDocuments() {
        return getBooleanSetting("backup.files.include.activity.documents", true);
    }

    public Boolean includeItineraryDocuments() {
        return getBooleanSetting("backup.files.include.itinerary.documents", true);
    }

    public Boolean includeQuoteDocuments() {
        return getBooleanSetting("backup.files.include.quote.documents", true);
    }

    public Boolean includeSafariDocuments() {
        return getBooleanSetting("backup.files.include.safari.documents", true);
    }

    // ==================== STORAGE SETTINGS ====================

    public String getFilenamePrefix() {
        return getSettingValue("backup.storage.filename.prefix", defaultFilenamePrefix);
    }

    public String getFilenameDateFormat() {
        return getSettingValue("backup.storage.filename.date.format", defaultFilenameDateFormat);
    }

    // ==================== RETENTION SETTINGS ====================

    public Integer getRetentionDays() {
        return getIntegerSetting("backup.retention.days", defaultRetentionDays);
    }

    public Integer getRetentionMaxCount() {
        return getIntegerSetting("backup.retention.max.count", defaultRetentionMaxCount);
    }

    public Boolean isAutoCleanupEnabled() {
        return getBooleanSetting("backup.retention.auto.cleanup.enabled", defaultAutoCleanupEnabled);
    }

    // ==================== COMPRESSION SETTINGS ====================

    public Boolean isCompressionEnabled() {
        return getBooleanSetting("backup.compression.enabled", defaultCompressionEnabled);
    }

    public String getCompressionFormat() {
        return getSettingValue("backup.compression.format", defaultCompressionFormat);
    }

    public Integer getCompressionLevel() {
        return getIntegerSetting("backup.compression.level", defaultCompressionLevel);
    }

    // ==================== NOTIFICATION SETTINGS ====================

    public Boolean isNotificationEnabled() {
        return getBooleanSetting("backup.notification.enabled", defaultNotificationEnabled);
    }

    public Boolean notifyOnSuccess() {
        return getBooleanSetting("backup.notification.on.success", defaultNotifyOnSuccess);
    }

    public Boolean notifyOnFailure() {
        return getBooleanSetting("backup.notification.on.failure", defaultNotifyOnFailure);
    }

    /**
     * Get list of notification email addresses
     * Emails are stored as comma-separated string in database
     *
     * @return List of email addresses
     */
    public List<String> getNotificationEmails() {
        String emailsString = getSettingValue("backup.notification.emails", defaultNotificationEmails);
        if (emailsString == null || emailsString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(emailsString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }
}
