package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for DOCX Generation Settings.
 * Mirrors {@code TranslationSettingController} — list, update by ID, reset by category.
 */
@RestController
@RequestMapping("/api/docx-settings")
@Validated
public class DocxSettingController {

    @Autowired
    private DocxSettingServices docxSettingServices;

    /**
     * Get all DOCX settings.
     * GET /api/docx-settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_DOCX_SETTING')")
    public ResponseEntity<?> getAllDocxSettings() {
        return docxSettingServices.getAllDocxSettings();
    }

    /**
     * Update a specific DOCX setting by ID.
     * PUT /api/docx-settings/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> updateDocxSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateDocxSettingDTO updateDTO
    ) {
        return docxSettingServices.updateDocxSetting(id, updateDTO);
    }

    /**
     * Reset ENGINE settings (docx.engine) to defaults.
     * POST /api/docx-settings/reset/engine
     */
    @PostMapping("/reset/engine")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> resetEngineSettings() {
        return docxSettingServices.resetEngineSettings();
    }

    /**
     * Reset LibreOffice / JODConverter settings to defaults.
     * POST /api/docx-settings/reset/libreoffice
     */
    @PostMapping("/reset/libreoffice")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> resetLibreOfficeSettings() {
        return docxSettingServices.resetLibreOfficeSettings();
    }

    /**
     * Reset all DOCX settings to defaults.
     * POST /api/docx-settings/reset/all
     */
    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> resetAllSettings() {
        return docxSettingServices.resetAllSettings();
    }

    // =====================================================================
    // LibreOffice pool lifecycle — status, reload, stop
    //
    // Admins can apply jodconverter.local.* edits without restarting the app.
    // Flip the settings via PUT /api/docx-settings/{id}, then POST the reload.
    // =====================================================================

    /**
     * Get the current LibreOffice pool status (running / stopped / failed /
     * starting / stopping) and the config that built the currently active pool.
     * GET /api/docx-settings/libreoffice/status
     */
    @GetMapping("/libreoffice/status")
    @PreAuthorize("hasAuthority('PERM_READ_DOCX_SETTING')")
    public ResponseEntity<?> libreOfficeStatus() {
        return docxSettingServices.getLibreOfficeStatus();
    }

    /**
     * Reload the LibreOffice pool from the current DB settings.
     * POST /api/docx-settings/libreoffice/reload
     */
    @PostMapping("/libreoffice/reload")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> libreOfficeReload() {
        return docxSettingServices.reloadLibreOfficePool();
    }

    /**
     * Stop the LibreOffice pool (free all soffice processes). DOCX requests
     * fall back to docx4j while the pool is stopped.
     * POST /api/docx-settings/libreoffice/stop
     */
    @PostMapping("/libreoffice/stop")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DOCX_SETTING')")
    public ResponseEntity<?> libreOfficeStop() {
        return docxSettingServices.stopLibreOfficePool();
    }
}
