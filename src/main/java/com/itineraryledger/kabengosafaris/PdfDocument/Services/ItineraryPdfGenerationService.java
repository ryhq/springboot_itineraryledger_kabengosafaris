package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.ItineraryDocumentDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
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
import java.util.Map;

/**
 * ItineraryPdfGenerationService - Handles PDF generation for itineraries
 *
 * Responsibilities:
 * - Generate itinerary PDFs with optional translation
 * - Generate and save itinerary PDFs to ItineraryDocuments
 * - Preview itinerary PDFs (HTML only)
 */
@Service
@Slf4j
public class ItineraryPdfGenerationService extends PdfGenerationBaseService {

    private final ItineraryFullGetService itineraryFullGetService;
    private final ItineraryDocumentCreateService itineraryDocumentCreateService;

    public ItineraryPdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            ItineraryFullGetService itineraryFullGetService,
            ItineraryDocumentCreateService itineraryDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.itineraryFullGetService = itineraryFullGetService;
        this.itineraryDocumentCreateService = itineraryDocumentCreateService;
    }

    /**
     * Generate PDF for itinerary with optional translation
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation (e.g., "fr", "de")
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryPdf(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        try {
            // Fetch itinerary data
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found: " + itineraryIdObfuscated, "ITINERARY_NOT_FOUND")
                );
            }

            // Generate PDF using base service
            return generatePdfInternal(
                "FULL_ITINERARY",
                itineraryData,
                templateIdObfuscated,
                language,
                data -> extractItineraryCode((FullItineraryDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate itinerary PDF: {}", itineraryIdObfuscated, e);
            logPdfError("GENERATE_PDF", "FULL_ITINERARY", itineraryIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate PDF for itinerary and save it to ItineraryDocuments
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param itineraryDocumentType The type of document to save as (e.g., QUOTATION, FINAL_ITINERARY)
     * @param title Optional custom title (auto-generated if null)
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes (document is also saved) or error
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryPdf(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch itinerary data first
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found: " + itineraryIdObfuscated, "ITINERARY_NOT_FOUND")
                );
            }

            // 2. Decode itinerary ID for saving
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // 3. Generate the PDF
            ResponseEntity<?> pdfResponse = generateItineraryPdf(itineraryIdObfuscated, templateIdObfuscated, language);

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
            String itineraryCode = itineraryData.getCode() != null ? itineraryData.getCode() : "ITI";
            String itineraryName = itineraryData.getName() != null ? itineraryData.getName() : "Itinerary";

            ItineraryDocument.DocumentType docType = itineraryDocumentType != null
                ? itineraryDocumentType
                : ItineraryDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), itineraryCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), itineraryName);

            // 6. Save the document
            ItineraryDocumentDTO savedDocument = itineraryDocumentCreateService.saveGeneratedDocument(
                itineraryId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for itinerary: {}", itineraryIdObfuscated);
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for itinerary {} (document ID: {})",
                fileName, itineraryIdObfuscated, savedDocument.getId());

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
            log.error("Failed to generate and save PDF for itinerary: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    /**
     * Preview itinerary PDF (return rendered HTML instead of PDF)
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with rendered HTML or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewItineraryPdf(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            return previewPdfInternal("FULL_ITINERARY", itineraryData, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview itinerary PDF: {}", itineraryIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "FULL_ITINERARY", itineraryIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Fetch full itinerary data
     */
    private FullItineraryDTO fetchItineraryData(String itineraryIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = itineraryFullGetService.getFullItinerary(itineraryIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null) {
                    Object data = apiResponse.getData();
                    if (data instanceof FullItineraryDTO) {
                        return (FullItineraryDTO) data;
                    } else if (data instanceof Map) {
                        Object itineraryObj = ((Map<?, ?>) data).get("itinerary");
                        if (itineraryObj instanceof FullItineraryDTO) {
                            return (FullItineraryDTO) itineraryObj;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch itinerary data: {}", itineraryIdObfuscated, e);
            return null;
        }
    }

    /**
     * Extract itinerary code from data
     */
    private String extractItineraryCode(FullItineraryDTO itinerary) {
        String code = itinerary.getCode();
        return (code != null && !code.isBlank()) ? code : "itinerary";
    }
}
