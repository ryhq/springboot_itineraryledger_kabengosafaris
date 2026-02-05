package com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderSafariDaysDTO - Data Transfer Object for reordering safari days
 *
 * Used when the UI performs drag-and-drop reordering of days.
 * The order list should contain ALL day IDs in the new desired order.
 *
 * IMPORTANT: When safari days are reordered, their actualDate fields will be
 * automatically recalculated based on the safari's startDate and the new dayNumber.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSafariDaysDTO {

    /**
     * List of day reorder items representing the new order.
     * The position in the list determines the new dayNumber (1-indexed).
     * First item becomes Day 1, second becomes Day 2, etc.
     *
     * The actualDate for each day will be recalculated as:
     * safari.startDate + (dayNumber - 1)
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
