package com.itineraryledger.kabengosafaris.AuditLog;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings.AuditLogSettingGetterServices;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogSettingGetterServices auditLogSettingGetterServices;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    /**
     * What may be sorted on.
     *
     * `createdAt` is the only order an audit log is ever read in by default — it is a
     * chronology — but who and what are worth sorting by when hunting.
     */
    private static final java.util.List<String> VALID_SORT_FIELDS = java.util.Arrays.asList(
        "createdAt", "username", "action", "entityType", "status"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Log an action asynchronously to avoid blocking the main request
     * Respects audit logging policies
     */
    @Async
    public void logAction(AuditLog auditLog) {
        try {
            // Check if audit logging is enabled globally
            if (!auditLogSettingGetterServices.isAuditLogEnabled()) {
                log.debug("Audit logging is disabled, skipping: {} - {}", auditLog.getAction(), auditLog.getEntityType());
                return;
            }

            // Apply audit logging policies
            applyAuditPolicies(auditLog);

            // Set temporary name before first save (will be replaced with proper name)
            auditLog.setName("TEMP_" + System.currentTimeMillis() + "_" + System.nanoTime());

            // Save audit log to get the generated ID
            AuditLog savedLog = auditLogRepository.save(auditLog);

            // Generate and set the audit log name with retry logic for concurrency
            String logName = generateAuditLogNameWithRetry(savedLog.getId());
            savedLog.setName(logName);
            auditLogRepository.save(savedLog);

            log.debug("Audit log saved: {} - {} - {} - Name: {}",
                savedLog.getUsername(), savedLog.getAction(), savedLog.getEntityType(), logName);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Synchronously log an action (blocks until saved)
     * Respects audit logging policies
     */
    public void logActionSync(AuditLog auditLog) {
        try {
            // Check if audit logging is enabled globally
            if (!auditLogSettingGetterServices.isAuditLogEnabled()) {
                log.debug("Audit logging is disabled, skipping: {} - {}", auditLog.getAction(), auditLog.getEntityType());
                return;
            }

            // Apply audit logging policies
            applyAuditPolicies(auditLog);

            // Set temporary name before first save (will be replaced with proper name)
            auditLog.setName("TEMP_" + System.currentTimeMillis() + "_" + System.nanoTime());

            // Save audit log to get the generated ID
            AuditLog savedLog = auditLogRepository.save(auditLog);

            // Generate and set the audit log name with retry logic for concurrency
            String logName = generateAuditLogNameWithRetry(savedLog.getId());
            savedLog.setName(logName);
            auditLogRepository.save(savedLog);

            log.debug("Audit log saved synchronously: {} - {} - {} - Name: {}",
                savedLog.getUsername(), savedLog.getAction(), savedLog.getEntityType(), logName);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Apply audit logging policies to the audit log
     * Enforces capture policies, field exclusions, and value length limits
     *
     * @param auditLog the audit log to apply policies to
     */
    private void applyAuditPolicies(AuditLog auditLog) {
        // Apply IP address capture policy
        if (!auditLogSettingGetterServices.shouldCaptureIpAddress()) {
            auditLog.setIpAddress(null);
        }

        // Apply user agent capture policy
        if (!auditLogSettingGetterServices.shouldCaptureUserAgent()) {
            auditLog.setUserAgent(null);
        }

        // Apply old values capture policy
        if (!auditLogSettingGetterServices.shouldCaptureOldValues()) {
            auditLog.setOldValues(null);
        }

        // Apply new values capture policy
        if (!auditLogSettingGetterServices.shouldCaptureNewValues()) {
            auditLog.setNewValues(null);
        }

        // Apply excluded fields policy
        Set<String> excludedFields = parseExcludedFields();
        auditLog.setOldValues(filterExcludedFields(auditLog.getOldValues(), excludedFields));
        auditLog.setNewValues(filterExcludedFields(auditLog.getNewValues(), excludedFields));

        // Apply max value length policy
        Integer maxValueLength = auditLogSettingGetterServices.getMaxValueLength();
        if (maxValueLength != null && maxValueLength > 0) {
            if (auditLog.getOldValues() != null && auditLog.getOldValues().length() > maxValueLength) {
                auditLog.setOldValues(auditLog.getOldValues().substring(0, maxValueLength) + "... [TRUNCATED]");
            }
            if (auditLog.getNewValues() != null && auditLog.getNewValues().length() > maxValueLength) {
                auditLog.setNewValues(auditLog.getNewValues().substring(0, maxValueLength) + "... [TRUNCATED]");
            }
        }
    }

    /**
     * Parse excluded fields from the configuration
     * @return set of lowercase field names to exclude
     */
    private Set<String> parseExcludedFields() {
        String excludedFieldsStr = auditLogSettingGetterServices.getExcludedFields();
        Set<String> excludedFields = new HashSet<>();
        if (excludedFieldsStr != null && !excludedFieldsStr.isEmpty()) {
            Arrays.stream(excludedFieldsStr.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .forEach(excludedFields::add);
        }
        return excludedFields;
    }

    /**
     * Filter out excluded fields from JSON values
     * Attempts to remove excluded fields from JSON strings
     *
     * @param jsonValue the JSON string to filter
     * @param excludedFields set of field names to exclude (lowercase)
     * @return filtered JSON string or original if filtering fails
     */
    private String filterExcludedFields(String jsonValue, Set<String> excludedFields) {
        if (jsonValue == null || jsonValue.isEmpty() || excludedFields.isEmpty()) {
            return jsonValue;
        }

        try {
            // Simple regex-based field removal from JSON
            String filtered = jsonValue;
            for (String field : excludedFields) {
                // Match field name (case-insensitive) and remove the key-value pair
                // Pattern: "fieldName":"value" or "fieldName":value (for various value types)
                filtered = filtered.replaceAll("(?i)\"" + Pattern.quote(field) + "\"\\s*:\\s*[^,}]*", "");
                // Clean up any leftover commas
                filtered = filtered.replaceAll(",\\s*,", ",");
                filtered = filtered.replaceAll(",\\s*}", "}");
                filtered = filtered.replaceAll("\\{,", "{");
            }
            return filtered;
        } catch (Exception e) {
            log.debug("Failed to filter excluded fields from audit log values", e);
            return jsonValue;
        }
    }

    /**
     * Delete old audit logs (retention policy)
     * Keeps logs for specified number of days based on policy
     */
    @Transactional
    public long deleteOldAuditLogs() {
        Integer retentionDays = auditLogSettingGetterServices.getAuditLogRetentionDays();
        if (retentionDays == null || retentionDays <= 0) {
            log.warn("Invalid retention days configured: {}", retentionDays);
            return 0;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldLogs = auditLogRepository.findByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                cutoffDate,
                PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        long deletedCount = oldLogs.size();
        if (deletedCount > 0) {
            auditLogRepository.deleteAll(oldLogs);
            log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        }
        return deletedCount;
    }

    /**
     * Delete old audit logs with specified retention days
     * Overloaded method for backward compatibility
     *
     * @param retentionDays number of days to retain logs
     * @return number of deleted logs
     */
    @Transactional
    public long deleteOldAuditLogs(int retentionDays) {
        if (retentionDays <= 0) {
            log.warn("Invalid retention days specified: {}", retentionDays);
            return 0;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldLogs = auditLogRepository.findByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                cutoffDate,
                PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        long deletedCount = oldLogs.size();
        if (deletedCount > 0) {
            auditLogRepository.deleteAll(oldLogs);
            log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        }
        return deletedCount;
    }

    /**
     * Get all audit logs with optional filtering, pagination, and sorting
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param name Filter by audit log name (partial match)
     * @param userIdObfuscated Filter by user ID (obfuscated)
     * @param username Filter by username (partial match)
     * @param action Filter by action (partial match)
     * @param entityType Filter by entity type (partial match)
     * @param entityIdObfuscated Filter by entity ID (obfuscated)
     * @param description Filter by description (partial match)
     * @param ipAddress Filter by IP address (exact match)
     * @param userAgent Filter by user agent (partial match)
     * @param status Filter by status (partial match)
     * @param errorMessage Filter by error message (partial match)
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    /** Kept so any caller still passing loose parameters keeps working. */
    public ResponseEntity<?> getAllAuditLogs(
        int page,
        int size,
        String name,
        String userIdObfuscated,
        String username,
        String action,
        String entityType,
        String entityIdObfuscated,
        String description,
        String ipAddress,
        String userAgent,
        String status,
        String errorMessage,
        String sortDirection
    ) {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setName(name);
        filter.setUserId(userIdObfuscated);
        filter.setUsername(username);
        filter.setAction(action);
        filter.setEntityType(entityType);
        filter.setEntityId(entityIdObfuscated);
        filter.setDescription(description);
        filter.setIpAddress(ipAddress);
        filter.setUserAgent(userAgent);
        filter.setStatus(status);
        filter.setErrorMessage(errorMessage);
        return getAllAuditLogs(filter, null, page, size, null, sortDirection);
    }

    /**
     * The log, filtered, counted and paged.
     *
     * The rows, the cards and prev/next all come off ONE specification, so a card cannot
     * report a figure the table would contradict.
     */
    public ResponseEntity<?> getAllAuditLogs(
        AuditLogFilter filter,
        Boolean includeStats,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        AuditLogFilter active = filter != null ? filter : new AuditLogFilter();
        if (page < 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE"));
        }
        // clamp: an unbounded size over a table this long is a way to hang the server
        int pageSize = size <= 0 ? 10 : Math.min(size, 100);

        String resolvedSort = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
            ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable paging = PageRequest.of(page, pageSize, Sort.by(direction, resolvedSort));

        Specification<AuditLog> specification;
        try {
            specification = buildSpec(active);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, e.getMessage(), "INVALID_FILTER"));
        }

        Page<AuditLog> pagedAuditLogs = auditLogRepository.findAll(specification, paging);
        List<AuditLogDTO> auditLogDTOs = getAuditLogDTOs(pagedAuditLogs.getContent());

        Map<String, Object> response = new HashMap<>();
        response.put("auditLogs", auditLogDTOs);
        response.put("currentPage", pagedAuditLogs.getNumber());
        response.put("totalItems", pagedAuditLogs.getTotalElements());
        response.put("totalPages", pagedAuditLogs.getTotalPages());
        response.put("pageSize", pagedAuditLogs.getSize());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", resolvedSort);
        response.put("currentSortDirection", direction.name().toLowerCase());
        if (!Boolean.FALSE.equals(includeStats)) {
            response.put("stats", buildStats(specification));
        }

        log.info("Successfully fetched {} audit logs on page {}", auditLogDTOs.size(), page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved audit logs.",
                response
            )
        );
    }

    /**
     * Get a single audit log by obfuscated ID
     *
     * @param idObfuscated The obfuscated audit log ID
     * @return ResponseEntity with ApiResponse containing audit log or error
     */
    public ResponseEntity<ApiResponse<?>> getAuditLog(String idObfuscated) {
        return getAuditLog(idObfuscated, null, null, null);
    }

    /** One entry, plus where it sits in the set the caller was looking at. */
    public ResponseEntity<ApiResponse<?>> getAuditLog(
        String idObfuscated,
        AuditLogFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            // Find audit log
            AuditLog auditLog = auditLogRepository.findById(id).orElse(null);

            if (auditLog == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Audit log not found", "RESOURCE_NOT_FOUND")
                );
            }

            /*
             * The walk runs over the SAME specification the list used, in the same order —
             * so stepping through failures stays among failures. It used to page over the
             * whole table by id, which on a log this long is a walk to nowhere.
             */
            Specification<AuditLog> navSpec = buildSpec(filter != null ? filter : new AuditLogFilter());
            String navSortBy = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                ? sortBy : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                AuditLog.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("auditLog", convertToDTO(auditLog));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            log.info("Successfully retrieved audit log {}", id);
            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved audit log.",
                    response
                )
            );
        } catch (Exception e) {
            log.error("Error getting audit log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get audit log", "GET_AUDIT_LOG_FAILED")
            );
        }
    }

    /**
     * Convert list of AuditLog entities to DTOs
     */
    private List<AuditLogDTO> getAuditLogDTOs(List<AuditLog> auditLogs) {
        return auditLogs.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Convert AuditLog entity to DTO with obfuscated IDs
     */
    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(idObfuscator.encodeId(auditLog.getId()))
                .name(auditLog.getName())
                .userId(auditLog.getUserId() != null ? idObfuscator.encodeId(auditLog.getUserId()) : null)
                .username(auditLog.getUsername())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId() != null ? idObfuscator.encodeId(auditLog.getEntityId()) : null)
                .description(auditLog.getDescription())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .status(auditLog.getStatus())
                .errorMessage(auditLog.getErrorMessage())
                .build();
    }

    /**
     * Generate audit log name with retry logic to handle concurrent inserts.
     * Uses the saved audit log ID as a fallback suffix to ensure uniqueness.
     *
     * Format: AUD_LOG_{####}{MM}{YY} or AUD_LOG_{####}{MM}{YY}_{ID} on collision
     *
     * @param auditLogId The saved audit log ID (used as fallback suffix)
     * @return Unique formatted audit log name
     */
    public String generateAuditLogNameWithRetry(Long auditLogId) {
        LocalDateTime now = LocalDateTime.now();
        String month = String.format("%02d", now.getMonthValue());
        String year = String.format("%02d", now.getYear() % 100);

        // Try to generate unique name with up to 3 retries
        for (int attempt = 0; attempt < 3; attempt++) {
            long monthlyCount = auditLogRepository.countByYearAndMonth(now.getYear(), now.getMonthValue()) + 1 + attempt;
            String countFormatted = String.format("%04d", monthlyCount);
            String candidateName = String.format("AUD_LOG_%s%s%s", countFormatted, month, year);

            // Check if name already exists
            if (!auditLogRepository.existsByName(candidateName)) {
                return candidateName;
            }
        }

        // Fallback: use the audit log's own ID to guarantee uniqueness
        long monthlyCount = auditLogRepository.countByYearAndMonth(now.getYear(), now.getMonthValue()) + 1;
        String countFormatted = String.format("%04d", monthlyCount);
        return String.format("AUD_LOG_%s%s%s_%d", countFormatted, month, year, auditLogId);
    }

    /**
     * Generate audit log name in format: AUD_LOG_{####}{MM}{YY}
     *
     * Format breakdown:
     * - AUD_LOG_ : Fixed prefix
     * - ####     : Sequential count for the given month/year (4 digits, zero-padded, resets each month)
     * - MM       : Month (2 digits, zero-padded)
     * - YY       : Last 2 digits of year
     *
     * Examples:
     * - 1st log in Dec 2024  → AUD_LOG_00011224
     * - 2nd log in Dec 2024  → AUD_LOG_00021224
     * - 1st log in Jan 2025  → AUD_LOG_00010125 (count resets)
     *
     * @return Formatted audit log name
     * @deprecated Use {@link #generateAuditLogNameWithRetry(Long)} instead for concurrency safety
     */
    @Deprecated
    public String generateAuditLogName() {
        // Get current date
        LocalDateTime now = LocalDateTime.now();

        // Format month (MM) and year (YY)
        String month = String.format("%02d", now.getMonthValue());
        String year = String.format("%02d", now.getYear() % 100);

        // Get count of logs for current month/year and increment by 1
        long monthlyCount = auditLogRepository.countByYearAndMonth(now.getYear(), now.getMonthValue()) + 1;
        String countFormatted = String.format("%04d", monthlyCount);

        // Construct final name: AUD_LOG_{####}{MM}{YY}
        return String.format("AUD_LOG_%s%s%s", countFormatted, month, year);
    }

    /**
     * One specification, used by the rows, the cards and the record walk.
     *
     * OR inside a dimension, AND across dimensions — so "deletes or updates, by Ricksy, on
     * Customers, since Monday" reads the way somebody means it.
     */
    private Specification<AuditLog> buildSpec(AuditLogFilter filter) {
        Specification<AuditLog> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(AuditLogSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and(AuditLogSpecification.nameLike(filter.getName()));
        }
        if (filter.getUsername() != null && !filter.getUsername().isBlank()) {
            spec = spec.and(AuditLogSpecification.usernameLike(filter.getUsername()));
        }
        if (!filter.allActions().isEmpty()) {
            spec = spec.and(AuditLogSpecification.actionIn(filter.allActions()));
        }
        if (!filter.allEntityTypes().isEmpty()) {
            spec = spec.and(AuditLogSpecification.entityTypeIn(filter.allEntityTypes()));
        }
        if (!filter.allStatuses().isEmpty()) {
            spec = spec.and(AuditLogSpecification.statusIn(filter.allStatuses()));
        }
        if (filter.getDescription() != null && !filter.getDescription().isBlank()) {
            spec = spec.and(AuditLogSpecification.descriptionLike(filter.getDescription()));
        }
        if (filter.getIpAddress() != null && !filter.getIpAddress().isBlank()) {
            spec = spec.and(AuditLogSpecification.hasIpAddress(filter.getIpAddress()));
        }
        if (filter.getUserAgent() != null && !filter.getUserAgent().isBlank()) {
            spec = spec.and(AuditLogSpecification.userAgentLike(filter.getUserAgent()));
        }
        if (filter.getErrorMessage() != null && !filter.getErrorMessage().isBlank()) {
            spec = spec.and(AuditLogSpecification.errorMessageLike(filter.getErrorMessage()));
        }
        if (filter.wants("failed")) spec = spec.and(AuditLogSpecification.isFailure());
        if (filter.wants("deletions")) spec = spec.and(AuditLogSpecification.isDeletion());
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(AuditLogSpecification.createdAfter(filter.getCreatedAfter()));
        }
        if (filter.getCreatedBefore() != null) {
            spec = spec.and(AuditLogSpecification.createdBefore(filter.getCreatedBefore()));
        }

        /*
         * An id that will not decode is a 400, not a silently ignored filter: "everything
         * this user did" answered with everything everybody did would be a dangerous lie on
         * this particular list.
         */
        if (filter.getUserId() != null && !filter.getUserId().isBlank()) {
            spec = spec.and(AuditLogSpecification.hasUserId(decodeOrFail(filter.getUserId(), "user")));
        }
        if (filter.getEntityId() != null && !filter.getEntityId().isBlank()) {
            spec = spec.and(AuditLogSpecification.hasEntityId(decodeOrFail(filter.getEntityId(), "record")));
        }
        return spec;
    }

    private Long decodeOrFail(String obfuscated, String what) {
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + what + " id: " + obfuscated);
        }
    }

    /**
     * The cards over the log.
     *
     * Two of these are the reason anybody opens it: something failed, or something was
     * deleted. Both are invisible among thousands of ordinary rows, and both are reachable
     * as filters.
     */
    private Map<String, Object> buildStats(Specification<AuditLog> spec) {
        return listStats.of(AuditLog.class, spec)
            .total()
            .count("failed", AuditLogSpecification.isFailure())
            .complement("succeeded", "failed")
            .count("deletions", AuditLogSpecification.isDeletion())
            .window("last24Hours", 1, AuditLogSpecification::createdAfter)
            .recency(AuditLogSpecification::createdAfter)
            .build();
    }

    /**
     * The values this log actually holds, for the filter dropdowns.
     *
     * There are 330 action names and 87 entity types in the code, and a hard-coded list of
     * them would drift the moment somebody adds a module. DISTINCT over the table can only
     * ever offer a value that matches something — which is the whole rule about filters
     * here — and it also says what this installation has really done.
     */
    public ResponseEntity<ApiResponse<?>> getFacetValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("actions", auditLogRepository.distinctActions());
        data.put("entityTypes", auditLogRepository.distinctEntityTypes());
        data.put("statuses", auditLogRepository.distinctStatuses());
        data.put("usernames", auditLogRepository.distinctUsernames());
        return ResponseEntity.ok(ApiResponse.success(200, "Audit log filter values", data));
    }
}
