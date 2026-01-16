package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderItineraryDaysDTO - Data Transfer Object for reordering itinerary days
 *
 * Used when the UI performs drag-and-drop reordering of days.
 * The order list should contain ALL day IDs in the new desired order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderItineraryDaysDTO {

    /**
     * List of day reorder items representing the new order.
     * The position in the list determines the new dayNumber (1-indexed).
     * First item becomes Day 1, second becomes Day 2, etc.
     */
    @NotNull(message = "Day order list is required")
    @NotEmpty(message = "Day order list cannot be empty")
    @Valid
    private List<DayOrderItem> dayOrder;

    /**
     * Inner class representing a single day's position in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayOrderItem {

        @NotNull(message = "Day ID is required")
        private String dayId; // Obfuscated day ID

        /**
         * Optional: The expected new day number (for validation/confirmation)
         * If provided, it will be validated against the position in the list
         */
        private Integer expectedDayNumber;
    }
}
