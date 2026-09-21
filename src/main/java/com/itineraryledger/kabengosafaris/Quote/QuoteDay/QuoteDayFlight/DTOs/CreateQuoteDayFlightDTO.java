package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQuoteDayFlightDTO {

    @NotBlank(message = "A flight needs a sector")
    private String flightRouteId;

    /**
     * The chosen departure. Optional: a quote may be built before the office has picked a time,
     * and the day then prices off the cheapest live fare for the sector and says so.
     */
    private String flightFareId;

    /** Null means everybody on the trip. A number is a positioning leg or a seat for a guide. */
    private Integer passengerCount;

    private Boolean isAlternative;
    private Boolean isIncludedInPrice;

    /** PERCENT or AMOUNT. Absent falls through to the fare's markup, then the airline's. */
    private String markupType;
    private BigDecimal markupValue;

    private Integer sortOrder;
    private String notes;
}
