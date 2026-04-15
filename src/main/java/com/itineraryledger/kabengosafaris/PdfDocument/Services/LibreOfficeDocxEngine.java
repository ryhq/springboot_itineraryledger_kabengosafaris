package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * LibreOfficeDocxEngine - HTML → DOCX via headless LibreOffice (JODConverter).
 *
 * Why file-based (instead of stream-based) conversion:
 * LibreOffice's HTML filter is well-behaved when the input arrives as a file
 * with an {@code .html} extension — it picks up the "HTML (StarWriter)" filter
 * and treats the file as a Writer document, so storing as DOCX succeeds. The
 * stream-based API (.convert(InputStream).as(HTML)) trips over format-family
 * resolution in some LibreOffice versions and aborts with
 * "IllegalArgumentException: Unsupported conversion" inside
 * LocalConversionTask.storeDocument. Writing to a temp file sidesteps this
 * entirely — at the cost of two tiny disk writes per request, which is
 * negligible next to the LibreOffice process round-trip.
 *
 * Much higher visual fidelity than docx4j: honors @page margins/size, renders
 * CSS more completely, handles page breaks properly, converts SVG to raster.
 *
 * Always registered as a bean — availability is reported by {@link #isAvailable()}
 * which checks whether {@link JodConverterManager} has a running pool. The
 * pool can be reloaded at runtime via POST /api/docx-settings/libreoffice/reload.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibreOfficeDocxEngine implements DocxEngine {

    private final JodConverterManager manager;
    private final PdfTemplateValidationService validationService;

    @Override
    public String id() {
        return "libreoffice";
    }

    /**
     * @return true iff the LibreOffice pool is currently running. Callers
     *         should check this before using the engine and fall back
     *         gracefully when false.
     */
    public boolean isAvailable() {
        return manager.isAvailable();
    }

    @Override
    public byte[] generateDocx(String html, PdfTemplate template) {
        if (!manager.isAvailable()) {
            throw new IllegalStateException(
                "LibreOffice pool is not running (status=" + manager.getStatus() +
                "). Enable jodconverter.local.enabled in DOCX settings and reload the pool, " +
                "or switch docx.engine=docx4j.");
        }

        Path tempDir = null;
        try {
            // Run the same entity normalization as the docx4j path so outputs are comparable.
            String normalized = validationService.normalizeHtmlEntities(html);
            byte[] htmlBytes = normalized.getBytes(StandardCharsets.UTF_8);

            tempDir = Files.createTempDirectory("kbs-docx-");
            Path htmlFile = tempDir.resolve("input.html");
            Path docxFile = tempDir.resolve("output.docx");

            Files.write(htmlFile, htmlBytes);

            final Path inHtml = htmlFile;
            final Path outDocx = docxFile;

            manager.withConverter(converter -> {
                try {
                    // File-based: JODConverter infers format from the extension.
                    // This uses LibreOffice's "HTML (StarWriter)" input filter and
                    // "MS Word 2007 XML" output filter, both of which are in the
                    // default document-formats registry.
                    converter.convert(inHtml.toFile())
                        .to(outDocx.toFile())
                        .execute();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("LibreOffice conversion failed: " + e.getMessage(), e);
                }
            });

            byte[] bytes = Files.readAllBytes(docxFile);
            log.info("DOCX generated via LibreOffice: {} {}, size: {} bytes",
                template.getPaperSize().getDisplayName(),
                template.getOrientation().getDisplayName(),
                bytes.length);
            return bytes;

        } catch (RuntimeException e) {
            log.error("LibreOffice DOCX conversion failed", e);
            throw e;
        } catch (Exception e) {
            log.error("LibreOffice DOCX conversion failed with unexpected error", e);
            throw new RuntimeException("Failed to generate DOCX (LibreOffice): " + e.getMessage(), e);
        } finally {
            if (tempDir != null) {
                cleanupQuietly(tempDir);
            }
        }
    }

    private static void cleanupQuietly(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best-effort cleanup — OS tmp reaper will mop up
                    }
                });
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
