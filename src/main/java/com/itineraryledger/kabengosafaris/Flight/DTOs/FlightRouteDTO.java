package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightRouteDTO {
    private String id;
    private String code;
    private String airlineId;
    private String airlineName;
    private String originAirstripId;
    private String originCode;
    private String originName;
    private String destinationAirstripId;
    private String destinationCode;
    private String destinationName;
    /** "ARS → ZNZ", which is how a route picker and a driver's sheet both want to read it. */
    private String sectorLabel;
    private Boolean isOnRequest;
    private Integer minimumSeats;
    private String remarks;
    private Boolean isActive;
    /** How many published departures this sector has. Zero means it cannot be priced yet. */
    private Long fareCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
