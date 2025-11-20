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
@RequestMapping("/api/auth/register")
public class RegistrationController {

    @Autowired
    private RegistrationHandler registrationHandler;

    /**
     * Endpoint to register a new user.
     *
     * @param request RegistrationRequest containing user details
     * @return ResponseEntity with ApiResponse
     */
    @PostMapping
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        return registrationHandler.registerUserHTTPHandler(request);
    }
}
