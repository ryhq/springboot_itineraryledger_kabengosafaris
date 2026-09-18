package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Null means "leave alone". Send an empty markupType to clear this flight's override. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayFlightDTO {
    private String flightRouteId;
    private String flightFareId;
    @Min(1) private Integer passengerCount;
    private Boolean isAlternative;
    private Boolean isIncludedInPrice;
    private String markupType;
    private BigDecimal markupValue;
    private Integer sortOrder;
    private String notes;
}
