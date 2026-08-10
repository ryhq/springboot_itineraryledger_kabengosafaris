package com.itineraryledger.kabengosafaris.PdfDocument.Controller;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.GeneratePdfRequestDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PaymentReceiptWordGenerationService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.WordGenerationService;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Word (.docx) Generation.
 *
 * Mirrors {@link PdfGenerationController} but produces .docx output.
 * Reuses the same Thymeleaf templates, the same data fetching, the same
 * translation pipeline, and the same per-module document storage chain.
 *
 * Per-request engine override: every endpoint accepts an optional `engine`
 * query param ("docx4j" or "libreoffice") so clients can A/B compare the
 * two engines without flipping the global `docx.engine` config. When omitted,
 * the global default is used. The engine that actually produced the bytes
 * is reported in the `X-Docx-Engine` response header.
 *
 * Authorization piggy-backs on PERM_GENERATE_PDF — admins can split into
 * a dedicated PERM_GENERATE_DOCX permission later via the RBAC UI if needed.
 */
@RestController
@RequestMapping("/api/docx")
@RequiredArgsConstructor
@Slf4j
public class WordGenerationController {

    private final WordGenerationService generationService;
    private final PaymentReceiptWordGenerationService paymentReceiptWordGenerationService;
    private final PaymentRepository paymentRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Generic DOCX generation endpoint — mirrors POST /api/pdf/generate.
     * The `engine` field on the request DTO overrides the global docx.engine setting.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateDocx(@Valid @RequestBody GeneratePdfRequestDTO request) {
        log.info("POST /api/docx/generate - Generating DOCX for {} / {}{}{}{}",
            request.getDocumentType(), request.getDataId(),
            request.getLanguage() != null ? ", language: " + request.getLanguage() : "",
            request.getEngine() != null ? ", engine: " + request.getEngine() : "",
            Boolean.TRUE.equals(request.getSaveToDocuments()) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_ITINERARY".equals(request.getDocumentType())) {
            return generationService.generateAndSaveItineraryDocx(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getItineraryDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes(),
                request.getEngine()
            );
        }

        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_QUOTE".equals(request.getDocumentType())) {
            return generationService.generateAndSaveQuoteDocx(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getQuoteDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes(),
                request.getEngine()
            );
        }

        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_SAFARI".equals(request.getDocumentType())) {
            return generationService.generateAndSaveSafariDocx(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getSafariDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes(),
                request.getEngine()
            );
        }

        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_INVOICE".equals(request.getDocumentType())) {
            return generationService.generateAndSaveInvoiceDocx(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getInvoiceDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes(),
                request.getEngine()
            );
        }

        return generationService.generateDocx(
            request.getDocumentType(),
            request.getDataId(),
            request.getTemplateId(),
            request.getLanguage(),
            request.getEngine(),
            request.getStartDate()
        );
    }

    // =====================================================================
    // ITINERARY DOCX
    // =====================================================================

    @GetMapping("/itinerary/{itineraryId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateItineraryDocx(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String engine,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) ItineraryDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/docx/itinerary/{} - Generating itinerary DOCX{}{}{}",
            itineraryId,
            language != null ? ", language: " + language : "",
            engine != null ? ", engine: " + engine : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveItineraryDocx(
                itineraryId, templateId, language, documentType,
                documentTitle, documentVersion, documentNotes, engine
            );
        }
        return generationService.generateItineraryDocx(itineraryId, templateId, language, engine);
    }

    // =====================================================================
    // QUOTE DOCX
    // =====================================================================

    @GetMapping("/quote/{quoteId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateQuoteDocx(
        @PathVariable String quoteId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String engine,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) QuoteDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/docx/quote/{} - Generating quote DOCX{}{}{}",
            quoteId,
            language != null ? ", language: " + language : "",
            engine != null ? ", engine: " + engine : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveQuoteDocx(
                quoteId, templateId, language, documentType,
                documentTitle, documentVersion, documentNotes, engine
            );
        }
        return generationService.generateQuoteDocx(quoteId, templateId, language, engine);
    }

    // =====================================================================
    // SAFARI DOCX
    // =====================================================================

    @GetMapping("/safari/{safariId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateSafariDocx(
        @PathVariable String safariId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String engine,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) SafariDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/docx/safari/{} - Generating safari DOCX{}{}{}",
            safariId,
            language != null ? ", language: " + language : "",
            engine != null ? ", engine: " + engine : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveSafariDocx(
                safariId, templateId, language, documentType,
                documentTitle, documentVersion, documentNotes, engine
            );
        }
        return generationService.generateSafariDocx(safariId, templateId, language, engine);
    }

    // =====================================================================
    // INVOICE DOCX
    // =====================================================================

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateInvoiceDocx(
        @PathVariable String invoiceId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String engine,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) InvoiceDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/docx/invoice/{} - Generating invoice DOCX{}{}{}",
            invoiceId,
            language != null ? ", language: " + language : "",
            engine != null ? ", engine: " + engine : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveInvoiceDocx(
                invoiceId, templateId, language, documentType,
                documentTitle, documentVersion, documentNotes, engine
            );
        }
        return generationService.generateInvoiceDocx(invoiceId, templateId, language, engine);
    }

    // =====================================================================
    // PAYMENT RECEIPT DOCX
    // =====================================================================

    /**
     * Generate a payment receipt as a .docx for a recorded payment.
     * Mirrors the PDF endpoint at /api/invoices/{invoiceId}/payments/{paymentId}/receipt-pdf
     * but lives under /api/docx/* per the user's chosen API shape.
     */
    @GetMapping("/payment-receipt/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generatePaymentReceiptDocx(
        @PathVariable String paymentId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String engine
    ) {
        log.info("GET /api/docx/payment-receipt/{} - Generating receipt DOCX{}{}",
            paymentId,
            language != null ? ", language: " + language : "",
            engine != null ? ", engine: " + engine : "");

        try {
            Long decodedPaymentId = idObfuscator.decodeId(paymentId);
            Payment payment = paymentRepository.findById(decodedPaymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }
            return paymentReceiptWordGenerationService.generatePaymentReceiptDocx(payment, language, templateId, engine);
        } catch (Exception e) {
            log.error("Error generating payment receipt DOCX", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to generate payment receipt DOCX", "DOCX_GENERATION_FAILED")
            );
        }
    }
}
