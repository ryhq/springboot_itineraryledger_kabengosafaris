package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.Response.ContentTypes;
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
import java.util.Map;

/**
 * SafariWordGenerationService - Handles Word (.docx) generation for safaris.
 *
 * Mirrors {@link SafariPdfGenerationService} but emits .docx via {@link WordGenerator}.
 */
@Service
@Slf4j
public class SafariWordGenerationService extends WordGenerationBaseService {

    private final SafariFullGetService safariFullGetService;
    private final SafariDocumentCreateService safariDocumentCreateService;

    public SafariWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            SafariFullGetService safariFullGetService,
            SafariDocumentCreateService safariDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.safariFullGetService = safariFullGetService;
        this.safariDocumentCreateService = safariDocumentCreateService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariDocx(String safariIdObfuscated, String templateIdObfuscated, String language) {
        return generateSafariDocx(safariIdObfuscated, templateIdObfuscated, language, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariDocx(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            String engine
    ) {
        try {
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found: " + safariIdObfuscated, "SAFARI_NOT_FOUND")
                );
            }

            return generateDocxInternal(
                "FULL_SAFARI",
                safariData,
                templateIdObfuscated,
                language,
                engine,
                data -> extractSafariCode((FullSafariDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate safari DOCX: {}", safariIdObfuscated, e);
            logDocxError("GENERATE_DOCX", "FULL_SAFARI", safariIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveSafariDocx(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveSafariDocx(safariIdObfuscated, templateIdObfuscated, language,
            safariDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveSafariDocx(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        try {
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found: " + safariIdObfuscated, "SAFARI_NOT_FOUND")
                );
            }

            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            ResponseEntity<?> docxResponse = generateSafariDocx(safariIdObfuscated, templateIdObfuscated, language, engine);
            if (!docxResponse.getStatusCode().is2xxSuccessful()) {
                return docxResponse;
            }

            byte[] docxBytes = (byte[]) docxResponse.getBody();
            if (docxBytes == null || docxBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated DOCX is empty", "EMPTY_DOCX")
                );
            }

            String safariCode = safariData.getCode() != null ? safariData.getCode() : "SAF";
            String safariName = safariData.getName() != null ? safariData.getName() : "Safari";

            SafariDocument.DocumentType docType = safariDocumentType != null
                ? safariDocumentType
                : SafariDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.docx",
                docType.name().toLowerCase(), safariCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), safariName);

            SafariDocumentDTO savedDocument = safariDocumentCreateService.saveGeneratedDocument(
                safariId,
                docxBytes,
                fileName,
                docType,
                documentTitle,
                null,
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("DOCX generated but failed to save to documents for safari: {}", safariIdObfuscated);
                return docxResponse;
            }

            log.info("Generated and saved DOCX document: {} for safari {} (document ID: {})",
                fileName, safariIdObfuscated, savedDocument.getId());

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
            log.error("Failed to generate and save DOCX for safari: {}", safariIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save DOCX: " + e.getMessage(), "DOCX_SAVE_FAILED")
            );
        }
    }

    private FullSafariDTO fetchSafariData(String safariIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = safariFullGetService.getFullSafari(safariIdObfuscated);
            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() != null) {
                    Object responseData = apiResponse.getData();
                    if (responseData instanceof FullSafariDTO) {
                        return (FullSafariDTO) responseData;
                    } else if (responseData instanceof Map) {
                        Object safariObj = ((Map<?, ?>) responseData).get("safari");
                        if (safariObj instanceof FullSafariDTO) {
                            return (FullSafariDTO) safariObj;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch safari data: {}", safariIdObfuscated, e);
            return null;
        }
    }

    private String extractSafariCode(FullSafariDTO safari) {
        String code = safari.getCode();
        return (code != null && !code.isBlank()) ? code : "safari";
    }
}
