package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.InvoiceFilter;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.InvoiceSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving invoices with filtering, pagination, and sorting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "invoiceCode", "title", "status", "issueDate", "dueDate", "sentDate", "paidDate", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final InvoicePaymentAggregationService paymentAggregationService;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    /**
     * Get a single invoice by obfuscated ID
     *
     * @param idObfuscated The obfuscated invoice ID
     * @return ResponseEntity with ApiResponse containing the invoice
     */
    public ResponseEntity<ApiResponse<?>> getInvoiceById(
        String idObfuscated,
        InvoiceFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching invoice with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            // Find invoice
            Invoice invoice = invoiceRepository.findById(id).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            /*
             * Prev/next walks the SAME set the list was showing.
             *
             * It used to walk every invoice in id order, so paging out of an
             * "Overdue" list landed on a paid one — arrows that traverse a
             * different set from the one on screen are worse than no arrows.
             */
            Specification<Invoice> navSpec = buildSpec(filter != null ? filter : new InvoiceFilter());
            String navSortBy = validateSortField(sortBy) != null
                ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                Invoice.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("invoice", invoiceDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice", "INVOICE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single invoice by invoice code
     *
     * @param invoiceCode The invoice code
     * @return ResponseEntity with ApiResponse containing the invoice
     */
    public ResponseEntity<ApiResponse<?>> getInvoiceByCode(String invoiceCode) {
        log.info("Fetching invoice with code: {}", invoiceCode);

        try {
            // Find invoice
            Invoice invoice = invoiceRepository.findByInvoiceCode(invoiceCode).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice retrieved successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice", "INVOICE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all invoices with pagination, sorting, and filtering
     *
     * @param invoiceCode Filter by invoice code (partial match)
     * @param title Filter by title (partial match)
     * @param status Filter by status
     * @param paymentStatus Filter by payment status
     * @param customerId Filter by customer ID (obfuscated)
     * @param safariId Filter by safari ID (obfuscated)
     * @param createdById Filter by created by ID (obfuscated)
     * @param updatedById Filter by updated by ID (obfuscated)
     * @param isActive Filter by active status
     * @param issueDateAfter Filter by issue date after
     * @param issueDateBefore Filter by issue date before
     * @param dueDateAfter Filter by due date after
     * @param dueDateBefore Filter by due date before
     * @param sentAfter Filter by sent after date
     * @param sentBefore Filter by sent before date
     * @param isOverdue Filter by overdue status
     * @param statusGroup Filter by status group (draft, unpaid, paid, closed)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated invoices
     */
    public ResponseEntity<ApiResponse<?>> getAllInvoices(
        InvoiceFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all invoices with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Specification<Invoice> spec = buildSpec(filter != null ? filter : new InvoiceFilter());

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = (size != null && size > 0) ? Math.min(size, 100) : 10;

            // Set sorting (always sort by createdAt)
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch invoices
            Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<InvoiceDTO> invoiceDTOs = invoicePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("invoices", invoiceDTOs);
            response.put("currentPage", invoicePage.getNumber());
            response.put("totalItems", invoicePage.getTotalElements());
            response.put("totalPages", invoicePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /*
             * The counters ride inside the list response and are built from the
             * SAME specification as the rows, so a card and the table under it
             * can never disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoices retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoices", "INVOICES_FETCH_FAILED")
            );
        }
    }

    /**
     * ONE specification, shared by the rows, the counters and the record walk.
     *
     * Every dimension ORs inside itself and ANDs across — asking for "sent or
     * overdue, for this customer" is one question, not two lists intersected by
     * hand afterwards.
     */
    private Specification<Invoice> buildSpec(InvoiceFilter filter) {
        Specification<Invoice> spec = Specification.unrestricted();

        if (filter.getInvoiceCode() != null && !filter.getInvoiceCode().isEmpty()) {
            spec = spec.and(InvoiceSpecification.byInvoiceCode(filter.getInvoiceCode()));
        }
        if (filter.getTitle() != null && !filter.getTitle().isEmpty()) {
            spec = spec.and(InvoiceSpecification.byTitle(filter.getTitle()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            spec = spec.and(InvoiceSpecification.searchKeyword(filter.getKeyword()));
        }
        spec = spec.and(InvoiceSpecification.byStatuses(filter.allStatuses()));
        spec = spec.and(InvoiceSpecification.byStatusGroups(filter.allStatusGroups()));
        if (filter.getIsActive() != null) {
            spec = spec.and(InvoiceSpecification.byIsActive(filter.getIsActive()));
        }
        if (Boolean.TRUE.equals(filter.getIsOverdue())) {
            spec = spec.and(InvoiceSpecification.byOverdue());
        }
        if (filter.getIsSupplement() != null) {
            spec = spec.and(InvoiceSpecification.bySupplement(filter.getIsSupplement()));
        }
        if (filter.getIssueDateAfter() != null) {
            spec = spec.and(InvoiceSpecification.byIssueDateAfter(filter.getIssueDateAfter()));
        }
        if (filter.getIssueDateBefore() != null) {
            spec = spec.and(InvoiceSpecification.byIssueDateBefore(filter.getIssueDateBefore()));
        }
        if (filter.getDueDateAfter() != null) {
            spec = spec.and(InvoiceSpecification.byDueDateAfter(filter.getDueDateAfter()));
        }
        if (filter.getDueDateBefore() != null) {
            spec = spec.and(InvoiceSpecification.byDueDateBefore(filter.getDueDateBefore()));
        }
        if (filter.getSentAfter() != null) {
            spec = spec.and(InvoiceSpecification.bySentAfter(filter.getSentAfter()));
        }
        if (filter.getSentBefore() != null) {
            spec = spec.and(InvoiceSpecification.bySentBefore(filter.getSentBefore()));
        }

        // data-quality asks, OR'd: "show me what needs chasing"
        Specification<Invoice> quality = null;
        if (filter.wants("dueSoon")) quality = or(quality, InvoiceSpecification.dueWithin(7));
        if (filter.wants("unsent")) quality = or(quality, InvoiceSpecification.unsent());
        if (filter.wants("unpaid")) quality = or(quality, InvoiceSpecification.unpaid());
        if (filter.wants("overdue")) quality = or(quality, InvoiceSpecification.byOverdue());
        if (quality != null) spec = spec.and(quality);

        spec = and(spec, filter.getCustomerId(), InvoiceSpecification::byCustomerId, "customer");
        spec = and(spec, filter.getSafariId(), InvoiceSpecification::bySafariId, "safari");
        spec = and(spec, filter.getCreatedById(), InvoiceSpecification::byCreatedById, "createdBy");
        spec = and(spec, filter.getUpdatedById(), InvoiceSpecification::byUpdatedById, "updatedBy");
        return spec;
    }

    private Specification<Invoice> or(Specification<Invoice> spec, Specification<Invoice> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * Narrows by an obfuscated id, or narrows to nothing if it will not decode.
     *
     * An unreadable id must not quietly widen the list to every invoice — that is
     * the opposite of what was asked for.
     */
    private Specification<Invoice> and(
        Specification<Invoice> spec,
        String obfuscatedId,
        java.util.function.Function<Long, Specification<Invoice>> by,
        String what
    ) {
        if (obfuscatedId == null || obfuscatedId.isEmpty()) return spec;
        try {
            return spec.and(by.apply(idObfuscator.decodeId(obfuscatedId)));
        } catch (Exception e) {
            log.warn("Unreadable {} id on the invoice filter: {}", what, obfuscatedId);
            return spec.and((root, query, cb) -> cb.disjunction());
        }
    }

    /**
     * The cards that head the list, every one of them reachable as a filter.
     *
     * Nothing here is a figure the rows cannot support: the money is deliberately
     * absent, because an invoice's total is a list of amounts per currency and a
     * single number across them would be an invented exchange rate.
     */
    private Map<String, Object> buildStats(Specification<Invoice> spec) {
        return listStats.of(Invoice.class, spec)
            .total()
            .count("active", InvoiceSpecification.byIsActive(true))
            .complement("inactive", "active")
            .breakdown("byStatus", InvoiceStatus.values(), InvoiceSpecification::byStatus)
            .count("draft", InvoiceSpecification.byDraftStatus())
            .count("unpaid", InvoiceSpecification.unpaid())
            .count("overdue", InvoiceSpecification.byOverdue())
            .count("dueSoon", InvoiceSpecification.dueWithin(7))
            .count("unsent", InvoiceSpecification.unsent())
            .count("supplements", InvoiceSpecification.bySupplement(true))
            .recency(InvoiceSpecification::createdAfter)
            .build();
    }

    /**
     * Validate and return the sort field, or null if invalid
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Invoice entity to InvoiceDTO
     */
    public InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = InvoiceDTO.builder()
            .id(idObfuscator.encodeId(invoice.getId()))
            .invoiceCode(invoice.getInvoiceCode())
            .title(invoice.getTitle())
            .description(invoice.getDescription())
            .isSupplement(Boolean.TRUE.equals(invoice.getIsSupplement()))
            .supplementReason(invoice.getSupplementReason())
            .subtotals(invoice.getSubtotals())
            .taxes(invoice.getTaxes())
            .discounts(invoice.getDiscounts())
            .grandTotals(invoice.getGrandTotals())
            .amountsPaid(paymentAggregationService.computeAmountsPaid(invoice))
            .balances(paymentAggregationService.computeBalances(invoice))
            .taxPercentage(invoice.getTaxPercentage())
            .discountPercentage(invoice.getDiscountPercentage())
            .discountReason(invoice.getDiscountReason())
            .agentCommissionPercentage(invoice.getAgentCommissionPercentage())
            .agentCommissionReason(invoice.getAgentCommissionReason())
            .marginUpliftPercentage(invoice.getMarginUpliftPercentage())
            .marginUpliftReason(invoice.getMarginUpliftReason())
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .sentDate(invoice.getSentDate())
            .paidDate(invoice.getPaidDate())
            .status(invoice.getStatus())
            .statusDisplayName(invoice.getStatus().getDisplayName())
            .internalNotes(invoice.getInternalNotes())
            .customerNotes(invoice.getCustomerNotes())
            .paymentTerms(invoice.getPaymentTerms())
            .isActive(invoice.getIsActive())
            .isOverdue(invoice.isOverdue())
            .lineItemCount(invoice.getLineItems() != null ? (long) invoice.getLineItems().size() : 0L)
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .build();

        // Set customer if present
        if (invoice.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(invoice.getCustomer().getId()));
            dto.setCustomerName(invoice.getCustomer().getDisplayName());
            dto.setCustomerEmail(invoice.getCustomer().getPrimaryEmail());
        }

        // Set safari if present
        if (invoice.getSafari() != null) {
            dto.setSafariId(idObfuscator.encodeId(invoice.getSafari().getId()));
            dto.setSafariCode(invoice.getSafari().getCode());
            dto.setSafariName(invoice.getSafari().getName());
        }

        // Set created by if present
        if (invoice.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(invoice.getCreatedBy().getId()));
            dto.setCreatedByName(invoice.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (invoice.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(invoice.getUpdatedBy().getId()));
            dto.setUpdatedByName(invoice.getUpdatedBy().getUsername());
        }

        return dto;
    }
}
