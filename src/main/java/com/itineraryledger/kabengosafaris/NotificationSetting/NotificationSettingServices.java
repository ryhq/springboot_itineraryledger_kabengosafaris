package com.itineraryledger.kabengosafaris.NotificationSetting;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSettingServices {

    private final NotificationSettingRepository notificationSettingRepository;

    public ResponseEntity<ApiResponse<?>> getAllSettings() {
        try {
            List<NotificationSetting> settings = notificationSettingRepository.findAll();
            List<NotificationSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} notification settings", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Notification settings retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving notification settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve notification settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getSettingsByCategory(NotificationSetting.Category category) {
        try {
            List<NotificationSetting> settings = notificationSettingRepository.findByCategory(category);
            List<NotificationSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} notification settings for category: {}", dtos.size(), category);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Notification settings for category " + category + " retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving notification settings for category: {}", category, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve notification settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getSettingByKey(String settingKey) {
        try {
            NotificationSetting setting = notificationSettingRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Notification setting not found: " + settingKey));

            NotificationSettingDTO dto = convertToDTO(setting);
            log.info("Retrieved notification setting: {}", settingKey);
            return ResponseEntity.ok(ApiResponse.success(200, "Notification setting retrieved successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Notification setting not found: {}", settingKey);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error retrieving notification setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve notification setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateSetting(String settingKey, UpdateNotificationSettingDTO updateDTO) {
        try {
            NotificationSetting setting = notificationSettingRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Notification setting not found: " + settingKey));

            validateSettingValue(setting, updateDTO.getSettingValue());

            setting.setSettingValue(updateDTO.getSettingValue());
            if (updateDTO.getActive() != null) {
                setting.setActive(updateDTO.getActive());
            }

            NotificationSetting updatedSetting = notificationSettingRepository.save(setting);
            NotificationSettingDTO dto = convertToDTO(updatedSetting);

            log.info("Updated notification setting: {} = {}", settingKey, updateDTO.getSettingValue());
            return ResponseEntity.ok(ApiResponse.success(200, "Notification setting updated successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Failed to update notification setting: {}", settingKey, e);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error updating notification setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to update notification setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    private void validateSettingValue(NotificationSetting setting, String value) {
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
                    break;
                default:
                    throw new IllegalArgumentException("Unknown data type: " + setting.getDataType());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + setting.getDataType() + " type: " + value);
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> resetSetting(String settingKey) {
        try {
            NotificationSetting setting = notificationSettingRepository.findBySettingKey(settingKey)
                    .orElseThrow(() -> new RuntimeException("Notification setting not found: " + settingKey));

            if (!setting.getIsSystemDefault()) {
                return ResponseEntity.status(400).body(
                        ApiResponse.error(400, "Cannot reset non-system default setting", "BAD_REQUEST")
                );
            }

            setting.setActive(true);
            NotificationSetting updatedSetting = notificationSettingRepository.save(setting);
            NotificationSettingDTO dto = convertToDTO(updatedSetting);

            log.info("Reset notification setting: {}", settingKey);
            return ResponseEntity.ok(ApiResponse.success(200, "Notification setting reset successfully", dto));

        } catch (RuntimeException e) {
            log.warn("Failed to reset notification setting: {}", settingKey, e);
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, e.getMessage(), "NOT_FOUND")
            );
        } catch (Exception e) {
            log.error("Error resetting notification setting: {}", settingKey, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset notification setting: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getActiveSettings() {
        try {
            List<NotificationSetting> settings = notificationSettingRepository.findByActiveTrue();
            List<NotificationSettingDTO> dtos = settings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} active notification settings", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Active notification settings retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error retrieving active notification settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to retrieve active notification settings: " + e.getMessage(), "INTERNAL_SERVER_ERROR")
            );
        }
    }

    private NotificationSettingDTO convertToDTO(NotificationSetting setting) {
        return NotificationSettingDTO.builder()
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
