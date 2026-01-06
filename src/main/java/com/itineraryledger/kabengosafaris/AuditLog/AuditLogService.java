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
            auditLog.setName("TEMP_" + System.currentTimeMillis());

            // Save audit log to get the generated ID
            AuditLog savedLog = auditLogRepository.save(auditLog);

            // Generate and set the audit log name
            String logName = generateAuditLogName();
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
            auditLog.setName("TEMP_" + System.currentTimeMillis());

            // Save audit log to get the generated ID
            AuditLog savedLog = auditLogRepository.save(auditLog);

            // Generate and set the audit log name
            String logName = generateAuditLogName();
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
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
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
        String sortDir
    ) {
        log.debug("Fetching audit logs with filters - page: {}, size: {}, name: {}, userId: {}, username: {}, " +
                "action: {}, entityType: {}, entityId: {}, description: {}, ipAddress: {}, userAgent: {}, " +
                "status: {}, errorMessage: {}, sortDir: {}",
                page, size, name, userIdObfuscated, username, action, entityType, entityIdObfuscated, description,
                ipAddress, userAgent, status, errorMessage, sortDir);
        
        // Decode obfuscated IDs
        Long userId = null;
        if (userIdObfuscated != null && !userIdObfuscated.isBlank()) {
            try {
                userId = idObfuscator.decodeId(userIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid user ID format: {}", userIdObfuscated);
                return ResponseEntity.badRequest().body("Invalid user ID format");
            }
        }

        Long entityId = null;
        if (entityIdObfuscated != null && !entityIdObfuscated.isBlank()) {
            try {
                entityId = idObfuscator.decodeId(entityIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid entity ID format: {}", entityIdObfuscated);
                return ResponseEntity.badRequest().body("Invalid entity ID format");
            }
        }

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        // Build dynamic specification
        Specification<AuditLog> specification = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            specification = specification.and(AuditLogSpecification.nameLike(name));
        }

        if (userId != null) {
            specification = specification.and(AuditLogSpecification.hasUserId(userId));
        }

        if (username != null && !username.isBlank()) {
            specification = specification.and(AuditLogSpecification.usernameLike(username));
        }

        if (action != null && !action.isBlank()) {
            specification = specification.and(AuditLogSpecification.actionLike(action));
        }

        if (entityType != null && !entityType.isBlank()) {
            specification = specification.and(AuditLogSpecification.entityTypeLike(entityType));
        }

        if (entityId != null) {
            specification = specification.and(AuditLogSpecification.hasEntityId(entityId));
        }

        if (description != null && !description.isBlank()) {
            specification = specification.and(AuditLogSpecification.descriptionLike(description));
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            specification = specification.and(AuditLogSpecification.hasIpAddress(ipAddress));
        }

        if (userAgent != null && !userAgent.isBlank()) {
            specification = specification.and(AuditLogSpecification.userAgentLike(userAgent));
        }

        if (status != null && !status.isBlank()) {
            specification = specification.and(AuditLogSpecification.statusLike(status));
        }

        if (errorMessage != null && !errorMessage.isBlank()) {
            specification = specification.and(AuditLogSpecification.errorMessageLike(errorMessage));
        }

        // Execute query with specifications
        Page<AuditLog> pagedAuditLogs = auditLogRepository.findAll(specification, paging);

        // Convert to DTOs
        List<AuditLogDTO> auditLogDTOs = getAuditLogDTOs(pagedAuditLogs.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("auditLogs", auditLogDTOs);
        response.put("currentPage", pagedAuditLogs.getNumber());
        response.put("totalItems", pagedAuditLogs.getTotalElements());
        response.put("totalPages", pagedAuditLogs.getTotalPages());

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

            log.info("Successfully retrieved audit log {}", id);
            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved audit log.",
                    convertToDTO(auditLog)
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
     */
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

}
