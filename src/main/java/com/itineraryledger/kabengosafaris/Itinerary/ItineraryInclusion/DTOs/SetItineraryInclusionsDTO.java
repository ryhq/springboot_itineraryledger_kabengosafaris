package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The whole list, in order, replacing whatever was there.
 *
 * <p>Not one row at a time. Position in the array IS the print order, so sending a single row's
 * new number would leave every other row disagreeing with what the person just did — the same
 * reason the FAQ and the inclusion catalogue take their running order as a whole.
 *
 * <p>An empty list is a legitimate instruction: it means this trip says nothing about what its
 * price covers. It is not the same as never having been set, which is what the legacy fallback is
 * for.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetItineraryInclusionsDTO {

    @NotNull(message = "Items list is required — send an empty list to clear it")
    private List<Row> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        /** Obfuscated id of the catalogue item. */
        private String inclusionItemId;
        /** True prints it under "included", false under "not included". */
        private Boolean isIncluded;
    }
}
