package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpenseFilter;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpenseSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseGetService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseLineItemRepository expenseLineItemRepository;
    private final ExpensePaymentAggregationService paymentAggregationService;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "expenseCode", "title", "expenseDate", "dueDate", "status", "createdAt", "updatedAt"
    );

    public ResponseEntity<ApiResponse<?>> getExpenseById(String idObfuscated) {
        return getExpenseById(idObfuscated, null, null, null);
    }

    /**
     * One bill, plus where it sits in the set the caller was looking at.
     *
     * Paging out of an "overdue" list must stay among overdue bills — arrows that
     * traverse a different set from the one on screen are worse than no arrows.
     */
    public ResponseEntity<ApiResponse<?>> getExpenseById(
            String idObfuscated,
            ExpenseFilter filter,
            String sortBy,
            String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            Specification<Expense> navSpec = buildSpec(filter != null ? filter : new ExpenseFilter());
            String navSortBy = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
            Map<String, Object> nav = recordNavigation.navigate(
                    Expense.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);
            Long navNextId = (Long) nav.get("nextRawId");
            Long navPreviousId = (Long) nav.get("previousRawId");

            Long nextId = expenseRepository.findNextId(id).orElse(null);
            Long previousId = expenseRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = expenseRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = expenseRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("expense", toDTO(expense));
            response.put("nextId", navNextId != null ? idObfuscator.encodeId(navNextId) : null);
            response.put("previousId", navPreviousId != null ? idObfuscator.encodeId(navPreviousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Expense retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch expense", "EXPENSE_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllExpenses(
            ExpenseFilter filter,
            Boolean includeStats,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        try {
            int pageNo = page == null || page < 0 ? 0 : page;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = size == null || size <= 0 ? 10 : Math.min(size, 100);
            String resolvedSort = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                    ? sortBy : "createdAt";
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, resolvedSort));

            Specification<Expense> spec = buildSpec(filter != null ? filter : new ExpenseFilter());
            Page<Expense> paged = expenseRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("expenses", paged.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", paged.getNumber());
            response.put("totalItems", paged.getTotalElements());
            response.put("totalPages", paged.getTotalPages());
            response.put("pageSize", paged.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows, so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Expenses retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing expenses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list expenses", "EXPENSE_LIST_FAILED"));
        }
    }

    /**
     * ONE specification, shared by the rows, the counters and the record walk.
     *
     * Every dimension ORs inside itself and ANDs across — "accommodation or park
     * fee, unpaid, for this safari" is one question.
     */
    private Specification<Expense> buildSpec(ExpenseFilter filter) {
        Specification<Expense> spec = Specification.<Expense>unrestricted()
            .and(ExpenseSpecification.byExpenseCode(filter.getExpenseCode()))
            .and(ExpenseSpecification.byTitle(filter.getTitle()))
            .and(ExpenseSpecification.byStatuses(filter.allStatuses()))
            .and(ExpenseSpecification.byCategories(filter.getCategories()))
            .and(ExpenseSpecification.byVendorId(decodeOrNull(filter.getVendorId())))
            .and(ExpenseSpecification.bySafariId(decodeOrNull(filter.getSafariId())))
            .and(ExpenseSpecification.bySafariIsNull(filter.getOperationalOnly()))
            .and(ExpenseSpecification.byIsActive(filter.getIsActive()))
            .and(ExpenseSpecification.byExpenseDateAfter(filter.getExpenseDateAfter()))
            .and(ExpenseSpecification.byExpenseDateBefore(filter.getExpenseDateBefore()))
            .and(ExpenseSpecification.byDueDateAfter(filter.getDueDateAfter()))
            .and(ExpenseSpecification.byDueDateBefore(filter.getDueDateBefore()))
            .and(ExpenseSpecification.byReferenceNumber(filter.getReferenceNumber()))
            .and(ExpenseSpecification.searchKeyword(filter.getKeyword()));

        if (filter.getCreatedById() != null && !filter.getCreatedById().isBlank()) {
            spec = spec.and(ExpenseSpecification.byCreatedById(decodeOrNull(filter.getCreatedById())));
        }
        if (Boolean.TRUE.equals(filter.getIsOverdue())) {
            spec = spec.and(ExpenseSpecification.byOverdue());
        }

        // the chase list, OR'd: "what needs paying"
        Specification<Expense> quality = null;
        if (filter.wants("unpaid")) quality = or(quality, ExpenseSpecification.byUnpaid());
        if (filter.wants("overdue")) quality = or(quality, ExpenseSpecification.byOverdue());
        if (filter.wants("dueSoon")) quality = or(quality, ExpenseSpecification.dueWithin(7));
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<Expense> or(Specification<Expense> spec, Specification<Expense> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * The cards that head the list, every one of them reachable as a filter.
     *
     * No money on any card. A bill's total is a list of amounts per currency —
     * a TZS lodge invoice and a USD park fee — and one figure across them would
     * be an exchange rate nobody here chose. What is owed in each currency
     * belongs on the safari's Money tab, where both sides are known.
     */
    private Map<String, Object> buildStats(Specification<Expense> spec) {
        return listStats.of(Expense.class, spec)
            .total()
            .count("active", ExpenseSpecification.byIsActive(true))
            .complement("inactive", "active")
            .breakdown("byStatus", ExpenseStatus.values(), ExpenseSpecification::byStatus)
            .count("unpaid", ExpenseSpecification.byUnpaid())
            .count("overdue", ExpenseSpecification.byOverdue())
            .count("dueSoon", ExpenseSpecification.dueWithin(7))
            .count("operational", ExpenseSpecification.bySafariIsNull(true))
            .recency(ExpenseSpecification::createdAfter)
            .build();
    }

    private Long decodeOrNull(String obf) {
        if (obf == null || obf.isBlank()) return null;
        try { return idObfuscator.decodeId(obf); } catch (Exception e) { return null; }
    }

    public ExpenseDTO toDTO(Expense e) {
        long lineItemCount = e.getId() != null ? expenseLineItemRepository.countByExpenseId(e.getId()) : 0;
        return ExpenseDTO.builder()
            .id(idObfuscator.encodeId(e.getId()))
            .expenseCode(e.getExpenseCode())
            .title(e.getTitle())
            .description(e.getDescription())
            .vendorId(e.getVendor() != null ? idObfuscator.encodeId(e.getVendor().getId()) : null)
            .vendorName(e.getVendor() != null ? e.getVendor().getName() : null)
            .vendorType(e.getVendor() != null && e.getVendor().getType() != null
                    ? e.getVendor().getType().getDisplayName() : null)
            .safariId(e.getSafari() != null ? idObfuscator.encodeId(e.getSafari().getId()) : null)
            .safariCode(e.getSafari() != null ? e.getSafari().getCode() : null)
            .safariName(e.getSafari() != null ? e.getSafari().getName() : null)
            .subtotals(e.getSubtotals())
            .taxes(e.getTaxes())
            .grandTotals(e.getGrandTotals())
            .amountsPaid(paymentAggregationService.computeAmountsPaid(e))
            .balances(paymentAggregationService.computeBalances(e))
            .taxPercentage(e.getTaxPercentage())
            .expenseDate(e.getExpenseDate())
            .dueDate(e.getDueDate())
            .referenceNumber(e.getReferenceNumber())
            .status(e.getStatus())
            .statusDisplayName(e.getStatus() != null ? e.getStatus().getDisplayName() : null)
            .internalNotes(e.getInternalNotes())
            .isActive(e.getIsActive())
            .isOverdue(e.isOverdue())
            .lineItemCount(lineItemCount)
            .createdById(e.getCreatedBy() != null ? idObfuscator.encodeId(e.getCreatedBy().getId()) : null)
            .createdByName(e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null)
            .updatedById(e.getUpdatedBy() != null ? idObfuscator.encodeId(e.getUpdatedBy().getId()) : null)
            .updatedByName(e.getUpdatedBy() != null ? e.getUpdatedBy().getUsername() : null)
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .build();
    }
}
