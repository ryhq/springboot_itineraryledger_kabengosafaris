package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.DTOs;

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
public class SafariDayFlightDTO {
    private String id;
    private String safariDayId;
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
    private String markupSource;
    /** PENDING, HELD, TICKETED or CANCELLED. */
    private String bookingStatus;
    private String bookingStatusDisplayName;
    private Boolean isTicketed;
    private String ticketReference;
    private Integer sortOrder;
    private String notes;
    private BigDecimal sellingPerAdult;
    private String pricingNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
