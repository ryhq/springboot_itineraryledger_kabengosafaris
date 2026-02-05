package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateSafariDayAccommodationDTO - Data Transfer Object for updating a SafariDayAccommodation
 *
 * Allows updating room count, alternative status, notes, and Safari-specific operational fields.
 * The core accommodation configuration (accommodation, room type, room standard, board type)
 * cannot be changed - delete and recreate if different configuration is needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayAccommodationDTO {

    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private Boolean isAlternative;

    private String notes;

    // Safari-specific operational fields
    private String confirmationNumber;
    private String checkInTime;
    private String checkOutTime;
    private String roomNumbers;
    private String guestFeedback;
    private String specialArrangements;
    private String bookingStatus; // PENDING, CONFIRMED, CANCELLED, NO_SHOW, COMPLETED
}
