package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.Response.ContentTypes;
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
 * ItineraryWordGenerationService - Handles Word (.docx) generation for itineraries.
 *
 * Mirrors {@link ItineraryPdfGenerationService} but emits .docx via {@link WordGenerator}.
 */
@Service
@Slf4j
public class ItineraryWordGenerationService extends WordGenerationBaseService {

    private final ItineraryFullGetService itineraryFullGetService;
    private final ItineraryDocumentCreateService itineraryDocumentCreateService;

    public ItineraryWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            ItineraryFullGetService itineraryFullGetService,
            ItineraryDocumentCreateService itineraryDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.itineraryFullGetService = itineraryFullGetService;
        this.itineraryDocumentCreateService = itineraryDocumentCreateService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryDocx(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        return generateItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            String engine
    ) {
        try {
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found: " + itineraryIdObfuscated, "ITINERARY_NOT_FOUND")
                );
            }

            return generateDocxInternal(
                "FULL_ITINERARY",
                itineraryData,
                templateIdObfuscated,
                language,
                engine,
                data -> extractItineraryCode((FullItineraryDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate itinerary DOCX: {}", itineraryIdObfuscated, e);
            logDocxError("GENERATE_DOCX", "FULL_ITINERARY", itineraryIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language,
            itineraryDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        try {
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found: " + itineraryIdObfuscated, "ITINERARY_NOT_FOUND")
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

            ResponseEntity<?> docxResponse = generateItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language, engine);
            if (!docxResponse.getStatusCode().is2xxSuccessful()) {
                return docxResponse;
            }

            byte[] docxBytes = (byte[]) docxResponse.getBody();
            if (docxBytes == null || docxBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated DOCX is empty", "EMPTY_DOCX")
                );
            }

            String itineraryCode = itineraryData.getCode() != null ? itineraryData.getCode() : "ITI";
            String itineraryName = itineraryData.getName() != null ? itineraryData.getName() : "Itinerary";

            ItineraryDocument.DocumentType docType = itineraryDocumentType != null
                ? itineraryDocumentType
                : ItineraryDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.docx",
                docType.name().toLowerCase(), itineraryCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), itineraryName);

            ItineraryDocumentDTO savedDocument = itineraryDocumentCreateService.saveGeneratedDocument(
                itineraryId,
                docxBytes,
                fileName,
                docType,
                documentTitle,
                null,
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("DOCX generated but failed to save to documents for itinerary: {}", itineraryIdObfuscated);
                return docxResponse;
            }

            log.info("Generated and saved DOCX document: {} for itinerary {} (document ID: {})",
                fileName, itineraryIdObfuscated, savedDocument.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(ContentTypes.safe(DOCX_MEDIA_TYPE));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(docxBytes.length);
            headers.set("X-Document-Saved", "true");
            headers.set("X-Document-Id", savedDocument.getId());
            headers.set("X-Document-Url", savedDocument.getDocumentUrl());
            String engineHeader = docxResponse.getHeaders().getFirst("X-Docx-Engine");
            if (engineHeader != null) {
                headers.set("X-Docx-Engine", engineHeader);
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(docxBytes);

        } catch (Exception e) {
            log.error("Failed to generate and save DOCX for itinerary: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save DOCX: " + e.getMessage(), "DOCX_SAVE_FAILED")
            );
        }
    }

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

    private String extractItineraryCode(FullItineraryDTO itinerary) {
        String code = itinerary.getCode();
        return (code != null && !code.isBlank()) ? code : "itinerary";
    }
}
