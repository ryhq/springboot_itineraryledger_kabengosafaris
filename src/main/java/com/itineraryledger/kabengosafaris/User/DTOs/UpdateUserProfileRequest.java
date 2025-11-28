package com.itineraryledger.kabengosafaris.User.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile information (first name, last name, phone number)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    private String firstName;    // Required
    private String lastName;     // Required
    private String phoneNumber;  // Optional
}
