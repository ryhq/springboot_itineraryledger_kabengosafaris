package com.itineraryledger.kabengosafaris.Backup.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * BackupDeleteService - Service for deleting backup files
 *
 * Provides safe deletion of backup files with validation and audit logging
 */
@Service
@Slf4j
@Transactional
public class BackupDeleteService {

    private final BackupSettingsGetterServices backupSettings;

    @Value("${backup.storage.path:./backups/}")
    private String storagePath;

    @Autowired
    public BackupDeleteService(BackupSettingsGetterServices backupSettings) {
        this.backupSettings = backupSettings;
    }

    /**
     * Delete backups by list of filenames
     *
     * @param filenames List of backup filenames to delete
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteBackups(List<String> filenames) {
        log.info("Attempting to delete {} backups", filenames.size());

        if (filenames == null || filenames.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No backup filenames provided", "NO_FILENAMES_PROVIDED")
            );
        }

        try {
            return deleteBackupsInternal(filenames);

        } catch (Exception e) {
            log.error("Error deleting backups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete backups",
                    "BACKUPS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete backups by list of filenames (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteBackupsInternal(List<String> filenames) {
        int deletedCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (String filename : filenames) {
            try {
                // Security: Validate filename
                if (filename == null || filename.trim().isEmpty()) {
                    skippedCount++;
                    skippedReasons.add("Empty filename provided");
                    continue;
                }

                // Security: Prevent directory traversal
                if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                    log.warn("Invalid filename attempted: {}", filename);
                    skippedCount++;
                    skippedReasons.add(String.format("Invalid filename: %s (contains directory traversal characters)", filename));
                    continue;
                }

                // Security: Ensure filename starts with backup prefix
                if (!filename.startsWith(backupSettings.getFilenamePrefix())) {
                    log.warn("Filename doesn't match backup prefix: {}", filename);
                    skippedCount++;
                    skippedReasons.add(String.format("Invalid filename: %s (doesn't match backup prefix)", filename));
                    continue;
                }

                // Check if file exists
                File backupFile = new File(storagePath, filename);
                if (!backupFile.exists()) {
                    log.warn("Backup file not found: {}", filename);
                    skippedCount++;
                    skippedReasons.add(String.format("Backup not found: %s", filename));
                    continue;
                }

                // Security: Ensure file is within backup directory
                String backupDir = new File(storagePath).getCanonicalPath();
                String requestedFile = backupFile.getCanonicalPath();
                if (!requestedFile.startsWith(backupDir)) {
                    log.warn("Path traversal attempt detected: {}", filename);
                    skippedCount++;
                    skippedReasons.add(String.format("Invalid backup path: %s", filename));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((BackupDeleteService) AopContext.currentProxy()).deleteBackup(filename);
                deletedCount++;
                log.info("Backup deleted successfully: {}", filename);

            } catch (Exception e) {
                log.error("Error deleting backup: {}", filename, e);
                skippedCount++;
                skippedReasons.add(String.format("Error deleting backup %s: %s", filename, e.getMessage()));
            }
        }

        // Build response message
        StringBuilder message = new StringBuilder();
        if (deletedCount > 0) {
            message.append(deletedCount)
                   .append(deletedCount > 1 ? " backups deleted successfully" : " backup deleted successfully");
        }

        if (skippedCount > 0) {
            if (deletedCount > 0) {
                message.append(". ");
            }
            message.append(skippedCount)
                   .append(skippedCount > 1 ? " backups skipped" : " backup skipped");
        }

        // Return appropriate response
        if (deletedCount == 0 && skippedCount > 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    message.toString() + ": " + String.join("; ", skippedReasons),
                    "NO_BACKUPS_DELETED"
                )
            );
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                message.toString(),
                skippedCount > 0 ? skippedReasons : null
            )
        );
    }

    /**
     * Delete a single backup by filename (internal method with audit logging)
     *
     * @param filename Backup filename to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_BACKUP",
        description = "Deleting backup file",
        entityType = "Backup"
    )
    public void deleteBackup(String filename) {
        File backupFile = new File(storagePath, filename);

        if (backupFile.isDirectory()) {
            deleteDirectory(backupFile);
        } else {
            if (!backupFile.delete()) {
                throw new RuntimeException("Failed to delete backup file: " + filename);
            }
        }
    }

    /**
     * Clean up old backups based on retention policy
     *
     * @return ResponseEntity with cleanup result
     */
    @AuditLogAnnotation(
        action = "CLEANUP_OLD_BACKUPS",
        description = "Cleaning up old backups based on retention policy",
        entityType = "Backup"
    )
    public ResponseEntity<ApiResponse<?>> cleanupOldBackups() {
        log.info("Starting old backups cleanup");

        try {
            int retentionDays = backupSettings.getRetentionDays();
            int maxCount = backupSettings.getRetentionMaxCount();

            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "No backups directory found, nothing to clean up", null)
                );
            }

            // Get all backup files/directories
            File[] backups = storageDir.listFiles((dir, name) ->
                    name.startsWith(backupSettings.getFilenamePrefix()));

            if (backups == null || backups.length == 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "No backups found to clean up", null)
                );
            }

            // Sort by last modified date (oldest first)
            java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));

            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(retentionDays);
            int deletedCount = 0;

            // Delete backups older than retention days or exceeding max count
            for (int i = 0; i < backups.length; i++) {
                File backup = backups[i];
                java.time.LocalDateTime backupDate = java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(backup.lastModified()),
                        java.time.ZoneId.systemDefault()
                );

                boolean shouldDelete = backupDate.isBefore(cutoffDate) ||
                        (backups.length - i > maxCount);

                if (shouldDelete) {
                    if (deleteBackupFile(backup)) {
                        deletedCount++;
                        log.info("Deleted old backup: {}", backup.getName());
                    }
                }
            }

            String message = deletedCount > 0
                ? deletedCount + " old backup(s) deleted"
                : "No old backups to clean up";

            log.info("Cleanup completed: {}", message);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message, null)
            );

        } catch (Exception e) {
            log.error("Failed to cleanup old backups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to cleanup old backups", "BACKUP_CLEANUP_FAILED")
            );
        }
    }

    /**
     * Delete a backup (file or directory) - internal helper
     *
     * @param backup backup file or directory
     * @return true if successful
     */
    private boolean deleteBackupFile(File backup) {
        if (backup.isDirectory()) {
            return deleteDirectory(backup);
        } else {
            return backup.delete();
        }
    }

    /**
     * Delete directory recursively
     *
     * @param directory directory to delete
     * @return true if successful
     */
    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }
}
