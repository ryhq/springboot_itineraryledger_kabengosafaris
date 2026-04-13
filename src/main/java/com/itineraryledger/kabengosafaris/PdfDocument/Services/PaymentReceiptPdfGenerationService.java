package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentReceiptDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * PaymentReceiptPdfGenerationService - Generates payment receipt PDFs.
 * Uses the PAYMENT_RECEIPT PDF document type.
 */
@Service
@Slf4j
public class PaymentReceiptPdfGenerationService extends PdfGenerationBaseService {

    private final PaymentRepository paymentRepository;

    public PaymentReceiptPdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            PaymentRepository paymentRepository
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.paymentRepository = paymentRepository;
    }

    /**
     * Generate payment receipt PDF
     *
     * @param payment  The payment entity
     * @param language Optional language for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePaymentReceiptPdf(Payment payment, String language) {
        return generatePaymentReceiptPdf(payment, language, null);
    }

    /**
     * Generate payment receipt PDF with an explicit PDF template override.
     *
     * @param payment        The payment entity
     * @param language       Optional language for translation
     * @param pdfTemplateId  Optional obfuscated PDF template ID (must belong to PAYMENT_RECEIPT document).
     *                       When null, the system default template is used.
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePaymentReceiptPdf(Payment payment, String language, String pdfTemplateId) {
        try {
            PaymentReceiptDTO receiptData = buildReceiptDTO(payment);

            return generatePdfInternal(
                "PAYMENT_RECEIPT",
                receiptData,
                pdfTemplateId,
                language,
                data -> extractReceiptCode((PaymentReceiptDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate payment receipt PDF for payment ID: {}", payment.getId(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate payment receipt PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Build PaymentReceiptDTO from Payment entity
     */
    public PaymentReceiptDTO buildReceiptDTO(Payment payment) {
        Invoice invoice = payment.getInvoice();

        PaymentReceiptDTO.PaymentReceiptDTOBuilder builder = PaymentReceiptDTO.builder()
            .paymentId(idObfuscator.encodeId(payment.getId()))
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .formattedAmount(formatAmount(payment.getAmount(), payment.getCurrency()))
            .paymentDate(payment.getPaymentDate())
            .paymentMethod(payment.getPaymentMethod().getDisplayName())
            .reference(payment.getReference())
            .notes(payment.getNotes())
            .receiptDate(LocalDate.now());

        if (payment.getRecordedBy() != null) {
            builder.recordedByName(payment.getRecordedBy().getUsername());
        }

        // Invoice details
        if (invoice != null) {
            builder.invoiceCode(invoice.getInvoiceCode())
                   .invoiceTitle(invoice.getTitle())
                   .invoiceStatus(invoice.getStatus().getDisplayName())
                   .grandTotal(formatPriceList(invoice.getGrandTotals()))
                   .totalPaid(calculateTotalPaid(invoice))
                   .balanceRemaining(calculateBalanceRemaining(invoice));

            // Customer
            if (invoice.getCustomer() != null) {
                builder.customer(PaymentReceiptDTO.CustomerDTO.builder()
                    .customerName(invoice.getCustomer().getDisplayName())
                    .email(invoice.getCustomer().getPrimaryEmail())
                    .phone(invoice.getCustomer().getPrimaryPhone())
                    .build());
            }

            // Safari
            if (invoice.getSafari() != null) {
                builder.safari(PaymentReceiptDTO.SafariDTO.builder()
                    .name(invoice.getSafari().getName())
                    .code(invoice.getSafari().getCode())
                    .build());
            }
        }

        return builder.build();
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

    private String extractReceiptCode(PaymentReceiptDTO receipt) {
        return receipt.getInvoiceCode() != null ? "RCPT_" + receipt.getInvoiceCode() : "receipt";
    }
}
