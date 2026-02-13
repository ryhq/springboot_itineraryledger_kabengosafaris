package com.itineraryledger.kabengosafaris.Backup;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for downloading backup files
 * Provides secure download endpoint for backup files
 */
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class BackupDownloadController {

    private final BackupSettingsGetterServices backupSettings;

    @Value("${backup.storage.path:/opt/lampp/htdocs/kabengosafaris/backups/}")
    private String storagePath;

    /**
     * Download a backup file by filename
     *
     * GET /api/backups/download/{filename}
     *
     * Security:
     * - Requires BACKUP_READ permission
     * - Validates filename to prevent directory traversal attacks
     * - Only allows downloading files that exist in the backup directory
     *
     * @param filename The backup filename to download
     * @return The backup file as a downloadable resource
     */
    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAuthority('PERM_DOWNLOAD_BACKUP')")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        try {
            log.info("Download request for backup file: {}", filename);

            // Security: Validate filename to prevent directory traversal
            if (!isValidFilename(filename)) {
                log.warn("Invalid filename attempted: {}", filename);
                return ResponseEntity.badRequest().build();
            }

            // Construct the full file path
            Path filePath = Paths.get(storagePath, filename).normalize();
            File file = filePath.toFile();

            // Security: Ensure the file is within the backup directory
            String backupDir = Paths.get(storagePath).normalize().toString();
            String requestedFile = filePath.normalize().toString();

            if (!requestedFile.startsWith(backupDir)) {
                log.warn("Directory traversal attempt detected: {}", filename);
                return ResponseEntity.badRequest().build();
            }

            // Check if file exists
            if (!file.exists()) {
                log.warn("Backup file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }

            // Check if it's actually a file (not a directory)
            if (!file.isFile()) {
                log.warn("Requested path is not a file: {}", filename);
                return ResponseEntity.badRequest().build();
            }

            // Prepare the file resource
            Resource resource = new FileSystemResource(file);

            // Determine content type based on file extension
            String contentType = determineContentType(filename);

            // Build response with appropriate headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading backup file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate filename to prevent directory traversal and other attacks
     *
     * @param filename The filename to validate
     * @return true if filename is valid, false otherwise
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        // Check for directory traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            return false;
        }

        // Must match expected backup filename pattern (with prefix)
        String expectedPrefix = backupSettings.getFilenamePrefix();
        if (!filename.startsWith(expectedPrefix)) {
            return false;
        }

        // Must end with .zip or be a directory name
        if (!filename.endsWith(".zip") && !filename.matches(".*_\\d{8}_\\d{6}$")) {
            return false;
        }

        return true;
    }

    /**
     * Determine content type based on file extension
     *
     * @param filename The filename
     * @return The MIME type
     */
    private String determineContentType(String filename) {
        if (filename.endsWith(".zip")) {
            return "application/zip";
        } else if (filename.endsWith(".tar.gz")) {
            return "application/gzip";
        } else if (filename.endsWith(".sql")) {
            return "application/sql";
        } else {
            return "application/octet-stream";
        }
    }
}
