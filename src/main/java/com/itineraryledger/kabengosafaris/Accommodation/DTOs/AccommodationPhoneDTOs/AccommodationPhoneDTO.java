package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AccommodationPhoneDTO - Response DTO for accommodation phones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationPhoneDTO {

    private String id; // Encoded ID
    private String accommodationId; // Encoded accommodation ID
    private String accommodationName;
    private String phoneNumber;
    private String countryCode;
    private PhoneType phoneType;
    private String phoneTypeDisplayName;
    private String phoneTypeDescription;
    private Boolean isPrimary;
    private Boolean isWhatsApp;
    private Boolean isActive;
    private String label;
    private String operatingHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
