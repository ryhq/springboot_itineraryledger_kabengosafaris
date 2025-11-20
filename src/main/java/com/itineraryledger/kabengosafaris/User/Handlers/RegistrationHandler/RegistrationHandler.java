package com.itineraryledger.kabengosafaris.User.Handlers.RegistrationHandler;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationException;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Handler for user registration HTTP requests.
 * Delegates registration logic to RegistrationServices.
 */
@Component
public class RegistrationHandler {

    @Autowired
    private RegistrationServices registrationServices;

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
}
