package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateAccommodationEmailDTO - Request DTO for updating accommodation emails
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccommodationEmailDTO {

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    private EmailType emailType;

    private Boolean isPrimary;

    private Boolean isActive;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
