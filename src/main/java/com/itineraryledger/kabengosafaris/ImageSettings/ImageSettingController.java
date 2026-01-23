package com.itineraryledger.kabengosafaris.ImageSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Image Settings Management
 * Provides endpoints to view, update, and reset image upload configurations
 */
@RestController
@RequestMapping("/api/image-settings")
@Validated
public class ImageSettingController {

    @Autowired
    private ImageSettingServices imageSettingServices;

    // =====================================================================
    // GENERAL SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Get all image settings
     * GET /api/image-settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_IMAGE_SETTING')")
    public ResponseEntity<?> getAllImageSettings() {
        return imageSettingServices.getAllImageSettings();
    }

    /**
     * Update a specific image setting by ID
     * PUT /api/image-settings/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_IMAGE_SETTING')")
    public ResponseEntity<?> updateImageSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateImageSettingDTO updateDTO
    ) {
        return imageSettingServices.updateImageSetting(id, updateDTO);
    }

    // =====================================================================
    // UPLOAD SETTINGS ENDPOINTS
    // =====================================================================

    /**
     * Reset image upload settings to defaults
     * POST /api/image-settings/reset/upload
     */
    @PostMapping("/reset/upload")
    @PreAuthorize("hasAuthority('PERM_UPDATE_IMAGE_SETTING')")
    public ResponseEntity<?> resetUploadSettings() {
        return imageSettingServices.resetUploadSettings();
    }

    // =====================================================================
    // GENERAL CONFIGURATION ENDPOINTS
    // =====================================================================

    /**
     * Reset all image settings to defaults
     * POST /api/image-settings/reset/all
     */
    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_IMAGE_SETTING')")
    public ResponseEntity<?> resetAllSettings() {
        return imageSettingServices.resetAllSettings();
    }
}
