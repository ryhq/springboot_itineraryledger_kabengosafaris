package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentCreateService;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.Safari.DTOs.CreateSafariFromItineraryDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCreateService;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.QuotePdfGenerationService;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentCreateService;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QuoteStatusService - Service for managing quote status workflow transitions
 *
 * Implements the complete quote lifecycle:
 * DRAFT → READY → SENT → [ACCEPTED/REJECTED/EXPIRED] → [CONVERTED/CANCELLED]
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteStatusService {

    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final QuotePdfGenerationService quotePdfGenerationService;
    private final TranslationService translationService;
    private final QuoteDocumentCreateService quoteDocumentCreateService;
    private final CustomerDocumentCreateService customerDocumentCreateService;
    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateRepository pdfTemplateRepository;
    private final SafariCreateService safariCreateService;


    /**
     * Mark quote as READY (ready to send to customer)
     * Only allowed from DRAFT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "READY_QUOTE", description = "Marking quote as ready", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> markAsReady(String idObfuscated) {
        log.info("Marking quote as ready: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status
            if (quote.getStatus() != QuoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only DRAFT quotes can be marked as READY", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate completeness
            if (!canMarkAsReady(quote)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Quote does not meet requirements. Must have itinerary, customer, and line items.",
                        "INCOMPLETE_QUOTE"
                    )
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.READY);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as ready: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as ready", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error marking quote as ready", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to mark quote as ready", "READY_FAILED")
            );
        }
    }

    /**
     * Send quote to customer
     * Allowed from DRAFT (if ready), READY, SENT (resend), ACCEPTED (resend), CONVERTED (resend)
     *
     * @param idObfuscated The obfuscated quote ID
     * @param language Optional language code for translation (falls back to customer's preferredLanguage)
     * @param emailTemplateIdObfuscated Optional obfuscated ID of a SEND_QUOTE email template to use
     * @param pdfTemplateIdObfuscated Optional obfuscated ID of a PDF template to use for generation
     * @param saveAsQuoteDocument Whether to save the PDF as a quote document
     * @param saveAsCustomerDocument Whether to save the PDF as a customer document
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "SEND_QUOTE", description = "Sending quote to customer", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> sendQuote(String idObfuscated, String language, String emailTemplateIdObfuscated,
                                                     String pdfTemplateIdObfuscated, boolean saveAsQuoteDocument, boolean saveAsCustomerDocument) {
        log.info("Sending quote: {} (language: {}, emailTemplateId: {}, pdfTemplateId: {}, saveQuoteDoc: {}, saveCustomerDoc: {})",
            idObfuscated, language, emailTemplateIdObfuscated, pdfTemplateIdObfuscated, saveAsQuoteDocument, saveAsCustomerDocument);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate status - cannot send REJECTED, EXPIRED, or CANCELLED quotes
            if (quote.getStatus() == QuoteStatus.REJECTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send REJECTED quote", "INVALID_STATUS_TRANSITION")
                );
            }
            if (quote.getStatus() == QuoteStatus.EXPIRED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send EXPIRED quote", "INVALID_STATUS_TRANSITION")
                );
            }
            if (quote.getStatus() == QuoteStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send CANCELLED quote", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate completeness if not already sent
            if (quote.getStatus() == QuoteStatus.DRAFT && !canMarkAsReady(quote)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Quote does not meet requirements to send. Must have itinerary, customer, and line items.",
                        "INCOMPLETE_QUOTE"
                    )
                );
            }

            // Validate email template ID belongs to SEND_QUOTE event (upfront, before status change)
            Long emailTemplateId = null;
            if (emailTemplateIdObfuscated != null && !emailTemplateIdObfuscated.isBlank()) {
                try {
                    emailTemplateId = idObfuscator.decodeId(emailTemplateIdObfuscated);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid email template ID format", "INVALID_EMAIL_TEMPLATE_ID")
                    );
                }
                EmailEvent quoteSentEvent = emailEventRepository.findByName("SEND_QUOTE").orElse(null);
                if (quoteSentEvent == null) {
                    return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "SEND_QUOTE email event not configured", "EMAIL_EVENT_NOT_FOUND")
                    );
                }
                EmailTemplate emailTemplate = emailTemplateRepository.findById(emailTemplateId).orElse(null);
                if (emailTemplate == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Email template not found", "EMAIL_TEMPLATE_NOT_FOUND")
                    );
                }
                if (!emailTemplate.getEmailEvent().getId().equals(quoteSentEvent.getId())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Email template belongs to '" + emailTemplate.getEmailEvent().getName() + "', not SEND_QUOTE",
                            "EMAIL_TEMPLATE_EVENT_MISMATCH")
                    );
                }
                if (!emailTemplate.getEnabled()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Email template is disabled: " + emailTemplate.getName(), "EMAIL_TEMPLATE_DISABLED")
                    );
                }
            }

            // Validate PDF template ID belongs to FULL_QUOTE document type (upfront)
            if (pdfTemplateIdObfuscated != null && !pdfTemplateIdObfuscated.isBlank()) {
                Long pdfTemplateId;
                try {
                    pdfTemplateId = idObfuscator.decodeId(pdfTemplateIdObfuscated);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid PDF template ID format", "INVALID_PDF_TEMPLATE_ID")
                    );
                }
                PdfDocument fullQuoteDoc = pdfDocumentRepository.findByName("FULL_QUOTE").orElse(null);
                if (fullQuoteDoc == null) {
                    return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "FULL_QUOTE PDF document type not configured", "PDF_DOCUMENT_NOT_FOUND")
                    );
                }
                PdfTemplate pdfTemplate = pdfTemplateRepository.findById(pdfTemplateId).orElse(null);
                if (pdfTemplate == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "PDF template not found", "PDF_TEMPLATE_NOT_FOUND")
                    );
                }
                if (!pdfTemplate.getPdfDocument().getId().equals(fullQuoteDoc.getId())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "PDF template belongs to '" + pdfTemplate.getPdfDocument().getName() + "', not FULL_QUOTE",
                            "PDF_TEMPLATE_DOCUMENT_MISMATCH")
                    );
                }
                if (!pdfTemplate.getEnabled()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "PDF template is disabled: " + pdfTemplate.getName(), "PDF_TEMPLATE_DISABLED")
                    );
                }
            }

            // Update status and sent date
            quote.setStatus(QuoteStatus.SENT);
            if (quote.getSentDate() == null) {
                quote.setSentDate(LocalDate.now());
            }
            quote = quoteRepository.save(quote);

            log.info("Quote sent to customer: {}", quote.getQuoteCode());

            // Resolve language: API param > customer preference > default "en"
            String resolvedLanguage = language;
            if (resolvedLanguage == null || resolvedLanguage.isBlank()) {
                resolvedLanguage = quote.getCustomer().getPreferredLanguage();
            }

            // Validate language is translatable (system + provider support), fallback to "en"
            if (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)) {
                if (!translationService.canTranslate(resolvedLanguage)) {
                    log.warn("Language '{}' is not supported for translation, falling back to English", resolvedLanguage);
                    resolvedLanguage = "en";
                }
            }

            // Send email notification to customer
            sendQuoteEmailToCustomer(quote, resolvedLanguage, emailTemplateId,
                pdfTemplateIdObfuscated, saveAsQuoteDocument, saveAsCustomerDocument);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote sent to customer successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error sending quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send quote", "SEND_FAILED")
            );
        }
    }

    /**
     * Mark quote as ACCEPTED by customer
     * Only allowed from SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "ACCEPT_QUOTE", description = "Marking quote as accepted", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> acceptQuote(String idObfuscated) {
        log.info("Accepting quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status
            if (quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT quotes can be marked as ACCEPTED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.ACCEPTED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as accepted: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as accepted", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error accepting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to accept quote", "ACCEPT_FAILED")
            );
        }
    }

    /**
     * Mark quote as REJECTED by customer
     * Only allowed from SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "REJECT_QUOTE", description = "Marking quote as rejected", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> rejectQuote(String idObfuscated) {
        log.info("Rejecting quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status
            if (quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT quotes can be marked as REJECTED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.REJECTED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as rejected: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as rejected", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error rejecting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reject quote", "REJECT_FAILED")
            );
        }
    }

    /**
     * Mark quote as EXPIRED
     * Allowed from SENT or READY status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "EXPIRE_QUOTE", description = "Marking quote as expired", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> expireQuote(String idObfuscated) {
        log.info("Expiring quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status - can only expire SENT or READY quotes
            if (quote.getStatus() != QuoteStatus.SENT && quote.getStatus() != QuoteStatus.READY) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT or READY quotes can be marked as EXPIRED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.EXPIRED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as expired: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as expired", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error expiring quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to expire quote", "EXPIRE_FAILED")
            );
        }
    }

    /**
     * Cancel a quote
     * Allowed from any status except CONVERTED
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "CANCEL_QUOTE", description = "Cancelling quote", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> cancelQuote(String idObfuscated) {
        log.info("Cancelling quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Cannot cancel CONVERTED quotes
            if (quote.getStatus() == QuoteStatus.CONVERTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot cancel CONVERTED quote", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.CANCELLED);
            quote = quoteRepository.save(quote);

            log.info("Quote cancelled: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote cancelled successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error cancelling quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to cancel quote", "CANCEL_FAILED")
            );
        }
    }

    /**
     * Revert quote to DRAFT status
     * Allowed from READY or SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "REVERT_QUOTE_TO_DRAFT", description = "Reverting quote to draft", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> revertToDraft(String idObfuscated) {
        log.info("Reverting quote to draft: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status
            if (quote.getStatus() == QuoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quote is already in DRAFT status", "ALREADY_DRAFT")
                );
            }

            // Only READY and SENT can be reverted to DRAFT
            if (quote.getStatus() != QuoteStatus.READY && quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot revert " + quote.getStatus().getDisplayName() + " quote to DRAFT. Create a new version instead.",
                        "INVALID_STATUS_TRANSITION"
                    )
                );
            }

            // Update status to DRAFT
            quote.setStatus(QuoteStatus.DRAFT);
            quote = quoteRepository.save(quote);

            log.info("Quote reverted to draft: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote reverted to draft successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error reverting quote to draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to revert quote to draft", "REVERT_TO_DRAFT_FAILED")
            );
        }
    }

    /**
     * Convert quote to booking/safari.
     * Creates a Safari (DRAFT) from the quote's itinerary and customer.
     * Invoice is NOT created here — it is created later when the safari reaches
     * CONFIRMED state via the request-payment endpoint.
     * Only allowed from ACCEPTED status.
     *
     * @param idObfuscated The obfuscated quote ID
     * @param startDate Optional safari start date override (uses quote.safariStartDate if not provided)
     * @return ResponseEntity with ApiResponse containing conversion results
     */
    @AuditLogAnnotation(action = "CONVERT_QUOTE", description = "Converting quote to booking", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> convertQuote(String idObfuscated, LocalDate startDate) {
        log.info("Converting quote to booking: {} (startDate: {})", idObfuscated, startDate);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Block superseded quotes (has a newer version)
            ResponseEntity<ApiResponse<?>> supersededCheck = checkSuperseded(quote);
            if (supersededCheck != null) return supersededCheck;

            // Validate current status
            if (quote.getStatus() != QuoteStatus.ACCEPTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only ACCEPTED quotes can be converted to bookings", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate itinerary exists and is published
            if (quote.getItinerary() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot convert quote - no itinerary linked", "NO_ITINERARY")
                );
            }
            if (quote.getItinerary().getStatus() != ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot convert quote - itinerary is not PUBLISHED", "ITINERARY_NOT_PUBLISHED")
                );
            }

            // Validate customer exists
            if (quote.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot convert quote - no customer linked", "NO_CUSTOMER")
                );
            }

            // Resolve start date: API param override > quote's safariStartDate
            LocalDate resolvedStartDate = startDate;
            if (resolvedStartDate == null) {
                resolvedStartDate = quote.getSafariStartDate();
            }
            if (resolvedStartDate == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Start date is required. This quote has no safariStartDate stored — provide startDate parameter.",
                        "START_DATE_REQUIRED")
                );
            }
            if (resolvedStartDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Safari start date " + resolvedStartDate + " is in the past. Provide a future startDate parameter to override.",
                        "START_DATE_IN_PAST")
                );
            }

            // ============================================================
            // Step 1: Create Safari — snapshot from Quote's own day-tree if
            // it has one; fall back to the Itinerary template for legacy
            // quotes that pre-date snapshot mode.
            // ============================================================
            ResponseEntity<ApiResponse<?>> safariResponse;
            boolean hasQuoteSnapshot = quote.getDays() != null && !quote.getDays().isEmpty();

            if (hasQuoteSnapshot) {
                log.info("Converting quote {} via Quote→Safari snapshot path", quote.getQuoteCode());
                safariResponse = safariCreateService.createSafariFromQuote(quote.getId(), resolvedStartDate);
            } else {
                log.info("Converting quote {} via legacy Itinerary→Safari path", quote.getQuoteCode());
                String itineraryObfuscatedId = idObfuscator.encodeId(quote.getItinerary().getId());
                String customerObfuscatedId = idObfuscator.encodeId(quote.getCustomer().getId());

                CreateSafariFromItineraryDTO safariDTO = new CreateSafariFromItineraryDTO();
                safariDTO.setItineraryId(itineraryObfuscatedId);
                safariDTO.setCustomerId(customerObfuscatedId);
                safariDTO.setStartDate(resolvedStartDate);
                safariDTO.setName(quote.getTitle() != null ? quote.getTitle() : quote.getItinerary().getName());
                safariDTO.setDescription(quote.getDescription());

                safariResponse = safariCreateService.createSafariFromItinerary(safariDTO);
            }

            if (!safariResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Safari creation failed during quote conversion for quote {}", quote.getQuoteCode());
                return ResponseEntity.status(safariResponse.getStatusCode()).body(
                    ApiResponse.error(
                        safariResponse.getStatusCode().value(),
                        "Quote conversion failed - could not create safari: " +
                            (safariResponse.getBody() != null ? safariResponse.getBody().getMessage() : "Unknown error"),
                        "SAFARI_CREATION_FAILED"
                    )
                );
            }

            // Extract created safari ID from response
            SafariDTO createdSafari = null;
            if (safariResponse.getBody() != null && safariResponse.getBody().getData() instanceof SafariDTO) {
                createdSafari = (SafariDTO) safariResponse.getBody().getData();
            }

            if (createdSafari == null || createdSafari.getId() == null) {
                log.error("Safari created but could not extract ID for quote {}", quote.getQuoteCode());
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Safari created but could not extract details", "SAFARI_EXTRACTION_FAILED")
                );
            }

            log.info("Safari created from quote {}: {} ({})", quote.getQuoteCode(), createdSafari.getCode(), createdSafari.getId());

            // ============================================================
            // Step 2: Update quote status to CONVERTED
            // ============================================================
            quote.setStatus(QuoteStatus.CONVERTED);
            quote = quoteRepository.save(quote);
            log.info("Quote status set to CONVERTED: {}", quote.getQuoteCode());

            // ============================================================
            // Step 3: Build response
            // ============================================================
            Map<String, Object> result = new HashMap<>();
            result.put("quote", convertToDTO(quote));
            result.put("safari", createdSafari);

            return ResponseEntity.ok().body(
                ApiResponse.success(200,
                    "Quote converted to booking successfully. Safari created in DRAFT state. Invoice will be created when payment is requested.",
                    result)
            );

        } catch (Exception e) {
            log.error("Error converting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to convert quote", "CONVERT_FAILED")
            );
        }
    }

    /**
     * Check if quote is superseded (has a next version).
     * Superseded quotes are locked — no workflow transitions allowed.
     */
    private ResponseEntity<ApiResponse<?>> checkSuperseded(Quote quote) {
        if (quote.getNextVersion() != null) {
            String nextCode = quote.getNextVersion().getQuoteCode();
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "This quote has been superseded by " + nextCode + ". All workflow actions must be performed on the latest version.",
                    "QUOTE_SUPERSEDED")
            );
        }
        return null;
    }

    /**
     * Check if quote meets requirements to be marked as READY
     */
    private boolean canMarkAsReady(Quote quote) {
        return quote.getItinerary() != null
            && quote.getCustomer() != null
            && quote.getItems() != null
            && !quote.getItems().isEmpty()
            && quote.getGrandTotals() != null
            && !quote.getGrandTotals().isEmpty();
    }

    /**
     * Convert Quote entity to QuoteDTO
     */
    private QuoteDTO convertToDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.builder()
            .id(idObfuscator.encodeId(quote.getId()))
            .quoteCode(quote.getQuoteCode())
            .title(quote.getTitle())
            .description(quote.getDescription())
            .itineraryId(idObfuscator.encodeId(quote.getItinerary().getId()))
            .itineraryCode(quote.getItinerary().getCode())
            .itineraryName(quote.getItinerary().getName())
            .customerId(idObfuscator.encodeId(quote.getCustomer().getId()))
            .customerName(quote.getCustomer().getDisplayName())
            .customerEmail(quote.getCustomer().getPrimaryEmail())
            .subtotals(quote.getSubtotals())
            .taxes(quote.getTaxes())
            .discounts(quote.getDiscounts())
            .grandTotals(quote.getGrandTotals())
            .isStoRate(quote.getIsStoRate())
            .taxPercentage(quote.getTaxPercentage())
            .discountPercentage(quote.getDiscountPercentage())
            .discountReason(quote.getDiscountReason())
            .version(quote.getVersion())
            .status(quote.getStatus())
            .safariStartDate(quote.getSafariStartDate())
            .sentDate(quote.getSentDate())
            .validFrom(quote.getValidFrom())
            .validTo(quote.getValidTo())
            .isValid(quote.getIsValid())
            .depositPercentage(quote.getDepositPercentage())
            .depositDueDate(quote.getDepositDueDate())
            .fullPaymentDueDate(quote.getFullPaymentDueDate())
            .internalNotes(quote.getInternalNotes())
            .customerNotes(quote.getCustomerNotes())
            .versionNotes(quote.getVersionNotes())
            .isActive(quote.getIsActive())
            .itemCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentCount(quote.getDocuments() != null ? (long) quote.getDocuments().size() : 0L)
            .createdAt(quote.getCreatedAt())
            .updatedAt(quote.getUpdatedAt())
            .build();

        // Set approver if present
        if (quote.getApprover() != null) {
            dto.setApproverId(idObfuscator.encodeId(quote.getApprover().getId()));
            dto.setApproverName(quote.getApprover().getUsername());
        }

        // Set approved by if present
        if (quote.getApprovedBy() != null) {
            dto.setApprovedById(idObfuscator.encodeId(quote.getApprovedBy().getId()));
            dto.setApprovedByName(quote.getApprovedBy().getUsername());
            dto.setApprovedAt(quote.getApprovedAt());
            dto.setApprovalNotes(quote.getApprovalNotes());
        }

        // Set previous version if present
        if (quote.getPreviousVersion() != null) {
            dto.setPreviousVersionId(idObfuscator.encodeId(quote.getPreviousVersion().getId()));
            dto.setPreviousVersionCode(quote.getPreviousVersion().getQuoteCode());
        }

        // Set next version if present
        if (quote.getNextVersion() != null) {
            dto.setNextVersionId(idObfuscator.encodeId(quote.getNextVersion().getId()));
            dto.setNextVersionCode(quote.getNextVersion().getQuoteCode());
        }

        // Set created by if present
        if (quote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(quote.getCreatedBy().getId()));
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (quote.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(quote.getUpdatedBy().getId()));
            dto.setUpdatedByName(quote.getUpdatedBy().getUsername());
        }

        return dto;
    }

    /**
     * Send quote email notification to the customer asynchronously.
     * Uses the SEND_QUOTE email event template and the customer's primary email.
     * Both the email body and the PDF attachment are translated to the specified language.
     * Failures are logged but do not block the status transition.
     *
     * @param quote The quote entity
     * @param language Language code for translation (e.g., "fr", "sw"). English ("en") skips translation.
     * @param emailTemplateId Optional specific template ID to use. Must belong to SEND_QUOTE event. Null uses the default.
     * @param pdfTemplateIdObfuscated Optional obfuscated PDF template ID for quote PDF generation
     * @param saveAsQuoteDocument Whether to save the PDF as a quote document
     * @param saveAsCustomerDocument Whether to save the PDF as a customer document
     */
    private void sendQuoteEmailToCustomer(Quote quote, String language, Long emailTemplateId,
                                           String pdfTemplateIdObfuscated, boolean saveAsQuoteDocument, boolean saveAsCustomerDocument) {
        try {
            String customerEmail = quote.getCustomer().getPrimaryEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                log.warn("Cannot send quote email - customer {} has no email address", quote.getCustomer().getDisplayName());
                return;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

            Map<String, String> variables = new HashMap<>();
            variables.put("customerName", quote.getCustomer().getDisplayName());
            variables.put("quoteCode", quote.getQuoteCode());
            variables.put("quoteTitle", quote.getTitle() != null ? quote.getTitle() : "");
            variables.put("sentDate", LocalDate.now().format(dateFormatter));
            variables.put("safariStartDate", quote.getSafariStartDate() != null
                    ? quote.getSafariStartDate().format(dateFormatter) : "");

            // Itinerary details
            Itinerary itinerary = quote.getItinerary();
            if (itinerary != null) {
                variables.put("itineraryName", itinerary.getName() != null ? itinerary.getName() : "");
                variables.put("itineraryCode", itinerary.getCode() != null ? itinerary.getCode() : "");
                variables.put("tripType", itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : "");
                variables.put("totalDays", itinerary.getTotalDays() != null ? String.valueOf(itinerary.getTotalDays()) : "");
                variables.put("totalNights", itinerary.getTotalNights() != null ? String.valueOf(itinerary.getTotalNights()) : "");
                variables.put("startLocation", itinerary.getStartLocation() != null ? itinerary.getStartLocation() : "");
                variables.put("endLocation", itinerary.getEndLocation() != null ? itinerary.getEndLocation() : "");
            } else {
                variables.put("itineraryName", "");
                variables.put("itineraryCode", "");
                variables.put("tripType", "");
                variables.put("totalDays", "");
                variables.put("totalNights", "");
                variables.put("startLocation", "");
                variables.put("endLocation", "");
            }

            // Grand total (first currency available)
            variables.put("grandTotal", formatGrandTotal(quote));

            // Items summary as HTML table
            variables.put("itemsSummary", buildItemsSummaryHtml(quote));

            // Validity dates
            variables.put("validFrom", quote.getValidFrom() != null ? quote.getValidFrom().format(dateFormatter) : "");
            variables.put("validTo", quote.getValidTo() != null ? quote.getValidTo().format(dateFormatter) : "");

            // Payment terms
            variables.put("depositPercentage", quote.getDepositPercentage() != null
                    ? quote.getDepositPercentage().stripTrailingZeros().toPlainString() + "%" : "");
            variables.put("depositDueDate", quote.getDepositDueDate() != null
                    ? quote.getDepositDueDate().format(dateFormatter) : "");
            variables.put("fullPaymentDueDate", quote.getFullPaymentDueDate() != null
                    ? quote.getFullPaymentDueDate().format(dateFormatter) : "");

            // Customer notes
            variables.put("customerNotes", quote.getCustomerNotes() != null ? quote.getCustomerNotes() : "");

            String subject = "Your Safari Quote: " + quote.getQuoteCode() + " - " + quote.getTitle();
            String quoteCode = quote.getQuoteCode();
            String obfuscatedId = idObfuscator.encodeId(quote.getId());

            // Generate PDF for attachment (translated if non-English, using specified PDF template if provided)
            byte[] pdfBytes = null;
            String pdfFileName = null;
            String pdfLanguage = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language)) ? language : null;
            try {
                ResponseEntity<?> pdfResponse = quotePdfGenerationService.generateQuotePdf(obfuscatedId, pdfTemplateIdObfuscated, pdfLanguage);
                if (pdfResponse.getStatusCode().is2xxSuccessful() && pdfResponse.getBody() instanceof byte[]) {
                    pdfBytes = (byte[]) pdfResponse.getBody();
                    pdfFileName = "Quote_" + quoteCode + ".pdf";
                    log.info("Generated PDF attachment for quote {}: {} bytes", quoteCode, pdfBytes.length);
                } else {
                    log.warn("PDF generation returned non-success for quote {}, sending email without attachment", quoteCode);
                }
            } catch (Exception e) {
                log.warn("Failed to generate PDF for quote {}, sending email without attachment: {}", quoteCode, e.getMessage());
            }

            // Save PDF as quote document
            if (pdfBytes != null && saveAsQuoteDocument) {
                try {
                    String docTitle = "Quote PDF - " + quoteCode;
                    quoteDocumentCreateService.saveGeneratedDocument(
                        quote.getId(), pdfBytes, pdfFileName,
                        QuoteDocument.DocumentType.QUOTE_PDF, docTitle
                    );
                    log.info("Saved PDF as quote document for quote {}", quoteCode);
                } catch (Exception e) {
                    log.warn("Failed to save PDF as quote document for {}: {}", quoteCode, e.getMessage());
                }
            }

            // Save PDF as customer document
            if (pdfBytes != null && saveAsCustomerDocument) {
                try {
                    String docTitle = "Quote " + quoteCode + " - " + (quote.getTitle() != null ? quote.getTitle() : "Safari Quote");
                    customerDocumentCreateService.saveGeneratedDocument(
                        quote.getCustomer().getId(), pdfBytes, pdfFileName,
                        CustomerDocument.DocumentType.CONTRACT, docTitle
                    );
                    log.info("Saved PDF as customer document for customer {} (quote {})",
                        quote.getCustomer().getDisplayName(), quoteCode);
                } catch (Exception e) {
                    log.warn("Failed to save PDF as customer document for {}: {}", quoteCode, e.getMessage());
                }
            }

            // Dispatch rendering + sending asynchronously
            final byte[] attachmentBytes = pdfBytes;
            final String attachmentName = pdfFileName;
            final String emailLanguage = language;
            final Long templateId = emailTemplateId;
            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = templateId != null
                        ? emailTemplateRenderer.renderTemplate("SEND_QUOTE", templateId, variables)
                        : emailTemplateRenderer.renderTemplate("SEND_QUOTE", variables);

                    // Translate email body and subject if non-English
                    String finalSubject = subject;
                    if (emailLanguage != null && !emailLanguage.isBlank() && !"en".equalsIgnoreCase(emailLanguage)) {
                        try {
                            String translatedHtml = translationService.translateHtml(renderedHtml, emailLanguage);
                            if (translatedHtml != null && !translatedHtml.isBlank()) {
                                renderedHtml = translatedHtml;
                            }
                            String translatedSubject = translationService.translatePlainText(subject, "en", emailLanguage);
                            if (translatedSubject != null && !translatedSubject.isBlank()) {
                                finalSubject = translatedSubject;
                            }
                            log.info("Email body and subject translated to {} for quote {}", emailLanguage, quoteCode);
                        } catch (Exception e) {
                            log.warn("Email translation to {} failed for quote {}, sending in English: {}", emailLanguage, quoteCode, e.getMessage());
                        }
                    }

                    if (attachmentBytes != null && attachmentBytes.length > 0) {
                        emailSendingService.sendHtmlEmailWithAttachment(customerEmail, finalSubject, renderedHtml, attachmentBytes, attachmentName);
                        log.info("Quote email with PDF sent to customer {} for quote {}", customerEmail, quoteCode);
                    } else {
                        emailSendingService.sendHtmlEmail(customerEmail, finalSubject, renderedHtml);
                        log.info("Quote email (without PDF) sent to customer {} for quote {}", customerEmail, quoteCode);
                    }
                } catch (Exception e) {
                    log.warn("Failed to send quote email for {}: {}", quoteCode, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("Failed to prepare quote email for {}: {}", quote.getQuoteCode(), e.getMessage());
        }
    }

    /**
     * Format the grand total from the first available currency for display
     */
    private String formatGrandTotal(Quote quote) {
        if (quote.getGrandTotals() == null || quote.getGrandTotals().isEmpty()) {
            return "";
        }
        Price firstTotal = quote.getGrandTotals().get(0);
        BigDecimal amount = firstTotal.getTotalPrice() != null ? firstTotal.getTotalPrice() : BigDecimal.ZERO;
        return firstTotal.getCurrency() + " " + String.format("%,.2f", amount);
    }

    /**
     * Build an HTML summary of quote line items for the email template.
     * Groups active items with their first price displayed.
     */
    private String buildItemsSummaryHtml(Quote quote) {
        if (quote.getItems() == null || quote.getItems().isEmpty()) {
            return "";
        }

        var activeItems = quote.getItems().stream()
            .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
            .sorted((a, b) -> {
                int orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
                int orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
                return Integer.compare(orderA, orderB);
            })
            .toList();

        if (activeItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"details-box\">");
        sb.append("<h3>Cost Breakdown</h3>");

        for (var item : activeItems) {
            String name = item.getItemName() != null ? item.getItemName() : "Item";
            String priceStr = "";
            if (item.getPrices() != null && !item.getPrices().isEmpty()) {
                Price p = item.getPrices().get(0);
                BigDecimal total = p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO;
                priceStr = p.getCurrency() + " " + String.format("%,.2f", total);
            }
            sb.append("<div class=\"detail-row\">");
            sb.append("<span class=\"detail-label\">").append(name).append("</span>");
            sb.append("<span class=\"detail-value\">").append(priceStr).append("</span>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }
}
