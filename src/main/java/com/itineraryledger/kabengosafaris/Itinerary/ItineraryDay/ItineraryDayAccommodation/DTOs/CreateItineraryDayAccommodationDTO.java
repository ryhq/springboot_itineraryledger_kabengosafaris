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

    private String roomTypeId; // Obfuscated ID (optional)
    private String roomStandardId; // Obfuscated ID (optional)
    private String boardTypeId; // Obfuscated ID (optional)

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    @Min(value = 1, message = "Nights must be at least 1")
    private Integer nights;

    private Boolean isAlternative;
    private String notes;
}
