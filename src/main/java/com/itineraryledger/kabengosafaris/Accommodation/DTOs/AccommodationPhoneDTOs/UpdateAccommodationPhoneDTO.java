package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateAccommodationPhoneDTO - Request DTO for updating accommodation phones
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccommodationPhoneDTO {

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String phoneType;

    private Boolean isPrimary;

    private Boolean isWhatsApp;

    private Boolean isActive;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;

    @Size(max = 200, message = "Operating hours must not exceed 200 characters")
    private String operatingHours;
}
