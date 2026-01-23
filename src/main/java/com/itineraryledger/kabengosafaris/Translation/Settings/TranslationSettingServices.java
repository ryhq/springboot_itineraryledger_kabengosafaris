package com.itineraryledger.kabengosafaris.Translation.Settings;

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
 * Service for managing translation settings.
 * Provides CRUD operations and reset functionality for translation configuration.
 */
@Service
public class TranslationSettingServices {

    // =====================================================================
    // Fallback values from application.properties - CONNECTION Settings
    // =====================================================================

    @Value("${translation.libretranslate.enabled:false}")
    private Boolean libretranslateEnabled;

    @Value("${translation.libretranslate.base.url:http://localhost:5000}")
    private String libretranslateBaseUrl;

    @Value("${translation.libretranslate.timeout.seconds:30}")
    private Integer libretranslateTimeoutSeconds;

    @Value("${translation.libretranslate.api.key:}")
    private String libretranslateApiKey;

    // =====================================================================
    // Fallback values from application.properties - LANGUAGES Settings
    // =====================================================================

    @Value("${translation.default.source.language:en}")
    private String defaultSourceLanguage;

    @Value("${translation.default.target.language:fr}")
    private String defaultTargetLanguage;

    @Value("${translation.supported.languages:en,fr,de,es,it,pt,sw}")
    private String supportedLanguages;

    // =====================================================================
    // Fallback values from application.properties - CACHING Settings
    // =====================================================================

    @Value("${translation.cache.enabled:true}")
    private Boolean cacheEnabled;

    @Value("${translation.cache.ttl.hours:168}")
    private Integer cacheTtlHours;

    // =====================================================================
    // Fallback values from application.properties - LIMITS Settings
    // =====================================================================

    @Value("${translation.max.characters:10000}")
    private Integer maxCharacters;

    @Value("${translation.chunk.size:5000}")
    private Integer chunkSize;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private TranslationSettingRepository translationSettingRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Convert entity to DTO
     */
    private TranslationSettingDTO toDTO(TranslationSetting setting) {
        return new TranslationSettingDTO(
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
    private List<TranslationSettingDTO> toDTOList(List<TranslationSetting> settings) {
        return settings.stream().map(this::toDTO).toList();
    }

    /**
     * Get all translation settings
     */
    public ResponseEntity<?> getAllTranslationSettings() {
        List<TranslationSetting> settings = translationSettingRepository.findAll();
        List<TranslationSettingDTO> dtos = toDTOList(settings);
        return ResponseEntity.ok(ApiResponse.success(200, "Translation Settings retrieved successfully.", dtos));
    }

    /**
     * Update a specific translation setting
     */
    @AuditLogAnnotation(
        action = "UPDATE_TRANSLATION_SETTING",
        description = "Updating a translation setting",
        entityIdParamName = "obfuscatedId",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> updateTranslationSetting(String obfuscatedId, UpdateTranslationSettingDTO updateDTO) {
        Long id;

        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Translation Setting ID provided.", "VALIDATION_ERROR")
            );
        }

        return updateTranslationSettingById(id, updateDTO);
    }

    private ResponseEntity<?> updateTranslationSettingById(Long id, UpdateTranslationSettingDTO updateDTO) {
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

        TranslationSetting setting = translationSettingRepository.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Translation Setting not found.", "NOT_FOUND")
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
                ApiResponse.success(200, "No changes detected. Translation Setting remains unchanged.", null)
            );
        }

        setting.setActive(active);
        setting.setSettingValue(settingValue);

        setting = translationSettingRepository.save(setting);

        // Log each individual field change
        logFieldChanges(setting.getId(), activeChanged, settingValueChanged, oldActive, active, oldSettingValue, settingValue);

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Translation Setting updated successfully. " +
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
                .action("Update Translation Setting Field")
                .entityType("TranslationSetting")
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
                .action("Update Translation Setting Field")
                .entityType("TranslationSetting")
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
     * Reset CONNECTION settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_TRANSLATION_CONNECTION_SETTINGS",
        description = "Resetting translation connection settings to defaults",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> resetConnectionSettings() {
        updateSettingIfExists("libretranslate.enabled", String.valueOf(libretranslateEnabled));
        updateSettingIfExists("libretranslate.base.url", libretranslateBaseUrl);
        updateSettingIfExists("libretranslate.timeout.seconds", String.valueOf(libretranslateTimeoutSeconds));
        updateSettingIfExists("libretranslate.api.key", libretranslateApiKey);

        return ResponseEntity.ok(
            ApiResponse.success(200, "Translation Connection Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset LANGUAGES settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_TRANSLATION_LANGUAGE_SETTINGS",
        description = "Resetting translation language settings to defaults",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> resetLanguageSettings() {
        updateSettingIfExists("translation.default.source.language", defaultSourceLanguage);
        updateSettingIfExists("translation.default.target.language", defaultTargetLanguage);
        updateSettingIfExists("translation.supported.languages", supportedLanguages);

        return ResponseEntity.ok(
            ApiResponse.success(200, "Translation Language Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset CACHING settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_TRANSLATION_CACHING_SETTINGS",
        description = "Resetting translation caching settings to defaults",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> resetCachingSettings() {
        updateSettingIfExists("translation.cache.enabled", String.valueOf(cacheEnabled));
        updateSettingIfExists("translation.cache.ttl.hours", String.valueOf(cacheTtlHours));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Translation Caching Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset LIMITS settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_TRANSLATION_LIMITS_SETTINGS",
        description = "Resetting translation limits settings to defaults",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> resetLimitsSettings() {
        updateSettingIfExists("translation.max.characters", String.valueOf(maxCharacters));
        updateSettingIfExists("translation.chunk.size", String.valueOf(chunkSize));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Translation Limits Settings reset to default values successfully.", null)
        );
    }

    /**
     * Reset ALL translation settings to defaults
     */
    @AuditLogAnnotation(
        action = "RESET_ALL_TRANSLATION_SETTINGS",
        description = "Resetting all translation settings to defaults",
        entityType = "TranslationSetting"
    )
    public ResponseEntity<?> resetAllSettings() {
        resetConnectionSettings();
        resetLanguageSettings();
        resetCachingSettings();
        resetLimitsSettings();

        return ResponseEntity.ok(
            ApiResponse.success(200, "All Translation Settings reset to default values successfully.", null)
        );
    }

    /**
     * Helper method to update a setting if it exists in the database
     */
    private void updateSettingIfExists(String settingKey, String settingValue) {
        TranslationSetting setting = translationSettingRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            translationSettingRepository.save(setting);
        }
    }
}
