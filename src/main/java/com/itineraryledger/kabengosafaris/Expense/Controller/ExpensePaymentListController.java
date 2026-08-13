package com.itineraryledger.kabengosafaris.Expense.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices.ExpensePaymentListService;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpensePaymentFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Money out, across every bill.
 *
 * The mirror of /api/payments, for the same reason: reconciling a statement means
 * starting from a line and a date rather than from the bill it answered.
 */
@RestController
@RequestMapping("/api/expense-payments")
@RequiredArgsConstructor
@Slf4j
public class ExpensePaymentListController {

    private final ExpensePaymentListService listService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> list(
        @ModelAttribute ExpensePaymentFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/expense-payments");
        return listService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getOne(
        @PathVariable String id,
        // the list's filters and sort, so prev/next walks that same set
        @ModelAttribute ExpensePaymentFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.getOne(id, filter, sortBy, sortDirection);
    }
}
