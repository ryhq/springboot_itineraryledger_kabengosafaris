package com.itineraryledger.kabengosafaris.Translation.Settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Translation Settings Management
 * Provides endpoints to view, update, and reset translation/LibreTranslate configurations
 */
@RestController
@RequestMapping("/api/translation-settings")
@Validated
public class TranslationSettingController {

    @Autowired
    private TranslationSettingServices translationSettingServices;

    // =====================================================================
    // GENERAL SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Get all translation settings
     * GET /api/translation-settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_SETTING')")
    public ResponseEntity<?> getAllTranslationSettings() {
        return translationSettingServices.getAllTranslationSettings();
    }

    /**
     * Update a specific translation setting by ID
     * PUT /api/translation-settings/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> updateTranslationSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateTranslationSettingDTO updateDTO
    ) {
        return translationSettingServices.updateTranslationSetting(id, updateDTO);
    }

    // =====================================================================
    // CONNECTION SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset LibreTranslate connection settings to defaults
     * POST /api/translation-settings/reset/connection
     */
    @PostMapping("/reset/connection")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> resetConnectionSettings() {
        return translationSettingServices.resetConnectionSettings();
    }

    // =====================================================================
    // LANGUAGE SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset language settings to defaults
     * POST /api/translation-settings/reset/languages
     */
    @PostMapping("/reset/languages")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> resetLanguageSettings() {
        return translationSettingServices.resetLanguageSettings();
    }

    // =====================================================================
    // CACHING SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset caching settings to defaults
     * POST /api/translation-settings/reset/caching
     */
    @PostMapping("/reset/caching")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> resetCachingSettings() {
        return translationSettingServices.resetCachingSettings();
    }

    // =====================================================================
    // LIMITS SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset limits settings to defaults
     * POST /api/translation-settings/reset/limits
     */
    @PostMapping("/reset/limits")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> resetLimitsSettings() {
        return translationSettingServices.resetLimitsSettings();
    }

    // =====================================================================
    // GENERAL CONFIGURATION ENDPOINTS
    // =====================================================================

    /**
     * Reset all translation settings to defaults
     * POST /api/translation-settings/reset/all
     */
    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_SETTING')")
    public ResponseEntity<?> resetAllSettings() {
        return translationSettingServices.resetAllSettings();
    }
}
