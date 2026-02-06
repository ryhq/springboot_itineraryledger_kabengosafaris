package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.SafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentCreateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariFullGetService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SafariPdfGenerationService - Handles PDF generation for safaris
 *
 * Responsibilities:
 * - Generate safari PDFs with optional translation
 * - Generate and save safari PDFs to SafariDocuments
 * - Preview safari PDFs (HTML only)
 */
@Service
@Slf4j
public class SafariPdfGenerationService extends PdfGenerationBaseService {

    private final SafariFullGetService safariFullGetService;
    private final SafariDocumentCreateService safariDocumentCreateService;

    public SafariPdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            SafariFullGetService safariFullGetService,
            SafariDocumentCreateService safariDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.safariFullGetService = safariFullGetService;
        this.safariDocumentCreateService = safariDocumentCreateService;
    }

    /**
     * Generate PDF for safari with optional translation
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariPdf(String safariIdObfuscated, String templateIdObfuscated, String language) {
        try {
            // Fetch safari data
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found: " + safariIdObfuscated, "SAFARI_NOT_FOUND")
                );
            }

            // Generate PDF using base service
            return generatePdfInternal(
                "FULL_SAFARI",
                safariData,
                templateIdObfuscated,
                language,
                data -> extractSafariCode((FullSafariDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate safari PDF: {}", safariIdObfuscated, e);
            logPdfError("GENERATE_PDF", "FULL_SAFARI", safariIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate Safari PDF and save it as a SafariDocument
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param safariDocumentType The document type (e.g., QUOTATION, FINAL_ITINERARY)
     * @param title Optional custom title for the document
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes and saved document metadata
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveSafariPdf(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch safari data first
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found: " + safariIdObfuscated, "SAFARI_NOT_FOUND")
                );
            }

            // 2. Decode safari ID for saving
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // 3. Generate the PDF
            ResponseEntity<?> pdfResponse = generateSafariPdf(safariIdObfuscated, templateIdObfuscated, language);

            // If PDF generation failed, return the error response
            if (!pdfResponse.getStatusCode().is2xxSuccessful()) {
                return pdfResponse;
            }

            // 4. Extract PDF bytes from response
            byte[] pdfBytes = (byte[]) pdfResponse.getBody();
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated PDF is empty", "EMPTY_PDF")
                );
            }

            // 5. Generate filename and title
            String safariCode = safariData.getCode() != null ? safariData.getCode() : "SAF";
            String safariName = safariData.getName() != null ? safariData.getName() : "Safari";

            SafariDocument.DocumentType docType = safariDocumentType != null
                ? safariDocumentType
                : SafariDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), safariCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), safariName);

            // 6. Save the document
            SafariDocumentDTO savedDocument = safariDocumentCreateService.saveGeneratedDocument(
                safariId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for safari: {}", safariIdObfuscated);
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for safari {} (document ID: {})",
                fileName, safariIdObfuscated, savedDocument.getId());

            // 7. Return the PDF response with additional headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);
            headers.set("X-Document-Saved", "true");
            headers.set("X-Document-Id", savedDocument.getId());
            headers.set("X-Document-Url", savedDocument.getDocumentUrl());

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to generate and save PDF for safari: {}", safariIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    /**
     * Preview safari PDF (return rendered HTML instead of PDF)
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with rendered HTML or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewSafariPdf(String safariIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            return previewPdfInternal("FULL_SAFARI", safariData, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview safari PDF: {}", safariIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "FULL_SAFARI", safariIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Fetch full safari data
     */
    private FullSafariDTO fetchSafariData(String safariIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = safariFullGetService.getFullSafari(safariIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() instanceof FullSafariDTO) {
                    return (FullSafariDTO) apiResponse.getData();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch safari data: {}", safariIdObfuscated, e);
            return null;
        }
    }

    /**
     * Extract safari code from data
     */
    private String extractSafariCode(FullSafariDTO safari) {
        String code = safari.getCode();
        return (code != null && !code.isBlank()) ? code : "safari";
    }
}
