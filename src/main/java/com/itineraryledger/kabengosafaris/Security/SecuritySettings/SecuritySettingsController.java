package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/security-settings")
@Validated
public class SecuritySettingsController {

    @Autowired
    private SecuritySettingsServices securitySettingsServices;
    
    @GetMapping
    public ResponseEntity<?> getAllSecuritySettings() {
        return securitySettingsServices.getAllSecuritySettings();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSecuritySetting(
        @PathVariable("id") String id,
        @RequestBody SecuritySettingDTO securitySettingDTO
    ) {

        return securitySettingsServices.updateSecuritySetting(id, securitySettingDTO);
    }

    // Reload security settings endpoint
    @PostMapping("/reload-id-obfuscation-configurations")
    public ResponseEntity<?> reloadIdObfuscationConfigurations() {
        return securitySettingsServices.reloadIdObfuscationConfigurations();
    }

    // Reset security settings endpoint
    @PostMapping("/reset-to-defaults")
    public ResponseEntity<?> resetIdObfuscationConfigurations() {
        return securitySettingsServices.resetIdObfuscationConfigurations();
    }

}
