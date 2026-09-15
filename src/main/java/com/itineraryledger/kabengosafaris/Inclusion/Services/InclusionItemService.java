package com.itineraryledger.kabengosafaris.Inclusion.Services;

import java.util.ArrayList;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.CreateInclusionItemDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionItemDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.ReorderInclusionItemsDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.UpdateInclusionItemDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;
import com.itineraryledger.kabengosafaris.Inclusion.Specifications.InclusionItemFilter;
import com.itineraryledger.kabengosafaris.Inclusion.Specifications.InclusionItemSpecification;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Repository.ItineraryInclusionRepository;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The catalogue of lines a trip's promise is assembled from — read, written and ordered.
 *
 * <p>One service rather than four, as {@code FaqService} is: an item is a sentence, a side and a
 * position, and splitting that across create/update/delete/get classes would be four files of
 * ceremony for one table.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InclusionItemService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "label", "category", "displayOrder", "isActive", "isStandard", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    private final InclusionItemRepository repository;
    private final ItineraryInclusionRepository itineraryInclusions;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    /** The ONE description of the filtered set: rows, counters and record arrows share it. */
    private Specification<InclusionItem> buildSpec(InclusionItemFilter filter) {
        Specification<InclusionItem> spec = Specification.unrestricted();
        if (filter == null) return spec;

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(InclusionItemSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            spec = spec.and(InclusionItemSpecification.byCategory(filter.getCategory()));
        }
        if (filter.getCategories() != null && !filter.getCategories().isEmpty()) {
            spec = spec.and(InclusionItemSpecification.byCategories(filter.getCategories()));
        }
        if (filter.getIsActive() != null) {
            spec = spec.and(InclusionItemSpecification.isActive(filter.getIsActive()));
        }
        /* enabled + disabled together is every row, so the pair cancels */
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            boolean enabled = filter.getStatuses().contains("enabled") || filter.getStatuses().contains("active");
            boolean disabled = filter.getStatuses().contains("disabled") || filter.getStatuses().contains("inactive");
            if (enabled != disabled) spec = spec.and(InclusionItemSpecification.isActive(enabled));
        }

        /* OR inside a dimension, AND across dimensions */
        Specification<InclusionItem> defaults = anyOf(filter.getDefaults(), want -> switch (want) {
            case "standard" -> InclusionItemSpecification.isStandard(true);
            case "optional" -> InclusionItemSpecification.isStandard(false);
            case "included-by-default" -> InclusionItemSpecification.defaultIncluded(true);
            case "excluded-by-default" -> InclusionItemSpecification.defaultIncluded(false);
            default -> null;
        });
        if (defaults != null) spec = spec.and(defaults);

        Specification<InclusionItem> claims = anyOf(filter.getClaims(), want -> switch (want) {
            case "claims" -> InclusionItemSpecification.makesAClaim();
            case "no-claim" -> InclusionItemSpecification.makesNoClaim();
            default -> null;
        });
        if (claims != null) spec = spec.and(claims);

        Specification<InclusionItem> usage = anyOf(filter.getUsage(), want -> switch (want) {
            case "in-use" -> InclusionItemSpecification.inUse(true);
            case "unused" -> InclusionItemSpecification.inUse(false);
            default -> null;
        });
        if (usage != null) spec = spec.and(usage);

        return spec;
    }

    /** Ors the recognised values of one dimension together; null when none applied. */
    private Specification<InclusionItem> anyOf(
            List<String> wanted,
            java.util.function.Function<String, Specification<InclusionItem>> of) {
        if (wanted == null || wanted.isEmpty()) return null;
        Specification<InclusionItem> combined = null;
        for (String want : wanted) {
            Specification<InclusionItem> one = of.apply(want);
            if (one == null) continue;
            combined = combined == null ? one : combined.or(one);
        }
        return combined;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(
        InclusionItemFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            int pageNumber = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? Math.min(size, 100) : 50;

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS,
                    "INVALID_SORT_FIELD"));
            }

            /* ascending by default: the order IS the print order */
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            Specification<InclusionItem> spec = buildSpec(filter);
            Page<InclusionItem> itemPage = repository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("inclusionItems", itemPage.getContent().stream()
                .map(this::toDTO).collect(Collectors.toList()));
            response.put("currentPage", itemPage.getNumber());
            response.put("totalItems", itemPage.getTotalElements());
            response.put("totalPages", itemPage.getTotalPages());
            response.put("pageSize", itemPage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            if (!Boolean.FALSE.equals(includeStats)) {
                /* Counters over the SAME specification as the rows, so a card cannot contradict the table. */
                response.put("stats", listStats.of(InclusionItem.class, spec)
                    .total()
                    .count("enabled", InclusionItemSpecification.isActive(true))
                    .complement("disabled", "enabled")
                    .count("standard", InclusionItemSpecification.isStandard(true))
                    .count("includedByDefault", InclusionItemSpecification.defaultIncluded(true))
                    .count("claims", InclusionItemSpecification.makesAClaim())
                    .count("inUse", InclusionItemSpecification.inUse(true))
                    .count("unused", InclusionItemSpecification.inUse(false))
                    /* One counter per heading somebody actually typed, not per enum constant. */
                    .breakdown("byCategory", distinctCategories().toArray(new String[0]),
                        InclusionItemSpecification::byCategory)
                    .recency(InclusionItemSpecification::createdAfter)
                    .build());
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Inclusion items retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing inclusion items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list inclusion items", "INCLUSION_ITEMS_LIST_FAILED"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getById(
            String obfuscatedId, InclusionItemFilter filter, String sortBy, String sortDirection) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid inclusion item ID", "INVALID_INCLUSION_ITEM_ID"));
            }

            InclusionItem item = repository.findById(id).orElse(null);
            if (item == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Inclusion item not found", "INCLUSION_ITEM_NOT_FOUND"));
            }

            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                InclusionItem.class,
                buildSpec(filter),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("inclusionItem", toDTO(item));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Inclusion item retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching inclusion item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch inclusion item", "INCLUSION_ITEM_FETCH_FAILED"));
        }
    }

    @AuditLogAnnotation(action = "CREATE_INCLUSION_ITEM",
        description = "Creating an inclusion item", entityType = "InclusionItem")
    public ResponseEntity<ApiResponse<?>> create(CreateInclusionItemDTO dto) {
        try {
            String label = dto.getLabel() == null ? null : dto.getLabel().trim();
            if (label == null || label.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Label is required", "LABEL_REQUIRED"));
            }
            /*
             * The same sentence twice in the catalogue is the duplication this replaced, arriving
             * by a different door. Refuse it and name the row that already says it.
             */
            var existing = repository.findByLabelIgnoringCaseAndSpace(label);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "That line already exists as " + existing.get().getCode(),
                    "INCLUSION_ITEM_DUPLICATE"));
            }

            InclusionItem item = InclusionItem.builder()
                .label(label)
                .category(trimToNull(dto.getCategory()))
                /* a new line goes to the end; where it belongs is decided by dragging */
                .displayOrder(safeOrder(repository.findMaxDisplayOrder()) + 1)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isSystem(false)
                .isStandard(dto.getIsStandard() != null ? dto.getIsStandard() : false)
                .defaultIncluded(dto.getDefaultIncluded() != null ? dto.getDefaultIncluded() : true)
                .claimAppliesTo(LineCategoryScope.canonicalOf(dto.getClaimAppliesTo(), QuoteItemType.class))
                .internalNotes(trimToNull(dto.getInternalNotes()))
                .createdBy(currentUser())
                .updatedBy(currentUser())
                .build();

            item = repository.saveAndFlush(item);
            item.setCode(item.generateCode());
            item = repository.save(item);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Inclusion item created successfully", toDTO(item)));
        } catch (Exception e) {
            log.error("Error creating inclusion item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create inclusion item", "INCLUSION_ITEM_CREATE_FAILED"));
        }
    }

    @AuditLogAnnotation(action = "UPDATE_INCLUSION_ITEM",
        description = "Updating an inclusion item", entityType = "InclusionItem")
    public ResponseEntity<ApiResponse<?>> update(String obfuscatedId, UpdateInclusionItemDTO dto) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid inclusion item ID", "INVALID_INCLUSION_ITEM_ID"));
            }

            InclusionItem item = repository.findById(id).orElse(null);
            if (item == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Inclusion item not found", "INCLUSION_ITEM_NOT_FOUND"));
            }

            /* null means "leave it alone" */
            if (dto.getLabel() != null && !dto.getLabel().isBlank()) item.setLabel(dto.getLabel().trim());
            if (dto.getCategory() != null) item.setCategory(trimToNull(dto.getCategory()));
            if (dto.getIsActive() != null) item.setIsActive(dto.getIsActive());
            if (dto.getIsStandard() != null) item.setIsStandard(dto.getIsStandard());
            if (dto.getDefaultIncluded() != null) item.setDefaultIncluded(dto.getDefaultIncluded());
            if (dto.getClaimAppliesTo() != null) {
                item.setClaimAppliesTo(
                    LineCategoryScope.canonicalOf(dto.getClaimAppliesTo(), QuoteItemType.class));
            }
            if (dto.getInternalNotes() != null) item.setInternalNotes(trimToNull(dto.getInternalNotes()));
            item.setUpdatedBy(currentUser());

            item = repository.save(item);
            return ResponseEntity.ok(
                ApiResponse.success(200, "Inclusion item updated successfully", toDTO(item)));
        } catch (Exception e) {
            log.error("Error updating inclusion item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update inclusion item", "INCLUSION_ITEM_UPDATE_FAILED"));
        }
    }

    /**
     * Delete, reference-checked.
     *
     * <p>An item any itinerary still lists is refused and says which itineraries — disabling is the
     * right move there, and it is reversible. A seeded item is refused outright. Note what does NOT
     * block: a quote, safari or invoice that printed this line holds the wording as its own text, so
     * tidying the catalogue can never empty a document already with a customer.
     */
    @AuditLogAnnotation(action = "DELETE_INCLUSION_ITEMS",
        description = "Deleting inclusion items", entityType = "InclusionItem")
    public ResponseEntity<ApiResponse<?>> delete(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No inclusion item IDs provided", "NO_IDS_PROVIDED"));
        }

        int deletedCount = 0;
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscatedId : obfuscatedIds) {
            try {
                Long id = idObfuscator.decodeId(obfuscatedId);
                InclusionItem item = repository.findById(id).orElse(null);
                if (item == null) {
                    skipped.add(skip(obfuscatedId, null,
                        "No such inclusion item — it may already have been deleted"));
                    continue;
                }
                if (item.isSystemItem()) {
                    skipped.add(skip(obfuscatedId, item.getCode(),
                        "One of the standard lines. Disable it instead — deleting it would take it "
                        + "off every itinerary that lists it."));
                    continue;
                }
                long uses = itineraryInclusions.countByInclusionItemId(id);
                if (uses > 0) {
                    List<String> codes = itineraryInclusions.findItineraryCodesUsing(id);
                    skipped.add(skip(obfuscatedId, item.getCode(),
                        "Listed on " + uses + " itinerar" + (uses == 1 ? "y" : "ies")
                        + (codes.isEmpty() ? "" : " (" + String.join(", ", codes.subList(0, Math.min(5, codes.size()))) + ")")
                        + ". Disable it instead."));
                    continue;
                }
                repository.delete(item);
                deletedCount++;
                deletedIds.add(obfuscatedId);
            } catch (Exception e) {
                skipped.add(skip(obfuscatedId, null,
                    e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok(ApiResponse.success(200,
            deletedCount == 0 ? "No inclusion items were deleted"
                              : deletedCount + " inclusion item(s) deleted successfully",
            report));
    }

    /**
     * The whole running order at once.
     *
     * <p>Position in the list IS the display order — sending one item's new number would leave the
     * rest of the list disagreeing with what the person just dragged.
     */
    @AuditLogAnnotation(action = "REORDER_INCLUSION_ITEMS",
        description = "Reordering inclusion items", entityType = "InclusionItem")
    public ResponseEntity<ApiResponse<?>> reorder(ReorderInclusionItemsDTO dto) {
        try {
            List<String> skipped = new ArrayList<>();
            int position = 1;
            for (String obfuscatedId : dto.getItemOrder()) {
                try {
                    Long id = idObfuscator.decodeId(obfuscatedId);
                    InclusionItem item = repository.findById(id).orElse(null);
                    if (item == null) {
                        skipped.add(obfuscatedId + ": no such inclusion item");
                        continue;
                    }
                    item.setDisplayOrder(position++);
                    repository.save(item);
                } catch (Exception e) {
                    skipped.add(obfuscatedId + ": unreadable id");
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("reorderedCount", position - 1);
            report.put("skipped", skipped);
            return ResponseEntity.ok(
                ApiResponse.success(200, "Inclusion items reordered successfully", report));
        } catch (Exception e) {
            log.error("Error reordering inclusion items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder inclusion items", "INCLUSION_ITEM_REORDER_FAILED"));
        }
    }

    public InclusionItemDTO toDTO(InclusionItem item) {
        if (item == null) return null;
        return InclusionItemDTO.builder()
            .id(idObfuscator.encodeId(item.getId()))
            .code(item.getCode())
            .label(item.getLabel())
            .category(item.getCategory())
            .displayOrder(item.getDisplayOrder())
            .isActive(item.getIsActive())
            .isSystem(item.getIsSystem())
            .isStandard(item.getIsStandard())
            .defaultIncluded(item.getDefaultIncluded())
            .claimAppliesTo(item.getClaimAppliesTo())
            /*
             * Only described when there IS a claim. describe(null, …) says "every line", which is
             * true for a tax scope and the opposite of the truth here.
             */
            .claimAppliesToLabel(item.makesACheckableClaim()
                ? LineCategoryScope.describe(item.getClaimAppliesTo(), QuoteItemType.class)
                : null)
            .internalNotes(item.getInternalNotes())
            .usageCount(item.getId() == null ? 0L : itineraryInclusions.countByInclusionItemId(item.getId()))
            .createdByName(item.getCreatedBy() != null ? item.getCreatedBy().getUsername() : null)
            .updatedByName(item.getUpdatedBy() != null ? item.getUpdatedBy().getUsername() : null)
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }

    /** The categories actually in use, so the breakdown names headings somebody typed. */
    @Transactional(readOnly = true)
    public List<String> distinctCategories() {
        return repository.findAll().stream()
            .map(InclusionItem::getCategory)
            .filter(c -> c != null && !c.isBlank())
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
    }

    private Map<String, Object> skip(String id, String code, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("code", code);
        entry.put("reason", reason);
        return entry;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safeOrder(Integer value) {
        return value != null ? value : 0;
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    private User currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal instanceof User user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }
}
