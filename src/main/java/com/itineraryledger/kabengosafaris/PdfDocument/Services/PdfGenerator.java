package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PdfGenerator - Converts rendered HTML to PDF using OpenHTMLToPDF
 *
 * Features:
 * - Modern CSS support (Flexbox, CSS3)
 * - Native SVG rendering via Batik
 * - Better font handling
 * - HTML5 support (not strict XHTML required)
 *
 * Responsibilities:
 * - Convert HTML string to PDF bytes
 * - Apply page settings (size, orientation, margins)
 * - Handle fonts, CSS, and SVG
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerator {

    private final PdfTemplateValidationService validationService;

    /**
     * Generate PDF from rendered HTML using template settings
     *
     * @param html The rendered HTML content
     * @param template The PDF template with page settings
     * @return PDF as byte array
     */
    public byte[] generatePdf(String html, PdfTemplate template) {
        return generatePdf(
            html,
            template.getPaperSize(),
            template.getOrientation(),
            template.getMarginTop(),
            template.getMarginBottom(),
            template.getMarginLeft(),
            template.getMarginRight()
        );
    }

    /**
     * Generate PDF from rendered HTML with custom settings
     *
     * @param html The rendered HTML content
     * @param paperSize Paper size enum
     * @param orientation Page orientation
     * @param marginTop Top margin in mm
     * @param marginBottom Bottom margin in mm
     * @param marginLeft Left margin in mm
     * @param marginRight Right margin in mm
     * @return PDF as byte array
     */
    public byte[] generatePdf(
        String html,
        PaperSize paperSize,
        Orientation orientation,
        Integer marginTop,
        Integer marginBottom,
        Integer marginLeft,
        Integer marginRight
    ) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Inject CSS @page rules for page size and margins
            String htmlWithPageRules = injectPageRules(
                html, paperSize, orientation, marginTop, marginBottom, marginLeft, marginRight
            );

            // Normalize HTML entities (convert named entities to numeric)
            String normalizedHtml = validationService.normalizeHtmlEntities(htmlWithPageRules);

            // Create OpenHTMLToPDF renderer
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Enable SVG support via Batik
            builder.useSVGDrawer(new BatikSVGDrawer());

            // Use fast mode for better performance
            builder.useFastMode();

            // Set the page size based on orientation
            BaseRendererBuilder.PageSizeUnits units = BaseRendererBuilder.PageSizeUnits.MM;
            float width = paperSize.getWidthMm();
            float height = paperSize.getHeightMm();

            if (orientation == Orientation.LANDSCAPE) {
                float temp = width;
                width = height;
                height = temp;
            }

            builder.useDefaultPageSize(width, height, units);

            // Set HTML content with base URI for relative resources
            builder.withHtmlContent(normalizedHtml, null);

            // Output to byte array
            builder.toStream(outputStream);

            // Build the PDF
            builder.run();

            log.info("PDF generated successfully: {} {}, margins: {}mm, size: {} bytes",
                paperSize.getDisplayName(),
                orientation.getDisplayName(),
                marginTop,
                outputStream.size());

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("PDF generation failed with unexpected error", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inject @page CSS rules into HTML for controlling page size and margins
     */
    private String injectPageRules(
        String html,
        PaperSize paperSize,
        Orientation orientation,
        Integer marginTop,
        Integer marginBottom,
        Integer marginLeft,
        Integer marginRight
    ) {
        // Calculate dimensions based on orientation
        int width = paperSize.getWidthMm();
        int height = paperSize.getHeightMm();

        if (orientation == Orientation.LANDSCAPE) {
            int temp = width;
            width = height;
            height = temp;
        }

        // Create @page CSS rule with OpenHTMLToPDF specific features
        String pageRule = String.format("""
            <style>
            @page {
                size: %dmm %dmm;
                margin: %dmm %dmm %dmm %dmm;
            }

            /* OpenHTMLToPDF specific: Page break controls */
            .page-break {
                page-break-after: always;
            }
            .avoid-break {
                page-break-inside: avoid;
            }
            </style>
            """,
            width, height,
            marginTop, marginRight, marginBottom, marginLeft
        );

        // Inject into <head> or at the beginning of HTML
        if (html.contains("</head>")) {
            return html.replace("</head>", pageRule + "</head>");
        } else if (html.contains("<body>")) {
            return html.replace("<body>", pageRule + "<body>");
        } else {
            return pageRule + html;
        }
    }
}
