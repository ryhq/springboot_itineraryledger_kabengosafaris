package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.Response.ContentTypes;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.QuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteFullGetService;
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
 * QuoteWordGenerationService - Handles Word (.docx) generation for quotes.
 *
 * Mirrors {@link QuotePdfGenerationService} but emits .docx via {@link WordGenerator}.
 */
@Service
@Slf4j
public class QuoteWordGenerationService extends WordGenerationBaseService {

    private final QuoteFullGetService quoteFullGetService;
    private final QuoteDocumentCreateService quoteDocumentCreateService;

    public QuoteWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            QuoteFullGetService quoteFullGetService,
            QuoteDocumentCreateService quoteDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.quoteFullGetService = quoteFullGetService;
        this.quoteDocumentCreateService = quoteDocumentCreateService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuoteDocx(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        return generateQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuoteDocx(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            String engine
    ) {
        try {
            FullQuoteDTO quoteData = fetchQuoteData(quoteIdObfuscated);
            if (quoteData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found: " + quoteIdObfuscated, "QUOTE_NOT_FOUND")
                );
            }

            return generateDocxInternal(
                "FULL_QUOTE",
                quoteData,
                templateIdObfuscated,
                language,
                engine,
                data -> extractQuoteCode((FullQuoteDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate quote DOCX: {}", quoteIdObfuscated, e);
            logDocxError("GENERATE_DOCX", "FULL_QUOTE", quoteIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveQuoteDocx(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            QuoteDocument.DocumentType quoteDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language,
            quoteDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveQuoteDocx(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            QuoteDocument.DocumentType quoteDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        try {
            FullQuoteDTO quoteData = fetchQuoteData(quoteIdObfuscated);
            if (quoteData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found: " + quoteIdObfuscated, "QUOTE_NOT_FOUND")
                );
            }

            Long quoteId;
            try {
                quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            ResponseEntity<?> docxResponse = generateQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language, engine);
            if (!docxResponse.getStatusCode().is2xxSuccessful()) {
                return docxResponse;
            }

            byte[] docxBytes = (byte[]) docxResponse.getBody();
            if (docxBytes == null || docxBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated DOCX is empty", "EMPTY_DOCX")
                );
            }

            String quoteCode = quoteData.getQuoteCode() != null ? quoteData.getQuoteCode() : "QTE";
            String quoteTitle = quoteData.getTitle() != null ? quoteData.getTitle() : "Quote";

            QuoteDocument.DocumentType docType = quoteDocumentType != null
                ? quoteDocumentType
                : QuoteDocument.DocumentType.QUOTE_PDF;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.docx",
                docType.name().toLowerCase(), quoteCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), quoteTitle);

            QuoteDocumentDTO savedDocument = quoteDocumentCreateService.saveGeneratedDocument(
                quoteId,
                docxBytes,
                fileName,
                docType,
                documentTitle,
                null,
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("DOCX generated but failed to save to documents for quote: {}", quoteIdObfuscated);
                return docxResponse;
            }

            log.info("Generated and saved DOCX document: {} for quote {} (document ID: {})",
                fileName, quoteIdObfuscated, savedDocument.getId());

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
            log.error("Failed to generate and save DOCX for quote: {}", quoteIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save DOCX: " + e.getMessage(), "DOCX_SAVE_FAILED")
            );
        }
    }

    private FullQuoteDTO fetchQuoteData(String quoteIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = quoteFullGetService.getFullQuote(quoteIdObfuscated);
            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null) {
                    Object data = apiResponse.getData();
                    if (data instanceof FullQuoteDTO) {
                        return (FullQuoteDTO) data;
                    } else if (data instanceof Map) {
                        Object quoteObj = ((Map<?, ?>) data).get("quote");
                        if (quoteObj instanceof FullQuoteDTO) {
                            return (FullQuoteDTO) quoteObj;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch quote data: {}", quoteIdObfuscated, e);
            return null;
        }
    }

    private String extractQuoteCode(FullQuoteDTO quote) {
        String code = quote.getQuoteCode();
        return (code != null && !code.isBlank()) ? code : "quote";
    }
}
