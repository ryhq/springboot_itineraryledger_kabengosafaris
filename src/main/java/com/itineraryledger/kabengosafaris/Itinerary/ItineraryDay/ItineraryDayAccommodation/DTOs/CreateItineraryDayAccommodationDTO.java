package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateItineraryDayAccommodationDTO - Data Transfer Object for creating an ItineraryDayAccommodation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayAccommodationDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Obfuscated ID

    @NotBlank(message = "Room Type ID is required")
    private String roomTypeId; // Obfuscated ID

    @NotBlank(message = "Room Standard ID is required")
    private String roomStandardId; // Obfuscated ID

    @NotBlank(message = "Board Type ID is required")
    private String boardTypeId; // Obfuscated ID

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private Boolean isAlternative;
    private String notes;
}
