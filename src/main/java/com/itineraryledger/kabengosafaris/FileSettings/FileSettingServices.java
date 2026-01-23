package com.itineraryledger.kabengosafaris.FileSettings;

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
 * Service for managing file settings.
 * Provides CRUD operations and reset functionality for file upload configuration.
 */
@Service
public class FileSettingServices {

    // =====================================================================
    // Fallback values from application.properties - GENERAL UPLOAD Settings
    // =====================================================================

    @Value("${file.upload.enabled:true}")
    private Boolean fileUploadEnabled;

    @Value("${file.upload.max.file.size:10485760}")
    private Long fileUploadMaxFileSize;

    @Value("${file.upload.allowed.extensions:pdf,doc,docx,xls,xlsx,csv,txt,zip}")
    private String fileUploadAllowedExtensions;

    @Value("${file.upload.blocked.extensions:exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js}")
    private String fileUploadBlockedExtensions;

    @Value("${file.upload.sanitize.filename:true}")
    private Boolean fileUploadSanitizeFilename;

    @Value("${file.upload.validate.content.type:true}")
    private Boolean fileUploadValidateContentType;

    // =====================================================================
    // Fallback values from application.properties - SPECIFIC FILE TYPE Settings
    // =====================================================================

    @Value("${email.signature.max.file.size:1048576}")
    private Long emailSignatureMaxFileSize;

    @Value("${email.template.max.file.size:2097152}")
    private Long emailTemplateMaxFileSize;

    @Value("${pdf.template.max.file.size:5242880}")
    private Long pdfTemplateMaxFileSize;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private FileSettingRepository fileSettingRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Convert entity to DTO
     */
    private FileSettingDTO toDTO(FileSetting setting) {
        return new FileSettingDTO(
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
    private List<FileSettingDTO> toDTOList(List<FileSetting> settings) {
        return settings.stream().map(this::toDTO).toList();
    }

    /**
     * Get all file settings
     */
    public ResponseEntity<?> getAllFileSettings() {
        List<FileSetting> settings = fileSettingRepository.findAll();
        List<FileSettingDTO> dtos = toDTOList(settings);
        return ResponseEntity.ok(ApiResponse.success(200, "File Settings retrieved successfully.", dtos));
    }

    /**
     * Get file settings by category
     */
    public ResponseEntity<?> getFileSettingsByCategory(FileSetting.Category category) {
        List<FileSetting> settings = fileSettingRepository.findActiveByCategoryOrderBySettingKeyAsc(category);
        List<FileSettingDTO> dtos = toDTOList(settings);
        return ResponseEntity.ok(ApiResponse.success(200,
            String.format("%s retrieved successfully.", category.getDisplayName()), dtos));
    }

    /**
     * Update a specific file setting
     */
    @AuditLogAnnotation(
        action = "UPDATE_FILE_SETTING",
        description = "Updating a file setting",
        entityIdParamName = "obfuscatedId",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> updateFileSetting(String obfuscatedId, UpdateFileSettingDTO updateDTO) {
        Long id;

        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid File Setting ID provided.", "VALIDATION_ERROR")
            );
        }

        return updateFileSettingById(id, updateDTO);
    }

    private ResponseEntity<?> updateFileSettingById(Long id, UpdateFileSettingDTO updateDTO) {
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

        FileSetting setting = fileSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "File Setting not found.", "NOT_FOUND")
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
                ApiResponse.success(200, "No changes detected. File Setting remains unchanged.", null)
            );
        }

        setting.setActive(active);
        setting.setSettingValue(settingValue);

        setting = fileSettingRepository.save(setting);

        // Log each individual field change
        logFieldChanges(setting.getId(), activeChanged, settingValueChanged, oldActive, active, oldSettingValue, settingValue);

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "File Setting updated successfully. " +
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
                .action("Update File Setting Field")
                .entityType("FileSetting")
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
                .action("Update File Setting Field")
                .entityType("FileSetting")
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
    // Reset Methods for Each Category
    // =====================================================================

    /**
     * Reset UPLOAD settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_FILE_UPLOAD_SETTINGS",
        description = "Resetting file upload settings to defaults",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> resetUploadSettings() {
        updateSettingIfExists("file.upload.enabled", String.valueOf(fileUploadEnabled));
        updateSettingIfExists("file.upload.max.file.size", String.valueOf(fileUploadMaxFileSize));
        updateSettingIfExists("file.upload.allowed.extensions", fileUploadAllowedExtensions);
        updateSettingIfExists("file.upload.blocked.extensions", fileUploadBlockedExtensions);
        updateSettingIfExists("file.upload.sanitize.filename", String.valueOf(fileUploadSanitizeFilename));
        updateSettingIfExists("file.upload.validate.content.type", String.valueOf(fileUploadValidateContentType));

        return ResponseEntity.ok(
            ApiResponse.success(200, "File Upload Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset EMAIL SIGNATURE settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_EMAIL_SIGNATURE_FILE_SETTINGS",
        description = "Resetting email signature file settings to defaults",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> resetEmailSignatureSettings() {
        updateSettingIfExists("file.email.signature.max.file.size", String.valueOf(emailSignatureMaxFileSize));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Email Signature File Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset EMAIL TEMPLATE settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_EMAIL_TEMPLATE_FILE_SETTINGS",
        description = "Resetting email template file settings to defaults",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> resetEmailTemplateSettings() {
        updateSettingIfExists("file.email.template.max.file.size", String.valueOf(emailTemplateMaxFileSize));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Email Template File Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset PDF TEMPLATE settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_PDF_TEMPLATE_FILE_SETTINGS",
        description = "Resetting PDF template file settings to defaults",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> resetPdfTemplateSettings() {
        updateSettingIfExists("file.pdf.template.max.file.size", String.valueOf(pdfTemplateMaxFileSize));

        return ResponseEntity.ok(
            ApiResponse.success(200, "PDF Template File Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset ALL file settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_ALL_FILE_SETTINGS",
        description = "Resetting all file settings to defaults",
        entityType = "FileSetting"
    )
    public ResponseEntity<?> resetAllSettings() {
        resetUploadSettings();
        resetEmailSignatureSettings();
        resetEmailTemplateSettings();
        resetPdfTemplateSettings();

        return ResponseEntity.ok(
            ApiResponse.success(200, "All File Settings reset to default values successfully.", null)
        );
    }

    /**
     * Helper method to update a setting if it exists in the database
     */
    private void updateSettingIfExists(String settingKey, String settingValue) {
        FileSetting setting = fileSettingRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            fileSettingRepository.save(setting);
        }
    }
}
