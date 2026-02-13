package com.itineraryledger.kabengosafaris.Backup;

import com.itineraryledger.kabengosafaris.Backup.Services.BackupCreateService;
import com.itineraryledger.kabengosafaris.Backup.Services.BackupDeleteService;
import com.itineraryledger.kabengosafaris.Backup.Services.BackupGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for Backup Operations
 * Provides endpoints to trigger backups, view backup history, and manage backups
 */
@RestController
@RequestMapping("/api/backups")
@Slf4j
@RequiredArgsConstructor
public class BackupController {

    private final BackupCreateService backupCreateService;
    private final BackupGetService backupGetService;
    private final BackupDeleteService backupDeleteService;

    /**
     * Trigger a manual backup
     *
     * POST /api/backups/trigger
     *
     * @return ResponseEntity with backup result
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('PERM_TRIGGER_MANUAL_BACKUP')")
    public ResponseEntity<ApiResponse<?>> triggerBackup() {
        log.info("POST /api/backups/trigger - Manual backup triggered");
        return backupCreateService.performBackup();
    }

    /**
     * List all available backups with pagination and filtering
     *
     * GET /api/backups
     *
     * @param filename Filter by filename (partial match)
     * @param startDate Filter by creation date (after)
     * @param endDate Filter by creation date (before)
     * @param minSize Minimum file size in bytes
     * @param maxSize Maximum file size in bytes
     * @param isCompressed Filter by compression status
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field (name, size, createdAt)
     * @param sortDirection Sort direction (asc, desc)
     * @return ResponseEntity with paginated list of backups
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_BACKUP_HISTORY')")
    public ResponseEntity<ApiResponse<?>> listBackups(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) Boolean isCompressed,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/backups - Fetching backup list with pagination");
        return backupGetService.getAllBackups(
            filename,
            startDate,
            endDate,
            minSize,
            maxSize,
            isCompressed,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get a single backup by filename
     *
     * GET /api/backups/{filename}
     *
     * @param filename The backup filename
     * @return ResponseEntity with backup details
     */
    @GetMapping("/{filename}")
    @PreAuthorize("hasAuthority('PERM_VIEW_BACKUP_HISTORY')")
    public ResponseEntity<ApiResponse<?>> getBackupByFilename(@PathVariable String filename) {
        log.info("GET /api/backups/{} - Fetching backup details", filename);
        return backupGetService.getBackupByFilename(filename);
    }

    /**
     * Get backup statistics
     *
     * GET /api/backups/stats
     *
     * @return ResponseEntity with backup statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_VIEW_BACKUP_HISTORY')")
    public ResponseEntity<ApiResponse<?>> getBackupStats() {
        log.info("GET /api/backups/stats - Fetching backup statistics");
        return backupGetService.getBackupStatistics();
    }

    /**
     * Delete backups by filename
     *
     * DELETE /api/backups
     *
     * @param filenames List of backup filenames to delete
     * @return ResponseEntity with deletion result
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_BACKUP')")
    public ResponseEntity<ApiResponse<?>> deleteBackups(@RequestBody List<String> filenames) {
        log.info("DELETE /api/backups - Deleting {} backups", filenames.size());
        return backupDeleteService.deleteBackups(filenames);
    }

    /**
     * Cleanup old backups manually
     *
     * POST /api/backups/cleanup
     *
     * @return ResponseEntity with cleanup result
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('PERM_DELETE_BACKUP')")
    public ResponseEntity<ApiResponse<?>> cleanupBackups() {
        log.info("POST /api/backups/cleanup - Manual cleanup triggered");
        return backupDeleteService.cleanupOldBackups();
    }

    /**
     * Health check endpoint
     *
     * GET /api/backups/health
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('PERM_VIEW_BACKUP_HISTORY')")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Backup API is healthy", null)
        );
    }
}
