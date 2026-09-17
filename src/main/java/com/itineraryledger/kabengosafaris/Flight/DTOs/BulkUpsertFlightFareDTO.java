package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One published line of a price list, on its way in.
 *
 * <p>Upserted rather than created because a price list is re-imported: a row already present for the
 * same sector, departure and window is updated, not duplicated. 350 of these arrive in one call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpsertFlightFareDTO {

    @NotBlank(message = "Flight route is required")
    private String flightRouteId;

    /** Null where the airline says TBC — which it does on 65 of Air Excel's 350 rows. */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime etd;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime eta;

    private String departureLabel;

    @NotNull(message = "A fare needs a validity window it is sold in")
    private LocalDate validFrom;

    @NotNull(message = "A fare needs a validity window it is sold in")
    private LocalDate validTo;

    /** "6,7,8,9,10,11". All twelve for year round; null only when no window was stated. */
    private String operatingMonths;

    /** What we pay. Null is allowed and means the fare cannot be quoted yet. */
    private BigDecimal netFare;

    /** What the airline publishes. Reference only. */
    private BigDecimal grossFare;

    private BigDecimal taxesAndFees;
    private String currency;
    private BigDecimal childPercent;
    private Integer minimumSeats;
    private String markupType;
    private BigDecimal markupValue;
    private String remarks;
}
