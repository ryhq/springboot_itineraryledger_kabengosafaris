package com.itineraryledger.kabengosafaris.Backup.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BackupListItemDTO - Lightweight backup information for listing
 * Used in paginated backup lists
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupListItemDTO {
    private String name;
    private long size;
    private String sizeFormatted;
    private LocalDateTime createdAt;
    private boolean isDirectory;
    private String fileExtension;
    private String downloadUrl;
}
