package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpensePaymentFilter;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpensePaymentSpecification;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every payment made, across all the bills.
 *
 * The mirror of the incoming list, and it exists for the same reason: the person
 * reconciling a bank statement has a line and a date, not a bill. Without this
 * they would have to guess which supplier it belonged to before they could look
 * for it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpensePaymentListService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "paymentDate", "amount", "currency", "paymentMethod", "reference", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "paymentDate";

    private final ExpensePaymentRepository paymentRepository;
    private final ExpensePaymentGetService getService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        ExpensePaymentFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<ExpensePayment> spec =
                buildSpec(filter != null ? filter : new ExpensePaymentFilter());
            Page<ExpensePayment> found = paymentRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("payments", found.getContent().stream().map(getService::toDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Payments retrieved", response));
        } catch (Exception e) {
            log.error("Error listing expense payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list payments", "EXPENSE_PAYMENTS_LIST_FAILED"));
        }
    }

    /** One payment, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        ExpensePaymentFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            ExpensePayment payment = paymentRepository.findById(id).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "EXPENSE_PAYMENT_NOT_FOUND"));
            }

            Specification<ExpensePayment> navSpec =
                buildSpec(filter != null ? filter : new ExpensePaymentFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                ExpensePayment.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("payment", getService.toDTO(payment));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Payment retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching expense payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payment", "EXPENSE_PAYMENT_FETCH_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<ExpensePayment> buildSpec(ExpensePaymentFilter filter) {
        Specification<ExpensePayment> spec = Specification.<ExpensePayment>unrestricted()
            .and(ExpensePaymentSpecification.byMethods(filter.allMethods()))
            .and(ExpensePaymentSpecification.byCurrencies(filter.getCurrencies()))
            .and(ExpensePaymentSpecification.paidAfter(filter.getPaidAfter()))
            .and(ExpensePaymentSpecification.paidBefore(filter.getPaidBefore()))
            .and(ExpensePaymentSpecification.byReference(filter.getReference()))
            .and(ExpensePaymentSpecification.searchKeyword(filter.getKeyword()));

        spec = and(spec, filter.getExpenseId(), ExpensePaymentSpecification::byExpenseId, "bill");
        spec = and(spec, filter.getVendorId(), ExpensePaymentSpecification::byVendorId, "vendor");
        spec = and(spec, filter.getSafariId(), ExpensePaymentSpecification::bySafariId, "safari");
        spec = and(spec, filter.getBankAccountId(),
            ExpensePaymentSpecification::byBankAccountId, "bank account");

        Specification<ExpensePayment> quality = null;
        if (filter.wants("crossCurrency")) {
            quality = or(quality, ExpensePaymentSpecification.crossCurrency());
        }
        if (filter.wants("noBankAccount")) {
            quality = or(quality, ExpensePaymentSpecification.noBankAccount());
        }
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<ExpensePayment> or(
        Specification<ExpensePayment> spec, Specification<ExpensePayment> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    private Specification<ExpensePayment> and(
        Specification<ExpensePayment> spec,
        String obfuscated,
        java.util.function.Function<Long, Specification<ExpensePayment>> by,
        String what
    ) {
        if (obfuscated == null || obfuscated.isBlank()) return spec;
        try {
            return spec.and(by.apply(idObfuscator.decodeId(obfuscated)));
        } catch (Exception e) {
            log.warn("Unreadable {} id on the payments filter: {}", what, obfuscated);
            return spec.and((root, query, cb) -> cb.disjunction());
        }
    }

    /** Counts, never sums — payments arrive in several currencies. */
    private Map<String, Object> buildStats(Specification<ExpensePayment> spec) {
        return listStats.of(ExpensePayment.class, spec)
            .total()
            .count("last7Days", ExpensePaymentSpecification.paidWithin(7))
            .count("last30Days", ExpensePaymentSpecification.paidWithin(30))
            .breakdown("byMethod", PaymentMethod.values(), ExpensePaymentSpecification::byMethod)
            .count("crossCurrency", ExpensePaymentSpecification.crossCurrency())
            .count("noBankAccount", ExpensePaymentSpecification.noBankAccount())
            .build();
    }
}
