package com.itineraryledger.kabengosafaris.AuditLog;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Maintenance scheduler for audit logs.
 * Automatically deletes old audit logs based on the configured retention policy.
 *
 * This service runs scheduled tasks to clean up audit logs older than the
 * configured retention period (auditLogRetentionDays).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogMaintenanceScheduler {

    private final AuditLogService auditLogService;
    private final AuditLogSettingGetterServices auditLogSettingGetterServices;

    /**
     * Delete old audit logs daily at 2 AM.
     *
     * Schedule format: cron expression (second, minute, hour, day of month, month, day of week)
     * - 0 0 2 * * *  = 2:00 AM every day
     *
     * This task respects the audit log retention policy configured in
     * AuditLogSettingGetterServices (auditLogRetentionDays).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldAuditLogsDaily() {
        try {
            log.info("Starting daily audit log cleanup task...");

            Integer retentionDays = auditLogSettingGetterServices.getAuditLogRetentionDays();
            if (retentionDays == null || retentionDays <= 0) {
                log.warn("Invalid audit log retention days configured: {}. Skipping cleanup.", retentionDays);
                return;
            }

            long deletedCount = auditLogService.deleteOldAuditLogs();

            if (deletedCount > 0) {
                log.info("Audit log cleanup completed successfully. Deleted {} logs older than {} days",
                        deletedCount, retentionDays);
            } else {
                log.info("Audit log cleanup completed. No logs older than {} days found.", retentionDays);
            }
        } catch (Exception e) {
            log.error("Error during audit log cleanup task", e);
        }
    }

    /**
     * Delete old audit logs weekly on Sunday at 3 AM.
     *
     * Schedule format: cron expression (second, minute, hour, day of month, month, day of week)
     * - 0 0 3 ? * SUN = 3:00 AM every Sunday
     *
     * This is a secondary cleanup task that runs weekly for thorough cleanup.
     * Complements the daily cleanup task.
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteOldAuditLogsWeekly() {
        try {
            log.info("Starting weekly audit log cleanup task...");

            Integer retentionDays = auditLogSettingGetterServices.getAuditLogRetentionDays();
            if (retentionDays == null || retentionDays <= 0) {
                log.warn("Invalid audit log retention days configured: {}. Skipping cleanup.", retentionDays);
                return;
            }

            long deletedCount = auditLogService.deleteOldAuditLogs();

            if (deletedCount > 0) {
                log.info("Weekly audit log cleanup completed successfully. Deleted {} logs older than {} days",
                        deletedCount, retentionDays);
            } else {
                log.info("Weekly audit log cleanup completed. No logs older than {} days found.", retentionDays);
            }
        } catch (Exception e) {
            log.error("Error during weekly audit log cleanup task", e);
        }
    }

    /**
     * Delete old audit logs every 6 hours (4 times per day).
     *
     * Schedule format: fixedRate in milliseconds
     * - fixedRate = 21600000ms = 6 hours
     * - initialDelay = 3600000ms = 1 hour (wait 1 hour before first execution)
     *
     * This task runs with a fixed rate for frequent cleanup of audit logs.
     * Useful for high-volume audit logging scenarios.
     *
     * Uncomment to use this schedule instead of the cron-based schedules above.
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 3600000)
    public void deleteOldAuditLogsFixedRate() {
        try {
            log.debug("Starting fixed-rate audit log cleanup task...");

            Integer retentionDays = auditLogSettingGetterServices.getAuditLogRetentionDays();
            if (retentionDays == null || retentionDays <= 0) {
                log.debug("Invalid audit log retention days configured: {}. Skipping cleanup.", retentionDays);
                return;
            }

            long deletedCount = auditLogService.deleteOldAuditLogs();

            if (deletedCount > 0) {
                log.debug("Fixed-rate audit log cleanup completed successfully. Deleted {} logs older than {} days",
                        deletedCount, retentionDays);
            }
        } catch (Exception e) {
            log.error("Error during fixed-rate audit log cleanup task", e);
        }
    }
}
