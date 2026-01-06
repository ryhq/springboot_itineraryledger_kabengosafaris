package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateAccommodationPhoneDTO - Request DTO for creating accommodation phones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccommodationPhoneDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Encoded accommodation ID

    @NotBlank(message = "Phone number is required")
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    @NotNull(message = "Phone type is required")
    private PhoneType phoneType;

    @Builder.Default
    private Boolean isPrimary = false;

    @Builder.Default
    private Boolean isWhatsApp = false;

    @Builder.Default
    private Boolean isActive = true;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;

    @Size(max = 200, message = "Operating hours must not exceed 200 characters")
    private String operatingHours;
}
