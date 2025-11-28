package com.itineraryledger.kabengosafaris.User.Controllers.LoginController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.MFAVerifyRequest;
import com.itineraryledger.kabengosafaris.User.Handlers.LoginHandler.LoginHandler;
import com.itineraryledger.kabengosafaris.User.Services.LoginServices.LoginService;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFAServices;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFATempTokenProvider;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller responsible for handling user Login HTTP endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class LoginController {

    @Autowired
    private LoginHandler loginHandler;

    @Autowired
    private LoginService loginService;

    @Autowired
    private MFATempTokenProvider mfaTempTokenProvider;

    @Autowired
    private MFAServices mfaServices;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
        @RequestBody LoginRequest loginRequest
    ) {
        return loginHandler.loginHTTPHandler(loginRequest);
    }
    
    /**
     * Verify MFA code during login (2FA verification)
     */
    @PostMapping("/mfa/verify-login")
    public ResponseEntity<?> verifyLoginMFA(
        @RequestHeader("X-MFA-Temp-Token") String tempToken,
        @RequestBody @Valid MFAVerifyRequest request
    ) {
        try {
            // Validate temp token
            User user = mfaTempTokenProvider.validateMFATempToken(tempToken);

            if (user == null) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Invalid or expired MFA token", "INVALID_MFA_TOKEN")
                );
            }

            // Verify the MFA code
            if (!mfaServices.verifyMFACode(user, request.getCode())) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Invalid MFA code", "INVALID_MFA_CODE")
                );
            }

            LoginResponse loginResponse = loginService.loginResponseMFA(user);

            // User is now verified with MFA
            // The client should now have access to protected resources
            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "MFA verification successful",
                    loginResponse
                )
            );
        } catch (Exception e) {
            log.error("Error verifying login MFA: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "MFA verification failed", "MFA_VERIFICATION_FAILED")
            );
        }
    }
}
