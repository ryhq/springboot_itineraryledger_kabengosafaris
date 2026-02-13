package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Backup Settings Management
 * Provides endpoints to configure and manage backup settings
 */
@RestController
@RequestMapping("/api/backup-settings")
@Slf4j
@RequiredArgsConstructor
public class BackupSettingsController {

    private final BackupSettingsServices backupSettingsServices;

    /**
     * Get all backup settings
     *
     * GET /api/backup-settings
     *
     * @return ResponseEntity with list of all backup settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> getAllSettings() {
        log.info("GET /api/backup-settings - Fetching all backup settings");
        return backupSettingsServices.getAllSettings();
    }

    /**
     * Get backup settings by category
     *
     * GET /api/backup-settings/category/{category}
     *
     * @param category the category to filter by
     * @return ResponseEntity with filtered backup settings
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('PERM_READ_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> getSettingsByCategory(@PathVariable BackupSetting.Category category) {
        log.info("GET /api/backup-settings/category/{} - Fetching backup settings by category", category);
        return backupSettingsServices.getSettingsByCategory(category);
    }

    /**
     * Get active backup settings only
     *
     * GET /api/backup-settings/active
     *
     * @return ResponseEntity with list of active backup settings
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PERM_READ_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> getActiveSettings() {
        log.info("GET /api/backup-settings/active - Fetching active backup settings");
        return backupSettingsServices.getActiveSettings();
    }

    /**
     * Get a specific backup setting by key
     *
     * GET /api/backup-settings/{settingKey}
     *
     * @param settingKey the setting key
     * @return ResponseEntity with the backup setting
     */
    @GetMapping("/{settingKey}")
    @PreAuthorize("hasAuthority('PERM_READ_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> getSettingByKey(@PathVariable String settingKey) {
        log.info("GET /api/backup-settings/{} - Fetching backup setting by key", settingKey);
        return backupSettingsServices.getSettingByKey(settingKey);
    }

    /**
     * Update a backup setting value
     *
     * PUT /api/backup-settings/{settingKey}
     *
     * @param settingKey the setting key
     * @param updateDTO the update DTO with new value
     * @return ResponseEntity with updated backup setting
     */
    @PutMapping("/{settingKey}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> updateSetting(
            @PathVariable String settingKey,
            @RequestBody UpdateBackupSettingDTO updateDTO) {
        log.info("PUT /api/backup-settings/{} - Updating backup setting", settingKey);
        return backupSettingsServices.updateSetting(settingKey, updateDTO);
    }

    /**
     * Reset a backup setting to its default value
     *
     * POST /api/backup-settings/{settingKey}/reset
     *
     * @param settingKey the setting key
     * @return ResponseEntity with reset backup setting
     */
    @PostMapping("/{settingKey}/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BACKUP_SETTING')")
    public ResponseEntity<ApiResponse<?>> resetSetting(@PathVariable String settingKey) {
        log.info("POST /api/backup-settings/{}/reset - Resetting backup setting", settingKey);
        return backupSettingsServices.resetSetting(settingKey);
    }

    /**
     * Health check endpoint
     *
     * GET /api/backup-settings/health
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Backup Settings API is healthy", null)
        );
    }
}
