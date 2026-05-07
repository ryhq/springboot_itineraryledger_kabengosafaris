package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpenseLineItemDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
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
public class ExpenseLineItemUpdateService {

    private final ExpenseLineItemRepository repository;
    private final ExpenseTotalsCalculationService totalsService;
    private final ExpenseLineItemGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_EXPENSE_LINE_ITEM",
        entityType = "EXPENSE_LINE_ITEM",
        entityIdParamName = "itemIdObfuscated",
        description = "Update a line item on an expense"
    )
    public ResponseEntity<ApiResponse<?>> updateLineItem(
            String expenseIdObfuscated,
            String itemIdObfuscated,
            UpdateExpenseLineItemDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Long itemId = idObfuscator.decodeId(itemIdObfuscated);

            ExpenseLineItem item = repository.findById(itemId).orElse(null);
            if (item == null || !item.getExpense().getId().equals(expenseId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Line item not found", "EXPENSE_LINE_ITEM_NOT_FOUND"));
            }
            if (!item.getExpense().isEditable()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Cannot update a line item once the expense is past DRAFT.",
                        "EXPENSE_NOT_EDITABLE"));
            }

            if (dto.getCategory() != null) item.setCategory(dto.getCategory());
            if (dto.getItemName() != null) item.setItemName(dto.getItemName().trim());
            if (dto.getDescription() != null) item.setDescription(dto.getDescription());
            if (dto.getIsActive() != null) item.setIsActive(dto.getIsActive());

            if (dto.getPrices() != null) {
                List<Price> rebuilt = new ArrayList<>();
                for (UpdateExpenseLineItemDTO.PriceInput in : dto.getPrices()) {
                    if (in.getCurrency() == null || in.getQuantity() == null || in.getUnitPrice() == null) continue;
                    BigDecimal qty = BigDecimal.valueOf(in.getQuantity());
                    BigDecimal total = in.getUnitPrice().multiply(qty);
                    rebuilt.add(Price.builder()
                        .currency(in.getCurrency().toUpperCase().trim())
                        .quantity(in.getQuantity())
                        .unitPrice(in.getUnitPrice())
                        .totalPrice(total)
                        .breakdown(in.getBreakdown())
                        .build());
                }
                item.getPrices().clear();
                item.getPrices().addAll(rebuilt);
            }

            item = repository.save(item);

            totalsService.recalculateTotals(item.getExpense());

            return ResponseEntity.ok(ApiResponse.success(200, "Line item updated", getService.toDTO(item)));
        } catch (Exception e) {
            log.error("Error updating expense line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update line item: " + e.getMessage(),
                        "EXPENSE_LINE_ITEM_UPDATE_FAILED"));
        }
    }
}
