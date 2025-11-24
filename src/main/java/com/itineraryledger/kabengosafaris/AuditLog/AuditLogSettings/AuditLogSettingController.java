package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Audit Log Settings Management
 * Provides endpoints to view, update, and reset audit logging configurations
 */
@RestController
@RequestMapping("/api/audit-log-settings")
@Validated
public class AuditLogSettingController {

    @Autowired
    private AuditLogSettingServices auditLogSettingServices;

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * GENERAL SETTINGS ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    /**
     * Get all audit log settings
     * GET /api/audit-log-settings
     */
    @GetMapping
    public ResponseEntity<?> getAllAuditLogSettings() {
        return auditLogSettingServices.getAllAuditLogSettings();
    }

    /**
     * Update a specific audit log setting by ID
     * PUT /api/audit-log-settings/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAuditLogSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateAuditLogSettingDTO updateDTO
    ) {
        return auditLogSettingServices.updateAuditLogSetting(id, updateDTO);
    }
    
    /**
     * ═════════════════════════════════════════════════════════════════════════
     * AUDIT LOGGING GENERAL CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    /**
     * Reset audit logging general settings (enabled, retention days) to defaults
     * POST /api/audit-log-settings/reset/general
     */
    @PostMapping("/reset/general")
    public ResponseEntity<?> resetAuditLoggingGeneralSettings() {
        return auditLogSettingServices.resetAuditLoggingGeneralSettings();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * AUDIT LOGGING CAPTURE CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    /**
     * Reset audit logging capture settings (IP, User-Agent, old/new values) to defaults
     * POST /api/audit-log-settings/reset/capture
     */
    @PostMapping("/reset/capture")
    public ResponseEntity<?> resetAuditLoggingCaptureSettings() {
        return auditLogSettingServices.resetAuditLoggingCaptureSettings();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * AUDIT LOGGING VALUE CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    /**
     * Reset audit logging value settings (excluded fields, max value length) to defaults
     * POST /api/audit-log-settings/reset/values
     */
    @PostMapping("/reset/values")
    public ResponseEntity<?> resetAuditLoggingValueSettings() {
        return auditLogSettingServices.resetAuditLoggingValueSettings();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * GENERAL CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    /**
     * Reset all audit log settings to defaults
     * POST /api/audit-log-settings/reset/all
     */
    @PostMapping("/reset/all")
    public ResponseEntity<?> resetAllAuditLogSettings() {
        return auditLogSettingServices.resetAllAuditLogSettings();
    }
}
