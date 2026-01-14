package com.itineraryledger.kabengosafaris.Activity.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning activity data with park-specific notes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityWithNotesDTO {
    private String id;
    private String name;
    private String slug;
    private Boolean hasTariff;
    private Boolean isWebActive;
    private ChargingBasis chargingBasis;
    private String description;
    private String detailedDescription;
    private Integer minimumAge;
    private Integer maximumParticipants;
    private String equipmentRequired;
    private String seasonAvailability;
    private String primaryImage;
    private String tags;
    private String safetyInformation;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Park-specific fields
    private String notes; // Notes about this activity in the specific park
}
