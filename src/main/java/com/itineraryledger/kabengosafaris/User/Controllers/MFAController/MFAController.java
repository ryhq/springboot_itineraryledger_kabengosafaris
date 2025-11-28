package com.itineraryledger.kabengosafaris.User.Controllers.MFAController;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.User.DTOs.MFAVerifyRequest;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFAServices;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/mfa")
@Slf4j
public class MFAController {

    @Autowired
    private MFAServices mfaServices;

    /**
     * Step 1: Initiate MFA setup
     * Returns QR code for user to scan
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableMFA(Authentication authentication) {
        return mfaServices.enableMFA(authentication);
    }

    /**
     * Step 2: Confirm MFA with verification code
     * User enters code from authenticator app
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmMFA(
        Authentication authentication,
        @RequestBody @Valid MFAVerifyRequest request
    ) {
        return mfaServices.confirmMFASetup(authentication, request.getCode());
    }

    /**
     * Get MFA status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getMFAStatus(Authentication authentication) {
        return mfaServices.getMFAStatus(authentication);
    }

    /**
     * Regenerate backup codes
     */
    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<?> regenerateBackupCodes(
        Authentication authentication,
        @RequestBody @Valid MFAVerifyRequest request
    ) {
        return mfaServices.regenerateBackupCodes(authentication, request.getCode());
    }

    /**
     * Disable MFA
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableMFA(
        Authentication authentication,
        @RequestBody @Valid MFAVerifyRequest request
    ) {
        return mfaServices.disableMFA(authentication, request.getCode());
    }
}
