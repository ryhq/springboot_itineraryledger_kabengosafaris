package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve Audit Logging Settings with fallback values from application.properties
 *
 * This service provides getter methods for all audit logging settings defined in the database.
 * If a setting is not found in the database, it falls back to the default values from application.properties.
 * If a setting is inactive, the fallback value is used instead.
 */
@Service
public class AuditLogSettingGetterServices {

    // Fallback values from application.properties
    

    /**
     * ############################################
     * ### Audit Logging General Configurations ###
     * ############################################
     */
    @Value("${audit.log.retention.days:90}")
    private Integer defaultAuditLogRetentionDays;

    @Value("${audit.log.enabled:true}")
    private Boolean defaultAuditLogEnabled;

    /**
     * ############################################
     * ### Audit Logging Capture Configurations ###
     * ############################################
     */
    @Value("${audit.log.capture.ip.address:true}")
    private Boolean defaultCaptureIpAddress;

    @Value("${audit.log.capture.user.agent:true}")
    private Boolean defaultCaptureUserAgent;

    @Value("${audit.log.capture.old.values:true}")
    private Boolean defaultCaptureOldValues;

    @Value("${audit.log.capture.new.values:true}")
    private Boolean defaultCaptureNewValues;

    /**
     * ##########################################
     * ### Audit Logging Value Configurations ###
     * ##########################################
     */
    @Value("${audit.log.excluded.fields:password,token,secret,apikey,creditcard}")
    private String defaultExcludedFields;

    @Value("${audit.log.max.value.length:2048}")
    private Integer defaultMaxValueLength;

    /**
     * Repository to access audit log settings from database
     */
    @Autowired
    private AuditLogSettingRepository auditLogSettingRepository;

    /**
     * ##########################################
     * ### Audit Logging General Getters ###
     * ##########################################
     */

    /**
     * Get audit logging enabled status
     * @return whether audit logging is enabled
     */
    public Boolean isAuditLogEnabled() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.enabled");
        if (setting == null) {
            return defaultAuditLogEnabled;
        }
        if (!setting.getActive()) {
            return defaultAuditLogEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return defaultAuditLogEnabled;
        }
    }

    /**
     * Get audit log retention days
     * @return number of days to retain audit logs
     */
    public Integer getAuditLogRetentionDays() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.retention.days");
        if (setting == null) {
            return defaultAuditLogRetentionDays;
        }
        if (!setting.getActive()) {
            return defaultAuditLogRetentionDays;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultAuditLogRetentionDays;
        }
    }

    /**
     * ##########################################
     * ### Audit Logging Capture Getters ###
     * ##########################################
     */

    /**
     * Get whether to capture IP address in audit logs
     * @return true if IP address should be captured
     */
    public Boolean shouldCaptureIpAddress() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.capture.ip.address");
        if (setting == null) {
            return defaultCaptureIpAddress;
        }
        if (!setting.getActive()) {
            return defaultCaptureIpAddress;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return defaultCaptureIpAddress;
        }
    }

    /**
     * Get whether to capture user agent in audit logs
     * @return true if user agent should be captured
     */
    public Boolean shouldCaptureUserAgent() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.capture.user.agent");
        if (setting == null) {
            return defaultCaptureUserAgent;
        }
        if (!setting.getActive()) {
            return defaultCaptureUserAgent;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return defaultCaptureUserAgent;
        }
    }

    /**
     * Get whether to capture old values in audit logs
     * @return true if old values should be captured
     */
    public Boolean shouldCaptureOldValues() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.capture.old.values");
        if (setting == null) {
            return defaultCaptureOldValues;
        }
        if (!setting.getActive()) {
            return defaultCaptureOldValues;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return defaultCaptureOldValues;
        }
    }

    /**
     * Get whether to capture new values in audit logs
     * @return true if new values should be captured
     */
    public Boolean shouldCaptureNewValues() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.capture.new.values");
        if (setting == null) {
            return defaultCaptureNewValues;
        }
        if (!setting.getActive()) {
            return defaultCaptureNewValues;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return defaultCaptureNewValues;
        }
    }

    /**
     * ##########################################
     * ### Audit Logging Value Getters ###
     * ##########################################
     */

    /**
     * Get excluded fields from audit logging
     * @return comma-separated list of excluded fields
     */
    public String getExcludedFields() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.excluded.fields");
        if (setting == null) {
            return defaultExcludedFields;
        }
        if (!setting.getActive()) {
            return defaultExcludedFields;
        }
        try {
            return setting.getSettingValue();
        } catch (Exception e) {
            return defaultExcludedFields;
        }
    }

    /**
     * Get maximum length for captured field values
     * @return maximum value length
     */
    public Integer getMaxValueLength() {
        AuditLogSetting setting = auditLogSettingRepository.findBySettingKey("audit.log.max.value.length");
        if (setting == null) {
            return defaultMaxValueLength;
        }
        if (!setting.getActive()) {
            return defaultMaxValueLength;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultMaxValueLength;
        }
    }
}
