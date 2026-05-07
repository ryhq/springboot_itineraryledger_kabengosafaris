package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseDeleteService {

    private final ExpenseRepository expenseRepository;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_EXPENSE",
        entityType = "EXPENSE",
        description = "Delete one or more expenses"
    )
    public ResponseEntity<ApiResponse<?>> deleteExpenses(List<String> idsObfuscated) {
        if (idsObfuscated == null || idsObfuscated.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No expense IDs provided", "INVALID_IDS"));
        }

        List<Long> ids = new ArrayList<>();
        for (String s : idsObfuscated) {
            try { ids.add(idObfuscator.decodeId(s)); }
            catch (Exception e) { log.warn("Failed to decode expense id: {}", s); }
        }

        try {
            int deleted = 0;
            int blocked = 0;
            for (Long id : ids) {
                Expense expense = expenseRepository.findById(id).orElse(null);
                if (expense == null) continue;
                if (!expense.isDeletable()) {
                    log.warn("Refusing to delete non-DRAFT expense {}", expense.getExpenseCode());
                    blocked++;
                    continue;
                }
                expenseRepository.deleteById(id);
                deleted++;
            }

            String msg = deleted + " expense(s) deleted successfully";
            if (blocked > 0) {
                msg += " — " + blocked + " skipped (only DRAFT expenses can be deleted; cancel paid ones instead)";
            }
            return ResponseEntity.ok(ApiResponse.success(200, msg, null));
        } catch (DataIntegrityViolationException e) {
            log.warn("Expense delete blocked by FK: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(409,
                    "One or more expenses are referenced and cannot be deleted.",
                    "EXPENSE_REFERENCED"));
        } catch (Exception e) {
            log.error("Error deleting expenses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete expenses: " + e.getMessage(),
                        "EXPENSE_DELETE_FAILED"));
        }
    }
}
