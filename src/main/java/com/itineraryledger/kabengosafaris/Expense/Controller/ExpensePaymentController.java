package com.itineraryledger.kabengosafaris.Expense.Controller;

import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpensePaymentDTO;
import com.itineraryledger.kabengosafaris.Expense.DTOs.SendPaymentAdviceDTO;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpensePaymentDTO;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses/{expenseId}/payments")
@Slf4j
@RequiredArgsConstructor
public class ExpensePaymentController {

    private final ExpensePaymentGetService getService;
    private final ExpensePaymentCreateService createService;
    private final ExpensePaymentUpdateService updateService;
    private final ExpensePaymentDeleteService deleteService;
    private final ExpensePaymentAdviceService adviceService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_RECORD_EXPENSE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> recordPayment(
            @PathVariable String expenseId,
            @Valid @RequestBody CreateExpensePaymentDTO dto) {
        return createService.createPayment(expenseId, dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> listPayments(@PathVariable String expenseId) {
        return getService.getPaymentsForExpense(expenseId);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getPayment(@PathVariable String expenseId,
                                                      @PathVariable String paymentId) {
        return getService.getPaymentById(expenseId, paymentId);
    }

    @PutMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_RECORD_EXPENSE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> updatePayment(
            @PathVariable String expenseId,
            @PathVariable String paymentId,
            @Valid @RequestBody UpdateExpensePaymentDTO dto) {
        return updateService.updatePayment(expenseId, paymentId, dto);
    }

    /**
     * POST /{paymentId}/advice — tell the supplier we have paid them.
     *
     * By hand, not on a state change: correcting a mistyped reference would otherwise email them a
     * second time about money that only moved once. `to` overrides the vendor's own address, for
     * the accounts desk that is not the reservations desk.
     */
    @PostMapping("/{paymentId}/advice")
    @PreAuthorize("hasAuthority('PERM_RECORD_EXPENSE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> sendAdvice(
            @PathVariable String expenseId,
            @PathVariable String paymentId,
            @RequestBody(required = false) SendPaymentAdviceDTO dto) {
        log.info("POST /api/expenses/{}/payments/{}/advice", expenseId, paymentId);
        return adviceService.send(
            paymentId,
            dto == null ? null : dto.getTo(),
            dto == null ? null : dto.getEmailTemplateId());
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_RECORD_EXPENSE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> deletePayment(
            @PathVariable String expenseId,
            @PathVariable String paymentId,
            @RequestParam(defaultValue = "false") boolean force) {
        return deleteService.deletePayment(expenseId, paymentId, force);
    }
}
