package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderSafariDayParksDTO - Data Transfer Object for reordering park visits within a safari day
 *
 * Used for drag-and-drop reordering of park visits from the UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSafariDayParksDTO {

    @NotNull(message = "Park visit order is required")
    @NotEmpty(message = "Park visit order cannot be empty")
    @Valid
    private List<ParkVisitOrderItem> parkVisitOrder;

    /**
     * Represents a single park visit in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParkVisitOrderItem {
        @NotNull(message = "Park visit ID is required")
        private String parkVisitId;

        /**
         * Optional: Expected sort order for validation
         * If provided, validates the client's expected position matches the actual new position
         */
        private Integer expectedSortOrder;
    }
}
