package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A new line for the catalogue.
 *
 * No displayOrder: a new item goes to the end, and where it belongs is decided by dragging — a
 * running order is a sequence, so one row cannot own it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInclusionItemDTO {

    @NotBlank(message = "Label is required")
    @Size(max = 300, message = "Label must be at most 300 characters")
    private String label;

    @Size(max = 120, message = "Category must be at most 120 characters")
    private String category;

    private Boolean isActive;
    private Boolean isStandard;
    private Boolean defaultIncluded;

    /**
     * Comma-separated line categories this sentence claims to cover, or null for "claims nothing".
     *
     * Leave it null unless the claim maps one-to-one onto a line type. A scope that fires on every
     * quote is a warning everybody learns to ignore.
     */
    @Size(max = 200, message = "Claim scope must be at most 200 characters")
    private String claimAppliesTo;

    private String internalNotes;
}
