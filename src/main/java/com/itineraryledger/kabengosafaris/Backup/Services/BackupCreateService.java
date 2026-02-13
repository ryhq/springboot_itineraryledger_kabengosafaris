package com.itineraryledger.kabengosafaris.Backup.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.Backup.DTOs.BackupResultDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BackupCreateService - Service for creating backups
 *
 * Handles backup creation with proper audit logging and response formatting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BackupCreateService {

    private final BackupService backupService;
    private final BackupSettingsGetterServices backupSettings;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Trigger a manual backup
     *
     * @return ResponseEntity with BackupResultDTO
     */
    @AuditLogAnnotation(
        action = "CREATE_BACKUP",
        description = "Manually triggering backup",
        entityType = "Backup"
    )
    public ResponseEntity<ApiResponse<?>> performBackup() {
        log.info("Manual backup triggered");

        try {
            // Perform backup using internal service
            BackupService.BackupResult result = backupService.performBackup();

            // Convert to DTO
            BackupResultDTO resultDTO = convertToDTO(result);

            if (result.isSuccess()) {
                // Send success notification if enabled
                if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnSuccess()) {
                    sendSuccessNotification(result);
                }

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Backup completed successfully", resultDTO)
                );
            } else {
                // Send failure notification if enabled
                if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnFailure()) {
                    sendFailureNotification(result);
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, result.getMessage(), "BACKUP_FAILED")
                );
            }

        } catch (Exception e) {
            log.error("Error performing backup", e);

            // Send failure notification
            if (backupSettings.isNotificationEnabled() && backupSettings.notifyOnFailure()) {
                sendFailureNotification(null);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to perform backup: " + e.getMessage(), "BACKUP_EXECUTION_FAILED")
            );
        }
    }

    /**
     * Convert BackupResult to BackupResultDTO
     */
    private BackupResultDTO convertToDTO(BackupService.BackupResult result) {
        String backupName = null;
        Long backupSize = null;
        String downloadUrl = null;

        if (result.getBackupPath() != null) {
            backupName = Paths.get(result.getBackupPath()).getFileName().toString();
            File backupFile = new File(result.getBackupPath());
            if (backupFile.exists()) {
                backupSize = getFileOrDirectorySize(backupFile);
            }
            downloadUrl = baseUrl + "/api/backups/download/" + backupName;
        }

        Long durationSeconds = null;
        if (result.getStartTime() != null && result.getEndTime() != null) {
            durationSeconds = Duration.between(result.getStartTime(), result.getEndTime()).getSeconds();
        }

        return BackupResultDTO.builder()
            .success(result.isSuccess())
            .message(result.getMessage())
            .error(result.getError())
            .startTime(result.getStartTime())
            .endTime(result.getEndTime())
            .durationSeconds(durationSeconds)
            .backupPath(result.getBackupPath())
            .backupName(backupName)
            .backupSize(backupSize)
            .backupSizeFormatted(backupSize != null ? formatFileSize(backupSize) : null)
            .databaseBackupSuccess(result.isDatabaseBackupSuccess())
            .filesBackupSuccess(result.isFilesBackupSuccess())
            .compressed(result.isCompressed())
            .downloadUrl(downloadUrl)
            .build();
    }

    /**
     * Get total size of file or directory
     */
    private long getFileOrDirectorySize(File file) {
        long size = 0;
        if (file.isFile()) {
            return file.length();
        }

        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    size += f.length();
                } else {
                    size += getFileOrDirectorySize(f);
                }
            }
        }
        return size;
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Send success notification email
     */
    @Async
    private void sendSuccessNotification(BackupService.BackupResult result) {
        try {
            List<String> recipients = backupSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No notification email addresses configured");
                return;
            }

            log.info("Preparing manual backup success notification for {} recipient(s)", recipients.size());

            // Prepare variables for the email template
            Map<String, String> variables = new HashMap<>();

            // Format backup time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            variables.put("backupTime", result.getEndTime().format(dateFormatter));

            // Backup type from configuration
            variables.put("backupType", backupSettings.getBackupType());

            // Calculate and format backup size
            File backupFile = new File(result.getBackupPath());
            String backupSize = backupFile.exists() ? formatFileSize(getFileOrDirectorySize(backupFile)) : "Unknown";
            variables.put("backupSize", backupSize);

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
            String backupName = Paths.get(result.getBackupPath()).getFileName().toString();
            String downloadLink = baseUrl + "/api/backups/download/" + backupName;
            variables.put("backupDownloadLink", downloadLink);

            // Next backup time (for manual backups, we can note it's manual)
            variables.put("nextBackupTime", "As per schedule");

            // Render the email template
            String htmlContent = emailTemplateRenderer.renderTemplate("BACKUP_SUCCESS", variables);

            // Send email to all recipients
            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(
                        email,
                        "✓ Manual Backup Completed Successfully - " + result.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        htmlContent
                    );
                    log.info("Manual backup success notification sent to: {}", email);
                } catch (Exception e) {
                    log.error("Failed to send manual backup success notification to: {}", email, e);
                }
            }

        } catch (Exception e) {
            log.error("Error sending manual backup success notifications", e);
        }
    }

    /**
     * Send failure notification email
     */
    @Async
    private void sendFailureNotification(BackupService.BackupResult result) {
        try {
            List<String> recipients = backupSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No notification email addresses configured");
                return;
            }

            log.info("Preparing manual backup failure notification for {} recipient(s)", recipients.size());

            // Prepare variables for the email template
            Map<String, String> variables = new HashMap<>();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Failure time
            LocalDateTime failureTime = result != null && result.getEndTime() != null
                ? result.getEndTime()
                : LocalDateTime.now();
            variables.put("failureTime", failureTime.format(dateFormatter));

            // Backup type
            variables.put("backupType", backupSettings.getBackupType());

            // Last successful backup (we'd need to query this from database in a real implementation)
            variables.put("lastSuccessfulBackup", "Check backup history");

            // Attempt number (default to 1 for manual backups)
            variables.put("attemptNumber", "1");

            // Error message
            String errorMessage = result != null && result.getMessage() != null
                ? result.getMessage()
                : "Manual backup failed due to an unexpected error";
            variables.put("errorMessage", errorMessage);

            // Next backup time
            variables.put("nextBackupTime", "As per schedule or manual trigger");

            // Render the email template
            String htmlContent = emailTemplateRenderer.renderTemplate("BACKUP_FAILURE", variables);

            // Send email to all recipients
            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(
                        email,
                        "⚠️ Manual Backup Failed - " + failureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        htmlContent
                    );
                    log.info("Manual backup failure notification sent to: {}", email);
                } catch (Exception e) {
                    log.error("Failed to send manual backup failure notification to: {}", email, e);
                }
            }

        } catch (Exception e) {
            log.error("Error sending manual backup failure notifications", e);
        }
    }
}
