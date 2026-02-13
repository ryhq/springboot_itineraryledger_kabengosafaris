package com.itineraryledger.kabengosafaris.Backup.Services;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Core Backup Service
 * Handles database and file system backups with compression and retention management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BackupService {

    private final BackupSettingsGetterServices backupSettings;

    @Value("${spring.datasource.password:}")
    private String databasePassword;

    @Value("${backup.files.base.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/}")
    private String filesBasePath;

    @Value("${backup.storage.path:/opt/lampp/htdocs/kabengosafaris/backups/}")
    private String storagePath;

    /**
     * Perform a complete backup (database + files)
     *
     * @return BackupResult with status and details
     */
    public BackupResult performBackup() {
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║                    BACKUP PROCESS STARTED                          ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");

        BackupResult result = new BackupResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Check if backup is enabled
            if (!backupSettings.isBackupEnabled()) {
                log.warn("Backup is disabled in settings");
                result.setSuccess(false);
                result.setMessage("Backup is disabled in settings");
                result.setEndTime(LocalDateTime.now());
                return result;
            }

            // Generate backup filename
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern(backupSettings.getFilenameDateFormat())
            );
            String backupFileName = backupSettings.getFilenamePrefix() + "_" + timestamp;

            // Ensure storage directory exists
            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
                log.info("Created backup storage directory: {}", storagePath);
            }

            // Create temporary directory for this backup
            String tempBackupDir = storagePath + backupFileName + "_temp/";
            File tempDir = new File(tempBackupDir);
            tempDir.mkdirs();

            // Perform database backup
            if (backupSettings.isDatabaseBackupEnabled()) {
                log.info("Starting database backup...");
                String dbBackupPath = backupDatabase(tempBackupDir, timestamp);
                if (dbBackupPath != null) {
                    result.setDatabaseBackupPath(dbBackupPath);
                    result.setDatabaseBackupSuccess(true);
                    log.info("✓ Database backup completed: {}", dbBackupPath);
                } else {
                    result.setDatabaseBackupSuccess(false);
                    log.error("✗ Database backup failed");
                }
            }

            // Perform file system backup
            if (backupSettings.isFilesBackupEnabled()) {
                log.info("Starting file system backup...");
                String filesBackupPath = backupFiles(tempBackupDir);
                if (filesBackupPath != null) {
                    result.setFilesBackupPath(filesBackupPath);
                    result.setFilesBackupSuccess(true);
                    log.info("✓ File system backup completed: {}", filesBackupPath);
                } else {
                    result.setFilesBackupSuccess(false);
                    log.error("✗ File system backup failed");
                }
            }

            // Compress backup if enabled
            String finalBackupPath;
            if (backupSettings.isCompressionEnabled()) {
                log.info("Compressing backup...");
                finalBackupPath = compressBackup(tempBackupDir, storagePath, backupFileName);
                result.setCompressed(true);
                log.info("✓ Backup compressed: {}", finalBackupPath);
            } else {
                // Move temp directory to final location
                finalBackupPath = storagePath + backupFileName + "/";
                Files.move(Paths.get(tempBackupDir), Paths.get(finalBackupPath),
                        StandardCopyOption.REPLACE_EXISTING);
                result.setCompressed(false);
            }

            result.setBackupPath(finalBackupPath);
            result.setSuccess(true);
            result.setMessage("Backup completed successfully");

            // Clean up old backups if auto-cleanup is enabled
            if (backupSettings.isAutoCleanupEnabled()) {
                cleanupOldBackups();
            }

            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║              ✓ BACKUP PROCESS COMPLETED SUCCESSFULLY               ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Backup process failed", e);
            result.setSuccess(false);
            result.setMessage("Backup failed: " + e.getMessage());
            result.setError(e.getMessage());

            log.error("╔════════════════════════════════════════════════════════════════════╗");
            log.error("║                                                                    ║");
            log.error("║                  ✗ BACKUP PROCESS FAILED                           ║");
            log.error("║                                                                    ║");
            log.error("╚════════════════════════════════════════════════════════════════════╝");
        }

        result.setEndTime(LocalDateTime.now());
        return result;
    }

    /**
     * Backup database using mysqldump
     *
     * @param outputDir directory to store backup
     * @param timestamp timestamp for filename
     * @return path to backup file, or null if failed
     */
    private String backupDatabase(String outputDir, String timestamp) {
        try {
            String dbName = backupSettings.getDatabaseName();
            String dbHost = backupSettings.getDatabaseHost();
            Integer dbPort = backupSettings.getDatabasePort();
            String dbUser = backupSettings.getDatabaseUsername();
            String backupFile = outputDir + "database_" + timestamp + ".sql";

            // Build mysqldump command
            List<String> command = new ArrayList<>();
            command.add("mysqldump");
            command.add("-h" + dbHost);
            command.add("-P" + dbPort);
            command.add("-u" + dbUser);

            if (databasePassword != null && !databasePassword.isEmpty()) {
                command.add("-p" + databasePassword);
            }

            // Add options based on settings
            if (backupSettings.includeRoutines()) {
                command.add("--routines");
            }
            if (backupSettings.includeTriggers()) {
                command.add("--triggers");
            }
            if (backupSettings.includeEvents()) {
                command.add("--events");
            }

            command.add("--single-transaction");
            command.add("--quick");
            command.add("--lock-tables=false");
            command.add(dbName);
            command.add("--result-file=" + backupFile);

            // Execute command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Database backup created successfully: {}", backupFile);
                return backupFile;
            } else {
                log.error("mysqldump failed with exit code: {}. Output: {}", exitCode, output);
                return null;
            }

        } catch (Exception e) {
            log.error("Database backup failed", e);
            return null;
        }
    }

    /**
     * Backup file system directories
     *
     * @param outputDir directory to store backup
     * @return path to files backup directory, or null if failed
     */
    private String backupFiles(String outputDir) {
        try {
            String filesOutputDir = outputDir + "files/";
            Files.createDirectories(Paths.get(filesOutputDir));

            Map<String, Boolean> fileInclusions = getFileInclusionSettings();

            int totalDirectories = 0;
            int successfulBackups = 0;

            for (Map.Entry<String, Boolean> entry : fileInclusions.entrySet()) {
                if (entry.getValue()) {
                    String dirName = entry.getKey();
                    String sourcePath = filesBasePath + dirName;
                    String targetPath = filesOutputDir + dirName;

                    totalDirectories++;
                    if (copyDirectory(sourcePath, targetPath)) {
                        successfulBackups++;
                        log.info("✓ Backed up: {}", dirName);
                    } else {
                        log.warn("✗ Failed to backup: {}", dirName);
                    }
                }
            }

            log.info("File backup completed: {}/{} directories backed up successfully",
                    successfulBackups, totalDirectories);

            return filesOutputDir;

        } catch (Exception e) {
            log.error("File system backup failed", e);
            return null;
        }
    }

    /**
     * Get file inclusion settings from backup settings
     */
    private Map<String, Boolean> getFileInclusionSettings() {
        Map<String, Boolean> inclusions = new LinkedHashMap<>();
        inclusions.put("email-signatures", backupSettings.includeEmailSignatures());
        inclusions.put("email-templates", backupSettings.includeEmailTemplates());
        inclusions.put("pdf-templates", backupSettings.includePdfTemplates());
        inclusions.put("accommodation-images", backupSettings.includeAccommodationImages());
        inclusions.put("accommodation-documents", backupSettings.includeAccommodationDocuments());
        inclusions.put("park-images", backupSettings.includeParkImages());
        inclusions.put("park-documents", backupSettings.includeParkDocuments());
        inclusions.put("activity-images", backupSettings.includeActivityImages());
        inclusions.put("activity-documents", backupSettings.includeActivityDocuments());
        inclusions.put("itinerary-documents", backupSettings.includeItineraryDocuments());
        inclusions.put("quote-documents", backupSettings.includeQuoteDocuments());
        inclusions.put("safari-documents", backupSettings.includeSafariDocuments());
        return inclusions;
    }

    /**
     * Copy directory recursively
     *
     * @param source source directory path
     * @param target target directory path
     * @return true if successful, false otherwise
     */
    private boolean copyDirectory(String source, String target) {
        try {
            Path sourcePath = Paths.get(source);
            Path targetPath = Paths.get(target);

            if (!Files.exists(sourcePath)) {
                log.debug("Source directory does not exist: {}", source);
                return false;
            }

            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                    Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.copy(file, targetPath.resolve(sourcePath.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });

            return true;

        } catch (Exception e) {
            log.error("Failed to copy directory: {} -> {}", source, target, e);
            return false;
        }
    }

    /**
     * Compress backup directory to ZIP file
     *
     * @param sourceDir source directory to compress
     * @param targetDir target directory for ZIP file
     * @param backupName backup name (without extension)
     * @return path to compressed file
     */
    private String compressBackup(String sourceDir, String targetDir, String backupName)
            throws IOException {
        String zipFilePath = targetDir + backupName + ".zip";
        Path sourcePath = Paths.get(sourceDir);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Path relativePath = sourcePath.relativize(file);
                    zipOut.putNextEntry(new ZipEntry(relativePath.toString()));
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if (!dir.equals(sourcePath)) {
                        Path relativePath = sourcePath.relativize(dir);
                        zipOut.putNextEntry(new ZipEntry(relativePath.toString() + "/"));
                        zipOut.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // Delete temporary directory after compression
        deleteDirectory(new File(sourceDir));

        return zipFilePath;
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

    /**
     * Clean up old backups based on retention policy
     */
    public void cleanupOldBackups() {
        try {
            int retentionDays = backupSettings.getRetentionDays();
            int maxCount = backupSettings.getRetentionMaxCount();

            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                return;
            }

            // Get all backup files/directories
            File[] backups = storageDir.listFiles((dir, name) ->
                    name.startsWith(backupSettings.getFilenamePrefix()));

            if (backups == null || backups.length == 0) {
                return;
            }

            // Sort by last modified date (oldest first)
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            int deletedCount = 0;

            // Delete backups older than retention days or exceeding max count
            for (int i = 0; i < backups.length; i++) {
                File backup = backups[i];
                LocalDateTime backupDate = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(backup.lastModified()),
                        java.time.ZoneId.systemDefault()
                );

                boolean shouldDelete = backupDate.isBefore(cutoffDate) ||
                        (backups.length - i > maxCount);

                if (shouldDelete) {
                    if (deleteBackup(backup)) {
                        deletedCount++;
                        log.info("Deleted old backup: {}", backup.getName());
                    }
                }
            }

            if (deletedCount > 0) {
                log.info("Cleanup completed: {} old backups deleted", deletedCount);
            } else {
                log.info("No old backups to clean up");
            }

        } catch (Exception e) {
            log.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Delete a backup (file or directory)
     *
     * @param backup backup file or directory
     * @return true if successful
     */
    private boolean deleteBackup(File backup) {
        if (backup.isDirectory()) {
            return deleteDirectory(backup);
        } else {
            return backup.delete();
        }
    }

    /**
     * Backup result DTO
     */
    public static class BackupResult {
        private boolean success;
        private String message;
        private String error;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String backupPath;
        private String databaseBackupPath;
        private String filesBackupPath;
        private boolean databaseBackupSuccess;
        private boolean filesBackupSuccess;
        private boolean compressed;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

        public String getDatabaseBackupPath() { return databaseBackupPath; }
        public void setDatabaseBackupPath(String databaseBackupPath) {
            this.databaseBackupPath = databaseBackupPath;
        }

        public String getFilesBackupPath() { return filesBackupPath; }
        public void setFilesBackupPath(String filesBackupPath) {
            this.filesBackupPath = filesBackupPath;
        }

        public boolean isDatabaseBackupSuccess() { return databaseBackupSuccess; }
        public void setDatabaseBackupSuccess(boolean databaseBackupSuccess) {
            this.databaseBackupSuccess = databaseBackupSuccess;
        }

        public boolean isFilesBackupSuccess() { return filesBackupSuccess; }
        public void setFilesBackupSuccess(boolean filesBackupSuccess) {
            this.filesBackupSuccess = filesBackupSuccess;
        }

        public boolean isCompressed() { return compressed; }
        public void setCompressed(boolean compressed) { this.compressed = compressed; }
    }
}
