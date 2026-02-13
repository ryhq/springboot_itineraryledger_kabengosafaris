package com.itineraryledger.kabengosafaris.Backup.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BackupDTO - Full details of a backup
 * Used when retrieving a single backup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupDTO {
    private String name;
    private String path;
    private long size;
    private String sizeFormatted;
    private LocalDateTime createdAt;
    private boolean isDirectory;
    private boolean isCompressed;
    private String compressionFormat;

    // Backup metadata
    private boolean containsDatabase;
    private boolean containsFiles;

    // File extension (for determining backup type)
    private String fileExtension;

    // Download URL
    private String downloadUrl;
}
