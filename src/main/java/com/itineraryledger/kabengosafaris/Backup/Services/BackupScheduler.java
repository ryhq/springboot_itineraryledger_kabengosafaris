package com.itineraryledger.kabengosafaris.Backup.Services;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled Backup Task
 * Automatically runs backups based on configured schedule
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupSettingsGetterServices backupSettings;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final BackupGetService backupGetService;

    @Value("${backup.schedule.cron:0 0 2 * * ?}")
    private String cronExpression;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Scheduled backup task
     * Runs based on cron expression from settings
     * Default: Daily at 2:00 AM (0 0 2 * * ?)
     *
     * Note: The actual schedule is controlled by backup.schedule.cron in settings
     * Spring Boot will read this dynamically using ${backup.schedule.cron}
     */
    @Scheduled(cron = "${backup.schedule.cron:0 0 2 * * ?}")
    public void scheduledBackup() {
        // Check if scheduled backups are enabled
        if (!backupSettings.isScheduleEnabled()) {
            log.debug("Scheduled backup skipped - scheduling is disabled in settings");
            return;
        }

        // Check if backup system is enabled
        if (!backupSettings.isBackupEnabled()) {
            log.debug("Scheduled backup skipped - backup system is disabled");
            return;
        }

        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║               SCHEDULED BACKUP TASK TRIGGERED                      ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");

        try {
            BackupService.BackupResult result = backupService.performBackup();

            if (result.isSuccess()) {
                log.info("Scheduled backup completed successfully");
                log.info("Backup location: {}", result.getBackupPath());

                // Send success notification if enabled
                if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnSuccess()) {
                    sendSuccessNotification(result);
                }
            } else {
                log.error("Scheduled backup failed: {}", result.getMessage());

                // Send failure notification if enabled
                if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnFailure()) {
                    sendFailureNotification(result);
                }
            }

        } catch (Exception e) {
            log.error("Scheduled backup encountered an exception", e);

            // Send failure notification
            if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnFailure()) {
                sendFailureNotification(null);
            }
        }
    }

    /**
     * Send success notification
     */
    private void sendSuccessNotification(BackupService.BackupResult result) {
        try {
            List<String> recipients = backupSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No notification email addresses configured");
                return;
            }

            log.info("Preparing backup success notification for {} recipient(s)", recipients.size());

            // Prepare variables for the email template
            Map<String, String> variables = new HashMap<>();

            // Format backup time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            variables.put("backupTime", result.getEndTime().format(dateFormatter));

            // Backup type from configuration
            variables.put("backupType", backupSettings.getBackupType());

            // Calculate and format backup size
            variables.put("backupSize", calculateBackupSize(result.getBackupPath()));

            // Database and files inclusion
            variables.put("databaseIncluded", result.isDatabaseBackupSuccess() ? "Yes" : "No");
            variables.put("filesIncluded", result.isFilesBackupSuccess() ? "Yes" : "No");

            // Compression settings
            variables.put("compressionFormat", backupSettings.getCompressionFormat().toLowerCase());
            variables.put("compressionLevel", String.valueOf(backupSettings.getCompressionLevel()));

            // Backup path and retention
            variables.put("backupPath", result.getBackupPath());
            variables.put("retentionDays", String.valueOf(backupSettings.getRetentionDays()));

            // Generate download link
            String downloadLink = generateDownloadLink(result.getBackupPath());
            variables.put("backupDownloadLink", downloadLink);

            // Next backup time calculated from cron expression
            variables.put("nextBackupTime", calculateNextBackupTime());

            // Render the email template
            String htmlContent = emailTemplateRenderer.renderTemplate("BACKUP_SUCCESS", variables);

            // Send email to all recipients
            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(
                        email,
                        "✓ Backup Completed Successfully - " + result.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        htmlContent
                    );
                    log.info("Backup success notification sent to: {}", email);
                } catch (Exception e) {
                    log.error("Failed to send backup success notification to: {}", email, e);
                }
            }

        } catch (Exception e) {
            log.error("Error sending backup success notifications", e);
        }
    }

    /**
     * Send failure notification
     */
    private void sendFailureNotification(BackupService.BackupResult result) {
        try {
            List<String> recipients = backupSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No notification email addresses configured");
                return;
            }

            log.warn("Preparing backup failure notification for {} recipient(s)", recipients.size());

            // Prepare variables for the email template
            Map<String, String> variables = new HashMap<>();

            // Format failure time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime failureTime = result != null && result.getEndTime() != null
                ? result.getEndTime()
                : LocalDateTime.now();
            variables.put("failureTime", failureTime.format(dateFormatter));

            // Backup type from configuration
            variables.put("backupType", backupSettings.getBackupType());

            // Get last successful backup from backup directory
            BackupGetService.BackupInfo lastBackup = backupGetService.getLastSuccessfulBackup();
            if (lastBackup != null) {
                variables.put("lastSuccessfulBackup", lastBackup.getCreatedAt().format(dateFormatter));
            } else {
                variables.put("lastSuccessfulBackup", "Never - No previous successful backups found");
            }

            // Attempt number is now optional with default value of 1, so we can omit it
            // If you want to track consecutive failures, implement a tracking system and set it here

            // Error message
            String errorMessage = "Unknown error";
            if (result != null) {
                if (result.getError() != null && !result.getError().isEmpty()) {
                    errorMessage = result.getError();
                } else if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                    errorMessage = result.getMessage();
                }
            }
            variables.put("errorMessage", errorMessage);

            // Next backup time calculated from cron expression
            variables.put("nextBackupTime", calculateNextBackupTime());

            // Render the email template
            String htmlContent = emailTemplateRenderer.renderTemplate("BACKUP_FAILURE", variables);

            // Send email to all recipients
            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(
                        email,
                        "✗ Backup Failed - Immediate Attention Required - " + failureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        htmlContent
                    );
                    log.warn("Backup failure notification sent to: {}", email);
                } catch (Exception e) {
                    log.error("Failed to send backup failure notification to: {}", email, e);
                }
            }

        } catch (Exception e) {
            log.error("Error sending backup failure notifications", e);
        }
    }

    /**
     * Calculate backup file size in human-readable format
     *
     * @param backupPath Path to the backup file
     * @return Formatted size string (e.g., "1.24 GB", "456 MB")
     */
    private String calculateBackupSize(String backupPath) {
        try {
            if (backupPath == null || backupPath.isEmpty()) {
                return "Unknown";
            }

            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                return "File not found";
            }

            long bytes = backupFile.length();

            if (bytes < 1024) {
                return bytes + " B";
            }

            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);

        } catch (Exception e) {
            log.error("Error calculating backup size for: {}", backupPath, e);
            return "Unknown";
        }
    }

    /**
     * Calculate the next scheduled backup time based on cron expression
     *
     * @return Formatted next backup time, or generic message if calculation fails
     */
    private String calculateNextBackupTime() {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime nextExecution = cron.next(LocalDateTime.now());

            if (nextExecution != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return nextExecution.format(formatter);
            } else {
                return "Not scheduled";
            }
        } catch (Exception e) {
            log.warn("Failed to calculate next backup time from cron expression: {}", cronExpression, e);
            return "As per configured schedule";
        }
    }

    /**
     * Generate a download link for the backup file
     *
     * @param backupPath Full file system path to the backup file
     * @return HTTP URL to download the backup file
     */
    private String generateDownloadLink(String backupPath) {
        try {
            if (backupPath == null || backupPath.isEmpty()) {
                return "Download link unavailable";
            }

            // Extract filename from the full path
            String filename = Paths.get(backupPath).getFileName().toString();

            // Build the download URL
            String downloadUrl = baseUrl + "/api/backups/download/" + filename;

            return downloadUrl;

        } catch (Exception e) {
            log.error("Error generating download link for: {}", backupPath, e);
            return "Download link unavailable";
        }
    }
}
