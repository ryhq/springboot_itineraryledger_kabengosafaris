package com.itineraryledger.kabengosafaris.AuditLog.Config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties class for Audit Logging settings.
 * These properties can be bound from both application.properties and the AuditLogConfig database table.
 *
 * Properties are prefixed with 'audit' and are automatically injected as @ConfigurationProperties.
 *
 * Example usage:
 * - @Autowired private AuditLogConfigProperties auditLogProperties;
 * - auditLogProperties.getLog().isEnabled()
 * - auditLogProperties.getLog().getRetentionDays()
 * - auditLogProperties.getTask().getExecution().getPool().getCoreSize()
 */
@Component
@ConfigurationProperties(prefix = "audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogConfigProperties {

    /**
     * Nested configuration for audit.log.* properties
     */
    private Log log = new Log();

    /**
     * Audit Log Configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Log {

        /**
         * Enable or disable audit logging globally
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Audit log retention in days - logs older than this will be cleaned up
         * Default: 90
         */
        private int retentionDays = 90;

        /**
         * Whether to capture IP address in audit logs
         * Default: true
         */
        private boolean captureIpAddress = true;

        /**
         * Whether to capture User-Agent in audit logs
         * Default: true
         */
        private boolean captureUserAgent = true;

        /**
         * Whether to capture old values before update
         * Default: true
         */
        private boolean captureOldValues = true;

        /**
         * Whether to capture new values after update
         * Default: true
         */
        private boolean captureNewValues = true;

        /**
         * Comma-separated list of fields to exclude from audit logs (case-insensitive)
         * Examples: password, token, secret, apiKey, creditCard
         * Default: password,token,secret,apikey,creditcard
         */
        private String excludedFields = "password,token,secret,apikey,creditcard";

        /**
         * Maximum length of captured old/new values (to prevent huge JSON strings)
         * Default: 5000
         */
        private int maxValueLength = 5000;
    }
}
