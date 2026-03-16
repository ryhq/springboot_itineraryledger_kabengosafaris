package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailSettingRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailSettingDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.UpdateEmailSettingDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailSettingServices {

    private final EmailSettingRepository emailSettingRepository;
    private final IdObfuscator idObfuscator;
    private final AuditLogService auditLogService;

    // ---- Fallback defaults for reset ----
    @Value("${email.retention.days:365}")
    private String defaultRetentionDays;
    @Value("${email.retention.auto.cleanup.enabled:true}")
    private String defaultAutoCleanupEnabled;
    @Value("${email.retention.trash.days:30}")
    private String defaultTrashRetentionDays;
    @Value("${email.fetch.auto.enabled:false}")
    private String defaultAutoFetchEnabled;
    @Value("${email.fetch.default.interval.minutes:5}")
    private String defaultFetchIntervalMinutes;
    @Value("${email.fetch.max.count:50}")
    private String defaultMaxFetchCount;
    @Value("${email.storage.base.path:./data/emails/}")
    private String defaultStorageBasePath;
    @Value("${email.storage.max.size:5368709120}")
    private String defaultMaxStorageSize;
    @Value("${email.eml.max.file.size:26214400}")
    private String defaultMaxEmlFileSize;
    @Value("${email.attachment.max.file.size:10485760}")
    private String defaultMaxAttachmentFileSize;
    @Value("${email.sent.capture.enabled:true}")
    private String defaultSentCaptureEnabled;

    private EmailSettingDTO toDTO(EmailSetting setting) {
        return new EmailSettingDTO(
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

    private List<EmailSettingDTO> toDTOList(List<EmailSetting> settings) {
        return settings.stream().map(this::toDTO).toList();
    }

    public ResponseEntity<?> getAllEmailSettings() {
        List<EmailSetting> settings = emailSettingRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(200, "Email Settings retrieved successfully.", toDTOList(settings)));
    }

    public ResponseEntity<?> getEmailSettingsByCategory(EmailSetting.Category category) {
        List<EmailSetting> settings = emailSettingRepository.findActiveByCategoryOrderBySettingKeyAsc(category);
        return ResponseEntity.ok(ApiResponse.success(200,
            String.format("%s retrieved successfully.", category.getDisplayName()), toDTOList(settings)));
    }

    @AuditLogAnnotation(
        action = "UPDATE_EMAIL_SETTING",
        description = "Updating an email setting",
        entityIdParamName = "obfuscatedId",
        entityType = "EmailSetting"
    )
    public ResponseEntity<?> updateEmailSetting(String obfuscatedId, UpdateEmailSettingDTO updateDTO) {
        Long id;
        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Email Setting ID provided.", "VALIDATION_ERROR"));
        }

        Boolean active = updateDTO.getActive();
        String settingValue = updateDTO.getSettingValue();

        if (active == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Active status must be provided.", "VALIDATION_ERROR"));
        }
        if (settingValue == null || settingValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Setting Value must be provided.", "VALIDATION_ERROR"));
        }

        EmailSetting setting = emailSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Email Setting not found.", "NOT_FOUND"));
        }

        Boolean oldActive = setting.getActive();
        String oldSettingValue = setting.getSettingValue();

        boolean activeChanged = !active.equals(oldActive);
        boolean settingValueChanged = !settingValue.equals(oldSettingValue);

        if (!activeChanged && !settingValueChanged) {
            return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(200, "No changes detected. Email Setting remains unchanged.", null));
        }

        setting.setActive(active);
        setting.setSettingValue(settingValue);
        setting = emailSettingRepository.save(setting);

        logFieldChanges(setting.getId(), activeChanged, settingValueChanged, oldActive, active, oldSettingValue, settingValue);

        return ResponseEntity.ok(ApiResponse.success(200,
            "Email Setting updated successfully. " +
                (activeChanged ? "Active status changed. " : "") +
                (settingValueChanged ? "Setting Value changed." : ""),
            toDTO(setting)));
    }

    // =====================================================================
    // Reset Methods
    // =====================================================================

    @AuditLogAnnotation(action = "RESET_EMAIL_RETENTION_SETTINGS", description = "Resetting email retention settings", entityType = "EmailSetting")
    public ResponseEntity<?> resetRetentionSettings() {
        updateSettingIfExists("email.retention.days", defaultRetentionDays);
        updateSettingIfExists("email.retention.auto.cleanup.enabled", defaultAutoCleanupEnabled);
        updateSettingIfExists("email.retention.trash.days", defaultTrashRetentionDays);
        return ResponseEntity.ok(ApiResponse.success(200, "Email Retention Settings reset to defaults.", null));
    }

    @AuditLogAnnotation(action = "RESET_EMAIL_FETCH_SETTINGS", description = "Resetting email fetch settings", entityType = "EmailSetting")
    public ResponseEntity<?> resetFetchSettings() {
        updateSettingIfExists("email.fetch.auto.enabled", defaultAutoFetchEnabled);
        updateSettingIfExists("email.fetch.default.interval.minutes", defaultFetchIntervalMinutes);
        updateSettingIfExists("email.fetch.max.count", defaultMaxFetchCount);
        return ResponseEntity.ok(ApiResponse.success(200, "Email Fetch Settings reset to defaults.", null));
    }

    @AuditLogAnnotation(action = "RESET_EMAIL_STORAGE_SETTINGS", description = "Resetting email storage settings", entityType = "EmailSetting")
    public ResponseEntity<?> resetStorageSettings() {
        updateSettingIfExists("email.storage.base.path", defaultStorageBasePath);
        updateSettingIfExists("email.storage.max.size", defaultMaxStorageSize);
        updateSettingIfExists("email.eml.max.file.size", defaultMaxEmlFileSize);
        updateSettingIfExists("email.attachment.max.file.size", defaultMaxAttachmentFileSize);
        updateSettingIfExists("email.sent.capture.enabled", defaultSentCaptureEnabled);
        return ResponseEntity.ok(ApiResponse.success(200, "Email Storage Settings reset to defaults.", null));
    }

    @AuditLogAnnotation(action = "RESET_ALL_EMAIL_SETTINGS", description = "Resetting all email settings", entityType = "EmailSetting")
    public ResponseEntity<?> resetAllSettings() {
        resetRetentionSettings();
        resetFetchSettings();
        resetStorageSettings();
        return ResponseEntity.ok(ApiResponse.success(200, "All Email Settings reset to defaults.", null));
    }

    private void updateSettingIfExists(String settingKey, String settingValue) {
        EmailSetting setting = emailSettingRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            emailSettingRepository.save(setting);
        }
    }

    private void logFieldChanges(Long entityId, boolean activeChanged, boolean settingValueChanged,
            Boolean oldActive, Boolean newActive, String oldSettingValue, String newSettingValue) {
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
            auditLogService.logActionSync(AuditLog.builder()
                .userId(userId).username(username)
                .action("Update Email Setting Field").entityType("EmailSetting").entityId(entityId)
                .description("Changed active from " + oldActive + " to " + newActive)
                .status("SUCCESS")
                .oldValues("{\"active\": " + oldActive + "}")
                .newValues("{\"active\": " + newActive + "}")
                .build());
        }

        if (settingValueChanged) {
            auditLogService.logActionSync(AuditLog.builder()
                .userId(userId).username(username)
                .action("Update Email Setting Field").entityType("EmailSetting").entityId(entityId)
                .description("Changed settingValue from \"" + oldSettingValue + "\" to \"" + newSettingValue + "\"")
                .status("SUCCESS")
                .oldValues("{\"settingValue\": \"" + escapeJson(oldSettingValue) + "\"}")
                .newValues("{\"settingValue\": \"" + escapeJson(newSettingValue) + "\"}")
                .build());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "null";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
