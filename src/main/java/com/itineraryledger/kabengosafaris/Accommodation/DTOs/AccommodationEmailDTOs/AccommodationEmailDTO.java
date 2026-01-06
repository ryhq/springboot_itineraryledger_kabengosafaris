package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AccommodationEmailDTO - Response DTO for accommodation email data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationEmailDTO {

    private String id; // Encoded ID
    private String accommodationId; // Encoded accommodation ID
    private String accommodationName;
    private String email;
    private EmailType emailType;
    private String emailTypeDisplayName;
    private String emailTypeDescription;
    private Boolean isPrimary;
    private Boolean isActive;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
