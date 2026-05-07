package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "expenseCode", "title", "expenseDate", "dueDate", "status", "createdAt", "updatedAt"
    );

    public ResponseEntity<ApiResponse<?>> getExpenseById(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            Long nextId = expenseRepository.findNextId(id).orElse(null);
            Long previousId = expenseRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = expenseRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = expenseRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("expense", toDTO(expense));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Expense retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch expense", "EXPENSE_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllExpenses(
            String expenseCode,
            String title,
            ExpenseStatus status,
            String vendorIdObf,
            String safariIdObf,
            Boolean operationalOnly,
            Boolean isActive,
            Boolean isOverdue,
            LocalDate expenseDateAfter,
            LocalDate expenseDateBefore,
            LocalDate dueDateAfter,
            LocalDate dueDateBefore,
            String referenceNumber,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        try {
            int pageNo = page == null || page < 0 ? 0 : page;
            int pageSize = size == null || size <= 0 ? 10 : Math.min(size, 200);
            String resolvedSort = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                    ? sortBy : "createdAt";
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, resolvedSort));

            Long vendorId = decodeOrNull(vendorIdObf);
            Long safariId = decodeOrNull(safariIdObf);

            Specification<Expense> spec = Specification.unrestricted()
                .and(ExpenseSpecification.byExpenseCode(expenseCode))
                .and(ExpenseSpecification.byTitle(title))
                .and(ExpenseSpecification.byStatus(status))
                .and(ExpenseSpecification.byVendorId(vendorId))
                .and(ExpenseSpecification.bySafariId(safariId))
                .and(ExpenseSpecification.bySafariIsNull(operationalOnly))
                .and(ExpenseSpecification.byIsActive(isActive))
                .and(ExpenseSpecification.byExpenseDateAfter(expenseDateAfter))
                .and(ExpenseSpecification.byExpenseDateBefore(expenseDateBefore))
                .and(ExpenseSpecification.byDueDateAfter(dueDateAfter))
                .and(ExpenseSpecification.byDueDateBefore(dueDateBefore))
                .and(ExpenseSpecification.byReferenceNumber(referenceNumber));

            if (Boolean.TRUE.equals(isOverdue)) {
                spec = spec.and(ExpenseSpecification.byOverdue());
            }

            Page<Expense> paged = expenseRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("expenses", paged.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", paged.getNumber());
            response.put("totalItems", paged.getTotalElements());
            response.put("totalPages", paged.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);

            return ResponseEntity.ok(ApiResponse.success(200, "Expenses retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing expenses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list expenses", "EXPENSE_LIST_FAILED"));
        }
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
