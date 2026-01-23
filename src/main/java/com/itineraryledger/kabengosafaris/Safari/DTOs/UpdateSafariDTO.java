package com.itineraryledger.kabengosafaris.Safari.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * UpdateSafariDTO - DTO for updating Safari fields
 *
 * Note: totalDays and totalNights are inherited from itinerary and cannot be updated.
 * Safari must match the itinerary structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDTO {

    private String name;

    private LocalDate startDate;

    @Min(value = 1, message = "Car count must be at least 1")
    private Integer carCount;

    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;

    private String specialRequests;
    private String dietaryRequirements;
    private String internalNotes;
    private String emergencyContact;
}
