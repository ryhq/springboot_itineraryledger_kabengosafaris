package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSettingGetterServices;

/**
 * Service to retrieve Access Log Settings
 *
 * ARCHITECTURE:
 * - Log file locations (directory, prefix, suffix) are read from server.tomcat.accesslog.* properties
 * - Analysis features (security, bot detection, performance) use hardcoded defaults
 * - Retention settings are SHARED with audit logs via AuditLogSettingGetterServices
 *
 * This simplified approach eliminates duplication between Tomcat and access log settings.
 * To change log file locations, update server.tomcat.accesslog.* properties and restart.
 * To change analysis thresholds, modify the static constants in this class and recompile.
 */
@Service
public class AccessLogSettingGetterServices {

    /**
     * Inject audit log settings service to share retention settings
     */
    @Autowired
    private AuditLogSettingGetterServices auditLogSettingGetterServices;

    // ==========================================
    // SECURITY ANALYSIS SETTINGS (Hardcoded Defaults)
    // ==========================================

    private static final Boolean DEFAULT_SECURITY_ANALYSIS_ENABLED = true;

    // ==========================================
    // BOT DETECTION SETTINGS (Hardcoded Defaults)
    // ==========================================

    private static final Boolean DEFAULT_BOT_DETECTION_ENABLED = true;

    // ==========================================
    // PERFORMANCE MONITORING SETTINGS (Hardcoded Defaults)
    // ==========================================

    private static final Boolean DEFAULT_PERFORMANCE_MONITORING_ENABLED = true;
    private static final Long DEFAULT_SLOW_REQUEST_THRESHOLD = 5000L;
    
    // ==========================================
    // LOG STORAGE SETTINGS (From Tomcat Configuration)
    // ==========================================

    // Note: With server.tomcat.basedir=logs, the full path becomes logs/<directory>
    // We need to return the full path for LogRetentionService to work correctly
    @Value("logs/${server.tomcat.accesslog.directory:access}")
    private String tomcatLogDirectory;

    @Value("${server.tomcat.accesslog.prefix:access_log}")
    private String tomcatLogPrefix;

    @Value("${server.tomcat.accesslog.suffix:.log}")
    private String tomcatLogSuffix;

    // ==========================================
    // LOG RETENTION SETTINGS (Hardcoded Defaults)
    // ==========================================

    private static final Boolean DEFAULT_ARCHIVE_ENABLED = true;
    private static final String DEFAULT_ARCHIVE_PATH = "logs/archive";
    private static final Boolean DEFAULT_CLEANUP_ENABLED = true;

    // ==========================================
    // SHARED RETENTION SETTINGS (from Audit Log)
    // ==========================================

    /**
     * Get log retention days
     * This SHARES the retention setting with audit logs to ensure consistent data retention
     *
     * @return number of days to retain logs (from audit log settings)
     */
    public Integer getLogRetentionDays() {
        return auditLogSettingGetterServices.getAuditLogRetentionDays();
    }

    // ==========================================
    // SECURITY ANALYSIS GETTERS
    // ==========================================

    public Boolean isSecurityAnalysisEnabled() {
        return DEFAULT_SECURITY_ANALYSIS_ENABLED;
    }

    // ==========================================
    // BOT DETECTION GETTERS
    // ==========================================

    public Boolean isBotDetectionEnabled() {
        return DEFAULT_BOT_DETECTION_ENABLED;
    }

    // ==========================================
    // PERFORMANCE MONITORING GETTERS
    // ==========================================

    public Boolean isPerformanceMonitoringEnabled() {
        return DEFAULT_PERFORMANCE_MONITORING_ENABLED;
    }

    public Long getSlowRequestThreshold() {
        return DEFAULT_SLOW_REQUEST_THRESHOLD;
    }

    // ==========================================
    // LOG STORAGE GETTERS (From Tomcat Configuration)
    // ==========================================

    public String getLogDirectory() {
        return tomcatLogDirectory;
    }

    public String getLogPrefix() {
        return tomcatLogPrefix;
    }

    public String getLogSuffix() {
        return tomcatLogSuffix;
    }

    // ==========================================
    // LOG RETENTION GETTERS
    // ==========================================

    public Boolean isArchiveEnabled() {
        return DEFAULT_ARCHIVE_ENABLED;
    }

    public String getArchivePath() {
        return DEFAULT_ARCHIVE_PATH;
    }

    public Boolean isCleanupEnabled() {
        return DEFAULT_CLEANUP_ENABLED;
    }
}
