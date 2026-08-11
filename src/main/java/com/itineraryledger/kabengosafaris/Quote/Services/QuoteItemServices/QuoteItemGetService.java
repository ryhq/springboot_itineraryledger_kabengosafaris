package com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices;

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

import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.QuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Specifications.QuoteItemSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving quote items with filtering, pagination, and sorting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteItemGetService {

    private final QuoteItemRepository quoteItemRepository;
    private final IdObfuscator idObfuscator;

    /*
     * displayOrder belongs here: the lines of a quote are a document, printed in
     * the order the reorder endpoint maintains, so it is the one order a caller
     * most needs — and asking for it was answered with 400.
     */
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "displayOrder", "itemType", "itemName", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    /**
     * Get a single quote item by obfuscated ID
     *
     * @param idObfuscated The obfuscated quote item ID
     * @return ResponseEntity with ApiResponse containing the quote item
     */
    public ResponseEntity<ApiResponse<?>> getQuoteItemById(String idObfuscated) {
        log.info("Fetching quote item with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote item ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote item ID", "INVALID_QUOTE_ITEM_ID")
                );
            }

            // Find quote item
            QuoteItem quoteItem = quoteItemRepository.findById(id).orElse(null);
            if (quoteItem == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote item not found", "QUOTE_ITEM_NOT_FOUND")
                );
            }

            // Convert to DTO
            QuoteItemDTO quoteItemDTO = convertToDTO(quoteItem);

            // Circular navigation, scoped to the parent quote so we don't leak
            // into items from a different quote when reaching the boundary.
            Long parentQuoteId = quoteItem.getQuote() != null ? quoteItem.getQuote().getId() : null;
            Long nextId = null;
            Long previousId = null;
            if (parentQuoteId != null) {
                nextId = quoteItemRepository.findNextIdInQuote(parentQuoteId, id).orElse(null);
                previousId = quoteItemRepository.findPreviousIdInQuote(parentQuoteId, id).orElse(null);
                if (nextId == null) nextId = quoteItemRepository.findFirstIdInQuote(parentQuoteId).orElse(null);
                if (previousId == null) previousId = quoteItemRepository.findLastIdInQuote(parentQuoteId).orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("quoteItem", quoteItemDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote item retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching quote item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote item", "QUOTE_ITEM_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all quote items for a specific quote with pagination, sorting, and filtering
     * Sorting is always by displayOrder, only direction can be specified
     *
     * @param quoteId The obfuscated quote ID (required)
     * @param itemType Filter by item type
     * @param itemName Filter by item name (partial match)
     * @param description Filter by description (partial match)
     * @param isActive Filter by active status
     * @param itemTypeGroup Filter by item type group (accommodation, parkfee, activity, transport, guide, meal)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc) - always sorts by displayOrder
     * @return ResponseEntity with ApiResponse containing paginated quote items
     */
    public ResponseEntity<ApiResponse<?>> getQuoteItemsByQuoteId(
        String quoteId,
        QuoteItemType itemType,
        String itemName,
        String description,
        Boolean isActive,
        String itemTypeGroup,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching quote items for quote ID: {}", quoteId);

        try {
            // Decode quote ID
            Long decodedQuoteId;
            try {
                decodedQuoteId = idObfuscator.decodeId(quoteId);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", quoteId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            // Build specification for filtering
            Specification<QuoteItem> spec = QuoteItemSpecification.byQuoteId(decodedQuoteId);

            // Apply additional filters
            if (itemType != null) {
                spec = spec.and(QuoteItemSpecification.byItemType(itemType));
            }
            if (itemName != null && !itemName.isEmpty()) {
                spec = spec.and(QuoteItemSpecification.byItemNameContaining(itemName));
            }
            if (description != null && !description.isEmpty()) {
                spec = spec.and(QuoteItemSpecification.byDescriptionContaining(description));
            }
            if (isActive != null) {
                spec = spec.and(QuoteItemSpecification.byIsActive(isActive));
            }
            if (itemTypeGroup != null && !itemTypeGroup.isEmpty()) {
                spec = spec.and(QuoteItemSpecification.byItemTypeGroup(itemTypeGroup));
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch quote items
            Page<QuoteItem> quoteItemPage = quoteItemRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<QuoteItemDTO> quoteItemDTOs = quoteItemPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("quoteItems", quoteItemDTOs);
            response.put("currentPage", quoteItemPage.getNumber());
            response.put("totalItems", quoteItemPage.getTotalElements());
            response.put("totalPages", quoteItemPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote items retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching quote items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote items", "QUOTE_ITEMS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all quote items with pagination, sorting, and filtering
     *
     * @param quoteId Filter by quote ID (obfuscated)
     * @param itemType Filter by item type
     * @param itemName Filter by item name (partial match)
     * @param description Filter by description (partial match)
     * @param isActive Filter by active status
     * @param itemTypeGroup Filter by item type group (accommodation, parkfee, activity, transport, guide, meal)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated quote items
     */
    public ResponseEntity<ApiResponse<?>> getAllQuoteItems(
        String quoteId,
        QuoteItemType itemType,
        String itemName,
        String description,
        Boolean isActive,
        String itemTypeGroup,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all quote items with filters");

        try {
            // Build specification for filtering
            Specification<QuoteItem> spec = Specification.unrestricted();

            // Apply quote ID filter
            if (quoteId != null && !quoteId.isEmpty()) {
                try {
                    Long decodedQuoteId = idObfuscator.decodeId(quoteId);
                    spec = spec.and(QuoteItemSpecification.byQuoteId(decodedQuoteId));
                } catch (Exception e) {
                    log.warn("Failed to decode quote ID: {}", quoteId, e);
                }
            }

            // Apply other filters
            if (itemType != null) {
                spec = spec.and(QuoteItemSpecification.byItemType(itemType));
            }
            if (itemName != null && !itemName.isEmpty()) {
                spec = spec.and(QuoteItemSpecification.byItemName(itemName));
            }
            if (description != null && !description.isEmpty()) {
                spec = spec.and(QuoteItemSpecification.byDescription(description));
            }
            if (isActive != null) {
                spec = spec.and(QuoteItemSpecification.byIsActive(isActive));
            }

            // Apply item type group filter
            if (itemTypeGroup != null && !itemTypeGroup.isEmpty()) {
                switch (itemTypeGroup.toLowerCase()) {
                    case "accommodation":
                        spec = spec.and(QuoteItemSpecification.byAccommodationItems());
                        break;
                    case "parkfee":
                        spec = spec.and(QuoteItemSpecification.byParkFeeItems());
                        break;
                    case "activity":
                        spec = spec.and(QuoteItemSpecification.byActivityItems());
                        break;
                    case "transport":
                        spec = spec.and(QuoteItemSpecification.byTransportItems());
                        break;
                    case "guide":
                        spec = spec.and(QuoteItemSpecification.byGuideItems());
                        break;
                    case "meal":
                        spec = spec.and(QuoteItemSpecification.byMealItems());
                        break;
                    default:
                        log.warn("Unknown item type group: {}", itemTypeGroup);
                }
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch quote items
            Page<QuoteItem> quoteItemPage = quoteItemRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<QuoteItemDTO> quoteItemDTOs = quoteItemPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("quoteItems", quoteItemDTOs);
            response.put("currentPage", quoteItemPage.getNumber());
            response.put("totalItems", quoteItemPage.getTotalElements());
            response.put("totalPages", quoteItemPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote items retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching quote items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote items", "QUOTE_ITEMS_FETCH_FAILED")
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert QuoteItem entity to QuoteItemDTO
     */
    private QuoteItemDTO convertToDTO(QuoteItem quoteItem) {
        return QuoteItemDTO.builder()
            .id(idObfuscator.encodeId(quoteItem.getId()))
            .quoteId(idObfuscator.encodeId(quoteItem.getQuote().getId()))
            .quoteCode(quoteItem.getQuote().getQuoteCode())
            .itemType(quoteItem.getItemType())
            .itemName(quoteItem.getItemName())
            .description(quoteItem.getDescription())
            .displayOrder(quoteItem.getDisplayOrder())
            .prices(quoteItem.getPrices())
            .isActive(quoteItem.getIsActive())
            .createdAt(quoteItem.getCreatedAt())
            .updatedAt(quoteItem.getUpdatedAt())
            .build();
    }
}
