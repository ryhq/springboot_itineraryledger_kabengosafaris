package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseAllocationsDTO;
import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseAllocationDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseAllocation;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseAllocationRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What each bill covers, across the days of a trip.
 *
 * The office's real question is never "what does this bill say" but "has this
 * night been paid for, and by which invoice" — and the answer is a relation, not
 * a field. This service keeps that relation and answers it from both ends: from a
 * bill (what am I paying for) and from a safari (what is already covered).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseAllocationService {

    private final ExpenseAllocationRepository allocationRepository;
    private final ExpenseRepository expenseRepository;
    private final SafariRepository safariRepository;
    private final ExpensePaymentAggregationService aggregationService;
    private final IdObfuscator idObfuscator;

    /* --------------------------------- reads --------------------------------- */

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> forExpense(String expenseIdObf) {
        Long expenseId = decode(expenseIdObf);
        if (expenseId == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid bill id", "INVALID_EXPENSE_ID"));
        }

        List<ExpenseAllocationDTO> rows = allocationRepository
            .findByExpenseIdOrderByDayNumberAsc(expenseId).stream()
            .map(this::toDTO)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("allocations", rows);
        response.put("totalItems", rows.size());
        return ResponseEntity.ok(ApiResponse.success(200, "Coverage retrieved", response));
    }

    /**
     * Everything billed on one safari, in one request.
     *
     * The day tree marks every stay, park and activity it draws, so asking per row
     * would be dozens of round trips on a fourteen-day trip. It is also why the
     * bill's own details ride along on each row.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> forSafari(String safariIdObf) {
        Long safariId = decode(safariIdObf);
        if (safariId == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid safari id", "INVALID_SAFARI_ID"));
        }

        List<ExpenseAllocationDTO> rows = allocationRepository.findBySafariWithExpense(safariId)
            .stream()
            .map(this::toDTO)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("allocations", rows);
        response.put("totalItems", rows.size());
        return ResponseEntity.ok(ApiResponse.success(200, "Coverage retrieved", response));
    }

    /* --------------------------------- writes -------------------------------- */

    @Transactional
    public ResponseEntity<ApiResponse<?>> attach(String expenseIdObf, CreateExpenseAllocationsDTO dto) {
        Long expenseId = decode(expenseIdObf);
        if (expenseId == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid bill id", "INVALID_EXPENSE_ID"));
        }

        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Bill not found", "EXPENSE_NOT_FOUND"));
        }

        List<ExpenseAllocationDTO> attached = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (CreateExpenseAllocationsDTO.Subject subject : dto.getSubjects()) {
            Long subjectId = decode(subject.getSubjectId());
            if (subjectId == null) {
                skipped.add(skip(subject.getSubjectName(), "Unreadable id"));
                continue;
            }

            /*
             * The same thing twice on the same bill is a mistake, not a second
             * charge — a second charge is a second bill. Saying so beats silently
             * doubling what the coverage view reports.
             */
            if (allocationRepository.existsByExpenseIdAndSubjectTypeAndSubjectId(
                    expenseId, subject.getSubjectType(), subjectId)) {
                skipped.add(skip(subject.getSubjectName(), "Already covered by this bill"));
                continue;
            }

            ExpenseAllocation allocation = ExpenseAllocation.builder()
                .expense(expense)
                .safari(expense.getSafari())
                .subjectType(subject.getSubjectType())
                .subjectId(subjectId)
                .safariDayId(decode(subject.getSafariDayId()))
                .dayNumber(subject.getDayNumber())
                .dayDate(parseDate(subject.getDayDate()))
                .subjectName(subject.getSubjectName())
                .share(subject.getShare())
                .shareCurrency(subject.getShareCurrency())
                .note(subject.getNote())
                .build();

            attached.add(toDTO(allocationRepository.save(allocation)));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("allocations", attached);
        response.put("attachedCount", attached.size());
        response.put("skipped", skipped);

        String message = attached.size() + (attached.size() == 1 ? " item covered" : " items covered")
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");
        return ResponseEntity.ok(ApiResponse.success(200, message, response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> detach(String expenseIdObf, List<String> ids) {
        Long expenseId = decode(expenseIdObf);
        if (expenseId == null || ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Nothing to remove", "NO_IDS"));
        }

        List<String> removed = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscated : ids) {
            Long id = decode(obfuscated);
            ExpenseAllocation allocation = id == null
                ? null
                : allocationRepository.findById(id).orElse(null);

            if (allocation == null) {
                skipped.add(skip(obfuscated, "No longer exists"));
                continue;
            }
            if (allocation.getExpense() == null || !expenseId.equals(allocation.getExpense().getId())) {
                skipped.add(skip(obfuscated, "Belongs to a different bill"));
                continue;
            }
            allocationRepository.delete(allocation);
            removed.add(obfuscated);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("removedCount", removed.size());
        response.put("removedIds", removed);
        response.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200,
            removed.size() + " no longer covered by this bill", response));
    }

    /* --------------------------------- mapping -------------------------------- */

    private ExpenseAllocationDTO toDTO(ExpenseAllocation a) {
        Expense e = a.getExpense();
        return ExpenseAllocationDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .subjectType(a.getSubjectType())
            .subjectTypeDisplayName(a.getSubjectType() != null ? a.getSubjectType().getDisplayName() : null)
            .subjectId(a.getSubjectId() != null ? idObfuscator.encodeId(a.getSubjectId()) : null)
            .safariDayId(a.getSafariDayId() != null ? idObfuscator.encodeId(a.getSafariDayId()) : null)
            .dayNumber(a.getDayNumber())
            .dayDate(a.getDayDate())
            .subjectName(a.getSubjectName())
            .share(a.getShare())
            .shareCurrency(a.getShareCurrency())
            .note(a.getNote())
            .expenseId(e != null ? idObfuscator.encodeId(e.getId()) : null)
            .expenseCode(e != null ? e.getExpenseCode() : null)
            .expenseTitle(e != null ? e.getTitle() : null)
            .expenseStatus(e != null && e.getStatus() != null ? e.getStatus().name() : null)
            .expenseStatusDisplayName(
                e != null && e.getStatus() != null ? e.getStatus().getDisplayName() : null)
            .vendorId(e != null && e.getVendor() != null
                ? idObfuscator.encodeId(e.getVendor().getId()) : null)
            .vendorName(e != null && e.getVendor() != null ? e.getVendor().getName() : null)
            .dueDate(e != null ? e.getDueDate() : null)
            .isOverdue(e != null ? isOverdue(e) : null)
            .grandTotals(e != null ? e.getGrandTotals() : null)
            .build();
    }

    /** Past the date they asked for it, and not settled. */
    private boolean isOverdue(Expense e) {
        if (e.getDueDate() == null) return false;
        if (e.getStatus() == null) return false;
        return e.getDueDate().isBefore(LocalDate.now())
            && switch (e.getStatus()) {
                case PAID, CANCELLED -> false;
                default -> true;
            };
    }

    private Long decode(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> skip(String what, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("subject", what);
        entry.put("reason", reason);
        return entry;
    }
}
