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
        @RequestBody UpdateSecuritySettingDTO updateSecuritySettingDTO
    ) {

        return securitySettingsServices.updateSecuritySetting(id, updateSecuritySettingDTO);
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * ID OBFUSCATION CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reload/id-obfuscation")
    public ResponseEntity<?> reloadIdObfuscationConfigurations() {
        return securitySettingsServices.reloadIdObfuscationConfigurations();
    }

    @GetMapping("/test/id-obfuscation")
    public ResponseEntity<?> testIdObfuscationConfigurations() {
        return securitySettingsServices.testIdObfuscationConfigurations();
    }

    @PostMapping("/reset/id-obfuscation")
    public ResponseEntity<?> resetIdObfuscationConfigurations() {
        return securitySettingsServices.resetIdObfuscationConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * PASSWORD POLICY CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */
    @PostMapping("/reset/password-policy")
    public ResponseEntity<?> resetPasswordPolicyConfigurations() {
        return securitySettingsServices.resetPasswordPolicyConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * JWT TOKEN CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reload/jwt-token")
    public ResponseEntity<?> reloadJwtTokenConfigurations() {
        return securitySettingsServices.reloadJwtTokenConfigurations();
    }

    @PostMapping("/reset/jwt-token")
    public ResponseEntity<?> resetJwtTokenConfigurations() {
        return securitySettingsServices.resetJwtTokenConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * LOGIN RATE LIMIT CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reset/login-rate-limit")
    public ResponseEntity<?> resetLoginRateLimitConfigurations() {
        return securitySettingsServices.resetLoginRateLimitConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * ACCOUNT LOCKOUT POLICY CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reset/account-lockout")
    public ResponseEntity<?> resetAccountLockoutPolicyConfigurations() {
        return securitySettingsServices.resetAccountLockoutPolicyConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * GENERAL CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reload/all")
    public ResponseEntity<?> reloadAllConfigurations() {
        return securitySettingsServices.reloadAllConfigurations();
    }

    @PostMapping("/reset/all")
    public ResponseEntity<?> resetAllConfigurations() {
        return securitySettingsServices.resetAllConfigurations();
    }
}
