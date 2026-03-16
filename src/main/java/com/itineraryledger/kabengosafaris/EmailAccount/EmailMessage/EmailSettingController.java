package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.UpdateEmailSettingDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailSettingServices;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-settings")
@Validated
@RequiredArgsConstructor
public class EmailSettingController {

    private final EmailSettingServices emailSettingServices;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_SETTING')")
    public ResponseEntity<?> getAllEmailSettings() {
        return emailSettingServices.getAllEmailSettings();
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_SETTING')")
    public ResponseEntity<?> getEmailSettingsByCategory(@PathVariable("category") EmailSetting.Category category) {
        return emailSettingServices.getEmailSettingsByCategory(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_SETTING')")
    public ResponseEntity<?> updateEmailSetting(
            @PathVariable("id") String id,
            @RequestBody UpdateEmailSettingDTO updateDTO) {
        return emailSettingServices.updateEmailSetting(id, updateDTO);
    }

    @PostMapping("/reset/retention")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_SETTING')")
    public ResponseEntity<?> resetRetentionSettings() {
        return emailSettingServices.resetRetentionSettings();
    }

    @PostMapping("/reset/fetch")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_SETTING')")
    public ResponseEntity<?> resetFetchSettings() {
        return emailSettingServices.resetFetchSettings();
    }

    @PostMapping("/reset/storage")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_SETTING')")
    public ResponseEntity<?> resetStorageSettings() {
        return emailSettingServices.resetStorageSettings();
    }

    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_SETTING')")
    public ResponseEntity<?> resetAllSettings() {
        return emailSettingServices.resetAllSettings();
    }
}
