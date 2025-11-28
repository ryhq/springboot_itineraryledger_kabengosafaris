package com.itineraryledger.kabengosafaris.User.Handlers.LoginHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginResponse;
import com.itineraryledger.kabengosafaris.User.Services.LoginServices.LoginException;
import com.itineraryledger.kabengosafaris.User.Services.LoginServices.LoginService;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFARequiredException;

@Component
public class LoginHandler {
    @Autowired
    private LoginService loginService;

    public ResponseEntity<ApiResponse<?>> loginHTTPHandler(LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = loginService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success(200, "Login successful", loginResponse));
        } catch (LoginException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(400, e.getMessage(), "LOGIN_ERROR")
            );
        } catch (MFARequiredException e) {
            // Handle MFA required scenario
            return ResponseEntity.status(200).body(
                ApiResponse.redirect(
                    302, 
                    "MFA verification required", 
                    e.getTempToken() 
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "An unexpected error occurred during login attempt", "INTERNAL_SERVER_ERROR")
            );
        }
    }
}
