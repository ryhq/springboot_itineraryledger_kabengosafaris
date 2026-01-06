package com.itineraryledger.kabengosafaris.ParkActivity.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for upserting park-activity relationships
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkActivityUpsertDTO {
    @NotBlank(message = "Activity ID is required")
    private String activityId;

    @NotBlank(message = "Park ID is required")
    private String parkId;

    private String notes;

    @NotNull(message = "Status is required")
    private Boolean status; // true = create/update, false = delete
}
