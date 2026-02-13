package com.itineraryledger.kabengosafaris.Log.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.RetentionStatsDTO;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Service for managing log retention, archival, and cleanup
 *
 * This service handles:
 * - Archiving old log files to compressed .gz format
 * - Deleting very old archived logs to free disk space
 * - Providing statistics on log retention
 * - Scheduled automated cleanup tasks
 *
 * Log retention follows the same policy as audit logs for consistency.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogRetentionService {

    private final AccessLogSettingGetterServices settings;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_RETENTION_DAYS = 90;
    private static final int DEFAULT_ARCHIVE_RETENTION_DAYS = 365;

    /**
     * Archive logs older than retention period
     *
     * Finds log files older than the configured retention period,
     * compresses them to .gz format, and moves them to the archive directory.
     *
     * @return count of archived files
     */
    public int archiveOldLogs() {
        if (!settings.isArchiveEnabled()) {
            log.info("Log archiving is disabled in settings");
            return 0;
        }

        try {
            Integer retentionDays = settings.getLogRetentionDays();
            if (retentionDays == null || retentionDays <= 0) {
                retentionDays = DEFAULT_RETENTION_DAYS;
            }

            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
            log.info("Starting log archival for logs older than {} (retention: {} days)", cutoffDate, retentionDays);

            Path logDirectory = Paths.get(settings.getLogDirectory());
            if (!Files.exists(logDirectory)) {
                log.warn("Log directory does not exist: {}", logDirectory);
                return 0;
            }

            // Ensure archive directory exists
            Path archiveDirectory = Paths.get(settings.getArchivePath());
            if (!Files.exists(archiveDirectory)) {
                Files.createDirectories(archiveDirectory);
                log.info("Created archive directory: {}", archiveDirectory);
            }

            // Find and archive old log files
            List<Path> oldLogs = findOldLogFiles(logDirectory, cutoffDate);
            int archivedCount = 0;

            for (Path logFile : oldLogs) {
                try {
                    Path compressedFile = compressLogFile(logFile);
                    if (compressedFile != null) {
                        // Delete original file after successful compression
                        Files.delete(logFile);
                        archivedCount++;
                        log.debug("Archived and deleted: {}", logFile.getFileName());
                    }
                } catch (IOException e) {
                    log.error("Failed to archive log file: {}", logFile, e);
                }
            }

            if (archivedCount > 0) {
                log.info("Successfully archived {} log files", archivedCount);
            } else {
                log.debug("No logs found for archival");
            }

            return archivedCount;

        } catch (Exception e) {
            log.error("Error during log archival process", e);
            return 0;
        }
    }

    /**
     * Delete very old archived logs
     *
     * Finds archived log files older than the archive retention period
     * and permanently deletes them to free disk space.
     *
     * @return count of deleted files
     */
    public int deleteArchivedLogs() {
        if (!settings.isCleanupEnabled()) {
            log.info("Log cleanup is disabled in settings");
            return 0;
        }

        try {
            LocalDate archiveCutoffDate = LocalDate.now().minusDays(DEFAULT_ARCHIVE_RETENTION_DAYS);
            log.info("Starting archived log deletion for archives older than {} ({} days)",
                archiveCutoffDate, DEFAULT_ARCHIVE_RETENTION_DAYS);

            Path archiveDirectory = Paths.get(settings.getArchivePath());
            if (!Files.exists(archiveDirectory)) {
                log.debug("Archive directory does not exist: {}", archiveDirectory);
                return 0;
            }

            // Find and delete very old archived files
            List<Path> oldArchives = findOldArchivedFiles(archiveDirectory, archiveCutoffDate);
            int deletedCount = 0;

            for (Path archiveFile : oldArchives) {
                try {
                    Files.delete(archiveFile);
                    deletedCount++;
                    log.debug("Deleted old archive: {}", archiveFile.getFileName());
                } catch (IOException e) {
                    log.error("Failed to delete archived log: {}", archiveFile, e);
                }
            }

            if (deletedCount > 0) {
                log.info("Successfully deleted {} old archived log files", deletedCount);
            } else {
                log.debug("No old archives found for deletion");
            }

            return deletedCount;

        } catch (Exception e) {
            log.error("Error during archived log deletion process", e);
            return 0;
        }
    }

    /**
     * Get retention statistics
     *
     * Provides comprehensive statistics about log files including:
     * - Count of active and archived logs
     * - Total disk space used
     * - Oldest log dates
     *
     * @return RetentionStatsDTO with retention statistics
     */
    public RetentionStatsDTO getRetentionStats() {
        try {
            Path logDirectory = Paths.get(settings.getLogDirectory());
            Path archiveDirectory = Paths.get(settings.getArchivePath());

            // Initialize stats
            int activeCount = 0;
            int archivedCount = 0;
            long activeSize = 0L;
            long archivedSize = 0L;
            LocalDate oldestActive = null;
            LocalDate oldestArchived = null;

            // Count and size active logs
            if (Files.exists(logDirectory)) {
                try (Stream<Path> files = Files.list(logDirectory)) {
                    List<Path> activeLogs = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(settings.getLogSuffix()))
                        .toList();

                    activeCount = activeLogs.size();
                    activeSize = activeLogs.stream()
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException e) {
                                return 0L;
                            }
                        })
                        .sum();

                    // Find oldest active log
                    oldestActive = activeLogs.stream()
                        .map(this::extractDateFromFilename)
                        .filter(date -> date != null)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                }
            }

            // Count and size archived logs
            if (Files.exists(archiveDirectory)) {
                try (Stream<Path> files = Files.list(archiveDirectory)) {
                    List<Path> archives = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".gz"))
                        .toList();

                    archivedCount = archives.size();
                    archivedSize = archives.stream()
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException e) {
                                return 0L;
                            }
                        })
                        .sum();

                    // Find oldest archived log
                    oldestArchived = archives.stream()
                        .map(this::extractDateFromFilename)
                        .filter(date -> date != null)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                }
            }

            return RetentionStatsDTO.builder()
                .activeLogCount(activeCount)
                .archivedLogCount(archivedCount)
                .activeSizeBytes(activeSize)
                .archivedSizeBytes(archivedSize)
                .oldestActiveLog(oldestActive)
                .oldestArchivedLog(oldestArchived)
                .build();

        } catch (Exception e) {
            log.error("Error getting retention statistics", e);
            return RetentionStatsDTO.builder()
                .activeLogCount(0)
                .archivedLogCount(0)
                .activeSizeBytes(0L)
                .archivedSizeBytes(0L)
                .build();
        }
    }

    /**
     * Main cleanup method scheduled to run daily at 2 AM
     *
     * Performs both archival and deletion operations, then logs the results.
     * This scheduled task ensures regular maintenance of log files.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        log.info("Starting scheduled log cleanup task");

        try {
            // Archive old logs
            int archivedCount = archiveOldLogs();

            // Delete very old archived logs
            int deletedCount = deleteArchivedLogs();

            // Get final statistics
            RetentionStatsDTO stats = getRetentionStats();

            log.info("Scheduled log cleanup completed - Archived: {}, Deleted: {}, " +
                "Active Logs: {}, Archived Logs: {}, Total Size: {} bytes",
                archivedCount, deletedCount, stats.getActiveLogCount(),
                stats.getArchivedLogCount(),
                (stats.getActiveSizeBytes() + stats.getArchivedSizeBytes()));

        } catch (Exception e) {
            log.error("Error during scheduled log cleanup", e);
        }
    }

    /**
     * Compress a single log file to .gz format
     *
     * Reads the log file, compresses it using GZIP, and saves it to the archive directory.
     * The original file is not deleted by this method.
     *
     * @param logFile the log file to compress
     * @return Path to the compressed file, or null if compression failed
     */
    public Path compressLogFile(Path logFile) {
        if (logFile == null || !Files.exists(logFile)) {
            log.warn("Log file does not exist: {}", logFile);
            return null;
        }

        try {
            Path archiveDirectory = Paths.get(settings.getArchivePath());
            if (!Files.exists(archiveDirectory)) {
                Files.createDirectories(archiveDirectory);
            }

            // Create compressed file in archive directory
            String compressedFilename = logFile.getFileName().toString() + ".gz";
            Path compressedFile = archiveDirectory.resolve(compressedFilename);

            // Compress the file
            try (FileInputStream fis = new FileInputStream(logFile.toFile());
                 FileOutputStream fos = new FileOutputStream(compressedFile.toFile());
                 GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    gzipOS.write(buffer, 0, bytesRead);
                }

                gzipOS.finish();
            }

            log.debug("Compressed {} to {}", logFile.getFileName(), compressedFile.getFileName());
            return compressedFile;

        } catch (IOException e) {
            log.error("Failed to compress log file: {}", logFile, e);
            return null;
        }
    }

    /**
     * Find log files older than the specified cutoff date
     *
     * @param directory the directory to search
     * @param cutoffDate the cutoff date (files older than this will be returned)
     * @return list of old log files
     */
    private List<Path> findOldLogFiles(Path directory, LocalDate cutoffDate) {
        List<Path> oldFiles = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(settings.getLogSuffix()))
                .filter(path -> !path.toString().endsWith(".gz"))
                .forEach(path -> {
                    LocalDate logDate = extractDateFromFilename(path);
                    if (logDate != null && logDate.isBefore(cutoffDate)) {
                        oldFiles.add(path);
                    }
                });
        } catch (IOException e) {
            log.error("Error finding old log files in directory: {}", directory, e);
        }

        return oldFiles;
    }

    /**
     * Find archived files older than the specified cutoff date
     *
     * @param directory the archive directory to search
     * @param cutoffDate the cutoff date (files older than this will be returned)
     * @return list of old archived files
     */
    private List<Path> findOldArchivedFiles(Path directory, LocalDate cutoffDate) {
        List<Path> oldFiles = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".gz"))
                .forEach(path -> {
                    LocalDate logDate = extractDateFromFilename(path);
                    if (logDate != null && logDate.isBefore(cutoffDate)) {
                        oldFiles.add(path);
                    }
                });
        } catch (IOException e) {
            log.error("Error finding old archived files in directory: {}", directory, e);
        }

        return oldFiles;
    }

    /**
     * Extract date from log filename
     *
     * Expected format: {prefix}.{yyyy-MM-dd}{suffix}
     * Example: access_log.2026-02-09.log or access_log.2026-02-09.log.gz
     *
     * @param filePath the file path
     * @return extracted date, or null if parsing fails
     */
    private LocalDate extractDateFromFilename(Path filePath) {
        try {
            String filename = filePath.getFileName().toString();

            // Remove .gz extension if present
            if (filename.endsWith(".gz")) {
                filename = filename.substring(0, filename.length() - 3);
            }

            // Remove prefix and suffix
            String prefix = settings.getLogPrefix();
            String suffix = settings.getLogSuffix();

            if (filename.startsWith(prefix + ".")) {
                filename = filename.substring((prefix + ".").length());
            }

            if (filename.endsWith(suffix)) {
                filename = filename.substring(0, filename.length() - suffix.length());
            }

            // Parse date
            return LocalDate.parse(filename, DATE_FORMATTER);

        } catch (Exception e) {
            log.debug("Could not extract date from filename: {}", filePath.getFileName(), e);
            return null;
        }
    }
}
