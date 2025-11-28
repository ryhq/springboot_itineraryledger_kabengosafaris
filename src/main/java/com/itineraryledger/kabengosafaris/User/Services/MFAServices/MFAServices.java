package com.itineraryledger.kabengosafaris.User.Services.MFAServices;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.DTOs.MFABackupCodesResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.MFASetupResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.MFAStatusResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.MFAVerifyResponse;
import com.itineraryledger.kabengosafaris.User.Services.UserService;
import com.google.gson.Gson;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MFAServices {

    @Autowired
    private UserService userService;
    
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public MFAServices() {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }
    
    /**
     * Generate a new secret for the user
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * Generate QR code data URI for the authenticator app
     * @throws QrGenerationException 
     */
    public String generateQrCodeImageUri(String secret, String username, String issuer) throws QrGenerationException {
        QrData data = new QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
        
        return qrGenerator.generate(data).toString();
    }
    
    /**
     * Verify the code entered by the user
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
    

    @AuditLogAnnotation(
        action = "MFA_SETUP_INITIATED",
        entityType = "User",
        entityIdParamName = "authentication",
        description = "User initiated MFA setup"
    )
    public ResponseEntity<?> enableMFA(Authentication authentication) {
        try {
            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(
                    401
                ).body(
                    ApiResponse.error(
                        401,
                        "User not authenticated",
                        "AUTHENTICATION_REQUIRED"
                    )
                );
            }

            // Get username from authentication
            String username = authentication.getName();
            log.debug("Fetching user details for username: {}", username);

            // Retrieve user from database
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "User not found",
                        "USER_NOT_FOUND"
                    )
                );
            }

            if (user.isMfaEnabled() && user.getMfaConfirmed()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "MFA is already enabled for this user",
                        "MFA_ALREADY_ENABLED"
                    )
                );
            }

            // Generate secret
            String secret = generateSecret();
            user.setMfaSecret(secret);
            user.setMfaConfirmed(false);

            // Save updated user
            userService.saveUser(user);

            // Generate QR code URI
            String qrCodeUri = generateQrCodeImageUri(
                secret,
                user.getUsername(),
                "KabengoSafaris"
            );

            String totpUrl = "otpauth://totp/" +
                URLEncoder.encode("KabengoSafaris", StandardCharsets.UTF_8) +
                ":" + 
                URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8) +
                "?secret=" + secret +
                "&issuer=" + URLEncoder.encode("KabengoSafaris", StandardCharsets.UTF_8) +
                "&digits=6&period=30&algorithm=SHA1";

            // Create response with QR code and secret
            MFASetupResponse response = new MFASetupResponse();
            response.setQrCodeUri(qrCodeUri);
            response.setSecret(secret);
            response.setTotpUrl(totpUrl);
            response.setMessage("Scan this QR code with your authenticator app or enter the secret manually");

            // Return success response with QR code URI
            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "MFA setup initiated",
                    response
                )
            );
        } catch (QrGenerationException e) {
            log.error("QR Generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(
                    500,
                    "Failed to generate QR code for MFA setup",
                    "QR_GENERATION_FAILED"
                )
            );
        }
         catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(
                    500,
                    "An error occurred while enabling MFA: " + e.getMessage(),
                    "MFA_ENABLE_ERROR"
                )
            );
        }
    }

    /**
     * Generate 10 backup codes for account recovery
     * Format: XXXX-XXXX (8 alphanumeric characters with dash)
     */
    public List<String> generateBackupCodes() {
        List<String> backupCodes = new ArrayList<>();
        Random random = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < 10; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            code.append("-");
            for (int j = 0; j < 4; j++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            backupCodes.add(code.toString());
        }

        return backupCodes;
    }

    /**
     * Verify if provided code is a valid backup code
     */
    public boolean isValidBackupCode(User user, String code) {
        if (user.getMfaBackupCodes() == null || user.getMfaBackupCodes().isEmpty()) {
            return false;
        }

        try {
            Gson gson = new Gson();
            BackupCodesData data = gson.fromJson(user.getMfaBackupCodes(), BackupCodesData.class);

            if (data == null || data.codes == null) {
                return false;
            }

            for (BackupCodeEntry entry : data.codes) {
                if (entry.code.equals(code) && !entry.used) {
                    entry.used = true;
                    user.setMfaBackupCodes(gson.toJson(data));
                    userService.saveUser(user);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error validating backup code: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Store backup codes in database (JSON format with usage tracking)
     */
    public void saveBackupCodes(User user, List<String> codes) {
        try {
            Gson gson = new Gson();
            BackupCodesData data = new BackupCodesData();
            data.codes = new ArrayList<>();

            for (String code : codes) {
                BackupCodeEntry entry = new BackupCodeEntry();
                entry.code = code;
                entry.used = false;
                data.codes.add(entry);
            }

            user.setMfaBackupCodes(gson.toJson(data));
            userService.saveUser(user);
        } catch (Exception e) {
            log.error("Error saving backup codes: {}", e.getMessage());
        }
    }

    /**
     * Verify and confirm MFA setup
     * Called after user enters code from authenticator
     */
    @AuditLogAnnotation(
        action = "MFA_SETUP_CONFIRMED",
        entityType = "User",
        entityIdParamName = "authentication",
        description = "User confirmed MFA setup with valid code"
    )
    public ResponseEntity<?> confirmMFASetup(Authentication authentication, String code) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "User not authenticated", "AUTHENTICATION_REQUIRED")
                );
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND")
                );
            }

            if (user.getMfaSecret() == null) {
                // Prepare redirect response to /enable endpoint
                Map<String, String> redirectData = new HashMap<>();
                redirectData.put("redirectUrl", "/mfa/enable");

                return ResponseEntity.status(HttpStatus.FOUND).body(
                        ApiResponse.redirect(
                            302,
                            "MFA setup not initiated. Redirecting to enable MFA.",
                            redirectData
                        )
                    );
            }

            // Verify the TOTP code
            if (!verifyCode(user.getMfaSecret(), code)) {
                return ResponseEntity.status(400).body(
                    ApiResponse.error(400, "Invalid MFA code. Please try again.", "INVALID_MFA_CODE")
                );
            }

            // Generate backup codes
            List<String> backupCodes = generateBackupCodes();
            saveBackupCodes(user, backupCodes);

            // Mark MFA as enabled and confirmed
            user.setMfaEnabled(true);
            user.setMfaConfirmed(true);
            user.setMfaEnabledAt(LocalDateTime.now());
            userService.saveUser(user);

            // Return backup codes (only time shown)
            MFAVerifyResponse response = new MFAVerifyResponse();
            response.setVerified(true);
            response.setBackupCodes(backupCodes);
            response.setMessage("MFA successfully enabled. Save these backup codes in a safe place.");

            return ResponseEntity.ok(
                ApiResponse.success(200, "MFA confirmed and enabled", response)
            );

        } catch (Exception e) {
            log.error("Error confirming MFA setup: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to confirm MFA", "MFA_CONFIRM_ERROR")
            );
        }
    }

    /**
     * Verify MFA code during login (2FA step)
     * Checks both TOTP and backup codes
     */
    public boolean verifyMFACode(User user, String code) {
        // Check if code is valid TOTP code
        if (verifyCode(user.getMfaSecret(), code)) {
            user.setLastMfaVerification(LocalDateTime.now());
            userService.saveUser(user);
            return true;
        }

        // Check if code is valid backup code
        if (isValidBackupCode(user, code)) {
            user.setLastMfaVerification(LocalDateTime.now());
            userService.saveUser(user);
            return true;
        }

        return false;
    }

    /**
     * Get MFA status for authenticated user
     */
    public ResponseEntity<?> getMFAStatus(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "User not authenticated", "AUTHENTICATION_REQUIRED")
                );
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND")
                );
            }

            MFAStatusResponse response = new MFAStatusResponse();
            response.setMfaEnabled(user.isMfaEnabled() && user.getMfaConfirmed());
            response.setEnabledAt(user.getMfaEnabledAt());
            response.setLastVerifiedAt(user.getLastMfaVerification());

            return ResponseEntity.ok(
                ApiResponse.success(200, "MFA status retrieved", response)
            );

        } catch (Exception e) {
            log.error("Error getting MFA status: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to get MFA status", "MFA_STATUS_ERROR")
            );
        }
    }

    /**
     * Regenerate backup codes
     * User must provide current MFA code to regenerate
     */
    @AuditLogAnnotation(
        action = "MFA_BACKUP_CODES_REGENERATED",
        entityType = "User",
        entityIdParamName = "authentication",
        description = "User regenerated MFA backup codes"
    )
    public ResponseEntity<?> regenerateBackupCodes(Authentication authentication, String code) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "User not authenticated", "AUTHENTICATION_REQUIRED")
                );
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND")
                );
            }

            if (!user.isMfaEnabled() || !user.getMfaConfirmed()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "MFA is not enabled", "MFA_NOT_ENABLED")
                );
            }

            // Verify the provided code (TOTP or backup code)
            if (!verifyMFACode(user, code)) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Invalid MFA code", "INVALID_MFA_CODE")
                );
            }

            // Generate new backup codes
            List<String> newBackupCodes = generateBackupCodes();
            saveBackupCodes(user, newBackupCodes);

            MFABackupCodesResponse response = new MFABackupCodesResponse();
            response.setBackupCodes(newBackupCodes);
            response.setMessage("New backup codes generated successfully. Save them in a safe place.");

            return ResponseEntity.ok(
                ApiResponse.success(200, "Backup codes regenerated", response)
            );

        } catch (Exception e) {
            log.error("Error regenerating backup codes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to regenerate backup codes", "BACKUP_CODES_REGEN_ERROR")
            );
        }
    }

    /**
     * Disable MFA for authenticated user
     * User must provide current MFA code to disable
     */
    @AuditLogAnnotation(
        action = "MFA_DISABLED",
        entityType = "User",
        entityIdParamName = "authentication",
        description = "User disabled MFA authentication"
    )
    public ResponseEntity<?> disableMFA(Authentication authentication, String code) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "User not authenticated", "AUTHENTICATION_REQUIRED")
                );
            }

            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND")
                );
            }

            if (!user.isMfaEnabled() || !user.getMfaConfirmed()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "MFA is not enabled for this user", "MFA_NOT_ENABLED")
                );
            }

            // Verify the provided code (must be valid TOTP code)
            if (!verifyCode(user.getMfaSecret(), code)) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Invalid MFA code. Cannot disable MFA without valid code.", "INVALID_MFA_CODE")
                );
            }

            // Disable MFA
            user.setMfaEnabled(false);
            user.setMfaConfirmed(false);
            user.setMfaSecret(null);
            user.setMfaBackupCodes(null);
            user.setLastMfaVerification(null);
            user.setMfaEnabledAt(null);
            userService.saveUser(user);

            return ResponseEntity.ok(
                ApiResponse.success(200, "MFA successfully disabled", null)
            );

        } catch (Exception e) {
            log.error("Error disabling MFA: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to disable MFA", "MFA_DISABLE_ERROR")
            );
        }
    }

    /**
     * Helper classes for backup code storage and retrieval
     */
    public static class BackupCodesData {
        public List<BackupCodeEntry> codes;
    }

    public static class BackupCodeEntry {
        public String code;
        public boolean used;
    }
}
