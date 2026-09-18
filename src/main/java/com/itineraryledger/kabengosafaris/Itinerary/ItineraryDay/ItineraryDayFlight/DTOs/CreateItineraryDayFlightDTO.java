package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayFlightDTO {

    @NotBlank(message = "A flight needs a sector")
    private String flightRouteId;

    /**
     * Optional, and usually left empty at this stage.
     *
     * <p>An itinerary is a product with no dates: the planner chooses the sector and the office
     * chooses the departure when the seats are actually held. Left null, the day prices off the
     * cheapest live fare for that sector and says so.
     */
    private String flightFareId;

    /** Null means everybody on the trip, which is the answer that survives a change of party size. */
    @Min(value = 1, message = "A passenger count below 1 means nothing")
    private Integer passengerCount;

    private Boolean isAlternative;
    private Boolean isIncludedInPrice;

    /** Overrides the fare's markup, which overrides the airline's. PERCENT or AMOUNT. */
    private String markupType;
    private BigDecimal markupValue;

    private Integer sortOrder;
    private String notes;
}
