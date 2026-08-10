package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationDocumentService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.ItineraryDocumentDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentCreateService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The costing, as a PDF.
 *
 * Unlike every other document here, this one has a parameter that changes the
 * answer rather than the presentation: the trip start date. Rates are seasonal,
 * so the same itinerary priced for February and for June are two correct and
 * different documents — which is why the date is passed in and printed on the
 * page rather than assumed.
 *
 * A saved copy is filed as an ItineraryDocument, because a costing belongs to
 * the itinerary it prices and that is where anyone would look for it.
 */
@Service
@Slf4j
public class CostEstimationPdfGenerationService extends PdfGenerationBaseService {

    private static final String DOCUMENT = "FULL_COST_ESTIMATION";

    private final CostEstimationDocumentService costingService;
    private final ItineraryDocumentCreateService itineraryDocumentCreateService;

    public CostEstimationPdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            CostEstimationDocumentService costingService,
            ItineraryDocumentCreateService itineraryDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.costingService = costingService;
        this.itineraryDocumentCreateService = itineraryDocumentCreateService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateCostEstimationPdf(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            LocalDate startDate
    ) {
        try {
            FullCostEstimationDTO costing = costingService.build(itineraryIdObfuscated, startDate);
            if (costing == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404,
                        "Nothing to cost for itinerary " + itineraryIdObfuscated
                            + " — it may not exist, or it has no days and pax to price.",
                        "COSTING_NOT_AVAILABLE")
                );
            }

            return generatePdfInternal(
                DOCUMENT,
                costing,
                templateIdObfuscated,
                language,
                data -> ((FullCostEstimationDTO) data).getSubjectCode()
            );

        } catch (Exception e) {
            log.error("Failed to generate costing PDF: {}", itineraryIdObfuscated, e);
            logPdfError("GENERATE_PDF", DOCUMENT, itineraryIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate the costing: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /** The rendered HTML, before anything becomes a file. */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewCostEstimationPdf(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            LocalDate startDate
    ) {
        try {
            FullCostEstimationDTO costing = costingService.build(itineraryIdObfuscated, startDate);
            if (costing == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404,
                        "Nothing to cost for itinerary " + itineraryIdObfuscated,
                        "COSTING_NOT_AVAILABLE")
                );
            }
            return previewPdfInternal(DOCUMENT, costing, templateIdObfuscated, language);
        } catch (Exception e) {
            log.error("Failed to preview costing PDF: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to preview the costing: " + e.getMessage(), "PDF_PREVIEW_FAILED")
            );
        }
    }

    /**
     * Generates it and files it against the itinerary.
     *
     * The filename carries the pricing date, not just the moment of generation:
     * two costings of the same trip differ by the date they assumed, and a
     * folder of files named only by timestamp cannot tell you which is which.
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveCostEstimationPdf(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            LocalDate startDate,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            FullCostEstimationDTO costing = costingService.build(itineraryIdObfuscated, startDate);
            if (costing == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404,
                        "Nothing to cost for itinerary " + itineraryIdObfuscated,
                        "COSTING_NOT_AVAILABLE")
                );
            }

            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            ResponseEntity<?> pdfResponse = generateCostEstimationPdf(
                itineraryIdObfuscated, templateIdObfuscated, language, startDate);
            if (!pdfResponse.getStatusCode().is2xxSuccessful()) {
                return pdfResponse;
            }

            byte[] pdfBytes = (byte[]) pdfResponse.getBody();
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "The generated costing was empty", "EMPTY_PDF")
                );
            }

            ItineraryDocument.DocumentType docType = itineraryDocumentType != null
                ? itineraryDocumentType
                : ItineraryDocument.DocumentType.QUOTATION;

            String code = costing.getSubjectCode() != null ? costing.getSubjectCode() : "ITI";
            String pricedFor = costing.getStartDate() != null ? costing.getStartDate().toString() : "today";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";

            String fileName = String.format("costing_%s_for_%s%s_%s.pdf",
                code, pricedFor, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("Costing - %s (priced for %s)",
                    costing.getSubjectName() != null ? costing.getSubjectName() : code, pricedFor);

            ItineraryDocumentDTO saved = itineraryDocumentCreateService.saveGeneratedDocument(
                itineraryId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                // the description records the basis, so a filed costing stays readable
                String.format("Priced for %s. %s",
                    pricedFor,
                    Boolean.TRUE.equals(costing.getHasIncompleteRates())
                        ? costing.getRateIssueCount() + " item(s) could not be priced — the totals are partial."
                        : "Every item priced."),
                version,
                notes
            );

            if (saved == null) {
                log.warn("Costing generated but not filed for itinerary: {}", itineraryIdObfuscated);
                return pdfResponse;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);
            headers.set("X-Document-Saved", "true");
            headers.set("X-Document-Id", saved.getId());
            headers.set("X-Document-Url", saved.getDocumentUrl());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to generate and file the costing for: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and file the costing: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }
}
