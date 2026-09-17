package com.itineraryledger.kabengosafaris.Flight.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlightRouteDTO {

    @NotBlank(message = "Airline is required")
    private String airlineId;

    @NotBlank(message = "Origin airstrip is required")
    private String originAirstripId;

    @NotBlank(message = "Destination airstrip is required")
    private String destinationAirstripId;

    /** The price list's "N/A": not scheduled, flown on request. Warns when used, never blocks. */
    private Boolean isOnRequest;

    @Min(value = 1, message = "A minimum seat count below 1 means nothing")
    private Integer minimumSeats;

    private String remarks;
    private Boolean isActive;
}
