package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateCustomerPhoneDTO - Request DTO for creating customer phones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerPhoneDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId; // Encoded customer ID

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
}
