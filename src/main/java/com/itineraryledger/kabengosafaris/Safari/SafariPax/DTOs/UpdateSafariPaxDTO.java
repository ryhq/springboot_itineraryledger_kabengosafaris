package com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateSafariPaxDTO - DTO for updating SafariPax
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariPaxDTO {

    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;

    @Min(value = 0, message = "Actual count cannot be negative")
    private Integer actualCount;

    private String specialRequirements;
    private String notes;

    /**
     * Whether to confirm this pax entry
     */
    private Boolean confirm;
}
