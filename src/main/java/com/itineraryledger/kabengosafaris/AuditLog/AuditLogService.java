package com.itineraryledger.kabengosafaris.AuditLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an action asynchronously to avoid blocking the main request
     */
    @Async
    public void logAction(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {} - {}", auditLog.getUsername(), auditLog.getAction(), auditLog.getEntityType());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Synchronously log an action (blocks until saved)
     */
    public void logActionSync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved synchronously: {} - {} - {}", auditLog.getUsername(), auditLog.getAction(), auditLog.getEntityType());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Get audit logs for a specific user with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get audit logs for a specific action with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getActionAuditLogs(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * Get audit logs for a specific entity type with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getEntityTypeAuditLogs(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }

    /**
     * Get audit logs for a specific entity instance (audit trail)
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityAuditTrail(String entityType, Long entityId) {
        return auditLogRepository.findEntityAuditTrail(entityType, entityId);
    }

    /**
     * Get audit logs for a specific entity type and ID with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getEntityAuditLogs(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    /**
     * Get audit logs within a date range with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Get audit logs for a user within a specific date range
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserAuditLogsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Get audit logs for an action within a date range
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getActionAuditLogsByDateRange(String action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByActionAndDateRange(action, startDate, endDate, pageable);
    }

    /**
     * Delete old audit logs (retention policy)
     * Keeps logs for specified number of days
     */
    @Transactional
    public long deleteOldAuditLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> oldLogs = auditLogRepository.findByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                cutoffDate,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        long deletedCount = oldLogs.size();
        auditLogRepository.deleteAll(oldLogs);
        log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        return deletedCount;
    }

}
