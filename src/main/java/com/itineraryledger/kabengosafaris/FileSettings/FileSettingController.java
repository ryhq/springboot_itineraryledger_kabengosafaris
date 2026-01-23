package com.itineraryledger.kabengosafaris.FileSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for File Settings Management
 * Provides endpoints to view, update, and reset file upload configurations
 */
@RestController
@RequestMapping("/api/file-settings")
@Validated
public class FileSettingController {

    @Autowired
    private FileSettingServices fileSettingServices;

    // =====================================================================
    // GENERAL SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Get all file settings
     * GET /api/file-settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_FILE_SETTING')")
    public ResponseEntity<?> getAllFileSettings() {
        return fileSettingServices.getAllFileSettings();
    }

    /**
     * Get file settings by category
     * GET /api/file-settings/category/{category}
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('PERM_READ_FILE_SETTING')")
    public ResponseEntity<?> getFileSettingsByCategory(@PathVariable("category") FileSetting.Category category) {
        return fileSettingServices.getFileSettingsByCategory(category);
    }

    /**
     * Update a specific file setting by ID
     * PUT /api/file-settings/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> updateFileSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateFileSettingDTO updateDTO
    ) {
        return fileSettingServices.updateFileSetting(id, updateDTO);
    }

    // =====================================================================
    // UPLOAD SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset general file upload settings to defaults
     * POST /api/file-settings/reset/upload
     */
    @PostMapping("/reset/upload")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> resetUploadSettings() {
        return fileSettingServices.resetUploadSettings();
    }

    // =====================================================================
    // EMAIL SIGNATURE SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset email signature file settings to defaults
     * POST /api/file-settings/reset/email-signature
     */
    @PostMapping("/reset/email-signature")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> resetEmailSignatureSettings() {
        return fileSettingServices.resetEmailSignatureSettings();
    }

    // =====================================================================
    // EMAIL TEMPLATE SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset email template file settings to defaults
     * POST /api/file-settings/reset/email-template
     */
    @PostMapping("/reset/email-template")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> resetEmailTemplateSettings() {
        return fileSettingServices.resetEmailTemplateSettings();
    }

    // =====================================================================
    // PDF TEMPLATE SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset PDF template file settings to defaults
     * POST /api/file-settings/reset/pdf-template
     */
    @PostMapping("/reset/pdf-template")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> resetPdfTemplateSettings() {
        return fileSettingServices.resetPdfTemplateSettings();
    }

    // =====================================================================
    // GENERAL CONFIGURATION ENDPOINTS
    // =====================================================================

    /**
     * Reset all file settings to defaults
     * POST /api/file-settings/reset/all
     */
    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FILE_SETTING')")
    public ResponseEntity<?> resetAllSettings() {
        return fileSettingServices.resetAllSettings();
    }
}
