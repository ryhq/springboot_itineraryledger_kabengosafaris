package com.itineraryledger.kabengosafaris.AuditLog;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogSettingGetterServices auditLogSettingGetterServices;

    /**
     * Log an action asynchronously to avoid blocking the main request
     * Respects audit logging policies
     */
    @Async
    public void logAction(AuditLog auditLog) {
        try {
            // Check if audit logging is enabled globally
            if (!auditLogSettingGetterServices.isAuditLogEnabled()) {
                log.debug("Audit logging is disabled, skipping: {} - {}", auditLog.getAction(), auditLog.getEntityType());
                return;
            }

            // Apply audit logging policies
            applyAuditPolicies(auditLog);

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {} - {}", auditLog.getUsername(), auditLog.getAction(), auditLog.getEntityType());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Synchronously log an action (blocks until saved)
     * Respects audit logging policies
     */
    public void logActionSync(AuditLog auditLog) {
        try {
            // Check if audit logging is enabled globally
            if (!auditLogSettingGetterServices.isAuditLogEnabled()) {
                log.debug("Audit logging is disabled, skipping: {} - {}", auditLog.getAction(), auditLog.getEntityType());
                return;
            }

            // Apply audit logging policies
            applyAuditPolicies(auditLog);

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved synchronously: {} - {} - {}", auditLog.getUsername(), auditLog.getAction(), auditLog.getEntityType());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Apply audit logging policies to the audit log
     * Enforces capture policies, field exclusions, and value length limits
     *
     * @param auditLog the audit log to apply policies to
     */
    private void applyAuditPolicies(AuditLog auditLog) {
        // Apply IP address capture policy
        if (!auditLogSettingGetterServices.shouldCaptureIpAddress()) {
            auditLog.setIpAddress(null);
        }

        // Apply user agent capture policy
        if (!auditLogSettingGetterServices.shouldCaptureUserAgent()) {
            auditLog.setUserAgent(null);
        }

        // Apply old values capture policy
        if (!auditLogSettingGetterServices.shouldCaptureOldValues()) {
            auditLog.setOldValues(null);
        }

        // Apply new values capture policy
        if (!auditLogSettingGetterServices.shouldCaptureNewValues()) {
            auditLog.setNewValues(null);
        }

        // Apply excluded fields policy
        Set<String> excludedFields = parseExcludedFields();
        auditLog.setOldValues(filterExcludedFields(auditLog.getOldValues(), excludedFields));
        auditLog.setNewValues(filterExcludedFields(auditLog.getNewValues(), excludedFields));

        // Apply max value length policy
        Integer maxValueLength = auditLogSettingGetterServices.getMaxValueLength();
        if (maxValueLength != null && maxValueLength > 0) {
            if (auditLog.getOldValues() != null && auditLog.getOldValues().length() > maxValueLength) {
                auditLog.setOldValues(auditLog.getOldValues().substring(0, maxValueLength) + "... [TRUNCATED]");
            }
            if (auditLog.getNewValues() != null && auditLog.getNewValues().length() > maxValueLength) {
                auditLog.setNewValues(auditLog.getNewValues().substring(0, maxValueLength) + "... [TRUNCATED]");
            }
        }
    }

    /**
     * Parse excluded fields from the configuration
     * @return set of lowercase field names to exclude
     */
    private Set<String> parseExcludedFields() {
        String excludedFieldsStr = auditLogSettingGetterServices.getExcludedFields();
        Set<String> excludedFields = new HashSet<>();
        if (excludedFieldsStr != null && !excludedFieldsStr.isEmpty()) {
            Arrays.stream(excludedFieldsStr.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .forEach(excludedFields::add);
        }
        return excludedFields;
    }

    /**
     * Filter out excluded fields from JSON values
     * Attempts to remove excluded fields from JSON strings
     *
     * @param jsonValue the JSON string to filter
     * @param excludedFields set of field names to exclude (lowercase)
     * @return filtered JSON string or original if filtering fails
     */
    private String filterExcludedFields(String jsonValue, Set<String> excludedFields) {
        if (jsonValue == null || jsonValue.isEmpty() || excludedFields.isEmpty()) {
            return jsonValue;
        }

        try {
            // Simple regex-based field removal from JSON
            String filtered = jsonValue;
            for (String field : excludedFields) {
                // Match field name (case-insensitive) and remove the key-value pair
                // Pattern: "fieldName":"value" or "fieldName":value (for various value types)
                filtered = filtered.replaceAll("(?i)\"" + Pattern.quote(field) + "\"\\s*:\\s*[^,}]*", "");
                // Clean up any leftover commas
                filtered = filtered.replaceAll(",\\s*,", ",");
                filtered = filtered.replaceAll(",\\s*}", "}");
                filtered = filtered.replaceAll("\\{,", "{");
            }
            return filtered;
        } catch (Exception e) {
            log.debug("Failed to filter excluded fields from audit log values", e);
            return jsonValue;
        }
    }

    /**
     * Delete old audit logs (retention policy)
     * Keeps logs for specified number of days based on policy
     */
    @Transactional
    public long deleteOldAuditLogs() {
        Integer retentionDays = auditLogSettingGetterServices.getAuditLogRetentionDays();
        if (retentionDays == null || retentionDays <= 0) {
            log.warn("Invalid retention days configured: {}", retentionDays);
            return 0;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldLogs = auditLogRepository.findByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                cutoffDate,
                PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        long deletedCount = oldLogs.size();
        if (deletedCount > 0) {
            auditLogRepository.deleteAll(oldLogs);
            log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        }
        return deletedCount;
    }

    /**
     * Delete old audit logs with specified retention days
     * Overloaded method for backward compatibility
     *
     * @param retentionDays number of days to retain logs
     * @return number of deleted logs
     */
    @Transactional
    public long deleteOldAuditLogs(int retentionDays) {
        if (retentionDays <= 0) {
            log.warn("Invalid retention days specified: {}", retentionDays);
            return 0;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldLogs = auditLogRepository.findByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                cutoffDate,
                PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        long deletedCount = oldLogs.size();
        if (deletedCount > 0) {
            auditLogRepository.deleteAll(oldLogs);
            log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        }
        return deletedCount;
    }

}
