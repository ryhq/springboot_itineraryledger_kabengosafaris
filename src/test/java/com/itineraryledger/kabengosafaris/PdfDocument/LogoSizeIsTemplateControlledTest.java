package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

/**
 * That the TEMPLATE decides how big the logo prints.
 *
 * An itinerary came out with a logo four times its intended width, inside a wrapper that plainly
 * said 180px. Two separate reasons, both about the file rather than the layout: a logo carrying
 * width/height attributes prints at those, and a logo carrying only a viewBox falls back to the
 * renderer's own default (300x160pt, measured below). Neither obeys the wrapper, so a template
 * author cannot change the size at all.
 *
 * Measured rather than reasoned about, because "CSS applies to a replaced element" is a claim about
 * this renderer. The page is rasterised and the drawn extent measured in pixels.
 */
class LogoSizeIsTemplateControlledTest {

    /** a logo shaped like a real one: wide lockup, viewBox only, as exported by a design tool */
    private static final String VIEWBOX_ONLY =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1646.94 872.56\">"
            + "<rect width=\"1646.94\" height=\"872.56\" fill=\"#014225\"/></svg>";

    /** the same logo as exported with its own dimensions baked in */
    private static final String WITH_ATTRIBUTES =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1646.94 872.56\" width=\"420\" height=\"222\">"
            + "<rect width=\"1646.94\" height=\"872.56\" fill=\"#014225\"/></svg>";

    /** Width of the drawn (non-white) area on page 1, in pixels at 72dpi. */
    private int drawnWidth(String logoMarkup, int wrapperPx) throws Exception {
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>"
            + "<style>@page{size:A4;margin:20px} body{margin:0}</style></head><body>"
            + "<span style=\"display:inline-block;width:" + wrapperPx + "px\">" + logoMarkup + "</span>"
            + "</body></html>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();

        try (PDDocument doc = PDDocument.load(out.toByteArray())) {
            BufferedImage page = new PDFRenderer(doc).renderImageWithDPI(0, 72);
            int minX = page.getWidth(), maxX = -1;
            for (int y = 0; y < page.getHeight(); y++) {
                for (int x = 0; x < page.getWidth(); x++) {
                    if ((page.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                    }
                }
            }
            return maxX < 0 ? 0 : maxX - minX + 1;
        }
    }

    @Test
    @DisplayName("a viewBox-only logo obeys its wrapper once normalised — it did not before")
    void wrapperGovernsAViewBoxOnlyLogo() throws Exception {
        /* the fault, still reproducible: identical size in a narrow and a wide wrapper */
        assertEquals(drawnWidth(VIEWBOX_ONLY, 180), drawnWidth(VIEWBOX_ONLY, 400),
            "a raw viewBox-only SVG was expected to ignore its wrapper");

        String fixed = CompanyIdentityService.sizeableSvg(VIEWBOX_ONLY);
        int narrow = drawnWidth(fixed, 180);
        int wide = drawnWidth(fixed, 400);

        /* 1pt = 0.75px at 72dpi, so a 180px wrapper draws ~135px; allow a pixel of rounding */
        assertEquals(135, narrow, 2, "a 180px wrapper should draw ~135px, got " + narrow);
        assertEquals(300, wide, 2, "a 400px wrapper should draw ~300px, got " + wide);
    }

    @Test
    @DisplayName("baked-in width and height attributes stop overriding the wrapper")
    void wrapperGovernsALogoThatStatesItsOwnSize() throws Exception {
        int before = drawnWidth(WITH_ATTRIBUTES, 180);
        assertTrue(before > 200,
            "a 420px-wide SVG in a 180px wrapper was expected to overflow it, drew " + before);

        int after = drawnWidth(CompanyIdentityService.sizeableSvg(WITH_ATTRIBUTES), 180);
        assertEquals(135, after, 2, "the wrapper should win after normalising, got " + after);
    }

    @Test
    @DisplayName("the aspect ratio survives, even for a file with no viewBox to keep it")
    void aspectRatioSurvives() {
        String noViewBox = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"200\">"
            + "<rect width=\"400\" height=\"200\" fill=\"#014225\"/></svg>";
        String fixed = CompanyIdentityService.sizeableSvg(noViewBox);
        assertTrue(fixed.contains("viewBox=\"0 0 400 200\""),
            "the removed width/height had to become a viewBox, or nothing carries the ratio: " + fixed);
        assertFalse(fixed.matches("(?s)<svg[^>]*\\swidth=\"400\".*"),
            "the intrinsic width should be gone: " + fixed);
        assertTrue(fixed.contains("width:100%"), "the CSS sizing should be present: " + fixed);
    }

    @Test
    @DisplayName("a style the file already carried is kept, with the sizing appended")
    void existingStyleSurvives() {
        String styled = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\" "
            + "style=\"shape-rendering:geometricPrecision\"><rect width=\"10\" height=\"10\"/></svg>";
        String fixed = CompanyIdentityService.sizeableSvg(styled);
        assertTrue(fixed.contains("shape-rendering:geometricPrecision"),
            "the file's own styling was discarded: " + fixed);
        assertTrue(fixed.indexOf("width:100%") > fixed.indexOf("shape-rendering"),
            "the sizing must come last so it wins: " + fixed);
        assertEquals(1, fixed.split("style=", -1).length - 1,
            "two style attributes is invalid markup: " + fixed);
    }
}
