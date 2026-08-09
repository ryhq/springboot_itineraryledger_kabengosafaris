package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What can change about a night's stay.
 *
 * The room configuration used to be fixed after creation — "delete and recreate
 * if a different configuration is needed" — which meant moving a booking from a
 * Superior Suite to a Bungalow destroyed the row and its notes, and any UI that
 * offered the choice appeared to save and silently did nothing.
 *
 * All four are now updatable. Null still means "leave unchanged", so a request
 * carrying only roomCount behaves exactly as before.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayAccommodationDTO {

    /** Moving the night to a different property; its room lists come with it. */
    private String accommodationId;

    private String roomTypeId;

    private String roomStandardId;

    private String boardTypeId;

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private Boolean isAlternative;

    private String notes;
}
