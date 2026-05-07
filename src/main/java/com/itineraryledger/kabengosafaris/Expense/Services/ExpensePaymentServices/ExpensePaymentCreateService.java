package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpensePaymentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpenseStateTransitionService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpensePaymentCreateService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository expensePaymentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final ExpenseStateTransitionService stateTransitionService;
    private final ExpensePaymentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "RECORD_EXPENSE_PAYMENT",
        entityType = "EXPENSE",
        entityIdParamName = "expenseIdObfuscated",
        description = "Record a payment made to a vendor against an expense"
    )
    public ResponseEntity<ApiResponse<?>> createPayment(String expenseIdObfuscated, CreateExpensePaymentDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            // Refuse to record payments on a draft expense (no totals yet) or
            // a cancelled one. PAID/PARTIALLY_PAID are fine — adds another payment.
            if (expense.getStatus() == null
                    || expense.getStatus() == com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus.DRAFT) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Mark the expense as RECORDED before adding payments.",
                        "EXPENSE_NOT_PAYABLE"));
            }
            if (expense.getStatus() == com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus.CANCELLED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Cancelled expenses cannot accept new payments.",
                        "EXPENSE_CANCELLED"));
            }

            String paymentCurrency = dto.getCurrency().toUpperCase().trim();

            // Resolve which expense currency this payment settles
            String expenseCurrency = dto.getExpenseCurrency() != null && !dto.getExpenseCurrency().isBlank()
                    ? dto.getExpenseCurrency().toUpperCase().trim()
                    : inferExpenseCurrency(expense, paymentCurrency);

            if (expenseCurrency == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Could not determine which expense currency this payment settles. "
                            + "Specify expenseCurrency explicitly.",
                        "EXPENSE_CURRENCY_REQUIRED"));
            }

            // Exchange rate: 1 paymentCurrency = X expenseCurrency
            BigDecimal exchangeRate;
            if (paymentCurrency.equals(expenseCurrency)) {
                exchangeRate = BigDecimal.ONE;
            } else if (dto.getExchangeRate() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Exchange rate is required when payment currency differs from expense currency.",
                        "EXCHANGE_RATE_REQUIRED"));
            } else {
                exchangeRate = dto.getExchangeRate();
            }

            BigDecimal baseAmount = dto.getAmount()
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);

            BankAccount bank = null;
            if (dto.getBankAccountId() != null && !dto.getBankAccountId().isBlank()) {
                Long bankId = idObfuscator.decodeId(dto.getBankAccountId());
                bank = bankAccountRepository.findById(bankId).orElse(null);
            }

            ExpensePayment payment = ExpensePayment.builder()
                .expense(expense)
                .amount(dto.getAmount())
                .currency(paymentCurrency)
                .paymentDate(dto.getPaymentDate())
                .paymentMethod(dto.getPaymentMethod())
                .expenseCurrency(expenseCurrency)
                .exchangeRate(exchangeRate)
                .baseAmount(baseAmount)
                .bankAccount(bank)
                .reference(dto.getReference())
                .notes(dto.getNotes())
                .recordedBy(getCurrentUser())
                .build();

            payment = expensePaymentRepository.save(payment);

            stateTransitionService.recomputeStatus(expense.getId());

            log.info("ExpensePayment recorded: expense={} amount={} {}",
                    expense.getExpenseCode(), payment.getAmount(), paymentCurrency);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Payment recorded", getService.toDTO(payment)));
        } catch (Exception e) {
            log.error("Error recording expense payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to record payment: " + e.getMessage(),
                        "EXPENSE_PAYMENT_CREATE_FAILED"));
        }
    }

    private String inferExpenseCurrency(Expense expense, String paymentCurrency) {
        List<Price> grandTotals = expense.getGrandTotals();
        if (grandTotals == null || grandTotals.isEmpty()) return null;
        // Single-currency expense: use that currency.
        if (grandTotals.size() == 1) return grandTotals.get(0).getCurrency();
        // Match payment currency to a grand-total currency if possible.
        for (Price p : grandTotals) {
            if (paymentCurrency.equalsIgnoreCase(p.getCurrency())) return p.getCurrency();
        }
        return null;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
