package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.JodConverterManager;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CRUD + reset operations for DOCX settings.
 * Mirrors TranslationSettingServices.
 */
@Service
public class DocxSettingServices {

    // =====================================================================
    // Fallback defaults from application.properties (used by reset endpoints)
    // =====================================================================

    @Value("${docx.engine:docx4j}")
    private String docxEngine;

    @Value("${jodconverter.local.enabled:false}")
    private Boolean jodconverterEnabled;

    @Value("${jodconverter.local.office-home:}")
    private String jodconverterOfficeHome;

    @Value("${jodconverter.local.port-numbers:2002}")
    private String jodconverterPortNumbers;

    @Value("${jodconverter.local.max-tasks-per-process:100}")
    private Integer jodconverterMaxTasksPerProcess;

    @Value("${jodconverter.local.task-execution-timeout:120000}")
    private Long jodconverterTaskExecutionTimeout;

    @Value("${jodconverter.local.task-queue-timeout:30000}")
    private Long jodconverterTaskQueueTimeout;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private DocxSettingRepository docxSettingRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JodConverterManager jodConverterManager;

    // =====================================================================
    // DTO mapping
    // =====================================================================

    private DocxSettingDTO toDTO(DocxSetting s) {
        return new DocxSettingDTO(
            idObfuscator.encodeId(s.getId()),
            s.getCategory().getDisplayName(),
            s.getSettingKey(),
            s.getSettingValue(),
            s.getDataType(),
            s.getDescription(),
            s.getActive(),
            s.getIsSystemDefault(),
            s.getCategory(),
            s.getRequiresRestart(),
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }

    private List<DocxSettingDTO> toDTOList(List<DocxSetting> settings) {
        return settings.stream().map(this::toDTO).toList();
    }

    // =====================================================================
    // Read
    // =====================================================================

    public ResponseEntity<?> getAllDocxSettings() {
        List<DocxSetting> settings = docxSettingRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(200, "DOCX Settings retrieved successfully.", toDTOList(settings)));
    }

    // =====================================================================
    // Update
    // =====================================================================

    @AuditLogAnnotation(
        action = "UPDATE_DOCX_SETTING",
        description = "Updating a DOCX setting",
        entityIdParamName = "obfuscatedId",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> updateDocxSetting(String obfuscatedId, UpdateDocxSettingDTO updateDTO) {
        Long id;
        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid DOCX Setting ID provided.", "VALIDATION_ERROR")
            );
        }
        return updateDocxSettingById(id, updateDTO);
    }

    private ResponseEntity<?> updateDocxSettingById(Long id, UpdateDocxSettingDTO updateDTO) {
        Boolean active = updateDTO.getActive();
        String settingValue = updateDTO.getSettingValue();

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

        DocxSetting setting = docxSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "DOCX Setting not found.", "NOT_FOUND")
            );
        }

        // Per-key validation for critical values
        String validationError = validate(setting.getSettingKey(), settingValue);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, validationError, "VALIDATION_ERROR")
            );
        }

        Boolean oldActive = setting.getActive();
        String oldSettingValue = setting.getSettingValue();

        boolean activeChanged = !active.equals(oldActive);
        boolean settingValueChanged = !settingValue.equals(oldSettingValue);

        if (!activeChanged && !settingValueChanged) {
            return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(200, "No changes detected. DOCX Setting remains unchanged.", null)
            );
        }

        setting.setActive(active);
        setting.setSettingValue(settingValue);
        setting = docxSettingRepository.save(setting);

        logFieldChanges(setting.getId(), activeChanged, settingValueChanged, oldActive, active, oldSettingValue, settingValue);

        String message = "DOCX Setting updated successfully. " +
            (activeChanged ? "Active status changed. " : "") +
            (settingValueChanged ? "Setting Value changed. " : "") +
            (Boolean.TRUE.equals(setting.getRequiresRestart()) ? "⚠ This change requires an application restart to take effect." : "");

        return ResponseEntity.ok(ApiResponse.success(200, message.trim(), toDTO(setting)));
    }

    /**
     * Per-key validation rules. Returns null when valid, or an error message.
     */
    private String validate(String key, String value) {
        if (key == null) return null;
        switch (key) {
            case "docx.engine" -> {
                String v = value.trim().toLowerCase();
                if (!v.equals("docx4j") && !v.equals("libreoffice") && !v.equals("loffice") && !v.equals("soffice")) {
                    return "docx.engine must be 'docx4j' or 'libreoffice'.";
                }
            }
            case "jodconverter.local.enabled" -> {
                String v = value.trim().toLowerCase();
                if (!v.equals("true") && !v.equals("false")) {
                    return "jodconverter.local.enabled must be 'true' or 'false'.";
                }
            }
            case "jodconverter.local.port-numbers" -> {
                for (String port : value.split(",")) {
                    try {
                        int p = Integer.parseInt(port.trim());
                        if (p < 1 || p > 65535) return "Port numbers must be between 1 and 65535.";
                    } catch (NumberFormatException e) {
                        return "port-numbers must be a comma-separated list of integers (e.g. '2002' or '2002,2003').";
                    }
                }
            }
            case "jodconverter.local.max-tasks-per-process" -> {
                try {
                    if (Integer.parseInt(value.trim()) < 1) return "max-tasks-per-process must be >= 1.";
                } catch (NumberFormatException e) {
                    return "max-tasks-per-process must be an integer.";
                }
            }
            case "jodconverter.local.task-execution-timeout",
                 "jodconverter.local.task-queue-timeout" -> {
                try {
                    if (Long.parseLong(value.trim()) < 1000) return key + " must be >= 1000 (ms).";
                } catch (NumberFormatException e) {
                    return key + " must be a long integer (milliseconds).";
                }
            }
            default -> {
                // office-home and other string settings: no validation
            }
        }
        return null;
    }

    private void logFieldChanges(
        Long entityId,
        boolean activeChanged, boolean settingValueChanged,
        Boolean oldActive, Boolean newActive,
        String oldSettingValue, String newSettingValue
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
                .userId(userId).username(username)
                .action("Update DOCX Setting Field").entityType("DocxSetting").entityId(entityId)
                .description("Changed active field from " + oldActive + " to " + newActive)
                .status("SUCCESS")
                .oldValues("{\"active\": " + oldActive + "}")
                .newValues("{\"active\": " + newActive + "}")
                .build();
            auditLogService.logActionSync(log);
        }
        if (settingValueChanged) {
            AuditLog log = AuditLog.builder()
                .userId(userId).username(username)
                .action("Update DOCX Setting Field").entityType("DocxSetting").entityId(entityId)
                .description("Changed settingValue field from \"" + oldSettingValue + "\" to \"" + newSettingValue + "\"")
                .status("SUCCESS")
                .oldValues("{\"settingValue\": \"" + escapeJson(oldSettingValue) + "\"}")
                .newValues("{\"settingValue\": \"" + escapeJson(newSettingValue) + "\"}")
                .build();
            auditLogService.logActionSync(log);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "null";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // =====================================================================
    // Reset
    // =====================================================================

    @AuditLogAnnotation(
        action = "RESET_DOCX_ENGINE_SETTINGS",
        description = "Resetting DOCX engine settings to defaults",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> resetEngineSettings() {
        updateSettingIfExists("docx.engine", docxEngine);
        return ResponseEntity.ok(
            ApiResponse.success(200, "DOCX Engine Settings reset to default values successfully.", null)
        );
    }

    @AuditLogAnnotation(
        action = "RESET_DOCX_LIBREOFFICE_SETTINGS",
        description = "Resetting DOCX LibreOffice/JODConverter settings to defaults",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> resetLibreOfficeSettings() {
        updateSettingIfExists("jodconverter.local.enabled", String.valueOf(jodconverterEnabled));
        updateSettingIfExists("jodconverter.local.office-home", jodconverterOfficeHome != null ? jodconverterOfficeHome : "");
        updateSettingIfExists("jodconverter.local.port-numbers", jodconverterPortNumbers);
        updateSettingIfExists("jodconverter.local.max-tasks-per-process", String.valueOf(jodconverterMaxTasksPerProcess));
        updateSettingIfExists("jodconverter.local.task-execution-timeout", String.valueOf(jodconverterTaskExecutionTimeout));
        updateSettingIfExists("jodconverter.local.task-queue-timeout", String.valueOf(jodconverterTaskQueueTimeout));

        return ResponseEntity.ok(
            ApiResponse.success(200, "DOCX LibreOffice Settings reset to default values successfully. A restart is required for LibreOffice setting changes to take effect.", null)
        );
    }

    @AuditLogAnnotation(
        action = "RESET_ALL_DOCX_SETTINGS",
        description = "Resetting all DOCX settings to defaults",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> resetAllSettings() {
        resetEngineSettings();
        resetLibreOfficeSettings();
        return ResponseEntity.ok(
            ApiResponse.success(200, "All DOCX Settings reset to default values successfully.", null)
        );
    }

    private void updateSettingIfExists(String settingKey, String settingValue) {
        DocxSetting setting = docxSettingRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            docxSettingRepository.save(setting);
        }
    }

    // =====================================================================
    // LibreOffice lifecycle — reload/stop/status
    //
    // Lets admins apply jodconverter.local.* changes without bouncing Spring Boot.
    // The pool runs under a ReadWriteLock: reload waits for in-flight conversions
    // to finish, stops the old pool, and starts a new one from current DB values.
    // =====================================================================

    /** Current LibreOffice pool status (no mutation). */
    public ResponseEntity<?> getLibreOfficeStatus() {
        JodConverterManager.ReloadResult snapshot = jodConverterManager.snapshot(null);
        return ResponseEntity.ok(
            ApiResponse.success(200, "LibreOffice pool status retrieved.", snapshot)
        );
    }

    /**
     * Reload the LibreOffice pool from current DB settings. Stops any running
     * pool, re-reads jodconverter.local.* values, and starts a fresh pool.
     * If jodconverter.local.enabled is false in DB, leaves the pool stopped.
     */
    @AuditLogAnnotation(
        action = "RELOAD_LIBREOFFICE_POOL",
        description = "Reloading LibreOffice (JODConverter) pool from current settings",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> reloadLibreOfficePool() {
        JodConverterManager.ReloadResult result = jodConverterManager.reload();
        HttpStatus httpStatus = result.status() == JodConverterManager.Status.FAILED
            ? HttpStatus.INTERNAL_SERVER_ERROR
            : HttpStatus.OK;
        String message = result.message() != null ? result.message() : "LibreOffice pool reloaded.";
        if (httpStatus == HttpStatus.OK) {
            return ResponseEntity.ok(ApiResponse.success(200, message, result));
        }
        return ResponseEntity.status(httpStatus).body(
            ApiResponse.error(500, message, "LIBREOFFICE_RELOAD_FAILED")
        );
    }

    /**
     * Stop the LibreOffice pool without restarting it. Useful to free soffice
     * processes when DOCX generation isn't needed for a while. DOCX requests
     * will transparently fall back to docx4j while stopped.
     */
    @AuditLogAnnotation(
        action = "STOP_LIBREOFFICE_POOL",
        description = "Stopping LibreOffice (JODConverter) pool",
        entityType = "DocxSetting"
    )
    public ResponseEntity<?> stopLibreOfficePool() {
        JodConverterManager.ReloadResult result = jodConverterManager.stop();
        return ResponseEntity.ok(
            ApiResponse.success(200, result.message(), result)
        );
    }
}
