package com.itineraryledger.kabengosafaris.Flight.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The airline and the two ends are deliberately absent: those three ARE the route's identity, and
 * changing one would silently repoint every fare and every trip already on it. Delete and recreate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlightRouteDTO {
    private Boolean isOnRequest;
    @Min(1) private Integer minimumSeats;
    private String remarks;
    private Boolean isActive;
}
