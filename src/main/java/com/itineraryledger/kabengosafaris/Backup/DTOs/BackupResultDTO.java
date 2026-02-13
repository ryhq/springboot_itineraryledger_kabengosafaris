package com.itineraryledger.kabengosafaris.Backup.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BackupResultDTO - Result of a backup operation
 * Contains success status, timing, and backup details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResultDTO {
    private boolean success;
    private String message;
    private String error;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;

    // Backup file details
    private String backupPath;
    private String backupName;
    private Long backupSize;
    private String backupSizeFormatted;

    // Component success flags
    private boolean databaseBackupSuccess;
    private boolean filesBackupSuccess;
    private boolean compressed;

    // Download URL
    private String downloadUrl;
}
