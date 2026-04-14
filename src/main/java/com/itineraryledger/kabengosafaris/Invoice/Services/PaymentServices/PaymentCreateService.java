package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreatePaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating payments against invoices.
 *
 * After recording a payment, the invoice status auto-transitions:
 *   - total paid >= grand total → PAID
 *   - total paid > 0 but < grand total → PARTIALLY_PAID
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentCreateService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final IdObfuscator idObfuscator;
    private final PaymentCustomerEmailService paymentCustomerEmailService;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * Payable invoice states — payments are only accepted when the invoice is
     * in one of these states.
     */
    private static final java.util.Set<InvoiceStatus> PAYABLE_STATES = java.util.Set.of(
        InvoiceStatus.SENT,
        InvoiceStatus.PARTIALLY_PAID,
        InvoiceStatus.OVERDUE
    );

    @AuditLogAnnotation(
        action = "CREATE_PAYMENT",
        description = "Recording a new payment against an invoice",
        entityType = "Payment"
    )
    public ResponseEntity<ApiResponse<?>> createPayment(CreatePaymentDTO dto) {
        log.info("Recording new payment for invoice: {}", dto.getInvoiceId());

        try {
            // ========================
            // VALIDATE INVOICE
            // ========================

            if (dto.getInvoiceId() == null || dto.getInvoiceId().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice ID is required", "INVOICE_ID_REQUIRED")
                );
            }

            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(dto.getInvoiceId());
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", dto.getInvoiceId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // ========================
            // VALIDATE PAYABLE STATE
            // ========================

            if (!PAYABLE_STATES.contains(invoice.getStatus())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot record payment on invoice in %s state. " +
                            "Invoice must be in one of: SENT, PARTIALLY_PAID, OVERDUE.",
                            invoice.getStatus().getDisplayName()),
                        "INVOICE_NOT_PAYABLE")
                );
            }

            // ========================
            // VALIDATE AMOUNT
            // ========================

            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Payment amount must be greater than zero", "INVALID_AMOUNT")
                );
            }

            String paymentCurrency = dto.getCurrency().toUpperCase();

            // ========================
            // RESOLVE INVOICE CURRENCY
            // ========================

            String invoiceCurrency = resolveInvoiceCurrency(dto.getInvoiceCurrency(), paymentCurrency, invoice);
            if (invoiceCurrency == null) {
                // Could not auto-infer — multi-currency invoice with cross-currency payment
                Set<String> grandTotalCurrencies = invoice.getGrandTotals().stream()
                    .map(Price::getCurrency)
                    .collect(Collectors.toSet());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("invoiceCurrency is required for cross-currency payments on " +
                            "multi-currency invoices. Invoice currencies: %s, payment currency: %s",
                            grandTotalCurrencies, paymentCurrency),
                        "INVOICE_CURRENCY_REQUIRED")
                );
            }

            // ========================
            // RESOLVE EXCHANGE RATE
            // ========================

            boolean crossCurrency = !paymentCurrency.equalsIgnoreCase(invoiceCurrency);
            BigDecimal exchangeRate;

            if (crossCurrency) {
                if (dto.getExchangeRate() == null || dto.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            String.format("exchangeRate is required and must be > 0 for cross-currency payments " +
                                "(%s → %s). Rate = 1 %s in %s.",
                                paymentCurrency, invoiceCurrency, paymentCurrency, invoiceCurrency),
                            "EXCHANGE_RATE_REQUIRED")
                    );
                }
                exchangeRate = dto.getExchangeRate();
            } else {
                exchangeRate = BigDecimal.ONE;
            }

            BigDecimal baseAmount = dto.getAmount()
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);

            // ========================
            // RESOLVE BANK ACCOUNT (optional)
            // ========================

            BankAccount bankAccount = null;
            if (dto.getBankAccountId() != null && !dto.getBankAccountId().isBlank()) {
                try {
                    Long bankAccountId = idObfuscator.decodeId(dto.getBankAccountId());
                    bankAccount = bankAccountRepository.findById(bankAccountId).orElse(null);
                    if (bankAccount == null) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Bank account not found", "BANK_ACCOUNT_NOT_FOUND")
                        );
                    }
                    if (!Boolean.TRUE.equals(bankAccount.getIsActive())) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Bank account is not active", "BANK_ACCOUNT_INACTIVE")
                        );
                    }
                } catch (Exception e) {
                    log.warn("Failed to decode bank account ID: {}", dto.getBankAccountId(), e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid bank account ID", "INVALID_BANK_ACCOUNT_ID")
                    );
                }
            }

            // ========================
            // GET CURRENT USER
            // ========================

            User currentUser = getCurrentUser();

            // ========================
            // BUILD AND SAVE PAYMENT
            // ========================

            Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(dto.getAmount())
                .currency(paymentCurrency)
                .invoiceCurrency(invoiceCurrency)
                .exchangeRate(exchangeRate)
                .baseAmount(baseAmount)
                .bankAccount(bankAccount)
                .paymentDate(dto.getPaymentDate())
                .paymentMethod(dto.getPaymentMethod())
                .reference(dto.getReference())
                .notes(dto.getNotes())
                .recordedBy(currentUser)
                .build();

            payment = paymentRepository.save(payment);

            log.info("Payment saved with ID: {} for invoice: {} ({} {} → {} {} @ rate {})",
                payment.getId(), invoice.getInvoiceCode(),
                paymentCurrency, dto.getAmount(),
                invoiceCurrency, baseAmount, exchangeRate);

            // ========================
            // AUTO-TRANSITION INVOICE STATUS
            // ========================

            recalculateInvoiceStatus(invoice);

            // Send payment receipt email if requested
            if (Boolean.TRUE.equals(dto.getNotifyCustomer())) {
                paymentCustomerEmailService.sendPaymentReceiptEmail(payment);
            }

            // Convert to DTO
            PaymentDTO paymentDTO = convertToDTO(payment);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Payment recorded successfully", paymentDTO)
            );

        } catch (Exception e) {
            log.error("Error recording payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to record payment", "PAYMENT_CREATE_FAILED")
            );
        }
    }

    /**
     * Resolve which invoice grand-total currency a payment is settling.
     *
     * <ol>
     *   <li>If explicitly provided and valid → use it</li>
     *   <li>If payment currency matches a grand-total currency → same-currency, use it</li>
     *   <li>If invoice has exactly one grand-total currency → use that</li>
     *   <li>Otherwise → null (caller must reject)</li>
     * </ol>
     */
    public String resolveInvoiceCurrency(String explicit, String paymentCurrency, Invoice invoice) {
        List<Price> grandTotals = invoice.getGrandTotals();
        if (grandTotals == null || grandTotals.isEmpty()) {
            return paymentCurrency; // edge case: no totals yet
        }

        Set<String> grandTotalCurrencies = grandTotals.stream()
            .map(Price::getCurrency)
            .collect(Collectors.toSet());

        // 1. Explicit
        if (explicit != null && !explicit.isBlank()) {
            String upper = explicit.toUpperCase();
            if (!grandTotalCurrencies.contains(upper)) {
                return null; // invalid — not one of the invoice currencies
            }
            return upper;
        }

        // 2. Payment currency matches a grand-total currency
        if (grandTotalCurrencies.contains(paymentCurrency)) {
            return paymentCurrency;
        }

        // 3. Single-currency invoice
        if (grandTotalCurrencies.size() == 1) {
            return grandTotalCurrencies.iterator().next();
        }

        // 4. Ambiguous — cannot infer
        return null;
    }

    /**
     * Recalculate invoice status based on total payments vs grand totals.
     *
     * For each currency in the invoice grand totals:
     *   - Sum all payments in that currency
     *   - If total paid >= grand total for ALL currencies → PAID
     *   - If total paid > 0 for ANY currency → PARTIALLY_PAID
     */
    public void recalculateInvoiceStatus(Invoice invoice) {
        // Flush pending changes so the SUM query sees the newly saved/deleted payment
        entityManager.flush();

        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            log.warn("Invoice {} has no grand totals, skipping status recalculation", invoice.getInvoiceCode());
            return;
        }

        boolean allFullyPaid = true;
        boolean anyPartiallyPaid = false;

        for (Price grandTotal : invoice.getGrandTotals()) {
            String currency = grandTotal.getCurrency();
            BigDecimal grandTotalAmount = grandTotal.getTotalPrice();

            if (grandTotalAmount == null || grandTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal totalPaid = paymentRepository.sumBaseAmountByInvoiceIdAndInvoiceCurrency(
                invoice.getId(), currency
            );

            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                anyPartiallyPaid = true;
            }

            if (totalPaid.compareTo(grandTotalAmount) < 0) {
                allFullyPaid = false;
            }
        }

        // Determine new status
        if (allFullyPaid && anyPartiallyPaid) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(LocalDate.now());
            log.info("Invoice {} auto-transitioned to PAID", invoice.getInvoiceCode());
        } else if (anyPartiallyPaid) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            log.info("Invoice {} auto-transitioned to PARTIALLY_PAID", invoice.getInvoiceCode());
        }

        invoiceRepository.save(invoice);
    }

    /**
     * Convert Payment entity to PaymentDTO
     */
    public PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = PaymentDTO.builder()
            .id(idObfuscator.encodeId(payment.getId()))
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .invoiceCurrency(payment.getInvoiceCurrency())
            .exchangeRate(payment.getExchangeRate())
            .baseAmount(payment.getBaseAmount())
            .paymentDate(payment.getPaymentDate())
            .paymentMethod(payment.getPaymentMethod())
            .paymentMethodDisplayName(payment.getPaymentMethod().getDisplayName())
            .reference(payment.getReference())
            .notes(payment.getNotes())
            .createdAt(payment.getCreatedAt())
            .build();

        // Set invoice relationship
        if (payment.getInvoice() != null) {
            dto.setInvoiceId(idObfuscator.encodeId(payment.getInvoice().getId()));
            dto.setInvoiceCode(payment.getInvoice().getInvoiceCode());
        }

        // Set bank account
        if (payment.getBankAccount() != null) {
            dto.setBankAccountId(idObfuscator.encodeId(payment.getBankAccount().getId()));
            dto.setBankAccountName(payment.getBankAccount().getAccountName());
            dto.setBankAccountCode(payment.getBankAccount().getAccountCode());
        }

        // Set recorded by user
        if (payment.getRecordedBy() != null) {
            dto.setRecordedById(idObfuscator.encodeId(payment.getRecordedBy().getId()));
            dto.setRecordedByName(payment.getRecordedBy().getUsername());
        }

        return dto;
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
