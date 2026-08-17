package com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices;

import java.time.LocalDateTime;
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

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.ActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving activity documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityDocumentGetService {

    private final ActivityDocumentRepository activityDocumentRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final ActivityDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "title", "documentType", "fileName", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            String activityId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String version,
            Boolean currentlyValid,
            String activityName,
            Boolean activityIsActive,
            Boolean hasTariff,
            Boolean safetyDocumentsOnly,
            java.util.List<ActivityDocument.DocumentType> documentTypes,
            java.util.List<String> statuses,
            java.util.List<String> validity,
            java.time.LocalDateTime createdAfter,
            java.time.LocalDateTime createdBefore,
            String sortBy,
            String sortDirection,
            String keyword,
            int page,
            int size
    ) {
        log.info("Fetching activity documents with filters");

        try {
            Long decodedActivityId = null;
            if (activityId != null && !activityId.isBlank()) {
                try {
                    decodedActivityId = idObfuscator.decodeId(activityId);
                } catch (Exception e) {
                    log.warn("Failed to decode activity ID: {}", activityId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
                    );
                }
            }

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

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            Specification<ActivityDocument> spec = buildSpec(activityId, documentType, isActive, title, version, currentlyValid, activityName, activityIsActive, hasTariff, safetyDocumentsOnly, documentTypes, statuses, validity, createdAfter, createdBefore, keyword);


            Page<ActivityDocument> documentPage = activityDocumentRepository.findAll(spec, pageable);
            Page<ActivityDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("documents", dtoPage.getContent());
            response.put("currentPage", dtoPage.getNumber());
            response.put("totalPages", dtoPage.getTotalPages());
            response.put("totalElements", dtoPage.getTotalElements());
            response.put("pageSize", dtoPage.getSize());
            response.put("hasNext", dtoPage.hasNext());
            response.put("hasPrevious", dtoPage.hasPrevious());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        // counters share the SAME spec as the rows, so cards and table always agree
        response.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activity documents retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching activity documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch activity documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(
        String obfuscatedId,
        String scopeParentId,
        String activityId,
        DocumentType documentType,
        Boolean isActive,
        String title,
        String version,
        Boolean currentlyValid,
        String activityName,
        Boolean activityIsActive,
        Boolean hasTariff,
        Boolean safetyDocumentsOnly,
        java.util.List<ActivityDocument.DocumentType> documentTypes,
        java.util.List<String> statuses,
        java.util.List<String> validity,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching activity document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode activity document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ActivityDocument document = activityDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Activity document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            ActivityDocumentDTO documentDTO = toDTO(document);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /*
             * Circular navigation over the caller's filtered, sorted set — scoped to the
             * parent when one is given. The id-ordered walk this replaces stepped through a
             * different set from the one on screen and could not say where you were in it.
             */
            String validatedSortBy = validateSortField(sortBy);
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                ActivityDocument.class,
                buildSpec(decodedParentId != null ? scopeParentId : activityId, documentType, isActive, title, version, currentlyValid, activityName, activityIsActive, hasTariff, safetyDocumentsOnly, documentTypes, statuses, validity, createdAfter, createdBefore, keyword),
                validatedSortBy != null ? validatedSortBy : "createdAt",
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activity document retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching activity document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch activity document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByActivityId(String activityId) {
        log.info("Fetching documents for activity: {}", activityId);

        try {
            Long decodedActivityId;
            try {
                decodedActivityId = idObfuscator.decodeId(activityId);
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", activityId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
                );
            }

            List<ActivityDocument> documents = activityDocumentRepository.findByActivityIdOrderByCreatedAtDesc(decodedActivityId);
            List<ActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activity documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching activity documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch activity documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ActivityDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return activityDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get activity document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public ActivityDocument getDocumentEntityByFileName(String fileName) {
        return activityDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public ActivityDocumentDTO toDTO(ActivityDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        String activityObfuscatedId = document.getActivity() != null
            ? idObfuscator.encodeId(document.getActivity().getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return ActivityDocumentDTO.builder()
            .id(obfuscatedId)
            .activityId(activityObfuscatedId)
            .activityName(document.getActivity() != null ? document.getActivity().getName() : null)
            .title(document.getTitle())
            .documentType(document.getDocumentType())
            .documentTypeDisplayName(document.getDocumentType() != null ? document.getDocumentType().getDisplayName() : null)
            .documentTypeDescription(document.getDocumentType() != null ? document.getDocumentType().getDescription() : null)
            .documentUrl(storageService.constructDocumentUrl(obfuscatedId))
            .fileDocumentUrl(storageService.constructFileDocumentUrl(document.getFileName()))
            .fileName(document.getFileName())
            .originalFileName(document.getOriginalFileName())
            .fileSize(document.getFileSize())
            .fileSizeFormatted(document.getFileSize() != null ? storageService.formatFileSize(document.getFileSize()) : null)
            .fileType(storageService.getExtension(document.getFileName()))
            .description(document.getDescription())
            .version(document.getVersion())
            .notes(document.getNotes())
            .validFrom(document.getValidFrom())
            .validTo(document.getValidTo())
            .isCurrentlyValid(isCurrentlyValid)
            .isActive(document.getIsActive())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(Specification<ActivityDocument> base) {
        return listStats.of(ActivityDocument.class, base)
            .total()
            .count("active", ActivityDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("expired", ActivityDocumentSpecification.expired())
            .count("expiringSoon", ActivityDocumentSpecification.expiringWithin(30))
            .count("noExpiry", ActivityDocumentSpecification.noExpiry())
            .recency(ActivityDocumentSpecification::createdAfter)
            .build();
    }

    /**
     * The ONE description of the filtered set, shared by the rows and by the record
     * arrows — paging that walked a different set from the one on screen would be
     * worse than no arrows (see CLAUDE.md).
     */
    private Specification<ActivityDocument> buildSpec(
        String activityId,
        DocumentType documentType,
        Boolean isActive,
        String title,
        String version,
        Boolean currentlyValid,
        String activityName,
        Boolean activityIsActive,
        Boolean hasTariff,
        Boolean safetyDocumentsOnly,
        java.util.List<ActivityDocument.DocumentType> documentTypes,
        java.util.List<String> statuses,
        java.util.List<String> validity,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore,
        String keyword
    ) {
        /* the parent arrives obfuscated; an unreadable one simply means "not scoped" */
        Long decodedActivityId = null;
        if (activityId != null && !activityId.isBlank()) {
            try {
                decodedActivityId = idObfuscator.decodeId(activityId);
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", activityId);
            }
        }

        Specification<ActivityDocument> spec = ActivityDocumentSpecification.byActivityId(decodedActivityId)
                .and(ActivityDocumentSpecification.byDocumentType(documentType))
                .and(ActivityDocumentSpecification.byIsActive(isActive))
                .and(ActivityDocumentSpecification.byTitleContains(title))
                .and(ActivityDocumentSpecification.byVersion(version))
                .and(ActivityDocumentSpecification.byActivityName(activityName))
                .and(ActivityDocumentSpecification.byActivityIsActive(activityIsActive))
                .and(ActivityDocumentSpecification.byActivityHasTariff(hasTariff));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(ActivityDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(safetyDocumentsOnly)) {
                spec = spec.and(ActivityDocumentSpecification.bySafetyDocument());
            }
            // multi-value + validity facets: every stat card must be filterable
            if (documentTypes != null && !documentTypes.isEmpty()) {
                spec = spec.and(ActivityDocumentSpecification.documentTypeIn(documentTypes));
            }
            if (statuses != null && !statuses.isEmpty()) {
                java.util.List<Boolean> states = new java.util.ArrayList<>();
                if (statuses.contains("active")) states.add(true);
                if (statuses.contains("inactive")) states.add(false);
                if (states.size() == 1) spec = spec.and(ActivityDocumentSpecification.byIsActive(states.get(0)));
            }
            if (validity != null && !validity.isEmpty()) {
                spec = spec.and(ActivityDocumentSpecification.anyValidityState(
                    validity.contains("expired"),
                    validity.contains("expiring"),
                    validity.contains("no-expiry")
                ));
            }
            if (createdAfter != null) spec = spec.and(ActivityDocumentSpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(ActivityDocumentSpecification.createdBefore(createdBefore));

            if (keyword != null && !keyword.isBlank()) spec = spec.and(ActivityDocumentSpecification.searchKeyword(keyword));

        return spec;
    }
}
