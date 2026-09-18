package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayFlightDTO {
    private String id;
    private String itineraryDayId;
    private String flightRouteId;
    private String flightFareId;
    private String airlineName;
    private String sectorLabel;
    private String etd;
    private String eta;
    private String departureLabel;
    private String displayName;
    private Integer passengerCount;
    private Boolean isAlternative;
    private Boolean isIncludedInPrice;
    private String markupType;
    private BigDecimal markupValue;
    /** Where the markup in force came from: this flight, the fare, or the airline. */
    private String markupSource;
    private Integer sortOrder;
    private String notes;
    /** What one adult costs, all in — fare plus markup plus the per-person tax. */
    private BigDecimal sellingPerAdult;
    private String pricingNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
