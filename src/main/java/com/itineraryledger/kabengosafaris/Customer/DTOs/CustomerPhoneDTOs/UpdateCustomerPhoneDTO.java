package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateCustomerPhoneDTO - Request DTO for updating customer phones
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerPhoneDTO {

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    private PhoneType phoneType;

    private Boolean isPrimary;

    private Boolean isWhatsApp;

    private Boolean isActive;

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;
}
