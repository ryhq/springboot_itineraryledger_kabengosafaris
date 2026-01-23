package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CustomerPhoneDTO - Response DTO for customer phone data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerPhoneDTO {

    private String id; // Encoded ID
    private String customerId; // Encoded customer ID
    private String customerDisplayName;
    private String phoneNumber;
    private String countryCode;
    private PhoneType phoneType;
    private String phoneTypeDisplayName;
    private String phoneTypeDescription;
    private Boolean isPrimary;
    private Boolean isWhatsApp;
    private Boolean isActive;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
