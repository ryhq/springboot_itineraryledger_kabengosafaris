package com.itineraryledger.kabengosafaris.Expense.Controller;

import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseDTO;
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
    public ResponseEntity<ApiResponse<?>> getExpenseById(@PathVariable String id) {
        return expenseGetService.getExpenseById(id);
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getFullExpense(@PathVariable String id) {
        return expenseFullGetService.getFullExpense(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getAllExpenses(
            @RequestParam(required = false) String expenseCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String safariId,
            @RequestParam(required = false) Boolean operationalOnly,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isOverdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDateAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDateBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateBefore,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return expenseGetService.getAllExpenses(expenseCode, title, status, vendorId, safariId,
                operationalOnly, isActive, isOverdue,
                expenseDateAfter, expenseDateBefore, dueDateAfter, dueDateBefore,
                referenceNumber, page, size, sortBy, sortDirection);
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
}
