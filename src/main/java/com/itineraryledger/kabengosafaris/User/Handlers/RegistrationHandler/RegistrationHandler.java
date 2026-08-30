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

    @Autowired
    private com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimiter rateLimiter;

    /**
     * Handle user registration request
     *
     * @param request Registration request containing user details
     * @return ResponseEntity with ApiResponse
     */
    public ResponseEntity<ApiResponse<?>> registerUserHTTPHandler(RegistrationRequest request) {
        /*
         * How many activation emails one address may be sent, whoever is asking.
         *
         * The filter limits the other two mail-senders by recipient, but it cannot do it here: this
         * endpoint carries its email in a JSON BODY, and a servlet filter reading the body consumes
         * the stream the controller then needs. Checked here instead, where the address is already
         * parsed — and only on THIS path, so an administrator creating accounts and the first-run
         * initializer are untouched.
         *
         * Without it the per-IP ceiling is the only guard, and the attack worth stopping is a
         * hundred machines asking this installation to mail the same person.
         */
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (email != null && !email.isBlank()) {
            long waitFor = rateLimiter.check("register-email:" + email, 3, java.time.Duration.ofHours(1));
            if (waitFor > 0) {
                return ResponseEntity.status(429).body(ApiResponse.error(429,
                    "That address has already been sent several activation emails. "
                        + "Check the inbox, or try again in about " + Math.max(1, waitFor / 60)
                        + " minutes.", "RATE_LIMITED"));
            }
        }

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
