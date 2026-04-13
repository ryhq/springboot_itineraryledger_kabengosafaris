package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PaymentReceiptPdfGenerationService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * PaymentCustomerEmailService - Sends payment receipt email to customer with optional PDF attachment.
 *
 * Uses SEND_PAYMENT_RECEIPT email event and PAYMENT_RECEIPT PDF document type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCustomerEmailService {

    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final PaymentReceiptPdfGenerationService paymentReceiptPdfGenerationService;
    private final PaymentRepository paymentRepository;
    private final TranslationService translationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Send payment receipt email to the customer (simple — uses defaults).
     * Called from PaymentCreateService when notifyCustomer=true.
     */
    public void sendPaymentReceiptEmail(Payment payment) {
        sendPaymentReceiptEmail(payment, null, null, null, true);
    }

    /**
     * Backwards-compatible overload without pdfTemplateId — uses the system default PDF template.
     */
    public void sendPaymentReceiptEmail(Payment payment, String language, Long emailTemplateId, boolean attachPdf) {
        sendPaymentReceiptEmail(payment, language, emailTemplateId, null, attachPdf);
    }

    /**
     * Send payment receipt email to the customer with full control.
     *
     * @param payment          The payment entity (with invoice loaded)
     * @param language         Optional language code for translation (e.g., "fr", "sw")
     * @param emailTemplateId  Optional specific email template ID (must belong to SEND_PAYMENT_RECEIPT event)
     * @param pdfTemplateId    Optional obfuscated PDF template ID (must belong to PAYMENT_RECEIPT document)
     * @param attachPdf        Whether to attach the payment receipt PDF
     */
    public void sendPaymentReceiptEmail(Payment payment, String language, Long emailTemplateId, String pdfTemplateId, boolean attachPdf) {
        Invoice invoice = payment.getInvoice();

        if (invoice == null || invoice.getCustomer() == null) {
            log.warn("Cannot send payment receipt - no customer linked to invoice");
            return;
        }

        String customerEmail = invoice.getCustomer().getPrimaryEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Cannot send payment receipt - customer has no email for invoice {}", invoice.getInvoiceCode());
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
        Map<String, String> variables = buildPaymentReceiptVariables(payment, invoice);

        // Generate receipt PDF if requested
        byte[] pdfBytes = null;
        String pdfFileName = null;
        if (attachPdf) {
            String pdfLang = (resolvedLanguage != null && !"en".equalsIgnoreCase(resolvedLanguage)) ? resolvedLanguage : null;
            try {
                ResponseEntity<?> pdfResponse = paymentReceiptPdfGenerationService.generatePaymentReceiptPdf(payment, pdfLang, pdfTemplateId);
                if (pdfResponse.getStatusCode().is2xxSuccessful() && pdfResponse.getBody() instanceof byte[]) {
                    pdfBytes = (byte[]) pdfResponse.getBody();
                    pdfFileName = "Payment_Receipt_" + invoice.getInvoiceCode() + ".pdf";
                }
            } catch (Exception e) {
                log.warn("Failed to generate payment receipt PDF for invoice {}, sending without attachment: {}",
                    invoice.getInvoiceCode(), e.getMessage());
            }
        }

        String subject = "Payment Receipt: " + invoice.getInvoiceCode() + " — " + formatAmount(payment.getAmount(), payment.getCurrency());
        String invoiceCode = invoice.getInvoiceCode();

        final byte[] finalPdfBytes = pdfBytes;
        final String finalPdfFileName = pdfFileName;
        final String emailLang = resolvedLanguage;
        final Long templateId = emailTemplateId;

        CompletableFuture.runAsync(() -> {
            try {
                String renderedHtml = templateId != null
                    ? emailTemplateRenderer.renderTemplate("SEND_PAYMENT_RECEIPT", templateId, variables)
                    : emailTemplateRenderer.renderTemplate("SEND_PAYMENT_RECEIPT", variables);

                String finalSubject = subject;

                if (emailLang != null && !"en".equalsIgnoreCase(emailLang)) {
                    try {
                        String translatedHtml = translationService.translateHtml(renderedHtml, emailLang);
                        if (translatedHtml != null && !translatedHtml.isBlank()) renderedHtml = translatedHtml;
                        String translatedSubject = translationService.translatePlainText(subject, "en", emailLang);
                        if (translatedSubject != null && !translatedSubject.isBlank()) finalSubject = translatedSubject;
                    } catch (Exception e) {
                        log.warn("Translation failed for payment receipt {}, sending in English", invoiceCode);
                    }
                }

                if (finalPdfBytes != null && finalPdfBytes.length > 0) {
                    emailSendingService.sendHtmlEmailWithAttachment(customerEmail, finalSubject, renderedHtml, finalPdfBytes, finalPdfFileName);
                } else {
                    emailSendingService.sendHtmlEmail(customerEmail, finalSubject, renderedHtml);
                }
                log.info("Payment receipt email sent to {} for invoice {}", customerEmail, invoiceCode);
            } catch (Exception e) {
                log.warn("Failed to send payment receipt email for {}: {}", invoiceCode, e.getMessage());
            }
        });
    }

    private Map<String, String> buildPaymentReceiptVariables(Payment payment, Invoice invoice) {
        Map<String, String> v = new HashMap<>();

        // Required variables
        v.put("customerName", invoice.getCustomer().getDisplayName());
        v.put("invoiceCode", invoice.getInvoiceCode());
        v.put("invoiceTitle", invoice.getTitle());
        v.put("paymentAmount", formatAmount(payment.getAmount(), payment.getCurrency()));
        v.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FMT) : "");
        v.put("paymentMethod", payment.getPaymentMethod().getDisplayName());
        v.put("grandTotal", formatPriceList(invoice.getGrandTotals()));
        v.put("totalPaid", calculateTotalPaid(invoice));
        v.put("balanceRemaining", calculateBalanceRemaining(invoice));
        v.put("paymentStatus", invoice.getStatus().getDisplayName());
        v.put("sentDate", LocalDate.now().format(DATE_FMT));

        // Optional variables
        v.put("paymentReference", payment.getReference() != null ? payment.getReference() : "");
        v.put("paymentNotes", payment.getNotes() != null ? payment.getNotes() : "");

        if (invoice.getSafari() != null) {
            v.put("safariName", invoice.getSafari().getName() != null ? invoice.getSafari().getName() : "");
            v.put("safariCode", invoice.getSafari().getCode() != null ? invoice.getSafari().getCode() : "");
        } else {
            v.put("safariName", "");
            v.put("safariCode", "");
        }

        return v;
    }

    private String formatAmount(BigDecimal amount, String currency) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return (currency != null ? currency.toUpperCase() : "") + " " + nf.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private String formatPriceList(List<Price> prices) {
        if (prices == null || prices.isEmpty()) return "N/A";
        return prices.stream()
            .map(p -> formatAmount(p.getTotalPrice(), p.getCurrency()))
            .reduce((a, b) -> a + " | " + b)
            .orElse("N/A");
    }

    private String calculateTotalPaid(Invoice invoice) {
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (Price gt : invoice.getGrandTotals()) {
            BigDecimal paid = paymentRepository.sumAmountByInvoiceIdAndCurrency(
                invoice.getId(), gt.getCurrency());
            if (paid == null) paid = BigDecimal.ZERO;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(formatAmount(paid, gt.getCurrency()));
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }

    private String calculateBalanceRemaining(Invoice invoice) {
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (Price gt : invoice.getGrandTotals()) {
            BigDecimal grand = gt.getTotalPrice() != null ? gt.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = paymentRepository.sumAmountByInvoiceIdAndCurrency(
                invoice.getId(), gt.getCurrency());
            if (paid == null) paid = BigDecimal.ZERO;
            BigDecimal balance = grand.subtract(paid);
            if (sb.length() > 0) sb.append(" | ");
            sb.append(formatAmount(balance, gt.getCurrency()));
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }
}
