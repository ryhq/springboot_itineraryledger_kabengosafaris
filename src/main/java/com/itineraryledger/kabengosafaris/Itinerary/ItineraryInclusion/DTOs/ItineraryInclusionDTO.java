package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One line of an itinerary's promise, with enough of the catalogue item to render it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryInclusionDTO {

    private String id;
    private String inclusionItemId;
    private String code;
    private String label;
    private String category;
    private String claimAppliesTo;
    private String claimAppliesToLabel;
    private Boolean isIncluded;
    private Integer sortOrder;

    /**
     * Whether the catalogue item behind this row is still enabled.
     *
     * <p>A row whose item was disabled after it was linked keeps its place and says so, rather than
     * disappearing: the office needs to see that a line it expects to print no longer will.
     */
    private Boolean itemIsActive;
}
