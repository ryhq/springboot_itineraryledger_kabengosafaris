package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSafariDayFlightDTO {

    @NotBlank(message = "A flight needs a sector")
    private String flightRouteId;

    private String flightFareId;
    private Integer passengerCount;
    private Boolean isAlternative;
    private Boolean isIncludedInPrice;
    private String markupType;
    private BigDecimal markupValue;

    /** PENDING, HELD, TICKETED or CANCELLED. Absent starts at PENDING, which is the honest state. */
    private String bookingStatus;
    private String ticketReference;

    private Integer sortOrder;
    private String notes;
}
