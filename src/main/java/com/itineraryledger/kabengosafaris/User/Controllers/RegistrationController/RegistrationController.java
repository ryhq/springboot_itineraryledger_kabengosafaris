package com.itineraryledger.kabengosafaris.User.Controllers.RegistrationController;

import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;
import com.itineraryledger.kabengosafaris.User.Handlers.RegistrationHandler.RegistrationHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller responsible for handling user registration HTTP endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    @Autowired
    private RegistrationHandler registrationHandler;

    /**
     * Endpoint to register a new user.
     *
     * @param request RegistrationRequest containing user details
     * @return ResponseEntity with ApiResponse
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        return registrationHandler.registerUserHTTPHandler(request);
    }

    /**
     * Public endpoint to verify user account using activation token.
     * This endpoint is called when user clicks the activation link in their email.
     *
     * @param token Activation token from the verification email
     * @return ResponseEntity with ApiResponse
     */
    @GetMapping("/account-activation")
    public ResponseEntity<?> accountActivation(@RequestParam String token) {
        return registrationHandler.accountActivation(token);
    }

    /**
     * Public endpoint to resend verification email.
     * Used when user didn't receive the initial verification email or it expired.
     *
     * @param email User's email address
     * @return ResponseEntity with ApiResponse
     */
    @PostMapping("/resend-account-activation")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String email) {
        return registrationHandler.resendVerificationEmailHTTPHandler(email);
    }
}
