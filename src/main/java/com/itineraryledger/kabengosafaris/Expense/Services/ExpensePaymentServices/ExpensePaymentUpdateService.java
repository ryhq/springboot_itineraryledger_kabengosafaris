package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpensePaymentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpenseStateTransitionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpensePaymentUpdateService {

    private final ExpensePaymentRepository repository;
    private final BankAccountRepository bankAccountRepository;
    private final ExpenseStateTransitionService stateTransitionService;
    private final ExpensePaymentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_EXPENSE_PAYMENT",
        entityType = "EXPENSE",
        entityIdParamName = "expenseIdObfuscated",
        description = "Update an expense payment"
    )
    public ResponseEntity<ApiResponse<?>> updatePayment(
            String expenseIdObfuscated,
            String paymentIdObfuscated,
            UpdateExpensePaymentDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Long paymentId = idObfuscator.decodeId(paymentIdObfuscated);

            ExpensePayment p = repository.findById(paymentId).orElse(null);
            if (p == null || !p.getExpense().getId().equals(expenseId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "EXPENSE_PAYMENT_NOT_FOUND"));
            }

            if (dto.getAmount() != null) p.setAmount(dto.getAmount());
            if (dto.getCurrency() != null) p.setCurrency(dto.getCurrency().toUpperCase().trim());
            if (dto.getPaymentDate() != null) p.setPaymentDate(dto.getPaymentDate());
            if (dto.getPaymentMethod() != null) p.setPaymentMethod(dto.getPaymentMethod());
            if (dto.getExpenseCurrency() != null) p.setExpenseCurrency(dto.getExpenseCurrency().toUpperCase().trim());
            if (dto.getExchangeRate() != null) p.setExchangeRate(dto.getExchangeRate());

            // Recompute baseAmount whenever amount, currency, or exchangeRate changed
            if (dto.getAmount() != null || dto.getCurrency() != null
                    || dto.getExchangeRate() != null || dto.getExpenseCurrency() != null) {
                BigDecimal rate = p.getExchangeRate() != null ? p.getExchangeRate() : BigDecimal.ONE;
                BigDecimal amount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                p.setBaseAmount(amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }

            if (dto.getBankAccountId() != null) {
                if (dto.getBankAccountId().isBlank()) {
                    p.setBankAccount(null);
                } else {
                    Long bankId = idObfuscator.decodeId(dto.getBankAccountId());
                    BankAccount bank = bankAccountRepository.findById(bankId).orElse(null);
                    p.setBankAccount(bank);
                }
            }

            if (dto.getReference() != null) p.setReference(dto.getReference());
            if (dto.getNotes() != null) p.setNotes(dto.getNotes());

            p = repository.save(p);

            stateTransitionService.recomputeStatus(expenseId);

            return ResponseEntity.ok(ApiResponse.success(200, "Payment updated", getService.toDTO(p)));
        } catch (Exception e) {
            log.error("Error updating expense payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update payment: " + e.getMessage(),
                        "EXPENSE_PAYMENT_UPDATE_FAILED"));
        }
    }
}
