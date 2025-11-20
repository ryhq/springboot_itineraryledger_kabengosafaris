package com.itineraryledger.kabengosafaris.AuditLog.Config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Audit Logging Configuration.
 * Provides API endpoints for viewing, creating, updating, and deleting audit configurations.
 *
 * All configurations are stored in the 'audit_log_config' database table and can be
 * modified at runtime without restarting the application.
 *
 * Base URL: /api/audit-config
 */
@RestController
@RequestMapping("/api/audit-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Configuration", description = "Manage audit logging configuration stored in the database")
public class AuditLogConfigController {

    private final AuditLogConfigService auditLogConfigService;

    /**
     * Get all active audit configurations
     * @return List of active configurations
     */
    @GetMapping
    @Operation(summary = "Get all active audit configurations", description = "Returns all active configurations from the database")
    public ResponseEntity<List<AuditLogConfig>> getAllConfigs() {
        List<AuditLogConfig> configs = auditLogConfigService.getAllActiveConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get specific configuration by key
     * @param configKey the configuration key
     * @return the configuration
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "Get configuration by key", description = "Retrieves a specific configuration by its key")
    @Parameter(name = "configKey", description = "Configuration key (e.g., 'audit.log.enabled')")
    public ResponseEntity<AuditLogConfig> getConfig(@PathVariable String configKey) {
        try {
            AuditLogConfig config = auditLogConfigService.getConfig(configKey);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.warn("Configuration not found: {}", configKey);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all system default configurations
     * @return List of system default configurations
     */
    @GetMapping("/defaults/all")
    @Operation(summary = "Get all system default configurations", description = "Returns all system default configurations")
    public ResponseEntity<List<AuditLogConfig>> getSystemDefaults() {
        List<AuditLogConfig> defaults = auditLogConfigService.getAllSystemDefaults();
        return ResponseEntity.ok(defaults);
    }

    /**
     * Get configuration value as string
     * @param configKey the configuration key
     * @return the configuration value
     */
    @GetMapping("/{configKey}/value")
    @Operation(summary = "Get configuration value", description = "Returns the string value of a configuration")
    public ResponseEntity<String> getConfigValue(@PathVariable String configKey) {
        try {
            String value = auditLogConfigService.getConfigValue(configKey);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            log.warn("Configuration not found: {}", configKey);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new configuration
     * @param config the configuration to create
     * @return the created configuration
     */
    @PostMapping
    @Operation(summary = "Create new configuration", description = "Creates a new audit configuration in the database")
    public ResponseEntity<AuditLogConfig> createConfig(@RequestBody AuditLogConfig config) {
        try {
            AuditLogConfig created = auditLogConfigService.saveConfig(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Configuration creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update configuration value
     * @param configKey the configuration key
     * @param newValue the new configuration value
     * @return the updated configuration
     */
    @PutMapping("/{configKey}")
    @Operation(summary = "Update configuration value", description = "Updates the value of an existing configuration")
    @Parameter(name = "configKey", description = "Configuration key")
    @Parameter(name = "newValue", description = "New configuration value")
    public ResponseEntity<AuditLogConfig> updateConfig(
            @PathVariable String configKey,
            @RequestParam String newValue) {
        try {
            AuditLogConfig updated = auditLogConfigService.updateConfigValue(configKey, newValue);
            log.info("Configuration updated: {} = {}", configKey, newValue);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.warn("Configuration update failed: {} - {}", configKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update full configuration
     * @param configKey the configuration key
     * @param config the updated configuration
     * @return the updated configuration
     */
    @PutMapping("/{configKey}/full")
    @Operation(summary = "Update full configuration", description = "Updates all properties of a configuration")
    public ResponseEntity<AuditLogConfig> updateFullConfig(
            @PathVariable String configKey,
            @RequestBody AuditLogConfig config) {
        try {
            AuditLogConfig existing = auditLogConfigService.getConfig(configKey);
            existing.setConfigValue(config.getConfigValue());
            existing.setDataType(config.getDataType());
            existing.setDescription(config.getDescription());
            existing.setActive(config.getActive());

            AuditLogConfig updated = auditLogConfigService.saveConfig(existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.warn("Configuration update failed: {} - {}", configKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a configuration (soft delete)
     * @param configKey the configuration key
     * @return response entity
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "Deactivate configuration", description = "Deactivates a configuration (soft delete, cannot delete system defaults)")
    public ResponseEntity<Void> deactivateConfig(@PathVariable String configKey) {
        try {
            auditLogConfigService.deactivateConfig(configKey);
            log.info("Configuration deactivated: {}", configKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Cannot deactivate system default: {}", configKey);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.warn("Configuration deactivation failed: {} - {}", configKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reactivate a deactivated configuration
     * @param configKey the configuration key
     * @return response entity
     */
    @PostMapping("/{configKey}/reactivate")
    @Operation(summary = "Reactivate configuration", description = "Reactivates a deactivated configuration")
    public ResponseEntity<Void> reactivateConfig(@PathVariable String configKey) {
        try {
            auditLogConfigService.reactivateConfig(configKey);
            log.info("Configuration reactivated: {}", configKey);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("Configuration reactivation failed: {} - {}", configKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reset configuration to system default value
     * @param configKey the configuration key
     * @return response entity
     */
    @PostMapping("/{configKey}/reset")
    @Operation(summary = "Reset to default", description = "Resets configuration to its system default value")
    public ResponseEntity<AuditLogConfig> resetToDefault(@PathVariable String configKey) {
        try {
            auditLogConfigService.resetToDefault(configKey);
            AuditLogConfig config = auditLogConfigService.getConfig(configKey);
            log.info("Configuration reset to default: {}", configKey);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.warn("Configuration reset failed: {} - {}", configKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if configuration exists
     * @param configKey the configuration key
     * @return true if configuration exists and is active
     */
    @GetMapping("/{configKey}/exists")
    @Operation(summary = "Check if configuration exists", description = "Returns true if the configuration exists and is active")
    public ResponseEntity<Boolean> configExists(@PathVariable String configKey) {
        boolean exists = auditLogConfigService.configExists(configKey);
        return ResponseEntity.ok(exists);
    }

    /**
     * Health check endpoint for audit configuration
     * @return response indicating if audit logging is enabled
     */
    @GetMapping("/health/status")
    @Operation(summary = "Audit configuration health status", description = "Returns the status of audit logging system")
    public ResponseEntity<AuditConfigStatus> getAuditStatus() {
        try {
            boolean enabled = auditLogConfigService.getConfigValueAsBoolean("audit.log.enabled");
            int retentionDays = auditLogConfigService.getConfigValueAsInteger("audit.log.retention.days");

            return ResponseEntity.ok(AuditConfigStatus.builder()
                    .enabled(enabled)
                    .retentionDays(retentionDays)
                    .status("healthy")
                    .build());
        } catch (Exception e) {
            log.error("Error checking audit status", e);
            return ResponseEntity.ok(AuditConfigStatus.builder()
                    .status("unhealthy")
                    .error(e.getMessage())
                    .build());
        }
    }

    /**
     * DTO for audit configuration status
     */
    @lombok.Data
    @lombok.Builder
    public static class AuditConfigStatus {
        private Boolean enabled;
        private Integer retentionDays;
        private String status;
        private String error;
    }
}
