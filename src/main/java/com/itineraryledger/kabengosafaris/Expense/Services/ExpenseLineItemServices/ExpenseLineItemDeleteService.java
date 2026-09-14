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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

            /* index-aligned with itemIds, so a skipped entry names the id the caller sent */
            List<Long> ids = new ArrayList<>();
            for (String s : itemIds) {
                try {
                    ids.add(idObfuscator.decodeId(s));
                } catch (Exception e) {
                    log.warn("Failed to decode line-item id: {}", s);
                    ids.add(null);
                }
            }
            if (ids.stream().allMatch(java.util.Objects::isNull)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No valid line item IDs provided", "INVALID_IDS"));
            }

            /*
             * Every refusal comes back named. This used to `continue` silently three times over,
             * then answer 200 with a null body, so the caller was told its delete succeeded while
             * the row stayed exactly where it was.
             */
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();
            Set<Long> affectedExpenseIds = new HashSet<>();

            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.get(i);
                String encodedId = itemIds.get(i);
                if (id == null) {
                    skipped.add(Map.of("id", encodedId, "reason", "Unreadable id"));
                    continue;
                }
                ExpenseLineItem item = repository.findById(id).orElse(null);
                if (item == null) {
                    skipped.add(Map.of("id", encodedId, "reason", "Line item not found"));
                    continue;
                }
                // Parent-scope guard so callers can't reach into a different expense's items.
                if (!item.getExpense().getId().equals(expenseId)) {
                    skipped.add(Map.of("id", encodedId, "reason", "It belongs to a different bill"));
                    continue;
                }

                Expense parent = item.getExpense();
                if (!parent.isEditable()) {
                    log.warn("Refusing to delete line item {} — parent expense not editable", id);
                    skipped.add(Map.of(
                        "id", encodedId,
                        "reason", "The bill is " + parent.getStatus() + " and can no longer be edited"
                    ));
                    continue;
                }

                affectedExpenseIds.add(parent.getId());
                repository.deleteById(id);
                deletedIds.add(encodedId);
            }
            int deleted = deletedIds.size();

            if (deleted > 0) {
                for (Long affected : affectedExpenseIds) {
                    totalsService.recalculateTotals(affected);
                }
            }

            String msg = deleted + " line item(s) deleted successfully";
            if (!skipped.isEmpty()) {
                msg += ", " + skipped.size() + " skipped";
            }
            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", deleted);
            data.put("deletedIds", deletedIds);
            data.put("skipped", skipped);
            return ResponseEntity.ok(ApiResponse.success(200, msg, data));
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
