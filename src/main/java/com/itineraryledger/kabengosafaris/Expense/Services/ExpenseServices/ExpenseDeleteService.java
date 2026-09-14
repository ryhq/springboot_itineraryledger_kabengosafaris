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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        /*
         * Index-aligned with idsObfuscated on purpose: a skipped entry has to name the id the
         * caller sent, and dropping an undecodable one would shift every id after it.
         */
        List<Long> ids = new ArrayList<>();
        for (String s : idsObfuscated) {
            try {
                ids.add(idObfuscator.decodeId(s));
            } catch (Exception e) {
                log.warn("Failed to decode expense id: {}", s);
                ids.add(null);
            }
        }

        try {
            /*
             * A refusal has to come back as data, not as prose.
             *
             * This counted what it refused and then threw the count away, answering 200 with a
             * null body. The panel has nothing structured to read in that case, so it reports
             * every id it asked about as deleted: a partially paid bill stayed on the list under
             * a toast saying "1 Bill(s) deleted". A 200 that silently deleted nothing is the one
             * answer this contract forbids (CLAUDE.md).
             */
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.get(i);
                String encodedId = idsObfuscated.get(i);
                if (id == null) {
                    skipped.add(Map.of("id", encodedId, "reason", "Unreadable id"));
                    continue;
                }
                Expense expense = expenseRepository.findById(id).orElse(null);
                if (expense == null) {
                    skipped.add(Map.of("id", encodedId, "reason", "Bill not found"));
                    continue;
                }
                if (!expense.isDeletable()) {
                    log.warn("Refusing to delete non-DRAFT expense {}", expense.getExpenseCode());
                    skipped.add(Map.of(
                        "id", encodedId,
                        "code", expense.getExpenseCode() != null ? expense.getExpenseCode() : "",
                        "reason", "It is " + expense.getStatus() + ", not a draft. Cancel it instead of deleting it."
                    ));
                    continue;
                }
                expenseRepository.deleteById(id);
                deletedIds.add(encodedId);
            }

            String msg = deletedIds.size() + " bill(s) deleted successfully";
            if (!skipped.isEmpty()) {
                msg += ", " + skipped.size() + " skipped";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", deletedIds.size());
            data.put("deletedIds", deletedIds);
            data.put("skipped", skipped);
            return ResponseEntity.ok(ApiResponse.success(200, msg, data));
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
