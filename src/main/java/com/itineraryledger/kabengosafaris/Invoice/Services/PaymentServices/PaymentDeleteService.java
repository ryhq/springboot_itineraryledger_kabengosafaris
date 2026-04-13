package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for deleting payments.
 *
 * After deleting a payment, the invoice status is recalculated:
 *   - No payments left → SENT (or OVERDUE if past due)
 *   - Partial payments remaining → PARTIALLY_PAID
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentDeleteService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final jakarta.persistence.EntityManager entityManager;

    /** Safari states past FULLY_PAID — deleting payments here is dangerous */
    private static final Set<SafariState> POST_PAYMENT_STATES = Set.of(
        SafariState.IN_PROGRESS, SafariState.COMPLETED, SafariState.CLOSED
    );

    /**
     * Delete a single payment by obfuscated ID.
     *
     * Payments can only be deleted when the parent invoice is not CANCELLED.
     * After deletion, the invoice status is recalculated based on remaining payments.
     *
     * @param idObfuscated The obfuscated payment ID
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @AuditLogAnnotation(
        action = "DELETE_PAYMENT",
        description = "Deleting a payment record",
        entityType = "Payment",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> deletePayment(String idObfuscated, boolean force) {
        log.info("Attempting to delete payment: {} (force: {})", idObfuscated, force);

        try {
            // Decode payment ID
            Long paymentId;
            try {
                paymentId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode payment ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid payment ID", "INVALID_PAYMENT_ID")
                );
            }

            // Find payment
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }

            // Validate invoice is not cancelled
            Invoice invoice = payment.getInvoice();
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot delete payment on a cancelled invoice",
                        "INVOICE_CANCELLED")
                );
            }

            // Guard: block delete if it would break fully-paid status on a progressed safari
            if (!force) {
                Safari safari = invoice.getSafari();
                if (safari != null && POST_PAYMENT_STATES.contains(safari.getState())) {
                    if (wouldBreakFullyPaid(payment, invoice)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                String.format("Cannot delete payment — safari %s is in %s state. " +
                                    "This would make the invoice no longer fully paid. " +
                                    "Use force=true to override.",
                                    safari.getCode(), safari.getState().getDisplayName()),
                                "SAFARI_PAYMENT_PROTECTED")
                        );
                    }
                }
            }

            // Delete the payment
            paymentRepository.delete(payment);
            log.info("Payment deleted: {} (invoice: {})", paymentId, invoice.getInvoiceCode());

            // Recalculate invoice status after deletion
            recalculateInvoiceStatusAfterDeletion(invoice);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete payment", "PAYMENT_DELETE_FAILED")
            );
        }
    }

    /**
     * Recalculate invoice status after a payment has been deleted.
     *
     * Logic:
     *   - If no payments remain → SENT (or OVERDUE if past due date)
     *   - If partial payments remain → PARTIALLY_PAID
     *   - If still fully paid (edge case) → PAID
     */
    private void recalculateInvoiceStatusAfterDeletion(Invoice invoice) {
        // Flush the delete so the COUNT/SUM queries see the deletion
        entityManager.flush();

        long remainingPaymentCount = paymentRepository.countByInvoiceId(invoice.getId());

        if (remainingPaymentCount == 0) {
            // No payments left — revert to SENT or OVERDUE
            if (invoice.getDueDate() != null && LocalDate.now().isAfter(invoice.getDueDate())) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                log.info("Invoice {} reverted to OVERDUE (no payments, past due date)", invoice.getInvoiceCode());
            } else {
                invoice.setStatus(InvoiceStatus.SENT);
                log.info("Invoice {} reverted to SENT (no payments remaining)", invoice.getInvoiceCode());
            }
            invoice.setPaidDate(null);
        } else {
            // Some payments remain — check if still fully paid or partially paid
            boolean allFullyPaid = true;
            boolean anyPartiallyPaid = false;

            if (invoice.getGrandTotals() != null) {
                for (Price grandTotal : invoice.getGrandTotals()) {
                    String currency = grandTotal.getCurrency();
                    BigDecimal grandTotalAmount = grandTotal.getTotalPrice();

                    if (grandTotalAmount == null || grandTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal totalPaid = paymentRepository.sumAmountByInvoiceIdAndCurrency(
                        invoice.getId(), currency
                    );

                    if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                        anyPartiallyPaid = true;
                    }

                    if (totalPaid.compareTo(grandTotalAmount) < 0) {
                        allFullyPaid = false;
                    }
                }
            }

            if (allFullyPaid && anyPartiallyPaid) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidDate(LocalDate.now());
                log.info("Invoice {} remains PAID after payment deletion", invoice.getInvoiceCode());
            } else if (anyPartiallyPaid) {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
                invoice.setPaidDate(null);
                log.info("Invoice {} set to PARTIALLY_PAID after payment deletion", invoice.getInvoiceCode());
            }
        }

        invoiceRepository.save(invoice);
    }

    /**
     * Check if deleting this payment would cause the invoice to no longer be fully paid.
     */
    private boolean wouldBreakFullyPaid(Payment payment, Invoice invoice) {
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return false;
        }

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal required = grandTotal.getTotalPrice();
            if (required == null || required.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal currentPaid = paymentRepository.sumAmountByInvoiceIdAndCurrency(invoice.getId(), currency);

            // Simulate removing this payment's contribution
            BigDecimal simulatedPaid = currentPaid;
            if (currency.equalsIgnoreCase(payment.getCurrency())) {
                simulatedPaid = simulatedPaid.subtract(payment.getAmount());
            }

            if (simulatedPaid.compareTo(required) < 0) {
                return true;
            }
        }
        return false;
    }
}
