package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

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

import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.PaymentFilter;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.PaymentSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every payment received, across all the invoices.
 *
 * The per-invoice endpoint answers "what has this customer paid". This answers
 * the question a bank statement asks: what came in this week, into which account,
 * and which of it needs checking. Neither can be derived from the other without
 * already knowing the invoice, which is exactly what the person reconciling does
 * not know.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentListService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "paymentDate", "amount", "currency", "paymentMethod", "reference", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "paymentDate";

    private final PaymentRepository paymentRepository;
    private final PaymentCreateService paymentCreateService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        PaymentFilter filter,
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

            Specification<Payment> spec = buildSpec(filter != null ? filter : new PaymentFilter());
            Page<Payment> found = paymentRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("payments", found.getContent().stream()
                .map(paymentCreateService::convertToDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
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

            return ResponseEntity.ok(ApiResponse.success(200, "Payments retrieved", response));
        } catch (Exception e) {
            log.error("Error listing payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list payments", "PAYMENTS_LIST_FAILED"));
        }
    }

    /** One payment, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        PaymentFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Payment payment = paymentRepository.findById(id).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND"));
            }

            Specification<Payment> navSpec = buildSpec(filter != null ? filter : new PaymentFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                Payment.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("payment", paymentCreateService.convertToDTO(payment));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Payment retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payment", "PAYMENT_FETCH_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<Payment> buildSpec(PaymentFilter filter) {
        Specification<Payment> spec = Specification.<Payment>unrestricted()
            .and(PaymentSpecification.byMethods(filter.allMethods()))
            .and(PaymentSpecification.byCurrencies(filter.getCurrencies()))
            .and(PaymentSpecification.paidAfter(filter.getPaidAfter()))
            .and(PaymentSpecification.paidBefore(filter.getPaidBefore()))
            .and(PaymentSpecification.byReference(filter.getReference()))
            .and(PaymentSpecification.searchKeyword(filter.getKeyword()));

        spec = and(spec, filter.getInvoiceId(), PaymentSpecification::byInvoiceId, "invoice");
        spec = and(spec, filter.getCustomerId(), PaymentSpecification::byCustomerId, "customer");
        spec = and(spec, filter.getSafariId(), PaymentSpecification::bySafariId, "safari");
        spec = and(spec, filter.getBankAccountId(), PaymentSpecification::byBankAccountId, "bank account");

        // what needs checking, OR'd together
        Specification<Payment> quality = null;
        if (filter.wants("crossCurrency")) quality = or(quality, PaymentSpecification.crossCurrency());
        if (filter.wants("noBankAccount")) quality = or(quality, PaymentSpecification.noBankAccount());
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<Payment> or(Specification<Payment> spec, Specification<Payment> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * Narrows by an obfuscated id, or narrows to nothing if it will not decode.
     *
     * An unreadable id must not quietly widen the list to every payment — that is
     * the opposite of what was asked for, and here it would be money.
     */
    private Specification<Payment> and(
        Specification<Payment> spec,
        String obfuscated,
        java.util.function.Function<Long, Specification<Payment>> by,
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

    /**
     * The cards that head the list.
     *
     * Counts, never sums. Payments arrive in several currencies and adding them
     * would need a rate nobody here chose — the money itself is read per row and
     * per currency on the invoice it answers.
     */
    private Map<String, Object> buildStats(Specification<Payment> spec) {
        return listStats.of(Payment.class, spec)
            .total()
            .count("last7Days", PaymentSpecification.receivedWithin(7))
            .count("last30Days", PaymentSpecification.receivedWithin(30))
            .breakdown("byMethod", PaymentMethod.values(), PaymentSpecification::byMethod)
            .count("crossCurrency", PaymentSpecification.crossCurrency())
            .count("noBankAccount", PaymentSpecification.noBankAccount())
            .build();
    }
}
