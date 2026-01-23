package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItineraryDayAccommodationDTO - Data Transfer Object for ItineraryDayAccommodation entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayAccommodationDTO {
    private String id;
    private String itineraryDayId;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
