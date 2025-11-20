package com.itineraryledger.kabengosafaris.AuditLog;

import com.itineraryledger.kabengosafaris.Security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for retrieving audit logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get all audit logs with pagination")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getEntityTypeAuditLogs("AuditLog", pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "userId", description = "User ID")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogs(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getUserAuditLogs(userId, pageable));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs for a specific action")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "action", description = "Action name (e.g., CREATE_ROLE, UPDATE_USER)")
    public ResponseEntity<Page<AuditLog>> getActionAuditLogs(
            @PathVariable String action,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getActionAuditLogs(action, pageable));
    }

    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get audit logs for a specific entity type")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "entityType", description = "Entity type (e.g., Role, User, Permission)")
    public ResponseEntity<Page<AuditLog>> getEntityTypeAuditLogs(
            @PathVariable String entityType,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getEntityTypeAuditLogs(entityType, pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get complete audit trail for a specific entity")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "entityType", description = "Entity type (e.g., Role, User)")
    @Parameter(name = "entityId", description = "Entity ID")
    public ResponseEntity<List<AuditLog>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getEntityAuditTrail(entityType, entityId));
    }

    @GetMapping("/entity/{entityType}/{entityId}/paginated")
    @Operation(summary = "Get paginated audit logs for a specific entity")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "entityType", description = "Entity type")
    @Parameter(name = "entityId", description = "Entity ID")
    public ResponseEntity<Page<AuditLog>> getEntityAuditLogsPaginated(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getEntityAuditLogs(entityType, entityId, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit logs within a date range")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    @Parameter(name = "startDate", description = "Start date (ISO 8601 format: yyyy-MM-ddTHH:mm:ss)")
    @Parameter(name = "endDate", description = "End date (ISO 8601 format: yyyy-MM-ddTHH:mm:ss)")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/user/{userId}/date-range")
    @Operation(summary = "Get audit logs for a user within a date range")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    public ResponseEntity<List<AuditLog>> getUserAuditLogsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(auditLogService.getUserAuditLogsByDateRange(userId, startDate, endDate));
    }

    @GetMapping("/action/{action}/date-range")
    @Operation(summary = "Get audit logs for an action within a date range")
    @RequirePermission(action = "READ", resource = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> getActionAuditLogsByDateRange(
            @PathVariable String action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getActionAuditLogsByDateRange(action, startDate, endDate, pageable));
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Delete audit logs older than specified days (retention policy)")
    @RequirePermission(action = "DELETE", resource = "AUDIT_LOG")
    @Parameter(name = "retentionDays", description = "Keep logs for this many days")
    public ResponseEntity<String> cleanupOldAuditLogs(
            @RequestParam(defaultValue = "90") int retentionDays) {
        long deletedCount = auditLogService.deleteOldAuditLogs(retentionDays);
        return ResponseEntity.ok(String.format("Deleted %d audit logs older than %d days", deletedCount, retentionDays));
    }

}
