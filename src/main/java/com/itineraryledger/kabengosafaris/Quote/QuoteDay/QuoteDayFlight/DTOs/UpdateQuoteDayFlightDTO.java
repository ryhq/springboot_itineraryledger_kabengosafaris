package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.DTOs;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Null means "leave it alone", which is the house patch convention.
 *
 * <p>Two fields are cleared with a BLANK string rather than null, because null already means skip:
 * {@code flightFareId} to unpick a departure, {@code markupType} to fall back to the fare's or the
 * airline's markup.
 */
@Data
public class UpdateQuoteDayFlightDTO {
    private String flightRouteId;
    private String flightFareId;
    private Integer passengerCount;
    private Boolean isAlternative;
    private Boolean isIncludedInPrice;
    private String markupType;
    private BigDecimal markupValue;
    private Integer sortOrder;
    private String notes;
}
