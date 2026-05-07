package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpenseTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mirrors the InvoiceLineItemDeleteService fixes: do NOT swallow per-item
 * exceptions silently inside @Transactional, and surface FK constraint
 * violations as a clean 409.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseLineItemDeleteService {

    private final ExpenseLineItemRepository repository;
    private final ExpenseTotalsCalculationService totalsService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_EXPENSE_LINE_ITEM",
        entityType = "EXPENSE_LINE_ITEM",
        description = "Delete one or more expense line items"
    )
    public ResponseEntity<ApiResponse<?>> deleteLineItems(String expenseIdObfuscated, List<String> itemIds) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);

            List<Long> ids = new ArrayList<>();
            for (String s : itemIds) {
                try { ids.add(idObfuscator.decodeId(s)); }
                catch (Exception e) { log.warn("Failed to decode line-item id: {}", s); }
            }
            if (ids.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No valid line item IDs provided", "INVALID_IDS"));
            }

            int deleted = 0;
            Set<Long> affectedExpenseIds = new HashSet<>();
            for (Long id : ids) {
                ExpenseLineItem item = repository.findById(id).orElse(null);
                if (item == null) continue;
                // Parent-scope guard so callers can't reach into a different expense's items.
                if (!item.getExpense().getId().equals(expenseId)) continue;

                Expense parent = item.getExpense();
                if (!parent.isEditable()) {
                    log.warn("Refusing to delete line item {} — parent expense not editable", id);
                    continue;
                }

                affectedExpenseIds.add(parent.getId());
                repository.deleteById(id);
                deleted++;
            }

            if (deleted > 0) {
                for (Long affected : affectedExpenseIds) {
                    totalsService.recalculateTotals(affected);
                }
            }

            return ResponseEntity.ok(ApiResponse.success(200,
                deleted + " line item(s) deleted successfully", null));
        } catch (DataIntegrityViolationException e) {
            log.warn("Expense line-item delete blocked by FK", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(409,
                    "These line items are referenced elsewhere and cannot be deleted yet.",
                    "EXPENSE_LINE_ITEM_REFERENCED"));
        } catch (Exception e) {
            log.error("Error deleting expense line items", e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete line items: " + detail,
                        "EXPENSE_LINE_ITEMS_DELETE_FAILED"));
        }
    }
}
