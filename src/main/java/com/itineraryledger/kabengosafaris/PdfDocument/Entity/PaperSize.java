package com.itineraryledger.kabengosafaris.PdfDocument.Entity;

import lombok.Getter;

/**
 * Enum representing standard paper sizes for PDF generation.
 * Dimensions are in millimeters.
 */
@Getter
public enum PaperSize {
    A4("A4", 210, 297),
    A3("A3", 297, 420),
    A5("A5", 148, 210),
    LETTER("Letter", 216, 279),
    LEGAL("Legal", 216, 356),
    TABLOID("Tabloid", 279, 432);

    private final String displayName;
    private final int widthMm;
    private final int heightMm;

    PaperSize(String displayName, int widthMm, int heightMm) {
        this.displayName = displayName;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    /**
     * Get width in points (1 point = 1/72 inch)
     * 1 mm = 2.83465 points
     */
    public float getWidthPoints() {
        return widthMm * 2.83465f;
    }

    /**
     * Get height in points
     */
    public float getHeightPoints() {
        return heightMm * 2.83465f;
    }
}
