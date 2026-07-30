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
            String sortBy,
            String sortDirection,
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

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId, String scopeParentId) {
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

            // Circular navigation (scoped if parent provided, global otherwise)
            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = activityDocumentRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = activityDocumentRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = activityDocumentRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = activityDocumentRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = activityDocumentRepository.findNextId(id).orElse(null);
                previousId = activityDocumentRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = activityDocumentRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = activityDocumentRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
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
}
