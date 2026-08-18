package com.itineraryledger.kabengosafaris.User.Handlers.RegistrationHandler;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.AccountVerificationService;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationException;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Handler for user registration HTTP requests.
 * Delegates registration logic to RegistrationServices and AccountVerificationService.
 */
@Component
public class RegistrationHandler {

    @Autowired
    private RegistrationServices registrationServices;

    @Autowired
    private AccountVerificationService accountVerificationService;

    /**
     * Handle user registration request
     *
     * @param request Registration request containing user details
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> registerUserHTTPHandler(RegistrationRequest request) {
        try {
            registrationServices.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "User registered successfully", null)
            );
        } catch (RegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(400, e.getMessage(), "REGISTRATION_ERROR")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "An unexpected error occurred during registration", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Handle account verification request
     *
     * @param token Activation token from the verification email
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> accountActivation(String token) {
        try {
            com.itineraryledger.kabengosafaris.User.User activated =
                    accountVerificationService.accountActivation(token);
            /*
             * The activated account's own email and first name.
             *
             * Activation only ENABLES the account; it sets no password. An account an
             * administrator created has a generated one nobody ever saw, so the next thing that
             * colleague needs is a password link — and the landing page can only offer that in one
             * click if it knows where to send it. Nothing is disclosed: the caller just proved
             * possession of that account's activation token.
             */
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("email", activated.getEmail());
            data.put("firstName", activated.getFirstName());
            return ResponseEntity.ok(
                    ApiResponse.success(200, "Account verified successfully. You can now log in.", data)
            );
        } catch (RegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(400, e.getMessage(), "VERIFICATION_ERROR")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "An unexpected error occurred during verification", "INTERNAL_SERVER_ERROR")
            );
        }
    }

    /**
     * Handle resend verification email request
     *
     * @param email User's email address
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> resendVerificationEmailHTTPHandler(String email) {
        try {
            accountVerificationService.resendVerificationEmail(email);
            return ResponseEntity.ok(
                    ApiResponse.success(200, "Verification email sent successfully", null)
            );
        } catch (RegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(400, e.getMessage(), "RESEND_VERIFICATION_ERROR")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "An unexpected error occurred while sending verification email", "INTERNAL_SERVER_ERROR")
            );
        }
    }
}
