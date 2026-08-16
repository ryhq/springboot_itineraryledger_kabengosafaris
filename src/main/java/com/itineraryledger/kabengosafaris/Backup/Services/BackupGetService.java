package com.itineraryledger.kabengosafaris.Backup.Services;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.Backup.DTOs.BackupDTO;
import com.itineraryledger.kabengosafaris.Backup.DTOs.BackupListItemDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * BackupGetService - Service for retrieving backup information with pagination
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackupGetService {

    private final BackupSettingsGetterServices backupSettings;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "size", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Value("${backup.storage.path:/opt/lampp/htdocs/kabengosafaris/backups/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Get a single backup by filename
     *
     * @param filename The backup filename
     * @return ResponseEntity with BackupDTO
     */
    public ResponseEntity<ApiResponse<?>> getBackupByFilename(String filename) {
        return getBackupByFilename(filename, null, null, null, null, null, null, null, null);
    }

    /** One backup, plus where it sits in the set the caller was looking at. */
    public ResponseEntity<ApiResponse<?>> getBackupByFilename(
        String filename,
        String navFilename,
        LocalDateTime navStartDate,
        LocalDateTime navEndDate,
        Long navMinSize,
        Long navMaxSize,
        Boolean navCompressed,
        String navSortBy,
        String navSortDirection
    ) {
        log.info("Fetching backup: {}", filename);

        try {
            // Validate filename
            if (filename == null || filename.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Filename is required", "FILENAME_REQUIRED")
                );
            }

            // Security: Prevent directory traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid filename", "INVALID_FILENAME")
                );
            }

            // Check if file exists
            File backupFile = new File(storagePath + filename);
            if (!backupFile.exists()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Backup not found", "BACKUP_NOT_FOUND")
                );
            }

            // Check if it's within backup directory
            String backupDir = new File(storagePath).getCanonicalPath();
            String requestedFile = backupFile.getCanonicalPath();
            if (!requestedFile.startsWith(backupDir)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid backup path", "INVALID_BACKUP_PATH")
                );
            }

            BackupDTO backupDTO = convertToDTO(backupFile);

            /*
             * The walk runs over the SAME filtered, sorted set the list used — page into a
             * backup from a search and the arrows stay inside that search — and reports the
             * position, so "3 of 40" replaces two arrows and no idea where you are.
             */
            List<BackupInfo> ordered = filteredBackups(
                navFilename, navStartDate, navEndDate, navMinSize, navMaxSize, navCompressed,
                navSortBy, navSortDirection);
            String nextFilename = null;
            String previousFilename = null;
            Integer position = null;
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).getName().equals(filename)) {
                    position = i + 1;
                    nextFilename = ordered.get((i + 1) % ordered.size()).getName();
                    previousFilename = ordered.get((i - 1 + ordered.size()) % ordered.size()).getName();
                    break;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("backup", backupDTO);
            response.put("nextId", nextFilename);
            response.put("previousId", previousFilename);
            response.put("position", position);
            response.put("total", ordered.size());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Backup retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching backup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch backup", "BACKUP_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all backups with pagination, sorting, and filtering
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
     * @return ResponseEntity with paginated backup list
     */
    /** Kept so any caller still passing the original parameters keeps working. */
    public ResponseEntity<ApiResponse<?>> getAllBackups(
        String filename,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long minSize,
        Long maxSize,
        Boolean isCompressed,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        return getAllBackups(filename, null, startDate, endDate, minSize, maxSize, isCompressed,
            null, page, size, sortBy, sortDirection);
    }

    public ResponseEntity<ApiResponse<?>> getAllBackups(
        String filename,
        /* the house name for the one search box; a backup's only searchable text is its name */
        String keyword,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long minSize,
        Long maxSize,
        Boolean isCompressed,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        if (keyword != null && !keyword.isBlank()) filename = keyword;
        try {
            List<BackupInfo> filteredBackups =
                filteredBackups(filename, startDate, endDate, minSize, maxSize, isCompressed,
                    sortBy, sortDirection);
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: "
                        + VALID_SORT_FIELDS, "INVALID_SORT_FIELD"));
            }
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            int start = pageNumber * pageSize;
            int end = Math.min(start + pageSize, filteredBackups.size());

            List<BackupInfo> pageContent = start < filteredBackups.size()
                ? filteredBackups.subList(start, end)
                : Collections.emptyList();

            // Convert to DTOs
            List<BackupListItemDTO> backupDTOs = pageContent.stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            // Create pageable for response
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, validatedSortBy));
            Page<BackupListItemDTO> backupPage = new PageImpl<>(backupDTOs, pageable, filteredBackups.size());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("backups", backupDTOs);
            response.put("currentPage", backupPage.getNumber());
            response.put("totalItems", backupPage.getTotalElements());
            response.put("totalPages", backupPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("pageSize", backupPage.getSize());
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            /*
             * Counters for the WHOLE filtered set, from the same list the rows came from, so
             * a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(filteredBackups));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Backups retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching backups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch backups", "BACKUPS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get backup statistics
     *
     * @return ResponseEntity with backup statistics
     */
    public ResponseEntity<ApiResponse<?>> getBackupStatistics() {
        log.info("Fetching backup statistics");

        try {
            List<BackupInfo> allBackups = listAllBackups();

            long totalSize = allBackups.stream()
                .mapToLong(BackupInfo::getSize)
                .sum();

            long totalBackups = allBackups.size();

            long compressedBackups = allBackups.stream()
                .filter(backup -> backup.getName().endsWith(".zip") ||
                                 backup.getName().endsWith(".tar.gz") ||
                                 backup.getName().endsWith(".gz"))
                .count();

            LocalDateTime oldestBackup = allBackups.stream()
                .map(BackupInfo::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

            LocalDateTime newestBackup = allBackups.stream()
                .map(BackupInfo::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBackups", totalBackups);
            stats.put("totalSize", totalSize);
            stats.put("totalSizeFormatted", formatFileSize(totalSize));
            stats.put("compressedBackups", compressedBackups);
            stats.put("uncompressedBackups", totalBackups - compressedBackups);
            stats.put("oldestBackup", oldestBackup);
            stats.put("newestBackup", newestBackup);
            stats.put("averageBackupSize", totalBackups > 0 ? totalSize / totalBackups : 0);
            stats.put("averageBackupSizeFormatted", totalBackups > 0 ? formatFileSize(totalSize / totalBackups) : "0 B");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Backup statistics retrieved successfully", stats)
            );

        } catch (Exception e) {
            log.error("Error fetching backup statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch backup statistics", "BACKUP_STATS_FAILED")
            );
        }
    }

    /**
     * Get the most recent successful backup
     *
     * @return BackupInfo of the last successful backup, or null if none found
     */
    public BackupInfo getLastSuccessfulBackup() {
        List<BackupInfo> backups = listAllBackups();
        return backups.isEmpty() ? null : backups.get(0); // Already sorted by newest first
    }

    /**
     * List all available backups (internal method)
     *
     * @return List of BackupInfo sorted by creation date (newest first)
     */
    private List<BackupInfo> listAllBackups() {
        List<BackupInfo> backupList = new ArrayList<>();

        try {
            File storageDir = new File(storagePath);

            if (!storageDir.exists()) {
                return backupList;
            }

            File[] backups = storageDir.listFiles((dir, name) ->
                    name.startsWith(backupSettings.getFilenamePrefix()));

            if (backups != null) {
                for (File backup : backups) {
                    BackupInfo info = new BackupInfo();
                    info.setName(backup.getName());
                    info.setPath(backup.getAbsolutePath());
                    info.setSize(getDirectorySize(backup));
                    info.setCreatedAt(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(backup.lastModified()),
                            java.time.ZoneId.systemDefault()
                    ));
                    info.setDirectory(backup.isDirectory());
                    backupList.add(info);
                }

                // Sort by creation date (newest first)
                backupList.sort(Comparator.comparing(BackupInfo::getCreatedAt).reversed());
            }

        } catch (Exception e) {
            log.error("Failed to list backups", e);
        }

        return backupList;
    }

    /**
     * Convert BackupInfo to BackupDTO (full details)
     */
    private BackupDTO convertToDTO(File backupFile) {
        String filename = backupFile.getName();
        long size = getDirectorySize(backupFile);

        return BackupDTO.builder()
            .name(filename)
            .path(backupFile.getAbsolutePath())
            .size(size)
            .sizeFormatted(formatFileSize(size))
            .createdAt(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(backupFile.lastModified()),
                java.time.ZoneId.systemDefault()
            ))
            .isDirectory(backupFile.isDirectory())
            .isCompressed(filename.endsWith(".zip") || filename.endsWith(".tar.gz") || filename.endsWith(".gz"))
            .compressionFormat(getCompressionFormat(filename))
            .containsDatabase(true) // Assume all backups contain database
            .containsFiles(true) // Assume all backups contain files
            .fileExtension(getFileExtension(filename))
            .downloadUrl(baseUrl + "/api/backups/download/" + filename)
            .build();
    }

    /**
     * Convert BackupInfo to BackupListItemDTO (lightweight)
     */
    private BackupListItemDTO convertToListItemDTO(BackupInfo backupInfo) {
        return BackupListItemDTO.builder()
            .name(backupInfo.getName())
            .size(backupInfo.getSize())
            .sizeFormatted(formatFileSize(backupInfo.getSize()))
            .createdAt(backupInfo.getCreatedAt())
            .isDirectory(backupInfo.isDirectory())
            .fileExtension(getFileExtension(backupInfo.getName()))
            .downloadUrl(baseUrl + "/api/backups/download/" + backupInfo.getName())
            .build();
    }

    /**
     * Get total size of directory or file
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.isFile()) {
            return directory.length();
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getDirectorySize(file);
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
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename.endsWith(".tar.gz")) {
            return "tar.gz";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Get compression format from filename
     */
    private String getCompressionFormat(String filename) {
        if (filename.endsWith(".zip")) {
            return "ZIP";
        } else if (filename.endsWith(".tar.gz")) {
            return "TAR.GZ";
        } else if (filename.endsWith(".gz")) {
            return "GZIP";
        }
        return "NONE";
    }

    /**
     * The filtered, sorted set — shared by the rows, the counters and the record walk.
     *
     * Backups are files rather than rows, so there is no Specification to hand around; this
     * is the same idea done in memory. Sharing it is what stops a card counting one set
     * while the arrows walk another.
     */
    private List<BackupInfo> filteredBackups(
        String filename, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
        Long minSize, Long maxSize, Boolean isCompressed, String sortBy, String sortDirection
    ) {
        List<BackupInfo> filtered = listAllBackups().stream()
            .filter(b -> filename == null || filename.isEmpty()
                || b.getName().toLowerCase().contains(filename.toLowerCase()))
            .filter(b -> startDate == null || b.getCreatedAt().isAfter(startDate))
            .filter(b -> endDate == null || b.getCreatedAt().isBefore(endDate))
            .filter(b -> minSize == null || b.getSize() >= minSize)
            .filter(b -> maxSize == null || b.getSize() <= maxSize)
            .filter(b -> isCompressed == null || isCompressed == isCompressedName(b.getName()))
            .collect(Collectors.toList());

        String sortField = validateSortField(sortBy) != null ? validateSortField(sortBy) : "createdAt";
        boolean ascending = "asc".equalsIgnoreCase(sortDirection);
        Comparator<BackupInfo> comparator = switch (sortField) {
            case "name" -> Comparator.comparing(BackupInfo::getName);
            case "size" -> Comparator.comparingLong(BackupInfo::getSize);
            default -> Comparator.comparing(BackupInfo::getCreatedAt);
        };
        filtered.sort(ascending ? comparator : comparator.reversed());
        return filtered;
    }

    /** A compressed archive by its extension, which is all a file can tell us. */
    private boolean isCompressedName(String name) {
        return name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".gz");
    }

    /**
     * The cards over the backups.
     *
     * Every figure is about the same question: is there a recent, complete backup to restore
     * from. "None in the last 24 hours" is the one worth acting on — a schedule that has
     * quietly stopped looks exactly like a system nobody has changed.
     */
    private Map<String, Object> buildStats(List<BackupInfo> filtered) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long compressed = filtered.stream().filter(b -> isCompressedName(b.getName())).count();
        long totalSize = filtered.stream().mapToLong(BackupInfo::getSize).sum();
        long last24 = filtered.stream().filter(b -> b.getCreatedAt().isAfter(now.minusDays(1))).count();
        long last7 = filtered.stream().filter(b -> b.getCreatedAt().isAfter(now.minusDays(7))).count();
        long large = filtered.stream().filter(b -> b.getSize() >= 100L * 1024 * 1024).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", filtered.size());
        stats.put("compressed", compressed);
        stats.put("uncompressed", filtered.size() - compressed);
        stats.put("newLast24Hours", last24);
        stats.put("newLast7Days", last7);
        stats.put("largeBackups", large);
        stats.put("totalSize", totalSize);
        stats.put("totalSizeFormatted", formatFileSize(totalSize));
        return stats;
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Internal DTO for backup information
     */
    @lombok.Data
    public static class BackupInfo {
        private String name;
        private String path;
        private long size;
        private LocalDateTime createdAt;
        private boolean isDirectory;
    }
}
