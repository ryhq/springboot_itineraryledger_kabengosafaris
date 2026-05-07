package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.ReorderExpenseItemsDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseLineItemReorderService {

    private final ExpenseLineItemRepository repository;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "REORDER_EXPENSE_LINE_ITEMS",
        entityType = "EXPENSE_LINE_ITEM",
        description = "Reorder line items on an expense"
    )
    public ResponseEntity<ApiResponse<?>> reorder(String expenseIdObfuscated, ReorderExpenseItemsDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);

            List<ExpenseLineItem> ordered = new ArrayList<>();
            for (String s : dto.getItemIds()) {
                Long id = idObfuscator.decodeId(s);
                ExpenseLineItem li = repository.findById(id).orElse(null);
                if (li == null || !li.getExpense().getId().equals(expenseId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Item " + s + " not part of this expense",
                                "INVALID_ITEM"));
                }
                ordered.add(li);
            }

            // Two-phase rebase to avoid any unique-constraint races, mirroring
            // the InvoiceLineItem reorder pattern.
            for (ExpenseLineItem li : ordered) {
                li.setDisplayOrder(li.getDisplayOrder() + 10000);
            }
            repository.saveAll(ordered);
            repository.flush();

            for (int i = 0; i < ordered.size(); i++) {
                ordered.get(i).setDisplayOrder(i);
            }
            repository.saveAll(ordered);

            return ResponseEntity.ok(ApiResponse.success(200, "Line items reordered", null));
        } catch (Exception e) {
            log.error("Error reordering expense line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder line items: " + e.getMessage(),
                        "EXPENSE_LINE_ITEM_REORDER_FAILED"));
        }
    }
}
