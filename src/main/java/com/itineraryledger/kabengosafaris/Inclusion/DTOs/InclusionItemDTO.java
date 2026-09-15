package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InclusionItemDTO {

    private String id;
    private String code;
    private String label;
    private String category;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isSystem;
    private Boolean isStandard;
    private Boolean defaultIncluded;
    private String claimAppliesTo;
    /** "accommodation and activity only" — the sentence LineCategoryScope writes for a document. */
    private String claimAppliesToLabel;
    private String internalNotes;

    /**
     * How many itineraries have a row for this.
     *
     * <p>Itineraries only. A sent quote holds its own copy as text, so it neither counts here nor
     * blocks a delete — which is what lets the catalogue be tidied without touching paperwork that
     * is already with a customer.
     */
    private Long usageCount;

    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
