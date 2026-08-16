package com.itineraryledger.kabengosafaris.AuditLog;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Audit Log Management
 *
 * Provides endpoints for:
 * - Retrieving audit logs with filtering, pagination, and sorting
 * - Getting a single audit log by ID
 *
 * All endpoints require READ_AUDIT_LOG permission to access.
 */
@RestController
@RequestMapping("/api/audit-logs")
@Slf4j
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Get all audit logs with optional filtering, pagination, and sorting
     *
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param name Filter by audit log name (partial match, case-insensitive)
     * @param userId Filter by user ID (obfuscated)
     * @param username Filter by username (partial match, case-insensitive)
     * @param action Filter by action (partial match, case-insensitive)
     * @param entityType Filter by entity type (partial match, case-insensitive)
     * @param entityId Filter by entity ID (obfuscated)
     * @param description Filter by description (partial match, case-insensitive)
     * @param ipAddress Filter by IP address (exact match)
     * @param userAgent Filter by user agent (partial match, case-insensitive)
     * @param status Filter by status (partial match, case-insensitive)
     * @param errorMessage Filter by error message (partial match, case-insensitive)
     * @param sortDirection Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated audit logs
     *
     * Example: GET /api/audit-logs?page=0&size=10&entityType=USER&status=SUCCESS&sortDirection=desc
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_AUDIT_LOG')")
    public ResponseEntity<?> getAllAuditLogs(
        /*
         * Every parameter the old signature took is still spelled the same on the wire —
         * @ModelAttribute binds them onto the filter — plus the multi-value forms (actions,
         * entityTypes, statuses), a keyword, and the date range an audit log is actually
         * read by.
         */
        @ModelAttribute AuditLogFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/audit-logs - Fetching audit logs with filters");
        return auditLogService.getAllAuditLogs(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * The values this log actually holds, for the filter dropdowns.
     *
     * A hard-coded list of 330 action names would drift the moment somebody adds a module,
     * and a filter offering a value that matches nothing is worse than no filter.
     */
    @GetMapping("/facets")
    @PreAuthorize("hasAuthority('PERM_READ_AUDIT_LOG')")
    public ResponseEntity<ApiResponse<?>> getFacetValues() {
        return auditLogService.getFacetValues();
    }

    /**
     * Get a single audit log by ID
     *
     * @param id Obfuscated audit log ID
     * @return ResponseEntity with audit log details or error
     *
     * Example: GET /api/audit-logs/abc123def456
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_AUDIT_LOG')")
    public ResponseEntity<ApiResponse<?>> getAuditLog(
        @PathVariable String id,
        // the list's filter and sort, so prev/next stays inside the set on screen
        @ModelAttribute AuditLogFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/audit-logs/{} - Fetching audit log", id);
        return auditLogService.getAuditLog(id, filter, sortBy, sortDirection);
    }
}
