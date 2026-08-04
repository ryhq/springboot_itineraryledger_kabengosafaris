package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.ParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving park activity documents.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkActivityDocumentGetService {

    private final ParkActivityDocumentRepository parkActivityDocumentRepository;
    private final ParkActivityDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

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

    @Autowired
    public ParkActivityDocumentGetService(
        ParkActivityDocumentRepository parkActivityDocumentRepository,
        ParkActivityDocumentStorageService storageService,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.parkActivityDocumentRepository = parkActivityDocumentRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    public ParkActivityDocumentDTO toDTO(ParkActivityDocument document) {
        String obfuscatedId = idObfuscator.encodeId(document.getId());
        return ParkActivityDocumentDTO.builder()
            .id(obfuscatedId)
            .parkId(idObfuscator.encodeId(document.getParkActivity().getPark().getId()))
            .parkName(document.getParkActivity().getPark().getName())
            .activityId(idObfuscator.encodeId(document.getParkActivity().getActivity().getId()))
            .activityName(document.getParkActivity().getActivity().getName())
            .title(document.getTitle())
            .documentType(document.getDocumentType())
            .documentTypeDisplayName(document.getDocumentType().getDisplayName())
            .documentTypeDescription(document.getDocumentType().getDescription())
            .documentUrl(storageService.constructDocumentUrl(obfuscatedId))
            .fileDocumentUrl(storageService.constructFileDocumentUrl(document.getFileName()))
            .fileName(document.getFileName())
            .originalFileName(document.getOriginalFileName())
            .fileSize(document.getFileSize())
            .fileSizeFormatted(document.getFileSize() != null ? storageService.formatFileSize(document.getFileSize()) : null)
            .fileType(document.getFileType())
            .description(document.getDescription())
            .version(document.getVersion())
            .notes(document.getNotes())
            .validFrom(document.getValidFrom())
            .validTo(document.getValidTo())
            .isCurrentlyValid(document.isCurrentlyValid())
            .isActive(document.getIsActive())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    public ResponseEntity<?> getAllDocuments(
            String obfuscatedParkId,
            String obfuscatedActivityId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String version,
            Boolean currentlyValid,
            String parkName,
            String activityName,
            Boolean parkIsActive,
            Boolean activityIsActive,
            Boolean hasTariff,
            java.util.List<DocumentType> documentTypes,
            java.util.List<String> statuses,
            java.util.List<String> validity,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            String keyword,
            Boolean includeStats,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        Specification<ParkActivityDocument> spec = Specification.unrestricted();

        // Filter by park ID
        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                Long parkId = idObfuscator.decodeId(obfuscatedParkId);
                spec = spec.and(ParkActivityDocumentSpecification.byParkId(parkId));
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", obfuscatedParkId);
            }
        }

        // Filter by activity ID
        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
                spec = spec.and(ParkActivityDocumentSpecification.byActivityId(activityId));
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", obfuscatedActivityId);
            }
        }

        if (documentType != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byDocumentType(documentType));
        }
        if (isActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byIsActive(isActive));
        }
        if (title != null && !title.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byTitle(title));
        }
        if (version != null && !version.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byVersion(version));
        }
        if (Boolean.TRUE.equals(currentlyValid)) {
            spec = spec.and(ParkActivityDocumentSpecification.currentlyValid(LocalDateTime.now()));
        }
        if (parkName != null && !parkName.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byParkName(parkName));
        }
        if (activityName != null && !activityName.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityName(activityName));
        }
        if (parkIsActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byParkIsActive(parkIsActive));
        }
        if (activityIsActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityIsActive(activityIsActive));
        }
        if (hasTariff != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityHasTariff(hasTariff));
        }
        // multi-value facets: every stat card must be reachable as a filter
        if (documentTypes != null && !documentTypes.isEmpty()) {
            spec = spec.and(ParkActivityDocumentSpecification.documentTypeIn(documentTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            // active+inactive is every row, so it cancels to no constraint
            if (states.size() == 1) spec = spec.and(ParkActivityDocumentSpecification.byIsActive(states.get(0)));
        }
        if (validity != null && !validity.isEmpty()) {
            spec = spec.and(ParkActivityDocumentSpecification.validityIn(validity));
        }
        if (createdAfter != null) spec = spec.and(ParkActivityDocumentSpecification.createdAfter(createdAfter));
        if (createdBefore != null) spec = spec.and(ParkActivityDocumentSpecification.createdBefore(createdBefore));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.searchKeyword(keyword));
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

        Page<ParkActivityDocument> documentPage = parkActivityDocumentRepository.findAll(spec, pageable);

        List<ParkActivityDocumentDTO> documentDTOs = documentPage.getContent().stream()
            .map(this::toDTO)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("documents", documentDTOs);
        response.put("currentPage", documentPage.getNumber());
        response.put("totalItems", documentPage.getTotalElements());
        response.put("totalPages", documentPage.getTotalPages());
        response.put("pageSize", documentPage.getSize());
        response.put("hasNext", documentPage.hasNext());
        response.put("hasPrevious", documentPage.hasPrevious());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        // counters built from the SAME spec as the rows, so cards and table agree
        if (includeStats == null || includeStats) {
            response.put("stats", computeStats(spec));
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", response));
    }

    /**
     * The filter chain the record pager walks — the same dimensions the list
     * offers, so paging from a filtered list stays inside those matches.
     */
    private Specification<ParkActivityDocument> navigationSpec(
        String obfuscatedParkId,
        String obfuscatedActivityId,
        java.util.List<DocumentType> documentTypes,
        java.util.List<String> statuses,
        java.util.List<String> validity,
        LocalDateTime createdAfter,
        String keyword
    ) {
        Specification<ParkActivityDocument> spec = Specification.unrestricted();
        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                spec = spec.and(ParkActivityDocumentSpecification.byParkId(idObfuscator.decodeId(obfuscatedParkId)));
            } catch (Exception ignored) { /* an unreadable id just means no park filter */ }
        }
        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                spec = spec.and(ParkActivityDocumentSpecification.byActivityId(idObfuscator.decodeId(obfuscatedActivityId)));
            } catch (Exception ignored) { /* likewise */ }
        }
        if (documentTypes != null && !documentTypes.isEmpty()) {
            spec = spec.and(ParkActivityDocumentSpecification.documentTypeIn(documentTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            if (states.size() == 1) spec = spec.and(ParkActivityDocumentSpecification.byIsActive(states.get(0)));
        }
        if (validity != null && !validity.isEmpty()) {
            spec = spec.and(ParkActivityDocumentSpecification.validityIn(validity));
        }
        if (createdAfter != null) spec = spec.and(ParkActivityDocumentSpecification.createdAfter(createdAfter));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.searchKeyword(keyword));
        }
        return spec;
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(Specification<ParkActivityDocument> base) {
        return listStats.of(ParkActivityDocument.class, base)
            .total()
            .count("active", ParkActivityDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("expired", ParkActivityDocumentSpecification.expired())
            .count("expiringSoon", ParkActivityDocumentSpecification.expiringWithin(30))
            .count("noExpiry", ParkActivityDocumentSpecification.noExpiry())
            .breakdown("byDocumentType", ParkActivityDocument.DocumentType.values(), ParkActivityDocumentSpecification::byDocumentType)
            .recency(ParkActivityDocumentSpecification::createdAfter)
            .build();
    }

    public ResponseEntity<?> getDocumentById(String obfuscatedId) {
        return getDocumentById(obfuscatedId, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One document, plus where it sits in the set the caller was looking at — the
     * arrows walk the same filtered, sorted list the table showed.
     */
    public ResponseEntity<?> getDocumentById(
            String obfuscatedId,
            String obfuscatedParkId,
            String obfuscatedActivityId,
            java.util.List<DocumentType> documentTypes,
            java.util.List<String> statuses,
            java.util.List<String> validity,
            LocalDateTime createdAfter,
            String keyword,
            String sortBy,
            String sortDirection
    ) {
        log.info("Getting park activity document with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ParkActivityDocument document = parkActivityDocumentRepository.findById(id).orElse(null);

            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            ParkActivityDocumentDTO documentDTO = toDTO(document);

            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ParkActivityDocument.class,
                navigationSpec(obfuscatedParkId, obfuscatedActivityId, documentTypes, statuses, validity, createdAfter, keyword),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            // the "N of M" readout makes the wraparound visible
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity document retrieved successfully", response));

        } catch (Exception e) {
            log.warn("Failed to decode park activity document ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByParkActivity(String obfuscatedParkId, String obfuscatedActivityId) {
        log.info("Getting documents for park-activity: parkId={}, activityId={}", obfuscatedParkId, obfuscatedActivityId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);

            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByParkActivityOrderByCreatedAtDesc(parkId, activityId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park or activity ID", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park or activity ID", "INVALID_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByParkId(String obfuscatedParkId) {
        log.info("Getting documents for park: {}", obfuscatedParkId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByParkIdOrderByCreatedAtDesc(parkId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park ID: {}", obfuscatedParkId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByActivityId(String obfuscatedActivityId) {
        log.info("Getting documents for activity: {}", obfuscatedActivityId);

        try {
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByActivityIdOrderByCreatedAtDesc(activityId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode activity ID: {}", obfuscatedActivityId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
            );
        }
    }

    public ParkActivityDocument getDocumentEntityByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return parkActivityDocumentRepository.findByFileName(fileName).orElse(null);
    }
}
