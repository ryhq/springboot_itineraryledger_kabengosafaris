package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseLineItemDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseLineItemGetService {

    private final ExpenseLineItemRepository repository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getAllForExpense(String expenseIdObfuscated) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            List<ExpenseLineItem> items = repository.findByExpenseIdOrderByDisplayOrderAsc(expenseId);
            return ResponseEntity.ok(ApiResponse.success(200, "Line items retrieved",
                    items.stream().map(this::toDTO).toList()));
        } catch (Exception e) {
            log.error("Error fetching expense line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch line items", "EXPENSE_LINE_ITEMS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String expenseIdObfuscated, String itemIdObfuscated) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Long itemId = idObfuscator.decodeId(itemIdObfuscated);

            ExpenseLineItem item = repository.findById(itemId).orElse(null);
            if (item == null || !item.getExpense().getId().equals(expenseId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Line item not found", "EXPENSE_LINE_ITEM_NOT_FOUND"));
            }

            /*
             * The list comes back ordered by displayOrder, so the arrows follow displayOrder
             * too — the id-ordered walk this replaces moved through a different sequence from
             * the one on screen, and could not say where in it you were.
             */
            Long parentId = expenseId;
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                ExpenseLineItem.class,
                (root, query, cb) -> cb.equal(root.get("expense").get("id"), parentId),
                "displayOrder",
                true,
                itemId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("lineItem", toDTO(item));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Line item retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching expense line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch line item", "EXPENSE_LINE_ITEM_FETCH_FAILED"));
        }
    }

    public List<ExpenseLineItemDTO> listForExpense(Long expenseId) {
        return repository.findByExpenseIdOrderByDisplayOrderAsc(expenseId).stream()
                .map(this::toDTO).toList();
    }

    public ExpenseLineItemDTO toDTO(ExpenseLineItem li) {
        return ExpenseLineItemDTO.builder()
            .id(idObfuscator.encodeId(li.getId()))
            .expenseId(li.getExpense() != null ? idObfuscator.encodeId(li.getExpense().getId()) : null)
            .expenseCode(li.getExpense() != null ? li.getExpense().getExpenseCode() : null)
            .category(li.getCategory())
            .categoryDisplayName(li.getCategory() != null ? li.getCategory().getDisplayName() : null)
            .itemName(li.getItemName())
            .description(li.getDescription())
            .displayOrder(li.getDisplayOrder())
            .prices(li.getPrices())
            .isActive(li.getIsActive())
            .createdAt(li.getCreatedAt())
            .updatedAt(li.getUpdatedAt())
            .build();
    }
}
