package com.itineraryledger.kabengosafaris.Security;

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
 * REST Controller for managing Security Settings.
 * Provides API endpoints for viewing, creating, updating, and deleting security configuration settings.
 *
 * All settings are stored in the 'security_settings' database table and can be
 * modified at runtime without restarting the application (unless requiresRestart flag is true).
 *
 * Base URL: /api/security-settings
 */
@RestController
@RequestMapping("/api/security-settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Settings", description = "Manage security configuration settings stored in the database")
public class SecuritySettingsController {

    private final SecuritySettingsService securitySettingsService;

    /**
     * Get all active security settings
     * @return List of active settings
     */
    @GetMapping
    @Operation(summary = "Get all active security settings", description = "Returns all active security settings from the database")
    public ResponseEntity<List<SecuritySettings>> getAllSettings() {
        List<SecuritySettings> settings = securitySettingsService.getAllActiveSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get specific setting by key
     * @param settingKey the setting key
     * @return the setting
     */
    @GetMapping("/{settingKey}")
    @Operation(summary = "Get setting by key", description = "Retrieves a specific security setting by its key")
    @Parameter(name = "settingKey", description = "Setting key (e.g., 'jwt.expiration.time.minutes')")
    public ResponseEntity<SecuritySettings> getSetting(@PathVariable String settingKey) {
        try {
            SecuritySettings setting = securitySettingsService.getSetting(settingKey);
            return ResponseEntity.ok(setting);
        } catch (Exception e) {
            log.warn("Security setting not found: {}", settingKey);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all settings by category
     * @param category the category
     * @return List of settings in that category
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get settings by category", description = "Retrieves all active settings in a specific category")
    @Parameter(name = "category", description = "Category (JWT, OBFUSCATION, PASSWORD, SESSION, ACCOUNT_LOCKOUT)")
    public ResponseEntity<List<SecuritySettings>> getSettingsByCategory(@PathVariable String category) {
        List<SecuritySettings> settings = securitySettingsService.getActiveSettingsByCategory(category);
        return ResponseEntity.ok(settings);
    }

    /**
     * Get all system default settings
     * @return List of system default settings
     */
    @GetMapping("/defaults/all")
    @Operation(summary = "Get all system default settings", description = "Returns all system default security settings")
    public ResponseEntity<List<SecuritySettings>> getSystemDefaults() {
        List<SecuritySettings> defaults = securitySettingsService.getAllSystemDefaults();
        return ResponseEntity.ok(defaults);
    }

    /**
     * Get setting value as string
     * @param settingKey the setting key
     * @return the setting value
     */
    @GetMapping("/{settingKey}/value")
    @Operation(summary = "Get setting value", description = "Returns the string value of a security setting")
    public ResponseEntity<String> getSettingValue(@PathVariable String settingKey) {
        try {
            String value = securitySettingsService.getSettingValue(settingKey);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            log.warn("Security setting not found: {}", settingKey);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all settings that require restart on change
     * @return List of settings requiring restart
     */
    @GetMapping("/info/requires-restart")
    @Operation(summary = "Get settings requiring restart", description = "Returns all security settings that require application restart when changed")
    public ResponseEntity<List<SecuritySettings>> getSettingsThatRequireRestart() {
        List<SecuritySettings> settings = securitySettingsService.getSettingsThatRequireRestart();
        return ResponseEntity.ok(settings);
    }

    /**
     * Create a new security setting
     * @param setting the setting to create
     * @return the created setting
     */
    @PostMapping
    @Operation(summary = "Create new security setting", description = "Creates a new security setting in the database")
    public ResponseEntity<SecuritySettings> createSetting(@RequestBody SecuritySettings setting) {
        try {
            SecuritySettings created = securitySettingsService.saveSetting(setting);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Setting creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating security setting", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update security setting value
     * @param settingKey the setting key
     * @param newValue the new setting value
     * @return the updated setting
     */
    @PutMapping("/{settingKey}")
    @Operation(summary = "Update setting value", description = "Updates the value of an existing security setting")
    @Parameter(name = "settingKey", description = "Setting key")
    @Parameter(name = "newValue", description = "New setting value")
    public ResponseEntity<SecuritySettings> updateSetting(
            @PathVariable String settingKey,
            @RequestParam String newValue) {
        try {
            SecuritySettings updated = securitySettingsService.updateSettingValue(settingKey, newValue);

            // Check if restart is required
            if (updated.getRequiresRestart()) {
                log.warn("SECURITY SETTING CHANGED - RESTART REQUIRED: {} = {}", settingKey, newValue);
            }

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.warn("Setting update failed: {} - {}", settingKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update full security setting
     * @param settingKey the setting key
     * @param setting the updated setting
     * @return the updated setting
     */
    @PutMapping("/{settingKey}/full")
    @Operation(summary = "Update full setting", description = "Updates all properties of a security setting")
    public ResponseEntity<SecuritySettings> updateFullSetting(
            @PathVariable String settingKey,
            @RequestBody SecuritySettings setting) {
        try {
            SecuritySettings existing = securitySettingsService.getSetting(settingKey);
            existing.setSettingValue(setting.getSettingValue());
            existing.setDataType(setting.getDataType());
            existing.setDescription(setting.getDescription());
            existing.setCategory(setting.getCategory());
            existing.setActive(setting.getActive());
            existing.setRequiresRestart(setting.getRequiresRestart());

            SecuritySettings updated = securitySettingsService.saveSetting(existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.warn("Setting update failed: {} - {}", settingKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a security setting (soft delete)
     * @param settingKey the setting key
     * @return response entity
     */
    @DeleteMapping("/{settingKey}")
    @Operation(summary = "Deactivate setting", description = "Deactivates a security setting (soft delete, cannot delete system defaults)")
    public ResponseEntity<Void> deactivateSetting(@PathVariable String settingKey) {
        try {
            securitySettingsService.deactivateSetting(settingKey);
            log.info("Security setting deactivated: {}", settingKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Cannot deactivate system default: {}", settingKey);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.warn("Setting deactivation failed: {} - {}", settingKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reactivate a deactivated security setting
     * @param settingKey the setting key
     * @return response entity
     */
    @PostMapping("/{settingKey}/reactivate")
    @Operation(summary = "Reactivate setting", description = "Reactivates a deactivated security setting")
    public ResponseEntity<Void> reactivateSetting(@PathVariable String settingKey) {
        try {
            securitySettingsService.reactivateSetting(settingKey);
            log.info("Security setting reactivated: {}", settingKey);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("Setting reactivation failed: {} - {}", settingKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reset security setting to system default value
     * @param settingKey the setting key
     * @return response entity with updated setting
     */
    @PostMapping("/{settingKey}/reset")
    @Operation(summary = "Reset to default", description = "Resets a security setting to its system default value")
    public ResponseEntity<SecuritySettings> resetToDefault(@PathVariable String settingKey) {
        try {
            securitySettingsService.resetToDefault(settingKey);
            SecuritySettings setting = securitySettingsService.getSetting(settingKey);
            log.info("Security setting reset to default: {}", settingKey);
            return ResponseEntity.ok(setting);
        } catch (Exception e) {
            log.warn("Setting reset failed: {} - {}", settingKey, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if setting exists
     * @param settingKey the setting key
     * @return true if setting exists and is active
     */
    @GetMapping("/{settingKey}/exists")
    @Operation(summary = "Check if setting exists", description = "Returns true if the security setting exists and is active")
    public ResponseEntity<Boolean> settingExists(@PathVariable String settingKey) {
        boolean exists = securitySettingsService.settingExists(settingKey);
        return ResponseEntity.ok(exists);
    }

    /**
     * Security settings health check endpoint
     * @return response indicating if security settings are properly configured
     */
    @GetMapping("/health/status")
    @Operation(summary = "Security settings health status", description = "Returns the status of security settings system")
    public ResponseEntity<SecuritySettingsStatus> getSecurityStatus() {
        try {
            long jwtExpiration = securitySettingsService.getJwtExpirationTimeMillis();
            int idObfuscationLength = securitySettingsService.getIdObfuscationLength();
            int idObfuscationSaltLength = securitySettingsService.getIdObfuscationSaltLength();
            boolean idObfuscationEnabled = securitySettingsService.isIdObfuscationEnabled();
            boolean accountLockoutEnabled = securitySettingsService.isAccountLockoutEnabled();

            return ResponseEntity.ok(SecuritySettingsStatus.builder()
                    .status("healthy")
                    .jwtExpirationMillis(jwtExpiration)
                    .idObfuscationLength(idObfuscationLength)
                    .idObfuscationSaltLength(idObfuscationSaltLength)
                    .idObfuscationEnabled(idObfuscationEnabled)
                    .accountLockoutEnabled(accountLockoutEnabled)
                    .build());
        } catch (Exception e) {
            log.error("Error checking security settings status", e);
            return ResponseEntity.ok(SecuritySettingsStatus.builder()
                    .status("unhealthy")
                    .error(e.getMessage())
                    .build());
        }
    }

    /**
     * DTO for security settings status
     */
    @lombok.Data
    @lombok.Builder
    public static class SecuritySettingsStatus {
        private String status;
        private Long jwtExpirationMillis;
        private Integer idObfuscationLength;
        private Integer idObfuscationSaltLength;
        private Boolean idObfuscationEnabled;
        private Boolean accountLockoutEnabled;
        private String error;
    }
}
