package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Settings.DocxSettingGetterServices;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WordGenerator - Router that selects between HTML→DOCX engines at runtime.
 *
 * Engines:
 *   - "docx4j"      → {@link Docx4jDocxEngine}      (default, in-JVM, no host deps)
 *   - "libreoffice" → {@link LibreOfficeDocxEngine} (higher fidelity, requires
 *                                                    LibreOffice + jodconverter.local.enabled=true)
 *
 * Selection: reads {@code docx.engine} from {@link DocxSettingGetterServices}
 * on every request — so a runtime edit via PUT /api/docx-settings/{id} takes
 * effect immediately without restart. Falls back to the application.properties
 * value (@Value inside DocxSettingGetterServices) when the DB row is absent or
 * inactive.
 *
 * If the resolved engine is "libreoffice" but the LibreOffice engine bean isn't
 * registered (JODConverter disabled or LibreOffice not installed), routes to
 * docx4j with a WARN log so requests never fail for a config issue.
 *
 * Downstream callers ({@link WordGenerationBaseService} and per-document
 * services) keep calling {@code wordGenerator.generateDocx(html, template)}
 * as before — routing is transparent.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WordGenerator {

    private final Docx4jDocxEngine docx4jEngine;
    private final LibreOfficeDocxEngine libreOfficeEngine;
    private final DocxSettingGetterServices settings;

    @PostConstruct
    void logSelectedEngine() {
        DocxEngine selected = resolveEngine();
        log.info("WordGenerator initialized: docx.engine={}, active engine={} (libreoffice available: {})",
            settings.getDocxEngine(),
            selected.id(),
            libreOfficeEngine.isAvailable());
    }

    /**
     * Generate a .docx from rendered HTML using the engine currently selected
     * via the {@code docx.engine} setting (DB → properties fallback).
     */
    public byte[] generateDocx(String html, PdfTemplate template) {
        return resolveEngine().generateDocx(html, template);
    }

    /**
     * Generate via a specific engine, ignoring the configured default. Used by
     * the {@code ?engine=...} query param for A/B comparison. Falls back to
     * docx4j if the requested engine isn't available.
     */
    public byte[] generateDocx(String html, PdfTemplate template, String engineOverride) {
        return resolveEngine(engineOverride).generateDocx(html, template);
    }

    /** @return the identifier of the engine currently selected via config. */
    public String activeEngineId() {
        return resolveEngine().id();
    }

    /**
     * @return the identifier of the engine that would actually run for the
     *         given request, accounting for fallback-to-docx4j. Use this when
     *         reporting the engine used on a per-request basis so the header
     *         never lies.
     */
    public String activeEngineIdFor(String requested) {
        return resolveEngine(requested).id();
    }

    private DocxEngine resolveEngine() {
        return resolveEngine(null);
    }

    private DocxEngine resolveEngine(String requested) {
        String id = (requested != null && !requested.isBlank())
            ? requested
            : settings.getDocxEngine();
        if (id == null || id.isBlank()) {
            return docx4jEngine;
        }
        id = id.trim().toLowerCase();
        if ("libreoffice".equals(id) || "loffice".equals(id) || "soffice".equals(id)) {
            if (libreOfficeEngine.isAvailable()) {
                return libreOfficeEngine;
            }
            log.warn("Requested DOCX engine 'libreoffice' but the pool isn't running " +
                "(enable jodconverter.local.enabled and POST /api/docx-settings/reload/libreoffice, " +
                "or verify LibreOffice is installed). Falling back to docx4j.");
            return docx4jEngine;
        }
        return docx4jEngine;
    }
}
