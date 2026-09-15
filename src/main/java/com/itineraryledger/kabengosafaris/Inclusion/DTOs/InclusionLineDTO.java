package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line of what a price covers, as a PDF template reads it.
 *
 * <p>Carries the heading as well as the wording, so a template that wants to group by category can,
 * while one that just wants the two lists uses the derived {@code inclusionsList} /
 * {@code exclusionsList} getters on the document instead.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InclusionLineDTO {

    private String label;
    private String category;
    private Boolean isIncluded;

    /** For Thymeleaf, which reads getters — {@code th:if="${line.included}"} beats a null check. */
    public boolean isIncluded() {
        return isIncluded != null && isIncluded;
    }
}
