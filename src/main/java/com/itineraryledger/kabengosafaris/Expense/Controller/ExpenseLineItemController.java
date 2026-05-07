package com.itineraryledger.kabengosafaris.Expense.Controller;

import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseLineItemDTO;
import com.itineraryledger.kabengosafaris.Expense.DTOs.ReorderExpenseItemsDTO;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpenseLineItemDTO;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseLineItemServices.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses/{expenseId}/line-items")
@Slf4j
@RequiredArgsConstructor
public class ExpenseLineItemController {

    private final ExpenseLineItemGetService getService;
    private final ExpenseLineItemCreateService createService;
    private final ExpenseLineItemUpdateService updateService;
    private final ExpenseLineItemDeleteService deleteService;
    private final ExpenseLineItemReorderService reorderService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> createLineItem(
            @PathVariable String expenseId,
            @Valid @RequestBody CreateExpenseLineItemDTO dto) {
        return createService.createLineItem(expenseId, dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> listLineItems(@PathVariable String expenseId) {
        return getService.getAllForExpense(expenseId);
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> getLineItem(@PathVariable String expenseId,
                                                       @PathVariable String itemId) {
        return getService.getById(expenseId, itemId);
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> updateLineItem(
            @PathVariable String expenseId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateExpenseLineItemDTO dto) {
        return updateService.updateLineItem(expenseId, itemId, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> deleteLineItems(
            @PathVariable String expenseId,
            @RequestBody List<String> itemIds) {
        return deleteService.deleteLineItems(expenseId, itemIds);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE_LINE_ITEM')")
    public ResponseEntity<ApiResponse<?>> reorderLineItems(
            @PathVariable String expenseId,
            @Valid @RequestBody ReorderExpenseItemsDTO dto) {
        return reorderService.reorder(expenseId, dto);
    }
}
