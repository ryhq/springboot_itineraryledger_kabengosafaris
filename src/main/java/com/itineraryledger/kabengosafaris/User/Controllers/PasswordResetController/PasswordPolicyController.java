package com.itineraryledger.kabengosafaris.User.Controllers.PasswordResetController;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;

/**
 * The password rules, for the pages where somebody sets a password without being logged in.
 *
 * The policy lives in the security settings and is enforced by PasswordValidator, which only the
 * server can read — so the reset and activation pages had no way to state the rules and could only
 * submit a password and relay the refusal. That is one round trip per guess, and the person setting
 * a password after clicking a link in their email has the least patience for it.
 *
 * Public on purpose (it sits under /api/auth/**, which is permitAll). It gives away the SHAPE of a
 * password, never a password: length bounds and which character classes are required — the same
 * thing anybody learns by submitting one bad password, and the same thing every sign-up form on the
 * internet prints next to the field.
 */
@RestController
@RequestMapping("/api/auth")
public class PasswordPolicyController {

    @Autowired
    private SecuritySettingsGetterServices securitySettings;

    @GetMapping("/password-policy")
    public ResponseEntity<ApiResponse<?>> passwordPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("minLength", securitySettings.getPasswordMinLength());
        policy.put("maxLength", securitySettings.getPasswordMaxLength());
        policy.put("requireUppercase", securitySettings.getPasswordRequireUppercase());
        policy.put("requireLowercase", securitySettings.getPasswordRequireLowercase());
        policy.put("requireNumbers", securitySettings.getPasswordRequireNumbers());
        policy.put("requireSpecialCharacters", securitySettings.getPasswordRequireSpecialCharacters());
        return ResponseEntity.ok(ApiResponse.success(200, "Password policy retrieved", policy));
    }
}
