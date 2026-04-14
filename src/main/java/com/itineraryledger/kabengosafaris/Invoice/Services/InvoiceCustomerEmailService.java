package com.itineraryledger.kabengosafaris.Invoice.Services;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.InvoicePdfGenerationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * InvoiceCustomerEmailService - Sends invoice email to customer with optional PDF attachment.
 *
 * Uses SEND_INVOICE email event and FULL_INVOICE PDF document type.
 * Supports language translation for both the email body and the attached PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceCustomerEmailService {

    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final InvoicePdfGenerationService invoicePdfGenerationService;
    private final TranslationService translationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Send invoice email to the customer with optional PDF attachment.
     *
     * @param invoice                The invoice entity (already validated and saved as SENT)
     * @param invoiceIdObfuscated    Obfuscated invoice ID (for PDF generation)
     * @param language               Optional language code for translation
     * @param emailTemplateId        Optional specific email template ID (must belong to SEND_INVOICE event)
     * @param pdfTemplateIdObfuscated Optional PDF template ID for FULL_INVOICE
     * @param attachPdf              Whether to attach the invoice PDF
     */
    public void sendInvoiceEmail(
            Invoice invoice,
            String invoiceIdObfuscated,
            String language,
            Long emailTemplateId,
            String pdfTemplateIdObfuscated,
            boolean attachPdf) {

        // Validate customer and email
        if (invoice.getCustomer() == null) {
            log.warn("Cannot send invoice email - no customer linked to invoice {}", invoice.getInvoiceCode());
            return;
        }

        String customerEmail = invoice.getCustomer().getPrimaryEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Cannot send invoice email - customer has no email for invoice {}", invoice.getInvoiceCode());
            return;
        }

        // Resolve language
        String resolvedLanguage = language;
        if (resolvedLanguage == null || resolvedLanguage.isBlank()) {
            resolvedLanguage = invoice.getCustomer().getPreferredLanguage();
        }
        if (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)
                && !translationService.canTranslate(resolvedLanguage)) {
            resolvedLanguage = "en";
        }

        // Build email variables
        Map<String, String> variables = buildInvoiceEmailVariables(invoice);

        // Generate PDF if requested
        byte[] pdfBytes = null;
        String pdfFileName = null;
        if (attachPdf) {
            String pdfLang = (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)) ? resolvedLanguage : null;
            try {
                ResponseEntity<?> pdfResponse = invoicePdfGenerationService.generateInvoicePdf(
                    invoiceIdObfuscated, pdfTemplateIdObfuscated, pdfLang);
                if (pdfResponse.getStatusCode().is2xxSuccessful() && pdfResponse.getBody() instanceof byte[]) {
                    pdfBytes = (byte[]) pdfResponse.getBody();
                    pdfFileName = "Invoice_" + invoice.getInvoiceCode() + ".pdf";
                }
            } catch (Exception e) {
                log.warn("Failed to generate PDF for invoice {}, sending without attachment: {}",
                    invoice.getInvoiceCode(), e.getMessage());
            }
        }

        String subject = "Your Invoice: " + invoice.getInvoiceCode() + " — " + invoice.getTitle();
        String invoiceCode = invoice.getInvoiceCode();

        final byte[] finalPdfBytes = pdfBytes;
        final String finalPdfFileName = pdfFileName;
        final String emailLang = resolvedLanguage;
        final Long templateId = emailTemplateId;

        CompletableFuture.runAsync(() -> {
            try {
                String renderedHtml = templateId != null
                    ? emailTemplateRenderer.renderTemplate("SEND_INVOICE", templateId, variables)
                    : emailTemplateRenderer.renderTemplate("SEND_INVOICE", variables);

                String finalSubject = subject;

                if (emailLang != null && !"en".equalsIgnoreCase(emailLang)) {
                    try {
                        String translatedHtml = translationService.translateHtml(renderedHtml, emailLang);
                        if (translatedHtml != null && !translatedHtml.isBlank()) renderedHtml = translatedHtml;
                        String translatedSubject = translationService.translatePlainText(subject, "en", emailLang);
                        if (translatedSubject != null && !translatedSubject.isBlank()) finalSubject = translatedSubject;
                    } catch (Exception e) {
                        log.warn("Translation failed for invoice {}, sending in English", invoiceCode);
                    }
                }

                if (finalPdfBytes != null && finalPdfBytes.length > 0) {
                    emailSendingService.sendHtmlEmailWithAttachment(customerEmail, finalSubject, renderedHtml, finalPdfBytes, finalPdfFileName);
                } else {
                    emailSendingService.sendHtmlEmail(customerEmail, finalSubject, renderedHtml);
                }
                log.info("Invoice email sent to {} for invoice {}", customerEmail, invoiceCode);
            } catch (Exception e) {
                log.warn("Failed to send invoice email for {}: {}", invoiceCode, e.getMessage());
            }
        });
    }

    // ============================================================
    // Variable builders
    // ============================================================

    private Map<String, String> buildInvoiceEmailVariables(Invoice invoice) {
        Map<String, String> v = new HashMap<>();

        // Required variables
        v.put("customerName", invoice.getCustomer().getDisplayName());
        v.put("invoiceCode", invoice.getInvoiceCode());
        v.put("invoiceTitle", invoice.getTitle());
        v.put("issueDate", invoice.getIssueDate() != null ? invoice.getIssueDate().format(DATE_FMT) : "");
        v.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FMT) : "");
        v.put("grandTotal", formatGrandTotals(invoice.getGrandTotals()));
        v.put("sentDate", LocalDate.now().format(DATE_FMT));

        // Optional variables
        if (invoice.getSafari() != null) {
            v.put("safariName", invoice.getSafari().getName() != null ? invoice.getSafari().getName() : "");
            v.put("safariCode", invoice.getSafari().getCode() != null ? invoice.getSafari().getCode() : "");
        } else {
            v.put("safariName", "");
            v.put("safariCode", "");
        }

        v.put("itemsSummary", buildItemsSummaryHtml(invoice));
        v.put("paymentTerms", invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "");
        v.put("customerNotes", invoice.getCustomerNotes() != null ? invoice.getCustomerNotes() : "");

        return v;
    }

    /**
     * Format grand totals for display (e.g., "USD 3,500.00 | EUR 2,800.00")
     */
    private String formatGrandTotals(List<Price> grandTotals) {
        if (grandTotals == null || grandTotals.isEmpty()) {
            return "N/A";
        }

        return grandTotals.stream()
            .map(price -> {
                String currency = price.getCurrency() != null ? price.getCurrency().toUpperCase() : "USD";
                BigDecimal amount = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                nf.setMinimumFractionDigits(2);
                nf.setMaximumFractionDigits(2);
                return currency + " " + nf.format(amount);
            })
            .collect(Collectors.joining(" | "));
    }

    /**
     * Build HTML table of line items for the email template
     */
    private String buildItemsSummaryHtml(Invoice invoice) {
        List<InvoiceLineItem> items = invoice.getLineItems();
        if (items == null || items.isEmpty()) {
            return "";
        }

        var sortedItems = items.stream()
            .filter(item -> item.getIsActive() != null && item.getIsActive())
            .sorted(Comparator.comparingInt(item -> item.getDisplayOrder() != null ? item.getDisplayOrder() : 0))
            .toList();

        if (sortedItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (InvoiceLineItem item : sortedItems) {
            sb.append("<div class=\"detail-row\">");
            sb.append("<span class=\"detail-label\">");
            if (item.getItemType() != null) {
                sb.append(item.getItemType().getDisplayName()).append(": ");
            }
            sb.append(item.getItemName() != null ? item.getItemName() : "");
            sb.append("</span>");
            sb.append("<span class=\"detail-value\">");
            if (item.getPrices() != null && !item.getPrices().isEmpty()) {
                String priceStr = item.getPrices().stream()
                    .map(price -> {
                        String currency = price.getCurrency() != null ? price.getCurrency().toUpperCase() : "";
                        BigDecimal total = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                        nf.setMinimumFractionDigits(2);
                        nf.setMaximumFractionDigits(2);
                        return currency + " " + nf.format(total);
                    })
                    .collect(Collectors.joining(" / "));
                sb.append(priceStr);
            }
            sb.append("</span>");
            sb.append("</div>");
        }

        return sb.toString();
    }
}
