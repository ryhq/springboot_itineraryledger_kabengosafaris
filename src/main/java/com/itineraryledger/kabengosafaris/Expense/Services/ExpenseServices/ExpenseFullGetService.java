package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.FullExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices.ExpenseLineItemGetService;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices.ExpensePaymentGetService;
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
@Transactional(readOnly = true)
public class ExpenseFullGetService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseGetService expenseGetService;
    private final ExpenseLineItemGetService lineItemGetService;
    private final ExpensePaymentGetService paymentGetService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getFullExpense(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            FullExpenseDTO dto = FullExpenseDTO.builder()
                .expense(expenseGetService.toDTO(expense))
                .lineItems(lineItemGetService.listForExpense(expense.getId()))
                .payments(paymentGetService.listForExpense(expense.getId()))
                .build();

            return ResponseEntity.ok(ApiResponse.success(200, "Expense retrieved successfully", dto));
        } catch (Exception e) {
            log.error("Error fetching full expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch expense", "EXPENSE_FETCH_FAILED"));
        }
    }
}
