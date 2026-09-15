package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The whole list, in order, replacing whatever the document held.
 *
 * <p>Wording rather than catalogue ids, unlike the itinerary's endpoint. A document's promise is
 * frozen text by design — that is what stops a catalogue edit rewriting a quote already with a
 * customer — so editing one here is editing the text, and a line that exists nowhere in the
 * catalogue is perfectly legitimate: "the Seronera to Zanzibar flight, quoted separately" belongs
 * to one quote and to nothing else.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetDocumentInclusionsDTO {

    @NotNull(message = "Items list is required — send an empty list to say nothing at all")
    private List<Row> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        /** The sentence the customer reads. */
        private String label;
        private String category;
        /** Optional LineCategoryScope text, so the accuracy check can still speak to this line. */
        private String claimAppliesTo;
        /** True prints it under "included", false under "not included". */
        private Boolean isIncluded;
    }
}
