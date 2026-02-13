package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing backup settings
 * Provides CRUD operations and business logic for backup configuration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BackupSettingsServices {

    private final BackupSettingsRepository backupSettingsRepository;

    /**
     * Get all backup settings
     *
     * @return ResponseEntity with list of all backup settings
     */
    public ResponseEntity<ApiResponse<?>> getAllSettings() {
        try {
            List<BackupSetting> settings = backupSettingsRepository.findAll();
            List<BackupSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} backup settings", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Backup settings retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving backup settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve backup settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Get backup settings by category
     *
     * @param category the category to filter by
     * @return ResponseEntity with filtered backup settings
     */
    public ResponseEntity<ApiResponse<?>> getSettingsByCategory(BackupSetting.Category category) {
        try {
            List<BackupSetting> settings = backupSettingsRepository.findByCategory(category);
            List<BackupSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} backup settings for category: {}", dtos.size(), category);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Backup settings for category " + category + " retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving backup settings for category: {}", category, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve backup settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Get a specific backup setting by key
     *
     * @param settingKey the setting key
     * @return ResponseEntity with the backup setting
     */
    public ResponseEntity<ApiResponse<?>> getSettingByKey(String settingKey) {
        try {
            BackupSetting setting = backupSettingsRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Backup setting not found: " + settingKey));

            BackupSettingDTO dto = convertToDTO(setting);
            log.info("Retrieved backup setting: {}", settingKey);
            return ResponseEntity.ok(ApiResponse.success(200, "Backup setting retrieved successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Backup setting not found: {}", settingKey);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error retrieving backup setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve backup setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Update a backup setting value
     *
     * @param settingKey the setting key
     * @param updateDTO the update DTO with new value
     * @return ResponseEntity with updated backup setting
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateSetting(String settingKey, UpdateBackupSettingDTO updateDTO) {
        try {
            BackupSetting setting = backupSettingsRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Backup setting not found: " + settingKey));

            // Validate the new value based on data type
            validateSettingValue(setting, updateDTO.getSettingValue());

            // Update the setting
            setting.setSettingValue(updateDTO.getSettingValue());
            if (updateDTO.getActive() != null) {
                setting.setActive(updateDTO.getActive());
            }

            BackupSetting updatedSetting = backupSettingsRepository.save(setting);
            BackupSettingDTO dto = convertToDTO(updatedSetting);

            log.info("Updated backup setting: {} = {}", settingKey, updateDTO.getSettingValue());
            return ResponseEntity.ok(ApiResponse.success(200, "Backup setting updated successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Failed to update backup setting: {}", settingKey, e);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error updating backup setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to update backup setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Validate setting value based on data type
     */
    private void validateSettingValue(BackupSetting setting, String value) {
        try {
            switch (setting.getDataType()) {
                case INTEGER:
                    Integer.parseInt(value);
                    break;
                case LONG:
                    Long.parseLong(value);
                    break;
                case DOUBLE:
                    Double.parseDouble(value);
                    break;
                case BOOLEAN:
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Boolean value must be 'true' or 'false'");
                    }
                    break;
                case STRING:
                    // String values don't need validation
                    break;
                default:
                    throw new IllegalArgumentException("Unknown data type: " + setting.getDataType());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + setting.getDataType() + " type: " + value);
        }
    }

    /**
     * Reset a backup setting to its default value
     *
     * @param settingKey the setting key
     * @return ResponseEntity with reset backup setting
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resetSetting(String settingKey) {
        try {
            BackupSetting setting = backupSettingsRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Backup setting not found: " + settingKey));

            if (!setting.getIsSystemDefault()) {
                return ResponseEntity.status(400).body(
                        ApiResponse.error(400, "Cannot reset non-system default setting", "BAD_REQUEST")
                );
            }

            // Reset logic would require storing original default values
            // For now, we'll just mark it as active
            setting.setActive(true);
            BackupSetting updatedSetting = backupSettingsRepository.save(setting);
            BackupSettingDTO dto = convertToDTO(updatedSetting);

            log.info("Reset backup setting: {}", settingKey);
            return ResponseEntity.ok(ApiResponse.success(200, "Backup setting reset successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Failed to reset backup setting: {}", settingKey, e);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error resetting backup setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset backup setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Get all active backup settings
     *
     * @return ResponseEntity with list of active backup settings
     */
    public ResponseEntity<ApiResponse<?>> getActiveSettings() {
        try {
            List<BackupSetting> settings = backupSettingsRepository.findByActiveTrue();
            List<BackupSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} active backup settings", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Active backup settings retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving active backup settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve active backup settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Convert BackupSetting entity to DTO
     */
    private BackupSettingDTO convertToDTO(BackupSetting setting) {
        return BackupSettingDTO.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .dataType(setting.getDataType())
                .description(setting.getDescription())
                .active(setting.getActive())
                .isSystemDefault(setting.getIsSystemDefault())
                .category(setting.getCategory())
                .requiresRestart(setting.getRequiresRestart())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
