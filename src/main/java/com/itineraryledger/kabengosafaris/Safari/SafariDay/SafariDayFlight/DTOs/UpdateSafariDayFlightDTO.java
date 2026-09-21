package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.DTOs;

import java.math.BigDecimal;

import lombok.Data;

/** Null means "leave it alone". A blank string clears flightFareId, markupType or ticketReference. */
@Data
public class UpdateSafariDayFlightDTO {
    private String flightRouteId;
    private String flightFareId;
    private Integer passengerCount;
    private Boolean isAlternative;
    private Boolean isIncludedInPrice;
    private String markupType;
    private BigDecimal markupValue;
    private String bookingStatus;
    private String ticketReference;
    private Integer sortOrder;
    private String notes;
}
