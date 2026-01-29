package com.itineraryledger.kabengosafaris.User.Controllers.PasswordResetController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.Services.PasswordResetServices.PasswordResetException;
import com.itineraryledger.kabengosafaris.User.Services.PasswordResetServices.PasswordResetService;

/**
 * REST Controller for handling password reset public endpoints.
 *
 * Public endpoints (no authentication required):
 * - POST /api/auth/forgot-password - Request password reset email
 * - POST /api/auth/reset-password - Reset password using token
 */
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Public endpoint to request a password reset email.
     *
     * This endpoint always returns success to prevent email enumeration.
     * The email will only be sent if the email exists, is enabled, and not locked.
     *
     * @param email User's email address
     * @return ResponseEntity with success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestParam String email) {
        try {
            passwordResetService.requestPasswordReset(email);
            return ResponseEntity.ok(
                ApiResponse.success(200,
                    "If an account exists with this email, you will receive a password reset link shortly.",
                    null)
            );
        } catch (PasswordResetException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(400, e.getMessage(), "PASSWORD_RESET_ERROR")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "An unexpected error occurred", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Public endpoint to reset password using the reset token.
     *
     * @param request Password reset request containing token and new password
     * @return ResponseEntity with success or error message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(
                ApiResponse.success(200, "Password reset successfully. You can now log in with your new password.", null)
            );
        } catch (PasswordResetException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(400, e.getMessage(), "PASSWORD_RESET_ERROR")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "An unexpected error occurred during password reset", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Request DTO for password reset.
     */
    public static class PasswordResetRequest {
        private String token;
        private String newPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
