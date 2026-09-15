package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One place a document's promise and its prices disagree.
 *
 * <p>Severity is a short enum-shaped string so the panel picks a colour without parsing prose, and
 * so the wording can be improved without breaking a screen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InclusionWarningDTO {

    /**
     * CONTRADICTED — the document says one thing and the price does another.
     * UNSUPPORTED — it promises something of a kind the quote has none of.
     * MISSING — the document states a price and says nothing at all about what it covers.
     */
    private String severity;

    private String inclusionLabel;
    private Boolean isIncluded;
    private List<String> categories;

    /** Written for whoever has to act on it, naming the line and what to do. */
    private String message;

    /** Capped at five, because a fourteen-day trip would otherwise return a wall nobody reads. */
    private List<OffendingLine> offendingLines;

    /** The real count, beside the capped list, so the number stays honest. */
    private Integer totalOffending;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffendingLine {
        private Integer dayNumber;
        private String label;
        private String category;
    }
}
