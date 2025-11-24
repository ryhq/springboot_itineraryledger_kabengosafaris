package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Audit Log Settings
 * Provides methods to retrieve, update, and reset audit logging configurations
 * All updates and resets are logged using AuditLogAnnotation
 */
@Service
@Slf4j
public class AuditLogSettingServices {

    // Fallback values from application.properties

    @Value("${audit.log.retention.days:90}")
    private Integer defaultAuditLogRetentionDays;

    @Value("${audit.log.enabled:true}")
    private Boolean defaultAuditLogEnabled;

    @Value("${audit.log.capture.ip.address:true}")
    private Boolean defaultCaptureIpAddress;

    @Value("${audit.log.capture.user.agent:true}")
    private Boolean defaultCaptureUserAgent;

    @Value("${audit.log.capture.old.values:true}")
    private Boolean defaultCaptureOldValues;

    @Value("${audit.log.capture.new.values:true}")
    private Boolean defaultCaptureNewValues;

    @Value("${audit.log.excluded.fields:password,token,secret,apikey,creditcard}")
    private String defaultExcludedFields;

    @Value("${audit.log.max.value.length:2048}")
    private Integer defaultMaxValueLength;

    @Autowired
    private AuditLogSettingRepository auditLogSettingRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Convert AuditLogSetting entity to DTO
     */
    private AuditLogSettingDTO getAuditLogSettingDTO(AuditLogSetting setting) {
        return new AuditLogSettingDTO(
                idObfuscator.encodeId(setting.getId()),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDataType(),
                setting.getDescription(),
                setting.getActive(),
                setting.getIsSystemDefault(),
                setting.getCategory().getDisplayName(),
                setting.getCategory(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }

    /**
     * Convert list of entities to DTOs
     */
    private List<AuditLogSettingDTO> getAuditLogSettingDTOs(List<AuditLogSetting> settings) {
        return settings.stream().map(this::getAuditLogSettingDTO).toList();
    }

    /**
     * Get all audit log settings
     */
    @AuditLogAnnotation(
            action = "RETRIEVE_AUDIT_LOG_SETTINGS",
            description = "Retrieves all audit log settings",
            entityType = "AuditLogSetting"
    )
    public ResponseEntity<?> getAllAuditLogSettings() {
        try {
            List<AuditLogSetting> settings = auditLogSettingRepository.findAll();
            List<AuditLogSettingDTO> settingDTOs = getAuditLogSettingDTOs(settings);
            return ResponseEntity.ok(
                ApiResponse.success(
                    200, 
                    "Audit Log Settings retrieved successfully.", 
                    settingDTOs
                )
            );
        } catch (Exception e) {
            log.error("Error retrieving audit log settings", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    500, 
                    "Failed to retrieve audit log settings.", 
                    "INTERNAL_SERVER_ERROR"
                )
            );
        }
    }

    /**
     * Update audit log setting by obfuscated ID
     */
    public ResponseEntity<?> updateAuditLogSetting(String obfuscatedId, UpdateAuditLogSettingDTO updateDTO) {
        Long id;

        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid Audit Log Setting ID provided.", "VALIDATION_ERROR")
            );
        }

        return updateAuditLogSettingById(id, updateDTO);
    }

    /**
     * Update audit log setting by ID with audit logging
     */
    @AuditLogAnnotation(
            action = "UPDATE_AUDIT_LOG_SETTING",
            description = "Updates an audit log setting by ID",
            entityIdParamName = "id",
            entityType = "AuditLogSetting"
    )
    private ResponseEntity<?> updateAuditLogSettingById(Long id, UpdateAuditLogSettingDTO updateDTO) {
        Boolean active = updateDTO.getActive();
        String description = updateDTO.getDescription();
        String settingValue = updateDTO.getSettingValue();

        // Validation
        if (active == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Active status must be provided.", "VALIDATION_ERROR")
            );
        }
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Description must be provided.", "VALIDATION_ERROR")
            );
        }
        if (settingValue == null || settingValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Setting Value must be provided.", "VALIDATION_ERROR")
            );
        }

        AuditLogSetting setting = auditLogSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Audit Log Setting not found.", "NOT_FOUND")
            );
        }

        // Store old values before changes
        Boolean oldActive = setting.getActive();
        String oldDescription = setting.getDescription();
        String oldSettingValue = setting.getSettingValue();

        // Detect changes
        boolean activeChanged = !active.equals(oldActive);
        boolean descriptionChanged = !description.equals(oldDescription);
        boolean settingValueChanged = !settingValue.equals(oldSettingValue);

        // Check if any changes are being made
        if (!activeChanged && !descriptionChanged && !settingValueChanged) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    ApiResponse.success(
                            200,
                            "No changes detected. Audit Log Setting remains unchanged.",
                            null
                    )
            );
        }

        setting.setActive(active);
        setting.setDescription(description);
        setting.setSettingValue(settingValue);

        setting = auditLogSettingRepository.save(setting);

        // Log each individual field change with old and new values
        logFieldChanges(
                setting.getId(),
                activeChanged,
                descriptionChanged,
                settingValueChanged,
                oldActive,
                active,
                oldDescription,
                description,
                oldSettingValue,
                settingValue
        );

        // Build message with details about which fields changed
        String changeDetails = buildChangeDetails(activeChanged, descriptionChanged, settingValueChanged);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Audit Log Setting updated successfully. " + changeDetails,
                        getAuditLogSettingDTO(setting)
                )
        );
    }

    /**
     * Log individual field changes with old and new values
     */
    private void logFieldChanges(
            Long entityId,
            boolean activeChanged,
            boolean descriptionChanged,
            boolean settingValueChanged,
            Boolean oldActive,
            Boolean newActive,
            String oldDescription,
            String newDescription,
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
                    .action("UPDATE_AUDIT_LOG_SETTING_FIELD")
                    .entityType("AuditLogSetting")
                    .entityId(entityId)
                    .description("Changed active field from " + oldActive + " to " + newActive)
                    .status("SUCCESS")
                    .oldValues("{\"active\": " + oldActive + "}")
                    .newValues("{\"active\": " + newActive + "}")
                    .build();
            auditLogService.logActionSync(log);
        }

        if (descriptionChanged) {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action("UPDATE_AUDIT_LOG_SETTING_FIELD")
                    .entityType("AuditLogSetting")
                    .entityId(entityId)
                    .description("Changed description field from \"" + oldDescription + "\" to \"" + newDescription + "\"")
                    .status("SUCCESS")
                    .oldValues("{\"description\": \"" + escapeJson(oldDescription) + "\"}")
                    .newValues("{\"description\": \"" + escapeJson(newDescription) + "\"}")
                    .build();
            auditLogService.logActionSync(log);
        }

        if (settingValueChanged) {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action("UPDATE_AUDIT_LOG_SETTING_FIELD")
                    .entityType("AuditLogSetting")
                    .entityId(entityId)
                    .description("Changed settingValue field from \"" + oldSettingValue + "\" to \"" + newSettingValue + "\"")
                    .status("SUCCESS")
                    .oldValues("{\"settingValue\": \"" + escapeJson(oldSettingValue) + "\"}")
                    .newValues("{\"settingValue\": \"" + escapeJson(newSettingValue) + "\"}")
                    .build();
            auditLogService.logActionSync(log);
        }
    }

    /**
     * Escape special characters in JSON strings
     */
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

    /**
     * Build a string describing which fields were changed
     */
    private String buildChangeDetails(boolean activeChanged, boolean descriptionChanged, boolean settingValueChanged) {
        StringBuilder details = new StringBuilder("Updated fields: ");
        List<String> changedFields = new ArrayList<>();

        if (activeChanged) {
            changedFields.add("active");
        }
        if (descriptionChanged) {
            changedFields.add("description");
        }
        if (settingValueChanged) {
            changedFields.add("settingValue");
        }

        details.append(String.join(", ", changedFields));
        return details.toString();
    }

    /**
     * ############################################
     * ### Reset Methods for All Settings ###
     * ############################################
     */

    /**
     * Reset audit logging general settings to defaults
     */
    @AuditLogAnnotation(
            action = "RESET_AUDIT_LOGGING_GENERAL_SETTINGS",
            description = "Resets audit logging general settings to default values",
            entityType = "AuditLogSetting"
    )
    public ResponseEntity<?> resetAuditLoggingGeneralSettings() {
        try {
            updateSettingIfExists("audit.log.enabled", String.valueOf(defaultAuditLogEnabled));
            updateSettingIfExists("audit.log.retention.days", String.valueOf(defaultAuditLogRetentionDays));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Audit Logging General Settings reset to default values successfully.",
                            null
                    )
            );
        } catch (Exception e) {
            log.error("Error resetting audit logging general settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset audit logging general settings.", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Reset audit logging capture settings to defaults
     */
    @AuditLogAnnotation(
            action = "RESET_AUDIT_LOGGING_CAPTURE_SETTINGS",
            description = "Resets audit logging capture settings to default values",
            entityType = "AuditLogSetting"
    )
    public ResponseEntity<?> resetAuditLoggingCaptureSettings() {
        try {
            updateSettingIfExists("audit.log.capture.ip.address", String.valueOf(defaultCaptureIpAddress));
            updateSettingIfExists("audit.log.capture.user.agent", String.valueOf(defaultCaptureUserAgent));
            updateSettingIfExists("audit.log.capture.old.values", String.valueOf(defaultCaptureOldValues));
            updateSettingIfExists("audit.log.capture.new.values", String.valueOf(defaultCaptureNewValues));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Audit Logging Capture Settings reset to default values successfully.",
                            null
                    )
            );
        } catch (Exception e) {
            log.error("Error resetting audit logging capture settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset audit logging capture settings.", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Reset audit logging value settings to defaults
     */
    @AuditLogAnnotation(
            action = "RESET_AUDIT_LOGGING_VALUE_SETTINGS",
            description = "Resets audit logging value settings to default values",
            entityType = "AuditLogSetting"
    )
    public ResponseEntity<?> resetAuditLoggingValueSettings() {
        try {
            updateSettingIfExists("audit.log.excluded.fields", defaultExcludedFields);
            updateSettingIfExists("audit.log.max.value.length", String.valueOf(defaultMaxValueLength));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Audit Logging Value Settings reset to default values successfully.",
                            null
                    )
            );
        } catch (Exception e) {
            log.error("Error resetting audit logging value settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset audit logging value settings.", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Reset all audit log settings to defaults
     */
    @AuditLogAnnotation(
            action = "RESET_ALL_AUDIT_LOG_SETTINGS",
            description = "Resets all audit log settings to default values",
            entityType = "AuditLogSetting"
    )
    public ResponseEntity<?> resetAllAuditLogSettings() {
        try {
            // Reset all categories
            resetAuditLoggingGeneralSettings();
            resetAuditLoggingCaptureSettings();
            resetAuditLoggingValueSettings();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "All Audit Log Settings reset to default values successfully.",
                            null
                    )
            );
        } catch (Exception e) {
            log.error("Error resetting all audit log settings", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to reset all audit log settings.", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Helper method to update a setting if it exists in the database
     */
    private void updateSettingIfExists(String settingKey, String settingValue) {
        try {
            AuditLogSetting setting = auditLogSettingRepository.findBySettingKey(settingKey);
            if (setting != null) {
                setting.setSettingValue(settingValue);
                auditLogSettingRepository.save(setting);
                log.debug("Updated setting {} to value: {}", settingKey, settingValue);
            } else {
                log.warn("Setting not found: {}", settingKey);
            }
        } catch (Exception e) {
            log.error("Error updating setting {}: {}", settingKey, e.getMessage());
        }
    }
}
