package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseLineItemDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpenseTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseLineItemCreateService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseLineItemRepository repository;
    private final ExpenseTotalsCalculationService totalsService;
    private final ExpenseLineItemGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_EXPENSE_LINE_ITEM",
        entityType = "EXPENSE_LINE_ITEM",
        description = "Add a line item to an expense"
    )
    public ResponseEntity<ApiResponse<?>> createLineItem(String expenseIdObfuscated, CreateExpenseLineItemDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }
            if (!expense.isEditable()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Cannot add a line item once the expense is past DRAFT.",
                        "EXPENSE_NOT_EDITABLE"));
            }

            int nextOrder = (repository.findMaxDisplayOrderByExpenseId(expenseId) + 1);

            List<Price> prices = new ArrayList<>();
            for (CreateExpenseLineItemDTO.PriceInput in : dto.getPrices()) {
                BigDecimal qty = BigDecimal.valueOf(in.getQuantity());
                BigDecimal total = in.getUnitPrice().multiply(qty);
                prices.add(Price.builder()
                    .currency(in.getCurrency().toUpperCase().trim())
                    .quantity(in.getQuantity())
                    .unitPrice(in.getUnitPrice())
                    .totalPrice(total)
                    .breakdown(in.getBreakdown())
                    .build());
            }

            ExpenseLineItem item = ExpenseLineItem.builder()
                .expense(expense)
                .category(dto.getCategory())
                .itemName(dto.getItemName().trim())
                .description(dto.getDescription())
                .displayOrder(nextOrder)
                .prices(prices)
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .build();

            item = repository.save(item);

            totalsService.recalculateTotals(expense);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Line item added", getService.toDTO(item)));
        } catch (Exception e) {
            log.error("Error creating expense line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add line item: " + e.getMessage(),
                        "EXPENSE_LINE_ITEM_CREATE_FAILED"));
        }
    }
}
