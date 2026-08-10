package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.CostEstimationDocumentService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * The costing as a DOCX, for when the figures have to be reworked.
 *
 * A PDF is what goes to a client; a costing more often goes to a colleague who
 * wants to change an assumption. Same data, same templates, editable output.
 */
@Service
@Slf4j
public class CostEstimationWordGenerationService extends WordGenerationBaseService {

    private static final String DOCUMENT = "FULL_COST_ESTIMATION";

    private final CostEstimationDocumentService costingService;

    public CostEstimationWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            CostEstimationDocumentService costingService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.costingService = costingService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateCostEstimationDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            String engine,
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

            return generateDocxInternal(
                DOCUMENT,
                costing,
                templateIdObfuscated,
                language,
                engine,
                data -> ((FullCostEstimationDTO) data).getSubjectCode()
            );

        } catch (Exception e) {
            log.error("Failed to generate costing DOCX: {}", itineraryIdObfuscated, e);
            logDocxError("GENERATE_DOCX", DOCUMENT, itineraryIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate the costing: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }
}
