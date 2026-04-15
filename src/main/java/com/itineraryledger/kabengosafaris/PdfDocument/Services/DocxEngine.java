package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;

/**
 * Strategy interface for HTML → DOCX conversion.
 *
 * Two implementations are provided:
 *  - {@link Docx4jDocxEngine} (default, always available): in-JVM conversion via
 *    docx4j's XHTMLImporter. No external services. Good body fidelity (tables,
 *    paragraphs, fonts, inline images). Silently drops @page rules, running
 *    footers, counter(page), and SVG.
 *  - {@link LibreOfficeDocxEngine}: shells out to headless LibreOffice via
 *    JODConverter. Much higher visual fidelity — respects @page margins,
 *    converts SVG, honors CSS more completely. Requires LibreOffice installed
 *    on the host and jodconverter.local.enabled=true.
 *
 * The active engine is selected at runtime via the `docx.engine` property
 * (see {@link WordGenerator}).
 */
public interface DocxEngine {

    /**
     * @return a short identifier ("docx4j", "libreoffice") used for logging
     *         and for matching against the `docx.engine` config value.
     */
    String id();

    /**
     * Convert the given HTML to a .docx byte array, applying page setup from
     * the template (paper size, orientation, margins).
     */
    byte[] generateDocx(String html, PdfTemplate template);
}
