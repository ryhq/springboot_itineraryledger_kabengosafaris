package com.itineraryledger.kabengosafaris.User.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.User.DTOs.UpdatePasswordRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdateUserProfileRequest;
import com.itineraryledger.kabengosafaris.User.Services.UserService;
import com.itineraryledger.kabengosafaris.User.Services.UserUpdateServices.UserUpdateServices;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for user-related endpoints
 */
@RestController
@RequestMapping("/api/user/me")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserUpdateServices userUpdateServices;

    @GetMapping
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        return userService.getMe(authentication);
    }

    @PutMapping("/personal-details")
    public ResponseEntity<?> updatePersonalDetails(Authentication authentication, @RequestBody UpdateUserProfileRequest updateRequest) {
        return userUpdateServices.updatePersonalDetails(authentication, updateRequest);
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody UpdatePasswordRequest updateRequest) {
        return userUpdateServices.updatePassword(authentication, updateRequest);
    }
}
