package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

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

import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Specifications.QuoteFilter;
import com.itineraryledger.kabengosafaris.Quote.Specifications.QuoteSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving quotes with filtering, pagination, and sorting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "quoteCode", "title", "status", "sentDate", "validFrom", "validTo", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final QuoteRepository quoteRepository;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final IdObfuscator idObfuscator;

    /**
     * Get a single quote by obfuscated ID
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the quote
     */
    public ResponseEntity<ApiResponse<?>> getQuoteById(String idObfuscated) {
        // no filters supplied: the walk is over every quote
        return getQuoteById(idObfuscated, new QuoteFilter(), null, null);
    }

    /**
     * One quote, plus where it sits in the set the caller was looking at.
     *
     * The filters and sort come from the list page so prev/next stays inside
     * that set — see buildSpec.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getQuoteById(
        String idObfuscated,
        QuoteFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching quote with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            Quote quote = quoteRepository.findById(id).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            QuoteDTO quoteDTO = convertToDTO(quote);

            /*
             * Prev/next walks the SAME filtered, sorted set the list was showing.
             * It used to walk a raw id-ordered query, so paging out of a "Sent"
             * list landed in a draft — arrows that traverse a different set than
             * the one on screen are worse than no arrows.
             */
            Specification<Quote> navSpec = buildSpec(filter != null ? filter : new QuoteFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                Quote.class, navSpec, navSortBy, ascending, id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("quote", quoteDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote", "QUOTE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single quote by quote code
     *
     * @param quoteCode The quote code
     * @return ResponseEntity with ApiResponse containing the quote
     */
    public ResponseEntity<ApiResponse<?>> getQuoteByCode(String quoteCode) {
        log.info("Fetching quote with code: {}", quoteCode);

        try {
            // Find quote
            Quote quote = quoteRepository.findByQuoteCode(quoteCode).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Convert to DTO
            QuoteDTO quoteDTO = convertToDTO(quote);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote retrieved successfully", quoteDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote", "QUOTE_FETCH_FAILED")
            );
        }
    }

    /**
     * A page of quotes, plus the counters for the whole filtered set.
     *
     * @param filter what to narrow by; every field optional
     * @param includeStats false to skip the counters (the rows alone are cheaper)
     */
    public ResponseEntity<ApiResponse<?>> getAllQuotes(
        QuoteFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all quotes with filters");

        try {
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Specification<Quote> spec = buildSpec(filter != null ? filter : new QuoteFilter());

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set sorting (always sort by createdAt)
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch quotes
            Page<Quote> quotePage = quoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<QuoteDTO> quoteDTOs = quotePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("quotes", quoteDTOs);
            response.put("currentPage", quotePage.getNumber());
            response.put("totalItems", quotePage.getTotalElements());
            response.put("totalPages", quotePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            /*
             * Counters for the whole filtered set, not this page. Opt-out rather
             * than opt-in: a list page without them shows a scope toggle it
             * cannot honour.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotes retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching quotes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quotes", "QUOTES_FETCH_FAILED")
            );
        }
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
     * Convert Quote entity to QuoteDTO
     */
    public QuoteDTO convertToDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.builder()
            .id(idObfuscator.encodeId(quote.getId()))
            .quoteCode(quote.getQuoteCode())
            .title(quote.getTitle())
            .description(quote.getDescription())
            .itineraryId(idObfuscator.encodeId(quote.getItinerary().getId()))
            .itineraryCode(quote.getItinerary().getCode())
            .itineraryName(quote.getItinerary().getName())
            .customerId(idObfuscator.encodeId(quote.getCustomer().getId()))
            .customerName(quote.getCustomer().getDisplayName())
            .customerEmail(quote.getCustomer().getPrimaryEmail())
            .subtotals(quote.getSubtotals())
            .taxes(quote.getTaxes())
            .discounts(quote.getDiscounts())
            .grandTotals(quote.getGrandTotals())
            .isStoRate(quote.getIsStoRate())
            .taxPercentage(quote.getTaxPercentage())
            .discountPercentage(quote.getDiscountPercentage())
            .discountReason(quote.getDiscountReason())
            .agentCommissionPercentage(quote.getAgentCommissionPercentage())
            .agentCommissionReason(quote.getAgentCommissionReason())
            .marginUpliftPercentage(quote.getMarginUpliftPercentage())
            .marginUpliftReason(quote.getMarginUpliftReason())
            .condenseItems(quote.getCondenseItems())
            .version(quote.getVersion())
            .status(quote.getStatus())
            // the DTO promised a readable status and never filled it, so every
            // client fell back to printing the enum constant
            .statusDisplayName(quote.getStatus() != null ? quote.getStatus().getDisplayName() : null)
            .safariStartDate(quote.getSafariStartDate())
            .sentDate(quote.getSentDate())
            .validFrom(quote.getValidFrom())
            .validTo(quote.getValidTo())
            .isValid(quote.getIsValid())
            .depositPercentage(quote.getDepositPercentage())
            .depositDueDate(quote.getDepositDueDate())
            .fullPaymentDueDate(quote.getFullPaymentDueDate())
            .internalNotes(quote.getInternalNotes())
            .customerNotes(quote.getCustomerNotes())
            .versionNotes(quote.getVersionNotes())
            .isActive(quote.getIsActive())
            .itemCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentCount(quote.getDocuments() != null ? (long) quote.getDocuments().size() : 0L)
            .itemsCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentsCount(quote.getDocuments() != null ? (long) quote.getDocuments().size() : 0L)
            .daysCount(quote.getDays() != null ? (long) quote.getDays().size() : 0L)
            .paxCount(quote.getPaxList() != null ? (long) quote.getPaxList().size() : 0L)
            .createdAt(quote.getCreatedAt())
            .updatedAt(quote.getUpdatedAt())
            .build();

        // Set approver if present
        if (quote.getApprover() != null) {
            dto.setApproverId(idObfuscator.encodeId(quote.getApprover().getId()));
            dto.setApproverName(quote.getApprover().getUsername());
        }

        // Set approved by if present
        if (quote.getApprovedBy() != null) {
            dto.setApprovedById(idObfuscator.encodeId(quote.getApprovedBy().getId()));
            dto.setApprovedByName(quote.getApprovedBy().getUsername());
            dto.setApprovedAt(quote.getApprovedAt());
            dto.setApprovalNotes(quote.getApprovalNotes());
        }

        // Set previous version if present
        if (quote.getPreviousVersion() != null) {
            dto.setPreviousVersionId(idObfuscator.encodeId(quote.getPreviousVersion().getId()));
            dto.setPreviousVersionCode(quote.getPreviousVersion().getQuoteCode());
        }

        // Set next version if present
        if (quote.getNextVersion() != null) {
            dto.setNextVersionId(idObfuscator.encodeId(quote.getNextVersion().getId()));
            dto.setNextVersionCode(quote.getNextVersion().getQuoteCode());
        }

        // Set created by if present
        if (quote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(quote.getCreatedBy().getId()));
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (quote.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(quote.getUpdatedBy().getId()));
            dto.setUpdatedByName(quote.getUpdatedBy().getUsername());
        }

        return dto;
    }

    /**
     * ONE specification, shared by the rows, the counters and the record walk.
     *
     * The three used to be able to disagree: the list filtered, the stat cards
     * did not exist, and prev/next walked a raw id-ordered query — so paging out
     * of a "Sent" list landed you in a draft. Anything built from this cannot
     * drift from what is on screen.
     */
    private Specification<Quote> buildSpec(QuoteFilter filter) {
        Specification<Quote> spec = Specification.unrestricted();

        // the search box: one param, several columns, joins included
        spec = spec.and(QuoteSpecification.byKeyword(filter.getKeyword()));

        if (filter.getQuoteCode() != null && !filter.getQuoteCode().isEmpty()) {
            spec = spec.and(QuoteSpecification.byQuoteCode(filter.getQuoteCode()));
        }
        if (filter.getTitle() != null && !filter.getTitle().isEmpty()) {
            spec = spec.and(QuoteSpecification.byTitle(filter.getTitle()));
        }
        // OR inside the dimension, AND across dimensions
        spec = spec.and(QuoteSpecification.byStatuses(filter.allStatuses()));
        spec = spec.and(QuoteSpecification.byStatusGroups(filter.allStatusGroups()));

        if (filter.getIsStoRate() != null) {
            spec = spec.and(QuoteSpecification.byIsStoRate(filter.getIsStoRate()));
        }
        if (filter.getIsActive() != null) {
            spec = spec.and(QuoteSpecification.byIsActive(filter.getIsActive()));
        }
        if (filter.getValidOn() != null) {
            spec = spec.and(QuoteSpecification.byValidOn(filter.getValidOn()));
        }
        if (filter.getSentAfter() != null) {
            spec = spec.and(QuoteSpecification.bySentAfter(filter.getSentAfter()));
        }
        if (filter.getSentBefore() != null) {
            spec = spec.and(QuoteSpecification.bySentBefore(filter.getSentBefore()));
        }
        if (filter.getVersion() != null) {
            spec = spec.and(QuoteSpecification.byVersion(filter.getVersion()));
        }

        spec = and(spec, filter.getItineraryId(), QuoteSpecification::byItineraryId, "itinerary");
        spec = and(spec, filter.getCustomerId(), QuoteSpecification::byCustomerId, "customer");
        spec = and(spec, filter.getApproverId(), QuoteSpecification::byApproverId, "approver");
        spec = and(spec, filter.getApprovedById(), QuoteSpecification::byApprovedById, "approved by");
        spec = and(spec, filter.getCreatedById(), QuoteSpecification::byCreatedById, "created by");
        spec = and(spec, filter.getUpdatedById(), QuoteSpecification::byUpdatedById, "updated by");

        return spec;
    }

    /**
     * Narrows by an obfuscated id, or narrows to nothing if it will not decode.
     *
     * A id that cannot be read used to be logged and dropped, which silently
     * widened the result to every quote — the opposite of what was asked for.
     */
    private Specification<Quote> and(
        Specification<Quote> spec,
        String obfuscatedId,
        java.util.function.Function<Long, Specification<Quote>> by,
        String what
    ) {
        if (obfuscatedId == null || obfuscatedId.isEmpty()) return spec;
        try {
            return spec.and(by.apply(idObfuscator.decodeId(obfuscatedId)));
        } catch (Exception e) {
            log.warn("Failed to decode {} ID: {}", what, obfuscatedId);
            return spec.and((root, query, cb) -> cb.disjunction());
        }
    }

    /**
     * Counters for the whole filtered set, built from the SAME specification.
     *
     * Every figure here is reachable as a filter, and every filter has a figure —
     * a card that cannot be clicked is decoration, and a filter with no counter
     * is a question the page refuses to answer.
     */
    private Map<String, Object> buildStats(Specification<Quote> base) {
        return listStats.of(Quote.class, base)
            .total()
            .count("active", QuoteSpecification.byIsActive(true))
            .complement("inactive", "active")
            .breakdown("byStatus", QuoteStatus.values(), QuoteSpecification::byStatus)
            .count("draftGroup", QuoteSpecification.byDraftStatus())
            .count("pendingGroup", QuoteSpecification.byPendingStatuses())
            .count("activeGroup", QuoteSpecification.byActiveStatuses())
            .count("closedGroup", QuoteSpecification.byClosedStatuses())
            // priced at operator rates rather than published ones
            .count("stoRate", QuoteSpecification.byIsStoRate(true))
            .complement("rackRate", "stoRate")
            // valid today: the ones that can still be accepted as they stand
            .count("validNow", QuoteSpecification.byValidOn(java.time.LocalDate.now()))
            .recency(QuoteSpecification::createdAfter)
            .build();
    }
}
