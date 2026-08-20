package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

/**
 * Which form of a logo actually survives into a PDF.
 *
 * This matters because the shipped templates each carried 27KB of ONE company's logo as inline SVG
 * path data, and replacing it with the company's own asset only helps if the replacement renders. The
 * choices are inline SVG markup, an <img> holding an SVG data URI, or an <img> holding a raster data
 * URI — and which of those the renderer supports is a fact about this library, not something to
 * assume. So it is measured: a page that draws the mark is bigger than the same page without it.
 */
class LogoInPdfTest {

    /** a tiny but real SVG: a filled square */
    private static final String SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"90\" height=\"90\">"
            + "<rect width=\"100\" height=\"100\" fill=\"#014225\"/></svg>";

    /** a 1×1 PNG, base64 */
    /** a 240×120 PNG — big enough that a page carrying it is measurably bigger */
    private static final String PNG_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAAPAAAAB4CAIAAABD1OhwAAABXElEQVR4nO3SUQkAIBTAwGcYi9g/lCUEYRxcgH1s1tmQMd8L4CFDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphibF0KQYmhRDk2JoUgxNiqFJMTQphiblAlBVtqR7STsrAAAAAElFTkSuQmCC";

    private byte[] render(String body) {
        String html = """
            <!DOCTYPE html><html><head><meta charset="utf-8"/></head><body>%s</body></html>
            """.formatted(body);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        assertDoesNotThrow(builder::run, "the renderer refused the page outright");
        return out.toByteArray();
    }

    @Test
    @DisplayName("inline SVG markup draws, which is what the templates relied on")
    void inlineSvgDraws() {
        int blank = render("<p>x</p>").length;
        int withLogo = render("<p>x</p>" + SVG).length;
        assertTrue(withLogo > blank + 200,
            "inline SVG added only " + (withLogo - blank) + " bytes — it did not draw");
    }

    @Test
    @DisplayName("an <img> holding an SVG data URI draws too")
    void svgDataUriDraws() {
        String dataUri = "data:image/svg+xml;base64,"
            + Base64.getEncoder().encodeToString(SVG.getBytes());
        int blank = render("<p>x</p>").length;
        int withLogo = render("<p>x</p><img src=\"" + dataUri + "\" style=\"width:90px\"/>").length;
        assertTrue(withLogo > blank + 200,
            "an SVG data URI added only " + (withLogo - blank) + " bytes — the renderer ignored it, "
                + "so a template must inline the markup instead");
    }

    @Test
    @DisplayName("an <img> holding a raster data URI draws")
    void rasterDataUriDraws() {
        int blank = render("<p>x</p>").length;
        int withLogo = render("<p>x</p><img src=\"data:image/png;base64," + PNG_BASE64
            + "\" style=\"width:90px\"/>").length;
        assertTrue(withLogo > blank + 100,
            "a raster data URI added only " + (withLogo - blank) + " bytes — it did not draw");
    }
}
