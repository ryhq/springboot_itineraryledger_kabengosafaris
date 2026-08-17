package com.itineraryledger.kabengosafaris.Faq.Services;

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
import com.itineraryledger.kabengosafaris.Faq.DTOs.CreateFaqDTO;
import com.itineraryledger.kabengosafaris.Faq.DTOs.FaqDTO;
import com.itineraryledger.kabengosafaris.Faq.DTOs.ReorderFaqsDTO;
import com.itineraryledger.kabengosafaris.Faq.DTOs.UpdateFaqDTO;
import com.itineraryledger.kabengosafaris.Faq.Entity.Faq;
import com.itineraryledger.kabengosafaris.Faq.Repository.FaqRepository;
import com.itineraryledger.kabengosafaris.Faq.Specifications.FaqFilter;
import com.itineraryledger.kabengosafaris.Faq.Specifications.FaqSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The site's global FAQ list — read, written and ordered.
 *
 * One service rather than four: an FAQ is a question, an answer and a position, and splitting
 * that across create/update/delete/get classes would be four files of ceremony for one table.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "question", "category", "displayOrder", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    private final FaqRepository faqRepository;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    /** The ONE description of the filtered set: rows, counters and record arrows share it. */
    private Specification<Faq> buildSpec(FaqFilter filter) {
        Specification<Faq> spec = Specification.unrestricted();
        if (filter == null) return spec;
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(FaqSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            spec = spec.and(FaqSpecification.byCategory(filter.getCategory()));
        }
        if (filter.getIsActive() != null) {
            spec = spec.and(FaqSpecification.isActive(filter.getIsActive()));
        }
        /* active + inactive together is every row, so the pair cancels */
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            boolean active = filter.getStatuses().contains("active");
            boolean inactive = filter.getStatuses().contains("inactive");
            if (active != inactive) spec = spec.and(FaqSpecification.isActive(active));
        }
        return spec;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAllFaqs(
        FaqFilter filter,
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
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            /* ascending by default: the order IS the editorial running order */
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            Specification<Faq> spec = buildSpec(filter);
            Page<Faq> faqPage = faqRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("faqs", faqPage.getContent().stream().map(this::toDTO).collect(Collectors.toList()));
            response.put("currentPage", faqPage.getNumber());
            response.put("totalItems", faqPage.getTotalElements());
            response.put("totalPages", faqPage.getTotalPages());
            response.put("pageSize", faqPage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", listStats.of(Faq.class, spec)
                    .total()
                    .count("active", FaqSpecification.isActive(true))
                    .complement("inactive", "active")
                    .count("missingCategory", FaqSpecification.missingCategory())
                    .build());
            }

            return ResponseEntity.ok(ApiResponse.success(200, "FAQs retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing FAQs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list FAQs", "FAQS_LIST_FAILED")
            );
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getFaqById(String obfuscatedId, FaqFilter filter, String sortBy, String sortDirection) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid FAQ ID", "INVALID_FAQ_ID"));
            }

            Faq faq = faqRepository.findById(id).orElse(null);
            if (faq == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "FAQ not found", "FAQ_NOT_FOUND"));
            }

            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                Faq.class,
                buildSpec(filter),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("faq", toDTO(faq));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "FAQ retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching FAQ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch FAQ", "FAQ_FETCH_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "CREATE_FAQ", description = "Creating an FAQ", entityType = "Faq")
    public ResponseEntity<ApiResponse<?>> createFaq(CreateFaqDTO createDTO) {
        try {
            Faq faq = Faq.builder()
                .question(createDTO.getQuestion())
                .answer(createDTO.getAnswer())
                .category(createDTO.getCategory())
                .displayOrder(createDTO.getDisplayOrder() != null
                    ? createDTO.getDisplayOrder()
                    : safeOrder(faqRepository.findMaxDisplayOrder()) + 1)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .createdBy(currentUser())
                .updatedBy(currentUser())
                .build();

            faq = faqRepository.save(faq);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "FAQ created successfully", toDTO(faq))
            );
        } catch (Exception e) {
            log.error("Error creating FAQ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create FAQ", "FAQ_CREATE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "UPDATE_FAQ", description = "Updating an FAQ", entityType = "Faq")
    public ResponseEntity<ApiResponse<?>> updateFaq(String obfuscatedId, UpdateFaqDTO updateDTO) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid FAQ ID", "INVALID_FAQ_ID"));
            }

            Faq faq = faqRepository.findById(id).orElse(null);
            if (faq == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "FAQ not found", "FAQ_NOT_FOUND"));
            }

            /* null means "leave it alone" */
            if (updateDTO.getQuestion() != null) faq.setQuestion(updateDTO.getQuestion());
            if (updateDTO.getAnswer() != null) faq.setAnswer(updateDTO.getAnswer());
            if (updateDTO.getCategory() != null) faq.setCategory(updateDTO.getCategory());
            if (updateDTO.getDisplayOrder() != null) faq.setDisplayOrder(updateDTO.getDisplayOrder());
            if (updateDTO.getIsActive() != null) faq.setIsActive(updateDTO.getIsActive());
            faq.setUpdatedBy(currentUser());

            faq = faqRepository.save(faq);
            return ResponseEntity.ok(ApiResponse.success(200, "FAQ updated successfully", toDTO(faq)));
        } catch (Exception e) {
            log.error("Error updating FAQ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update FAQ", "FAQ_UPDATE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_FAQS", description = "Deleting FAQs", entityType = "Faq")
    public ResponseEntity<ApiResponse<?>> deleteFaqs(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "No FAQ IDs provided", "NO_IDS_PROVIDED"));
        }

        int deletedCount = 0;
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscatedId : obfuscatedIds) {
            try {
                Long id = idObfuscator.decodeId(obfuscatedId);
                Faq faq = faqRepository.findById(id).orElse(null);
                if (faq == null) {
                    skipped.add(skip(obfuscatedId, "No such FAQ — it may already have been deleted"));
                    continue;
                }
                faqRepository.delete(faq);
                deletedCount++;
                deletedIds.add(obfuscatedId);
            } catch (Exception e) {
                skipped.add(skip(obfuscatedId, e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok(ApiResponse.success(200,
            deletedCount == 0 ? "No FAQs were deleted" : deletedCount + " FAQ(s) deleted successfully",
            report));
    }

    /**
     * The whole running order at once.
     *
     * Position in the list IS the display order — sending one item's new number would leave
     * the rest of the list disagreeing with what the person just dragged.
     */
    @AuditLogAnnotation(action = "REORDER_FAQS", description = "Reordering FAQs", entityType = "Faq")
    public ResponseEntity<ApiResponse<?>> reorder(ReorderFaqsDTO dto) {
        try {
            List<String> skipped = new ArrayList<>();
            int position = 1;
            for (String obfuscatedId : dto.getFaqOrder()) {
                try {
                    Long id = idObfuscator.decodeId(obfuscatedId);
                    Faq faq = faqRepository.findById(id).orElse(null);
                    if (faq == null) {
                        skipped.add(obfuscatedId + ": no such FAQ");
                        continue;
                    }
                    faq.setDisplayOrder(position++);
                    faqRepository.save(faq);
                } catch (Exception e) {
                    skipped.add(obfuscatedId + ": unreadable id");
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("reorderedCount", position - 1);
            report.put("skipped", skipped);
            return ResponseEntity.ok(ApiResponse.success(200, "FAQs reordered successfully", report));
        } catch (Exception e) {
            log.error("Error reordering FAQs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder FAQs", "FAQ_REORDER_FAILED")
            );
        }
    }

    public FaqDTO toDTO(Faq faq) {
        if (faq == null) return null;
        return FaqDTO.builder()
            .id(idObfuscator.encodeId(faq.getId()))
            .question(faq.getQuestion())
            .answer(faq.getAnswer())
            .category(faq.getCategory())
            .displayOrder(faq.getDisplayOrder())
            .isActive(faq.getIsActive())
            .createdByName(faq.getCreatedBy() != null ? faq.getCreatedBy().getUsername() : null)
            .updatedByName(faq.getUpdatedBy() != null ? faq.getUpdatedBy().getUsername() : null)
            .createdAt(faq.getCreatedAt())
            .updatedAt(faq.getUpdatedAt())
            .build();
    }

    private Map<String, Object> skip(String id, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
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
