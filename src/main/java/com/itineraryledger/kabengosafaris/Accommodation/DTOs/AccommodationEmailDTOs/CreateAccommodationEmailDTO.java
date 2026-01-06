package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateAccommodationEmailDTO - Request DTO for creating accommodation emails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccommodationEmailDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Encoded accommodation ID

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    @Builder.Default
    private Boolean isPrimary = false;

    @Builder.Default
    private Boolean isActive = true;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
