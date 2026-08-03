package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.SafariPdfGenerationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SafariCustomerEmailService - Sends safari-related emails to customers.
 *
 * Two email types:
 * 1. SEND_SAFARI_DETAILS: Structured email with full safari details, optional PDF attachment
 * 2. SEND_SAFARI_MESSAGE: Freeform message about the safari
 *
 * Both support: language translation, email template selection, PDF template selection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariCustomerEmailService {

    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final SafariPdfGenerationService safariPdfGenerationService;
    private final TranslationService translationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Send structured safari details email to the customer.
     *
     * @param safariIdObfuscated Safari ID
     * @param language Optional language for translation
     * @param emailTemplateId Optional specific email template (must belong to SEND_SAFARI_DETAILS)
     * @param pdfTemplateIdObfuscated Optional PDF template for attachment
     * @param attachPdf Whether to attach the safari PDF
     * @return ResponseEntity with result
     */
    public ResponseEntity<ApiResponse<?>> sendSafariDetails(
            String safariIdObfuscated, String language,
            Long emailTemplateId, String pdfTemplateIdObfuscated, boolean attachPdf) {

        log.info("Sending safari details to customer: {} (language: {}, attachPdf: {})", safariIdObfuscated, language, attachPdf);

        try {
            Long id = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariRepository.findById(id).orElse(null);

            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            if (safari.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari has no customer linked", "NO_CUSTOMER")
                );
            }

            String customerEmail = safari.getCustomer().getPrimaryEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer has no email address", "NO_CUSTOMER_EMAIL")
                );
            }

            // Resolve language
            String resolvedLanguage = language;
            if (resolvedLanguage == null || resolvedLanguage.isBlank()) {
                resolvedLanguage = safari.getCustomer().getPreferredLanguage();
            }
            if (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)
                    && !translationService.canTranslate(resolvedLanguage)) {
                resolvedLanguage = "en";
            }

            // Build variables
            Map<String, String> variables = buildSafariDetailsVariables(safari);

            // Generate PDF if requested
            byte[] pdfBytes = null;
            String pdfFileName = null;
            if (attachPdf) {
                String pdfLang = (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)) ? resolvedLanguage : null;
                try {
                    ResponseEntity<?> pdfResponse = safariPdfGenerationService.generateSafariPdf(
                        safariIdObfuscated, pdfTemplateIdObfuscated, pdfLang);
                    if (pdfResponse.getStatusCode().is2xxSuccessful() && pdfResponse.getBody() instanceof byte[]) {
                        pdfBytes = (byte[]) pdfResponse.getBody();
                        pdfFileName = "Safari_" + safari.getCode() + ".pdf";
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate PDF for safari {}, sending without attachment: {}", safari.getCode(), e.getMessage());
                }
            }

            String subject = "Your Safari Details: " + safari.getCode() + " — " + safari.getName();
            String safariCode = safari.getCode();

            final byte[] finalPdfBytes = pdfBytes;
            final String finalPdfFileName = pdfFileName;
            final String emailLang = resolvedLanguage;
            final Long templateId = emailTemplateId;

            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = templateId != null
                        ? emailTemplateRenderer.renderTemplate("SEND_SAFARI_DETAILS", templateId, variables)
                        : emailTemplateRenderer.renderTemplate("SEND_SAFARI_DETAILS", variables);

                    String finalSubject = subject;

                    if (emailLang != null && !"en".equalsIgnoreCase(emailLang)) {
                        try {
                            String translatedHtml = translationService.translateHtml(renderedHtml, emailLang);
                            if (translatedHtml != null && !translatedHtml.isBlank()) renderedHtml = translatedHtml;
                            String translatedSubject = translationService.translatePlainText(subject, "en", emailLang);
                            if (translatedSubject != null && !translatedSubject.isBlank()) finalSubject = translatedSubject;
                        } catch (Exception e) {
                            log.warn("Translation failed for safari {}, sending in English", safariCode);
                        }
                    }

                    if (finalPdfBytes != null && finalPdfBytes.length > 0) {
                        emailSendingService.sendHtmlEmailWithAttachment(customerEmail, finalSubject, renderedHtml, finalPdfBytes, finalPdfFileName);
                    } else {
                        emailSendingService.sendHtmlEmail(customerEmail, finalSubject, renderedHtml);
                    }
                    log.info("Safari details email sent to {} for safari {}", customerEmail, safariCode);
                } catch (Exception e) {
                    log.error("Failed to send safari details email for {} to {}", safariCode, customerEmail, e);
                }
            });

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari details email sent to " + customerEmail)
            );

        } catch (Exception e) {
            log.error("Error sending safari details email", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to send safari details email: " + e.getMessage(), "EMAIL_SEND_FAILED")
            );
        }
    }

    /**
     * Send freeform message to the customer about their safari.
     *
     * @param safariIdObfuscated Safari ID
     * @param messageSubject The message subject/heading
     * @param messageBody The message content (HTML supported)
     * @param language Optional language for translation
     * @param emailTemplateId Optional specific email template (must belong to SEND_SAFARI_MESSAGE)
     * @return ResponseEntity with result
     */
    public ResponseEntity<ApiResponse<?>> sendCustomerMessage(
            String safariIdObfuscated, String messageSubject, String messageBody,
            String language, Long emailTemplateId) {

        log.info("Sending custom message to customer for safari: {}", safariIdObfuscated);

        try {
            Long id = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariRepository.findById(id).orElse(null);

            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            if (safari.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari has no customer linked", "NO_CUSTOMER")
                );
            }

            String customerEmail = safari.getCustomer().getPrimaryEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer has no email address", "NO_CUSTOMER_EMAIL")
                );
            }

            if (messageSubject == null || messageSubject.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Message subject is required", "SUBJECT_REQUIRED")
                );
            }

            if (messageBody == null || messageBody.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Message body is required", "BODY_REQUIRED")
                );
            }

            // Resolve language
            String resolvedLanguage = language;
            if (resolvedLanguage == null || resolvedLanguage.isBlank()) {
                resolvedLanguage = safari.getCustomer().getPreferredLanguage();
            }
            if (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)
                    && !translationService.canTranslate(resolvedLanguage)) {
                resolvedLanguage = "en";
            }

            // Build variables
            Map<String, String> variables = new HashMap<>();
            variables.put("customerName", safari.getCustomer().getDisplayName());
            variables.put("safariCode", safari.getCode());
            variables.put("safariName", safari.getName());
            variables.put("startDate", safari.getStartDate() != null ? safari.getStartDate().format(DATE_FMT) : "");
            variables.put("endDate", safari.getEndDate() != null ? safari.getEndDate().format(DATE_FMT) : "");
            variables.put("messageSubject", messageSubject);
            variables.put("messageBody", messageBody);
            variables.put("sentDate", LocalDate.now().format(DATE_FMT));
            variables.put("senderName", "Kabengo Safaris Team");

            String emailSubject = safari.getCode() + " — " + messageSubject;
            String safariCode = safari.getCode();
            final String emailLang = resolvedLanguage;
            final Long templateId = emailTemplateId;

            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = templateId != null
                        ? emailTemplateRenderer.renderTemplate("SEND_SAFARI_MESSAGE", templateId, variables)
                        : emailTemplateRenderer.renderTemplate("SEND_SAFARI_MESSAGE", variables);

                    String finalSubject = emailSubject;

                    if (emailLang != null && !"en".equalsIgnoreCase(emailLang)) {
                        try {
                            String translatedHtml = translationService.translateHtml(renderedHtml, emailLang);
                            if (translatedHtml != null && !translatedHtml.isBlank()) renderedHtml = translatedHtml;
                            String translatedSubject = translationService.translatePlainText(emailSubject, "en", emailLang);
                            if (translatedSubject != null && !translatedSubject.isBlank()) finalSubject = translatedSubject;
                        } catch (Exception e) {
                            log.warn("Translation failed for safari message {}, sending in English", safariCode);
                        }
                    }

                    emailSendingService.sendHtmlEmail(customerEmail, finalSubject, renderedHtml);
                    log.info("Custom message sent to {} for safari {}", customerEmail, safariCode);
                } catch (Exception e) {
                    log.warn("Failed to send custom message for {}: {}", safariCode, e.getMessage());
                }
            });

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Message sent to " + customerEmail)
            );

        } catch (Exception e) {
            log.error("Error sending customer message", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to send message: " + e.getMessage(), "EMAIL_SEND_FAILED")
            );
        }
    }

    // ============================================================
    // Variable builders
    // ============================================================

    private Map<String, String> buildSafariDetailsVariables(Safari safari) {
        Map<String, String> v = new HashMap<>();
        v.put("customerName", safari.getCustomer().getDisplayName());
        v.put("safariCode", safari.getCode());
        v.put("safariName", safari.getName());
        v.put("startDate", safari.getStartDate() != null ? safari.getStartDate().format(DATE_FMT) : "");
        v.put("endDate", safari.getEndDate() != null ? safari.getEndDate().format(DATE_FMT) : "");
        v.put("totalDays", String.valueOf(safari.getTotalDays()));
        v.put("totalNights", String.valueOf(safari.getTotalNights()));
        v.put("startLocation", safari.getStartLocation() != null ? safari.getStartLocation() : "");
        v.put("endLocation", safari.getEndLocation() != null ? safari.getEndLocation() : "");
        v.put("itineraryName", safari.getItinerary() != null ? safari.getItinerary().getName() : "");
        v.put("state", safari.getState() != null ? safari.getState().getDisplayName() : "");
        v.put("specialRequests", safari.getSpecialRequests() != null ? safari.getSpecialRequests() : "");
        v.put("emergencyContact", safari.getEmergencyContact() != null ? safari.getEmergencyContact() : "");
        v.put("sentDate", LocalDate.now().format(DATE_FMT));

        // Build day summary HTML
        v.put("daySummary", buildDaySummaryHtml(safari));

        return v;
    }

    private String buildDaySummaryHtml(Safari safari) {
        if (safari.getDays() == null || safari.getDays().isEmpty()) {
            return "";
        }

        var sortedDays = safari.getDays().stream()
            .sorted(Comparator.comparingInt(SafariDay::getDayNumber))
            .toList();

        StringBuilder sb = new StringBuilder();
        for (SafariDay day : sortedDays) {
            sb.append("<div class=\"detail-row\">");
            sb.append("<span class=\"detail-label\">Day ").append(day.getDayNumber());
            if (day.getActualDate() != null) {
                sb.append(" (").append(day.getActualDate().format(DateTimeFormatter.ofPattern("MMM dd"))).append(")");
            }
            sb.append("</span>");
            sb.append("<span class=\"detail-value\">").append(day.getTitle() != null ? day.getTitle() : "").append("</span>");
            sb.append("</div>");
        }

        return sb.toString();
    }
}
