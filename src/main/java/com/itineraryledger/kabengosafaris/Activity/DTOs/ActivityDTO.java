package com.itineraryledger.kabengosafaris.Activity.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ActivityDTO - Data Transfer Object for Activity entity
 * Contains obfuscated ID for secure data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDTO {
    private String idObfuscated;
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
}
