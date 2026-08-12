package com.itineraryledger.kabengosafaris.Expense.Controller;

import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpenseFilter;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@Slf4j
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseGetService expenseGetService;
    private final ExpenseFullGetService expenseFullGetService;
    private final ExpenseCreateService expenseCreateService;
    private final ExpenseUpdateService expenseUpdateService;
    private final ExpenseDeleteService expenseDeleteService;
    private final ExpenseTotalsCalculationService totalsCalculationService;
    private final IdObfuscator idObfuscator;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> createExpense(@Valid @RequestBody CreateExpenseDTO dto) {
        log.info("POST /api/expenses - vendor={}, safari={}", dto.getVendorId(), dto.getSafariId());
        return expenseCreateService.createExpense(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getExpenseById(
            @PathVariable String id,
            // the list's filters and sort, so prev/next walks that same set
            @ModelAttribute ExpenseFilter filter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return expenseGetService.getExpenseById(id, filter, sortBy, sortDirection);
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getFullExpense(@PathVariable String id) {
        return expenseFullGetService.getFullExpense(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getAllExpenses(
            @ModelAttribute ExpenseFilter filter,
            @RequestParam(required = false) Boolean includeStats,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return expenseGetService.getAllExpenses(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> updateExpense(
            @PathVariable String id,
            @Valid @RequestBody UpdateExpenseDTO dto) {
        return expenseUpdateService.updateExpense(id, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> deleteExpenses(@RequestBody List<String> ids) {
        return expenseDeleteService.deleteExpenses(ids);
    }

    @PostMapping("/{id}/recalculate-totals")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> recalculateTotals(@PathVariable String id) {
        try {
            Long decoded = idObfuscator.decodeId(id);
            totalsCalculationService.recalculateTotals(decoded);
            return expenseGetService.getExpenseById(id);
        } catch (Exception e) {
            log.error("Failed to recalculate expense totals", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to recalculate totals: " + e.getMessage(),
                        "RECALC_FAILED"));
        }
    }

    /**
     * POST /{id}/cancel — we do not owe this after all.
     *
     * Refused once money has been paid against it: that is a refund, not a
     * cancellation, and the two are different facts.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> cancel(@PathVariable String id) {
        log.info("POST /api/expenses/{}/cancel", id);
        return expenseUpdateService.setCancelled(id, true);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> reopen(@PathVariable String id) {
        log.info("POST /api/expenses/{}/reopen", id);
        return expenseUpdateService.setCancelled(id, false);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository bulkFlagsRepository;

    /** PATCH /bulk — one request for a whole selection, with per-id outcomes. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EXPENSE')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("bill", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
