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
    private final IdObfuscator idObfuscator;

    /**
     * Get a single quote by obfuscated ID
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the quote
     */
    public ResponseEntity<ApiResponse<?>> getQuoteById(String idObfuscated) {
        log.info("Fetching quote with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            // Find quote
            Quote quote = quoteRepository.findById(id).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Convert to DTO
            QuoteDTO quoteDTO = convertToDTO(quote);

            // Build navigation
            Long nextId = quoteRepository.findNextId(id).orElse(null);
            Long previousId = quoteRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = quoteRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = quoteRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("quote", quoteDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
     * Get all quotes with pagination, sorting, and filtering
     *
     * @param quoteCode Filter by quote code (partial match)
     * @param title Filter by title (partial match)
     * @param status Filter by status
     * @param itineraryId Filter by itinerary ID (obfuscated)
     * @param customerId Filter by customer ID (obfuscated)
     * @param approverId Filter by approver ID (obfuscated)
     * @param approvedById Filter by approved by ID (obfuscated)
     * @param createdById Filter by created by ID (obfuscated)
     * @param updatedById Filter by updated by ID (obfuscated)
     * @param isStoRate Filter by STO rate flag
     * @param isActive Filter by active status
     * @param validOn Filter by valid on date
     * @param sentAfter Filter by sent after date
     * @param sentBefore Filter by sent before date
     * @param version Filter by version number
     * @param statusGroup Filter by status group (draft, pending, active, closed)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated quotes
     */
    public ResponseEntity<ApiResponse<?>> getAllQuotes(
        String quoteCode,
        String title,
        QuoteStatus status,
        String itineraryId,
        String customerId,
        String approverId,
        String approvedById,
        String createdById,
        String updatedById,
        Boolean isStoRate,
        Boolean isActive,
        LocalDate validOn,
        LocalDate sentAfter,
        LocalDate sentBefore,
        Integer version,
        String statusGroup,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all quotes with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification for filtering
            Specification<Quote> spec = Specification.unrestricted();

            if (quoteCode != null && !quoteCode.isEmpty()) {
                spec = spec.and(QuoteSpecification.byQuoteCode(quoteCode));
            }
            if (title != null && !title.isEmpty()) {
                spec = spec.and(QuoteSpecification.byTitle(title));
            }
            if (status != null) {
                spec = spec.and(QuoteSpecification.byStatus(status));
            }
            if (isStoRate != null) {
                spec = spec.and(QuoteSpecification.byIsStoRate(isStoRate));
            }
            if (isActive != null) {
                spec = spec.and(QuoteSpecification.byIsActive(isActive));
            }
            if (validOn != null) {
                spec = spec.and(QuoteSpecification.byValidOn(validOn));
            }
            if (sentAfter != null) {
                spec = spec.and(QuoteSpecification.bySentAfter(sentAfter));
            }
            if (sentBefore != null) {
                spec = spec.and(QuoteSpecification.bySentBefore(sentBefore));
            }
            if (version != null) {
                spec = spec.and(QuoteSpecification.byVersion(version));
            }

            // Apply relationship filters (decode IDs)
            if (itineraryId != null && !itineraryId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(itineraryId);
                    spec = spec.and(QuoteSpecification.byItineraryId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode itinerary ID: {}", itineraryId, e);
                }
            }
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(customerId);
                    spec = spec.and(QuoteSpecification.byCustomerId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                }
            }
            if (approverId != null && !approverId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(approverId);
                    spec = spec.and(QuoteSpecification.byApproverId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode approver ID: {}", approverId, e);
                }
            }
            if (approvedById != null && !approvedById.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(approvedById);
                    spec = spec.and(QuoteSpecification.byApprovedById(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode approved by ID: {}", approvedById, e);
                }
            }
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(createdById);
                    spec = spec.and(QuoteSpecification.byCreatedById(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode created by ID: {}", createdById, e);
                }
            }
            if (updatedById != null && !updatedById.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(updatedById);
                    spec = spec.and(QuoteSpecification.byUpdatedById(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode updated by ID: {}", updatedById, e);
                }
            }

            // Apply status group filter
            if (statusGroup != null && !statusGroup.isEmpty()) {
                switch (statusGroup.toLowerCase()) {
                    case "draft":
                        spec = spec.and(QuoteSpecification.byDraftStatus());
                        break;
                    case "pending":
                        spec = spec.and(QuoteSpecification.byPendingStatuses());
                        break;
                    case "active":
                        spec = spec.and(QuoteSpecification.byActiveStatuses());
                        break;
                    case "closed":
                        spec = spec.and(QuoteSpecification.byClosedStatuses());
                        break;
                    default:
                        log.warn("Unknown status group: {}", statusGroup);
                }
            }

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
            response.put("currentSortDir", direction.name().toLowerCase());

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
            .version(quote.getVersion())
            .status(quote.getStatus())
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
}
