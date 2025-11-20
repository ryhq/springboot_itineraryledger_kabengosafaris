package com.itineraryledger.kabengosafaris.User.Controllers.LoginController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.User.DTOs.LoginRequest;
import com.itineraryledger.kabengosafaris.User.Handlers.LoginHandler.LoginHandler;

/**
 * REST Controller responsible for handling user Login HTTP endpoints.
 */
@RestController
@RequestMapping("/api/auth/login")
public class LoginController {

    @Autowired
    private LoginHandler loginHandler;

    @PostMapping
    public ResponseEntity<?> loginUser(
        @RequestBody LoginRequest loginRequest
    ) {
        return loginHandler.loginHTTPHandler(loginRequest);
    }
    
}
