package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.Body;
import org.docx4j.wml.Document;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.SectPr.PgMar;
import org.docx4j.wml.SectPr.PgSz;
import org.docx4j.wml.STPageOrientation;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * Docx4jDocxEngine - In-JVM HTML → DOCX conversion via docx4j's XHTMLImporter.
 *
 * Default engine — works without any external dependencies. Produces good
 * body fidelity (tables, paragraphs, fonts, colors, inline raster images,
 * simple page breaks). Silently drops CSS print-only features:
 *   - @page margins, @page headers/footers
 *   - position: running(...) running elements
 *   - counter(page) page numbering
 *   - SVG images (Batik isn't wired through)
 *   - Flexbox / Grid layouts (degrade to inline/block flow)
 *
 * For higher visual fidelity, switch to {@link LibreOfficeDocxEngine} via
 * the `docx.engine=libreoffice` property.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Docx4jDocxEngine implements DocxEngine {

    /** 1 mm in DXA (twentieths of a point). 1 mm = 1/25.4 inch * 1440 twips/inch. */
    private static final double MM_TO_DXA = 1440.0 / 25.4;

    private final PdfTemplateValidationService validationService;

    @Override
    public String id() {
        return "docx4j";
    }

    @Override
    public byte[] generateDocx(String html, PdfTemplate template) {
        return generateDocx(
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
     * Package-private escape hatch for callers that want to override template page setup.
     */
    byte[] generateDocx(
        String html,
        PaperSize paperSize,
        Orientation orientation,
        Integer marginTop,
        Integer marginBottom,
        Integer marginLeft,
        Integer marginRight
    ) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // XHTMLImporter expects parseable XHTML — convert named entities to numeric.
            String normalizedHtml = validationService.normalizeHtmlEntities(html);

            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mainPart = wordPackage.getMainDocumentPart();

            applyPageSetup(mainPart, paperSize, orientation,
                marginTop, marginBottom, marginLeft, marginRight);

            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordPackage);
            importer.setHyperlinkStyle("Hyperlink");

            List<Object> converted = importer.convert(normalizedHtml, null);
            mainPart.getContent().addAll(converted);

            wordPackage.save(outputStream);

            log.info("DOCX generated via docx4j: {} {}, margins: {}mm, size: {} bytes",
                paperSize.getDisplayName(),
                orientation.getDisplayName(),
                marginTop,
                outputStream.size());

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("docx4j DOCX generation failed", e);
            throw new RuntimeException("Failed to generate DOCX (docx4j): " + e.getMessage(), e);
        }
    }

    private void applyPageSetup(
        MainDocumentPart mainPart,
        PaperSize paperSize,
        Orientation orientation,
        Integer marginTop,
        Integer marginBottom,
        Integer marginLeft,
        Integer marginRight
    ) throws Exception {
        Document doc = mainPart.getContents();
        Body body = doc.getBody();

        SectPr sectPr = body.getSectPr();
        if (sectPr == null) {
            sectPr = new org.docx4j.wml.ObjectFactory().createSectPr();
            body.setSectPr(sectPr);
        }

        int widthMm = paperSize.getWidthMm();
        int heightMm = paperSize.getHeightMm();
        if (orientation == Orientation.LANDSCAPE) {
            int tmp = widthMm;
            widthMm = heightMm;
            heightMm = tmp;
        }

        PgSz pgSz = new org.docx4j.wml.ObjectFactory().createSectPrPgSz();
        pgSz.setW(BigInteger.valueOf(mmToDxa(widthMm)));
        pgSz.setH(BigInteger.valueOf(mmToDxa(heightMm)));
        pgSz.setOrient(orientation == Orientation.LANDSCAPE
            ? STPageOrientation.LANDSCAPE
            : STPageOrientation.PORTRAIT);
        sectPr.setPgSz(pgSz);

        PgMar pgMar = new org.docx4j.wml.ObjectFactory().createSectPrPgMar();
        pgMar.setTop(BigInteger.valueOf(mmToDxa(safeMargin(marginTop, 20))));
        pgMar.setBottom(BigInteger.valueOf(mmToDxa(safeMargin(marginBottom, 20))));
        pgMar.setLeft(BigInteger.valueOf(mmToDxa(safeMargin(marginLeft, 15))));
        pgMar.setRight(BigInteger.valueOf(mmToDxa(safeMargin(marginRight, 15))));
        pgMar.setHeader(BigInteger.valueOf(mmToDxa(12)));
        pgMar.setFooter(BigInteger.valueOf(mmToDxa(12)));
        pgMar.setGutter(BigInteger.ZERO);
        sectPr.setPgMar(pgMar);
    }

    private static long mmToDxa(int mm) {
        return Math.round(mm * MM_TO_DXA);
    }

    private static int safeMargin(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
