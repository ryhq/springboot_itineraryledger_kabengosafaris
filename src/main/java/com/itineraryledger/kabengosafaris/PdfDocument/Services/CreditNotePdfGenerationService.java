package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.FullCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteFullGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreditNotePdfGenerationService - PDF generation for credit notes
 *
 * The FULL_CREDIT_NOTE document type, its schema and its Thymeleaf template have all shipped since
 * credit notes were added, but nothing ever reached them: the dispatcher had no case for the type,
 * so every request fell through to the default branch and came back "PDF document type not found".
 * A credit note could be raised and sent, and never printed.
 */
@Service
@Slf4j
public class CreditNotePdfGenerationService extends PdfGenerationBaseService {

    private final CreditNoteFullGetService creditNoteFullGetService;

    public CreditNotePdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            CreditNoteFullGetService creditNoteFullGetService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.creditNoteFullGetService = creditNoteFullGetService;
    }

    /**
     * Generate the PDF for a credit note, optionally translated.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateCreditNotePdf(
            String creditNoteIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullCreditNoteDTO data = creditNoteFullGetService.fetch(creditNoteIdObfuscated);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Credit note not found: " + creditNoteIdObfuscated,
                        "CREDIT_NOTE_NOT_FOUND")
                );
            }

            return generatePdfInternal(
                "FULL_CREDIT_NOTE",
                data,
                templateIdObfuscated,
                language,
                d -> ((FullCreditNoteDTO) d).getCreditNoteCode()
            );

        } catch (Exception e) {
            log.error("Failed to generate credit note PDF: {}", creditNoteIdObfuscated, e);
            logPdfError("GENERATE_PDF", "FULL_CREDIT_NOTE", creditNoteIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Render the credit note to HTML instead of PDF, for previewing a template.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewCreditNotePdf(
            String creditNoteIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullCreditNoteDTO data = creditNoteFullGetService.fetch(creditNoteIdObfuscated);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Credit note not found: " + creditNoteIdObfuscated,
                        "CREDIT_NOTE_NOT_FOUND")
                );
            }

            return previewPdfInternal("FULL_CREDIT_NOTE", data, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview credit note PDF: {}", creditNoteIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "FULL_CREDIT_NOTE", creditNoteIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to preview PDF: " + e.getMessage(), "PDF_PREVIEW_FAILED")
            );
        }
    }
}
