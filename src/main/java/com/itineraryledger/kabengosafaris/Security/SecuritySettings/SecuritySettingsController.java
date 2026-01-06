package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/security-settings")
@Validated
public class SecuritySettingsController {

    @Autowired
    private SecuritySettingsServices securitySettingsServices;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SECURITY_SETTING')")
    public ResponseEntity<?> getAllSecuritySettings() {
        return securitySettingsServices.getAllSecuritySettings();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
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
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> reloadIdObfuscationConfigurations() {
        return securitySettingsServices.reloadIdObfuscationConfigurations();
    }

    @GetMapping("/test/id-obfuscation")
    @PreAuthorize("hasAuthority('PERM_READ_SECURITY_SETTING')")
    public ResponseEntity<?> testIdObfuscationConfigurations() {
        return securitySettingsServices.testIdObfuscationConfigurations();
    }

    @PostMapping("/reset/id-obfuscation")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetIdObfuscationConfigurations() {
        return securitySettingsServices.resetIdObfuscationConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * PASSWORD POLICY CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */
    @PostMapping("/reset/password-policy")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetPasswordPolicyConfigurations() {
        return securitySettingsServices.resetPasswordPolicyConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * JWT TOKEN CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reload/jwt-token")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> reloadJwtTokenConfigurations() {
        return securitySettingsServices.reloadJwtTokenConfigurations();
    }

    @PostMapping("/reset/jwt-token")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetJwtTokenConfigurations() {
        return securitySettingsServices.resetJwtTokenConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * LOGIN RATE LIMIT CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reset/login-rate-limit")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetLoginRateLimitConfigurations() {
        return securitySettingsServices.resetLoginRateLimitConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * ACCOUNT LOCKOUT POLICY CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reset/account-lockout")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetAccountLockoutPolicyConfigurations() {
        return securitySettingsServices.resetAccountLockoutPolicyConfigurations();
    }

    /**
     * ═════════════════════════════════════════════════════════════════════════
     * GENERAL CONFIGURATION ENDPOINTS
     * ═════════════════════════════════════════════════════════════════════════
     */

    @PostMapping("/reload/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> reloadAllConfigurations() {
        return securitySettingsServices.reloadAllConfigurations();
    }

    @PostMapping("/reset/all")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SECURITY_SETTING')")
    public ResponseEntity<?> resetAllConfigurations() {
        return securitySettingsServices.resetAllConfigurations();
    }
}
