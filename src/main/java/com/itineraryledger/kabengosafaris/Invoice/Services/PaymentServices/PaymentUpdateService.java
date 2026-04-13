package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdatePaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating existing payments.
 *
 * After updating a payment, the invoice status is recalculated
 * in case the amount or currency changed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentUpdateService {

    private final PaymentRepository paymentRepository;
    private final PaymentCreateService paymentCreateService;
    private final IdObfuscator idObfuscator;

    /** Safari states that are past FULLY_PAID — editing payments here is dangerous */
    private static final Set<SafariState> POST_PAYMENT_STATES = Set.of(
        SafariState.IN_PROGRESS, SafariState.COMPLETED, SafariState.CLOSED
    );

    @AuditLogAnnotation(
        action = "UPDATE_PAYMENT",
        description = "Updating an existing payment record",
        entityType = "Payment",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updatePayment(String idObfuscated, UpdatePaymentDTO dto) {
        log.info("Updating payment: {}", idObfuscated);

        try {
            Long paymentId;
            try {
                paymentId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode payment ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid payment ID", "INVALID_PAYMENT_ID")
                );
            }

            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }

            Invoice invoice = payment.getInvoice();
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot edit payment on a cancelled invoice", "INVOICE_CANCELLED")
                );
            }

            // Guard: block financial changes that would break fully-paid status
            // if the linked safari has already progressed past FULLY_PAID
            boolean isFinancialEdit = (dto.getAmount() != null || (dto.getCurrency() != null && !dto.getCurrency().isBlank()));
            if (isFinancialEdit && !Boolean.TRUE.equals(dto.getForce())) {
                Safari safari = invoice.getSafari();
                if (safari != null && POST_PAYMENT_STATES.contains(safari.getState())) {
                    // Simulate: would this edit break the fully-paid status?
                    if (wouldBreakFullyPaid(payment, invoice, dto)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                String.format("Cannot reduce payment — safari %s is in %s state. " +
                                    "This change would make the invoice no longer fully paid. " +
                                    "Set force=true to override.",
                                    safari.getCode(), safari.getState().getDisplayName()),
                                "SAFARI_PAYMENT_PROTECTED")
                        );
                    }
                }
            }

            // Track whether amount/currency changed (needs status recalculation)
            boolean financialChange = false;

            if (dto.getAmount() != null) {
                if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Payment amount must be greater than zero", "INVALID_AMOUNT")
                    );
                }
                financialChange = true;
                payment.setAmount(dto.getAmount());
            }

            if (dto.getCurrency() != null && !dto.getCurrency().isBlank()) {
                financialChange = true;
                payment.setCurrency(dto.getCurrency().toUpperCase());
            }

            if (dto.getPaymentDate() != null) {
                payment.setPaymentDate(dto.getPaymentDate());
            }

            if (dto.getPaymentMethod() != null) {
                payment.setPaymentMethod(dto.getPaymentMethod());
            }

            if (dto.getReference() != null) {
                payment.setReference(dto.getReference());
            }

            if (dto.getNotes() != null) {
                payment.setNotes(dto.getNotes());
            }

            payment = paymentRepository.save(payment);
            log.info("Payment {} updated for invoice {}", paymentId, invoice.getInvoiceCode());

            // Recalculate invoice status if amount or currency changed
            if (financialChange) {
                paymentCreateService.recalculateInvoiceStatus(invoice);
            }

            PaymentDTO paymentDTO = paymentCreateService.convertToDTO(payment);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment updated successfully", paymentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating payment", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to update payment", "PAYMENT_UPDATE_FAILED")
            );
        }
    }

    /**
     * Check if editing this payment would cause the invoice to no longer be fully paid.
     * Simulates the new totals by subtracting the old payment and adding the new values.
     */
    private boolean wouldBreakFullyPaid(Payment payment, Invoice invoice, UpdatePaymentDTO dto) {
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return false;
        }

        String oldCurrency = payment.getCurrency();
        BigDecimal oldAmount = payment.getAmount();
        String newCurrency = (dto.getCurrency() != null && !dto.getCurrency().isBlank())
            ? dto.getCurrency().toUpperCase() : oldCurrency;
        BigDecimal newAmount = dto.getAmount() != null ? dto.getAmount() : oldAmount;

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal required = grandTotal.getTotalPrice();
            if (required == null || required.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal currentPaid = paymentRepository.sumAmountByInvoiceIdAndCurrency(invoice.getId(), currency);

            // Simulate: subtract old contribution, add new contribution for this currency
            BigDecimal simulatedPaid = currentPaid;
            if (currency.equalsIgnoreCase(oldCurrency)) {
                simulatedPaid = simulatedPaid.subtract(oldAmount);
            }
            if (currency.equalsIgnoreCase(newCurrency)) {
                simulatedPaid = simulatedPaid.add(newAmount);
            }

            if (simulatedPaid.compareTo(required) < 0) {
                return true;
            }
        }
        return false;
    }
}
