package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SafariDayAccommodationDTO - Data Transfer Object for SafariDayAccommodation entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafariDayAccommodationDTO {
    private String id;
    private String safariDayId;
    private String accommodationId;
    private String accommodationName;
    private String accommodationSlug;
    private String roomTypeId;
    private String roomTypeName;
    private String roomStandardId;
    private String roomStandardName;
    private String boardTypeId;
    private String boardTypeName;
    private Integer roomCount;
    private Boolean isAlternative;
    private String notes;

    // Safari-specific fields
    private String confirmationNumber;
    private LocalDateTime confirmedAt;
    private String checkInTime;
    private String checkOutTime;
    private String roomNumbers;
    private String guestFeedback;
    private String specialArrangements;
    private String bookingStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
