package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItineraryDayParkTariffDTO - Data Transfer Object for ItineraryDayParkTariff entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayParkTariffDTO {
    private String id;
    private String itineraryDayParkId;
    private String parkId;
    private String parkName;
    private String tariffId;
    private String tariffName;
    private String notes;
    private Boolean isIncludedInPrice;
    private LocalDateTime createdAt;
}
