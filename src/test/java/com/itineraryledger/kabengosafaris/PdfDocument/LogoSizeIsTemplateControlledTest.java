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
 * How big the logo prints, and how much room it takes up doing it.
 *
 * Both were wrong, one after the other, and neither is visible in the markup:
 *
 *  · A logo file states its own size and the renderer believes it, so a wrapper that plainly said
 *    180px got whatever the file felt like — for a viewBox-only export, the renderer's 300pt default.
 *  · Sizing it with CSS fixed that and introduced the next fault: this renderer never learns a
 *    vector's aspect ratio, so {@code height:auto} left its default 150pt box — a 300px-tall band
 *    around a 72px drawing — as empty space above and below the logo on every letterhead.
 *
 * So the page is rasterised and measured: how wide the ink is, and how much taller the element's box
 * is than the ink inside it. A red rule immediately after the logo marks where the flow resumed.
 */
class LogoSizeIsTemplateControlledTest {

    /** a wide lockup carrying only a viewBox, as a design tool exports one */
    private static final String VIEWBOX_ONLY =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1646.94 872.56\">"
            + "<rect width=\"1646.94\" height=\"872.56\" fill=\"#014225\"/></svg>";

    /** the same lockup with its own dimensions baked in */
    private static final String WITH_ATTRIBUTES =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1646.94 872.56\" width=\"420\" height=\"222\">"
            + "<rect width=\"1646.94\" height=\"872.56\" fill=\"#014225\"/></svg>";

    private record Measured(int inkWidth, int inkHeight, int flowHeight) {
        /** space the element occupied beyond the logo drawn in it */
        int wasted() {
            return flowHeight - inkHeight;
        }
    }

    private Measured render(String logoMarkup, String extraCss) throws Exception {
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><style>"
            + "@page{size:A4;margin:0} body{margin:0} " + extraCss
            + "</style></head><body>"
            + "<div class=\"letterhead\">" + logoMarkup + "</div>"
            /* the flow resumes here, so where this lands is the element's real height */
            + "<div style=\"height:4px;background:#ff0000\"></div>"
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
            int inkTop = -1, inkBottom = -1, ruleTop = -1, inkWidth = 0;
            for (int y = 0; y < page.getHeight(); y++) {
                int ink = 0, red = 0, minX = page.getWidth(), maxX = -1;
                for (int x = 0; x < page.getWidth(); x++) {
                    int rgb = page.getRGB(x, y) & 0xFFFFFF;
                    if (rgb == 0xFFFFFF) continue;
                    int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF;
                    if (r > 180 && g < 80) { red++; continue; }
                    ink++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
                if (ink > 0) {
                    if (inkTop < 0) inkTop = y;
                    inkBottom = y;
                    inkWidth = Math.max(inkWidth, maxX - minX + 1);
                }
                if (red > 0 && ruleTop < 0) ruleTop = y;
            }
            return new Measured(inkWidth, inkBottom < 0 ? 0 : inkBottom - inkTop + 1, ruleTop);
        }
    }

    @Test
    @DisplayName("the logo leaves no empty band around itself")
    void theBoxIsTightAroundTheLogo() throws Exception {
        /* the fault, still reproducible: CSS-sized, so the renderer keeps its 150pt default box */
        Measured cssSized = render(VIEWBOX_ONLY.replace("<svg ",
            "<svg style=\"width:100%;height:auto\" "), ".letterhead{width:180px}");
        assertTrue(cssSized.wasted() > 100,
            "height:auto was expected to leave its default box behind, wasted only "
                + cssSized.wasted());

        Measured fixed = render(CompanyIdentityService.sizeableSvg(VIEWBOX_ONLY, 180), "");
        assertTrue(fixed.wasted() <= 2,
            "the element should end where the logo does, but wasted " + fixed.wasted()
                + "px — that band is the empty space above and below the logo on a letterhead");
    }

    @Test
    @DisplayName("it prints at the size asked for, whatever the file said")
    void theRequestedWidthIsTheWidth() throws Exception {
        /* 1 CSS px = 0.75pt at 72dpi, so 180 becomes ~135 */
        assertEquals(135, render(CompanyIdentityService.sizeableSvg(VIEWBOX_ONLY, 180), "").inkWidth(), 2);
        assertEquals(83, render(CompanyIdentityService.sizeableSvg(VIEWBOX_ONLY, 110), "").inkWidth(), 2);

        /* a file that states 420px wide no longer overflows a letterhead */
        assertEquals(135, render(CompanyIdentityService.sizeableSvg(WITH_ATTRIBUTES, 180), "").inkWidth(), 2);
    }

    @Test
    @DisplayName("a template's own CSS rule still overrides the size")
    void aTemplateCanOverrideIt() throws Exception {
        /*
         * This is why the size is in ATTRIBUTES rather than an inline style: a stylesheet beats an
         * attribute and loses to an inline style, so putting it inline would have made the size
         * impossible to change from the template — the opposite of what was asked for.
         */
        Measured overridden = render(CompanyIdentityService.sizeableSvg(VIEWBOX_ONLY, 180),
            ".letterhead svg{width:90px;height:48px}");
        assertEquals(68, overridden.inkWidth(), 3,
            "a template rule of 90px should win over the 180 attribute, drew " + overridden.inkWidth());
        assertTrue(overridden.wasted() <= 2, "and still leave no gap, wasted " + overridden.wasted());
    }

    @Test
    @DisplayName("the aspect ratio survives, even with no viewBox to carry it")
    void aspectRatioSurvives() {
        String noViewBox = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"200\">"
            + "<rect width=\"400\" height=\"200\" fill=\"#014225\"/></svg>";
        String fixed = CompanyIdentityService.sizeableSvg(noViewBox, 180);

        assertTrue(fixed.contains("viewBox=\"0 0 400 200\""),
            "the removed dimensions had to become a viewBox: " + fixed);
        assertTrue(fixed.contains("width=\"180\"") && fixed.contains("height=\"90\""),
            "2:1 at 180 wide is 90 tall: " + fixed);
        assertTrue(fixed.contains("preserveAspectRatio=\"xMinYMin meet\""),
            "a template that overrides only the width must not get a stretched logo: " + fixed);
    }

    @Test
    @DisplayName("a logo with neither viewBox nor dimensions is still drawn, square")
    void nothingToGoOn() {
        String bare = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"5\" cy=\"5\" r=\"5\"/></svg>";
        String fixed = CompanyIdentityService.sizeableSvg(bare, 120);
        assertTrue(fixed.contains("width=\"120\"") && fixed.contains("height=\"120\""),
            "with no ratio to be had, 1:1 beats dividing by zero: " + fixed);
    }
}
