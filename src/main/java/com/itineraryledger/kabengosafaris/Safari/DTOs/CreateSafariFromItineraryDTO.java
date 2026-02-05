package com.itineraryledger.kabengosafaris.Safari.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * CreateSafariFromItineraryDTO - DTO for creating a Safari from an Itinerary template
 *
 * This DTO is used to create a new Safari by copying the structure
 * from an existing Itinerary and assigning actual dates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSafariFromItineraryDTO {

    /**
     * Obfuscated ID of the source itinerary
     */
    @NotBlank(message = "Itinerary ID is required")
    private String itineraryId;

    /**
     * Obfuscated ID of the customer this safari is for
     */
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    /**
     * Start date of the safari
     */
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    /**
     * Optional custom name (defaults to itinerary name if not provided)
     */
    private String name;

    /**
     * Optional custom description
     */
    private String description;

    /**
     * Special requests for this safari booking
     */
    private String specialRequests;

    /**
     * Dietary requirements
     */
    private String dietaryRequirements;

    /**
     * Emergency contact information
     */
    private String emergencyContact;
}
