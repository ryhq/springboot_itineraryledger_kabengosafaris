package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.PdfDocumentVariables;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializer for PDF Documents and their System Default Templates.
 * Runs at application startup and initializes predefined PDF document types in the database.
 *
 * This ensures that the system has the required PDF document types for various generation scenarios.
 * Each document type is created with system-defined variables/schema and a system default template.
 *
 * PDF Document Types:
 * - FULL_ITINERARY: Complete itinerary with all nested data (days, parks, activities, etc.)
 *
 * Note: Other document types (SAFARI_QUOTE, BOOKING_CONFIRMATION, etc.) will be added in future iterations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentInitializer implements ApplicationRunner, Ordered {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateCreateService pdfTemplateCreateService;

    /**
     * Run initialization at application startup
     * Priority: Run after TariffInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 14;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = false;

        try {
            initializePdfDocuments();
            success = true;
        } catch (Exception e) {
            log.error("Error during PDF document initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    /**
     * Print start banner for PDF document initialization
     */
    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║             PDF DOCUMENT INITIALIZER - START                       ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    /**
     * Print end banner for PDF document initialization
     *
     * @param success whether the initialization was successful
     */
    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✓ PDF DOCUMENT INITIALIZER - COMPLETED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✗ PDF DOCUMENT INITIALIZER - FAILED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize predefined PDF document types
     */
    private void initializePdfDocuments() {
        // Initialize all supported document types
        for (String documentName : PdfDocumentVariables.getSupportedDocuments()) {
            initializeDocument(documentName);
        }
    }

    /**
     * Initialize a single PDF document type with system-defined schema and default template
     */
    private void initializeDocument(String documentName) {
        try {
            /*
             * An existing document still gets its templates checked. A document
             * created before a template shipped would otherwise never receive
             * it, and the only way to notice would be a missing option in the
             * generate drawer.
             */
            PdfDocument existing = pdfDocumentRepository.findByName(documentName).orElse(null);
            if (existing != null) {
                log.debug("⊘ PDF document already exists: {}", documentName);
                ensureTemplates(existing, documentName);
                return;
            }

            // Get metadata from PdfDocumentVariables
            String displayName = PdfDocumentVariables.getDisplayName(documentName);
            String description = PdfDocumentVariables.getDescription(documentName);
            String dataSourceClass = PdfDocumentVariables.getDataSourceClass(documentName);
            String rootVariableName = PdfDocumentVariables.getRootVariableName(documentName);
            String variablesJson = PdfDocumentVariables.getVariablesForDocument(documentName);

            // Create PDF document entity
            PdfDocument document = PdfDocument.builder()
                .name(documentName)
                .displayName(displayName)
                .description(description)
                .dataSourceClass(dataSourceClass)
                .rootVariableName(rootVariableName)
                .enabled(true)
                .variablesJson(variablesJson)
                .build();

            PdfDocument savedDocument = pdfDocumentRepository.save(document);
            log.info("Created PDF document: {} ({})", documentName, displayName);

            ensureTemplates(savedDocument, documentName);

        } catch (Exception e) {
            log.error("Failed to initialize PDF document: {}", documentName, e);
        }
    }

    /**
     * The default template, plus any extras the document type ships with.
     *
     * Both steps are idempotent, so this is safe to run on every start.
     */
    private void ensureTemplates(PdfDocument document, String documentName) {
        if (pdfTemplateCreateService.createSystemDefaultTemplate(document)) {
            log.debug("System default template present for: {}", documentName);
        } else {
            log.warn("Failed to create system default template for document: {}", documentName);
        }

        for (String[] extra : PdfDocumentVariables.getExtraTemplates(documentName)) {
            pdfTemplateCreateService.createSeededTemplate(document, extra[0], extra[1], extra[2]);
        }
    }
}
