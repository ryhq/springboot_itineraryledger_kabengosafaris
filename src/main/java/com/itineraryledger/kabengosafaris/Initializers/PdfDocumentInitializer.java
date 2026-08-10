package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.PdfDocument.PdfDocumentVariables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

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

    private final PdfDocumentSeeder seeder;

    /**
     * Run initialization at application startup
     * Priority: Run after TariffInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 14;
    }

    /*
     * NOT @Transactional. Sharing one transaction across every document is what
     * made a single failed insert fatal: the per-document catch swallowed the
     * exception, the transaction was already rollback-only, and the commit threw
     * afterwards — taking the context down and putting systemd in a restart
     * loop. Each document now gets its own transaction via PdfDocumentSeeder.
     */
    @Override
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
     * Initialize predefined PDF document types.
     *
     * A failure is reported and stepped over. A document type that cannot be
     * seeded is a missing option in a drawer; it is never a reason for the API
     * to be unreachable.
     */
    private void initializePdfDocuments() {
        int seeded = 0;
        int failed = 0;

        for (String documentName : PdfDocumentVariables.getSupportedDocuments()) {
            try {
                seeder.seed(documentName);
                seeded++;
            } catch (Exception e) {
                failed++;
                log.error("✗ Could not seed PDF document '{}': {} — the application continues without it",
                    documentName, e.getMessage());
            }
        }

        log.info("PDF documents: {} ready, {} failed", seeded, failed);
    }
}
