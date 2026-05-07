package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpensePaymentDeleteService {

    private final ExpensePaymentRepository repository;
    private final ExpenseStateTransitionService stateTransitionService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_EXPENSE_PAYMENT",
        entityType = "EXPENSE",
        entityIdParamName = "expenseIdObfuscated",
        description = "Delete an expense payment record"
    )
    public ResponseEntity<ApiResponse<?>> deletePayment(
            String expenseIdObfuscated,
            String paymentIdObfuscated,
            boolean force) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Long paymentId = idObfuscator.decodeId(paymentIdObfuscated);

            ExpensePayment p = repository.findById(paymentId).orElse(null);
            if (p == null || !p.getExpense().getId().equals(expenseId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "EXPENSE_PAYMENT_NOT_FOUND"));
            }

            // Optional safety net: refuse to break a fully-paid expense unless force=true.
            // The state transition service will recompute status afterwards either way.
            if (!force && p.getExpense().getStatus()
                    == com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus.PAID) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Deleting this payment will break a fully-paid expense. "
                            + "Re-issue the request with force=true if intentional.",
                        "EXPENSE_PAYMENT_BREAKS_PAID"));
            }

            repository.deleteById(paymentId);

            stateTransitionService.recomputeStatus(expenseId);

            return ResponseEntity.ok(ApiResponse.success(200, "Payment deleted", null));
        } catch (Exception e) {
            log.error("Error deleting expense payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete payment: " + e.getMessage(),
                        "EXPENSE_PAYMENT_DELETE_FAILED"));
        }
    }
}
