package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One line of what a quote, safari or invoice says its price covers. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentInclusionDTO {

    private String id;

    /** Where it came from. Null once that catalogue row is gone; nothing renders from it. */
    private String inclusionItemId;

    /** The wording as it was when this document was produced. This is what prints. */
    private String label;
    private String category;
    private String claimAppliesTo;
    private Boolean isIncluded;
    private Integer sortOrder;
}
