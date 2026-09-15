package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A change to one line. Null means "leave it alone", as everywhere else in this API.
 *
 * displayOrder is absent on purpose: the order is set by dragging the whole list, because typing 7
 * into one row leaves every other row holding a number that now disagrees with it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInclusionItemDTO {

    @Size(max = 300, message = "Label must be at most 300 characters")
    private String label;

    @Size(max = 120, message = "Category must be at most 120 characters")
    private String category;

    private Boolean isActive;
    private Boolean isStandard;
    private Boolean defaultIncluded;

    @Size(max = 200, message = "Claim scope must be at most 200 characters")
    private String claimAppliesTo;

    private String internalNotes;
}
