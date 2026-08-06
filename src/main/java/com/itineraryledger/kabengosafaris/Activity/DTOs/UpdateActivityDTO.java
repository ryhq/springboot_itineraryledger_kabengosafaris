package com.itineraryledger.kabengosafaris.Activity.DTOs;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateActivityDTO - Data Transfer Object for updating an existing Activity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityDTO {
    private String name;
    private String slug;
    private Boolean hasTariff;
    private Boolean isWebActive;
    /**
     * Sent as a String on purpose: blank CLEARS the charging basis, null leaves it
     * alone. Bound as the enum itself, an empty value made Jackson reject the
     * entire request body, so an activity's charging basis could never be unset.
     */
    private String chargingBasis;
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
}
