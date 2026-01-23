package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateItineraryDayAccommodationDTO - Data Transfer Object for updating an ItineraryDayAccommodation
 *
 * Only allows updating room count, alternative status, and notes.
 * The core accommodation configuration (accommodation, room type, room standard, board type)
 * cannot be changed - delete and recreate if different configuration is needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayAccommodationDTO {

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private Boolean isAlternative;

    private String notes;
}
