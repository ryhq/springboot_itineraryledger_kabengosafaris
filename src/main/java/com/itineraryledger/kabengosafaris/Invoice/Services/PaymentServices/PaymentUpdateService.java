package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
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
    private final BankAccountRepository bankAccountRepository;
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
            boolean isFinancialEdit = (dto.getAmount() != null
                || (dto.getCurrency() != null && !dto.getCurrency().isBlank())
                || dto.getExchangeRate() != null
                || (dto.getInvoiceCurrency() != null && !dto.getInvoiceCurrency().isBlank()));
            if (isFinancialEdit && !Boolean.TRUE.equals(dto.getForce())) {
                Safari safari = invoice.getSafari();
                if (safari != null && POST_PAYMENT_STATES.contains(safari.getState())) {
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

            // Track whether financial fields changed (needs status recalculation)
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

            if (dto.getInvoiceCurrency() != null && !dto.getInvoiceCurrency().isBlank()) {
                financialChange = true;
                payment.setInvoiceCurrency(dto.getInvoiceCurrency().toUpperCase());
            }

            if (dto.getExchangeRate() != null) {
                if (dto.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Exchange rate must be greater than zero", "INVALID_EXCHANGE_RATE")
                    );
                }
                financialChange = true;
                payment.setExchangeRate(dto.getExchangeRate());
            }

            // Recompute baseAmount if any financial field changed
            if (financialChange) {
                BigDecimal rate = payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE;
                // If payment currency == invoice currency, force rate to 1
                if (payment.getCurrency() != null && payment.getCurrency().equalsIgnoreCase(payment.getInvoiceCurrency())) {
                    rate = BigDecimal.ONE;
                    payment.setExchangeRate(BigDecimal.ONE);
                }
                payment.setBaseAmount(payment.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }

            // Bank account (optional) — empty string clears, null leaves unchanged
            if (dto.getBankAccountId() != null) {
                if (dto.getBankAccountId().isBlank()) {
                    payment.setBankAccount(null);
                } else {
                    try {
                        Long bankAccountId = idObfuscator.decodeId(dto.getBankAccountId());
                        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId).orElse(null);
                        if (bankAccount == null) {
                            return ResponseEntity.badRequest().body(
                                ApiResponse.error(400, "Bank account not found", "BANK_ACCOUNT_NOT_FOUND")
                            );
                        }
                        payment.setBankAccount(bankAccount);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Invalid bank account ID", "INVALID_BANK_ACCOUNT_ID")
                        );
                    }
                }
            }

            if (dto.getPaymentDate() != null) {
                payment.setPaymentDate(dto.getPaymentDate());
            }

            if (dto.getPaymentMethod() != null) {
                payment.setPaymentMethod(dto.getPaymentMethod().isBlank() ? null : com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod.valueOf(dto.getPaymentMethod().trim()));
            }

            if (dto.getReference() != null) {
                payment.setReference(dto.getReference());
            }

            if (dto.getNotes() != null) {
                payment.setNotes(dto.getNotes());
            }

            payment = paymentRepository.save(payment);
            log.info("Payment {} updated for invoice {}", paymentId, invoice.getInvoiceCode());

            // Recalculate invoice status if financial fields changed
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
     * Simulates the new baseAmount totals by subtracting the old contribution and adding the new.
     */
    private boolean wouldBreakFullyPaid(Payment payment, Invoice invoice, UpdatePaymentDTO dto) {
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return false;
        }

        // Old contribution
        String oldInvoiceCurrency = payment.getInvoiceCurrency() != null
            ? payment.getInvoiceCurrency() : payment.getCurrency();
        BigDecimal oldBaseAmount = payment.getBaseAmount() != null
            ? payment.getBaseAmount() : payment.getAmount();

        // Simulate new values
        String newInvoiceCurrency = (dto.getInvoiceCurrency() != null && !dto.getInvoiceCurrency().isBlank())
            ? dto.getInvoiceCurrency().toUpperCase() : oldInvoiceCurrency;
        BigDecimal newAmount = dto.getAmount() != null ? dto.getAmount() : payment.getAmount();
        String newPaymentCurrency = (dto.getCurrency() != null && !dto.getCurrency().isBlank())
            ? dto.getCurrency().toUpperCase() : payment.getCurrency();
        BigDecimal newRate;
        if (newPaymentCurrency.equalsIgnoreCase(newInvoiceCurrency)) {
            newRate = BigDecimal.ONE;
        } else {
            newRate = dto.getExchangeRate() != null ? dto.getExchangeRate()
                : (payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE);
        }
        BigDecimal newBaseAmount = newAmount.multiply(newRate).setScale(2, RoundingMode.HALF_UP);

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal required = grandTotal.getTotalPrice();
            if (required == null || required.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal currentPaid = paymentRepository.sumBaseAmountByInvoiceIdAndInvoiceCurrency(
                invoice.getId(), currency);

            // Simulate: subtract old contribution, add new contribution
            BigDecimal simulatedPaid = currentPaid;
            if (currency.equalsIgnoreCase(oldInvoiceCurrency)) {
                simulatedPaid = simulatedPaid.subtract(oldBaseAmount);
            }
            if (currency.equalsIgnoreCase(newInvoiceCurrency)) {
                simulatedPaid = simulatedPaid.add(newBaseAmount);
            }

            if (simulatedPaid.compareTo(required) < 0) {
                return true;
            }
        }
        return false;
    }
}
