package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayFlightDTO {
    private String id;
    private String quoteDayId;
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
    /** Where the markup in force came from: this flight, the fare, or the airline's name. */
    private String markupSource;
    private Integer sortOrder;
    private String notes;
    /** One adult seat at the markup in force, so the panel can show a figure without a recalc. */
    private BigDecimal sellingPerAdult;
    private String pricingNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
