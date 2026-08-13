package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpensePaymentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpensePaymentGetService {

    private final ExpensePaymentRepository repository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getPaymentsForExpense(String expenseIdObfuscated) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            return ResponseEntity.ok(ApiResponse.success(200, "Payments retrieved",
                    listForExpense(expenseId)));
        } catch (Exception e) {
            log.error("Error fetching expense payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payments", "EXPENSE_PAYMENTS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getPaymentById(String expenseIdObfuscated, String paymentIdObfuscated) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Long paymentId = idObfuscator.decodeId(paymentIdObfuscated);

            ExpensePayment p = repository.findById(paymentId).orElse(null);
            if (p == null || !p.getExpense().getId().equals(expenseId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "EXPENSE_PAYMENT_NOT_FOUND"));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Payment retrieved", toDTO(p)));
        } catch (Exception e) {
            log.error("Error fetching expense payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payment", "EXPENSE_PAYMENT_FETCH_FAILED"));
        }
    }

    public List<ExpensePaymentDTO> listForExpense(Long expenseId) {
        return repository.findByExpenseIdOrderByPaymentDateDesc(expenseId).stream()
                .map(this::toDTO).toList();
    }

    public ExpensePaymentDTO toDTO(ExpensePayment p) {
        return ExpensePaymentDTO.builder()
            .id(idObfuscator.encodeId(p.getId()))
            .expenseId(p.getExpense() != null ? idObfuscator.encodeId(p.getExpense().getId()) : null)
            .expenseCode(p.getExpense() != null ? p.getExpense().getExpenseCode() : null)
            .expenseTitle(p.getExpense() != null ? p.getExpense().getTitle() : null)
            // who we paid and which trip it was for
            .vendorId(p.getExpense() != null && p.getExpense().getVendor() != null
                ? idObfuscator.encodeId(p.getExpense().getVendor().getId()) : null)
            .vendorName(p.getExpense() != null && p.getExpense().getVendor() != null
                ? p.getExpense().getVendor().getName() : null)
            .safariId(p.getExpense() != null && p.getExpense().getSafari() != null
                ? idObfuscator.encodeId(p.getExpense().getSafari().getId()) : null)
            .safariName(p.getExpense() != null && p.getExpense().getSafari() != null
                ? p.getExpense().getSafari().getName() : null)
            .amount(p.getAmount())
            .currency(p.getCurrency())
            .paymentDate(p.getPaymentDate())
            .paymentMethod(p.getPaymentMethod())
            .paymentMethodDisplayName(p.getPaymentMethod() != null ? p.getPaymentMethod().getDisplayName() : null)
            .expenseCurrency(p.getExpenseCurrency())
            .exchangeRate(p.getExchangeRate())
            .baseAmount(p.getBaseAmount())
            .bankAccountId(p.getBankAccount() != null
                    ? idObfuscator.encodeId(p.getBankAccount().getId()) : null)
            .bankAccountName(p.getBankAccount() != null ? p.getBankAccount().getAccountName() : null)
            .bankAccountCode(p.getBankAccount() != null ? p.getBankAccount().getAccountCode() : null)
            .reference(p.getReference())
            .notes(p.getNotes())
            .recordedById(p.getRecordedBy() != null ? idObfuscator.encodeId(p.getRecordedBy().getId()) : null)
            .recordedByName(p.getRecordedBy() != null ? p.getRecordedBy().getUsername() : null)
            .createdAt(p.getCreatedAt())
            .build();
    }
}
