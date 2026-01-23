package com.itineraryledger.kabengosafaris.ImageSettings;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

/**
 * Service for managing image settings.
 * Provides CRUD operations and reset functionality for image configuration.
 */
@Service
public class ImageSettingServices {

    // =====================================================================
    // Fallback values from application.properties - UPLOAD Settings
    // =====================================================================

    @Value("${image.upload.enabled:true}")
    private Boolean imageUploadEnabled;

    @Value("${image.upload.max.file.size:5242880}")
    private Long imageUploadMaxFileSize;

    @Value("${image.upload.allowed.formats:jpg,jpeg,png,webp,gif}")
    private String imageUploadAllowedFormats;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private ImageSettingRepository imageSettingRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Convert entity to DTO
     */
    private ImageSettingDTO toDTO(ImageSetting setting) {
        return new ImageSettingDTO(
            idObfuscator.encodeId(setting.getId()),
            setting.getCategory().getDisplayName(),
            setting.getSettingKey(),
            setting.getSettingValue(),
            setting.getDataType(),
            setting.getDescription(),
            setting.getActive(),
            setting.getIsSystemDefault(),
            setting.getCategory(),
            setting.getRequiresRestart(),
            setting.getCreatedAt(),
            setting.getUpdatedAt()
        );
    }

    /**
     * Convert list of entities to DTOs
     */
    private List<ImageSettingDTO> toDTOList(List<ImageSetting> settings) {
        return settings.stream().map(this::toDTO).toList();
    }

    /**
     * Get all image settings
     */
    public ResponseEntity<?> getAllImageSettings() {
        List<ImageSetting> settings = imageSettingRepository.findAll();
        List<ImageSettingDTO> dtos = toDTOList(settings);
        return ResponseEntity.ok(ApiResponse.success(200, "Image Settings retrieved successfully.", dtos));
    }

    /**
     * Update a specific image setting
     */
    @AuditLogAnnotation(
        action = "UPDATE_IMAGE_SETTING",
        description = "Updating an image setting",
        entityIdParamName = "obfuscatedId",
        entityType = "ImageSetting"
    )
    public ResponseEntity<?> updateImageSetting(String obfuscatedId, UpdateImageSettingDTO updateDTO) {
        Long id;

        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Image Setting ID provided.", "VALIDATION_ERROR")
            );
        }

        return updateImageSettingById(id, updateDTO);
    }

    private ResponseEntity<?> updateImageSettingById(Long id, UpdateImageSettingDTO updateDTO) {
        Boolean active = updateDTO.getActive();
        String settingValue = updateDTO.getSettingValue();

        // Validation
        if (active == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Active status must be provided.", "VALIDATION_ERROR")
            );
        }

        if (settingValue == null || settingValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Setting Value must be provided.", "VALIDATION_ERROR")
            );
        }

        ImageSetting setting = imageSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Image Setting not found.", "NOT_FOUND")
            );
        }

        // Store old values before changes
        Boolean oldActive = setting.getActive();
        String oldSettingValue = setting.getSettingValue();

        // Detect changes
        boolean activeChanged = !active.equals(oldActive);
        boolean settingValueChanged = !settingValue.equals(oldSettingValue);

        // Check if any changes are being made
        if (!activeChanged && !settingValueChanged) {
            return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(200, "No changes detected. Image Setting remains unchanged.", null)
            );
        }

        setting.setActive(active);
        setting.setSettingValue(settingValue);

        setting = imageSettingRepository.save(setting);

        // Log each individual field change
        logFieldChanges(setting.getId(), activeChanged, settingValueChanged, oldActive, active, oldSettingValue, settingValue);

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Image Setting updated successfully. " +
                    (activeChanged ? "Active status changed. " : "") +
                    (settingValueChanged ? "Setting Value changed." : ""),
                toDTO(setting)
            )
        );
    }

    private void logFieldChanges(
        Long entityId,
        boolean activeChanged,
        boolean settingValueChanged,
        Boolean oldActive,
        Boolean newActive,
        String oldSettingValue,
        String newSettingValue
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "SYSTEM";

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            username = authentication.getName();
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.itineraryledger.kabengosafaris.User.User) {
                userId = ((com.itineraryledger.kabengosafaris.User.User) principal).getId();
            }
        }

        if (activeChanged) {
            AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action("Update Image Setting Field")
                .entityType("ImageSetting")
                .entityId(entityId)
                .description("Changed active field from " + oldActive + " to " + newActive)
                .status("SUCCESS")
                .oldValues("{\"active\": " + oldActive + "}")
                .newValues("{\"active\": " + newActive + "}")
                .build();
            auditLogService.logActionSync(log);
        }

        if (settingValueChanged) {
            AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action("Update Image Setting Field")
                .entityType("ImageSetting")
                .entityId(entityId)
                .description("Changed settingValue field from \"" + oldSettingValue + "\" to \"" + newSettingValue + "\"")
                .status("SUCCESS")
                .oldValues("{\"settingValue\": \"" + escapeJson(oldSettingValue) + "\"}")
                .newValues("{\"settingValue\": \"" + escapeJson(newSettingValue) + "\"}")
                .build();
            auditLogService.logActionSync(log);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // =====================================================================
    // Reset Methods for UPLOAD Category
    // =====================================================================

    /**
     * Reset UPLOAD settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_IMAGE_UPLOAD_SETTINGS",
        description = "Resetting image upload settings to defaults",
        entityType = "ImageSetting"
    )
    public ResponseEntity<?> resetUploadSettings() {
        updateSettingIfExists("image.upload.enabled", String.valueOf(imageUploadEnabled));
        updateSettingIfExists("image.upload.max.file.size", String.valueOf(imageUploadMaxFileSize));
        updateSettingIfExists("image.upload.allowed.formats", imageUploadAllowedFormats);

        return ResponseEntity.ok(
            ApiResponse.success(200, "Image Upload Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset ALL image settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_ALL_IMAGE_SETTINGS",
        description = "Resetting all image settings to defaults",
        entityType = "ImageSetting"
    )
    public ResponseEntity<?> resetAllSettings() {
        resetUploadSettings();

        return ResponseEntity.ok(
            ApiResponse.success(200, "All Image Settings reset to default values successfully.", null)
        );
    }

    /**
     * Helper method to update a setting if it exists in the database
     */
    private void updateSettingIfExists(String settingKey, String settingValue) {
        ImageSetting setting = imageSettingRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            imageSettingRepository.save(setting);
        }
    }
}
