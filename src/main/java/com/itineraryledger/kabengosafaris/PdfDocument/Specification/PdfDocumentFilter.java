package com.itineraryledger.kabengosafaris.PdfDocument.Specification;

import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the PDF document-type list by.
 *
 * The same shape as email events, because it is the same idea: one row per kind of
 * document the system produces — an invoice, a quote, a receipt, a full itinerary — each
 * defined by the code that generates it, each with templates behind it.
 *
 * And the same two silences worth catching. A document type switched off produces nothing;
 * a type switched ON with no enabled template has nothing to produce it FROM, which looks
 * identical from every other column.
 */
@Data
public class PdfDocumentFilter {

    /** Free text across the display name, the system name and the description. */
    private String keyword;
    /** what the older screens called it */
    private String search;

    private String name;
    private Boolean enabled;

    private List<String> statuses;

    /**
     * noTemplates — nothing to render with, so the document cannot be produced at all
     * nothingToRender — a template exists but none is enabled: the same outcome, reached
     *                   by accident rather than on purpose
     */
    private List<String> qualities;

    /** The old `search` param and the house `keyword` mean the same thing. */
    public String effectiveKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword;
        return search;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }

    public boolean hasStatus(String status) {
        return statuses != null && statuses.contains(status);
    }
}
