package com.itineraryledger.kabengosafaris.PdfDocument.Entity;

import lombok.Getter;

/**
 * Enum representing page orientation for PDF generation.
 */
@Getter
public enum Orientation {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape");

    private final String displayName;

    Orientation(String displayName) {
        this.displayName = displayName;
    }
}
